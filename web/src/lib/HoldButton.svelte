<script lang="ts">
  import { HOLD_MS } from './animazioni'
  import { ferma, molla, passo, ridotto } from './molla'
  // Voce 6, fedele all'originale (designbookmark.com/motion, Hold Button; app-animazioni-fedeli-26-11). Il dito (o Invio, Spazio)
  // giù schiaccia il tasto a 0,96 (molla 520 20) e lo riempie da sinistra in HOLD_MS, lineare; la scritta si inverte esattamente
  // sotto il riempimento. Lasciato prima: il riempimento torna con la molla 260 14 e il tasto fa «no» (molla 700 9, spinta
  // 260·min(1, 2p + 0,3) px/s). Pieno: cerchio verde (larghezza con la molla 320 20, pop +2,5), scritta che sfuma e si sfoca, spunta
  // che si disegna; l'ok parte a spunta disegnata, dopo 1,7 s il tasto torna (molla 256 20). Con il moto ridotto niente movimento,
  // ma il tempo da tenere resta.
  let { label, onDone, cls = '' }: { label: string; onDone: () => void; cls?: string } = $props()

  const p = molla(0, 260, 14)
  const W = molla(0, 320, 20)
  const sc = molla(1, 520, 20)
  const sh = molla(0, 700, 9)
  let holding = false
  let done = $state(false)
  let btn = $state<HTMLButtonElement>()
  let fill = $state<HTMLElement>()
  let top = $state<HTMLElement>()
  let w0 = 0
  let id = 0
  let last = 0
  const timers: ReturnType<typeof setTimeout>[] = []
  const later = (ms: number, f: () => void) => { timers.push(setTimeout(f, ms)) }

  function render() {
    if (!btn || !fill || !top) return
    const base = w0 || btn.offsetWidth
    const h = btn.offsetHeight
    if (w0) btn.style.width = (base + (h - base) * W.x).toFixed(2) + 'px'
    btn.style.transform = `translateX(${sh.x.toFixed(2)}px) scale(${sc.x.toFixed(4)})`
    const fw = Math.max(0, p.x) * base
    fill.style.width = fw.toFixed(2) + 'px'
    top.style.clipPath = `inset(0 ${Math.max(0, base - fw).toFixed(2)}px 0 0)`
  }
  function loop(now: number) {
    const d = Math.min((now - last) / 1000, 1 / 30) / 4
    last = now
    for (let i = 0; i < 4; i++) {
      if (holding) { p.x = Math.min(1, p.x + d / (HOLD_MS / 1000)); p.v = 0; p.t = p.x }
      for (const s of [p, W, sc, sh]) passo(s, d)
    }
    if (holding && p.x >= 1) complete()
    render()
    if (!holding && [p, W, sc, sh].every(ferma)) {
      id = 0
      if (!done && btn && W.x === W.t && W.t === 0) { btn.style.width = ''; w0 = 0 }
      return
    }
    id = requestAnimationFrame(loop)
  }
  const go = () => { if (!id) { last = performance.now(); id = requestAnimationFrame(loop) } }

  function complete() {
    holding = false
    done = true
    W.k = 320; W.t = 1
    sc.t = 1; sc.v += 2.5
    later(610, onDone)
    later(1700, () => { done = false; W.k = 256; W.t = 0; p.x = p.v = p.t = 0; go() })
  }
  function start(e: Event) {
    if (holding || done) return
    e.preventDefault()
    if (ridotto()) { holding = true; later(HOLD_MS, () => { if (holding) { holding = false; onDone() } }); return }
    if (!w0 && btn) w0 = btn.offsetWidth
    holding = true
    sc.t = 0.96
    go()
  }
  function stop() {
    if (!holding) return
    holding = false
    if (ridotto()) { timers.forEach(clearTimeout); return }
    sc.t = 1
    if (p.x < 1) { p.t = 0; sh.v += 260 * Math.min(1, p.x * 2 + 0.3) }
    go()
  }
  $effect(() => () => { timers.forEach(clearTimeout); if (id) cancelAnimationFrame(id) })
</script>

<button bind:this={btn} class="hold {cls}" class:done data-testid="tieni-premuto"
  onpointerdown={start} onpointerup={stop} onpointerleave={stop} onpointercancel={stop} oncontextmenu={(e) => e.preventDefault()}
  onkeydown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && !e.repeat) start(e) }} onkeyup={stop} onblur={stop}>
  <span class="fill" bind:this={fill} aria-hidden="true"></span>
  <span class="lab">{label}</span>
  <span class="lab top" bind:this={top} aria-hidden="true">{label}</span>
  <svg class="check" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
</button>

<style>
  .hold { position: relative; overflow: hidden; touch-action: none; user-select: none; -webkit-user-select: none; -webkit-touch-callout: none; white-space: nowrap; }
  .fill { position: absolute; left: 0; top: 0; bottom: 0; width: 0; background: var(--red); }
  .lab { position: relative; transition: opacity 0.2s, filter 0.2s; }
  .lab.top { position: absolute; inset: 0; display: grid; place-items: center; color: var(--bg); clip-path: inset(0 100% 0 0); }
  .check { position: absolute; left: 50%; top: 50%; width: 26px; height: 26px; margin: -13px 0 0 -13px; opacity: 0; fill: none; stroke: var(--bg); stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; }
  .check path { stroke-dasharray: 24; stroke-dashoffset: 24; }
  .hold.done { background: var(--good); border-color: var(--good); }
  .done .fill { opacity: 0; }
  .done .lab { opacity: 0; filter: blur(4px); }
  .done .check { opacity: 1; transition: opacity 0.1s 0.12s; }
  .done .check path { stroke-dashoffset: 0; transition: stroke-dashoffset 0.45s cubic-bezier(0.65, 0, 0.35, 1) 0.16s; }
  @media (prefers-reduced-motion: reduce) { .fill, .lab.top { display: none; } }
</style>
