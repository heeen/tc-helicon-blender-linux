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
    /// Start TUI mixer (BLE by default, --midi for USB MIDI)
    Tui {
        /// Use USB MIDI transport instead of BLE
        #[arg(long)]
        midi: bool,
    },
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
    /// USB MIDI SysEx subcommands (flash via patched primary firmware)
    #[command(subcommand)]
    Midi(MidiCommand),
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
    /// Reboot device (jumps to XIP bootloader, full DMA reload from SPI)
    Reboot,
    /// Force USB re-enumeration (simulates cable replug, no reboot)
    Reenum,
}

#[derive(Subcommand)]
enum MidiCommand {
    /// SPI flash operations over USB-MIDI SysEx (manuf 0x7D)
    #[command(subcommand)]
    Flash(MidiFlashCommand),
}

#[derive(Subcommand)]
enum MidiFlashCommand {
    /// Query flash + dispatcher version (JEDEC, sector/flash size, MFD tag)
    Info,
    /// Read flash region via sst25xx_fast_read
    Read {
        /// Start address (hex, e.g. 0x40000)
        addr: String,
        /// Length in bytes (hex or decimal, ≤ 192 per call)
        len: String,
        /// Output file (omit for hex dump to stdout)
        #[arg(long)]
        out: Option<String>,
    },
    /// HASH_RANGE — whole CRC + per-sector CRC array drained via MEM_READ
    Hash {
        /// Start address
        addr: String,
        /// Length in bytes
        len: String,
        /// Print every per-sector CRC
        #[arg(long)]
        show_sectors: bool,
    },
    /// Erase one or more 4 KB sectors
    Erase {
        /// Start address (sector-aligned)
        addr: String,
        /// Sector count
        #[arg(default_value = "1")]
        count: u16,
    },
    /// Write a small region (one frame, ≤ chunk size)
    Write {
        /// Start address
        addr: String,
        /// Input file
        file: String,
    },
    /// Sector-diff update — flash only sectors that differ from --ref
    Update {
        /// Reference image file
        #[arg(long)]
        r#ref: String,
        /// Region start in flash (default 0x40000 — patched primary base)
        #[arg(long, default_value = "0x40000")]
        region: String,
        /// Region length (default = primary size = 0x4B000 = 75 sectors)
        #[arg(long, default_value = "0x4B000")]
        length: String,
    },
    /// Reboot device (best-effort — reply may be lost during reset)
    Reboot {
        #[arg(long, default_value = "0")]
        mode: u8,
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
        Command::Tui { midi } => cmd_tui(midi).await,
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
        Command::Midi(midi) => {
            tokio::task::spawn_blocking(move || match midi {
                MidiCommand::Flash(flash) => cmd_midi_flash(flash),
            })
            .await?
        }
    }
}

// ── BLE Commands ────────────────────────────────────────────────────────

async fn cmd_tui(midi: bool) -> Result<()> {
    if midi {
        let client = blender_midi::MidiClient::connect()?;
        let state_rx = client.state_watch();
        let cmd_tx = client.command_sender();
        blender_tui::run(state_rx, cmd_tx).await
    } else {
        let server = blender_ble::BleClient::connect().await?;
        let state_rx = server.state_watch();
        let cmd_tx = server.command_sender();
        blender_tui::run(state_rx, cmd_tx).await
    }
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
        FlashCommand::Reboot => {
            println!("Rebooting device...");
            dev.flash_reboot()?;
            println!("Reboot command sent. Device will reload from SPI.");
            Ok(())
        }
        FlashCommand::Reenum => {
            println!("Forcing USB re-enumeration...");
            dev.flash_reenum()?;
            println!("USB re-enumeration triggered. Device will re-appear shortly.");
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

// ── MIDI Flash Commands ─────────────────────────────────────────────────

/// Wraps a MIDI flash error with a hint pointing at the JTAG fallback path.
/// Reached when MIDI is unreachable (e.g. patched primary not flashed yet,
/// EP3 OUT wedged after soft-reboot, dispatcher BSS un-initialized) — the
/// JTAG path bypasses the running firmware entirely.
fn jtag_fallback_hint(e: anyhow::Error) -> anyhow::Error {
    eprintln!();
    eprintln!("MIDI flash failed: {e:#}");
    eprintln!();
    eprintln!("JTAG fallback (requires Pico-DAP / MioLink):");
    eprintln!("  cd firmware/patch && make flash");
    eprintln!("    — uses blender_tool.py + v2 mailbox driver via JTAG.");
    eprintln!("    — flashes the persistent SPI image (sector-diffed).");
    eprintln!("    — auto-reboots; physical power-cycle still needed if EP3 OUT wedges.");
    eprintln!();
    e
}

fn cmd_midi_flash(cmd: MidiFlashCommand) -> Result<()> {
    let mut fc = blender_midi::flash::MidiFlash::open()
        .map_err(jtag_fallback_hint)?;

    match cmd {
        MidiFlashCommand::Info => {
            let info = fc.info().map_err(jtag_fallback_hint)?;
            println!("{info}");
            Ok(())
        }
        MidiFlashCommand::Read { addr, len, out } => {
            let addr = parse_u32(&addr)?;
            let total = parse_u32(&len)?;
            let mut buf = Vec::with_capacity(total as usize);
            let mut a = addr;
            let mut remaining = total;
            while remaining > 0 {
                let chunk = remaining.min(192) as u16;
                buf.extend(fc.read(a, chunk).map_err(jtag_fallback_hint)?);
                a += chunk as u32;
                remaining -= chunk as u32;
            }
            if let Some(path) = out {
                std::fs::write(&path, &buf)?;
                println!("wrote {} bytes → {path}", buf.len());
            } else {
                blender_usb::hexdump("  ", &buf);
            }
            Ok(())
        }
        MidiFlashCommand::Hash { addr, len, show_sectors } => {
            let addr = parse_u32(&addr)?;
            let length = parse_u32(&len)?;
            let (whole, count) = fc
                .hash_range(addr, length, blender_midi::flash::DEFAULT_SECTOR_BUF_ADDR, show_sectors)
                .map_err(jtag_fallback_hint)?;
            println!("whole CRC: {:#010x}", whole);
            println!("sectors:   {}", count);
            if show_sectors {
                let (_, crcs) = fc
                    .device_crc32_batch(addr, length, blender_midi::flash::DEFAULT_SECTOR_BUF_ADDR)
                    .map_err(jtag_fallback_hint)?;
                for (i, c) in crcs.iter().enumerate() {
                    let sec_addr = addr + (i as u32) * blender_midi::flash::SECTOR_SIZE;
                    println!("  sector {i:3} (0x{sec_addr:06x}): {:#010x}", c);
                }
            }
            Ok(())
        }
        MidiFlashCommand::Erase { addr, count } => {
            let addr = parse_u32(&addr)?;
            fc.erase_sector(addr, count).map_err(jtag_fallback_hint)?;
            println!("erased {count} sector(s) @ {addr:#x}");
            Ok(())
        }
        MidiFlashCommand::Write { addr, file } => {
            let addr = parse_u32(&addr)?;
            let data = std::fs::read(&file)?;
            let info = fc.info().map_err(jtag_fallback_hint)?;
            let chunk_size = info.write_aai_chunk();
            if data.len() > chunk_size {
                anyhow::bail!(
                    "write payload {} > dispatcher chunk size {}; use 'update' for multi-frame writes",
                    data.len(),
                    chunk_size
                );
            }
            fc.write_aai(addr, &data).map_err(jtag_fallback_hint)?;
            println!("wrote {} bytes @ {addr:#x}", data.len());
            Ok(())
        }
        MidiFlashCommand::Update { r#ref, region, length } => {
            cmd_midi_flash_update(&mut fc, &r#ref, &region, &length)
        }
        MidiFlashCommand::Reboot { mode } => {
            fc.reboot(mode).map_err(jtag_fallback_hint)?;
            println!("reboot requested (mode={mode}). Power-cycle if USB wedges.");
            Ok(())
        }
    }
}

fn cmd_midi_flash_update(
    fc: &mut blender_midi::flash::MidiFlash,
    ref_path: &str,
    region_str: &str,
    length_str: &str,
) -> Result<()> {
    use blender_midi::flash::{batch_diff, DEFAULT_SECTOR_BUF_ADDR, SECTOR_SIZE};

    let region = parse_u32(region_str)?;
    let length = parse_u32(length_str)?;
    let full = std::fs::read(ref_path)?;
    if region as usize + length as usize > full.len() {
        anyhow::bail!(
            "ref {ref_path} ({} bytes) too short for region {region:#x}+{length:#x}",
            full.len()
        );
    }
    let ref_slice = &full[region as usize..region as usize + length as usize];
    println!(
        "Reference: {ref_path} sliced to {region:#x}+{length:#x} ({} sectors)",
        length / SECTOR_SIZE
    );

    let info = fc.info().map_err(jtag_fallback_hint)?;
    println!("Device dispatcher: {info}");
    let chunk_size = info.write_aai_chunk();

    println!("Computing batch CRC + per-sector diff...");
    let diff = batch_diff(fc, region, ref_slice, DEFAULT_SECTOR_BUF_ADDR)
        .map_err(jtag_fallback_hint)?;
    if diff.diff.is_empty() {
        println!(
            "Already up to date (whole CRC = {:#010x}).",
            diff.whole_local
        );
        return Ok(());
    }
    println!(
        "  whole-image local=0x{:08x} device=0x{:08x}; {} sectors differ",
        diff.whole_local,
        diff.whole_device,
        diff.diff.len()
    );

    let total = diff.diff.len();
    for (idx, &rel_off) in diff.diff.iter().enumerate() {
        let sector_addr = region + rel_off;
        let sector_data = &ref_slice[rel_off as usize..(rel_off + SECTOR_SIZE) as usize];
        fc.erase_sector(sector_addr, 1).map_err(jtag_fallback_hint)?;
        let mut off = 0usize;
        while off < sector_data.len() {
            let end = (off + chunk_size).min(sector_data.len());
            let chunk = &sector_data[off..end];
            fc.write_aai(sector_addr + off as u32, chunk)
                .map_err(jtag_fallback_hint)?;
            off = end;
        }
        if (idx + 1) % 16 == 0 || idx + 1 == total {
            println!("  {}/{} sectors flashed...", idx + 1, total);
        }
    }

    println!("Re-verifying...");
    let post = batch_diff(fc, region, ref_slice, DEFAULT_SECTOR_BUF_ADDR)
        .map_err(jtag_fallback_hint)?;
    if post.diff.is_empty() {
        println!(
            "Done. {} sectors flashed; whole-image CRC matches ({:#010x}).",
            total, post.whole_local
        );
        Ok(())
    } else {
        eprintln!(
            "Verify FAILED: {} sectors still differ (first: {:#x})",
            post.diff.len(),
            region + post.diff[0]
        );
        Err(jtag_fallback_hint(anyhow::anyhow!(
            "post-flash diff still has {} sectors",
            post.diff.len()
        )))
    }
}
