use std::sync::Arc;

use anyhow::{Context, Result};
use btleplug::api::{
    Central, Characteristic, Manager as _, Peripheral as _, ScanFilter, WriteType,
};
use btleplug::platform::{Adapter, Manager, Peripheral};
use futures::StreamExt;
use tokio::sync::{mpsc, watch, Mutex};
use uuid::Uuid;

use blender_proto::param::ParamId;
use blender_proto::state::MixerState;
use blender_proto::tuple::Tuple;

/// ParameterService UUID from BluetoothConstants.java.
pub const PARAM_SERVICE_UUID: Uuid =
    Uuid::from_u128(0xe71ee188_279f_4ed6_8055_12d77bfd900c);
/// ParameterCharacteristic UUID.
pub const PARAM_CHAR_UUID: Uuid =
    Uuid::from_u128(0x50e2d021_f23b_46fb_b7e6_fbe12301276a);

/// BLE central (GATT client) for the Blender.
///
/// Scans for the Blender advertising ParameterService, connects as a GATT
/// client, subscribes to notifications, and handles the handshake.
pub struct BleClient {
    state_rx: watch::Receiver<MixerState>,
    cmd_tx: mpsc::Sender<Vec<Tuple>>,
    // Keep the peripheral alive so the connection isn't dropped
    _peripheral: Peripheral,
}

impl BleClient {
    /// Scan for the Blender, connect, and start the bidirectional tuple exchange.
    pub async fn connect() -> Result<Self> {
        let manager = Manager::new().await.context("BLE manager")?;
        let adapters = manager.adapters().await.context("BLE adapters")?;
        let adapter = adapters
            .into_iter()
            .next()
            .ok_or_else(|| anyhow::anyhow!("No BLE adapter found"))?;

        log::info!("Scanning for Blender (ParameterService UUID)...");
        println!("Scanning for Blender... Press BT/PAIR on the device.");

        let peripheral = Self::scan_for_blender(&adapter).await?;
        let props = peripheral.properties().await?.unwrap_or_default();
        let name = props.local_name.unwrap_or_default();
        let addr = props.address;
        log::info!("Found: {name} ({addr})");
        println!("Found: {name} ({addr})");

        Self::setup_connection(peripheral).await
    }

    /// Common setup after we have a Peripheral reference.
    async fn setup_connection(peripheral: Peripheral) -> Result<Self> {
        if !peripheral.is_connected().await? {
            log::info!("Connecting...");
            peripheral.connect().await.context("BLE connect")?;
        }
        log::info!("Connected");

        peripheral
            .discover_services()
            .await
            .context("Discover services")?;

        let char = Self::find_param_characteristic(&peripheral)?;
        log::info!("Found ParameterCharacteristic");

        let (state_tx, state_rx) = watch::channel(MixerState::default());
        let (cmd_tx, mut cmd_rx) = mpsc::channel::<Vec<Tuple>>(64);
        let state = Arc::new(Mutex::new(MixerState::default()));

        // Subscribe to notifications
        peripheral
            .subscribe(&char)
            .await
            .context("Subscribe to notifications")?;
        log::info!("Subscribed to notifications");

        let mut notification_stream = peripheral
            .notifications()
            .await
            .context("Notification stream")?;

        // Spawn notification reader
        let state_for_rx = Arc::clone(&state);
        let char_for_handshake = char.clone();
        let peripheral_for_handshake = peripheral.clone();
        tokio::spawn(async move {
            log::info!("Notification reader task started");
            let mut handshake_sent = false;
            while let Some(notification) = notification_stream.next().await {
                let data = notification.value;
                let tuples = Tuple::decode(&data);
                if tuples.is_empty() {
                    continue;
                }

                let has_is_blender = tuples
                    .iter()
                    .any(|t| t.param_id == ParamId::IsBlender as u8);

                // Log decoded tuples
                for t in &tuples {
                    if let Some(param) = ParamId::from_u8(t.param_id) {
                        log::trace!(
                            "BLE RX: {}[{}] = {} (0x{:02X})",
                            param.name(),
                            t.sub_param,
                            t.value,
                            t.value
                        );
                    } else {
                        log::trace!(
                            "BLE RX: unknown({})[{}] = {} (0x{:02X})",
                            t.param_id,
                            t.sub_param,
                            t.value,
                            t.value
                        );
                    }
                }

                {
                    let mut s = state_for_rx.lock().await;
                    s.apply_tuples(&tuples);
                    if has_is_blender {
                        s.connected = true;
                    }
                    let _ = state_tx.send(s.clone());
                }

                if has_is_blender && !handshake_sent {
                    handshake_sent = true;
                    log::info!("Blender identified itself — sending handshake");
                    let handshake = Tuple::encode(&[
                        Tuple::new(ParamId::HasBlender as u8, 0, 1),
                        Tuple::new(ParamId::RequestState as u8, 0, 0),
                    ]);
                    if let Err(e) = peripheral_for_handshake
                        .write(&char_for_handshake, &handshake, WriteType::WithResponse)
                        .await
                    {
                        log::warn!("Failed to send handshake: {e}");
                    } else {
                        log::info!("Handshake sent: hasBlender(1) + requestBlenderState(0)");
                    }
                }
            }
            log::warn!("Notification stream ended — Blender disconnected");
        });

        // Send proactive handshake — handles reconnection when BlueZ caches
        // the connection and the Blender doesn't re-send isBlender.
        tokio::time::sleep(std::time::Duration::from_millis(500)).await;
        let handshake = Tuple::encode(&[
            Tuple::new(ParamId::HasBlender as u8, 0, 1),
            Tuple::new(ParamId::RequestState as u8, 0, 0),
        ]);
        peripheral
            .write(&char, &handshake, WriteType::WithResponse)
            .await
            .context("Send handshake")?;
        log::info!("Sent proactive handshake: hasBlender(1) + requestBlenderState(0)");

        // Spawn command writer
        let peripheral_for_tx = peripheral.clone();
        let char_for_tx = char.clone();
        tokio::spawn(async move {
            while let Some(tuples) = cmd_rx.recv().await {
                let data = Tuple::encode(&tuples);
                if let Err(e) = peripheral_for_tx
                    .write(&char_for_tx, &data, WriteType::WithResponse)
                    .await
                {
                    log::warn!("Failed to write command: {e}");
                } else {
                    log::trace!("BLE TX: {} tuples", tuples.len());
                }
            }
        });

        Ok(Self {
            state_rx,
            cmd_tx,
            _peripheral: peripheral,
        })
    }

    /// Scan until we find a device advertising ParameterService.
    async fn scan_for_blender(adapter: &Adapter) -> Result<Peripheral> {
        adapter
            .start_scan(ScanFilter {
                services: vec![PARAM_SERVICE_UUID],
            })
            .await
            .context("Start scan")?;

        // Poll for discovered peripherals
        for _ in 0..100 {
            tokio::time::sleep(std::time::Duration::from_millis(200)).await;

            let peripherals = adapter.peripherals().await?;
            for p in peripherals {
                if let Some(props) = p.properties().await? {
                    if props.services.contains(&PARAM_SERVICE_UUID) {
                        adapter.stop_scan().await.ok();
                        return Ok(p);
                    }
                    if let Some(ref name) = props.local_name {
                        if name.to_lowercase().contains("blender") {
                            adapter.stop_scan().await.ok();
                            return Ok(p);
                        }
                    }
                }
            }
        }

        adapter.stop_scan().await.ok();
        anyhow::bail!("Timed out scanning for Blender (20s)")
    }

    /// Find the ParameterCharacteristic on the connected peripheral.
    fn find_param_characteristic(peripheral: &Peripheral) -> Result<Characteristic> {
        for char in peripheral.characteristics() {
            if char.uuid == PARAM_CHAR_UUID {
                return Ok(char);
            }
        }
        anyhow::bail!("ParameterCharacteristic not found on device")
    }

    /// Get a receiver for the current mixer state.
    pub fn state_watch(&self) -> watch::Receiver<MixerState> {
        self.state_rx.clone()
    }

    /// Get a sender for outbound commands.
    pub fn command_sender(&self) -> mpsc::Sender<Vec<Tuple>> {
        self.cmd_tx.clone()
    }

    /// Send tuples to the Blender.
    pub async fn send(&self, tuples: Vec<Tuple>) -> Result<()> {
        self.cmd_tx
            .send(tuples)
            .await
            .map_err(|_| anyhow::anyhow!("Command channel closed"))
    }

    /// Wait until the handshake completes (isBlender received + state dump).
    pub async fn wait_connected(&mut self) {
        loop {
            if self.state_rx.borrow().connected {
                return;
            }
            if self.state_rx.changed().await.is_err() {
                return;
            }
        }
    }
}
