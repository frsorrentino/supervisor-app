<script lang="ts">
  import type { Suggestion } from './contract'
  import { t } from './t'
  import { options } from './suggestions'
  import ItemList from './ItemList.svelte'
  import { split, suggestion } from './itemRows'

  // «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup docs/mockup/2026-10-24-suggerimenti-home; contratto
  // 1.84): la stessa struttura delle altre schede, al massimo tre voci, ognuna col suo simbolo, «Accetta» tonale, «Parliamone»,
  // «Non ora» e «Mai». Accetta manda il prompt alla master: niente parte senza un tocco. Contratto 1.88: una scelta
  // (`choice`) ha un tasto tonale per opzione al posto di Accetta, come in «Per te».
  let { shown, onAnswer }: { shown: Suggestion[]; onAnswer: (s: Suggestion, action: 'accept' | 'later' | 'never' | 'discuss' | 'choose', choice?: string) => void } = $props()
  // «Altre risposte» (app-home-riordino-29-10): Parliamone, Non ora e Mai dietro un tocco; prima erano dodici tasti in tre voci.
  let more = $state(new Set<string>())
  // Voce 12: la voce esce nel verso della scelta, la successiva sale con la molla. Se la voce resta, torna.
  let leaving = $state<Record<string, string>>({})
  // Una voce risposta esce subito dalle righe; se il relay la tiene, dopo 3 secondi torna.
  function answer(s: Suggestion, action: 'accept' | 'later' | 'never' | 'discuss' | 'choose', option?: string) {
    leaving = { ...leaving, [s.id]: action }
    ;(onAnswer as (a: Suggestion, b: string, c?: string) => void)(s, action, option)
    setTimeout(() => { const { [s.id]: _, ...rest } = leaving; leaving = rest }, 3000)
  }
  const rows = $derived(shown.filter(s => !leaving[s.id]))
</script>

<!-- Nello stile «Studio e Diretta» (30/10): fuori da «Ti aspettano», una lista a filo col binario; il tipo è l'icona grigia
     a sinistra e la parola sopra il testo; a destra «Altre risposte» (da cui si rifiuta) e «Accetta» tonale. -->
<!-- Le voci come righe (app-richiede-te-righe-02-11): pallino ambra, la prima frase, a destra il tipo, sotto il resto; il tocco
     apre il testo intero coi tasti: «Altre risposte» (da cui si rifiuta) e «Accetta» tonale. -->
<ItemList rows={rows.map(suggestion)} testid="suggerimento">{#snippet card(r)}{@const s = rows.find(x => x.id === r.key)}{#if s}
  <span class="txt">
    {#if split(s.text)[1]}<span class="body">{split(s.text)[1]}</span>{/if}
    <span class="btns">
      {#if more.has(s.id)}
        <button class="text" onclick={() => answer(s, 'discuss')}>{t.sugDiscuss}</button>
        <button class="text" onclick={() => answer(s, 'later')}>{t.sugLater}</button>
        <button class="text" onclick={() => answer(s, 'never')}>{t.sugNever}</button>
      {:else}<button class="text" aria-expanded="false" onclick={() => (more = new Set([...more, s.id]))}>{t.sugMore}</button>{/if}
      {#each options(s) as o (o)}<button class="tonal" onclick={() => answer(s, 'choose', o)}>{o}</button>{:else}<button class="tonal" onclick={() => answer(s, 'accept')}>{t.sugAccept}</button>{/each}
    </span>
  </span>{/if}{/snippet}</ItemList>

<style>
  .txt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; overflow-wrap: anywhere; }
  .body { font-size: 15px; line-height: 1.4; color: var(--tx); }
  .btns { display: flex; flex-wrap: wrap; justify-content: flex-end; align-items: center; gap: 6px; margin-top: 8px; }
  .btns button { font: 600 15px var(--display); letter-spacing: .02em; white-space: nowrap; }
  .tonal { min-height: 40px; padding: 0 16px; border-radius: 20px; background: var(--s3); color: var(--tx); }
  .tonal:hover { background: color-mix(in srgb, var(--tx) 8%, var(--s3)); }
  .text { min-height: 40px; padding: 0 10px; border-radius: 20px; color: var(--tx2); }
  .text:hover { background: var(--s2); }
</style>
