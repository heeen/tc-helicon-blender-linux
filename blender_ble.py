#!/usr/bin/env python3
"""
BLE central (GATT client) for controlling the TC Helicon Blender.

The Blender advertises ParameterService UUID when PAIR is pressed.
We scan for it, connect as a GATT client, subscribe to notifications
on the ParameterCharacteristic, and exchange 3-byte tuples
(paramID, subParam, value).

Usage:
    .venv/bin/python blender_ble.py

Press PAIR on the Blender, then run this script. Type 'help' for commands.
"""

import asyncio
import signal
import sys
from enum import IntEnum

from bleak import BleakClient, BleakScanner

# ── BLE UUIDs ────────────────────────────────────────────────────────────

PARAM_SERVICE_UUID = "e71ee188-279f-4ed6-8055-12d77bfd900c"
PARAM_CHAR_UUID = "50e2d021-f23b-46fb-b7e6-fbe12301276a"

# ── Parameter IDs (from Constants.java) ──────────────────────────────────


class Param(IntEnum):
    INPUT1 = 0
    INPUT2 = 1
    INPUT3 = 2
    INPUT4 = 3
    INPUT5 = 4
    INPUT6 = 5
    LEVEL = 6
    COMPRESSOR = 7
    MIC_GAIN = 8
    TALK = 9
    # 10 unused
    BLENDER_STATE = 11
    VERSION = 12
    # 13 unused
    ICON_CHANGE = 14
    SCAN_FOR_MORE = 15
    DISCONNECT = 16
    IS_BLENDER = 17
    HAS_BLENDER = 18
    REQUEST_STATE = 19
    MUTE_OUTPUT = 20
    COMPRESSOR_ON_OFF = 21


PARAM_NAMES = {p.value: p.name.lower() for p in Param}

# Slider params have 4 sub-params (output buses 0-3)
SLIDER_PARAMS = {
    Param.INPUT1, Param.INPUT2, Param.INPUT3,
    Param.INPUT4, Param.INPUT5, Param.INPUT6,
    Param.LEVEL, Param.COMPRESSOR, Param.MIC_GAIN,
}

# Defaults from Constants.java (signed bytes)
DEFAULTS = {
    Param.INPUT1: -51,  # 0xCD
    Param.INPUT2: -51,
    Param.INPUT3: -51,
    Param.INPUT4: -51,
    Param.INPUT5: -51,
    Param.INPUT6: -51,
    Param.LEVEL: 82,
    Param.COMPRESSOR: -125,  # 0x83
    Param.MIC_GAIN: 82,
    Param.TALK: 0,
    Param.BLENDER_STATE: 0,
    Param.VERSION: 0,
    Param.MUTE_OUTPUT: 0,
    Param.COMPRESSOR_ON_OFF: 0,
}

# CLI-friendly aliases
PARAM_ALIASES = {
    "input1": Param.INPUT1, "in1": Param.INPUT1,
    "input2": Param.INPUT2, "in2": Param.INPUT2,
    "input3": Param.INPUT3, "in3": Param.INPUT3,
    "input4": Param.INPUT4, "in4": Param.INPUT4,
    "input5": Param.INPUT5, "in5": Param.INPUT5,
    "input6": Param.INPUT6, "in6": Param.INPUT6,
    "level": Param.LEVEL, "lvl": Param.LEVEL,
    "comp": Param.COMPRESSOR, "compressor": Param.COMPRESSOR,
    "mic": Param.MIC_GAIN, "micgain": Param.MIC_GAIN,
    "talk": Param.TALK,
    "mute": Param.MUTE_OUTPUT,
}

# ── Helpers ──────────────────────────────────────────────────────────────


def to_signed(val: int) -> int:
    return val - 256 if val > 127 else val


def to_unsigned(val: int) -> int:
    return val & 0xFF


def format_value(param_id: int, val: int) -> str:
    return f"{val:3d} (0x{val:02X})"


def encode_tuples(tuples: list[tuple[int, int, int]]) -> bytearray:
    data = bytearray()
    for pid, sub, val in tuples:
        data.extend([pid & 0xFF, sub & 0xFF, to_unsigned(val)])
    return data


def decode_tuples(data: bytes) -> list[tuple[int, int, int]]:
    tuples = []
    for i in range(0, len(data) - 2, 3):
        tuples.append((data[i], data[i + 1], data[i + 2]))
    return tuples


# ── Blender State ────────────────────────────────────────────────────────


class BlenderState:
    def __init__(self):
        self.values: dict[tuple[int, int], int] = {}
        self.connected = False
        for pid, default in DEFAULTS.items():
            if pid in SLIDER_PARAMS:
                for sub in range(4):
                    self.values[(pid, sub)] = to_unsigned(default)
            else:
                self.values[(pid, 0)] = to_unsigned(default)

    def update(self, param_id: int, sub: int, value: int):
        self.values[(param_id, sub)] = value

    def get(self, param_id: int, sub: int = 0) -> int:
        return self.values.get((param_id, sub), 0)

    def format_status(self) -> str:
        lines = []
        lines.append(f"Connected: {self.connected}")
        lines.append("")
        for pid in [Param.INPUT1, Param.INPUT2, Param.INPUT3,
                     Param.INPUT4, Param.INPUT5, Param.INPUT6,
                     Param.LEVEL, Param.COMPRESSOR, Param.MIC_GAIN]:
            name = PARAM_NAMES[pid]
            vals = [format_value(pid, self.get(pid, s)) for s in range(4)]
            lines.append(f"  {name:12s}  {vals[0]:>14s}  {vals[1]:>14s}  {vals[2]:>14s}  {vals[3]:>14s}")
        lines.append("")
        for pid in [Param.TALK, Param.MUTE_OUTPUT, Param.COMPRESSOR_ON_OFF,
                     Param.BLENDER_STATE, Param.VERSION]:
            name = PARAM_NAMES[pid]
            val = self.get(pid, 0)
            lines.append(f"  {name:12s}  {format_value(pid, val)}")
        return "\n".join(lines)


# ── GATT Client ──────────────────────────────────────────────────────────


class BlenderBLE:
    def __init__(self):
        self.state = BlenderState()
        self.client: BleakClient | None = None
        self.stop_event = asyncio.Event()

    def on_notification(self, sender, data: bytearray):
        """Called when Blender sends a notification on ParameterCharacteristic."""
        tuples = decode_tuples(bytes(data))
        parts = []
        for pid, sub, val in tuples:
            name = PARAM_NAMES.get(pid, f"param_{pid}")
            parts.append(f"{name}[{sub}]={format_value(pid, val)}")
        print(f"  RX  {data.hex()}  ({', '.join(parts)})")

        for pid, sub, val in tuples:
            if pid == Param.IS_BLENDER:
                if not self.state.connected:
                    print("  Blender identified itself!")
                    self.state.connected = True
                    asyncio.get_event_loop().create_task(self._on_is_blender())
            elif pid == Param.VERSION:
                self.state.update(pid, sub, val)
                print(f"  Firmware version byte: {val}")
            elif pid == Param.BLENDER_STATE:
                self.state.update(pid, sub, val)
                print(f"  Jack sense bitmap: 0x{val:02X}")
            else:
                self.state.update(pid, sub, val)

    async def _on_is_blender(self):
        """Handle isBlender: send hasBlender + requestBlenderState."""
        print("  Handshake: sending hasBlender(1) + requestBlenderState")
        # Per Central.java: setHasBlender(true), then requestBlenderState
        await self.send_params([(Param.HAS_BLENDER, 0, 1)])
        await asyncio.sleep(0.01)  # sleepBetweenBroadcastTime = 10ms
        await self.send_params([(Param.REQUEST_STATE, 0, 0)])

    async def send_params(self, tuples: list[tuple[int, int, int]]):
        """Write parameter tuples to the Blender's ParameterCharacteristic."""
        if not self.client or not self.client.is_connected:
            print("  Not connected")
            return
        data = encode_tuples(tuples)
        # Central.java uses writeType=2 (WRITE_TYPE_DEFAULT = write with response)
        await self.client.write_gatt_char(PARAM_CHAR_UUID, data, response=True)
        names = ", ".join(
            f"{PARAM_NAMES.get(pid, f'param_{pid}')}[{sub}]={format_value(pid, to_unsigned(val))}"
            for pid, sub, val in tuples
        )
        print(f"  TX  {data.hex()}  ({names})")

    async def scan_and_connect(self):
        """Scan for the Blender and connect."""
        print("Scanning for Blender (advertising ParameterService)...")
        print("Press PAIR on the Blender now!")
        print()

        device = None
        while not device and not self.stop_event.is_set():
            devices = await BleakScanner.discover(
                timeout=5.0,
                service_uuids=[PARAM_SERVICE_UUID],
            )
            for d in devices:
                print(f"  Found: {d.name} ({d.address})")
                if d.name and "blender" in d.name.lower():
                    device = d
                    break

            if not device:
                if devices:
                    print("  No Blender found in scan results, retrying...")
                else:
                    print("  No BLE devices found, retrying...")

        if self.stop_event.is_set():
            return

        print(f"\nConnecting to {device.name} ({device.address})...")
        self.client = BleakClient(device.address)
        await self.client.connect()
        print(f"Connected! MTU={self.client.mtu_size}")

        # List services for debugging
        for service in self.client.services:
            print(f"  Service: {service.uuid}")
            for char in service.characteristics:
                props = ", ".join(char.properties)
                print(f"    Char: {char.uuid} [{props}]")
                for desc in char.descriptors:
                    print(f"      Desc: {desc.uuid}")

        # Subscribe to notifications on ParameterCharacteristic
        print(f"\nSubscribing to notifications on {PARAM_CHAR_UUID}...")
        await self.client.start_notify(PARAM_CHAR_UUID, self.on_notification)
        print("Subscribed. Waiting for Blender handshake...")
        print()

    # ── CLI ───────────────────────────────────────────────────────────

    def parse_value(self, s: str) -> int:
        s = s.strip()
        if s.startswith("0x") or s.startswith("0X"):
            return int(s, 16) & 0xFF
        val = int(s)
        if val < 0:
            return to_unsigned(val)
        return val & 0xFF

    async def handle_command(self, line: str):
        parts = line.strip().split()
        if not parts:
            return
        cmd = parts[0].lower()

        if cmd in ("help", "?", "h"):
            print("""Commands:
  status              Show all parameter values
  input1 <val>        Set input1 on all 4 buses (-128..127 or 0..255 or 0xNN)
  input1.0 <val>      Set input1 bus 0 only
  level <val>         Master output level
  comp <val>          Compressor threshold
  comp on             Enable compressor (all buses)
  comp off            Disable compressor
  mic <val>           Mic gain
  talk on|off         Talkback toggle
  mute <bitmap>       Set mute output bitmap (0-255 or 0xNN)
  raw <id> <sub> <v>  Send raw tuple
  dump                Request full state from Blender
  services            List GATT services
  quit                Exit""")
            return

        if cmd in ("status", "st", "s"):
            print(self.state.format_status())
            return

        if cmd in ("quit", "exit", "q"):
            self.stop_event.set()
            return

        if cmd == "dump":
            await self.send_params([(Param.REQUEST_STATE, 0, 0)])
            return

        if cmd == "services":
            if self.client and self.client.is_connected:
                for service in self.client.services:
                    print(f"  {service.uuid}")
                    for char in service.characteristics:
                        props = ", ".join(char.properties)
                        val = await self.client.read_gatt_char(char.uuid)
                        print(f"    {char.uuid} [{props}] = {val.hex()}")
            else:
                print("  Not connected")
            return

        if cmd == "raw":
            if len(parts) < 4:
                print("Usage: raw <param_id> <sub> <value>")
                return
            pid = int(parts[1])
            sub = int(parts[2])
            val = self.parse_value(parts[3])
            await self.send_params([(pid, sub, val)])
            return

        # comp on/off special case
        if cmd in ("comp", "compressor") and len(parts) == 2 and parts[1] in ("on", "off"):
            val = 0x0F if parts[1] == "on" else 0x00
            await self.send_params([(Param.COMPRESSOR_ON_OFF, 0, val)])
            self.state.update(Param.COMPRESSOR_ON_OFF, 0, val)
            return

        # talk on/off
        if cmd == "talk" and len(parts) == 2 and parts[1] in ("on", "off"):
            val = 1 if parts[1] == "on" else 0
            await self.send_params([(Param.TALK, 0, val)])
            self.state.update(Param.TALK, 0, val)
            return

        # mute
        if cmd == "mute" and len(parts) == 2:
            val = self.parse_value(parts[1])
            await self.send_params([(Param.MUTE_OUTPUT, 0, val)])
            self.state.update(Param.MUTE_OUTPUT, 0, val)
            return

        # Param with optional .sub notation
        param_name = cmd.split(".")[0]
        sub_str = cmd.split(".")[1] if "." in cmd else None

        if param_name not in PARAM_ALIASES:
            print(f"Unknown command: {cmd}")
            return

        pid = PARAM_ALIASES[param_name]
        if len(parts) < 2:
            if pid in SLIDER_PARAMS:
                for s in range(4):
                    v = self.state.get(pid, s)
                    print(f"  {param_name}[{s}] = {format_value(pid, v)}")
            else:
                v = self.state.get(pid, 0)
                print(f"  {param_name} = {format_value(pid, v)}")
            return

        val = self.parse_value(parts[1])

        if sub_str is not None:
            sub = int(sub_str)
            await self.send_params([(pid, sub, val)])
            self.state.update(pid, sub, val)
        elif pid in SLIDER_PARAMS:
            tuples = [(pid, s, val) for s in range(4)]
            await self.send_params(tuples)
            for s in range(4):
                self.state.update(pid, s, val)
        else:
            await self.send_params([(pid, 0, val)])
            self.state.update(pid, 0, val)

    async def cli_loop(self):
        loop = asyncio.get_event_loop()
        while not self.stop_event.is_set():
            try:
                line = await loop.run_in_executor(None, lambda: input("blender> "))
                await self.handle_command(line)
            except EOFError:
                self.stop_event.set()
            except KeyboardInterrupt:
                self.stop_event.set()
            except Exception as e:
                print(f"Error: {e}")

    async def run(self):
        loop = asyncio.get_event_loop()
        for sig in (signal.SIGINT, signal.SIGTERM):
            loop.add_signal_handler(sig, self.stop_event.set)

        await self.scan_and_connect()
        if self.stop_event.is_set():
            return

        cli_task = asyncio.create_task(self.cli_loop())
        await self.stop_event.wait()
        cli_task.cancel()

        if self.client and self.client.is_connected:
            await self.client.disconnect()
            print("Disconnected.")


def main():
    ble = BlenderBLE()
    asyncio.run(ble.run())


if __name__ == "__main__":
    main()
