use crate::param::ParamId;
use crate::tuple::Tuple;

/// Number of output buses (A, B, C, D).
pub const NUM_BUSES: usize = 4;
/// Number of input channels.
pub const NUM_INPUTS: usize = 6;

/// Canonical mixer state, matching the Blender's BLE parameter model.
///
/// Slider params (inputs, level, compressor, mic_gain) have 4 values — one per output bus.
/// Toggle/status params have a single value.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MixerState {
    /// Input levels [channel][bus]. 6 inputs x 4 buses.
    pub inputs: [[u8; NUM_BUSES]; NUM_INPUTS],
    /// Master output level per bus.
    pub level: [u8; NUM_BUSES],
    /// Compressor threshold per bus.
    pub compressor: [u8; NUM_BUSES],
    /// Mic gain per bus.
    pub mic_gain: [u8; NUM_BUSES],
    /// Talkback on/off.
    pub talk: bool,
    /// Mute output bitmap (bit3=A, bit2=B, bit1=C, bit0=D).
    pub mute: u8,
    /// Compressor enable bitmap (bit3=A, bit2=B, bit1=C, bit0=D).
    pub comp_on_off: u8,
    /// Input jack sense bitmap (bit N = input N+1 plugged). From BlenderState sub_param.
    pub input_jack_sense: u8,
    /// Output jack sense bitmap (bits 0-3 = outputs A-D, bit 4 = SD card). From BlenderState value.
    pub output_jack_sense: u8,
    /// Firmware version byte.
    pub version: u8,
    /// Whether BLE connection is active.
    pub connected: bool,
}

impl Default for MixerState {
    fn default() -> Self {
        let input_default = ParamId::Input1.default_value();
        let level_default = ParamId::Level.default_value();
        let comp_default = ParamId::Compressor.default_value();
        let mic_default = ParamId::MicGain.default_value();

        Self {
            inputs: [[input_default; NUM_BUSES]; NUM_INPUTS],
            level: [level_default; NUM_BUSES],
            compressor: [comp_default; NUM_BUSES],
            mic_gain: [mic_default; NUM_BUSES],
            talk: false,
            mute: 0,
            comp_on_off: 0,
            input_jack_sense: 0,
            output_jack_sense: 0,
            version: 0,
            connected: false,
        }
    }
}

impl MixerState {
    /// Get the value for a (param_id, sub_param) pair.
    pub fn get(&self, param: ParamId, sub: u8) -> u8 {
        let s = sub as usize;
        match param {
            ParamId::Input1 => self.inputs[0].get(s).copied().unwrap_or(0),
            ParamId::Input2 => self.inputs[1].get(s).copied().unwrap_or(0),
            ParamId::Input3 => self.inputs[2].get(s).copied().unwrap_or(0),
            ParamId::Input4 => self.inputs[3].get(s).copied().unwrap_or(0),
            ParamId::Input5 => self.inputs[4].get(s).copied().unwrap_or(0),
            ParamId::Input6 => self.inputs[5].get(s).copied().unwrap_or(0),
            ParamId::Level => self.level.get(s).copied().unwrap_or(0),
            ParamId::Compressor => self.compressor.get(s).copied().unwrap_or(0),
            ParamId::MicGain => self.mic_gain.get(s).copied().unwrap_or(0),
            ParamId::Talk => u8::from(self.talk),
            ParamId::MuteOutput => self.mute,
            ParamId::CompressorOnOff => self.comp_on_off,
            ParamId::BlenderState => self.output_jack_sense,
            ParamId::Version => self.version,
            _ => 0,
        }
    }

    /// Set the value for a (param_id, sub_param) pair.
    pub fn set(&mut self, param: ParamId, sub: u8, value: u8) {
        let s = sub as usize;
        match param {
            ParamId::Input1 => {
                if s < NUM_BUSES {
                    self.inputs[0][s] = value;
                }
            }
            ParamId::Input2 => {
                if s < NUM_BUSES {
                    self.inputs[1][s] = value;
                }
            }
            ParamId::Input3 => {
                if s < NUM_BUSES {
                    self.inputs[2][s] = value;
                }
            }
            ParamId::Input4 => {
                if s < NUM_BUSES {
                    self.inputs[3][s] = value;
                }
            }
            ParamId::Input5 => {
                if s < NUM_BUSES {
                    self.inputs[4][s] = value;
                }
            }
            ParamId::Input6 => {
                if s < NUM_BUSES {
                    self.inputs[5][s] = value;
                }
            }
            ParamId::Level => {
                if s < NUM_BUSES {
                    self.level[s] = value;
                }
            }
            ParamId::Compressor => {
                if s < NUM_BUSES {
                    self.compressor[s] = value;
                }
            }
            ParamId::MicGain => {
                if s < NUM_BUSES {
                    self.mic_gain[s] = value;
                }
            }
            ParamId::Talk => self.talk = value != 0,
            ParamId::MuteOutput => self.mute = value,
            ParamId::CompressorOnOff => self.comp_on_off = value,
            ParamId::BlenderState => {
                self.input_jack_sense = sub;
                self.output_jack_sense = value;
            }
            ParamId::Version => self.version = value,
            _ => {}
        }
    }

    /// Apply a slice of tuples to update state.
    pub fn apply_tuples(&mut self, tuples: &[Tuple]) {
        for t in tuples {
            if let Some(param) = ParamId::from_u8(t.param_id) {
                self.set(param, t.sub_param, t.value);
            }
        }
    }

    /// Compute the tuples needed to go from `self` to `other`.
    pub fn diff(&self, other: &MixerState) -> Vec<Tuple> {
        let mut tuples = Vec::new();
        for &param in ParamId::ALL {
            if param.is_slider() {
                for sub in 0..NUM_BUSES as u8 {
                    let old = self.get(param, sub);
                    let new = other.get(param, sub);
                    if old != new {
                        tuples.push(Tuple::new(param as u8, sub, new));
                    }
                }
            } else if param == ParamId::BlenderState {
                if self.input_jack_sense != other.input_jack_sense
                    || self.output_jack_sense != other.output_jack_sense
                {
                    tuples.push(Tuple::new(
                        param as u8,
                        other.input_jack_sense,
                        other.output_jack_sense,
                    ));
                }
            } else {
                let old = self.get(param, 0);
                let new = other.get(param, 0);
                if old != new {
                    tuples.push(Tuple::new(param as u8, 0, new));
                }
            }
        }
        tuples
    }

    /// Generate tuples representing the full state (for state dump).
    pub fn to_tuples(&self) -> Vec<Tuple> {
        let mut tuples = Vec::new();
        for &param in ParamId::SLIDERS {
            for sub in 0..NUM_BUSES as u8 {
                tuples.push(Tuple::new(param as u8, sub, self.get(param, sub)));
            }
        }
        for &param in &[
            ParamId::Talk,
            ParamId::MuteOutput,
            ParamId::CompressorOnOff,
            ParamId::Version,
        ] {
            tuples.push(Tuple::new(param as u8, 0, self.get(param, 0)));
        }
        // BlenderState encodes both jack sense bitmaps
        tuples.push(Tuple::new(
            ParamId::BlenderState as u8,
            self.input_jack_sense,
            self.output_jack_sense,
        ));
        tuples
    }

    /// Returns true if input channel (0-based, 0–5) has a cable plugged in.
    /// Bitmap is MSB-first: bit5=input1, bit0=input6.
    pub fn input_plugged(&self, input_idx: usize) -> bool {
        self.input_jack_sense & (1 << (5 - input_idx)) != 0
    }

    /// Returns true if output bus (0=A, 1=B, 2=C, 3=D) has a cable plugged in.
    /// Bitmap is MSB-first: bit3=A, bit0=D (same layout as mute/comp).
    pub fn output_plugged(&self, bus_idx: usize) -> bool {
        self.output_jack_sense & (1 << (3 - bus_idx)) != 0
    }

    /// Returns true if the given bus (0=A..3=D) is muted. Mute bitmap: bit3=A, bit0=D.
    pub fn bus_muted(&self, bus: usize) -> bool {
        self.mute & (1 << (3 - bus)) != 0
    }

    /// Returns true if the compressor is enabled for the given bus. Same bit layout as mute.
    pub fn bus_comp_enabled(&self, bus: usize) -> bool {
        self.comp_on_off & (1 << (3 - bus)) != 0
    }

    /// Get the slider array for an input param (0-based channel index).
    fn input_idx(param: ParamId) -> Option<usize> {
        match param {
            ParamId::Input1 => Some(0),
            ParamId::Input2 => Some(1),
            ParamId::Input3 => Some(2),
            ParamId::Input4 => Some(3),
            ParamId::Input5 => Some(4),
            ParamId::Input6 => Some(5),
            _ => None,
        }
    }

    /// Get the slider values for a param (4 buses). Returns None for non-slider params.
    pub fn slider_values(&self, param: ParamId) -> Option<[u8; NUM_BUSES]> {
        if let Some(idx) = Self::input_idx(param) {
            Some(self.inputs[idx])
        } else {
            match param {
                ParamId::Level => Some(self.level),
                ParamId::Compressor => Some(self.compressor),
                ParamId::MicGain => Some(self.mic_gain),
                _ => None,
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn default_values() {
        let s = MixerState::default();
        assert_eq!(s.get(ParamId::Input1, 0), 0xCD);
        assert_eq!(s.get(ParamId::Level, 2), 82);
        assert_eq!(s.get(ParamId::Compressor, 3), 0x83);
        assert_eq!(s.get(ParamId::Talk, 0), 0);
    }

    #[test]
    fn set_and_get() {
        let mut s = MixerState::default();
        s.set(ParamId::Input1, 0, 200);
        assert_eq!(s.get(ParamId::Input1, 0), 200);
        assert_eq!(s.get(ParamId::Input1, 1), 0xCD); // other bus unchanged
    }

    #[test]
    fn apply_tuples() {
        let mut s = MixerState::default();
        s.apply_tuples(&[
            Tuple::new(0, 0, 100),
            Tuple::new(0, 1, 101),
            Tuple::new(9, 0, 1), // talk on
        ]);
        assert_eq!(s.get(ParamId::Input1, 0), 100);
        assert_eq!(s.get(ParamId::Input1, 1), 101);
        assert!(s.talk);
    }

    #[test]
    fn diff_detects_changes() {
        let s1 = MixerState::default();
        let mut s2 = s1.clone();
        s2.set(ParamId::Input1, 0, 200);
        s2.talk = true;
        let diff = s1.diff(&s2);
        assert_eq!(diff.len(), 2);
        assert!(diff.contains(&Tuple::new(0, 0, 200)));
        assert!(diff.contains(&Tuple::new(9, 0, 1)));
    }

    #[test]
    fn roundtrip_tuples() {
        let mut s = MixerState::default();
        s.input_jack_sense = 0b00101011;
        s.output_jack_sense = 0b00010101;
        let tuples = s.to_tuples();
        let mut s2 = MixerState {
            inputs: [[0; NUM_BUSES]; NUM_INPUTS],
            level: [0; NUM_BUSES],
            compressor: [0; NUM_BUSES],
            mic_gain: [0; NUM_BUSES],
            talk: false,
            mute: 0,
            comp_on_off: 0,
            input_jack_sense: 0,
            output_jack_sense: 0,
            version: 0,
            connected: false,
        };
        s2.apply_tuples(&tuples);
        assert_eq!(s.inputs, s2.inputs);
        assert_eq!(s.level, s2.level);
        assert_eq!(s.compressor, s2.compressor);
        assert_eq!(s.input_jack_sense, s2.input_jack_sense);
        assert_eq!(s.output_jack_sense, s2.output_jack_sense);
    }

    #[test]
    fn jack_sense_helpers() {
        let mut s = MixerState::default();
        s.input_jack_sense = 0b00_101001; // inputs 1, 3, 6 plugged (bit5=in1, bit0=in6)
        s.output_jack_sense = 0b00_001010; // outputs A, C plugged (bit3=A, bit0=D)

        assert!(s.input_plugged(0));  // input 1
        assert!(!s.input_plugged(1)); // input 2
        assert!(s.input_plugged(2));  // input 3
        assert!(!s.input_plugged(3));
        assert!(!s.input_plugged(4));
        assert!(s.input_plugged(5));  // input 6

        assert!(s.output_plugged(0));  // A (bit 3)
        assert!(!s.output_plugged(1)); // B (bit 2)
        assert!(s.output_plugged(2));  // C (bit 1)
        assert!(!s.output_plugged(3)); // D (bit 0)
    }

    #[test]
    fn bus_mute_comp_helpers() {
        let mut s = MixerState::default();
        s.mute = 0b1010; // A and C muted
        s.comp_on_off = 0b0101; // B and D comp enabled

        assert!(s.bus_muted(0));   // A
        assert!(!s.bus_muted(1));  // B
        assert!(s.bus_muted(2));   // C
        assert!(!s.bus_muted(3));  // D

        assert!(!s.bus_comp_enabled(0)); // A
        assert!(s.bus_comp_enabled(1));  // B
        assert!(!s.bus_comp_enabled(2)); // C
        assert!(s.bus_comp_enabled(3));  // D
    }

    #[test]
    fn blender_state_set_stores_both() {
        let mut s = MixerState::default();
        s.set(ParamId::BlenderState, 0b111111, 0b10101);
        assert_eq!(s.input_jack_sense, 0b111111);
        assert_eq!(s.output_jack_sense, 0b10101);
    }
}
