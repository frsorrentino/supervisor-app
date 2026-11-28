<script lang="ts">
  import { untrack } from 'svelte'
  import { ferma, molla, passo, ridotto, type Molla } from './molla'
  import { DONE_MS, FIRE_MS, MORPH, PRESS, RELEASE, SHAKE, SQUASH, damping, kick } from './tieniPremuto'
  import { t } from './t'
  // Il tasto «tieni premuto» (app-tieni-premuto-28-11, Franz 28/11 16:22, «Tutti e tre»; mockup tieni-premuto-28-11.html), fedele al
  // Hold to publish di designbookmark (hold.js e le molle di motion.js), coi colori, i font e i raggi dell'app. Uno solo per la web,
  // gemello di HoldButton in Animazioni.kt.
  // Tenere premuto (`mode` hold): il dito, Spazio o Invio giù schiacciano la pillola a 0,96 (molla 520 20) e la riempiono da sinistra,
  // lineare in `ms` (è una barra di avanzamento); la scritta si inverte sotto il pieno (una seconda scritta tagliata alla larghezza del
  // pieno). Lasciata prima: il pieno torna con la molla 260 14, appena oltre lo zero, e la pillola fa «no» (molla 700 9, spinta
  // 260·min(1, 2p + 0,3)); non parte niente. In fondo: la pillola diventa un cerchio (molla 320 20), verde, la scritta sfuma e si
  // sfoca, la spunta si disegna (0,45 s, (.65, 0, .35, 1), dopo 0,16 s); poi parte l'azione.
  // Toccare (`mode` tap, correzione della master del 28/11): ogni altro «Approva» e ogni «Va» restano a un tocco col finale dello
  // stesso componente; l'azione parte al tocco e chi lo ospita fa uscire la carta o la riga come prima. Con `on` è chi lo ospita a dire
  // se il tasto è fatto: se il PC rifiuta lo spegne e la pillola torna.
  // Lettore di schermo: «Tieni premuto per …», poi l'esito. Moto ridotto: niente schiacciata né scossa, molle critiche; il pieno cresce
  // lo stesso, perché dice quanto manca, e la spunta compare senza disegnarsi. `pin` ferma il pieno (0..1; 1 = fatto) per le prove.
  let {
    label, onDone, mode = 'hold', ms = 1200, tone = 'amber', size = 'pill', icon = null, aria = null, said = null, on = undefined, pin = null,
    disabled = false, stop = false, cls = '', testid = 'tieni-premuto',
  }: {
    label: string; onDone: () => void; mode?: 'hold' | 'tap'; ms?: number
    /** red: chi chiude; ink: il colore del testo; amber: l'ambra dei tasti principali; grey: un tasto tonale. */
    tone?: 'red' | 'ink' | 'amber' | 'grey'
    /** pill: la pillola da 52 larga quanto il posto; key: un tasto della carta; mini: il tasto di una riga. */
    size?: 'pill' | 'key' | 'mini'
    /** Il tracciato 24×24 dell'icona davanti alla scritta, a solo contorno. */
    icon?: string | null
    aria?: string | null; said?: string | null; on?: boolean; pin?: number | null; disabled?: boolean
    /** Il tocco non arriva a chi contiene il tasto (la riga di una voce, che al tocco apre la sua scheda). */
    stop?: boolean
    cls?: string; testid?: string
  } = $props()

  const p = molla(0, ...RELEASE)
  const W = molla(0, ...MORPH)
  const sc = molla(1, ...PRESS)
  const sh = molla(0, ...SHAKE)
  const tune = (s: Molla, [k, c]: [number, number]) => { s.k = k; s.c = damping(k, c, ridotto()) }
  let holding = false
  let mine = $state(false)
  const done = $derived(pin != null ? pin >= 1 : (on ?? mine))
  let news = $state('')
  let btn = $state<HTMLButtonElement>()
  let fill = $state<HTMLElement>()
  let top = $state<HTMLElement>()
  let id = 0
  let last = 0
  const timers: ReturnType<typeof setTimeout>[] = []
  const later = (wait: number, f: () => void) => { timers.push(setTimeout(f, wait)) }

  function render() {
    if (!btn) return
    const w = btn.offsetWidth, h = btn.offsetHeight
    // La pillola diventa cerchio senza cambiare il posto che occupa: si taglia dai due lati verso il centro, coi bordi tondi.
    const cut = Math.max(0, Math.min(w / 2 - 1, ((w - h) / 2) * W.x))
    btn.style.clipPath = cut > 0.05 ? `inset(0 ${cut.toFixed(2)}px round ${(h / 2).toFixed(1)}px)` : ''
    btn.style.transform = Math.abs(sh.x) > 0.01 || Math.abs(sc.x - 1) > 1e-4 ? `translateX(${sh.x.toFixed(2)}px) scale(${sc.x.toFixed(4)})` : ''
    if (fill && top) {
      const fw = Math.max(0, Math.min(1, p.x)) * w
      fill.style.width = `${fw.toFixed(2)}px`
      top.style.clipPath = `inset(0 ${Math.max(0, w - fw).toFixed(2)}px 0 0)`
    }
  }
  function loop(now: number) {
    const d = Math.min((now - last) / 1000, 1 / 30) / 4
    last = now
    for (let i = 0; i < 4; i++) {
      // Lineare mentre si tiene: è una barra di avanzamento; la molla riprende quando si lascia.
      if (holding) { p.x = Math.min(1, p.x + d / (ms / 1000)); p.v = 0; p.t = p.x } else passo(p, d)
      passo(W, d); passo(sc, d); passo(sh, d)
    }
    if (holding && p.x >= 1) complete()
    render()
    if (!holding && [p, W, sc, sh].every(ferma)) { for (const s of [p, W, sc, sh]) { s.x = s.t; s.v = 0 } render(); id = 0; return }
    id = requestAnimationFrame(loop)
  }
  const go = () => { if (!id && typeof requestAnimationFrame === 'function') { last = performance.now(); id = requestAnimationFrame(loop) } }

  // Il cerchio segue lo stato «fatto», di qui o di chi ospita il tasto (`on`): avanti con la molla 320 20, indietro con 0,8 della rigidità.
  $effect(() => {
    const d = done
    untrack(() => {
      if (pin != null) { p.x = p.t = Math.max(0, Math.min(1, pin)); p.v = 0; W.x = W.t = d ? 1 : 0; W.v = 0; render(); return }
      if (d) { tune(W, MORPH); W.t = 1 } else { tune(W, [MORPH[0] * 0.8, MORPH[1]]); W.t = 0; if (!holding) { tune(p, RELEASE); p.t = 0 } }
      go()
    })
  })

  function complete() {
    holding = false
    mine = true
    sc.t = 1
    if (!ridotto()) sc.v += 2.5
    news = said ?? ''
    later(FIRE_MS, onDone)
    later(DONE_MS, back)
  }
  /** Il tasto torna pillola se nessuno l'ha tolto (l'originale dopo 1,7 s). */
  function back() { if (on === undefined) { mine = false; p.x = p.v = p.t = 0; go() } }
  function start(e: Event) {
    if (mode !== 'hold' || holding || done || disabled || pin != null) return
    e.preventDefault()
    holding = true
    news = ''
    tune(p, RELEASE)
    if (!ridotto()) { tune(sc, PRESS); sc.t = SQUASH }
    go()
  }
  function stopHold() {
    if (!holding) return
    holding = false
    sc.t = 1
    if (p.x < 1) { tune(p, RELEASE); p.t = 0; sh.v += kick(p.x, ridotto()); news = t.holdEarly }
    go()
  }
  function tap() {
    if (mode !== 'tap' || done || disabled || pin != null) return
    mine = true
    if (!ridotto()) sc.v += 2.5
    news = said ?? ''
    onDone()
    go()
    later(DONE_MS, back)
  }
  function down(e: PointerEvent) {
    // Dentro una scheda che si apre col tocco o col tocco lungo (la home), il dito sul tasto resta del tasto.
    if (stop) e.stopPropagation()
    if (mode !== 'hold' || e.button !== 0) return
    start(e)
    if (holding) try { btn?.setPointerCapture(e.pointerId) } catch { /* il dito resta nostro finché si alza */ }
  }
  const isKey = (e: KeyboardEvent) => e.key === ' ' || e.key === 'Enter'
  $effect(() => () => { timers.forEach(clearTimeout); if (id) cancelAnimationFrame(id) })
</script>

<button bind:this={btn} type="button" class="hold {tone} {size} {cls}" class:done class:tap={mode === 'tap'} data-testid={testid} disabled={disabled && !done}
  aria-label={aria ?? label} aria-disabled={done || undefined}
  onpointerdown={down} onpointerup={(e) => { if (stop) e.stopPropagation(); stopHold() }} onpointercancel={stopHold} onlostpointercapture={stopHold} oncontextmenu={(e) => { if (mode === 'hold') e.preventDefault() }}
  onkeydown={(e) => { if (stop) e.stopPropagation(); if (mode === 'hold' && isKey(e)) { e.preventDefault(); if (!e.repeat) start(e) } }}
  onkeyup={(e) => { if (mode === 'hold' && isKey(e)) { e.preventDefault(); stopHold() } }} onblur={stopHold} onclick={(e) => { if (stop) e.stopPropagation(); tap() }}>
  <span class="lbl base">{#if icon}<svg viewBox="0 0 24 24" aria-hidden="true"><path d={icon} /></svg>{/if}{label}</span>
  {#if mode === 'hold'}
    <span class="fill" bind:this={fill} aria-hidden="true"></span>
    <span class="lbl top" bind:this={top} aria-hidden="true">{#if icon}<svg viewBox="0 0 24 24"><path d={icon} /></svg>{/if}{label}</span>
  {/if}
  <svg class="chk" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
</button>
<span class="sr" aria-live="polite">{news}</span>

<style>
  .hold { position: relative; display: block; overflow: hidden; border-radius: 999px; background: var(--s2); color: var(--tx); box-shadow: inset 0 0 0 1px var(--line);
    font: 600 15px var(--display); letter-spacing: .01em; white-space: nowrap; touch-action: none; user-select: none; -webkit-user-select: none; -webkit-touch-callout: none;
    -webkit-tap-highlight-color: transparent; will-change: transform; transition: background-color .2s, box-shadow .2s; }
  .hold.tap { touch-action: manipulation; }
  /* La pillola da tenere premuto: alta 52, raggio 26, larga quanto il posto, al centro (il cerchio resta dov'era il centro). */
  .pill { width: 100%; height: 52px; padding: 0 20px; margin: 0 auto; }
  .key { display: inline-block; min-height: 42px; padding: 0 16px; }
  .mini { display: inline-block; min-height: 36px; padding: 0 14px; font-size: 14.5px; }
  .lbl { display: flex; align-items: center; justify-content: center; gap: 8px; height: 100%; transition: opacity .2s, filter .2s; }
  .key .lbl, .mini .lbl { min-height: inherit; }
  .lbl svg { width: 20px; height: 20px; flex: none; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  .base { position: relative; }
  .fill { position: absolute; left: 0; top: 0; bottom: 0; width: 0; border-radius: inherit; }
  .top { position: absolute; inset: 0; color: var(--bg); clip-path: inset(0 100% 0 0); }
  .chk { position: absolute; left: 50%; top: 50%; width: 26px; height: 26px; margin: -13px 0 0 -13px; opacity: 0; fill: none; stroke: var(--bg); stroke-width: 2.6; stroke-linecap: round; stroke-linejoin: round; }
  .mini .chk { width: 22px; height: 22px; margin: -11px 0 0 -11px; }
  .chk path { stroke-dasharray: 24; stroke-dashoffset: 24; }
  /* I tre colori del mockup: rosso per chiudere, il colore del testo, l'ambra dei tasti principali; più il tonale grigio. */
  .red .base { color: var(--red); } .red .fill { background: var(--red); }
  .ink .base { color: var(--tx); } .ink .fill { background: var(--tx); }
  .amber { background: color-mix(in srgb, var(--wait) 12%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 50%, transparent); }
  .amber .base { color: var(--wait); } .amber .fill { background: var(--wait); }
  .grey { background: var(--s3); box-shadow: none; }
  .hold:focus-visible { outline: 2px solid var(--tx); outline-offset: 2px; }
  .hold:disabled { opacity: .45; cursor: default; }
  /* Fatto: il cerchio verde, la scritta che sfuma e si sfoca, la spunta che si disegna. */
  .hold.done { background: var(--good); box-shadow: none; cursor: default; }
  .done .lbl { opacity: 0; filter: blur(4px); }
  .done .fill { opacity: 0; }
  .done .chk { opacity: 1; transition: opacity .1s .12s; }
  .done .chk path { stroke-dashoffset: 0; transition: stroke-dashoffset .45s cubic-bezier(.65, 0, .35, 1) .16s; }
  @media (prefers-reduced-motion: reduce) {
    .hold, .lbl { transition: none; }
    .done .chk { transition: none; }
    .done .chk path { transition: none; }
  }
  .sr { position: absolute; width: 1px; height: 1px; margin: -1px; padding: 0; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; border: 0; }
</style>
