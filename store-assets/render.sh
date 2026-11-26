#!/bin/zsh
# Renders store art with headless Chrome. Usage: store-assets/render.sh   (run from repo root)
set -e
C="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"; D="$PWD/store-assets"
shot() { "$C" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --default-background-color=00000000 --screenshot="$2" --window-size=$3 "file://$1" >/dev/null 2>&1; }
shot "$D/icon.svg" "$D/icon-512.png" 512,512
shot "$D/mascot/tally-1024.html" "$D/mascot/tally-1024.png" 1024,1024
shot "$D/mascot/tally-512.html" "$D/mascot/tally-512.png" 512,512
shot "$D/feature.html" "$D/feature-graphic-1024x500.png" 1024,500
mkdir -p "$D/mascot/expressions"
