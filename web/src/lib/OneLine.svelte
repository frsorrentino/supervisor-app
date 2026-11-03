<script lang="ts">
  import { tick } from 'svelte'
  import { count, shorts, SEP } from './oneLine'
  // Il riassunto a destra su una riga (app-riassunti-una-riga-03-11): le parti che non entrano cadono intere dall'ultima; se non
  // entra nemmeno la prima resta il suo numero. Misura la larghezza vera: quella del contenitore, a ogni ridimensionamento.
  // `tone`: red per un fallito, mute per una parte che non chiede niente adesso (grigia anche in una riga ambra).
  let { parts }: { parts: { text: string; tone?: 'red' | 'mute' }[] } = $props()
  let el: HTMLElement | undefined = $state()
  function fit() {
    if (!el) return
    const items = [...el.querySelectorAll<HTMLElement>(':scope > .p')]
    const alts = [...el.querySelectorAll<HTMLElement>(':scope > .n')]
    const show = (k: number) => items.forEach((s, i) => (s.hidden = i >= k))
    alts.forEach(a => (a.hidden = true))
    // Lo spazio si misura con tutte le parti in vista: ridotta, la riga sarebbe larga quanto quel che resta e non ricrescerebbe più.
    show(items.length)
    const max = el.clientWidth + 0.5
    const k = count(items.length, max, n => { show(n); return el!.scrollWidth })
    show(k)
    if (k > 0) return
    // Nemmeno la prima: la sua forma corta più lunga che entra, o niente.
    for (const a of alts) { a.hidden = false; if (el.scrollWidth <= max) return; a.hidden = true }
  }
  $effect(() => {
    if (!el) return
    const ro = new ResizeObserver(() => fit())
    ro.observe(el.parentElement ?? el)
    document.fonts?.ready.then(fit)
    return () => ro.disconnect()
  })
  $effect(() => { void parts; tick().then(fit) })
</script>

<span class="ol" bind:this={el}>{#each parts as p, i}<span class="p">{#if i}{SEP}{/if}<span class:red={p.tone === 'red'} class:mute={p.tone === 'mute'}>{p.text}</span></span>{/each}{#if parts.length}{#each shorts(parts[0].text) as x}<span class="n" hidden><span class:red={parts[0].tone === 'red'} class:mute={parts[0].tone === 'mute'}>{x}</span></span>{/each}{/if}</span>

<style>
  .ol { display: block; min-width: 0; overflow: hidden; white-space: nowrap; text-align: right; }
  .p[hidden], .n[hidden] { display: none; }
  .red { color: var(--red); }
  .mute { color: var(--tx2); }
</style>
