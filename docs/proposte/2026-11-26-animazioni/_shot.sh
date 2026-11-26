#!/usr/bin/env bash
# PNG a grandezza reale (1 px CSS = 1 px, 360 di larghezza) del fotogramma fermo di un mockup: ./_shot.sh 01-copia 520
E="/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"
PD="${TMPDIR:-/tmp}/edge-mockup"
W=$(cygpath -m "$PWD")
timeout 60 "$E" --headless=new --disable-gpu --hide-scrollbars --user-data-dir="$PD" --force-device-scale-factor=1 --window-size=500,"$2" --virtual-time-budget=4000 --screenshot="$W/$1.full.png" "file:///$W/$1.html?still" >/dev/null 2>&1
python -c "from PIL import Image;import sys;im=Image.open(sys.argv[1]);im.crop((0,0,360,im.height)).save(sys.argv[2]);" "$1.full.png" "$1.png" && rm -f "$1.full.png"
