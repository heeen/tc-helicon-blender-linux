/// DCP (DICE Control Protocol) packet framing.
///
/// Header format (little-endian, 16 bytes):
/// ```text
/// [0..4]  command_id: u32  = (category << 12) | opcode
/// [4..6]  body_length: u16
/// [6..8]  command_index: u16 (sequence counter)
/// [8..16] reserved (zeros)
/// ```

/// Build a DCP packet with LE header.
pub fn build_packet(cmd_id: u32, body: &[u8], cmd_idx: u16) -> Vec<u8> {
    let mut pkt = vec![0u8; 16 + body.len()];
    pkt[0..4].copy_from_slice(&cmd_id.to_le_bytes());
    pkt[4..6].copy_from_slice(&(body.len() as u16).to_le_bytes());
    pkt[6..8].copy_from_slice(&cmd_idx.to_le_bytes());
    pkt[16..].copy_from_slice(body);
    pkt
}

/// Parse a DCP response header.
#[derive(Debug)]
pub struct DcpResponse {
    pub cmd_id: u32,
    pub body_len: u16,
    pub cmd_idx: u16,
    pub body: Vec<u8>,
}

impl DcpResponse {
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 16 {
            return None;
        }
        Some(Self {
            cmd_id: u32::from_le_bytes([data[0], data[1], data[2], data[3]]),
            body_len: u16::from_le_bytes([data[4], data[5]]),
            cmd_idx: u16::from_le_bytes([data[6], data[7]]),
            body: data[16..].to_vec(),
        })
    }

    pub fn category(&self) -> u16 {
        ((self.cmd_id >> 12) & 0xFFF) as u16
    }

    pub fn opcode(&self) -> u16 {
        (self.cmd_id & 0xFFF) as u16
    }
}

/// Build a command ID from category and opcode.
pub fn cmd_id(category: u16, opcode: u16) -> u32 {
    ((category as u32 & 0xFFF) << 12) | (opcode as u32 & 0xFFF)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn packet_roundtrip() {
        let pkt = build_packet(0x80F000, &[1, 2, 3], 5);
        assert_eq!(pkt.len(), 19);
        let resp = DcpResponse::parse(&pkt).unwrap();
        assert_eq!(resp.cmd_id, 0x80F000);
        assert_eq!(resp.body_len, 3);
        assert_eq!(resp.cmd_idx, 5);
        assert_eq!(resp.body, &[1, 2, 3]);
        assert_eq!(resp.category(), 0x80F);
        assert_eq!(resp.opcode(), 0);
    }

    #[test]
    fn cmd_id_composition() {
        assert_eq!(cmd_id(0x80F, 0x001), 0x80F001);
        assert_eq!(cmd_id(0, 2), 2);
    }
}
