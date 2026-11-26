<script lang="ts">
  import { locale } from './lang'
  import { molla, motore, ridotto, clamp } from './molla'
  import { neck, rubber } from './jelly'
  import { RATE_MAX, RATE_MIN, RATE_STEP, rateFraction, snapRate } from './speechRules'
  import { t } from './t'
  // Lo slider della velocità (SpeechRate.kt, approvato da Franz il 07/10 alle 22:06): da 0,5× a 2× a passi di 0,05; `onDone` al
  // rilascio, così la voce riparte una volta sola. Lo usano la barra di lettura e le Impostazioni.
  // Voce 5 del terzo giro, fedele all'originale (Jelly slider; app-animazioni-terzo-c-26-11) con la gelatina b, il collo calcolato
  // (jelly.ts) invece del filtro: il pollice segue il dito sulla molla 900 50 e al rilascio torna sulla 300 14, con l'elastico oltre le
  // estremità; si allunga con la velocità; premuto scala a 1,16 (520 16). La goccia col numero sale di 38 px e scala da 0,5 a 1
  // (420 22), insegue il pollice sulla 260 18 e si inclina col ritardo fino a 28°. Una tacca ogni 0,05, lunga ogni 0,25, accesa
  // quando il valore la passa. L'input vero resta sotto, trasparente, per tastiera e lettori di schermo. Moto ridotto: pollice e
  // goccia senza rimbalzo.
  let { value, onInput = () => {}, onDone }: { value: number; onInput?: (v: number) => void; onDone: (v: number) => void } = $props()
  const label = (r: number) => `${r.toLocaleString(locale, { maximumFractionDigits: 2 })}×`
  let draft = $state(0)
  $effect(() => { draft = value })
  const R = 12, D = 18, RISE = -38
  let w = $state(0)
  let held = $state(false)
  // Con la tastiera (fuoco visibile) la goccia resta su, come nell'originale.
  let kbd = $state(false)
  let finger: number | null = null
  let svg = $state<SVGSVGElement>()
  const X = molla(R, 900, 50), DX = molla(R, 260, 18), DY = molla(0, 420, 22), DS = molla(0.5, 420, 22), PS = molla(1, 520, 16)
  const toX = (v: number) => R + clamp(rateFraction(v), 0, 1) * Math.max(0, w - 2 * R)
  const marks = Array.from({ length: Math.round((RATE_MAX - RATE_MIN) / 0.05) + 1 }, (_, i) => ({ v: RATE_MIN + i * 0.05, major: i % 5 === 0 }))
  let ready = false
  function sync() {
    if (!w) return
    const rest = toX(draft)
    if (!ready) { X.x = X.t = DX.x = DX.t = rest; ready = true }
    X.t = held && finger != null ? rubber(finger, R, w - R, 18) : rest
    const tune = (s: typeof X, k: number, c: number) => { s.k = k; s.c = ridotto() ? 2 * Math.sqrt(k) : c }
    if (held) tune(X, 900, 50); else if (kbd) tune(X, 420, 34); else tune(X, 300, 14)
    tune(DY, 420, 22); tune(DS, 420, 22)
    DY.t = held || kbd ? RISE : 0
    DS.t = held || kbd ? 1 : 0.5
    PS.t = held ? 1.16 : 1
    m.vai()
  }
  function render() {
    DX.t = X.x
    if (!svg) return
    const cy = 30
    const x = X.x
    const fill = clamp(x, 0, w) * clamp((x - R) / R, 0, 1)
    svg.querySelector('.fill')?.setAttribute('width', fill.toFixed(2))
    const st = 1 + Math.min(Math.abs(X.v) / 2400, 0.4), ps = PS.x
    const rx = R * st * ps, ry = R * ps / st
    const th = svg.querySelector('.thumb')
    th?.setAttribute('cx', x.toFixed(2)); th?.setAttribute('rx', rx.toFixed(2)); th?.setAttribute('ry', ry.toFixed(2))
    const up = DS.x > 0.52 || Math.abs(DY.x) > 1
    const dx = DX.x, dy = cy + DY.x, dr = D * DS.x
    const drop = svg.querySelector('.drop'), nk = svg.querySelector('.neck'), lb = svg.querySelector<SVGTextElement>('.lbl')
    drop?.setAttribute('cx', dx.toFixed(2)); drop?.setAttribute('cy', dy.toFixed(2)); drop?.setAttribute('r', up ? dr.toFixed(2) : '0')
    nk?.setAttribute('d', up ? neck(x, cy, Math.min(rx, ry), dx, dy, dr, R + dr + 16) ?? '' : '')
    if (lb) {
      const a = clamp((DS.x - 0.7) / 0.25, 0, 1), tilt = clamp((DX.x - X.x) * 0.9, -28, 28)
      lb.setAttribute('x', dx.toFixed(2)); lb.setAttribute('y', dy.toFixed(2)); lb.style.opacity = up ? a.toFixed(3) : '0'
      lb.setAttribute('transform', `rotate(${tilt.toFixed(2)} ${dx.toFixed(2)} ${dy.toFixed(2)})`)
    }
    svg.querySelectorAll<SVGRectElement>('.mk').forEach((el, i) => { el.classList.toggle('on', marks[i].v <= draft + 0.001); el.setAttribute('x', (toX(marks[i].v) - 0.75).toFixed(2)) })
  }
  const m = motore([X, DX, DY, DS, PS], render)
  $effect(() => { void draft; void w; void held; void kbd; sync() })
  $effect(() => () => m.stop())
  function pointerX(e: PointerEvent) { const r = (e.currentTarget as HTMLElement).getBoundingClientRect(); return e.clientX - r.left }
</script>

<div class="rs" bind:clientWidth={w}>
  <div class="jl">
    <svg bind:this={svg} width={w} height="60" viewBox="0 -24 {w || 1} 60" aria-hidden="true">
      <rect class="track" x="0" y="28" width={w} height="4" rx="2" />
      <rect class="fill" x="0" y="28" width="0" height="4" rx="2" />
      {#each marks as mk}<rect class="mk" class:major={mk.major} x="0" y="35" width="1.5" height={mk.major ? 8 : 4} />{/each}
      <path class="neck" d="" />
      <circle class="drop" cx="0" cy="30" r="0" />
      <ellipse class="thumb" cx={R} cy="30" rx={R} ry={R} />
      <text class="lbl" x="0" y="30" text-anchor="middle" dominant-baseline="central">{label(draft)}</text>
    </svg>
    <input type="range" min={RATE_MIN} max={RATE_MAX} step={RATE_STEP} value={draft} aria-label={t.rateChange(label(draft))}
      onpointerdown={(e) => { held = true; finger = pointerX(e) }} onpointermove={(e) => { if (held) { finger = pointerX(e); sync() } }}
      onpointerup={() => { held = false; finger = null }} onpointercancel={() => { held = false; finger = null }}
      oninput={(e) => { draft = snapRate(Number((e.currentTarget as HTMLInputElement).value)); onInput(draft) }}
      onfocus={(e) => (kbd = (e.currentTarget as HTMLInputElement).matches(':focus-visible'))} onblur={() => (kbd = false)}
      onchange={() => onDone(draft)} />
  </div>
  <div class="labs" aria-hidden="true">
    <span style="left:0">{label(RATE_MIN)}</span>
    <span style="left:{rateFraction(1) * 100}%;transform:translateX(-50%)">{label(1)}</span>
    <span style="right:0">{label(RATE_MAX)}</span>
  </div>
</div>

<style>
  .rs { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .jl { position: relative; height: 36px; }
  svg { position: absolute; left: 0; top: -24px; overflow: visible; pointer-events: none; }
  input { position: absolute; inset: 0; width: 100%; height: 36px; margin: 0; opacity: 0; cursor: pointer; }
  .track { fill: var(--s3); }
  .fill, .neck, .drop, .thumb { fill: var(--icon); }
  .lbl { fill: var(--bg); font: 700 12px var(--sans, sans-serif); }
  .mk { fill: var(--s3); transition: fill .2s ease; }
  .mk.major { fill: var(--tx3); }
  .mk.on { fill: var(--tx2); }
  .mk.major.on { fill: var(--tx); }
  .labs { position: relative; height: 14px; margin: 0 6px; font-size: 11px; color: var(--text2); }
  .labs span { position: absolute; top: 0; white-space: nowrap; }
</style>
