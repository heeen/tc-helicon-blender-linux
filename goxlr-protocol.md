# GoXLR / TC Helicon Blender Protocol Reference

Reverse engineered from `GoXLR App.exe` v1.6.4.014 (Windows, Ghidra) and the
TC Helicon Blender Android app v1.2 (jadx).

Devices sharing this protocol:
- GoXLR (`VID 0x1220 PID 0x8FE0`)
- TC Helicon Blender (`VID 0x1220 PID 0x8FE1`)
- GoXLR Mini (`VID 0x1220 PID 0x8FE4`)

All three use a TCAT DICE DSP/audio platform with Thesycon TUSBAudio USB driver.

---

## 1. USB Boot Initialization

The device does not stream audio until the host sends a specific init sequence.
Without it, ALSA/PulseAudio sees the card but `arecord` gets I/O errors.

### Sequence

| Step | Direction | Type | bRequest | wValue | wIndex | Data | Purpose |
|------|-----------|------|----------|--------|--------|------|---------|
| 1 | IN  | Vendor/Interface | 0x00 | 0 | 0 | read 24 bytes | Clear/activate vendor interface |
| 2 | OUT | Class/Interface  | 0x01 | 0x0100 | 0x2900 | `80 bb 00 00` | SET_CUR sample rate 48kHz |

Step 1 response is discarded. Step 2 sets the clock source (entity 41, CS=1).
After ~1s the device enumerates PCM endpoints.

This is implemented as `snd_usb_tc_helicon_boot_quirk()` in the Linux
snd-usb-audio driver (must go in `snd_usb_apply_boot_quirk_once`, not the
per-interface variant).

---

## 2. DCP (DICE Control Protocol) over USB

### Transport

DCP packets ride on USB control transfers:

| Field | Value |
|-------|-------|
| bmRequestType | Vendor, Interface recipient |
| bRequest | 1 |
| wValue | 0 |
| wIndex | 0 (Linux direct) or `audio_ctrl_interface` (Windows TUSBAUDIO, e.g. 0x2900) |
| Data | DCP packet (header + payload) |

Write = `USB_DIR_OUT | USB_TYPE_VENDOR | USB_RECIP_INTERFACE`
Read  = `USB_DIR_IN  | USB_TYPE_VENDOR | USB_RECIP_INTERFACE`

Protocol: send a write transfer with the DCP packet, then a read transfer to
get the response. Both use the same bRequest/wValue/wIndex.

### Packet Format

Every DCP packet has a 16-byte big-endian header:

```
Offset  Size  Field
0x00    4     word0: (category & 0xFFF) << 12 | (opcode & 0xFFF)
0x04    4     word1: (seqnum & 0xFFFF) << 16 | (data_size & 0xFFFF)
0x08    4     reserved (0)
0x0C    4     reserved (0)
0x10    N     payload (data_size bytes, big-endian u32 words)
```

- **category**: 12-bit command group
- **opcode**: 12-bit operation within group
- **seqnum**: 16-bit sequence counter (incremented per command, echoed in response)
- **data_size**: 16-bit payload length in bytes

Response echoes the same category/opcode/seqnum with response payload.

---

## 3. Standard DICE Commands (Categories 0-4)

### Category 0: System

| Opcode | Dir | Payload | Description |
|--------|-----|---------|-------------|
| 0x000 | W | 0 bytes | Flush / ping |
| 0x001 | R | 4 bytes in (cat_id:u32), 4 bytes out | Query category support |
| 0x002 | R | 0x70 bytes | System info |
| 0x018 | R | 0x18 bytes | Global data (magic: `0x04061973`, `0x18101966`) |

### Category 1: Peaks

| Opcode | Dir | Payload | Description |
|--------|-----|---------|-------------|
| 0x000 | R | varies | Peak metering capabilities |

### Category 2: Mixer

| Opcode | Dir | Payload | Description |
|--------|-----|---------|-------------|
| 0x000 | R | 8 bytes | Mixer capabilities |

### Category 3: Router

| Opcode | Dir | Payload | Description |
|--------|-----|---------|-------------|
| 0x000 | R | 0x0C bytes | Router capabilities |

### Category 4: NVM (Non-Volatile Memory)

| Opcode | Dir | Payload | Description |
|--------|-----|---------|-------------|
| 0x000 | R | 0x10 bytes | NVM capabilities |
| 0x001 | R | 0x18 bytes | Segment info (iterated) |
| 0x002 | W | varies | Erase segment (part 1) |
| 0x003 | W | varies | Erase segment (part 2) |
| 0x004 | W | varies | Write segment |
| 0x006 | R | varies | CRC32 check |

---

## 4. Vendor Commands (Categories 0x800+)

All vendor commands go through `goxlr_send_dcp_command()` which takes a
critical section lock and retries via `dcp_cmd_with_retry()`.

### 0x800: Read Device State

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | R | varies | Full device state dump (sent after profile load) |

### 0x801: Set Effect Parameters (bulk)

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | W | N×8 | Array of `(paramID:u32, value:u32)` pairs |

Bulk-sets DSP effect parameters from the dspTree.

### 0x802: Set Scribble Strip

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| strip_idx | W | varies | Scribble strip image/text for one strip |

GoXLR-specific (has LCD scribble strips). Blender does not have these.

### 0x803: Set Colors

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | W | 0x208 (520 bytes) | Full LED/button color map |

GoXLR-specific.

### 0x804: Set Routing Row

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| row_index | W | 0x1A (26 bytes) | Single routing matrix row: 13 × u16 channel levels |

Full matrix: 18 rows × 13 channels. This is the audio routing table that
maps inputs to outputs.

### 0x805: Set Fader Assignment

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| varies | W | varies | Which audio channel a physical fader controls |

### 0x806: Set Fader Position

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| (fader_idx << 4) \| chan | W | 1 byte | Fader position (0-255, from float 0.0-1.0 × 255) |

### 0x808: Set Button State

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | W | 0x18 (24 bytes) | Button state bitmap |

### 0x809: Set Per-Channel Animation

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| channel_id | W | 1 byte | 0 = off, 1 = on |

### 0x80A: Set Encoder Position

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| encoder_idx | W | 1 byte | Position value (4 encoders, idx 0-3) |

### 0x80B: Set DSP Parameters (key-value)

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | W | N×8 | Array of `(paramID:u32, value:u32)` pairs |

**Primary DSP control mechanism.** Used for:
- Mic type selection (`MIC_TYPE` paramID)
- Mic gain (`MIC_GAIN_DYNAMIC`, `MIC_GAIN_CONDENSER`, `MIC_GAIN_TRS`)
- All effect parameters from the dspTree

Parameter IDs are looked up by string name in a registry, then decremented by 1
before sending.

### 0x80C: Read Serial Number

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | R | 2 bytes | Device serial number (u16) |

### 0x80F: Read Firmware Version

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | R | 0x18 (24 bytes) | Firmware version info |

### 0x810: Poll Firmware Update Status

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 5 | R | 0x18 (24 bytes) | Update progress; status codes: 2=done, 3=abort |

### 0x811: Set LED State

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| fader_idx | W | 2 bytes | byte 0: mode (1=off, 3=on), byte 1: behavior (1=off, 2=gradient, 4=solid) |

Iterate fader_idx 0-3 to set all faders. GoXLR-specific.

### 0x814: Set Per-Channel Volume

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| channel_idx | W | 2 bytes | Volume level (u16), 4 channels (idx 0-3) |

### 0x816: Set Animation Command

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | W | 5 bytes | Animation control |

### 0x817: Unknown

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | W | 8 bytes | Unknown |

### 0x818: Unknown

| Opcode | Dir | Size | Description |
|--------|-----|------|-------------|
| 0 | W | 1 byte | Unknown |

---

## 5. DSP Parameter System (dspTree)

The GoXLR app uses a hierarchical parameter tree. Parameters are referenced by
string name in the UI/XML layer, mapped to numeric IDs at runtime.

### Known Parameter Names

| Name | Description |
|------|-------------|
| MIC_TYPE | Microphone type selection |
| MIC_GAIN_DYNAMIC | Dynamic mic gain |
| MIC_GAIN_CONDENSER | Condenser mic gain |
| MIC_GAIN_TRS | TRS/line mic gain |
| MIC_EQ | Microphone EQ |
| REVERB_TYPE | Reverb algorithm |
| REVERB_DECAY | Reverb decay time |
| REVERB_HIFACTOR | Reverb HF factor |
| REVERB_MODSPEED | Reverb modulation speed |
| REVERB_LOCOLOR | Reverb LF color |
| REVERB_OUT_LEVEL | Reverb output level |
| COMPRESSOR | Compressor parameters |

### Wire Format

Sent via commands 0x801 or 0x80B as big-endian pairs:
```
struct param_entry {
    u32 param_id;   // numeric ID (from name lookup, minus 1)
    u32 value;      // parameter value
};
```

Multiple entries batched in one command.

---

## 6. Firmware Update

### Infrastructure

The GoXLR desktop app handles firmware updates (not the Android BLE app).

**Download URLs:**
- Production: `https://www.tc-helicon.com/FirmwareAssets/GOXLR/`
- Beta/RC: `https://mediadl.musictribe.com/media/PLM/sftp/incoming/hybris/import/FirmwareAssets/GOXLR/LiveReleaseCandidate`
- Test: `.../LiveTestArea`
- Alpha: `.../AlphaTestLocation`

**Manifest:** `UpdateManifest_v3.xml`
**Binaries:** `GoXLR_firmware.bin`, `GoXLR_MINI_firmware.bin`

### Update Process

1. Download manifest + binary from URL
2. Validate firmware header (magic, product ID, manufacturer)
3. Load via `TUSBAUDIO_LoadFirmwareImageFromFile` or `TUSBAUDIO_LoadFirmwareImageFromBuffer`
4. Flash via `TUSBAUDIO_UpdateFirmware`
5. Poll progress with DCP command 0x810 (opcode 5)
6. Status codes: 2 = complete, 3 = aborted

### TUSBAUDIO Flash API

| Function | Description |
|----------|-------------|
| TUSBAUDIO_GetFirmwareVersion | Read current firmware version |
| TUSBAUDIO_UpdateFirmware | Flash loaded firmware image |
| TUSBAUDIO_LoadFirmwareImageFromFile | Load .bin from filesystem |
| TUSBAUDIO_LoadFirmwareImageFromBuffer | Load .bin from memory |
| TUSBAUDIO_ReadSPIFlash | Direct SPI flash read |
| TUSBAUDIO_WriteSPIFlash | Direct SPI flash write |

---

## 7. Device Initialization Sequence (from GoXLR App)

Full sequence from `tcat_dice_tusb_device_open()`:

1. Open USB device via TUSBAUDIO API
2. Create event notification thread
3. Create "Global TCAT Dice Mutex"
4. Validate DCP global data: cat=0 op=0x18, check magic `0x04061973` and `0x18101966`
5. Register for device events
6. Flush: `dcp_send_command(cat=0, op=0, data=NULL, size=0)`
7. Query capabilities:
   - Cat 0, Op 2: System info (0x70 bytes)
   - Cat 0, Op 1: Check support for categories 1-4
   - Cat 1, Op 0: Peaks capabilities
   - Cat 2, Op 0: Mixer capabilities (8 bytes)
   - Cat 3, Op 0: Router capabilities (0xC bytes)
   - Cat 4, Op 0: NVM capabilities (0x10 bytes)
   - Cat 4, Op 1: Segment info (iterated)

---

## 8. BLE Protocol (Blender Only)

The Blender has Bluetooth LE for wireless control via the Android/iOS app.
The architecture is **reversed**: the host advertises as a GATT peripheral and
the Blender connects as a central.

### UUIDs

| UUID | Name |
|------|------|
| `E71EE188-279F-4ED6-8055-12D77BFD900C` | ParameterService |
| `50E2D021-F23B-46FB-B7E6-FBE12301276A` | ParameterCharacteristic |
| `F7E58580-9BB5-48A3-B8A4-6BE6A391B8DF` | AppDetailService |

### Pairing

1. Host advertises ParameterService UUID with name "BlenderCtl" (connectable, discoverable)
2. User presses PAIR button on Blender
3. Blender scans for the UUID and connects as central
4. Blender writes/reads ParameterCharacteristic for control

### Characteristic Properties

- Read, Write, Write Without Response, Notify
- CCCD descriptor 0x2902 for notifications

### Parameter Protocol

Data on ParameterCharacteristic is packed as sequential 3-byte tuples:

```
byte 0: paramID
byte 1: subParam
byte 2: value (0-255)
```

### Known BLE Parameters

| ID | Name | Description |
|----|------|-------------|
| 0 | input1 | Input 1 level |
| 1 | input2 | Input 2 level |
| 2 | input3 | Input 3 level |
| 3 | input4 | Input 4 level |
| 4 | input5 | Input 5 level |
| 5 | input6 | Input 6 level |
| 6 | level | Master level |
| 7 | compressor | Compressor amount |
| 8 | micGain | Microphone gain |
| 9 | talk | Talkback |
| 11 | blenderState | Device state |
| 12 | version | Firmware version |
| 20 | muteOutput | Mute output |
| 21 | compressorOnOff | Compressor enable |

---

## 9. Blender vs GoXLR Differences

| Feature | GoXLR | Blender |
|---------|-------|---------|
| Audio channels | 12×8 (in×out) | 12×2 (stereo mixer) |
| USB PID | 0x8FE0 / 0x8FE4 | 0x8FE1 |
| Physical controls | Faders, buttons, encoders | Knobs only |
| Scribble strips (0x802) | Yes (LCD) | No |
| LED colors (0x803) | Yes (RGB) | No |
| LED state (0x811) | Yes | No |
| Animation (0x809, 0x816) | Yes | No |
| BLE control | No | Yes |
| DSP params (0x80B) | Yes | Likely yes |
| Routing (0x804) | Yes | Likely yes |
| Boot quirk | Same | Same |
| DICE platform | Same | Same |

---

## 10. TUSBAUDIO Driver API (vtable)

The Windows driver (`blender_audio.sys` / TUSBAudio) is loaded dynamically.
Function pointers at known offsets from the loader base:

| Offset | Function |
|--------|----------|
| +0x04 | GetApiVersion |
| +0x08 | CheckApiVersion |
| +0x0C | EnumerateDevices |
| +0x10 | GetDeviceProperties |
| +0x14 | GetDeviceInfo |
| +0x18 | GetDriverInfo |
| +0x1C | OpenDeviceByIndex |
| +0x20 | OpenDeviceByGuid |
| +0x24 | CloseDevice |
| +0x28 | GetDeviceContextHandle |
| +0x2C | RegisterDeviceCallback |
| +0x30 | RegisterPnpNotification |
| +0x34 | UnregisterPnpNotification |
| +0x38 | GetStreamingInterfaceProperties |
| +0x3C | GetDeviceStringDescriptor |
| +0x40 | GetUsbConfigDescriptor |
| +0x44 | SetAudioClockSource |
| +0x48 | GetAudioClockSourceList |
| +0x4C | GetCurrentAudioClockSource |
| +0x50 | GetAudioClockDomain |
| +0x54 | SetSampleRate |
| +0x58 | GetCurrentSampleRate |
| +0x5C | GetSampleRateList |
| +0x60 | SetAsioSampleRate |
| +0x64 | GetCurrentFrameSize |
| +0x68 | GetFrameSizeList |
| +0x6C | SetAudioMode |
| +0x70 | GetCurrentAudioMode |
| +0x74 | GetAudioModeList |
| +0x78 | AudioStreamingStart |
| +0x7C | AudioStreamingStop |
| +0x80 | AudioStreamingGetCurrentState |
| +0x84 | AudioStreamingSendData |
| +0x88 | AudioStreamingReceiveData |
| +0x8C | ReadRegister |
| +0x90 | WriteRegister |
| +0x94 | ReadSPIFlash |
| +0x98 | WriteSPIFlash |
| +0x9C | ClassVendorRequestOut |
| +0xA0 | ClassVendorRequestIn |
| +0xA4 | GetFirmwareVersion |
| +0xA8 | UpdateFirmware |
| +0xAC | PollDeviceStatus |
| +0xB0 | GetCurrentBitDepth |
| +0xB4 | GetBitDepthList |
| +0xB8 | GetAsioProperties |
| +0xBC | SetMasterClockDivider |
| +0xC0 | GetCurrentMasterClockDivider |
| +0xC4 | GetMasterClockDividerList |
| +0xC8 | GetDevicePowerState |
| +0xCC | SetDevicePowerState |
| +0xD0 | NotifyStreamingFormatChange |
| +0xD4 | GetCurrentVolumeLevel |
| +0xD8 | SetVolumeLevel |
| +0xDC | GetVolumeLevelRange |
| +0xE0 | GetCurrentMuteState |
| +0xE4 | SetMuteState |
| +0xE8 | GetDspProperty |
| +0xEC | SetDspProperty |
| +0xF0 | GetDspPropertyDescription |
| +0xF4 | GetDspPropertyNameFromId |
| +0xF8 | EnableDspPropertyChangeNotification |
| +0xFC | DisableDspPropertyChangeNotification |

---

## 11. Key Ghidra Addresses (GoXLR App.exe)

| Address | Function |
|---------|----------|
| 0x00622280 | tusbaudio_load_api |
| 0x006217d0 | tcat_dice_tusb_device_open |
| 0x0061fb60 | dcp_send_command |
| 0x0061f960 | dcp_cmd_with_retry |
| 0x0061ed90 | dcp_validate_global_data |
| 0x0061f4e0 | dcp_get_capabilities |
| 0x00621660 | dcp_transfer_out |
| 0x00621560 | dcp_transfer_in |
| 0x00692d40 | goxlr_send_dcp_command |
| 0x00683aa0 | goxlr_dispatch_pending_commands |
| 0x00686ac0 | goxlr_send_routing |
| 0x006863c0 | goxlr_send_fader_positions |
| 0x006865a0 | goxlr_send_fader_associations |
| 0x006867a0 | goxlr_send_scribble_strips |
| 0x00686200 | goxlr_send_button_state |
| 0x006862d0 | goxlr_send_encoder_positions |
| 0x00686120 | goxlr_send_mic_type |
| 0x00686040 | goxlr_send_mic_gain_dynamic |
| 0x00685f60 | goxlr_send_mic_gain_condenser |
| 0x00685e80 | goxlr_send_mic_gain_trs |
| 0x006839a0 | goxlr_send_mic_gain_all_types |
| 0x00686e20 | goxlr_send_effect_parameters |
| 0x00686c40 | goxlr_send_effect_params |
| 0x00686ff0 | goxlr_send_colors |
| 0x00686f20 | goxlr_send_animation_command |
| 0x00685c30 | goxlr_send_led_state |
| 0x00685e00 | goxlr_send_led_state_all_faders |
| 0x00685ca0 | goxlr_send_per_channel_animation |
| 0x00685d90 | goxlr_send_per_channel_volume |
| 0x00685bd0 | goxlr_send_unknown_0x818 |
| 0x00685b60 | goxlr_send_cmd_0x817 |
| 0x00775d00 | goxlr_send_fader_position_single |
| 0x00727230 | goxlr_read_firmware_version |
| 0x00677280 | goxlr_read_serial_number |
| 0x00506050 | goxlr_poll_firmware_status |
| 0x0067f6e0 | goxlr_handle_profile_change |
