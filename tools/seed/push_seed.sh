#!/bin/zsh
# Pushes demo data (made by make_seed.py) into the DEBUG app on the connected emulator. Usage: push_seed.sh <seed-dir>
set -e
seed="${1:?seed dir}"; pkg=com.wizlabs.billbox
adb shell am force-stop $pkg
adb shell rm -rf /data/local/tmp/seed; adb push "$seed" /data/local/tmp/seed >/dev/null
adb shell "run-as $pkg sh -c 'rm -rf files/photos files/docs files/bills.json files/demo-mode; mkdir -p files/photos files/docs; cp /data/local/tmp/seed/photos/* files/photos/ 2>/dev/null; cp /data/local/tmp/seed/docs/* files/docs/ 2>/dev/null; cp /data/local/tmp/seed/bills.json files/bills.json; cp /data/local/tmp/seed/demo-mode files/demo-mode'"
echo seeded
