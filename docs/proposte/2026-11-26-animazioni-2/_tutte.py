# Costruisce tutte.html: un file solo, con CSS e JS dentro e ogni mockup in un iframe srcdoc (la master lo manda da solo).
import html, pathlib
d = pathlib.Path(__file__).parent
css = (d / '_motion.css').read_text(encoding='utf-8')
js = (d / '_motion.js').read_text(encoding='utf-8')
voci = [('01-messaggio', 'Messaggio che entra'), ('02-barra-master', 'Barra della master che si allunga'), ('03-pila', 'Per te come una pila'), ('04-annulla', 'Annulla col tempo'),
        ('05-puntini', 'Puntini del visore'), ('06-suggerimento', 'Suggerimento nel campo'), ('07-limite', 'Limite ad anello'), ('08-polso', 'Domanda che arriva al polso')]
figs = []
for i, (f, t) in enumerate(voci, 1):
    page = (d / f'{f}.html').read_text(encoding='utf-8')
    page = page.replace('<link rel="stylesheet" href="_motion.css">', f'<style>{css}</style>').replace('<script src="_motion.js"></script>', f'<script>{js}</script>')
    figs.append(f'  <figure><iframe title="{html.escape(t)}" srcdoc="{html.escape(page, quote=True)}"></iframe><figcaption>{i} · {html.escape(t)}</figcaption></figure>')
out = f'''<!doctype html><html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Animazioni, seconda proposta</title>
<style>{css}
  body {{ width: auto; max-width: 1560px; padding: 20px 16px; }}
  h1 {{ font: 700 26px/1.2 var(--display); }}
  .sub {{ color: var(--tx2); margin: 4px 0 18px; max-width: 70ch; }}
  .grid {{ display: flex; flex-wrap: wrap; gap: 16px; }}
  figure {{ margin: 0; }}
  iframe {{ width: 360px; height: 720px; border: 1px solid var(--line); border-radius: 14px; background: var(--bg); display: block; }}
  figcaption {{ font: 400 12.5px/1.4 var(--mono); color: var(--tx3); margin-top: 6px; }}
</style></head><body>
<h1>Animazioni, seconda proposta</h1>
<p class="sub">Otto voci nuove, diverse dalle otto già scelte, con le stesse molle e gli stessi tempi dell'app. Ognuna si ripete da sola; dentro c'è il riquadro per le animazioni ridotte. Costi e opzioni A-D sono in proposta.md.</p>
<div class="grid">
{chr(10).join(figs)}
</div>
</body></html>
'''
(d / 'tutte.html').write_text(out, encoding='utf-8', newline='\n')
print(len(out))
