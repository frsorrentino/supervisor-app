<script lang="ts">
  import { untrack } from 'svelte'
  import type { WaitFile } from './contract'
  import { coming, failed, imageRatios, media, missed } from './mediaCache.svelte'
  import { waitFileKey } from './scelta'
  import { t } from './t'
  // Un'immagine di una richiesta nella sua cornice (app-scheda-scelta-12-11, Franz 12/11 15:03: «Un link a una sezione che non
  // mostra immagini ma che mostra solo il testo non è di aiuto»): intera su un fondo neutro, con la lettera dell'opzione; un tocco
  // la ingrandisce. Si chiede al PC appena la cornice è in pagina, con la fila e la cache dei file della chat (mediaCache): finché
  // non arriva la cornice dice che è in arrivo e da quanto; non arrivata, lo dice e ha il tasto per richiederla.
  // `small`: la miniatura della scheda compatta e dei giri precedenti, senza parole, con la stessa fascia più bassa.
  let { file, letter = null, label, small = false, picked = false, onAsk, onOpen = null }: {
    file: WaitFile; letter?: string | null; label: string; small?: boolean; picked?: boolean
    onAsk: (f: WaitFile) => void; onOpen?: (() => void) | null
  } = $props()
  const key = $derived(waitFileKey(file))
  const url = $derived(media[key])
  $effect(() => { const k = key; if (!media[k] && !coming[k] && !missed[k] && !failed[k]) untrack(() => onAsk(file)) })
  // Da quanto è in arrivo, dopo i primi dieci secondi: la fila del relay può tenerla oltre un minuto (12/11).
  const SHOW_S = 10
  let nowMs = $state(Date.now())
  $effect(() => {
    if (small || url || !coming[key]) return
    nowMs = Date.now()
    const i = setInterval(() => (nowMs = Date.now()), 1000)
    return () => clearInterval(i)
  })
  // La forma dell'immagine arrivata: la cornice della scheda la prende (frameRatio).
  const sized = (e: Event) => { const im = e.currentTarget as HTMLImageElement; if (im.naturalHeight > 0) imageRatios[key] = im.naturalWidth / im.naturalHeight }
  const waited = $derived(coming[key] ? Math.max(0, Math.floor((nowMs - coming[key]) / 1000)) : 0)
</script>

<div class="fr" class:small class:picked data-testid="attesa-immagine">
  {#if letter}<span class="cap" aria-hidden="true">{letter}</span>{/if}
  <div class="area">
    {#if url}
      {#if onOpen}
        <button class="pic" aria-label={t.sceltaZoom(label)} onclick={(e) => { e.stopPropagation(); onOpen() }}><img src={url} alt="" draggable="false" onload={sized} /></button>
      {:else}
        <span class="pic"><img src={url} alt={label} draggable="false" onload={sized} /></span>
      {/if}
    {:else if !small}
      <div class="wait" role="status">
        {#if missed[key]}
          <span>{t.previewMissed}</span>
          <button class="again" onclick={(e) => { e.stopPropagation(); onAsk(file) }}>{t.previewRetry}</button>
        {:else if failed[key]}
          <span>{file.name}</span>
        {:else}
          <span>{waited >= SHOW_S ? t.previewComingFor(waited) : t.previewComing}</span>
        {/if}
      </div>
    {/if}
  </div>
</div>

<style>
  /* Il fondo neutro: un gradino sopra la superficie su cui sta, di notte e di giorno, senza bordo. La lettera dell'opzione sta in
     una fascia sopra l'immagine, sullo stesso fondo: l'immagine non è mai coperta. */
  .fr { position: relative; display: flex; flex-direction: column; width: 100%; border-radius: 12px; overflow: hidden; background: color-mix(in srgb, var(--tx) 7%, var(--s1)); }
  .fr.small { border-radius: 8px; }
  .fr.picked { box-shadow: inset 0 0 0 2px var(--good); }
  .cap { flex: none; height: 30px; padding: 0 10px; display: flex; align-items: center; font: 700 17px/1 var(--display); color: var(--tx2); }
  .small .cap { height: 18px; padding: 0 6px; font-size: 12px; }
  .picked .cap { color: var(--good); }
  /* La forma è quella delle immagini della scheda (`--ar`), ma mai più alta di due quinti della finestra: su una colonna larga
     una schermata di telefono resta intera a schermo, coi tasti delle opzioni sotto senza scorrere. */
  .area { position: relative; width: 100%; aspect-ratio: var(--ar, 4 / 3); max-height: min(var(--cap, 40dvh), 480px); transition: aspect-ratio 260ms cubic-bezier(0.23, 1, 0.32, 1); }
  .small .area { transition: none; max-height: none; }
  /* Fuori dal flusso: l'area tiene la sua forma e l'immagine ci sta dentro, senza allungarla. */
  .pic { position: absolute; inset: 0; display: block; width: 100%; height: 100%; padding: 0; }
  button.pic { cursor: zoom-in; touch-action: manipulation; -webkit-touch-callout: none; transition: transform 120ms ease-out; }
  button.pic:active { transform: scale(.985); }
  button.pic:focus-visible { outline: 2px solid var(--tx); outline-offset: -2px; }
  .pic img { display: block; width: 100%; height: 100%; object-fit: contain; user-select: none; -webkit-user-select: none; }
  .wait { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 12px; text-align: center; font-size: 13.5px; line-height: 18px; color: var(--tx2); }
  .again { min-height: 36px; padding: 0 14px; border-radius: 999px; background: var(--s3); color: var(--tx); font: 600 14px var(--display); touch-action: manipulation; }
  .again:focus-visible { outline: 2px solid var(--tx); outline-offset: 2px; }
  @media (hover: hover) and (pointer: fine) { button.pic:hover img { opacity: .92; } }
  @media (prefers-reduced-motion: reduce) { .area, button.pic { transition: none; } }
</style>
