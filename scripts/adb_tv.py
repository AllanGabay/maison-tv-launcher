"""Target one explicitly selected TV; never scan a network or change packages automatically."""
import argparse
from pathlib import Path
import shutil
import subprocess

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--address', required=True, help='TV IPv4 address and ADB port, e.g. TV_IP:5555')
parser.add_argument('--adb', default=shutil.which('adb') or str(root / 'tools' / 'platform-tools' / 'adb.exe'))
parser.add_argument('command', nargs=argparse.REMAINDER)
args = parser.parse_args()
if not args.command:
    parser.error('Supply an ADB command after --address, for example shell getprop ro.product.model')
subprocess.run([args.adb, 'connect', args.address], check=True)
raise SystemExit(subprocess.run([args.adb, '-s', args.address, *args.command]).returncode)
