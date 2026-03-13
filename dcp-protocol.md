# DCP-over-USB Protocol (GoXLR / TC Helicon Blender)

Reverse engineered from `GoXLR App.exe` (Windows) using Ghidra.
Both GoXLR (PID 0x8FE0) and Blender (PID 0x8FE1) use the same TCAT DICE platform
and share this protocol.

## USB Transport Layer

All DCP commands are sent as USB Class Vendor Requests via the Thesycon TUSBAUDIO driver:

### Outbound (host -> device)
```
TUSBAUDIO_ClassVendorRequestOut(
    device_handle,
    bRequest  = 1,
    wValue    = 0,
    buffer    = <unused>,
    wIndex    = audio_ctrl_interface,  // e.g. 0x2900
    flags     = 0,
    data_ptr  = <DCP packet>,
    data_size = <packet size>,
    &bytes_transferred,
    timeout
)
```

### Inbound (device -> host)
```
TUSBAUDIO_ClassVendorRequestIn(
    device_handle,
    bRequest  = 1,
    wValue    = 0,
    buffer    = <unused>,
    wIndex    = audio_ctrl_interface,
    flags     = 0,
    data_ptr  = <response buffer>,
    data_size = <buffer size>,
    &bytes_transferred,
    timeout
)
```

## DCP Packet Format

Every DCP packet has a 16-byte header followed by optional payload:

```
Offset  Size  Field
0x00    4     word0: (category & 0xFFF) << 12 | (opcode & 0xFFF)
0x04    4     word1: (seqnum << 16) | (data_size & 0xFFFF)
0x08    4     word2: 0 (reserved)
0x0C    4     word3: 0 (reserved)
0x10    N     payload (data_size bytes)
```

- **category**: 12-bit command category
- **opcode**: 12-bit operation code within category
- **seqnum**: 16-bit sequence number (incremented per command, validated in response)
- **data_size**: 16-bit payload size in bytes

The response echoes the same header (category, opcode, seqnum) with response data.

## DCP Categories

### Standard TCAT DICE Categories (0x000-0x004)

| Cat | Name    | Description |
|-----|---------|-------------|
| 0   | System  | Global info, capabilities, flush |
| 1   | Peaks   | Peak metering |
| 2   | Mixer   | DICE mixer matrix |
| 3   | Router  | DICE audio router |
| 4   | NVM     | Non-volatile memory |

### Vendor-Specific Categories (0x800+)

All GoXLR/Blender custom commands use categories 0x800 and above.

## Standard DICE Commands (Category 0-4)

### Category 0: System

| Opcode | Direction | Size   | Description |
|--------|-----------|--------|-------------|
| 0x000  | Write     | 0      | Flush / ping |
| 0x001  | Read      | varies | supports_dcp_category(cat_id) |
| 0x002  | Read      | 0x70   | System info |
| 0x018  | Read      | 0x18   | Global data (validated with magic 0x04061973, 0x18101966) |

### Category 4: NVM

| Opcode | Direction | Size   | Description |
|--------|-----------|--------|-------------|
| 0x000  | Read      | 0x10   | NVM capabilities |
| 0x001  | Read      | 0x18   | Segment info (iterated) |
| 0x002  | Write     | varies | Erase segment (part 1) |
| 0x003  | Write     | varies | Erase segment (part 2) |
| 0x004  | Write     | varies | Write segment |
| 0x006  | Read      | varies | CRC32 check |

## GoXLR/Blender Vendor Commands

All vendor commands go through `goxlr_send_dcp_command()` which enters a critical
section and calls `dcp_cmd_with_retry()`.

The call signature is:
```c
goxlr_send_dcp_command(
    uint16_t category,    // DCP category (0x800+)
    uint16_t opcode,      // Operation within category
    void*    send_data,   // Data to send (or NULL)
    uint32_t send_size,   // Size of send data
    void*    recv_data,   // Buffer for response (or NULL)
    uint32_t* recv_size   // Pointer to response size (or NULL)
)
```

### Command 0x800: Read Device State

| Opcode | Dir  | Size | Description |
|--------|------|------|-------------|
| 0      | Read | 0    | Read current device state (sent after profile change) |

### Command 0x801: Set Effect Parameters (bulk)

| Opcode | Dir   | Size     | Description |
|--------|-------|----------|-------------|
| 0      | Write | N * 8    | Array of (paramID:u32, value:u32) pairs |

Sends a batch of effect parameter changes. Each entry is 8 bytes:
- bytes 0-3: parameter ID (from dspTree)
- bytes 4-7: parameter value

### Command 0x802: Set Scribble Strip

| Opcode    | Dir   | Size    | Description |
|-----------|-------|---------|-------------|
| strip_idx | Write | varies  | Scribble strip data for a specific strip |

### Command 0x803: Set Colors

| Opcode | Dir   | Size   | Description |
|--------|-------|--------|-------------|
| 0      | Write | 0x208  | Full color map (520 bytes) for all LEDs/buttons |

### Command 0x804: Set Routing Row

| Opcode    | Dir   | Size  | Description |
|-----------|-------|-------|-------------|
| row_index | Write | 0x1A  | Single routing matrix row (13 x uint16 values) |

Total routing matrix is 0x12 rows (18 rows), each 0x1A bytes (26 bytes = 13 channels).

### Command 0x805: Set Fader Assignment

| Opcode | Dir   | Size | Description |
|--------|-------|------|-------------|
| varies | Write | var  | Which audio channel a fader controls |

### Command 0x806: Set Fader Position

| Opcode                    | Dir   | Size | Description |
|---------------------------|-------|------|-------------|
| (fader_idx << 4) \| chan  | Write | 1    | Single fader position (0-255) |

The opcode encodes both the fader index and channel. Position byte is scaled
from a float (0.0-1.0) multiplied by 255.

### Command 0x808: Set Button State

| Opcode | Dir   | Size  | Description |
|--------|-------|-------|-------------|
| 0      | Write | 0x18  | Button state bitmap (24 bytes) |

### Command 0x809: Set Per-Channel Animation

| Opcode     | Dir   | Size | Description |
|------------|-------|------|-------------|
| channel_id | Write | 1    | Animation enable/disable for a channel |

Data byte: 0 = off, 1 = on.

### Command 0x80A: Set Encoder Position

| Opcode      | Dir   | Size | Description |
|-------------|-------|------|-------------|
| encoder_idx | Write | 1    | Encoder position value |

4 encoders (indices 0-3).

### Command 0x80B: Set DSP Parameters (key-value)

| Opcode | Dir   | Size  | Description |
|--------|-------|-------|-------------|
| 0      | Write | N * 8 | Array of (paramID:u32, value:u32) pairs |

This is the primary mechanism for setting DSP parameters. Used for:
- Mic type selection (MIC_TYPE paramID + type value)
- Mic gain (MIC_GAIN_DYNAMIC, MIC_GAIN_CONDENSER, MIC_GAIN_TRS paramIDs)
- Effect parameters (from the dspTree)

The paramID comes from looking up a named parameter (e.g. "MIC_GAIN_DYNAMIC")
in a parameter registry, getting its numeric ID, and subtracting 1.

### Command 0x80C: Read Serial Number

| Opcode | Dir  | Size | Description |
|--------|------|------|-------------|
| 0      | Read | 2    | Device serial number (uint16) |

### Command 0x80F: Read Firmware Version

| Opcode | Dir  | Size  | Description |
|--------|------|-------|-------------|
| 0      | Read | 0x18  | Firmware version info (24 bytes) |

### Command 0x810: Read Firmware Status

| Opcode | Dir  | Size  | Description |
|--------|------|-------|-------------|
| 5      | Read | 0x18  | Firmware status (24 bytes) |

### Command 0x811: Set LED State

| Opcode    | Dir   | Size | Description |
|-----------|-------|------|-------------|
| fader_idx | Write | 2    | LED state for a fader (2 bytes: mode + color) |

Byte 0: LED mode (1 = single color off, 3 = single color on, etc.)
Byte 1: LED behavior (1 = off, 2 = gradient, 4 = solid)

When fader_idx = 0 and iterating 0-3, sets all fader LEDs.

### Command 0x814: Set Per-Channel Volume

| Opcode      | Dir   | Size | Description |
|-------------|-------|------|-------------|
| channel_idx | Write | 2    | Volume level (uint16) for a channel |

4 channels (indices 0-3).

### Command 0x816: Set Animation Command

| Opcode | Dir   | Size | Description |
|--------|-------|------|-------------|
| 0      | Write | 5    | Animation control (5 bytes) |

### Command 0x817: Unknown

| Opcode | Dir   | Size | Description |
|--------|-------|------|-------------|
| 0      | Write | 8    | Unknown 8-byte payload |

### Command 0x818: Unknown

| Opcode | Dir   | Size | Description |
|--------|-------|------|-------------|
| 0      | Write | 1    | Unknown single byte |

## Device Initialization Sequence

From `tcat_dice_tusb_device_open()`:

1. Open USB device via TUSBAUDIO API
2. Create event notification thread
3. Create "Global TCAT Dice Mutex"
4. Validate DCP global data: read cat=0, op=0x18 (24 bytes), check magic words
   `0x04061973` and `0x18101966`
5. Register for device events
6. Send flush command: `dcp_send_command(cat=0, op=0, data=NULL, size=0)`
7. Query capabilities:
   - Cat 0, Op 2: System info (0x70 bytes)
   - Cat 0, Op 1: Check support for categories 1-4
   - Cat 1, Op 0: Peaks capabilities
   - Cat 2, Op 0: Mixer capabilities (8 bytes)
   - Cat 3, Op 0: Router capabilities (0xC bytes)
   - Cat 4, Op 0: NVM capabilities (0x10 bytes)
   - Cat 4, Op 1: Segment info (iterated)

## DSP Parameter System (dspTree)

The GoXLR app uses a hierarchical parameter tree ("dspTree") to manage DSP effects.
Parameters are identified by string names in the UI/XML layer:

### Known Parameter Names (from strings/UI code)
- `MIC_TYPE` - Microphone type selection
- `MIC_GAIN_DYNAMIC` - Dynamic mic gain
- `MIC_GAIN_CONDENSER` - Condenser mic gain
- `MIC_GAIN_TRS` - TRS mic gain
- `MIC_EQ` - Microphone EQ
- `REVERB_TYPE` - Reverb algorithm type
- `REVERB_DECAY` - Reverb decay time
- `REVERB_HIFACTOR` - Reverb high frequency factor
- `REVERB_MODSPEED` - Reverb modulation speed
- `REVERB_LOCOLOR` - Reverb low frequency color
- `REVERB_OUT_LEVEL` - Reverb output level
- `COMPRESSOR` - Compressor parameters

### Parameter Value Format

When sent via DCP commands 0x801 or 0x80B, parameters are packed as:
```
struct param_entry {
    uint32_t param_id;   // Numeric ID (looked up from name, minus 1)
    uint32_t value;      // Parameter value
};
```

Multiple entries can be batched in a single command.

## TUSBAUDIO API Vtable

The TUSBAUDIO DLL is loaded dynamically. Function pointers are stored in a vtable
at known offsets from the loader object base:

| Offset | Function |
|--------|----------|
| +0x04  | TUSBAUDIO_GetApiVersion |
| +0x08  | TUSBAUDIO_CheckApiVersion |
| +0x0C  | TUSBAUDIO_EnumerateDevices |
| +0x10  | TUSBAUDIO_GetDeviceProperties |
| +0x14  | TUSBAUDIO_GetDeviceInfo |
| +0x18  | TUSBAUDIO_GetDriverInfo |
| +0x1C  | TUSBAUDIO_OpenDeviceByIndex |
| +0x20  | TUSBAUDIO_OpenDeviceByGuid |
| +0x24  | TUSBAUDIO_CloseDevice |
| +0x28  | TUSBAUDIO_GetDeviceContextHandle |
| +0x2C  | TUSBAUDIO_RegisterDeviceCallback |
| +0x30  | TUSBAUDIO_RegisterPnpNotification |
| +0x34  | TUSBAUDIO_UnregisterPnpNotification |
| +0x38  | TUSBAUDIO_GetStreamingInterfaceProperties |
| +0x3C  | TUSBAUDIO_GetDeviceStringDescriptor |
| +0x40  | TUSBAUDIO_GetUsbConfigDescriptor |
| +0x44  | TUSBAUDIO_SetAudioClockSource |
| +0x48  | TUSBAUDIO_GetAudioClockSourceList |
| +0x4C  | TUSBAUDIO_GetCurrentAudioClockSource |
| +0x50  | TUSBAUDIO_GetAudioClockDomain |
| +0x54  | TUSBAUDIO_SetSampleRate |
| +0x58  | TUSBAUDIO_GetCurrentSampleRate |
| +0x5C  | TUSBAUDIO_GetSampleRateList |
| +0x60  | TUSBAUDIO_SetAsioSampleRate |
| +0x64  | TUSBAUDIO_GetCurrentFrameSize |
| +0x68  | TUSBAUDIO_GetFrameSizeList |
| +0x6C  | TUSBAUDIO_SetAudioMode |
| +0x70  | TUSBAUDIO_GetCurrentAudioMode |
| +0x74  | TUSBAUDIO_GetAudioModeList |
| +0x78  | TUSBAUDIO_AudioStreamingStart |
| +0x7C  | TUSBAUDIO_AudioStreamingStop |
| +0x80  | TUSBAUDIO_AudioStreamingGetCurrentState |
| +0x84  | TUSBAUDIO_AudioStreamingSendData |
| +0x88  | TUSBAUDIO_AudioStreamingReceiveData |
| +0x8C  | TUSBAUDIO_ReadRegister |
| +0x90  | TUSBAUDIO_WriteRegister |
| +0x94  | TUSBAUDIO_ReadSPIFlash |
| +0x98  | TUSBAUDIO_WriteSPIFlash |
| +0x9C  | TUSBAUDIO_ClassVendorRequestOut |
| +0xA0  | TUSBAUDIO_ClassVendorRequestIn |
| +0xA4  | TUSBAUDIO_GetFirmwareVersion |
| +0xA8  | TUSBAUDIO_UpdateFirmware |
| +0xAC  | TUSBAUDIO_PollDeviceStatus |
| +0xB0  | TUSBAUDIO_GetCurrentBitDepth |
| +0xB4  | TUSBAUDIO_GetBitDepthList |
| +0xB8  | TUSBAUDIO_GetAsioProperties |
| +0xBC  | TUSBAUDIO_SetMasterClockDivider |
| +0xC0  | TUSBAUDIO_GetCurrentMasterClockDivider |
| +0xC4  | TUSBAUDIO_GetMasterClockDividerList |
| +0xC8  | TUSBAUDIO_GetDevicePowerState |
| +0xCC  | TUSBAUDIO_SetDevicePowerState |
| +0xD0  | TUSBAUDIO_NotifyStreamingFormatChange |
| +0xD4  | TUSBAUDIO_GetCurrentVolumeLevel |
| +0xD8  | TUSBAUDIO_SetVolumeLevel |
| +0xDC  | TUSBAUDIO_GetVolumeLevelRange |
| +0xE0  | TUSBAUDIO_GetCurrentMuteState |
| +0xE4  | TUSBAUDIO_SetMuteState |
| +0xE8  | TUSBAUDIO_GetDspProperty |
| +0xEC  | TUSBAUDIO_SetDspProperty |
| +0xF0  | TUSBAUDIO_GetDspPropertyDescription |
| +0xF4  | TUSBAUDIO_GetDspPropertyNameFromId |
| +0xF8  | TUSBAUDIO_EnableDspPropertyChangeNotification |
| +0xFC  | TUSBAUDIO_DisableDspPropertyChangeNotification |
| +0x100 | hModule (DLL handle) |
| +0x104 | apiVersion |

## Key Ghidra Function Names (renamed)

| Address    | Name |
|------------|------|
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

## Relevance to TC Helicon Blender

The Blender (PID 0x8FE1) uses the same TCAT DICE chip and same DCP protocol.
The GoXLR App.exe even references PID 0x8FE4 (GoXLR Mini) explicitly in the
audio_settings_constructor, confirming the sister-device relationship.

For Linux implementation:
1. The boot init sequence (vendor read + class write) activates audio streaming
2. DCP commands 0x80B (DSP params) and 0x804 (routing) are the most important
   for controlling the mixer
3. The parameter IDs used in 0x80B correspond to named DSP properties in the
   TUSBAUDIO driver's property system
4. Commands 0x803 (colors), 0x811 (LEDs), 0x816 (animation) are GoXLR-specific
   visual features that the Blender may not have
5. Commands 0x80B for mic gain and 0x804 for routing are likely shared
