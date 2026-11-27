<script lang="ts">
  import { speech } from './speech.svelte'
  import { t } from './t'
  import { overOne, wordsBeforeBlock } from './longText'
  import { readBlocks } from './speechRules'
  import { wordCount } from './animazioni'
  import ReadAlong from './ReadAlong.svelte'
  import ReadButton from './ReadButton.svelte'
  import type { Snippet } from 'svelte'
  // app-play-lettura-ovunque-27-11 (Franz 27/11 13:29): ogni testo più lungo di una riga ha accanto il ▶ della chat (stessa icona, «Leggi ad
  // alta voce», la pillola con velocità e barra dei paragrafi); mentre legge, le parole dette si accendono con la pillola di Read along.
  // Prima: un testo delle pagine nuove (Da decidere, Da provare, Da seguire, Prima raccolta, Risultati) oltre due righe aveva accanto
  // il tasto ▶ della lettura vocale, ■ mentre legge (Franz 03/11 15:22, app-tasto-play-03-11). Le righe si misurano sullo
  // schermo; diventato lungo, il testo resta lungo finché non cambia, così il tasto non va e viene col margine che prende.
  // Classi e stile li mette chi lo usa su un contenitore: qui il testo eredita carattere e colore. `clamp`: righe mostrate da piegato.
  // `body`: un testo disegnato in un altro modo (segni, link, icone), sempre con `text` come testo letto; lì le parole non si accendono.
  let { text, testid = null, clamp = 0, pre = false, body = null }: { text: string; testid?: string | null; clamp?: number; pre?: boolean; body?: Snippet | null } = $props()
  let el = $state<HTMLElement | null>(null)
  let long = $state(false)
  let seen = ''
  $effect(() => {
    const p = el, now = text
    if (!p) return
    if (now !== seen) { seen = now; long = false }
    const measure = () => { if (!long && overOne(p.scrollHeight, parseFloat(getComputedStyle(p).lineHeight))) long = true }
    measure()
    const ro = new ResizeObserver(measure)
    ro.observe(p)
    return () => ro.disconnect()
  })
  // La parola detta nel testo intero: le parole dei paragrafi letti prima più quella del paragrafo di adesso.
  const word = $derived.by(() => {
    if (speech.text !== text || speech.word == null || speech.block == null) return null
    return wordsBeforeBlock(readBlocks(text, t.outcomeLabel).map((b) => wordCount(b.text)), speech.block) + speech.word
  })
</script>
<div class="lt" class:long>
  <p bind:this={el} class:pre class:clamp={clamp > 0} style:--clamp={clamp || null} data-testid={testid}>{#if body}{@render body()}{:else if word != null}<ReadAlong {text} {word} />{:else}{text}{/if}</p>
  {#if long}<ReadButton {text} />{/if}
</div>
<style>
  .lt { display: flex; align-items: flex-start; gap: 10px; }
  .lt > p { flex: 1; min-width: 0; margin: 0; font: inherit; color: inherit; }
  .pre { white-space: pre-wrap; overflow-wrap: anywhere; }
  .clamp { display: -webkit-box; -webkit-line-clamp: var(--clamp); line-clamp: var(--clamp); -webkit-box-orient: vertical; overflow: hidden; }
</style>
