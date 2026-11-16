// Prova dello spostamento laterale con lo zoom delle dita (app-zoom-pan-laterale-16-11, Franz 16/11 sul Chromebook: dopo lo
// zoom l'inquadratura andava solo in verticale). Con la demo avviata (`npm run dev`), apre un browser Chromium senza finestra,
// ingrandisce la pagina come le dita (Emulation.setPageScaleFactor) e su una griglia di punti di Home, sessione e schermo largo
// prova la rotella orizzontale (il touchpad) e il dito: l'inquadratura (visualViewport.offsetLeft) deve spostarsi di lato.
// Un elemento che scorre di lato per conto suo (un diff largo) può prendersi lo spostamento: conta come «interno», non come errore.
//   node scripts/zoom-pan.mjs [url-della-demo]            exit 1 se un punto resta fermo
//   node scripts/zoom-pan.mjs [url] --nozoom              senza zoom: stampa cosa si sposta per ogni punto, da confrontare prima/dopo
//   --only="home telefono"  una pagina sola;  --debug  lo stato prima e dopo ogni gesto
// Il browser: CHROME=<percorso> oppure Edge o Chrome nei posti soliti. Gli eventi di CDP vogliono pixel CSS dall'angolo
// dell'inquadratura, senza lo zoom. Il 16/11, prima della correzione: Home e sessione del telefono ferme in 36 punti su 36 (la
// striscia tratteneva lo scorrimento laterale), schermo largo fermo sulla chat; dopo: tutti i punti, e senza zoom gli stessi esiti.
import { spawn } from 'node:child_process'
import { existsSync, mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const args = process.argv.slice(2)
const nozoom = args.includes('--nozoom')
const debug = args.includes('--debug')
const only = args.find(a => a.startsWith('--only='))?.slice(7)
const base = args.find(a => !a.startsWith('--')) ?? 'http://localhost:4237'
const SCALE = nozoom ? 1 : 1.6
const exe = [process.env.CHROME, 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  '/usr/bin/google-chrome', '/usr/bin/chromium', '/usr/bin/chromium-browser', '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'].find(p => p && existsSync(p))
if (!exe) { console.error('Nessun browser Chromium: CHROME=<percorso>'); process.exit(2) }
const port = 9300 + Math.floor(Math.random() * 600)
const browser = spawn(exe, ['--headless=new', '--disable-gpu', `--remote-debugging-port=${port}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), 'zoom-pan-'))}`, '--hide-scrollbars', 'about:blank'])
const sleep = ms => new Promise(r => setTimeout(r, ms))
let ws
for (let i = 0; i < 60 && !ws; i++) { await sleep(300); try { const l = await (await fetch(`http://127.0.0.1:${port}/json`)).json(); ws = l.find(t => t.type === 'page')?.webSocketDebuggerUrl } catch {} }
if (!ws) { console.error('Il browser non risponde'); browser.kill(); process.exit(2) }
const sock = new WebSocket(ws); await new Promise(r => (sock.onopen = r))
let id = 0; const wait = {}
sock.onmessage = m => { const d = JSON.parse(m.data); if (d.id && wait[d.id]) { wait[d.id](d); delete wait[d.id] } }
const send = (method, params = {}) => new Promise(r => { const i = ++id; wait[i] = r; sock.send(JSON.stringify({ id: i, method, params })) })
const ev = async e => { const r = await send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true }); return r.result?.result?.value }

// Lo stato: dove sta l'inquadratura, quanto può spostarsi, e la somma degli scorrimenti laterali interni (la striscia del
// telefono compresa) con la pagina aperta: se cambiano, il gesto se l'è preso un elemento della pagina.
const state = () => ev(`JSON.stringify((() => {
  const vv = visualViewport
  let inner = 0; for (const e of document.querySelectorAll('body *')) if (e.scrollLeft) inner += Math.round(e.scrollLeft)
  return { l: vv.offsetLeft, t: vv.offsetTop, max: innerWidth - vv.width, inner, page: location.href + '|' + document.querySelectorAll('dialog[open]').length }
})())`).then(JSON.parse)
// L'inquadratura centrata su un punto della pagina, come con le dita: un segno di un pixel portato in vista.
const centerOn = (x, y) => ev(`(() => { const m = document.createElement('div'); m.style.cssText = 'position:absolute;left:${x}px;top:${y}px;width:1px;height:1px;pointer-events:none'; document.body.append(m); m.scrollIntoView({ block: 'center', inline: 'center', behavior: 'instant' }); m.remove(); return 1 })()`)
const under = (x, y) => ev(`(() => { const e = document.elementFromPoint(${x}, ${y}); if (!e) return '-'; const c = typeof e.className === 'string' && e.className ? e.className.split(' ').find(k => !k.startsWith('svelte-')) : ''; return (c || e.tagName.toLowerCase()) + (e.closest('.lines') ? ' (chat)' : e.closest('.home') ? ' (home)' : '') })()`)

const control = 'data:text/html,' + encodeURIComponent('<meta name=viewport content="width=device-width,initial-scale=1"><body style="margin:0;height:100%"><div style="height:100%;background:linear-gradient(90deg,#c33,#33c)"></div>')
const pages = [
  { name: 'controllo', url: control, w: 412, h: 915, mobile: true, cols: 2, rows: 2 },
  { name: 'home telefono', url: `${base}/?demo`, w: 412, h: 915, mobile: true, cols: 3, rows: 6 },
  { name: 'sessione telefono', url: `${base}/?demo&s=storefront`, w: 412, h: 915, mobile: true, cols: 3, rows: 6 },
  { name: 'largo sessione', url: `${base}/?demo&s=storefront`, w: 1366, h: 768, mobile: false, cols: 6, rows: 4 },
  { name: 'largo home', url: `${base}/?demo`, w: 1366, h: 768, mobile: false, cols: 6, rows: 4 },
]
async function load(p) {
  await send('Emulation.setDeviceMetricsOverride', { width: p.w, height: p.h, deviceScaleFactor: 1, mobile: p.mobile })
  await send('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 5 })
  await send('Page.navigate', { url: 'about:blank' }); await sleep(150)
  await send('Page.navigate', { url: p.url })
  // L'app pronta: la home o la chat disegnate (al primo caricamento il server di sviluppo compila), poi un attimo per assestarsi.
  if (p.name !== 'controllo') for (let i = 0; i < 60 && !(await ev(`!!document.querySelector('.home, .lines')`)); i++) await sleep(250)
  await sleep(p.name === 'controllo' ? 600 : 1500)
  await send('Emulation.setPageScaleFactor', { pageScaleFactor: SCALE }); await sleep(250)
  const cls = await ev('document.documentElement.className')
  if (p.name !== 'controllo' && !nozoom && !cls.split(' ').includes('zoomed')) console.log(`${p.name}: html senza la classe zoomed dopo lo zoom (${cls})`)
}
async function gesture(how, x, y, dir) {
  if (how === 'rotella') return send('Input.dispatchMouseEvent', { type: 'mouseWheel', x, y, deltaX: dir * 120, deltaY: 0 })
  await send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x, y }] })
  for (let i = 1; i <= 6; i++) { await sleep(16); await send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: x - dir * i * 18, y }] }) }
  return send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] })
}

let stuck = 0
for (const p of pages.filter(q => !only || q.name === only)) {
  await load(p)
  const out = { ok: 0, inner: 0, stuck: [] }
  for (let r = 0; r < p.rows; r++) for (let c = 0; c < p.cols; c++) for (const how of ['rotella', 'dito']) {
    const x = Math.round(p.w * (c + 0.5) / p.cols), y = Math.round(p.h * (r + 0.5) / p.rows)
    await centerOn(x, y); await sleep(60)
    const a = await state()
    const dir = a.l < a.max - 1 ? 1 : -1
    const what = await under(x, y)
    await gesture(how, Math.round(x - a.l), Math.round(y - a.t), dir)
    await sleep(450)
    const b = await state()
    const moved = nozoom ? false : (b.l - a.l) * dir > 0.5
    if (debug) console.log(how, x, y, 'dir', dir, 'a', JSON.stringify(a), 'b', JSON.stringify(b))
    const inner = b.inner !== a.inner || b.page !== a.page
    if (nozoom) console.log(`${p.name} ${x},${y} ${how}: ${inner ? 'si sposta dentro la pagina' : 'fermo'} · ${what}`)
    else if (moved) out.ok++
    else if (inner) out.inner++
    else out.stuck.push(`${how} ${x},${y} ${what}`)
    if (inner) await load(p)
  }
  if (!nozoom) {
    stuck += out.stuck.length
    console.log(`${p.name.padEnd(17)} di lato ${out.ok} · interno ${out.inner} · fermi ${out.stuck.length}${out.stuck.length ? '\n   ' + out.stuck.join('\n   ') : ''}`)
  }
}
sock.close(); browser.kill()
process.exit(stuck ? 1 : 0)
