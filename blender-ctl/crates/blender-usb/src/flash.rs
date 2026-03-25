//! USB flash programmer client for the DICE3 SPI flash handler.
//!
//! Communicates with the DCP category 0x81F handler injected into the
//! Blender firmware. Uses the standard DCP command/response mechanism
//! (bReq=2 write, bReq=3 read) with category 0x81F and opcodes 0–3.

use std::fs;
use std::io::Write;
use std::path::Path;

use anyhow::{bail, Context, Result};

use crate::BlenderUsb;

/// DCP category for our flash handler.
const FLASH_CATEGORY: u16 = 0x81F;

/// Maximum bytes per READ operation (DCP body limit).
const MAX_READ_CHUNK: usize = 1024;

/// Maximum data bytes per WRITE operation (DCP body = 4 addr + data).
const MAX_WRITE_CHUNK: usize = 1020;

/// Golden copy / recovery partition: SPI 0x10000–0x3FFFF.
/// The firmware handler also enforces this server-side.
const GOLDEN_START: u32 = 0x10000;
const GOLDEN_END: u32 = 0x40000;

/// Flash info returned by the INFO opcode.
#[derive(Debug)]
pub struct FlashInfo {
    pub jedec_id: u32,
    pub sector_size: u32,
    pub flash_size: u32,
}

impl std::fmt::Display for FlashInfo {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let mfr = (self.jedec_id >> 16) & 0xFF;
        let typ = (self.jedec_id >> 8) & 0xFF;
        let cap = self.jedec_id & 0xFF;
        write!(
            f,
            "JEDEC ID: {:06X} (mfr={:02X} type={:02X} cap={:02X})\n\
             Sector size: {} bytes ({} KB)\n\
             Flash size:  {} bytes ({} KB)",
            self.jedec_id,
            mfr,
            typ,
            cap,
            self.sector_size,
            self.sector_size / 1024,
            self.flash_size,
            self.flash_size / 1024,
        )
    }
}

/// Build a DCP command ID from category and opcode.
fn cmd_id(opcode: u16) -> u32 {
    ((FLASH_CATEGORY as u32) << 12) | (opcode as u32 & 0xFFF)
}

impl BlenderUsb {
    /// Send a flash command and return the response body.
    fn flash_cmd(&mut self, opcode: u16, body: &[u8]) -> Result<Vec<u8>> {
        let resp = self.dcp_command(cmd_id(opcode), body)?;
        if resp.len() < 16 {
            bail!("Flash command: short response ({} bytes)", resp.len());
        }
        Ok(resp[16..].to_vec())
    }

    /// Query flash info (opcode 0).
    pub fn flash_info(&mut self) -> Result<FlashInfo> {
        let body = self.flash_cmd(0, &[])?;
        if body.len() < 12 {
            bail!("Flash INFO: expected 12 bytes, got {}", body.len());
        }
        Ok(FlashInfo {
            jedec_id: u32::from_le_bytes([body[0], body[1], body[2], body[3]]),
            sector_size: u32::from_le_bytes([body[4], body[5], body[6], body[7]]),
            flash_size: u32::from_le_bytes([body[8], body[9], body[10], body[11]]),
        })
    }

    /// Read flash data (opcode 1). Handles chunking for large reads.
    pub fn flash_read(&mut self, addr: u32, len: u32) -> Result<Vec<u8>> {
        let mut result = Vec::with_capacity(len as usize);
        let mut offset = 0u32;

        while offset < len {
            let chunk = (len - offset).min(MAX_READ_CHUNK as u32);
            let mut body = Vec::with_capacity(6);
            body.extend_from_slice(&(addr + offset).to_le_bytes());
            body.extend_from_slice(&(chunk as u16).to_le_bytes());

            let data = self.flash_cmd(1, &body)?;

            // Check for error response (4 bytes starting with 0xDEAD)
            if data.len() == 4 {
                let status = u32::from_le_bytes([data[0], data[1], data[2], data[3]]);
                if status != 0 {
                    bail!(
                        "Flash READ error at {:#x}: status {:#x}",
                        addr + offset,
                        status
                    );
                }
            }

            result.extend_from_slice(&data);
            offset += chunk;

            if offset < len && offset % 0x10000 == 0 {
                log::info!("  read {:#x}/{:#x}...", offset, len);
            }
        }

        Ok(result)
    }

    /// Erase flash range (opcode 2). Addr and len should be sector-aligned.
    pub fn flash_erase(&mut self, addr: u32, len: u32) -> Result<()> {
        let end = addr + len;
        if addr < GOLDEN_END && end > GOLDEN_START {
            bail!(
                "Erase refused: range {:#x}–{:#x} overlaps golden copy ({:#x}–{:#x})",
                addr,
                end,
                GOLDEN_START,
                GOLDEN_END,
            );
        }

        let mut body = Vec::with_capacity(8);
        body.extend_from_slice(&addr.to_le_bytes());
        body.extend_from_slice(&len.to_le_bytes());

        let resp = self.flash_cmd(2, &body)?;
        if resp.len() < 4 {
            bail!("Flash ERASE: short response");
        }
        let status = u32::from_le_bytes([resp[0], resp[1], resp[2], resp[3]]);
        if status != 0 {
            bail!("Flash ERASE failed at {:#x}: status {:#x}", addr, status);
        }
        Ok(())
    }

    /// Write data to flash (opcode 3). Handles chunking.
    /// Flash must be erased first!
    pub fn flash_write(&mut self, addr: u32, data: &[u8]) -> Result<()> {
        let end = addr as u64 + data.len() as u64;
        if (addr as u64) < GOLDEN_END as u64 && end > GOLDEN_START as u64 {
            bail!(
                "Write refused: range {:#x}–{:#x} overlaps golden copy ({:#x}–{:#x})",
                addr,
                end,
                GOLDEN_START,
                GOLDEN_END,
            );
        }

        let mut offset = 0usize;
        while offset < data.len() {
            let chunk = (data.len() - offset).min(MAX_WRITE_CHUNK);
            let mut body = Vec::with_capacity(4 + chunk);
            body.extend_from_slice(&(addr + offset as u32).to_le_bytes());
            body.extend_from_slice(&data[offset..offset + chunk]);

            let resp = self.flash_cmd(3, &body)?;
            if resp.len() < 4 {
                bail!("Flash WRITE: short response");
            }
            let status = u32::from_le_bytes([resp[0], resp[1], resp[2], resp[3]]);
            if status != 0 {
                bail!(
                    "Flash WRITE failed at {:#x}: status {:#x}",
                    addr + offset as u32,
                    status
                );
            }

            offset += chunk;
            if offset < data.len() && offset % 0x10000 == 0 {
                log::info!("  written {:#x}/{:#x}...", offset, data.len());
            }
        }
        Ok(())
    }

    /// Read and verify flash contents against expected data.
    pub fn flash_verify(&mut self, addr: u32, expected: &[u8]) -> Result<bool> {
        let actual = self.flash_read(addr, expected.len() as u32)?;
        if actual.len() != expected.len() {
            bail!(
                "Verify length mismatch: got {} expected {}",
                actual.len(),
                expected.len()
            );
        }
        for (i, (&a, &e)) in actual.iter().zip(expected.iter()).enumerate() {
            if a != e {
                log::warn!(
                    "Verify mismatch at {:#x}: got {:#04x} expected {:#04x}",
                    addr + i as u32,
                    a,
                    e
                );
                return Ok(false);
            }
        }
        Ok(true)
    }

    /// Dump entire memory-mapped flash (1MB) to a file.
    pub fn flash_dump(&mut self, path: &Path) -> Result<()> {
        let info = self.flash_info()?;
        // Dump the memory-mapped region (1MB)
        let size = 0x100000u32.min(info.flash_size);
        println!("Reading {:#x} bytes from flash...", size);

        let data = self.flash_read(0, size)?;

        let mut f = fs::File::create(path).context("Failed to create dump file")?;
        f.write_all(&data).context("Failed to write dump")?;
        println!("Wrote {} bytes to {}", data.len(), path.display());
        Ok(())
    }

    /// Full firmware update: erase + write + verify.
    /// Only touches the primary copy (0x40000–0x8AFFF). Golden copy is protected.
    pub fn flash_update(&mut self, image_path: &Path) -> Result<()> {
        let image = fs::read(image_path).context("Failed to read firmware image")?;

        // Primary partition starts at 0x40000, max ~300KB body
        if image.len() > 0x4B000 {
            bail!(
                "Image too large ({} bytes, max {} for primary copy)",
                image.len(),
                0x4B000
            );
        }

        // Get flash info
        let info = self.flash_info()?;
        println!("{}", info);
        println!();

        let sector_size = info.sector_size as usize;
        if sector_size == 0 || sector_size > 0x10000 {
            bail!("Invalid sector size: {}", sector_size);
        }

        // Read current image for backup comparison
        println!("Reading current flash for comparison...");
        let current = self.flash_read(0, image.len() as u32)?;

        // Find changed sectors
        let mut changed_sectors = Vec::new();
        for sector_start in (0..image.len()).step_by(sector_size) {
            let sector_end = (sector_start + sector_size).min(image.len());
            let new_sector = &image[sector_start..sector_end];
            let old_sector = if sector_start < current.len() {
                &current[sector_start..sector_end.min(current.len())]
            } else {
                &[]
            };

            if new_sector != old_sector {
                changed_sectors.push(sector_start as u32);
            }
        }

        println!(
            "{} of {} sectors need updating",
            changed_sectors.len(),
            (image.len() + sector_size - 1) / sector_size
        );

        if changed_sectors.is_empty() {
            println!("Flash is already up to date!");
            return Ok(());
        }

        for (i, &sector_addr) in changed_sectors.iter().enumerate() {
            let sector_end = (sector_addr as usize + sector_size).min(image.len());
            let sector_data = &image[sector_addr as usize..sector_end];

            println!(
                "[{}/{}] Sector {:#07x}: erase...",
                i + 1,
                changed_sectors.len(),
                sector_addr
            );
            self.flash_erase(sector_addr, sector_size as u32)?;

            print!("  write...");
            std::io::stdout().flush()?;
            self.flash_write(sector_addr, sector_data)?;

            print!(" verify...");
            std::io::stdout().flush()?;
            let ok = self.flash_verify(sector_addr, sector_data)?;
            if !ok {
                bail!("Verification failed at sector {:#x}!", sector_addr);
            }
            println!(" OK");
        }

        println!();
        println!(
            "Update complete! {} sector(s) written and verified.",
            changed_sectors.len()
        );
        println!("Golden copy ({:#x}–{:#x}) is untouched.", GOLDEN_START, GOLDEN_END);
        Ok(())
    }
}
