<script lang="ts">
  import type { CmdResult, ResultsPage, ResultsSummary } from './contract'
  import { lastKey, line, ofSession, shown } from './guardare'
  import { blockOpen, saveBlock } from './perTe'
  import { newCmd } from './transport'
  import { t } from './t'
  import Blocco from './Blocco.svelte'
  // La riga «Risultati» fissa in cima alla chat (app-da-guardare-02-11, mockup punto 2; contratto 1.106): quello che la sessione
  // ha consegnato. Senza consegne non c'è. Dal formato unico (app-formato-unico-12-11, blocchi B) è un blocco ridotto: una riga con
  // l'ultimo lavoro non visto; il tocco lo apre sul posto, coi lavori della sessione uno per riga, e resta come lasciato. Il tocco
  // su un lavoro apre la pagina dei risultati di questa sessione su quel lavoro; «Tutti» la apre intera.
  let { run, session, summary, onOpen }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; session: string; summary: ResultsSummary | null | undefined; onOpen: (id: string | null) => void } = $props()
  let page = $state<ResultsPage | null>(null)
  // Si rilegge quando cambia il riassunto dei risultati, non a ogni stato (app-letture-leggere-11-11); le righe di più colonne
  // partono insieme e diventano una lettura sola (LightTransport).
  const sig = $derived(JSON.stringify(summary ?? null))
  $effect(() => { void session; void sig; void run(newCmd('results', null)).then(r => { if (r?.ok) try { page = JSON.parse(r.text) as ResultsPage } catch { /* resta com'era */ } }) })
  const mine = $derived(ofSession(page, session))
  // La X la chiude fino al risultato successivo (Guardare.lastKey, app-risultati-x-05-11), ricordata in questo browser.
  const CLOSED = 'cm.results_closed'
  const readClosed = (): Record<string, string> => { try { return JSON.parse(localStorage.getItem(CLOSED) ?? '{}') } catch { return {} } }
  let closed = $state<Record<string, string>>(readClosed())
  function close(e: Event) {
    e.stopPropagation()
    const k = lastKey(mine)
    if (!k) return
    closed = { ...closed, [session]: k }
    try { localStorage.setItem(CLOSED, JSON.stringify(closed)) } catch { /* resta chiusa finché la pagina è aperta */ }
  }
  let open = $state(blockOpen('risultati'))
  const toggle = () => { open = !open; saveBlock('risultati', open) }
  const name = (l: ReturnType<typeof line>) => (l.version != null ? t.watchTitleVersion(l.title, l.version) : l.title)
</script>

{#if shown(mine, closed[session])}
  {@const l = mine.latest ? line(mine.latest) : null}
  <Blocco icon="eye" title={t.watchResults} n={mine.groups.length} first={l ? name(l) : null} {open} testid="chat-risultati" onToggle={toggle}>
    {#snippet extra()}
      {#if mine.groups.some(g => g.unseen)}<i class="dot"></i>{/if}
      <span class="x" role="button" tabindex="0" aria-label={t.watchResultsClose} title={t.watchResultsClose} onclick={close} onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); close(e) } }}><span class="ms" aria-hidden="true">close</span></span>
    {/snippet}
    {#each mine.groups as g (g.title + g.at)}{@const top = g.items[0]}
      {#if top}{@const gl = line(top)}
        <button class="row" data-testid="chat-risultato-{top.id}" onclick={() => onOpen(top.id)}>
          <i class="dot" class:off={!g.unseen}></i><span class="ti">{name(gl)}</span><small>{t.watchKind[gl.kind]}</small><span class="ms" aria-hidden="true">chevron_right</span>
        </button>
      {/if}
    {/each}
    <button class="row all" data-testid="chat-risultati-tutti" onclick={() => onOpen(null)}><span class="ti">{t.watchAll}</span><span class="ms" aria-hidden="true">chevron_right</span></button>
  </Blocco>
{/if}

<style>
  .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--wait); flex: none; }
  .dot.off { background: none; }
  .x { width: 32px; height: 32px; margin: 0 -4px; display: grid; place-items: center; border-radius: 16px; color: var(--tx2); flex: none; }
  .x:hover { background: var(--s3); color: var(--tx); }
  .row { display: flex; align-items: center; gap: 10px; width: 100%; min-height: 44px; padding: 8px 12px; border-top: 1px solid var(--line); text-align: left; color: var(--tx); }
  .row:hover { background: var(--s2); }
  .ti { flex: 1; min-width: 0; font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .row small { color: var(--tx2); font-size: 13px; white-space: nowrap; }
  .row .ms { color: var(--tx3); }
  .row.all .ti { color: var(--tx2); font-size: 14px; }
</style>
