# Il celeste del colore d'accento dell'app (--icon #A8C7FA, --primary #D3E3FD) per la master (Franz, 06/10 21:19).
import math, subprocess
from PIL import Image, ImageDraw, ImageFont
from tonalita import hexc, fam, FLAT

def oklch(h):
    lin = lambda c: c/12.92 if c <= 0.04045 else ((c+0.055)/1.055)**2.4
    r, g, b = [lin(int(h[i:i+2], 16)/255) for i in (1, 3, 5)]
    l = (0.4122214708*r+0.5363325363*g+0.0514459929*b)**(1/3); m = (0.2119034982*r+0.6806995451*g+0.1073969566*b)**(1/3); s = (0.0883024619*r+0.2817188376*g+0.6299787005*b)**(1/3)
    L = 0.2104542553*l+0.7936177850*m-0.0040720468*s; a = 1.9779984951*l-2.4285922050*m+0.4505937099*s; bb = 0.0259040371*l+0.7827717662*m-0.8086757660*s
    return L, math.hypot(a, bb), math.degrees(math.atan2(bb, a)) % 360
H = oklch('#A8C7FA')[2]
print('tinta del celeste', round(H, 1))
V = [
    ('c1', 'Celeste velato', fam(H, .04), ''),
    ('c2', 'Celeste contenitore', fam(H, .065, tab=.40, disc=.47, sheet=.33, field=.262, chat=.225, raise_=.36), ''),
    ('c3', 'Celeste solo linguetta', dict(fam(262, .014), tab=hexc(.40, .07, H), disc=hexc(.47, .07, H)), ''),
    ('c4', 'Celeste pieno (l’accento)', dict(fam(H, .04), tab='#A8C7FA', disc='#C9DCFC'),
     '.dock .mono { color: #20365C !important; } .dock b { color: #0A2050 !important; } .dock .ib { color: #0A2050 !important; }'),
]
for k, name, t, extra in V:
    css = ":root { --master-tab: %(tab)s; --master-disc: %(disc)s; --master-sheet: %(sheet)s; --master-well: %(field)s; --master-raise: %(raise_)s; --m-chat: %(chat)s; }\n" % t + FLAT + extra + "\n"
    open(f'concept-{k}.css', 'w').write(f"/* {name} */\n" + css)
    subprocess.run(['sh', 'render.sh', k], check=True, capture_output=True); print(k, 'ok', flush=True)
# tavola: velo blu (t3) e i quattro celesti, chiusa sopra e aperta sotto
f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 26)
fm = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf', 19)
import tonalita
items = [('t3', 'Velo blu', next(t for k, _, t in tonalita.V if k == 't3'))] + [(k, n, t) for k, n, t, _ in V]
cols = []
for k, n, t in items:
    h = Image.open(f'shot-{k}-home.png').convert('RGB'); h = h.crop((0, h.height - 760, h.width, h.height)).resize((412, 380))
    o = Image.open(f'shot-{k}-open.png').convert('RGB').resize((412, 915))
    cols.append((k, n, t, h, o))
pad, lab = 12, 84
out = Image.new('RGB', (len(cols)*(412+pad)+pad, lab + 380 + pad + 915 + pad*2), (11, 11, 15)); d = ImageDraw.Draw(out)
for i, (k, n, t, h, o) in enumerate(cols):
    x = pad + i*(412+pad)
    d.text((x+4, 8), n, fill=(242, 244, 247), font=f); d.text((x+4, 44), f"{t['tab']} {t['sheet']}", fill=(176, 184, 196), font=fm)
    out.paste(h, (x, lab)); out.paste(o, (x, lab + 380 + pad))
out.save('tonalita-celeste.png'); print('tonalita-celeste.png', out.size)
