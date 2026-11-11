<script lang="ts">
  import type { CmdResult, ResultsPage, ResultsSummary } from './contract'
  import { lastKey, line, ofSession, shown } from './guardare'
  import { newCmd } from './transport'
  import { t } from './t'
  import CatIcon from './CatIcon.svelte'
  // La riga «Risultati» fissa in cima alla chat (app-da-guardare-02-11, mockup punto 2; contratto 1.106): quello che la sessione
  // ha consegnato, l'ultimo a destra; il tocco apre la pagina dei risultati di questa sessione. Senza consegne non c'è.
  let { run, session, summary, onOpen }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; session: string; summary: ResultsSummary | null | undefined; onOpen: () => void } = $props()
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
  function close(e: MouseEvent) {
    e.stopPropagation()
    const k = lastKey(mine)
    if (!k) return
    closed = { ...closed, [session]: k }
    try { localStorage.setItem(CLOSED, JSON.stringify(closed)) } catch { /* resta chiusa finché la pagina è aperta */ }
  }
</script>

{#if shown(mine, closed[session])}
  {@const l = mine.latest ? line(mine.latest) : null}
  <button class="rr" data-testid="chat-risultati" onclick={onOpen}>
    <CatIcon name="eye" size={22} /><b>{t.watchResults}</b><small>{mine.groups.length}</small>
    <span class="sp"></span>{#if mine.groups.some(g => g.unseen)}<i class="dot"></i>{/if}
    {#if l}<span class="last">{l.version != null ? t.watchTitleVersion(l.title, l.version) : l.title}</span>{/if}<span class="ms" aria-hidden="true">chevron_right</span>
    <span class="x" role="button" tabindex="0" aria-label={t.watchResultsClose} title={t.watchResultsClose} onclick={close} onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); close(e as unknown as MouseEvent) } }}><span class="ms" aria-hidden="true">close</span></span>
  </button>
{/if}

<style>
  /* Allineata alle altre righe in cima alla chat (margine come .nudge). */
  .rr { min-height: 48px; display: flex; align-items: center; gap: 10px; box-sizing: border-box; width: calc(100% - 24px); padding: 10px 14px; border-radius: 14px; background: var(--s2); color: var(--tx); text-align: left; margin: 0 12px 8px; }
  .rr b { font-size: 15.5px; }
  .rr small { color: var(--tx2); }
  .sp { flex: 1; }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--wait); flex: none; }
  .x { width: 32px; height: 32px; margin: -6px -8px -6px 0; display: grid; place-items: center; border-radius: 16px; color: var(--tx2); flex: none; }
  .x:hover { background: var(--s3); color: var(--tx); }
  .last { color: var(--tx2); font-size: 14px; white-space: nowrap; overflow: hidden; min-width: 0; }
</style>
