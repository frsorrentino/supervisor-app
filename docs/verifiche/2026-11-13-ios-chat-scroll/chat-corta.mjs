// app-ios-chat-scroll-13-11: una chat corta (un turno col lavoro ridotto alle ultime voci, come nel video del collega) nella vista
// di un iPhone. Misura lo spazio vuoto sotto l'ultima voce e sopra la prima, con la regola nuova e senza (tolta con uno stile
// iniettato), e controlla che una chat lunga scorra ancora fino in cima. Demo su http://localhost:4237 (vite).
import { webkit, chromium } from 'playwright'
const url = process.argv[2] ?? 'http://localhost:4237/?demo&s=payments-api'
const out = []
for (const [name, b] of [['webkit', webkit], ['chromium', chromium]]) {
  const br = await b.launch()
  const ctx = await br.newContext({ viewport: { width: 390, height: 844 }, hasTouch: true, deviceScaleFactor: 2 })
  const p = await ctx.newPage()
  await p.goto(url); await p.waitForTimeout(4000)
  const r = await p.evaluate(async () => {
    const raf = () => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)))
    // La lista a schermo: fra le pagine dello scorrimento laterale, quella dentro la finestra.
    const l = [...document.querySelectorAll('.lines')].find(e => { const r = e.getBoundingClientRect(); return r.left >= 0 && r.right <= innerWidth + 1 && e.querySelector('.it') })
    if (!l) return { err: 'nessuna lista a schermo' }
    // Lunga com'è: si arriva in cima e si torna in fondo.
    const long = { client: l.clientHeight, scroll: l.scrollHeight }
    l.scrollTop = 0; await raf(); long.top = l.scrollTop
    l.scrollTop = l.scrollHeight; await raf(); long.end = Math.round(l.scrollHeight - l.scrollTop - l.clientHeight)
    // Corta, come nel video: resta l'ultimo turno, solo col suo lavoro.
    const its = [...l.querySelectorAll('.it')], last = its.at(-1)
    for (const e of [...l.children]) if (e !== last && !e.classList.contains('toend')) e.remove()
    for (const e of last.querySelectorAll('.ans, .tm, .me')) e.remove()
    const gaps = () => { const a = l.getBoundingClientRect(), b = last.getBoundingClientRect(); return { sopra: Math.round(b.top - a.top), sotto: Math.round(a.bottom - b.bottom), scorre: l.scrollHeight > l.clientHeight } }
    await raf()
    const dopo = gaps()
    const st = document.createElement('style'); st.textContent = '.lines > :first-child { margin-top: 0 !important }'; document.head.appendChild(st)
    await raf()
    const prima = gaps()
    const cs = getComputedStyle(document.documentElement), cl = getComputedStyle(l)
    return { long, prima, dopo, pagina: cs.overscrollBehaviorY ?? cs.overscrollBehavior, lista: cl.overscrollBehaviorY ?? cl.overscrollBehavior }
  })
  out.push({ browser: name, ...r })
  await br.close()
}
console.log(JSON.stringify(out, null, 1))
