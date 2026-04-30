#!/usr/bin/env python3
import argparse
import json
import re
import subprocess
import time
import usb.core

LOG_PATH = "/home/florian/src-misc/helicon-blender/.cursor/debug-5388fd.log"
SESSION_ID = "5388fd"


def dbg(run_id: str, hypothesis_id: str, location: str, message: str, data: dict) -> None:
    # region agent log
    payload = {
        "sessionId": SESSION_ID,
        "runId": run_id,
        "hypothesisId": hypothesis_id,
        "location": location,
        "message": message,
        "data": data,
        "timestamp": int(time.time() * 1000),
    }
    with open(LOG_PATH, "a", encoding="utf-8") as f:
        f.write(json.dumps(payload, separators=(",", ":")) + "\n")
    # endregion


def main() -> int:
    ap = argparse.ArgumentParser(description="Diagnose MIDI EP timeout with UART + USB evidence")
    ap.add_argument("--run-id", default=f"diag-{int(time.time())}")
    ap.add_argument("--uart-seconds", type=float, default=10.0)
    ap.add_argument("--iface", type=int, default=4)
    ap.add_argument("--ep", type=lambda x: int(x, 0), default=0x03)
    ap.add_argument("--timeout-ms", type=int, default=800)
    ap.add_argument("--vid", type=lambda x: int(x, 0), default=0x1220)
    ap.add_argument("--pid", type=lambda x: int(x, 0), default=0x8FE1)
    ap.add_argument("--wait-device-seconds", type=float, default=8.0)
    ap.add_argument("--wait-for-power", action="store_true",
                    help="Print power/reset instructions and wait for Enter before probing")
    args = ap.parse_args()

    dbg(args.run_id, "H8", "midi_timeout_diag.py:start", "diag_start", {
        "uart_seconds": args.uart_seconds,
        "iface": args.iface,
        "ep": args.ep,
        "timeout_ms": args.timeout_ms,
    })

    if args.wait_for_power:
        print("[diag] Step 1: keep device OFF/reset now.")
        print("[diag] Step 2: press Enter here, then immediately power on/reset device.")
        input("[diag] Press Enter to arm capture: ")
        print("[diag] Capture armed. Power on/reset NOW.")

    uart = subprocess.Popen(
        ["python3", "firmware/uart_read.py", str(args.uart_seconds)],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    t0 = time.monotonic()
    found = False
    while time.monotonic() - t0 < args.wait_device_seconds:
        if usb.core.find(idVendor=args.vid, idProduct=args.pid) is not None:
            found = True
            break
        time.sleep(0.2)
    dbg(args.run_id, "H9", "midi_timeout_diag.py:wait-device", "device_wait_result", {
        "found": found,
        "wait_seconds": round(time.monotonic() - t0, 3),
        "vid": args.vid,
        "pid": args.pid,
    })
    probe = subprocess.run(
        [
            "python3",
            "firmware/usb_midi_bulk_send.py",
            "--iface", str(args.iface),
            "--ep", hex(args.ep),
            "--timeout-ms", str(args.timeout_ms),
        ],
        capture_output=True,
        text=True,
    )
    uart_out, uart_err = uart.communicate(timeout=max(5.0, args.uart_seconds + 2.0))

    wrote = probe.stdout.count("WROTE")
    tout = probe.stdout.count("TIMEOUT")
    uerr = probe.stdout.count("USBERR")
    dbg(args.run_id, "H9", "midi_timeout_diag.py:usb", "usb_probe_summary", {
        "exit_code": probe.returncode,
        "wrote": wrote,
        "timeout": tout,
        "usberr": uerr,
        "stdout": probe.stdout.strip().splitlines()[-6:],
        "stderr": probe.stderr.strip().splitlines()[-4:],
    })
    print(f"[diag] usb exit={probe.returncode} wrote={wrote} timeout={tout} usberr={uerr}")
    if probe.stdout.strip():
        print("[diag] usb tail:")
        for ln in probe.stdout.strip().splitlines()[-6:]:
            print(f"  {ln}")
    if probe.stderr.strip():
        print("[diag] usb stderr tail:")
        for ln in probe.stderr.strip().splitlines()[-4:]:
            print(f"  {ln}")

    saw_cb = "[midi] cb patched" in uart_out
    ec1_lines = [ln for ln in uart_out.splitlines() if " s=" in ln and " f=" in ln and " q0=" in ln]
    ec2_lines = [ln for ln in uart_out.splitlines() if ln.startswith("EC2 ")]
    ec3_lines = [ln for ln in uart_out.splitlines() if ln.startswith("EC3 ")]
    midi_lines = [ln for ln in uart_out.splitlines() if "[midi]" in ln]
    last_ec1 = ec1_lines[-1] if ec1_lines else ""
    last_ec2 = ec2_lines[-1] if ec2_lines else ""
    last_ec3 = ec3_lines[-1] if ec3_lines else ""

    def _last(field, line):
        m = re.search(rf"\b{field}=([0-9A-Fa-f]+)", line)
        return m.group(1) if m else ""

    # EC1 columns under DWC2 schema:
    #   f= start_fn   qp= ep_regs (DOEPCTL3 MMIO)
    #   q0= DOEPCTL3  q2= DOEPINT3   q4= DOEPTSIZ3   q5= DOEPDMA3
    #   da= DAINT     dam= DAINTMSK
    #   dim= DIEPMSK  dom= DOEPMSK   dem= DIEPEMPMSK   th= DTHRCTL
    f_val   = _last("f",   last_ec1)
    qp_val  = _last("qp",  last_ec1)
    q0_val  = _last("q0",  last_ec1)
    q2_val_s = _last("q2", last_ec1)
    q4_val  = _last("q4",  last_ec1)
    q5_val  = _last("q5",  last_ec1)
    da_val  = _last("da",  last_ec1)
    dam_val = _last("dam", last_ec1)
    dim_val = _last("dim", last_ec1)
    dom_val = _last("dom", last_ec1)
    dem_val = _last("dem", last_ec1)
    th_val  = _last("th",  last_ec1)

    # EC2 columns: u= eCos conn state, g= speed byte, es= DSTS.EnumSpd
    # decoded (H/F/L/X), dcfg/dctl/dsts, plus ep_handle fields bp/bs/hl/F/mps.
    es_match = re.search(r"\bes=([HFLX])", last_ec2)
    es_val   = es_match.group(1) if es_match else ""
    dcfg_val = _last("dcfg", last_ec2)
    dctl_val = _last("dctl", last_ec2)
    dsts_val = _last("dsts", last_ec2)
    bp_val   = _last("bp",   last_ec2)
    bs_val   = _last("bs",   last_ec2)
    mps_val  = _last("mps",  last_ec2)

    # EC3 columns: ha= GAHBCFG, us= GUSBCFG, gis/gim/grf/gnt/gns,
    # hc2/hc3 = GHWCFG2/3, sid= GSNPSID, gdc= GDFIFOCFG.
    gahbcfg = _last("ha",  last_ec3)
    gusbcfg = _last("us",  last_ec3)
    gintsts = _last("gis", last_ec3)
    gintmsk = _last("gim", last_ec3)
    grxfsiz = _last("grf", last_ec3)
    gsnpsid = _last("sid", last_ec3)

    # 2026-04-22: scan any UART line for `otc=NNNNNNNN` — OUT-direction trace
    # counter. Use the MAX observed value across the capture.
    def _max_field(name):
        vals = [int(m, 16) for m in re.findall(rf"\b{name}=([0-9A-Fa-f]{{8}})", uart_out)]
        return max(vals) if vals else None

    otc_count = _max_field("otc")
    cmc_count = _max_field("cmc")    # completion callback invocations
    q2c_count = _max_field("q2c")    # DOEPINT3 value-change observations
    css_count = _max_field("css")    # DAINT bit 19 sightings
    cfc_count = _max_field("cfc")    # complete_fn observed cleared (= ISR ran cmp)
    fr_count  = _max_field("fr")     # forced re-arm attempts (legacy DescDMA path; should stay 0)
    act_count = _max_field("act")    # DOEPCTL3 EPENA|CNAK|USBACTEP re-set pokes
    xc_count  = _max_field("xc")     # DOEPINT3.XFERCOMPL seen
    od_count  = _max_field("od")     # DOEPINT3.OUTTKNEPDIS seen
    # Slave-mode HS arming (DWC2 v3.20a) — added 2026-04-28
    hsa_count = _max_field("hsa")    # full slave-mode arm sequences (boot + EPENA-clear edges)
    hsr_count = _max_field("hsr")    # re-arms after EPENA observed clear
    dfx_count = _max_field("dfx")    # DCFG.DevSpd→HS fix-ups applied
    rfc_count = _max_field("rfc")    # GINTSTS.RXFLVL transitions seen

    installed = "[midi] installed OUT trace as start_fn" in uart_out
    cmp_installed = "[midi] cmp wrapper installed" in uart_out
    cmp_lines = [ln for ln in uart_out.splitlines() if "[midi] cmp " in ln]

    # DOEPINT3 bit decode for q2 reporting.
    q2_bits_known = {
        0x0001: "XFERCOMPL",
        0x0002: "EPDISBLD",
        0x0004: "AHBERR",
        0x0008: "SETUP",
        0x0010: "OUTTKNEPDIS",
        0x0020: "STSPHSERCVD",
        0x2000: "NAKINTRPT",
        0x4000: "NYETINTRPT",
        0x8000: "SETUP_RCVD",
    }
    q2_val = int(q2_val_s, 16) if q2_val_s else 0
    q2_decoded = ",".join(name for bit, name in q2_bits_known.items() if q2_val & bit)
    q2_unknown = q2_val & ~0xFF  # bits outside the known low-byte

    # GSNPSID sanity: should read 0x4F54320A ("OT2\n") for DWC2 v3.20a.
    gsnpsid_ok = (gsnpsid.lower() == "4f54320a") if gsnpsid else False

    dbg(args.run_id, "H10", "midi_timeout_diag.py:uart", "uart_state_summary", {
        "saw_cb_patched": saw_cb,
        "out_trace_installed": installed,
        "ec1_count": len(ec1_lines),
        "ec2_count": len(ec2_lines),
        "ec3_count": len(ec3_lines),
        "midi_lines_tail": midi_lines[-6:],
        "f": f_val, "qp": qp_val,
        "q0": q0_val, "q2": q2_val_s, "q4": q4_val, "q5": q5_val,
        "q2_decoded": q2_decoded,
        "q2_unknown_hi_bits": f"{q2_unknown:08x}" if q2_unknown else "",
        "da": da_val, "dam": dam_val,
        "dim": dim_val, "dom": dom_val, "dem": dem_val, "th": th_val,
        "dcfg": dcfg_val, "dctl": dctl_val, "dsts": dsts_val, "es": es_val,
        "bp": bp_val, "bs": bs_val, "mps": mps_val,
        "gahbcfg": gahbcfg, "gusbcfg": gusbcfg, "gintsts": gintsts,
        "gintmsk": gintmsk, "grxfsiz": grxfsiz,
        "gsnpsid": gsnpsid, "gsnpsid_ok": gsnpsid_ok,
        "otc": otc_count,
        "hsa": hsa_count, "hsr": hsr_count, "dfx": dfx_count, "rfc": rfc_count,
        "last_ec1": last_ec1,
        "last_ec2": last_ec2,
        "last_ec3": last_ec3,
        "uart_err_tail": uart_err.strip().splitlines()[-4:],
    })
    print(
        "[diag] uart "
        f"cb={int(saw_cb)} ec1={len(ec1_lines)} ec2={len(ec2_lines)} ec3={len(ec3_lines)} "
        f"f={f_val or 'NA'} qp={qp_val or 'NA'} "
        f"q0={q0_val or 'NA'} q2={q2_val_s or 'NA'}"
        f"{' [' + q2_decoded + ']' if q2_decoded else ''}"
        f"{' +hi=0x' + f'{q2_unknown:08x}' if q2_unknown else ''} "
        f"q4={q4_val or 'NA'} q5={q5_val or 'NA'} "
        f"da={da_val or 'NA'} dam={dam_val or 'NA'} "
        f"dom={dom_val or 'NA'} dim={dim_val or 'NA'} "
        f"es={es_val or '?'} dcfg={dcfg_val or 'NA'} dsts={dsts_val or 'NA'} "
        f"otc={otc_count if otc_count is not None else 'NA'} "
        f"cmc={cmc_count if cmc_count is not None else 'NA'} "
        f"q2c={q2c_count if q2c_count is not None else 'NA'} "
        f"css={css_count if css_count is not None else 'NA'} "
        f"cfc={cfc_count if cfc_count is not None else 'NA'} "
        f"fr={fr_count if fr_count is not None else 'NA'} "
        f"act={act_count if act_count is not None else 'NA'} "
        f"xc={xc_count if xc_count is not None else 'NA'} "
        f"od={od_count if od_count is not None else 'NA'} "
        f"hsa={hsa_count if hsa_count is not None else 'NA'} "
        f"hsr={hsr_count if hsr_count is not None else 'NA'} "
        f"dfx={dfx_count if dfx_count is not None else 'NA'} "
        f"rfc={rfc_count if rfc_count is not None else 'NA'} "
        f"inst={int(installed)} cmp_inst={int(cmp_installed)} "
        f"sid={gsnpsid or 'NA'}{'' if gsnpsid_ok or not gsnpsid else ' [unexpected]'}"
    )
    if cmp_lines:
        print("[diag] cmp lines:")
        for ln in cmp_lines[:6]:
            print(f"  {ln}")
    if midi_lines:
        print("[diag] uart midi lines tail:")
        for ln in midi_lines[-6:]:
            print(f"  {ln}")
    else:
        print("[diag] uart midi lines tail: (none)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

