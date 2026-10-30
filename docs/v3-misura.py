#!/usr/bin/env python3
"""Conta i pixel dei colori vecchi (CmColors) e dei token v3 in ogni immagine di prova del telefono.
Uso: python3 docs/v3-misura.py [cartella immagini]; esce 1 se una voce resta vecchia (old >= 1,5 %, oppure nero puro >= 50 %).
GiornoV3Test si salta: è il giorno."""
import glob, os, sys
from PIL import Image

D = sys.argv[1] if len(sys.argv) > 1 else 'mobile/src/test/snapshots/images'
old = {int(h, 16): h for h in '000000 001C33 072510 0A2050 12141A 141A24 1B1F26 1F1147 23272E 24161A 292F3A 2A1A00 2A2E35 3457D5 34C759 43A047 455165 4C7DFF 5B6578 5F1412 65C581 7DCCFB 7FA1FF 8AB4F8 8BB4F7 98A2B3 9CA3AF A8C7FA B0B8C4 B9A6F5 BCC0CB BCE4C7 C2554D C58AF9 D3E3FD D97757 E3E3E8 E53935 E5736B F2B8B5 F2F4F7 F4F4F4 FF453A FFB020 FFC46B FFE08A'.split()}  # la palette di CmColors prima del 30/10
v3 = [0x0B0C0E, 0x121316, 0x1B1D22, 0x272A31, 0x25272D, 0xEDEEF0, 0x969BA4, 0x858A93, 0xFFB23F, 0x5AA2FF, 0x34D399, 0xFF5A5F]
for h in v3 + [0xFFFFFF]: old.pop(h, None)
fs = sorted(glob.glob(D + '/*cmwatch.mobile_*.png'))
if not fs: sys.exit('nessuna immagine in ' + D)
bad = 0
for f in fs:
    name = os.path.basename(f).split('mobile_')[-1][:-4]
    if name.startswith(('GiornoV3Test',)): continue
    im = Image.open(f).convert('RGB'); n = im.width * im.height
    c = {(r << 16) | (g << 8) | b: k for k, (r, g, b) in im.getcolors(1 << 24)}
    o = 100 * sum(k for h, k in c.items() if h in old and h != 0) / n
    black = 100 * c.get(0, 0) / n
    if o >= 1.5 or black >= 50:
        bad += 1; print(f'vecchio {name} old={o:.1f} nero={black:.1f}')
print('vecchie:', bad)
sys.exit(1 if bad else 0)
