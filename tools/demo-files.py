#!/usr/bin/env python3
"""I file della demo web (Franz, 15/10 22:20: «anche nella demo le anteprime»): un'immagine, un PDF di una pagina, un
markdown e uno zip, serviti da web/public/demo-files. Rilanciare dopo un cambio e aggiornare le misure in demo.ts."""
import io, os, zipfile
from PIL import Image, ImageDraw, ImageFont
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas

OUT = os.path.join(os.path.dirname(__file__), '..', 'web', 'public', 'demo-files')
os.makedirs(OUT, exist_ok=True)

def font(size, bold=False):
    for p in ['/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']:
        if os.path.exists(p): return ImageFont.truetype(p, size)
    return ImageFont.load_default()

# Il checkout prima del refactor: una schermata finta, leggibile anche in piccolo.
W, H = 960, 600
im = Image.new('RGB', (W, H), (246, 247, 249))
d = ImageDraw.Draw(im)
d.rectangle([0, 0, W, 64], fill=(31, 41, 55))
d.text((32, 18), 'storefront · Checkout', font=font(26, True), fill=(255, 255, 255))
d.rounded_rectangle([32, 96, 600, 560], 16, fill=(255, 255, 255), outline=(225, 228, 234))
d.text((56, 120), 'Your cart', font=font(28, True), fill=(17, 24, 39))
rows = [('Edge caching handbook', '€ 24.00'), ('Gift card', '€ 50.00'), ('Shipping', '€ 4.90')]
for i, (a, b) in enumerate(rows):
    y = 180 + i * 64
    d.text((56, y), a, font=font(22), fill=(55, 65, 81)); d.text((480, y), b, font=font(22), fill=(55, 65, 81))
    d.line([56, y + 44, 576, y + 44], fill=(235, 237, 241), width=2)
d.text((56, 390), 'Total', font=font(26, True), fill=(17, 24, 39)); d.text((468, 390), '€ 78.90', font=font(26, True), fill=(17, 24, 39))
d.rounded_rectangle([56, 460, 576, 524], 12, fill=(79, 70, 229))
d.text((236, 476), 'Pay now', font=font(26, True), fill=(255, 255, 255))
d.rounded_rectangle([632, 96, 928, 300], 16, fill=(254, 242, 242), outline=(252, 165, 165))
d.text((656, 120), 'Webhook check', font=font(22, True), fill=(153, 27, 27))
for i, line in enumerate(['signature verified', 'in two places: moved', 'into the handler']):
    d.text((656, 168 + i * 34), line, font=font(20), fill=(127, 29, 29))
im.save(os.path.join(OUT, 'checkout-before.png'), optimize=True)

# Il resoconto dei test: un PDF vero, di una pagina, con testo.
c = canvas.Canvas(os.path.join(OUT, 'checkout-tests.pdf'), pagesize=A4)
w, h = A4
c.setFillColorRGB(0.12, 0.16, 0.22); c.rect(0, h - 90, w, 90, fill=1, stroke=0)
c.setFillColorRGB(1, 1, 1); c.setFont('Helvetica-Bold', 22); c.drawString(48, h - 58, 'Checkout test report')
c.setFillColorRGB(0.1, 0.1, 0.1); c.setFont('Helvetica', 12)
c.drawString(48, h - 130, 'storefront · after the Stripe webhook refactor')
y = h - 180
for name, res in [('cart totals', 'passed'), ('gift card redeem', 'passed'), ('refund webhook', 'passed'), ('forged signature is rejected', 'passed'),
                  ('saved carts migration 011', 'passed'), ('checkout on staging', 'passed')]:
    c.setFont('Helvetica', 13); c.drawString(64, y, name)
    c.setFillColorRGB(0.09, 0.5, 0.25); c.drawString(400, y, res); c.setFillColorRGB(0.1, 0.1, 0.1)
    y -= 30
c.setFont('Helvetica-Bold', 14); c.drawString(48, y - 20, '214 tests, 0 failed, 41.2 s')
c.showPage(); c.save()

# Il post del blog, in markdown.
open(os.path.join(OUT, 'edge-caching.md'), 'w').write("""# Edge caching, three ways

Most pages on the shop change once a day, but we rendered them on every request.

## 1. Cache the HTML at the edge
A short `s-maxage` and `stale-while-revalidate` cut the median response from 410 ms to 38 ms.

## 2. Keep the cart out of the cache
The cart is the one personal part: it loads after the page, from its own endpoint.

## 3. Purge on publish
Publishing a product purges its page and the category, nothing else.

| page | before | after |
|---|---|---|
| home | 410 ms | 38 ms |
| product | 520 ms | 41 ms |
""")

# L'anteprima del post come pagina HTML (Franz, 16/10 11:18: si vede renderizzata, in un riquadro sandbox).
HTML = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>Edge caching, three ways</title>
<style>body{font:16px/1.5 system-ui,sans-serif;margin:24px;color:#1b1b1f;background:#fafafa}h1{font-size:24px;margin:0 0 8px}
p{margin:0 0 12px;color:#44474e}table{border-collapse:collapse}td,th{padding:6px 14px;border-bottom:1px solid #ddd;text-align:left}
.after{color:#18803f;font-weight:600}</style></head>
<body><h1>Edge caching, three ways</h1><p>Most pages on the shop change once a day, but we rendered them on every request.</p>
<table><tr><th>page</th><th>before</th><th>after</th></tr><tr><td>home</td><td>410 ms</td><td class="after">38 ms</td></tr>
<tr><td>product</td><td>520 ms</td><td class="after">41 ms</td></tr></table>
<p id="js"></p><script>document.getElementById('js').textContent = 'Rendered ' + new Date().getFullYear()</script></body></html>
"""
open(os.path.join(OUT, 'edge-caching-preview.html'), 'w').write(HTML)

# I dati del benchmark: uno zip, che resta una scheda larga senza anteprima.
buf = io.BytesIO()
with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('bench.csv', 'page,before_ms,after_ms\nhome,410,38\nproduct,520,41\n')
open(os.path.join(OUT, 'bench-data.zip'), 'wb').write(buf.getvalue())

for f in sorted(os.listdir(OUT)):
    print(f, os.path.getsize(os.path.join(OUT, f)))
