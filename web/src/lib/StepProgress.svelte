<script lang="ts">
  import { untrack } from 'svelte'
  import { clamp, molla, motore, ridotto, type Molla } from './molla'
  import { step, stepLine } from './animazioni'
  // Step progress, fedele all'originale (designbookmark.com/motion, step-progress: steps.js e steps.css; app-chiusura-step-progress-27-11,
  // Franz 27/11 12:00), come StepProgress.kt: una linea da 4 px dal primo all'ultimo pallino, col verde che va al passo attuale sulla
  // molla 160 22, e un pallino da 32 col numero per passo. Un passo fatto si riempie di verde (260 24) mentre il numero sfuma, e la spunta
  // si disegna 0,12 s dopo (140 20); il passo che diventa attuale fa un pop da 0,7 (500 14, spinta 6) e si accende col colore di lavoro
  // (300 26), col numero scuro. Sotto i nomi, in chiaro fino all'attuale (0,3 s). Colori v3: verde --good, attuale --busy, pallini --s3,
  // linea al 14% del testo come la fill-3 dell'originale. L'attuale è fatto solo a lavoro finito (`finished`): nella chiusura «Chiusa»
  // raggiunta aspetta la conferma del PC (scelta a della master). Moto ridotto: i passi accesi, fermi.
  let { names, at, finished = false }: { names: string[]; at: number; finished?: boolean } = $props()
  type P = { f: Molla; c: Molla; p: Molla; on: Molla; timer?: ReturnType<typeof setTimeout> }
  const L = molla(0, 160, 22)
  let ps: P[] = []
  let cur = -1
  const all: Molla[] = [L]
  let root = $state<HTMLElement>()
  const n = $derived(names.length)
  const i = $derived(Math.min(Math.max(0, at), Math.max(0, names.length - 1)))
  const pos = (k: number) => (n <= 1 ? 50 : (k / (n - 1)) * 100)
  function render() {
    if (!root) return
    const bar = root.querySelector<HTMLElement>('.bar')
    if (bar) bar.style.width = (clamp(L.x, 0, 1) * 100).toFixed(3) + '%'
    root.querySelectorAll<HTMLElement>('.dot').forEach((d, k) => {
      const s = ps[k]
      if (!s) return
      const f = clamp(s.f.x, 0, 1), o = clamp(s.on.x, 0, 1)
      d.style.background = f > 0.01 ? `color-mix(in srgb, var(--good) ${(f * 100).toFixed(1)}%, var(--s3))` : o > 0.01 ? `color-mix(in srgb, var(--busy) ${(o * 100).toFixed(1)}%, var(--s3))` : ''
      d.style.color = o > 0.5 ? 'var(--bg)' : ''
      const num = d.querySelector<HTMLElement>('b')
      if (num) num.style.opacity = (1 - f).toFixed(3)
      const path = d.querySelector<SVGPathElement>('path')
      if (path) path.style.strokeDashoffset = (22 * (1 - clamp(s.c.x, 0, 1))).toFixed(2)
      d.style.transform = `scale(${s.p.x.toFixed(4)})`
    })
  }
  const m = motore(all, render)
  // Come `go` di steps.js.
  function go(at: number, count: number, fin: boolean) {
    if (ps.length !== count) {
      ps.forEach((s) => clearTimeout(s.timer))
      ps = Array.from({ length: count }, () => ({ f: molla(0, 260, 24), c: molla(0, 140, 20), p: molla(1, 500, 14), on: molla(0, 300, 26) }))
      all.length = 1
      for (const s of ps) all.push(s.f, s.c, s.p, s.on)
      cur = -1
    }
    if (!count) return
    L.t = stepLine(at, count)
    ps.forEach((s, k) => {
      const st = step(k, at, fin)
      s.f.t = st === 'done' ? 1 : 0
      s.on.t = st === 'current' ? 1 : 0
      clearTimeout(s.timer)
      if (st === 'done') s.timer = setTimeout(() => { s.c.t = 1; m.vai() }, 120)
      else s.c.t = 0
    })
    if (at !== cur) { ps[at].p.x = 0.7; ps[at].p.v = 6 }
    cur = at
    // Moto ridotto: anche la spunta al suo posto subito.
    if (ridotto()) ps.forEach((s, k) => { if (step(k, at, fin) === 'done') { clearTimeout(s.timer); s.c.t = 1 } })
    queueMicrotask(() => { render(); m.vai() })
  }
  $effect(() => { const a = i, c = n, f = finished; untrack(() => go(a, c, f)) })
  $effect(() => () => { ps.forEach((s) => clearTimeout(s.timer)); m.stop() })
</script>

<div class="sp" bind:this={root} data-testid="passi-chiusura">
  <div class="track" aria-hidden="true"><span class="bar"></span>
    <div class="dots">
      {#each names as _, k (k)}<span class="dot" style:left="{pos(k)}%"><b>{k + 1}</b><svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg></span>{/each}
    </div>
  </div>
  <div class="names">
    {#each names as name, k (k)}<span class:on={k <= i} style:left="{pos(k)}%" aria-current={k === i ? 'step' : undefined}>{name}</span>{/each}
  </div>
</div>

<style>
  .sp { align-self: stretch; margin-top: 10px; }
  .track { position: relative; height: 32px; margin: 0 14px; }
  .track::before { content: ''; position: absolute; left: 0; right: 0; top: 14px; height: 4px; border-radius: 2px; background: color-mix(in srgb, var(--tx) 14%, transparent); }
  .bar { position: absolute; left: 0; top: 14px; width: 0; height: 4px; border-radius: 2px; background: var(--good); will-change: width; }
  .dots { position: absolute; inset: 0; }
  .dot { position: absolute; top: 0; width: 32px; height: 32px; margin-left: -16px; border-radius: 50%; background: var(--s3); color: var(--tx2); display: grid; place-items: center; font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums; will-change: transform; }
  .dot b { position: absolute; font-weight: inherit; }
  .dot svg { position: absolute; width: 16px; height: 16px; fill: none; stroke: var(--bg); stroke-width: 2.8; stroke-linecap: round; stroke-linejoin: round; }
  .dot path { stroke-dasharray: 22; stroke-dashoffset: 22; }
  .names { position: relative; height: 18px; margin: 10px 14px 0; }
  .names span { position: absolute; top: 0; transform: translateX(-50%); font-size: 12.5px; line-height: 18px; color: var(--tx2); white-space: nowrap; transition: color .3s; }
  .names span.on { color: var(--tx); }
</style>
