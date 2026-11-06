<script lang="ts">
  import { onMount } from 'svelte'
  import { summarize, type Loaf, type Summary } from './frameStats'
  import { setMeter } from './meterSwitch.svelte'
  import { t } from './t'
  // Il contatore dei fotogrammi (app-fluidita-06-11), montato solo quando è acceso: da ogni tocco, clic o tasto conta i
  // fotogrammi fino a quando la pagina resta ferma per 1,5 s (al massimo 4 s dopo l'ultimo tocco: su una macchina lenta anche la
  // pagina ferma ha fotogrammi lunghi), e mostra l'ultimo gesto. Spento non esiste: niente ascoltatori,
  // niente giro di fotogrammi, niente osservatori.
  const QUIET = 1500, CAP = 4000
  let last = $state<Summary | null>(null)
  let live = $state(false)
  onMount(() => {
    let frames: number[] = [], loaf: Loaf[] = [], from = 0, prev = 0, lastSlow = 0, lastInput = 0, raf = 0
    const tickF = (ts: number) => {
      if (prev) { const d = ts - prev; frames.push(d); if (d > 34) lastSlow = ts }
      prev = ts
      if (ts - lastInput > QUIET && (ts - lastSlow > QUIET || ts - lastInput > CAP)) { live = false; last = summarize(frames, loaf.filter(l => l.at >= from)); raf = 0; return }
      raf = requestAnimationFrame(tickF)
    }
    const start = () => {
      lastInput = performance.now()
      if (raf) return
      frames = []; loaf = []; from = lastInput; prev = 0; lastSlow = from; live = true
      raf = requestAnimationFrame(tickF)
    }
    let po: PerformanceObserver | null = null
    try {
      po = new PerformanceObserver(list => {
        for (const e of list.getEntries() as (PerformanceEntry & { styleAndLayoutStart?: number; scripts?: { duration: number; sourceFunctionName?: string; invoker?: string }[] })[]) {
          if (!raf) continue
          loaf.push({ at: e.startTime, d: e.duration, layoutFrom: e.styleAndLayoutStart || null,
            scripts: (e.scripts ?? []).map(x => ({ name: (x.sourceFunctionName || x.invoker || '?').slice(0, 40), d: x.duration })) })
        }
      })
      po.observe({ type: 'long-animation-frame', buffered: false })
    } catch { po = null /* il browser non dà i long-animation-frame: restano i fotogrammi */ }
    const opts = { capture: true, passive: true }
    for (const ev of ['pointerdown', 'keydown', 'wheel'] as const) window.addEventListener(ev, start, opts)
    return () => { for (const ev of ['pointerdown', 'keydown', 'wheel'] as const) window.removeEventListener(ev, start, opts); po?.disconnect(); if (raf) cancelAnimationFrame(raf) }
  })
  const why = (c: Summary['loaf'][number]['cause']) => (c.kind === 'script' ? t.meterScript(c.name) : c.kind === 'layout' ? t.meterLayout : t.meterPaint)
</script>

<div class="meter" data-testid="contatore-fotogrammi" aria-live="polite">
  <div class="mh"><b>{t.meterTitle}</b>{#if live}<span class="dot"></span>{/if}<button aria-label={t.meterOff} title={t.meterOff} onclick={() => setMeter(false)}>✕</button></div>
  {#if last}
    <div class="row"><span>{t.meterMax}</span><b class:bad={last.max > 34}>{last.max} ms</b></div>
    <div class="row"><span>{t.meterOver}</span><b class:bad={last.over33 > 0}>{last.over33} / {last.frames}</b></div>
    {#each last.loaf as l, i (i)}<div class="row lo"><span>{why(l.cause)}</span><b>{l.d} ms</b></div>{/each}
  {:else}<p>{t.meterHint}</p>{/if}
</div>

<style>
  /* Fermo in basso a sinistra, sopra tutto, piccolo: il pannello non deve pesare sui fotogrammi che misura. */
  .meter { position: fixed; left: 12px; bottom: 12px; z-index: 90; width: 240px; padding: 8px 10px; border-radius: 12px; background: rgb(0 0 0 / .78); color: #e8e8e8; font: 12px/1.5 var(--mono, ui-monospace, monospace); pointer-events: auto; contain: layout paint; }
  .mh { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
  .mh b { flex: 1; font-weight: 600; }
  .mh button { color: inherit; opacity: .7; }
  .dot { width: 6px; height: 6px; border-radius: 50%; background: #ffb23f; }
  .row { display: flex; justify-content: space-between; gap: 8px; }
  .row span { min-width: 0; overflow-wrap: anywhere; opacity: .8; }
  .row b { font-weight: 600; white-space: nowrap; }
  .row b.bad { color: #ff8a80; }
  .lo span { opacity: .65; }
  p { margin: 0; opacity: .7; }
</style>
