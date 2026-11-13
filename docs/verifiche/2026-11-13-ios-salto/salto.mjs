// app-ios-scatti-tastiera-12-11: sale una trascrizione di voci lunghe e misura il salto di una voce tenuta d'occhio, per ogni
// passo di scorrimento: salto = quanto la voce si muove sullo schermo meno quanto si è scorso. Con `cv` le voci hanno la regola
// di Feed.svelte (content-visibility: auto, 120 px stimati); senza, la correzione per i browser senza overflow-anchor.
import { webkit, chromium } from 'playwright'
const page = cv => `<!doctype html><meta name=viewport content="width=390"><style>
  body{margin:0;font:16px/24px sans-serif} #l{height:100vh;overflow:auto} .it{padding:12px 16px}
  ${cv ? '.it{content-visibility:auto;contain-intrinsic-size:auto 120px}' : ''}</style>
  <div id=l>${Array.from({ length: 160 }, (_, i) => `<div class=it id=i${i}>${i}: ${'Una risposta lunga della sessione, con molte righe di testo. '.repeat(i % 3 ? 4 : 30)}</div>`).join('')}</div>`
const out = []
for (const [name, b] of [['webkit', webkit], ['chromium', chromium]]) {
  const br = await b.launch(); const p = await br.newPage({ viewport: { width: 390, height: 700 } })
  for (const cv of [true, false]) {
    await p.setContent(page(cv))
    const r = await p.evaluate(async () => {
      const l = document.getElementById('l'), raf = () => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)))
      l.scrollTop = l.scrollHeight; await raf(); l.scrollTop = l.scrollHeight; await raf()
      let worst = 0, total = 0
      for (let k = 0; k < 60; k++) {
        // la voce più in alto ancora visibile
        const top = [...l.children].find(e => e.getBoundingClientRect().bottom > 40)
        const y0 = top.getBoundingClientRect().top, s0 = l.scrollTop
        l.scrollTop = s0 - 300; await raf()
        const moved = top.getBoundingClientRect().top - y0, scrolled = s0 - l.scrollTop
        const jump = Math.abs(moved - scrolled); worst = Math.max(worst, jump); total += jump
        if (l.scrollTop <= 0) break
      }
      return { worst: Math.round(worst), total: Math.round(total), anchor: CSS.supports('overflow-anchor: auto') }
    })
    out.push({ browser: name, contentVisibility: cv, ...r })
  }
  await br.close()
}
console.log(JSON.stringify(out, null, 1))
