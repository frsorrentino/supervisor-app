<script lang="ts">
  import { molla, motore, ridotto, clamp } from './molla'
  import { t } from './t'
  // Voce 8 del terzo giro, fedele agli originali (Scrubber e Range slider; app-animazioni-terzo-c-26-11): sotto la pillola della
  // lettura una barra con una tacca per pezzo letto; il riempimento arriva alla fine del pezzo in corso e ai salti va in 150 ms con
  // (0.23, 1, 0.32, 1). La capsula del punto è spenta (0,15, scala 0,7) e si accende mentre trascini (0,8, 1) sulla molla di Scrubber
  // (ζ 0,9, rigidità 1150). Trascinando, sopra il dito un'etichetta dice dove arrivi e si inclina con la velocità come la bolla di
  // Range slider (-0,02° per px/s, fino a 12°); al rilascio la lettura riparte da lì. Moto ridotto: salti secchi.
  let { count, at, onSeek }: { count: number; at: number; onSeek: (i: number) => void } = $props()
  let bar = $state<HTMLElement>()
  let drag = $state<number | null>(null)
  let tilt = $state(0)
  let lastX = 0, lastT = 0
  const C = molla(0, 1150, 2 * 0.9 * Math.sqrt(1150))
  let capEl = $state<HTMLElement>()
  const m = motore([C], () => { if (capEl) { const c = clamp(C.x, 0, 1); capEl.style.opacity = (0.15 + 0.65 * c).toFixed(3); capEl.style.scale = (0.7 + 0.3 * c).toFixed(3) } })
  $effect(() => { C.t = drag != null ? 1 : 0; if (ridotto()) { C.x = C.t; C.v = 0 } m.vai() })
  $effect(() => () => m.stop())
  const frac = $derived(drag != null && bar ? clamp(drag / bar.clientWidth, 0, 1) : (at + 1) / count)
  const target = $derived(drag != null && bar ? Math.min(count - 1, Math.floor(clamp(drag / bar.clientWidth, 0, 1) * count)) : at)
  function x(e: PointerEvent) { return e.clientX - (bar?.getBoundingClientRect().left ?? 0) }
  function down(e: PointerEvent) { (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId); drag = x(e); lastX = e.clientX; lastT = e.timeStamp }
  function move(e: PointerEvent) {
    if (drag == null) return
    drag = x(e)
    const dt = (e.timeStamp - lastT) / 1000
    if (dt > 0) tilt = clamp(((e.clientX - lastX) / dt) * -0.02, -12, 12)
    lastX = e.clientX; lastT = e.timeStamp
  }
  function up() { if (drag == null) return; onSeek(target); drag = null; tilt = 0 }
</script>

<div class="scrub" bind:this={bar} class:dragging={drag != null} class:still={ridotto()} role="slider" tabindex="0" aria-label={t.readingSeek(at + 1, count)}
  aria-valuemin={1} aria-valuemax={count} aria-valuenow={at + 1} data-testid="lettura-scrubber"
  onpointerdown={down} onpointermove={move} onpointerup={up} onpointercancel={() => { drag = null; tilt = 0 }}
  onkeydown={(e) => { if (e.key === 'ArrowRight' && at < count - 1) onSeek(at + 1); else if (e.key === 'ArrowLeft' && at > 0) onSeek(at - 1); else if (e.key === 'Home') onSeek(0); else if (e.key === 'End') onSeek(count - 1) }}>
  <span class="fill" style:width="{frac * 100}%"></span>
  {#each Array.from({ length: count - 1 }, (_, i) => i + 1) as i}<span class="tk" style:left="{(i / count) * 100}%"></span>{/each}
  <span class="capw" style:left="{frac * 100}%"><span class="cap" bind:this={capEl}></span></span>
  {#if drag != null}<span class="lbl" style:left="{frac * 100}%" style:rotate="{tilt}deg">{t.readingSeek(target + 1, count)}</span>{/if}
</div>

<style>
  .scrub { position: relative; flex: 1 0 100%; height: 24px; margin: 0 8px 2px; touch-action: none; cursor: pointer; }
  .scrub::before { content: ''; position: absolute; left: 0; right: 0; top: 8px; height: 8px; border-radius: 4px; background: var(--s3); }
  .fill { position: absolute; left: 0; top: 8px; height: 8px; border-radius: 4px; background: rgb(255 255 255 / .14); transition: width 150ms cubic-bezier(0.23, 1, 0.32, 1); }
  .capw { position: absolute; top: 50%; translate: -50% -50%; transition: left 150ms cubic-bezier(0.23, 1, 0.32, 1); }
  .dragging .fill, .dragging .capw, .still .fill, .still .capw { transition: none; }
  .cap { display: block; width: 4px; height: 22px; border-radius: 999px; background: var(--tx); opacity: .15; scale: .7; }
  .tk { position: absolute; top: 9px; width: 1px; height: 6px; border-radius: 999px; background: var(--tx3); translate: -50% 0; }
  .lbl { position: absolute; bottom: 40px; translate: -50% 0; transform-origin: 50% 100%; padding: 5px 10px; border-radius: 10px; background: var(--icon); color: var(--bg); font: 600 12.5px var(--sans, sans-serif); white-space: nowrap; pointer-events: none; }
</style>
