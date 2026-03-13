/// A 3-byte BLE parameter tuple: (param_id, sub_param, value).
///
/// This is the wire format used on the ParameterCharacteristic.
/// Multiple tuples are concatenated in a single BLE write/indication.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Tuple {
    pub param_id: u8,
    pub sub_param: u8,
    pub value: u8,
}

impl Tuple {
    pub fn new(param_id: u8, sub_param: u8, value: u8) -> Self {
        Self {
            param_id,
            sub_param,
            value,
        }
    }

    /// Encode a slice of tuples into a byte buffer.
    pub fn encode(tuples: &[Tuple]) -> Vec<u8> {
        let mut buf = Vec::with_capacity(tuples.len() * 3);
        for t in tuples {
            buf.push(t.param_id);
            buf.push(t.sub_param);
            buf.push(t.value);
        }
        buf
    }

    /// Decode a byte buffer into tuples. Ignores trailing bytes if not a multiple of 3.
    pub fn decode(data: &[u8]) -> Vec<Tuple> {
        let mut tuples = Vec::with_capacity(data.len() / 3);
        let mut i = 0;
        while i + 2 < data.len() {
            tuples.push(Tuple {
                param_id: data[i],
                sub_param: data[i + 1],
                value: data[i + 2],
            });
            i += 3;
        }
        tuples
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encode_decode_roundtrip() {
        let tuples = vec![
            Tuple::new(0, 0, 200),
            Tuple::new(6, 1, 82),
            Tuple::new(17, 0, 0),
        ];
        let encoded = Tuple::encode(&tuples);
        assert_eq!(encoded.len(), 9);
        let decoded = Tuple::decode(&encoded);
        assert_eq!(decoded, tuples);
    }

    #[test]
    fn decode_ignores_trailing() {
        let data = [0, 0, 100, 1, 0]; // 1 full tuple + 2 trailing bytes
        let tuples = Tuple::decode(&data);
        assert_eq!(tuples.len(), 1);
        assert_eq!(tuples[0], Tuple::new(0, 0, 100));
    }

    #[test]
    fn empty() {
        assert!(Tuple::decode(&[]).is_empty());
        assert!(Tuple::encode(&[]).is_empty());
    }
}
