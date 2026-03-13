/// BLE parameter IDs from Constants.java.
///
/// IDs 0–8 are "slider" params with 4 sub-params (output buses A–D).
/// Other params use sub_param=0 only.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[repr(u8)]
pub enum ParamId {
    Input1 = 0,
    Input2 = 1,
    Input3 = 2,
    Input4 = 3,
    Input5 = 4,
    Input6 = 5,
    Level = 6,
    Compressor = 7,
    MicGain = 8,
    Talk = 9,
    // 10 unused
    BlenderState = 11,
    Version = 12,
    // 13 unused
    IconChange = 14,
    ScanForMore = 15,
    Disconnect = 16,
    IsBlender = 17,
    HasBlender = 18,
    RequestState = 19,
    MuteOutput = 20,
    CompressorOnOff = 21,
}

impl ParamId {
    pub const ALL: &[ParamId] = &[
        Self::Input1,
        Self::Input2,
        Self::Input3,
        Self::Input4,
        Self::Input5,
        Self::Input6,
        Self::Level,
        Self::Compressor,
        Self::MicGain,
        Self::Talk,
        Self::BlenderState,
        Self::Version,
        Self::IconChange,
        Self::ScanForMore,
        Self::Disconnect,
        Self::IsBlender,
        Self::HasBlender,
        Self::RequestState,
        Self::MuteOutput,
        Self::CompressorOnOff,
    ];

    /// Slider params have 4 sub-params (buses A–D).
    pub const SLIDERS: &[ParamId] = &[
        Self::Input1,
        Self::Input2,
        Self::Input3,
        Self::Input4,
        Self::Input5,
        Self::Input6,
        Self::Level,
        Self::Compressor,
        Self::MicGain,
    ];

    pub fn is_slider(self) -> bool {
        (self as u8) <= 8
    }

    pub fn from_u8(val: u8) -> Option<ParamId> {
        match val {
            0 => Some(Self::Input1),
            1 => Some(Self::Input2),
            2 => Some(Self::Input3),
            3 => Some(Self::Input4),
            4 => Some(Self::Input5),
            5 => Some(Self::Input6),
            6 => Some(Self::Level),
            7 => Some(Self::Compressor),
            8 => Some(Self::MicGain),
            9 => Some(Self::Talk),
            11 => Some(Self::BlenderState),
            12 => Some(Self::Version),
            14 => Some(Self::IconChange),
            15 => Some(Self::ScanForMore),
            16 => Some(Self::Disconnect),
            17 => Some(Self::IsBlender),
            18 => Some(Self::HasBlender),
            19 => Some(Self::RequestState),
            20 => Some(Self::MuteOutput),
            21 => Some(Self::CompressorOnOff),
            _ => None,
        }
    }

    pub fn name(self) -> &'static str {
        match self {
            Self::Input1 => "input1",
            Self::Input2 => "input2",
            Self::Input3 => "input3",
            Self::Input4 => "input4",
            Self::Input5 => "input5",
            Self::Input6 => "input6",
            Self::Level => "level",
            Self::Compressor => "compressor",
            Self::MicGain => "mic_gain",
            Self::Talk => "talk",
            Self::BlenderState => "blender_state",
            Self::Version => "version",
            Self::IconChange => "icon_change",
            Self::ScanForMore => "scan_for_more",
            Self::Disconnect => "disconnect",
            Self::IsBlender => "is_blender",
            Self::HasBlender => "has_blender",
            Self::RequestState => "request_state",
            Self::MuteOutput => "mute_output",
            Self::CompressorOnOff => "comp_on_off",
        }
    }

    /// Default value (as unsigned byte) from Constants.java.
    pub fn default_value(self) -> u8 {
        match self {
            Self::Input1 | Self::Input2 | Self::Input3 | Self::Input4 | Self::Input5
            | Self::Input6 => 0xCD, // -51 signed
            Self::Level | Self::MicGain => 82,
            Self::Compressor => 0x83, // -125 signed
            _ => 0,
        }
    }

    /// Parse a CLI-friendly name to a ParamId.
    pub fn from_name(s: &str) -> Option<ParamId> {
        match s {
            "input1" | "in1" => Some(Self::Input1),
            "input2" | "in2" => Some(Self::Input2),
            "input3" | "in3" => Some(Self::Input3),
            "input4" | "in4" => Some(Self::Input4),
            "input5" | "in5" => Some(Self::Input5),
            "input6" | "in6" => Some(Self::Input6),
            "level" | "lvl" => Some(Self::Level),
            "comp" | "compressor" => Some(Self::Compressor),
            "mic" | "micgain" | "mic_gain" => Some(Self::MicGain),
            "talk" => Some(Self::Talk),
            "mute" | "mute_output" => Some(Self::MuteOutput),
            "comp_on_off" => Some(Self::CompressorOnOff),
            _ => None,
        }
    }
}

impl std::fmt::Display for ParamId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.name())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn roundtrip_u8() {
        for &p in ParamId::ALL {
            assert_eq!(ParamId::from_u8(p as u8), Some(p));
        }
    }

    #[test]
    fn slider_classification() {
        assert!(ParamId::Input1.is_slider());
        assert!(ParamId::MicGain.is_slider());
        assert!(!ParamId::Talk.is_slider());
        assert!(!ParamId::MuteOutput.is_slider());
    }

    #[test]
    fn name_parse_roundtrip() {
        for &p in ParamId::ALL {
            // Not all params have CLI aliases, but name() should produce something parseable
            // for the ones that do
            if let Some(parsed) = ParamId::from_name(p.name()) {
                assert_eq!(parsed, p);
            }
        }
    }
}
