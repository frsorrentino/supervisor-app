#!/usr/bin/env python3
"""Conta i pixel dei colori vecchi (CmColors) e dei token v3 in ogni immagine di prova del telefono.
Uso: python3 docs/v3-misura.py [cartella immagini]; esce 1 se una voce resta vecchia (old >= 1,5 %, oppure nero puro >= 50 %).
GiornoV3Test e WidgetConfigScreenTest si saltano: il primo è il giorno, il secondo aspetta la scelta sui widget."""
import glob, os, re, sys
from PIL import Image

D = sys.argv[1] if len(sys.argv) > 1 else 'mobile/src/test/snapshots/images'
old = {int(h, 16): n for n, h in re.findall(r'val (\w+) = Color\(0xFF([0-9A-F]{6})\)', open('ui-tokens/src/main/kotlin/it/pixelbox/cmwatch/ui/tokens/CmColors.kt').read())}
v3 = [0x0B0C0E, 0x121316, 0x1B1D22, 0x272A31, 0x25272D, 0xEDEEF0, 0x969BA4, 0x858A93, 0xFFB23F, 0x5AA2FF, 0x34D399, 0xFF5A5F]
for h in v3 + [0xFFFFFF]: old.pop(h, None)
bad = 0
for f in sorted(glob.glob(D + '/*.png')):
    name = os.path.basename(f).split('mobile_')[-1][:-4]
    if name.startswith(('GiornoV3Test', 'WidgetConfigScreenTest')): continue
    im = Image.open(f).convert('RGB'); n = im.width * im.height
    c = {(r << 16) | (g << 8) | b: k for k, (r, g, b) in im.getcolors(1 << 24)}
    o = 100 * sum(k for h, k in c.items() if h in old and h != 0) / n
    black = 100 * c.get(0, 0) / n
    if o >= 1.5 or black >= 50:
        bad += 1; print(f'vecchio {name} old={o:.1f} nero={black:.1f}')
print('vecchie:', bad)
sys.exit(1 if bad else 0)
