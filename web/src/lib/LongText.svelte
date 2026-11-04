<script lang="ts">
  import { speech, toggle } from './speech.svelte'
  import { t } from './t'
  import { overTwo } from './longText'
  // Un testo delle pagine nuove (Da decidere, Da provare, Da seguire, Prima raccolta, Risultati): oltre due righe ha accanto
  // il tasto ▶ della lettura vocale, ■ mentre legge (Franz 03/11 15:22, app-tasto-play-03-11). Le righe si misurano sullo
  // schermo; diventato lungo, il testo resta lungo finché non cambia, così il tasto non va e viene col margine che prende.
  // Classi e stile li mette chi lo usa su un contenitore: qui il testo eredita carattere e colore. `clamp`: righe mostrate da piegato.
  let { text, testid = null, clamp = 0, pre = false }: { text: string; testid?: string | null; clamp?: number; pre?: boolean } = $props()
  let el = $state<HTMLElement | null>(null)
  let long = $state(false)
  let seen = ''
  $effect(() => {
    const p = el, now = text
    if (!p) return
    if (now !== seen) { seen = now; long = false }
    const measure = () => { if (!long && overTwo(p.scrollHeight, parseFloat(getComputedStyle(p).lineHeight))) long = true }
    measure()
    const ro = new ResizeObserver(measure)
    ro.observe(p)
    return () => ro.disconnect()
  })
  const reading = $derived(speech.text === text && !speech.paused)
</script>
<div class="lt" class:long>
  <p bind:this={el} class:pre class:clamp={clamp > 0} style:--clamp={clamp || null} data-testid={testid}>{text}</p>
  {#if long}<button class="play" data-testid="leggi" aria-label={reading ? t.stopReading : t.listen} title={reading ? t.stopReading : t.listen} onclick={(e) => { e.stopPropagation(); toggle(text) }}>
    <svg viewBox="0 0 24 24" width="20" height="20">{#if reading}<rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" />{:else}<path d="M8 5.5v13l10.5-6.5z" fill="currentColor" />{/if}</svg>
  </button>{/if}
</div>
<style>
  .lt { display: flex; align-items: flex-start; gap: 10px; }
  .lt > p { flex: 1; min-width: 0; margin: 0; font: inherit; color: inherit; }
  .pre { white-space: pre-wrap; overflow-wrap: anywhere; }
  .clamp { display: -webkit-box; -webkit-line-clamp: var(--clamp); line-clamp: var(--clamp); -webkit-box-orient: vertical; overflow: hidden; }
  .play { flex: none; width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; color: var(--icon); background: var(--high); cursor: pointer; }
</style>
