use std::path::PathBuf;

use anyhow::Result;
use clap::{Parser, Subcommand};
use tokio::io::{self, AsyncBufReadExt, BufReader};
use tokio::sync::{mpsc, watch};

use blender_proto::dcp::DcpResponse;
use blender_proto::param::ParamId;
use blender_proto::state::MixerState;
use blender_proto::tuple::Tuple;

#[derive(Parser)]
#[command(name = "blender-ctl", about = "TC Helicon Blender control tool")]
struct Cli {
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// Start BLE + TUI mixer
    Tui,
    /// Start BLE + interactive REPL
    Ble,
    /// Set a parameter via BLE
    Set {
        /// Parameter name (input1, level, comp, mic, talk, mute, comp_on_off)
        param: String,
        /// Value (0-255 or 0xNN)
        value: String,
        /// Bus number (0-3), omit to set all buses for slider params
        #[arg(long, short)]
        bus: Option<u8>,
    },
    /// Get current state via BLE
    Get {
        /// Parameter name (omit for full state)
        param: Option<String>,
    },
    /// USB subcommands
    #[command(subcommand)]
    Usb(UsbCommand),
}

#[derive(Subcommand)]
enum UsbCommand {
    /// Initialize the device (activate USB audio)
    Init,
    /// Show USB device info and descriptors
    Info,
    /// Send a raw DCP command
    Dcp {
        /// Command ID (hex, e.g. 0x80F000)
        cmd_id: String,
        /// Body as hex bytes (e.g. "01 02 03")
        body: Option<String>,
    },
    /// SPI flash operations (requires patched firmware)
    #[command(subcommand)]
    Flash(FlashCommand),
}

#[derive(Subcommand)]
enum FlashCommand {
    /// Query flash info (JEDEC ID, sector/flash size)
    Info,
    /// Read flash region
    Read {
        /// Start address (hex, e.g. 0x50000)
        addr: String,
        /// Length in bytes (hex or decimal)
        len: String,
        /// Output file (omit for hexdump to stdout)
        file: Option<String>,
    },
    /// Erase flash sectors
    Erase {
        /// Start address (sector-aligned)
        addr: String,
        /// Length in bytes
        len: String,
    },
    /// Write file to flash (must be erased first!)
    Write {
        /// Start address
        addr: String,
        /// Input file
        file: String,
    },
    /// Full 1MB flash dump
    Dump {
        /// Output file (default: flash_dump.bin)
        file: Option<String>,
    },
    /// Erase + write + verify firmware update (primary copy only)
    Update {
        /// Firmware image file
        file: String,
    },
    /// Erase + write multiple sectors in one session (addr:file pairs)
    WriteSectors {
        /// Sector specifications: 0xADDR:path [0xADDR:path ...]
        specs: Vec<String>,
    },
}

fn parse_value(s: &str) -> Result<u8> {
    let s = s.trim();
    if let Some(hex) = s.strip_prefix("0x").or_else(|| s.strip_prefix("0X")) {
        Ok(u8::from_str_radix(hex, 16)?)
    } else {
        let val: i16 = s.parse()?;
        if val < 0 {
            Ok((val as i8) as u8)
        } else {
            Ok(val as u8)
        }
    }
}

fn parse_cmd_id(s: &str) -> Result<u32> {
    let s = s.trim();
    if let Some(hex) = s.strip_prefix("0x").or_else(|| s.strip_prefix("0X")) {
        Ok(u32::from_str_radix(hex, 16)?)
    } else {
        Ok(s.parse()?)
    }
}

fn parse_hex_body(s: &str) -> Result<Vec<u8>> {
    let mut bytes = Vec::new();
    for part in s.split_whitespace() {
        bytes.push(u8::from_str_radix(part, 16)?);
    }
    Ok(bytes)
}

fn format_value(val: u8) -> String {
    format!("{val:3} (0x{val:02X})")
}

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::Builder::from_env(env_logger::Env::default().default_filter_or("info"))
        .format_timestamp_millis()
        .init();

    let cli = Cli::parse();

    match cli.command {
        Command::Tui => cmd_tui().await,
        Command::Ble => cmd_ble().await,
        Command::Set { param, value, bus } => cmd_set(&param, &value, bus).await,
        Command::Get { param } => cmd_get(param.as_deref()).await,
        Command::Usb(usb) => {
            // USB commands are sync — run in blocking thread
            tokio::task::spawn_blocking(move || match usb {
                UsbCommand::Init => cmd_usb_init(),
                UsbCommand::Info => cmd_usb_info(),
                UsbCommand::Dcp { cmd_id, body } => cmd_usb_dcp(&cmd_id, body.as_deref()),
                UsbCommand::Flash(flash) => cmd_usb_flash(flash),
            })
            .await?
        }
    }
}

// ── BLE Commands ────────────────────────────────────────────────────────

async fn cmd_tui() -> Result<()> {
    let server = blender_ble::BleClient::connect().await?;

    let state_rx = server.state_watch();
    let cmd_tx = server.command_sender();

    blender_tui::run(state_rx, cmd_tx).await
}

async fn cmd_ble() -> Result<()> {
    let server = blender_ble::BleClient::connect().await?;
    println!("Type 'help' for commands.\n");

    let mut state_rx = server.state_watch();
    let cmd_tx = server.command_sender();

    let stdin = BufReader::new(io::stdin());
    let mut lines = stdin.lines();

    loop {
        tokio::select! {
            line = lines.next_line() => {
                let line = match line? {
                    Some(l) => l,
                    None => break,
                };
                if handle_ble_command(&line, &state_rx, &cmd_tx).await? {
                    break;
                }
            }
            _ = state_rx.changed() => {
                // State changed — could show a notification
                let s = state_rx.borrow();
                if s.connected {
                    log::debug!("State updated");
                }
            }
        }
    }

    Ok(())
}

async fn handle_ble_command(
    line: &str,
    state_rx: &watch::Receiver<MixerState>,
    cmd_tx: &mpsc::Sender<Vec<Tuple>>,
) -> Result<bool> {
    let parts: Vec<&str> = line.split_whitespace().collect();
    if parts.is_empty() {
        return Ok(false);
    }

    match parts[0] {
        "help" | "?" | "h" => {
            println!("Commands:");
            println!("  status              Show all parameter values");
            println!("  input1 <val>        Set input1 on all 4 buses");
            println!("  input1.0 <val>      Set input1 bus 0 only");
            println!("  level <val>         Master output level");
            println!("  comp <val>          Compressor threshold");
            println!("  comp on|off         Enable/disable compressor");
            println!("  mic <val>           Mic gain");
            println!("  talk on|off         Talkback toggle");
            println!("  mute <bitmap>       Set mute output bitmap");
            println!("  raw <id> <sub> <v>  Send raw tuple");
            println!("  dump                Request full state from Blender");
            println!("  quit                Exit");
        }
        "status" | "st" | "s" => {
            let state = state_rx.borrow();
            println!("Connected: {}", state.connected);
            println!();
            println!("  {:12}  {:>14}  {:>14}  {:>14}  {:>14}", "", "Bus A", "Bus B", "Bus C", "Bus D");
            for &param in ParamId::SLIDERS {
                let vals: Vec<String> = (0..4)
                    .map(|s| format_value(state.get(param, s)))
                    .collect();
                println!(
                    "  {:12}  {:>14}  {:>14}  {:>14}  {:>14}",
                    param.name(),
                    vals[0],
                    vals[1],
                    vals[2],
                    vals[3]
                );
            }
            println!();
            for &param in &[
                ParamId::Talk,
                ParamId::MuteOutput,
                ParamId::CompressorOnOff,
                ParamId::BlenderState,
                ParamId::Version,
            ] {
                println!("  {:12}  {}", param.name(), format_value(state.get(param, 0)));
            }
        }
        "quit" | "exit" | "q" => return Ok(true),
        "dump" => {
            cmd_tx
                .send(vec![Tuple::new(ParamId::RequestState as u8, 0, 0)])
                .await?;
            println!("Requested state dump from Blender");
        }
        "raw" => {
            if parts.len() < 4 {
                println!("Usage: raw <param_id> <sub> <value>");
            } else {
                let pid: u8 = parts[1].parse()?;
                let sub: u8 = parts[2].parse()?;
                let val = parse_value(parts[3])?;
                cmd_tx.send(vec![Tuple::new(pid, sub, val)]).await?;
                println!("Sent: ({pid}, {sub}, {})", format_value(val));
            }
        }
        cmd => {
            // comp on/off
            if (cmd == "comp" || cmd == "compressor") && parts.len() == 2 {
                match parts[1] {
                    "on" => {
                        cmd_tx
                            .send(vec![Tuple::new(ParamId::CompressorOnOff as u8, 0, 0x0F)])
                            .await?;
                        return Ok(false);
                    }
                    "off" => {
                        cmd_tx
                            .send(vec![Tuple::new(ParamId::CompressorOnOff as u8, 0, 0x00)])
                            .await?;
                        return Ok(false);
                    }
                    _ => {}
                }
            }

            // talk on/off
            if cmd == "talk" && parts.len() == 2 {
                match parts[1] {
                    "on" => {
                        cmd_tx
                            .send(vec![Tuple::new(ParamId::Talk as u8, 0, 1)])
                            .await?;
                        return Ok(false);
                    }
                    "off" => {
                        cmd_tx
                            .send(vec![Tuple::new(ParamId::Talk as u8, 0, 0)])
                            .await?;
                        return Ok(false);
                    }
                    _ => {}
                }
            }

            // mute
            if cmd == "mute" && parts.len() == 2 {
                let val = parse_value(parts[1])?;
                cmd_tx
                    .send(vec![Tuple::new(ParamId::MuteOutput as u8, 0, val)])
                    .await?;
                return Ok(false);
            }

            // Param with optional .sub notation
            let (param_name, sub_str) = if let Some(dot) = cmd.find('.') {
                (&cmd[..dot], Some(&cmd[dot + 1..]))
            } else {
                (cmd, None)
            };

            let param = match ParamId::from_name(param_name) {
                Some(p) => p,
                None => {
                    println!("Unknown command: {cmd}");
                    return Ok(false);
                }
            };

            if parts.len() < 2 {
                // Just show current value
                let state = state_rx.borrow();
                if param.is_slider() {
                    for s in 0..4 {
                        println!(
                            "  {}[{s}] = {}",
                            param.name(),
                            format_value(state.get(param, s))
                        );
                    }
                } else {
                    println!("  {} = {}", param.name(), format_value(state.get(param, 0)));
                }
                return Ok(false);
            }

            let val = parse_value(parts[1])?;

            if let Some(sub_s) = sub_str {
                let sub: u8 = sub_s.parse()?;
                cmd_tx.send(vec![Tuple::new(param as u8, sub, val)]).await?;
            } else if param.is_slider() {
                let tuples: Vec<Tuple> = (0..4)
                    .map(|s| Tuple::new(param as u8, s, val))
                    .collect();
                cmd_tx.send(tuples).await?;
            } else {
                cmd_tx.send(vec![Tuple::new(param as u8, 0, val)]).await?;
            }
        }
    }

    Ok(false)
}

async fn cmd_set(param_name: &str, value_str: &str, bus: Option<u8>) -> Result<()> {
    let param = ParamId::from_name(param_name)
        .ok_or_else(|| anyhow::anyhow!("Unknown parameter: {param_name}"))?;
    let value = parse_value(value_str)?;

    let mut server = blender_ble::BleClient::connect().await?;
    server.wait_connected().await;

    let tuples = if let Some(bus) = bus {
        vec![Tuple::new(param as u8, bus, value)]
    } else if param.is_slider() {
        (0..4).map(|s| Tuple::new(param as u8, s, value)).collect()
    } else {
        vec![Tuple::new(param as u8, 0, value)]
    };

    server.send(tuples).await?;
    println!("Set {}={}", param.name(), format_value(value));

    // Brief delay for the indication to be sent
    tokio::time::sleep(std::time::Duration::from_millis(200)).await;
    Ok(())
}

async fn cmd_get(param_name: Option<&str>) -> Result<()> {
    let mut server = blender_ble::BleClient::connect().await?;
    server.wait_connected().await;

    // Wait a moment for state dump to arrive
    tokio::time::sleep(std::time::Duration::from_millis(500)).await;

    let state = server.state_watch().borrow().clone();

    if let Some(name) = param_name {
        let param = ParamId::from_name(name)
            .ok_or_else(|| anyhow::anyhow!("Unknown parameter: {name}"))?;
        if param.is_slider() {
            for s in 0..4 {
                println!("{}[{s}] = {}", param.name(), format_value(state.get(param, s)));
            }
        } else {
            println!("{} = {}", param.name(), format_value(state.get(param, 0)));
        }
    } else {
        // Print full state
        for &param in ParamId::SLIDERS {
            let vals: Vec<String> = (0..4)
                .map(|s| format_value(state.get(param, s)))
                .collect();
            println!(
                "{:12}  {}  {}  {}  {}",
                param.name(),
                vals[0],
                vals[1],
                vals[2],
                vals[3]
            );
        }
        println!();
        for &param in &[
            ParamId::Talk,
            ParamId::MuteOutput,
            ParamId::CompressorOnOff,
            ParamId::BlenderState,
            ParamId::Version,
        ] {
            println!("{:12}  {}", param.name(), format_value(state.get(param, 0)));
        }
    }

    Ok(())
}

// ── USB Commands ────────────────────────────────────────────────────────

fn cmd_usb_init() -> Result<()> {
    let dev = blender_usb::BlenderUsb::open()?;
    println!("Found {} at {}", dev.name(), dev.addr);
    dev.claim()?;
    dev.init()?;
    dev.release();
    println!("Done! Device should now be available as an ALSA device.");
    println!("Check with: arecord -l / aplay -l");
    Ok(())
}

fn cmd_usb_info() -> Result<()> {
    blender_usb::BlenderUsb::print_info()
}

fn cmd_usb_flash(cmd: FlashCommand) -> Result<()> {
    let mut dev = blender_usb::BlenderUsb::open()?;
    println!("Found {} at {}", dev.name(), dev.addr);
    dev.claim()?;

    // Skip boot init for recovery PID — it doesn't support the normal init sequence.
    // For flash commands, init failure is non-fatal: the device may already be
    // running (e.g. after JTAG injection) and just needs DCP reset.
    if dev.pid != blender_usb::PID_BLENDER_BOOT {
        if let Err(_) = dev.ping() {
            log::info!("Device not initialized, running boot sequence...");
            if let Err(e) = dev.init() {
                log::warn!("Boot init failed ({e}), proceeding anyway — device may already be running");
            }
        }
        let _ = dev.dcp_flush();
        let _ = dev.dcp_reset();
    }

    let result = match cmd {
        FlashCommand::Info => {
            let info = dev.flash_info()?;
            println!("{info}");
            Ok(())
        }
        FlashCommand::Read { addr, len, file } => {
            let addr = parse_u32(&addr)?;
            let len = parse_u32(&len)?;
            let data = dev.flash_read(addr, len)?;
            if let Some(path) = file {
                std::fs::write(&path, &data)?;
                println!("Wrote {} bytes to {path}", data.len());
            } else {
                blender_usb::hexdump("", &data);
            }
            Ok(())
        }
        FlashCommand::Erase { addr, len } => {
            let addr = parse_u32(&addr)?;
            let len = parse_u32(&len)?;
            println!("Erasing {len:#x} bytes at {addr:#x}...");
            dev.flash_erase(addr, len)?;
            println!("Erase OK");
            Ok(())
        }
        FlashCommand::Write { addr, file } => {
            let addr = parse_u32(&addr)?;
            let data = std::fs::read(&file)?;
            println!("Writing {} bytes to {addr:#x}...", data.len());
            dev.flash_write(addr, &data)?;
            println!("Write OK");
            Ok(())
        }
        FlashCommand::Dump { file } => {
            let path = PathBuf::from(file.as_deref().unwrap_or("flash_dump.bin"));
            dev.flash_dump(&path)?;
            Ok(())
        }
        FlashCommand::Update { file } => {
            dev.flash_update(&PathBuf::from(file))?;
            Ok(())
        }
        FlashCommand::WriteSectors { specs } => {
            for spec in &specs {
                let (addr_str, path) = spec.split_once(':')
                    .ok_or_else(|| anyhow::anyhow!("Bad spec '{}', expected 0xADDR:path", spec))?;
                let addr = parse_u32(addr_str)?;
                let data = std::fs::read(path)
                    .map_err(|e| anyhow::anyhow!("Failed to read {}: {}", path, e))?;
                let sector_size = 0x1000u32;
                println!("[{addr:#x}] erase {sector_size:#x}...");
                dev.flash_erase(addr, sector_size)?;
                // Flush stale DCP state + settle after erase
                std::thread::sleep(std::time::Duration::from_millis(100));
                dev.dcp_flush()?;
                dev.ping()?;
                std::thread::sleep(std::time::Duration::from_millis(100));
                println!("[{addr:#x}] write {} bytes...", data.len());
                dev.flash_write(addr, &data)?;
                println!("[{addr:#x}] OK");
            }
            Ok(())
        }
    };

    dev.release();
    result
}

fn parse_u32(s: &str) -> Result<u32> {
    let s = s.trim();
    if let Some(hex) = s.strip_prefix("0x").or_else(|| s.strip_prefix("0X")) {
        Ok(u32::from_str_radix(hex, 16)?)
    } else {
        Ok(s.parse()?)
    }
}

fn cmd_usb_dcp(cmd_id_str: &str, body_str: Option<&str>) -> Result<()> {
    let cmd_id = parse_cmd_id(cmd_id_str)?;
    let body = match body_str {
        Some(s) => parse_hex_body(s)?,
        None => vec![],
    };

    let mut dev = blender_usb::BlenderUsb::open()?;
    println!("Found {} at {}", dev.name(), dev.addr);
    dev.claim()?;

    // Init check
    if let Err(_) = dev.ping() {
        log::info!("Device not initialized, running boot sequence...");
        dev.init()?;
    }
    dev.dcp_flush()?;
    dev.dcp_reset()?;

    let resp = dev.dcp_command(cmd_id, &body)?;
    if let Some(parsed) = DcpResponse::parse(&resp) {
        println!(
            "Response: cmd=0x{:08x} body_len={} idx={}",
            parsed.cmd_id, parsed.body_len, parsed.cmd_idx
        );
        if !parsed.body.is_empty() {
            blender_usb::hexdump("  ", &parsed.body);
        }
    } else {
        println!("Short response: {} bytes", resp.len());
    }

    dev.release();
    Ok(())
}
