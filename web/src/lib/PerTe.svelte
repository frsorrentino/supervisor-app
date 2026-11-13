<script lang="ts">
  import type { CmdResult, ResultsPage, WaitItem } from './contract'
  import { decodeWaits, readPage } from './decideDecode'
  import { newCmd } from './transport'
  import { blockOpen, saveBlock, voci, type Voce as Item } from './perTe'
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
  let { run, session, sig, now, results = true, onOpenWait, onOpenResult, onAnswered = () => {} }: {
    run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; session: string; sig: string; now: number
    /** Il relay conosce `results` (contratto 1.106). */
    results?: boolean
    onOpenWait: (id: string) => void; onOpenResult: (id: string) => void; onAnswered?: (w: WaitItem) => void
  } = $props()
  let waits = $state<WaitItem[]>([]), page = $state<ResultsPage | null>(null)
  let n = 0
  function read() {
    const mine = ++n
    void run(newCmd('attese', null)).then(r => { const { page: p } = readPage(r, decodeWaits, null); if (p && mine === n) waits = p.items })
    if (results) void run(newCmd('results', null)).then(r => { if (r?.ok && mine === n) try { page = JSON.parse(r.text) as ResultsPage } catch { /* resta com'era */ } })
  }
  $effect(() => { void session; void sig; read() })
  // Le voci a cui è già partita la risposta da qui.
  let sent = $state<Record<string, true>>({})
  const items = $derived(voci(waits, page?.groups ?? [], session).filter(v => !sent[v.key]))
  let open = $state(blockOpen('perte'))
  const toggle = () => { open = !open; saveBlock('perte', open) }

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
    sent = { ...sent, [key]: true }
    const r = await run(c)
    if (r?.ok) { read(); return true }
    const { [key]: _gone, ...rest } = sent; sent = rest
    return false
  }
  const sub = (v: Item) => [t.perTeGroup[v.group], v.result ? [t.watchKind[v.result.kind], v.result.version != null ? t.perTeVersion(v.result.version) : null].filter(Boolean).join(', ') : v.context].filter(Boolean).join(' · ')
  const ago = (v: Item) => (v.group === 'aspetta' ? t.tiAspettaSince(age(now - v.since)) : age(now - v.since))
</script>

{#if items.length}{@const top = items[0]}
  <Blocco icon="hand" title={t.forYou} n={items.length} first={top.title} hint={t.perTeHint} {open} amber testid="chat-per-te" onToggle={toggle}>
    {#snippet extra()}{#if !open && mainLabel(top)}<button class="mini" data-testid="chat-per-te-tasto" onclick={(e) => { e.stopPropagation(); void act(top) }}>{mainLabel(top)}</button>{/if}{/snippet}
    {#each items as v (v.key)}
      <Voce group={v.group} title={v.title} sub={sub(v)} ago={ago(v)} main={mainLabel(v)} testid="voce-{v.key}" onOpen={() => openOf(v)} onMain={() => void act(v)} />
    {/each}
  </Blocco>
{/if}

<style>
  .mini { flex: none; font: 600 14.5px var(--display); min-height: 34px; padding: 5px 13px; border-radius: 999px; white-space: nowrap;
    background: color-mix(in srgb, var(--wait) 12%, var(--s1)); color: var(--wait); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 45%, transparent); }
  .mini:hover { filter: brightness(1.12); }
</style>
