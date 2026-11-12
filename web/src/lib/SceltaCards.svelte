<script lang="ts">
  import { getContext } from 'svelte'
  import type { WaitItem } from './contract'
  import AttesaImmagine from './AttesaImmagine.svelte'
  import { imageRatios } from './mediaCache.svelte'
  import { frameRatio, outcomeOf, thumbsOf, waitFileKey, type WaitCardsHost } from './scelta'
  import { t } from './t'
  // Le richieste citate nelle chat come schede compatte (app-scheda-scelta-12-11, descrizione approvata da Franz il 12/11 alle 15:10:
  // «in chat, al posto del link, una scheda compatta: titolo, una riga su cosa decidere, miniature delle opzioni, stato; un tocco la
  // apre»), della stessa famiglia delle prove citate (ProvaCards). Il titolo e lo stato a parole sulla prima riga («Da scegliere» in
  // ambra, «Scelto B» in verde), sotto cosa c'è da decidere su una riga, poi le miniature con la lettera dell'opzione. I dati vengono
  // dall'op `attesa` con `archive` (contratto 1.137), una lettura per scheda e poi la cache di App: dopo la risposta la scheda resta,
  // con quello che è stato scelto. Una richiesta che il PC non conosce più resta un link; senza il contesto (provini), anche.
  // `flat` (app-scheda-scelta-12-11, punto 3): dentro una scheda della home o di una sessione sta senza bordo né fondo, come una riga.
  let { ids, flat = false }: { ids: string[]; flat?: boolean } = $props()
  const host = getContext<WaitCardsHost | undefined>('wait-cards')
  $effect(() => { for (const id of ids) host?.ask(id) })
  const kindOf = (l: string) => (l === 'choose' ? 'choose' : l === 'verify' ? 'verify' : l === 'reply' ? 'answer' : 'decide')
  function words(w: WaitItem): [string, string] {
    const o = outcomeOf(w)
    if (o.state === 'chosen') return ['good', t.sceltaChosenShort(o.letter, o.name)]
    if (o.state === 'approved') return ['good', t.sceltaApproved]
    if (o.state === 'redo') return ['wait', t.sceltaRedo]
    if (o.state === 'closed') return ['off', t.sceltaClosed]
    return ['wait', t.sceltaOpen[kindOf(w.label)]]
  }
  const head = (w: WaitItem) => w.title || w.text
  const what = (w: WaitItem) => [w.text, w.description].find(x => !!x && x !== head(w)) ?? null
</script>

{#each ids as id (id)}
  {@const w = host?.read(id)}
  {#if !host || w === 'gone'}
    <a class="lnk" href="#/attesa/{id}" data-testid="scelta-link-{id}">{t.sceltaOpenCard}</a>
  {:else}
    {@const thumbs = w ? thumbsOf(w) : []}
    <div class="wc" class:flat role="button" tabindex="0" data-testid="scelta-scheda-{id}" onclick={(e) => { e.stopPropagation(); host.open(id) }} onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); e.stopPropagation(); host.open(id) } }}>
      {#if !w}
        <span class="m">{t.sceltaCardLoading}</span>
      {:else}
        {@const [tone, state] = words(w)}
        <div class="top"><b class="one">{head(w)}</b><span class="st {tone}">{#if tone === 'good'}<svg viewBox="0 0 20 20" width="15" height="15" aria-hidden="true"><path d="M4.5 10.5 8.5 14.5 15.5 6" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>{/if}{state}</span></div>
        {#if what(w)}<span class="m one">{what(w)}</span>{/if}
        {#if thumbs.length}
          <div class="th">{#each thumbs as x (waitFileKey(x.file))}<div class="tf" style:--ar={frameRatio([imageRatios[waitFileKey(x.file)]])}><AttesaImmagine small file={x.file} letter={x.letter} label={[x.letter, x.file.name].filter(Boolean).join(', ')} onAsk={host.file} /></div>{/each}</div>
        {/if}
      {/if}
    </div>
  {/if}
{/each}

<style>
  .wc { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; padding: 10px 12px; border-radius: 14px; background: var(--s1); border: 1px solid var(--line); cursor: pointer; text-align: left; touch-action: manipulation; -webkit-touch-callout: none; transition: transform 120ms ease-out; }
  .wc.flat { margin: 6px 0 12px; padding: 0; border: 0; border-radius: 6px; background: none; }
  .wc:active { transform: scale(.99); }
  .wc:focus-visible { outline: 2px solid var(--tx); outline-offset: 2px; }
  .top { display: flex; align-items: baseline; gap: 10px; }
  .top b { flex: 1; min-width: 0; font: 600 17px/21px var(--display); color: var(--tx); }
  .st { flex: none; display: inline-flex; align-items: center; gap: 4px; font: 600 14px/21px var(--display); letter-spacing: .01em; color: var(--tx2); }
  .st.wait { color: var(--wait); }
  .st.good { color: var(--good); }
  .st svg { align-self: center; }
  .m { font-size: 14.5px; line-height: 20px; color: var(--tx2); }
  /* Una riga sola, tagliata senza «…» (regole di forma, Franz 12/09): il testo intero sta nella scheda aperta. */
  .one { white-space: nowrap; overflow: hidden; text-overflow: clip; }
  /* Le miniature: tutte alte uguali, larghe quanto vuole la loro immagine. */
  .th { display: flex; gap: 6px; margin-top: 4px; overflow: hidden; }
  .tf { flex: none; width: calc(60px * var(--ar, 1.3333)); }
  .lnk { display: inline-block; margin-top: 8px; color: var(--tx); text-decoration: underline; text-decoration-color: var(--tx3); text-underline-offset: 3px; }
  @media (hover: hover) and (pointer: fine) { .wc:hover { border-color: var(--tx3); } .wc.flat:hover .top b { text-decoration: underline; text-decoration-color: var(--tx3); text-underline-offset: 3px; } }
  @media (prefers-reduced-motion: reduce) { .wc { transition: none; } }
</style>
