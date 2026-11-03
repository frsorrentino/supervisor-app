<script lang="ts">
  import type { CmdResult, ResultsPage, ResultsSummary } from './contract'
  import { line, ofSession } from './guardare'
  import { newCmd } from './transport'
  import { t } from './t'
  import CatIcon from './CatIcon.svelte'
  // La riga «Risultati» fissa in cima alla chat (app-da-guardare-02-11, mockup punto 2; contratto 1.106): quello che la sessione
  // ha consegnato, l'ultimo a destra; il tocco apre la pagina dei risultati di questa sessione. Senza consegne non c'è.
  let { run, session, summary, onOpen }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; session: string; summary: ResultsSummary | null | undefined; onOpen: () => void } = $props()
  let page = $state<ResultsPage | null>(null)
  $effect(() => { void session; void summary?.n_unseen; void run(newCmd('results', null)).then(r => { if (r?.ok) try { page = JSON.parse(r.text) as ResultsPage } catch { /* resta com'era */ } }) })
  const mine = $derived(ofSession(page, session))
</script>

{#if mine.latest}
  {@const l = line(mine.latest)}
  <button class="rr" data-testid="chat-risultati" onclick={onOpen}>
    <CatIcon name="eye" size={22} /><b>{t.watchResults}</b><small>{mine.groups.length}</small>
    <span class="sp"></span>{#if mine.groups.some(g => g.unseen)}<i class="dot"></i>{/if}
    <span class="last">{l.version != null ? t.watchTitleVersion(l.title, l.version) : l.title}</span><span class="ms" aria-hidden="true">chevron_right</span>
  </button>
{/if}

<style>
  .rr { display: flex; align-items: center; gap: 10px; width: 100%; padding: 10px 14px; border-radius: 14px; background: var(--s2); color: var(--tx); text-align: left; margin: 4px 0; }
  .rr b { font-size: 16px; }
  .rr small { color: var(--tx2); }
  .sp { flex: 1; }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--wait); flex: none; }
  .last { color: var(--tx2); font-size: 14px; white-space: nowrap; overflow: hidden; min-width: 0; }
</style>
