# Varianti di tonalità piatte per la master (Franz, 06/10 21:15): ogni variante è un gruppo di token, rese sulla web app vera.
import math, subprocess, sys
from PIL import Image, ImageDraw, ImageFont

def hexc(L, C, H):
    a = C * math.cos(math.radians(H)); b = C * math.sin(math.radians(H))
    l_ = L + 0.3963377774*a + 0.2158037573*b; m_ = L - 0.1055613458*a - 0.0638541728*b; s_ = L - 0.0894841775*a - 1.2914855480*b
    l, m, s = l_**3, m_**3, s_**3
    rgb = (4.0767416621*l - 3.3077115913*m + 0.2309699292*s, -1.2684380046*l + 2.6097574011*m - 0.3413193965*s, -0.0041960863*l - 0.7034186147*m + 1.7076147010*s)
    enc = lambda x: round(255 * (12.92*x if x <= 0.0031308 else 1.055*x**(1/2.4) - 0.055)) if 0 <= x <= 1 else (0 if x < 0 else 255)
    return '#%02X%02X%02X' % tuple(enc(x) for x in rgb)

def fam(H, C, tab=.33, disc=.40, sheet=.285, field=.238, chat=.205, raise_=.31):
    return dict(tab=hexc(tab, C, H), disc=hexc(disc, C, H), sheet=hexc(sheet, C, H), field=hexc(field, C, H), chat=hexc(chat, C, H), raise_=hexc(raise_, C, H))

V = [
    ('t1', 'Grafite delle card', fam(262, .014)),
    ('t2', 'Token dell’app', dict(tab='#292F3A', disc='#343A46', sheet='#23272E', field='#1B1F26', chat='#14171C', raise_='#292F3A')),
    ('t3', 'Velo blu (primario)', fam(255, .03)),
    ('t4', 'Ardesia', fam(215, .014)),
    ('t5', 'Antracite neutra', fam(0, 0)),
    ('t6', 'Calda appena', fam(70, .008)),
    ('t7', 'Contrasto alto', fam(262, .016, tab=.37, disc=.44, sheet=.32, field=.25, chat=.225, raise_=.35)),
    ('t8', 'Sommessa', fam(262, .012, tab=.295, disc=.36, sheet=.262, field=.225, chat=.19, raise_=.285)),
    ('t9', 'Notte (più scura)', fam(262, .012, tab=.26, disc=.33, sheet=.2, field=.238, chat=.15, raise_=.25)),
]

FLAT = """.slide { animation: none !important; }
.pane > .closed { background: var(--master-sheet) !important; box-shadow: none !important; }
.dock { background: var(--master-tab) !important; box-shadow: none !important; }
.dock::after { content: none !important; }
.dock .ib { box-shadow: none !important; }
.field.master { box-shadow: none !important; border: 0 !important; }
.chat.master { background: var(--m-chat) !important; }
.chat.master > header { box-shadow: none !important; border-bottom: 1px solid var(--line) !important; }
.chat.master header .pill, .chat.master .me .bubble, .chat.master .play, .chat.master .steps, .chat.master .box { box-shadow: none !important; }
.chat.master .steps { background: var(--master-sheet) !important; }
.chat.master > .bar { background: var(--master-sheet) !important; box-shadow: none !important; }
"""

if __name__ == '__main__':
    only = sys.argv[1:] or [k for k, _, _ in V]
    for k, name, t in V:
        if k not in only: continue
        css = ":root { --master-tab: %(tab)s; --master-disc: %(disc)s; --master-sheet: %(sheet)s; --master-well: %(field)s; --master-raise: %(raise_)s; --m-chat: %(chat)s; }\n" % t + FLAT
        open(f'concept-{k}.css', 'w').write(f"/* {name} */\n" + css)
        subprocess.run(['sh', 'render.sh', k], check=True, capture_output=True)
        print(k, 'ok', flush=True)
    # tavole: chiusa (fondo della home) e aperta (pagina intera), 3×3
    f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 30)
    fm = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf', 22)
    for which, crop in (('home', 760), ('open', None)):
        tiles = []
        for k, name, t in V:
            im = Image.open(f'shot-{k}-{which}.png').convert('RGB')
            if crop: im = im.crop((0, im.height - crop, im.width, im.height))
            else: im = im.resize((im.width // 2, im.height // 2))
            tiles.append((k, name, t, im))
        tw, th = tiles[0][3].size
        cols = 3; rows = 3; pad = 14; lab = 92
        out = Image.new('RGB', (cols * (tw + pad) + pad, rows * (th + lab + pad) + pad), (11, 11, 15)); d = ImageDraw.Draw(out)
        for i, (k, name, t, im) in enumerate(tiles):
            x = pad + (i % cols) * (tw + pad); y = pad + (i // cols) * (th + lab + pad)
            d.text((x + 6, y + 4), f"{k[1:]} · {name}", fill=(242, 244, 247), font=f)
            d.text((x + 6, y + 46), f"{t['tab']} {t['sheet']} {t['field']} {t['chat']}", fill=(176, 184, 196), font=fm)
            out.paste(im, (x, y + lab))
        out.save(f'tonalita-{which}.png'); print(f'tonalita-{which}.png', out.size)
