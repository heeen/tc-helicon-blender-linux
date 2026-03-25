pub mod flash;

use std::thread;
use std::time::Duration;

use anyhow::{bail, Context, Result};
use rusb::{DeviceHandle, Direction, GlobalContext, Recipient, RequestType};

use blender_proto::dcp;

pub const VID_TC_HELICON: u16 = 0x1220;
pub const PID_BLENDER: u16 = 0x8fe1;
pub const PID_BLENDER_BOOT: u16 = 0x802a;
pub const PID_GOXLR: u16 = 0x8fe0;
pub const PID_GOXLR_MINI: u16 = 0x8fe4;

const SUPPORTED_PIDS: &[u16] = &[PID_BLENDER, PID_BLENDER_BOOT, PID_GOXLR, PID_GOXLR_MINI];

pub fn pid_name(pid: u16) -> &'static str {
    match pid {
        PID_BLENDER => "Blender",
        PID_BLENDER_BOOT => "Blender (boot/recovery)",
        PID_GOXLR => "GoXLR",
        PID_GOXLR_MINI => "GoXLR Mini",
        _ => "Unknown",
    }
}

pub struct BlenderUsb {
    handle: DeviceHandle<GlobalContext>,
    pub pid: u16,
    pub addr: String,
    cmd_idx: u16,
}

impl BlenderUsb {
    /// Find and open the first TC Helicon device.
    pub fn open() -> Result<Self> {
        let devices = rusb::devices().context("Failed to enumerate USB devices")?;
        for device in devices.iter() {
            let desc = device
                .device_descriptor()
                .context("Failed to read device descriptor")?;
            if desc.vendor_id() == VID_TC_HELICON && SUPPORTED_PIDS.contains(&desc.product_id()) {
                let addr = format!(
                    "bus {:03} dev {:03} port {}",
                    device.bus_number(),
                    device.address(),
                    device.port_number()
                );
                let handle = device.open().context(
                    "Failed to open device (do you have permissions? try udev rule or run as root)",
                )?;
                return Ok(Self {
                    handle,
                    pid: desc.product_id(),
                    addr,
                    cmd_idx: 0,
                });
            }
        }
        bail!("No TC Helicon device found (looking for VID 0x{VID_TC_HELICON:04x})");
    }

    pub fn name(&self) -> &'static str {
        pid_name(self.pid)
    }

    /// Claim interfaces and enable auto-detach.
    pub fn claim(&self) -> Result<()> {
        self.handle
            .set_auto_detach_kernel_driver(true)
            .context("Couldn't enable auto-detach")?;
        for iface in 0..5 {
            match self.handle.claim_interface(iface) {
                Ok(()) => log::debug!("Claimed interface {iface}"),
                Err(e) => log::debug!("Interface {iface}: {e} (skipping)"),
            }
        }
        Ok(())
    }

    /// Release interfaces.
    pub fn release(&self) {
        for iface in 0..5 {
            let _ = self.handle.release_interface(iface);
        }
    }

    /// Run the USB init sequence (activate audio).
    pub fn init(&self) -> Result<()> {
        let req_in_vendor =
            rusb::request_type(Direction::In, RequestType::Vendor, Recipient::Interface);
        let req_out_class =
            rusb::request_type(Direction::Out, RequestType::Class, Recipient::Interface);
        let req_out_vendor =
            rusb::request_type(Direction::Out, RequestType::Vendor, Recipient::Interface);
        let req_in_vendor2 =
            rusb::request_type(Direction::In, RequestType::Vendor, Recipient::Interface);
        let timeout = Duration::from_secs(1);

        // 1. Vendor read 0x00
        let mut buf = [0u8; 24];
        match self
            .handle
            .read_control(req_in_vendor, 0x00, 0, 0, &mut buf, timeout)
        {
            Ok(n) => log::info!("Step 1 (vendor read 0x00): got {n} bytes"),
            Err(e) => log::warn!("Step 1 (vendor read 0x00): {e}"),
        }

        // 2. SET_CUR 48kHz on clock source 41
        let srate = 48000u32.to_le_bytes();
        self.handle
            .write_control(req_out_class, 0x01, 0x0100, 0x2900, &srate, timeout)
            .context("Step 2 (SET_CUR 48kHz) failed")?;
        log::info!("Step 2 (SET_CUR 48kHz): ok");

        // 3. Vendor write 0x01 (activate)
        match self
            .handle
            .write_control(req_out_vendor, 0x01, 0, 0, &[], timeout)
        {
            Ok(_) => log::info!("Step 3 (vendor write 0x01): ok"),
            Err(e) => log::warn!("Step 3 (vendor write 0x01): {e}"),
        }

        // 4. Wait for firmware
        thread::sleep(Duration::from_secs(1));

        // 5. Read device state
        let mut state = [0u8; 64];
        match self
            .handle
            .read_control(req_in_vendor2, 0x03, 0, 0, &mut state, timeout)
        {
            Ok(n) => log::info!("Step 5 (vendor read 0x03): got {n} bytes"),
            Err(e) => log::warn!("Step 5 (vendor read 0x03): {e}"),
        }

        Ok(())
    }

    /// Print device info (descriptors, interfaces, endpoints).
    pub fn print_info() -> Result<()> {
        let devices = rusb::devices().context("Failed to enumerate USB devices")?;
        let mut found = false;
        for device in devices.iter() {
            let desc = device.device_descriptor()?;
            if desc.vendor_id() != VID_TC_HELICON || !SUPPORTED_PIDS.contains(&desc.product_id()) {
                continue;
            }
            found = true;
            let pid = desc.product_id();
            println!(
                "{} (VID {:04x} PID {:04x}) at bus {:03} dev {:03}",
                pid_name(pid),
                desc.vendor_id(),
                pid,
                device.bus_number(),
                device.address(),
            );
            println!(
                "  USB {}.{}  class {:02x}:{:02x}:{:02x}",
                desc.usb_version().major(),
                desc.usb_version().minor(),
                desc.class_code(),
                desc.sub_class_code(),
                desc.protocol_code(),
            );
            println!("  {} configuration(s)", desc.num_configurations());

            for cfg_idx in 0..desc.num_configurations() {
                if let Ok(cfg) = device.config_descriptor(cfg_idx) {
                    println!(
                        "  Config {}: {} interface(s), max power {}mA",
                        cfg.number(),
                        cfg.num_interfaces(),
                        cfg.max_power() * 2,
                    );
                    for iface in cfg.interfaces() {
                        for alt in iface.descriptors() {
                            println!(
                                "    Interface {} alt {}: class {:02x}:{:02x}:{:02x}, {} endpoint(s)",
                                alt.interface_number(),
                                alt.setting_number(),
                                alt.class_code(),
                                alt.sub_class_code(),
                                alt.protocol_code(),
                                alt.num_endpoints(),
                            );
                            for ep in alt.endpoint_descriptors() {
                                println!(
                                    "      EP 0x{:02x} {:?} {:?} maxpkt {}",
                                    ep.address(),
                                    ep.direction(),
                                    ep.transfer_type(),
                                    ep.max_packet_size(),
                                );
                            }
                        }
                    }
                }
            }
        }
        if !found {
            bail!("No TC Helicon device found");
        }
        Ok(())
    }

    /// Send a DCP command and read the response.
    /// Uses the correct protocol: bReq=2 write, bReq=3 read, with retry loop.
    pub fn dcp_command(&mut self, cmd_id: u32, body: &[u8]) -> Result<Vec<u8>> {
        let bmreq_out =
            rusb::request_type(Direction::Out, RequestType::Vendor, Recipient::Interface);
        let bmreq_in =
            rusb::request_type(Direction::In, RequestType::Vendor, Recipient::Interface);
        let timeout = Duration::from_secs(2);

        // Don't increment for ResetCommandIndex (cmd_id=0)
        if cmd_id != 0 {
            self.cmd_idx = self.cmd_idx.wrapping_add(1);
        }

        let pkt = dcp::build_packet(cmd_id, body, self.cmd_idx);

        self.handle
            .write_control(bmreq_out, 2, 0, 0, &pkt, timeout)
            .context("DCP write (bReq=2) failed")?;

        // Retry loop: up to 20 attempts with 10ms sleep (matching goxlr-utility)
        for attempt in 0..20 {
            thread::sleep(Duration::from_millis(10));
            let mut resp = vec![0u8; 1040];
            match self
                .handle
                .read_control(bmreq_in, 3, 0, 0, &mut resp, timeout)
            {
                Ok(n) => {
                    resp.truncate(n);
                    return Ok(resp);
                }
                Err(e) => {
                    if attempt < 19 {
                        continue;
                    }
                    bail!("DCP response read failed after 20 attempts: {e}");
                }
            }
        }
        unreachable!()
    }

    /// Drain the DCP pipe — read all pending responses until empty.
    pub fn dcp_flush(&self) -> Result<()> {
        let bmreq_in =
            rusb::request_type(Direction::In, RequestType::Vendor, Recipient::Interface);
        let mut buf = vec![0u8; 1040];
        for _ in 0..10 {
            match self
                .handle
                .read_control(bmreq_in, 3, 0, 0, &mut buf, Duration::from_millis(100))
            {
                Ok(n) => log::debug!("Flushed {n} bytes from DCP pipe"),
                Err(_) => break, // timeout = pipe is empty
            }
        }
        Ok(())
    }

    /// Reset the DCP command index.
    pub fn dcp_reset(&mut self) -> Result<Vec<u8>> {
        self.cmd_idx = 0;
        // Drain any stale responses first
        self.dcp_flush()?;
        self.dcp_command(0, &[])
    }

    /// Ping the device (bReq=1, zero-length vendor write).
    pub fn ping(&self) -> Result<()> {
        let bmreq_out =
            rusb::request_type(Direction::Out, RequestType::Vendor, Recipient::Interface);
        self.handle
            .write_control(bmreq_out, 1, 0, 0, &[], Duration::from_secs(2))
            .context("Ping (bReq=1) failed")?;
        Ok(())
    }
}

pub fn hexdump(prefix: &str, data: &[u8]) {
    for row in 0..(data.len() + 15) / 16 {
        let off = row * 16;
        let end = (off + 16).min(data.len());
        let hex: String = data[off..end]
            .iter()
            .map(|b| format!("{b:02x}"))
            .collect::<Vec<_>>()
            .join(" ");
        let ascii: String = data[off..end]
            .iter()
            .map(|&b| {
                if b.is_ascii_graphic() || b == b' ' {
                    b as char
                } else {
                    '.'
                }
            })
            .collect();
        println!("{prefix}{off:04x}: {hex:<48} {ascii}");
    }
}
