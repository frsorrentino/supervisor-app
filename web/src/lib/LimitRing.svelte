<script lang="ts">
  import { fill, limit } from './animazioni'
  import { t } from './t'
  // Voce 7 di app-animazioni-seconde-26-11 (Limite ad anello): un anello piccolo si riempie mentre scrivi; negli ultimi 20 caratteri è
  // ambra e dice quanti ne restano, oltre il limite rosso. Non blocca la scrittura. Con le animazioni ridotte nessuno scatto.
  let { length, max }: { length: number; max: number } = $props()
  const state = $derived(limit(length, max))
  const left = $derived(max - length)
  const C = 2 * Math.PI * 9
</script>

<span class="lim {state}" role="status" aria-label={t.sceltaLeft(Math.max(0, left))} data-testid="limite">
  {#if state !== 'ok'}<span class="n">{left}</span>{/if}
  <svg viewBox="0 0 22 22" aria-hidden="true"><circle class="bg" cx="11" cy="11" r="9" /><circle class="fg" cx="11" cy="11" r="9" style:stroke-dasharray={C} style:stroke-dashoffset={C * (1 - fill(length, max))} /></svg>
</span>

<style>
  .lim { display: inline-flex; align-items: center; gap: 6px; font: 500 12.5px/1 var(--mono); color: var(--wait); }
  .lim.over { color: var(--red); }
  svg { width: 22px; height: 22px; transform: rotate(-90deg); transition: transform var(--spring-fast-t) var(--spring-fast); }
  .lim.near svg, .lim.over svg { transform: rotate(-90deg) scale(1.15); }
  circle { fill: none; stroke-width: 3; }
  .bg { stroke: color-mix(in srgb, var(--tx) 14%, transparent); }
  .fg { stroke: var(--tx2); transition: stroke-dashoffset 120ms linear, stroke 200ms; }
  .lim.near .fg { stroke: var(--wait); }
  .lim.over .fg { stroke: var(--red); }
  @media (prefers-reduced-motion: reduce) { svg, .fg { transition: none; } }
</style>
