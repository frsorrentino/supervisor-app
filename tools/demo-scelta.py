#!/usr/bin/env python3
"""Le immagini della scheda di scelta della demo web (app-scheda-scelta-12-11): due proposte del carrello di storefront sul
telefono, A col riepilogo in alto e B con la barra in fondo, e la proposta del primo giro, scartata. Schermate finte, leggibili
anche in piccolo, servite da web/public/demo-files. Rilanciare dopo un cambio e aggiornare le misure in demo.ts (`waitsDemo`)."""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(__file__), '..', 'web', 'public', 'demo-files')
os.makedirs(OUT, exist_ok=True)

def font(size, bold=False):
    for p in (['C:/Windows/Fonts/arialbd.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'] if bold
              else ['C:/Windows/Fonts/arial.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']):
        if os.path.exists(p): return ImageFont.truetype(p, size)
    return ImageFont.load_default()

W, H = 540, 1080
INK, MUTED, LINE, PAGE, WHITE, BRAND = (17, 24, 39), (107, 114, 128), (229, 231, 235), (246, 247, 249), (255, 255, 255), (79, 70, 229)
ITEMS = [('Edge caching handbook', 'Paperback', '€ 24.00'), ('Gift card', 'Digital', '€ 50.00'), ('Notebook, dotted', 'A5', '€ 9.00')]

def screen():
    im = Image.new('RGB', (W, H), PAGE)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, W, 96], fill=(31, 41, 55))
    d.text((28, 30), 'storefront', font=font(30, True), fill=WHITE)
    d.text((W - 150, 36), 'Cart · 3', font=font(24), fill=(209, 213, 219))
    return im, d

def item(d, y, name, kind, price, tall=True):
    h = 132 if tall else 84
    d.rounded_rectangle([24, y, W - 24, y + h], 18, fill=WHITE, outline=LINE)
    if tall:
        d.rounded_rectangle([40, y + 18, 136, y + 114], 12, fill=(238, 242, 255))
        d.text((156, y + 26), name, font=font(24, True), fill=INK)
        d.text((156, y + 62), kind, font=font(21), fill=MUTED)
        d.text((W - 136, y + 88), price, font=font(23, True), fill=INK)
    else:
        d.text((40, y + 14), name, font=font(21, True), fill=INK)
        d.text((40, y + 46), kind, font=font(18), fill=MUTED)
        d.text((W - 130, y + 28), price, font=font(21, True), fill=INK)
    return y + h + 14

def pay(d, box, label='Pay € 83.00'):
    d.rounded_rectangle(box, 16, fill=BRAND)
    f = font(26, True)
    w = d.textlength(label, font=f)
    d.text(((box[0] + box[2] - w) / 2, (box[1] + box[3]) / 2 - 15), label, font=f, fill=WHITE)

# A: il riepilogo in alto, col tasto subito sotto la testata.
im, d = screen()
d.rounded_rectangle([24, 120, W - 24, 372], 20, fill=WHITE, outline=LINE)
d.text((44, 142), 'Order summary', font=font(24, True), fill=INK)
for i, (k, v) in enumerate([('Items', '€ 83.00'), ('Shipping', 'free')]):
    d.text((44, 190 + i * 38, ), k, font=font(22), fill=MUTED); d.text((W - 160, 190 + i * 38), v, font=font(22), fill=INK)
pay(d, [44, 284, W - 44, 352])
y = 396
d.text((28, y), 'Your items', font=font(22, True), fill=MUTED); y += 44
for it in ITEMS: y = item(d, y, *it)
im.save(os.path.join(OUT, 'cart-a.png'), optimize=True)

# B: gli articoli per primi, il totale e il tasto in una barra fissa in fondo.
im, d = screen()
y = 120
d.text((28, y), 'Your items', font=font(22, True), fill=MUTED); y += 44
for it in ITEMS: y = item(d, y, *it)
d.rounded_rectangle([24, y + 6, W - 24, y + 78], 18, outline=LINE, fill=PAGE)
d.text((44, y + 28), 'Add a discount code', font=font(21), fill=MUTED)
d.rectangle([0, H - 190, W, H], fill=WHITE)
d.line([0, H - 190, W, H - 190], fill=LINE, width=2)
d.text((32, H - 168), 'Total', font=font(22), fill=MUTED); d.text((32, H - 136), '€ 83.00', font=font(32, True), fill=INK)
d.text((W - 190, H - 150), 'Shipping free', font=font(20), fill=MUTED)
pay(d, [32, H - 84, W - 32, H - 20], 'Pay now')
im.save(os.path.join(OUT, 'cart-b.png'), optimize=True)

# Il primo giro, scartato: una lista fitta, col tasto in fondo alla pagina.
im, d = screen()
y = 120
for it in ITEMS + [('Sticker pack', 'Set of 6', '€ 4.00'), ('Tote bag', 'Natural', '€ 12.00')]: y = item(d, y, *it, tall=False)
d.text((40, y + 10), 'Total', font=font(22, True), fill=INK); d.text((W - 130, y + 10), '€ 99.00', font=font(22, True), fill=INK)
pay(d, [24, y + 60, W - 24, y + 122], 'Pay')
im.save(os.path.join(OUT, 'cart-round1.png'), optimize=True)

for n in ('cart-a.png', 'cart-b.png', 'cart-round1.png'):
    print(n, os.path.getsize(os.path.join(OUT, n)))
