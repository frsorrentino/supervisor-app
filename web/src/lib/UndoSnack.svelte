<script lang="ts">
  import { UNDO_MS } from './animazioni'
  import { ridotto } from './molla'
  import { cancel, undoState } from './undo.svelte'
  import { t } from './t'
  // Voce 4, fedele all'originale (Kokonut Undo button; app-animazioni-fedeli-26-11): l'avviso in basso con «Annulla»; il bordo del tasto
  // si svuota in senso orario dal centro in alto, lineare in UNDO_MS, tratto 1,5 px; accanto i secondi che restano salgono di 10 px in
  // 200 ms, ease-out (0.23, 1, 0.32, 1). Finito il bordo il comando è partito. Moto ridotto: bordo pieno, i secondi cambiano e basta.
  let btn = $state<HTMLButtonElement>()
  let secs = $state(Math.ceil(UNDO_MS / 1000))
  let d = $state('')
  let len = 0
  $effect(() => {
    const u = undoState.shown
    if (!u || !btn) return
    const start = performance.now()
    let id = 0
    const shape = () => {
      if (!btn) return
      const w = btn.offsetWidth, h = btn.offsetHeight, s = 0.75, r = h / 2 - s
      d = `M${w / 2} ${s}H${w - s - r}A${r} ${r} 0 0 1 ${w - s - r} ${h - s}H${s + r}A${r} ${r} 0 0 1 ${s + r} ${s}Z`
      len = 2 * (w - 2 * s - 2 * r) + 2 * Math.PI * r
    }
    shape()
    const path = () => btn?.querySelector<SVGPathElement>('path')
    const tick = (now: number) => {
      const left = Math.max(0, UNDO_MS - (now - start))
      secs = Math.max(1, Math.ceil(left / 1000))
      const p = path()
      if (p) p.style.strokeDasharray = ridotto() ? 'none' : `${(len * left / UNDO_MS).toFixed(2)} ${len.toFixed(2)}`
      if (left > 0) id = requestAnimationFrame(tick)
    }
    id = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(id)
  })
</script>

{#if undoState.shown}{@const u = undoState.shown}
  {#key u.id}
    <div class="snack" role="status" data-testid="annulla">
      <p>{u.text}</p>
      <button class="undo" bind:this={btn} onclick={cancel}>
        <svg class="edge" aria-hidden="true"><path {d} /></svg>{t.undo}
        <span class="secs" aria-hidden="true">{#key secs}<span class="dg">{secs}</span>{/key}</span>
      </button>
    </div>
  {/key}
{/if}

<style>
  .snack { position: fixed; left: 50%; bottom: calc(16px + env(safe-area-inset-bottom, 0px)); transform: translateX(-50%); z-index: 50; display: flex; align-items: center; gap: 12px; max-width: min(92vw, 480px); padding: 8px 8px 8px 16px; border-radius: 14px; background: var(--s3); color: var(--tx); box-shadow: 0 6px 24px rgb(0 0 0 / .35); animation: sn-in var(--spring-fast-t) var(--spring-fast) both; }
  @keyframes sn-in { from { opacity: 0; transform: translate(-50%, 8px); } }
  p { flex: 1; min-width: 0; margin: 0; font-size: 14.5px; }
  .undo { position: relative; flex: none; display: inline-flex; align-items: center; gap: 8px; min-height: 40px; padding: 0 14px; border-radius: 999px; font: 600 15px var(--display); color: var(--tx); }
  .edge { position: absolute; inset: 0; width: 100%; height: 100%; overflow: visible; pointer-events: none; }
  .edge path { fill: none; stroke: var(--tx); stroke-width: 1.5; stroke-linecap: round; }
  .secs { position: relative; display: inline-block; height: 20px; min-width: 1ch; overflow: hidden; font: 500 13px/20px var(--mono); color: var(--tx2); font-variant-numeric: tabular-nums; }
  .dg { display: block; animation: dg-in 200ms cubic-bezier(0.23, 1, 0.32, 1) both; }
  @keyframes dg-in { from { transform: translateY(10px); opacity: 0; } }
  @media (prefers-reduced-motion: reduce) { .snack, .dg { animation: none; } }
</style>
