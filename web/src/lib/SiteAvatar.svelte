<script lang="ts">
  // app-favicon-siti-18-11, scelta B di Franz (18/11 17:11): la favicon su un riquadro chiaro arrotondato, così si legge anche scura
  // di notte, con intorno l'anello del colore dello stato; al lavoro un arco che gira. SiteAvatar.kt sul telefono.
  let { url, ring, busy = false, size = 28, label }: { url: string; ring: string; busy?: boolean; size?: number; label?: string } = $props()
</script>

<span class="av" class:busy style="--s:{size}px; --ring:{ring}" role={label ? 'img' : undefined} aria-label={label}>
  <span class="tile"><img src={url} alt="" /></span>
</span>

<style>
  .av { position: relative; flex: none; display: inline-grid; place-items: center; width: var(--s); height: var(--s); border-radius: calc(var(--s) * .3);
    box-shadow: inset 0 0 0 calc(var(--s) * .075) var(--ring); }
  .tile { width: calc(var(--s) * .72); height: calc(var(--s) * .72); border-radius: calc(var(--s) * .2); background: #F5F5F2; display: grid; place-items: center; }
  :global(.theme-day) .tile { background: #fff; box-shadow: 0 0 0 1px rgba(0, 0, 0, .12); }
  img { width: 80%; height: 80%; display: block; }
  .busy { box-shadow: inset 0 0 0 calc(var(--s) * .075) color-mix(in srgb, var(--ring) 28%, transparent); }
  .busy::after { content: ''; position: absolute; inset: 0; border-radius: inherit; border: calc(var(--s) * .075) solid transparent;
    border-top-color: var(--ring); border-right-color: var(--ring); animation: giro 1.4s linear infinite; }
  @keyframes giro { to { transform: rotate(360deg); } }
  @media (prefers-reduced-motion: reduce) { .busy::after { animation: none; } }
</style>
