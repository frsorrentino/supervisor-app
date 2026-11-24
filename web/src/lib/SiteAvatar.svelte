<script lang="ts">
  // app-favicon-siti-18-11, scelta B di Franz (18/11 17:11): la favicon su un riquadro chiaro arrotondato, così si legge anche scura
  // di notte, con intorno l'anello del colore dello stato. SiteAvatar.kt sul telefono.
  // Al lavoro (app-indicatore-scelto-24-11, Franz 24/11 20:28, proposta A «traccia sul bordo»): il riquadro sta fermo e un tratto
  // lungo un quarto del contorno lo percorre in 1,6 s sopra l'anello tenue; con «riduci movimento» l'anello è pieno e fermo.
  let { url, ring, busy = false, size = 28, label }: { url: string; ring: string; busy?: boolean; size?: number; label?: string } = $props()
  const w = $derived(size * 0.075)
</script>

<span class="av" class:busy style="--s:{size}px; --ring:{ring}" role={label ? 'img' : undefined} aria-label={label}>
  <span class="tile"><img src={url} alt="" /></span>
  {#if busy}
    <svg class="trk" viewBox="0 0 {size} {size}" aria-hidden="true">
      <rect class="run" pathLength="100" x={w / 2} y={w / 2} width={size - w} height={size - w} rx={size * 0.3 - w / 2} stroke-width={w} />
    </svg>
  {/if}
</span>

<style>
  .av { position: relative; flex: none; display: inline-grid; place-items: center; width: var(--s); height: var(--s); border-radius: calc(var(--s) * .3);
    box-shadow: inset 0 0 0 calc(var(--s) * .075) var(--ring); }
  .tile { width: calc(var(--s) * .72); height: calc(var(--s) * .72); border-radius: calc(var(--s) * .2); background: #F5F5F2; display: grid; place-items: center; }
  :global(.theme-day) .tile { background: #fff; box-shadow: 0 0 0 1px rgba(0, 0, 0, .12); }
  img { width: 80%; height: 80%; display: block; }
  .busy { box-shadow: inset 0 0 0 calc(var(--s) * .075) color-mix(in srgb, var(--ring) 28%, transparent); }
  .trk { position: absolute; inset: 0; width: 100%; height: 100%; overflow: visible; pointer-events: none; }
  .run { fill: none; stroke: var(--ring); stroke-linecap: round; stroke-dasharray: 24 76; animation: corre 1.6s linear infinite; }
  @keyframes corre { from { stroke-dashoffset: 100; } to { stroke-dashoffset: 0; } }
  @media (prefers-reduced-motion: reduce) { .run { animation: none; stroke-dasharray: none; } }
</style>
