<script lang="ts">
  import { fill, limit } from './animazioni'
  import { clamp, molla, motore } from './molla'
  import { t } from './t'
  // Voce 7, fedele all'originale (designbookmark.com/motion, Letter limit; app-animazioni-fedeli-26-11): l'anello va con la molla
  // 280 22; a ogni carattere il numero fa un pop (da 1,12, molla 520 16); ogni carattere oltre il limite dà una scossa (spinta 34 px/s
  // sulla molla 700 12, metà ampiezza) e il pop da 1,25. Colori in 200 ms. Il testo oltre il limite non si taglia (regola dell'app).
  // Moto ridotto: al posto.
  let { length, max }: { length: number; max: number } = $props()
  const state = $derived(limit(length, max))
  const left = $derived(max - length)
  const C = 2 * Math.PI * 9

  const F = molla(0, 280, 22)
  const CP = molla(1, 520, 16)
  const SH = molla(0, 700, 12)
  let root = $state<HTMLElement>()
  let last: number | null = null
  function render() {
    if (!root) return
    root.style.transform = `translateX(${(SH.x * 0.5).toFixed(2)}px)`
    const fg = root.querySelector<SVGCircleElement>('.fg')
    if (fg) fg.style.strokeDashoffset = (C * (1 - clamp(F.x, 0, 1))).toFixed(2)
    const n = root.querySelector<HTMLElement>('.n')
    if (n) n.style.transform = `scale(${CP.x.toFixed(4)})`
  }
  const m = motore([F, CP, SH], render)
  $effect(() => {
    const len = length
    F.t = fill(len, max)
    if (last == null) { F.x = F.t } else if (len !== last) {
      const over = len > max && len > last
      CP.x = over ? 1.25 : Math.max(CP.x, 1.12)
      if (over) SH.v += 34 * (len - last)
    }
    last = len
    queueMicrotask(() => { render(); m.vai() })
  })
  $effect(() => () => m.stop())
</script>

<span class="lim {state}" bind:this={root} role="status" aria-label={t.sceltaLeft(Math.max(0, left))} data-testid="limite">
  {#if state !== 'ok'}<span class="n">{left}</span>{/if}
  <svg viewBox="0 0 22 22" aria-hidden="true"><circle class="bg" cx="11" cy="11" r="9" /><circle class="fg" cx="11" cy="11" r="9" style:stroke-dasharray={C} /></svg>
</span>

<style>
  .lim { display: inline-flex; align-items: center; gap: 6px; font: 500 12.5px/1 var(--mono); color: var(--wait); transition: color 0.2s; }
  .lim.over { color: var(--red); }
  .n { display: inline-block; }
  svg { width: 22px; height: 22px; transform: rotate(-90deg); }
  circle { fill: none; stroke-width: 3; }
  .bg { stroke: color-mix(in srgb, var(--tx) 14%, transparent); }
  .fg { stroke: var(--tx2); transition: stroke 0.2s; }
  .lim.near .fg { stroke: var(--wait); }
  .lim.over .fg { stroke: var(--red); }
</style>
