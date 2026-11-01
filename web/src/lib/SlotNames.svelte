<script lang="ts">
  import { fitNames } from './slotInfo'
  // Suggerimenti nello slot (Franz 01/11 20:02): una riga ferma coi nomi interi che stanno nella larghezza vera, poi «+N».
  let { names }: { names: string[] } = $props()
  let width = $state(0)
  let el = $state<HTMLElement>()
  let ready = $state(0)
  $effect(() => { document.fonts?.ready.then(() => { ready++ }) })
  const line = $derived.by(() => {
    void ready
    if (!el || !width) return null
    const ctx = document.createElement('canvas').getContext('2d')
    if (!ctx) return names[0] ?? null
    ctx.font = getComputedStyle(el).font
    return fitNames(names, width, x => ctx.measureText(x).width)
  })
</script>

<span class="usnames" bind:this={el} bind:clientWidth={width}>{line ?? '\u00a0'}</span>

<style>
  .usnames { display: block; width: 100%; font-size: 13.5px; line-height: 1.3; color: var(--tx2); white-space: nowrap; overflow: hidden; }
</style>
