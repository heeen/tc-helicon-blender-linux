#!/usr/bin/env python3
"""Canonical Blender host tool entrypoint."""

from __future__ import annotations

import sys

import jtag_flash_v2
import jtag_reboot


def main(argv=None):
    args = list(sys.argv[1:] if argv is None else argv)
    if not args or args[0] in ("-h", "--help"):
        print("usage: blender_tool.py {flash,reboot} ...")
        print()
        print("Canonical Blender host tool entrypoint.")
        print()
        print("subcommands:")
        print("  flash    Run v2 flash workflow")
        print("  reboot   Run reboot-stub workflow")
        return 0

    cmd, rest = args[0], args[1:]
    if cmd == "flash":
        return jtag_flash_v2.main(rest)
    if cmd == "reboot":
        return jtag_reboot.main(rest)
    print(f"unknown subcommand: {cmd}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
