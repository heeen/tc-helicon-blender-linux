use std::sync::Arc;

use anyhow::Result;
use midir::{MidiInput, MidiInputConnection, MidiOutput};
use tokio::sync::{mpsc, watch};

use blender_proto::param::ParamId;
use blender_proto::state::MixerState;
use blender_proto::tuple::Tuple;

pub mod flash;

/// MIDI CC numbers matching the firmware's midi_cc_handler in handlers.c.
const CC_MASTER_LEVEL: u8 = 7;
const CC_INPUT1: u8 = 16;
const CC_MIC_GAIN: u8 = 22;
const CC_COMPRESSOR: u8 = 23;

fn ble_to_midi(val: u8) -> u8 {
    (val >> 1).min(127)
}

fn midi_to_ble(val: u8) -> u8 {
    val.saturating_mul(2) | (val >> 6)
}

fn param_to_cc(param: ParamId, bus: u8) -> Option<(u8, u8)> {
    if bus >= 4 {
        return None;
    }
    let cc = match param {
        ParamId::Input1 => CC_INPUT1,
        ParamId::Input2 => CC_INPUT1 + 1,
        ParamId::Input3 => CC_INPUT1 + 2,
        ParamId::Input4 => CC_INPUT1 + 3,
        ParamId::Input5 => CC_INPUT1 + 4,
        ParamId::Input6 => CC_INPUT1 + 5,
        ParamId::Level => CC_MASTER_LEVEL,
        ParamId::Compressor => CC_COMPRESSOR,
        ParamId::MicGain => CC_MIC_GAIN,
        _ => return None,
    };
    Some((cc, bus))
}

fn cc_to_param(cc: u8) -> Option<ParamId> {
    match cc {
        CC_MASTER_LEVEL => Some(ParamId::Level),
        16 => Some(ParamId::Input1),
        17 => Some(ParamId::Input2),
        18 => Some(ParamId::Input3),
        19 => Some(ParamId::Input4),
        20 => Some(ParamId::Input5),
        21 => Some(ParamId::Input6),
        CC_MIC_GAIN => Some(ParamId::MicGain),
        CC_COMPRESSOR => Some(ParamId::Compressor),
        _ => None,
    }
}

fn find_blender_port<T: midir::MidiIO>(midi_io: &T) -> Option<T::Port> {
    for port in midi_io.ports() {
        if let Ok(name) = midi_io.port_name(&port) {
            if name.to_lowercase().contains("blender") {
                return Some(port);
            }
        }
    }
    None
}

type SharedWatchTx = Arc<std::sync::Mutex<watch::Sender<MixerState>>>;

/// USB MIDI client for the Blender.
///
/// Bidirectional native USB MIDI:
///   RX (device→host): ALSA MIDI input via `midir` (bulk EP 0x82 IN)
///   TX (host→device): ALSA MIDI output via `midir` (bulk EP 0x03 OUT)
///
/// The firmware patches the MIDI callback at boot and arms EP 0x03 from
/// the main-loop hook — no DCP initialization needed.
pub struct MidiClient {
    state_rx: watch::Receiver<MixerState>,
    cmd_tx: mpsc::Sender<Vec<Tuple>>,
    _midi_in: MidiInputConnection<()>,
}

impl MidiClient {
    pub fn connect() -> Result<Self> {
        // Open MIDI input (device→host, EP 0x82)
        let midi_in = MidiInput::new("blender-ctl-in")
            .map_err(|e| anyhow::anyhow!("MIDI input init: {e}"))?;
        let in_port = find_blender_port(&midi_in)
            .ok_or_else(|| anyhow::anyhow!("No Blender MIDI input port found"))?;
        let in_name = midi_in.port_name(&in_port).unwrap_or_default();
        log::info!("MIDI IN: {in_name}");

        // Open MIDI output (host→device, EP 0x03)
        let midi_out = MidiOutput::new("blender-ctl-out")
            .map_err(|e| anyhow::anyhow!("MIDI output init: {e}"))?;
        let out_port = find_blender_port(&midi_out)
            .ok_or_else(|| anyhow::anyhow!("No Blender MIDI output port found"))?;
        let out_name = midi_out.port_name(&out_port).unwrap_or_default();
        log::info!("MIDI OUT: {out_name}");

        let midi_out_conn = midi_out
            .connect(&out_port, "blender-out")
            .map_err(|e| anyhow::anyhow!("Connect MIDI output: {e}"))?;

        let mut initial_state = MixerState::default();
        initial_state.connected = true;
        initial_state.input_jack_sense = 0b111111;
        initial_state.output_jack_sense = 0b1111;

        let (state_tx, state_rx) = watch::channel(initial_state.clone());
        let state_tx: SharedWatchTx = Arc::new(std::sync::Mutex::new(state_tx));
        let (cmd_tx, mut cmd_rx) = mpsc::channel::<Vec<Tuple>>(64);
        let state = Arc::new(std::sync::Mutex::new(initial_state));

        // MIDI RX callback (midir thread — device→host)
        let state_for_rx = Arc::clone(&state);
        let state_tx_for_rx = Arc::clone(&state_tx);
        let midi_in_conn = midi_in
            .connect(
                &in_port,
                "blender-in",
                move |_timestamp, message: &[u8], _| {
                    if message.len() < 3 || (message[0] & 0xF0) != 0xB0 {
                        return;
                    }
                    let channel = message[0] & 0x0F;
                    if channel >= 4 {
                        return;
                    }
                    if let Some(param) = cc_to_param(message[1]) {
                        let ble_val = midi_to_ble(message[2]);
                        let updated = {
                            let mut s = state_for_rx.lock().unwrap();
                            s.set(param, channel, ble_val);
                            s.clone()
                        };
                        let _ = state_tx_for_rx.lock().unwrap().send(updated);
                    }
                },
                (),
            )
            .map_err(|e| anyhow::anyhow!("Connect MIDI input: {e}"))?;

        // Command writer — sends CCs via native MIDI output (host→device)
        let state_for_tx = Arc::clone(&state);
        let state_tx_for_tx = Arc::clone(&state_tx);
        let midi_out_conn = Arc::new(std::sync::Mutex::new(midi_out_conn));
        tokio::spawn(async move {
            while let Some(tuples) = cmd_rx.recv().await {
                let mut messages: Vec<[u8; 3]> = Vec::new();
                let mut state_updates: Vec<(ParamId, u8, u8)> = Vec::new();

                for t in &tuples {
                    let Some(param) = ParamId::from_u8(t.param_id) else { continue };
                    let Some((cc_num, channel)) = param_to_cc(param, t.sub_param) else { continue };
                    let cc_val = ble_to_midi(t.value);
                    messages.push([0xB0 | channel, cc_num, cc_val]);
                    state_updates.push((param, t.sub_param, t.value));
                }

                if !messages.is_empty() {
                    let conn = Arc::clone(&midi_out_conn);
                    let result = tokio::task::spawn_blocking(move || {
                        let mut out = conn.lock().unwrap();
                        for msg in &messages {
                            if let Err(e) = out.send(msg) {
                                log::warn!("MIDI send: {e}");
                                return Err(anyhow::anyhow!("MIDI send: {e}"));
                            }
                        }
                        Ok(())
                    })
                    .await;

                    if let Err(e) = result {
                        log::warn!("MIDI send task: {e}");
                    }
                }

                if !state_updates.is_empty() {
                    let updated = {
                        let mut s = state_for_tx.lock().unwrap();
                        for (param, sub, val) in &state_updates {
                            s.set(*param, *sub, *val);
                        }
                        s.clone()
                    };
                    let _ = state_tx_for_tx.lock().unwrap().send(updated);
                }
            }
        });

        println!("Connected to Blender MIDI: {in_name} / {out_name}");

        Ok(Self {
            state_rx,
            cmd_tx,
            _midi_in: midi_in_conn,
        })
    }

    pub fn state_watch(&self) -> watch::Receiver<MixerState> {
        self.state_rx.clone()
    }

    pub fn command_sender(&self) -> mpsc::Sender<Vec<Tuple>> {
        self.cmd_tx.clone()
    }
}
