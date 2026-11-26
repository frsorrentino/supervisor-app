<script lang="ts">
  import type { CmdResult, ResultsPage, WaitItem } from './contract'
  import { decodeWaits, readPage } from './decideDecode'
  import { cala } from './molla'
  import { newCmd } from './transport'
  import { voci, type Voce as Item } from './perTe'
  import { results as shared, waits as store } from './attese.svelte'
  import { boxOpen, boxToggle } from './riquadri.svelte'
  import { actVoce, adviceText, agoOf, mainLabelOf, silenceOf, subOf } from './voceRow'
  import { t } from './t'
  import { stackEdges } from './animazioni'
  import Blocco from './Blocco.svelte'
  import Voce from './Voce.svelte'
  // «Per te» in cima alla chat di una sessione (app-formato-unico-12-11, Franz 12/11 07:29: formato A e blocchi B): una voce per
  // tutto ciò che aspetta la persona in questa sessione, nell'ordine Ti aspetta, Decidi, Prova, Verifica. Ridotto è una riga con la
  // prima voce e il suo tasto; aperto, l'elenco, ogni voce col tasto principale. Il tocco su una voce apre la sua scheda: quella di
  // «Ti aspetta» per una richiesta, il risultato per una verifica. Un tasto che chiede un testo apre la scheda. Dopo un tasto la voce
  // esce subito, e torna se il PC rifiuta. Le richieste si leggono con `attese`, i risultati con `results`, e si rileggono quando lo
  // stato dice che sono cambiati (`sig`), non a ogni stato.
  // `items` (app-riall-1-dati-16-11, contratto 1.151): le voci dall'archivio unico; senza (relay precedente) «Per te» le legge da sé.
  // app-riall-2-viste-16-11 (direzione A): `onMaster` è il rimando «anche in Dalla master ›» in testa alla lista; sullo schermo largo
  // (`wide`) la colonna della sessione tiene solo il numero, e il tocco mostra il suo gruppo nella colonna Dalla master. Se quella
  // colonna non c'è (`onMaster` risponde false), la lista si apre qui come prima.
  let { run, session, sig, now, results = true, items: given = null, wide = false, onMaster = null, onOpenWait, onOpenResult, onAnswered = () => {} }: {
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; session: string; sig: string; now: number
    /** Il relay conosce `results` (contratto 1.106). */
    results?: boolean
    items?: WaitItem[] | null
    wide?: boolean; onMaster?: ((session: string) => boolean) | null
    onOpenWait: (id: string) => void; onOpenResult: (id: string) => void; onAnswered?: (w: WaitItem) => void
  } = $props()
  let waits = $state<WaitItem[]>([]), page = $state<ResultsPage | null>(null)
  let n = 0
  function read() {
    const mine = ++n
    if (!given) void run(newCmd('attese', null)).then(r => { const { page: p } = readPage(r, decodeWaits, null); if (p && mine === n) waits = p.items })
    // Con l'archivio unico i risultati li legge l'app, una volta per tutte le viste (attese.svelte.ts).
    if (results && !given) void run(newCmd('results', null)).then(r => { if (r?.ok && mine === n) try { page = JSON.parse(r.text) as ResultsPage } catch { /* resta com'era */ } })
  }
  $effect(() => { void session; void sig; read() })
  // Le voci a cui è già partita la risposta, da qui o da un'altra vista: l'insieme è uno solo (attese.svelte.ts).
  const items = $derived(voci(given ?? waits, (given ? shared.groups : page?.groups) ?? [], session).filter(v => !store.answered.has(v.key)))
  // app-riquadri-chiusi-16-11: chiuso è una pillola; si apre da solo quando c'è una voce che aspetta la persona (non un risultato da
  // guardare), e chiuso a mano resta chiuso finché non ne arriva una nuova. Lo stato resta per sessione.
  const waiting = $derived(items.filter(v => v.group !== 'verifica').map(v => v.key))
  const open = $derived(boxOpen(session, 'perte', waiting))
  const toggle = () => boxToggle(session, 'perte', waiting)
  // Sul largo la lista non si apre qui: se la colonna Dalla master non c'è, si apre qui solo per questo tocco.
  let here = $state(false)
  const tap = () => { if (!wide) toggle(); else if (!onMaster?.(session)) here = !here }

  const openOf = (v: Item) => (v.result ? onOpenResult(v.result.id) : onOpenWait(v.key))
  // La riga e il suo tasto, gli stessi di Dalla master (voceRow.ts).
  const act = (v: Item) => actVoce(run, v, { open: openOf, reread: read, onAnswered })
</script>

{#if items.length}{@const top = items[0]}
  <Blocco pill stack={stackEdges(items.length)} fan={items.slice(1, 3).map((x) => x.title)} icon="hand" title={t.forYou} n={items.length} first={top.title} hint={t.perTeHint} open={wide ? here : open} amber testid="chat-per-te" onToggle={tap}>
    {#if onMaster && !wide}<button class="also" data-testid="per-te-in-master" onclick={() => onMaster(session)}>{t.perTeAlsoMaster}<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>{/if}
    {#each items as v, i (v.key)}
      <div use:cala={i}><Voce group={v.group} title={v.title} sub={subOf(v)} ago={agoOf(v, now)} silence={silenceOf(v, now)} advice={adviceText(v.advice)} main={mainLabelOf(v)} rec={!!v.main?.recommended} testid="voce-{v.key}" onOpen={() => openOf(v)} onMain={() => void act(v)} /></div>
    {/each}
  </Blocco>
{/if}

<style>
  .also { display: flex; align-items: center; justify-content: flex-end; gap: 2px; width: 100%; min-height: 36px; padding: 0 12px; border-top: 1px solid var(--line); font-size: 13px; color: var(--tx2); }
  .also:hover { color: var(--tx); }
</style>
