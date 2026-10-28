#!/bin/sh
# Rende un concept sulla web app vera, alla misura del telefono (412×915, densità 2):
#   sh render.sh <k>   →  shot-<k>-home.png (master chiusa), shot-<k>-open.png (master aperta), concept-<k>.png (affiancate)
# Il CSS sta in concept-<k>.css accanto a questo file; il server è vite preview su :4179 (web/dist, con mc → questa cartella).
set -e
D=$(cd "$(dirname "$0")" && pwd)
K=${1:?serve il nome del concept}
for o in 0 1; do
  n=home; [ "$o" = 1 ] && n=open
  timeout 150 chromium --headless=new --no-sandbox --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
    --window-size=412,915 --virtual-time-budget=9000 --screenshot="$D/shot-$K-$n.png" \
    "http://localhost:4179/concept.html?k=$K&open=$o" >/dev/null 2>&1
done
python3 - "$D" "$K" <<'PY'
import sys
from PIL import Image, ImageDraw, ImageFont
d, k = sys.argv[1], sys.argv[2]
a = Image.open(f"{d}/shot-{k}-home.png").convert("RGB"); b = Image.open(f"{d}/shot-{k}-open.png").convert("RGB")
W, H = a.width + b.width + 60, max(a.height, b.height) + 110
c = Image.new("RGB", (W, H), (11, 11, 15)); dr = ImageDraw.Draw(c)
f = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 40)
dr.text((20, 30), f"{k} · master chiusa", fill=(242, 244, 247), font=f)
dr.text((a.width + 40, 30), f"{k} · master aperta", fill=(242, 244, 247), font=f)
c.paste(a, (20, 100)); c.paste(b, (a.width + 40, 100)); c.save(f"{d}/concept-{k}.png")
print(f"{d}/concept-{k}.png")
PY
