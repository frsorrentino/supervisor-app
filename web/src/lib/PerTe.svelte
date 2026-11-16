<script lang="ts">
  import type { CmdResult, ResultsPage, WaitItem } from './contract'
  import { decodeWaits, readPage } from './decideDecode'
  import { newCmd } from './transport'
  import { voci, type Voce as Item } from './perTe'
  import { markAnswered, waits as store } from './attese.svelte'
  import { boxOpen, boxToggle } from './riquadri.svelte'
  import { age, cmdOf } from './tiAspetta'
  import { t } from './t'
  import Blocco from './Blocco.svelte'
  import Voce from './Voce.svelte'
  // «Per te» in cima alla chat di una sessione (app-formato-unico-12-11, Franz 12/11 07:29: formato A e blocchi B): una voce per
  // tutto ciò che aspetta la persona in questa sessione, nell'ordine Ti aspetta, Decidi, Prova, Verifica. Ridotto è una riga con la
  // prima voce e il suo tasto; aperto, l'elenco, ogni voce col tasto principale. Il tocco su una voce apre la sua scheda: quella di
  // «Ti aspetta» per una richiesta, il risultato per una verifica. Un tasto che chiede un testo apre la scheda. Dopo un tasto la voce
  // esce subito, e torna se il PC rifiuta. Le richieste si leggono con `attese`, i risultati con `results`, e si rileggono quando lo
  // stato dice che sono cambiati (`sig`), non a ogni stato.
  // `items` (app-riall-1-dati-16-11, contratto 1.151): le voci dall'archivio unico; senza (relay precedente) «Per te» le legge da sé.
  let { run, session, sig, now, results = true, items: given = null, onOpenWait, onOpenResult, onAnswered = () => {} }: {
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; session: string; sig: string; now: number
    /** Il relay conosce `results` (contratto 1.106). */
    results?: boolean
    items?: WaitItem[] | null
    onOpenWait: (id: string) => void; onOpenResult: (id: string) => void; onAnswered?: (w: WaitItem) => void
  } = $props()
  let waits = $state<WaitItem[]>([]), page = $state<ResultsPage | null>(null)
  let n = 0
  function read() {
    const mine = ++n
    if (!given) void run(newCmd('attese', null)).then(r => { const { page: p } = readPage(r, decodeWaits, null); if (p && mine === n) waits = p.items })
    if (results) void run(newCmd('results', null)).then(r => { if (r?.ok && mine === n) try { page = JSON.parse(r.text) as ResultsPage } catch { /* resta com'era */ } })
  }
  $effect(() => { void session; void sig; read() })
  // Le voci a cui è già partita la risposta, da qui o da un'altra vista: l'insieme è uno solo (attese.svelte.ts).
  const items = $derived(voci(given ?? waits, page?.groups ?? [], session).filter(v => !store.answered.has(v.key)))
  // app-riquadri-chiusi-16-11: chiuso è una pillola; si apre da solo quando c'è una voce che aspetta la persona (non un risultato da
  // guardare), e chiuso a mano resta chiuso finché non ne arriva una nuova. Lo stato resta per sessione.
  const waiting = $derived(items.filter(v => v.group !== 'verifica').map(v => v.key))
  const open = $derived(boxOpen(session, 'perte', waiting))
  const toggle = () => boxToggle(session, 'perte', waiting)

  const mainLabel = (v: Item) => (v.result ? t.watchSeen : v.main?.label ?? null)
  const openOf = (v: Item) => (v.result ? onOpenResult(v.result.id) : onOpenWait(v.key))
  async function act(v: Item) {
    if (v.result) { await fire(v.key, newCmd('result_seen', null, v.result.id)); return }
    const w = v.wait, a = w && v.main ? w.actions?.[v.main.index] : null
    if (!w || !a || !v.main?.direct) { openOf(v); return }
    const c = cmdOf(a, null)
    if (await fire(v.key, { ...newCmd(c.op as Parameters<typeof newCmd>[0], c.session ?? null, c.arg ?? null), ...c } as ReturnType<typeof newCmd>)) onAnswered(w)
  }
  async function fire(key: string, c: ReturnType<typeof newCmd>): Promise<boolean> {
    markAnswered(key)
    const r = await run(c)
    if (r?.ok) { read(); return true }
    markAnswered(key, false)
    return false
  }
  const sub = (v: Item) => [t.perTeGroup[v.group], v.result ? [t.watchKind[v.result.kind], v.result.version != null ? t.perTeVersion(v.result.version) : null].filter(Boolean).join(', ') : v.context].filter(Boolean).join(' · ')
  const ago = (v: Item) => (v.group === 'aspetta' ? t.tiAspettaSince(age(now - v.since)) : age(now - v.since))
</script>

{#if items.length}{@const top = items[0]}
  <Blocco pill icon="hand" title={t.forYou} n={items.length} first={top.title} hint={t.perTeHint} {open} amber testid="chat-per-te" onToggle={toggle}>
    {#each items as v (v.key)}
      <Voce group={v.group} title={v.title} sub={sub(v)} ago={ago(v)} main={mainLabel(v)} testid="voce-{v.key}" onOpen={() => openOf(v)} onMain={() => void act(v)} />
    {/each}
  </Blocco>
{/if}

