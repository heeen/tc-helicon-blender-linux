use std::sync::atomic::{AtomicU8, Ordering};
use std::sync::Arc;
use std::thread::JoinHandle;

/// Number of audio channels: 12 capture + 2 playback.
const NUM_CHANNELS: usize = 14;

/// Lock-free audio peak metering via PipeWire.
///
/// Spawns a dedicated thread running PipeWire's main loop, capturing audio
/// from the Blender's ALSA nodes and computing per-channel peak levels.
pub struct AudioMeter {
    peaks: Arc<[AtomicU8; NUM_CHANNELS]>,
    _thread: JoinHandle<()>,
}

impl AudioMeter {
    /// Read current peak for a channel (0–11 = capture, 12–13 = playback). Returns 0–255.
    pub fn peak(&self, channel: usize) -> u8 {
        if channel < NUM_CHANNELS {
            self.peaks[channel].load(Ordering::Relaxed)
        } else {
            0
        }
    }

    /// Stereo capture peak for an input (0–5). Returns max of L/R channels.
    pub fn input_peak(&self, input_idx: usize) -> u8 {
        let l = self.peak(input_idx * 2);
        let r = self.peak(input_idx * 2 + 1);
        l.max(r)
    }

    /// Stereo playback peak. Returns max of L/R.
    pub fn output_peak(&self) -> u8 {
        let l = self.peak(12);
        let r = self.peak(13);
        l.max(r)
    }
}

/// Block character for a peak level (0–255).
pub fn peak_char(level: u8) -> char {
    match level {
        0 => ' ',
        1..=31 => '▁',
        32..=63 => '▂',
        64..=95 => '▃',
        96..=127 => '▄',
        128..=159 => '▅',
        160..=191 => '▆',
        192..=223 => '▇',
        224..=255 => '█',
    }
}

#[cfg(feature = "audio-meter")]
mod pw_impl {
    use super::*;
    use pipewire as pw;
    use pw::properties::properties;
    use std::mem;

    impl AudioMeter {
        /// Try to connect to PipeWire and the Blender audio nodes.
        /// Returns None if PipeWire or device not available.
        pub fn try_start() -> Option<Self> {
            pw::init();

            let peaks: Arc<[AtomicU8; NUM_CHANNELS]> =
                Arc::new(std::array::from_fn(|_| AtomicU8::new(0)));
            let peaks_clone = peaks.clone();

            let thread = std::thread::Builder::new()
                .name("pw-meter".into())
                .spawn(move || {
                    if let Err(e) = run_pw_loop(peaks_clone) {
                        log::warn!("PipeWire meter thread exited: {e}");
                    }
                })
                .ok()?;

            // Give PipeWire a moment to connect
            std::thread::sleep(std::time::Duration::from_millis(200));

            Some(AudioMeter {
                peaks,
                _thread: thread,
            })
        }
    }

    struct CaptureData {
        peaks: Arc<[AtomicU8; NUM_CHANNELS]>,
        channel_offset: usize,
        n_channels: u32,
    }

    fn run_pw_loop(
        peaks: Arc<[AtomicU8; NUM_CHANNELS]>,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mainloop = pw::main_loop::MainLoopRc::new(None)?;
        let context = pw::context::ContextRc::new(&mainloop, None)?;
        let core = context.connect_rc(None)?;

        // Capture stream (12 channels from Blender multichannel input)
        let capture_props = properties! {
            *pw::keys::MEDIA_TYPE => "Audio",
            *pw::keys::MEDIA_CATEGORY => "Monitor",
            *pw::keys::MEDIA_ROLE => "DSP",
            "target.object" => "alsa_input.usb-TC-Helicon_Blender_10400041-00.multichannel-input",
        };
        let capture_stream =
            pw::stream::StreamBox::new(&core, "blender-capture-meter", capture_props)?;
        let capture_data = CaptureData {
            peaks: peaks.clone(),
            channel_offset: 0,
            n_channels: 12,
        };
        let _capture_listener = capture_stream
            .add_local_listener_with_user_data(capture_data)
            .process(process_callback)
            .register()?;
        connect_stream(&capture_stream, 12)?;

        // Playback stream (2 channels from Blender output)
        let playback_props = properties! {
            *pw::keys::MEDIA_TYPE => "Audio",
            *pw::keys::MEDIA_CATEGORY => "Monitor",
            *pw::keys::MEDIA_ROLE => "DSP",
            "target.object" => "alsa_output.usb-TC-Helicon_Blender_10400041-00.analog-stereo",
        };
        let playback_stream =
            pw::stream::StreamBox::new(&core, "blender-playback-meter", playback_props)?;
        let playback_data = CaptureData {
            peaks,
            channel_offset: 12,
            n_channels: 2,
        };
        let _playback_listener = playback_stream
            .add_local_listener_with_user_data(playback_data)
            .process(process_callback)
            .register()?;
        connect_stream(&playback_stream, 2)?;

        mainloop.run();

        Ok(())
    }

    fn process_callback(stream: &pw::stream::Stream, data: &mut CaptureData) {
        let Some(mut buffer) = stream.dequeue_buffer() else {
            return;
        };
        let datas = buffer.datas_mut();
        if datas.is_empty() {
            return;
        }

        // Interleaved F32LE in the first data block
        let d = &mut datas[0];
        let chunk_size = d.chunk().size() as usize;
        if chunk_size == 0 {
            return;
        }
        let Some(slice) = d.data() else { return };
        let n_channels = data.n_channels as usize;
        let n_samples = chunk_size / mem::size_of::<f32>();
        let samples: &[f32] = bytemuck::cast_slice(&slice[..n_samples * mem::size_of::<f32>()]);

        for ch in 0..n_channels {
            if ch + data.channel_offset >= NUM_CHANNELS {
                break;
            }
            let mut peak: f32 = 0.0;
            let mut i = ch;
            while i < samples.len() {
                let v = samples[i].abs();
                if v > peak {
                    peak = v;
                }
                i += n_channels;
            }
            let scaled = (peak.min(1.0) * 255.0) as u8;
            data.peaks[ch + data.channel_offset].store(scaled, Ordering::Relaxed);
        }
    }

    fn connect_stream(
        stream: &pw::stream::StreamBox<'_>,
        channels: u32,
    ) -> Result<(), Box<dyn std::error::Error>> {
        use libspa::param::audio::{AudioFormat, AudioInfoRaw};
        use libspa::pod::Pod;
        use pw::stream::StreamFlags;

        let mut audio_info = AudioInfoRaw::new();
        audio_info.set_format(AudioFormat::F32LE);
        audio_info.set_rate(48000);
        audio_info.set_channels(channels);

        let values: Vec<u8> = libspa::pod::serialize::PodSerializer::serialize(
            std::io::Cursor::new(Vec::new()),
            &libspa::pod::Value::Object(libspa::pod::Object {
                type_: libspa::sys::SPA_TYPE_OBJECT_Format,
                id: libspa::sys::SPA_PARAM_EnumFormat,
                properties: audio_info.into(),
            }),
        )
        .map_err(|e| format!("pod serialize: {e:?}"))?
        .0
        .into_inner();

        let mut params = [Pod::from_bytes(&values).ok_or("bad pod")?];

        stream.connect(
            libspa::utils::Direction::Input,
            None,
            StreamFlags::AUTOCONNECT | StreamFlags::MAP_BUFFERS,
            &mut params,
        )?;

        Ok(())
    }
}

#[cfg(not(feature = "audio-meter"))]
impl AudioMeter {
    /// Stub when compiled without audio-meter feature.
    pub fn try_start() -> Option<Self> {
        None
    }
}
