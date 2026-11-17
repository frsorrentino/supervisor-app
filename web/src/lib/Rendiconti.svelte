<script lang="ts">
  import type { ContiPage } from './conti'
  import { monthName } from './conti'
  import { locale } from './lang'
  import { t } from './t'
  // app-rendiconti-2-02-11 (contratto 1.103): il mese coi clienti; il tocco su un cliente legge il suo dettaglio (lavori, commit,
  // release misurati) e sotto c'è l'unico tasto pieno, «Chiedi la bozza», che passa la richiesta alla master (`conti_azione`).
  let { page, failed = false, onLoad, onAsk }: {
    page: ContiPage | null; failed?: boolean
    onLoad: (mese: string | null, project: string | null) => void
    onAsk: (mese: string, project: string) => Promise<{ ok: boolean; text: string } | null>
  } = $props()
  let note = $state<string | null>(null), asking = $state(false)
  const open = $derived(page?.dettaglio?.project ?? null)
  const day = (at: number) => new Date(at * 1000).toLocaleDateString(locale, { day: 'numeric', month: 'numeric' })
  function pick(project: string) { note = null; onLoad(page!.mese, open === project ? null : project) }
  async function ask(project: string) {
    if (!page || asking) return
    asking = true
    const r = await onAsk(page.mese, project)
    asking = false
    note = !r ? t.contiAskFailed : r.ok ? t.contiAsked : r.text
  }
</script>

<div class="conti" data-testid="rendiconti">
  {#if !page}<p class="dim">{failed ? t.contiFailed : t.contiLoading}</p>
  {:else}
    {#if page.mesi.length > 1}
      <div class="mesi">{#each page.mesi as m (m)}<button class="chip" class:on={m === page.mese} aria-pressed={m === page.mese} onclick={() => { note = null; onLoad(m, null) }}>{monthName(m)}</button>{/each}</div>
    {/if}
    {#if !page.clienti.length}<p class="dim">{t.contiNone(monthName(page.mese))}</p>{/if}
    {#each page.clienti as c (c.project)}
      {@const d = page.dettaglio && page.dettaglio.project === c.project ? page.dettaglio : null}
      <button class="row" aria-expanded={!!d} data-testid="rendiconti-{c.name}" onclick={() => pick(c.project)}>
        <span class="nm">{c.name}</span>
        <span class="n">{[t.contiTasks(c.n), c.draft_at ? t.contiDraftOn(day(c.draft_at)) : null].filter(Boolean).join(' · ')}</span>
        <span class="ms ar" aria-hidden="true">{d ? 'expand_less' : 'expand_more'}</span>
      </button>
      {#if d}
        <div class="det">
          <p class="meas">{t.contiMeasured(d.measured.tasks, d.measured.commits, d.measured.releases)}</p>
          {#if d.tasks.length}<ul>{#each d.tasks as x (x.task)}<li><span>{x.title}</span><small>{day(x.at)}</small></li>{/each}</ul>{/if}
          {#if d.releases.length}<p class="dim">{t.contiReleases(d.releases.map(r => r.tag).join(', '))}</p>{/if}
          {#if d.git === false}<p class="dim">{t.contiNoGit}</p>{/if}
          <button class="filled" disabled={asking} data-testid="rendiconti-bozza" onclick={() => ask(c.project)}>{c.draft_at ? t.contiAskAgain : t.contiAsk}</button>
          {#if note}<p class="note" role="status">{note}</p>{/if}
        </div>
      {/if}
    {/each}
  {/if}
</div>

<style>
  .conti { display: flex; flex-direction: column; gap: 6px; padding: 4px 0; }
  .mesi { display: flex; flex-wrap: wrap; gap: 6px; padding: 0 14px 6px; }
  .chip { padding: 6px 12px; border-radius: 999px; background: var(--s2); color: var(--tx2); font-size: 14px; }
  .chip.on { background: var(--s3); color: var(--tx); box-shadow: inset 0 0 0 1px var(--tx2); }
  .row { display: flex; align-items: center; gap: 10px; width: 100%; min-height: 44px; padding: 6px 12px 6px 14px; border-top: 1px solid var(--line); text-align: left; color: var(--tx); }
  .nm { font: 600 15px var(--display); flex: none; }
  .n { flex: 1; min-width: 0; text-align: right; color: var(--tx2); font-size: 14px; }
  .ar { color: var(--tx3); }
  .det { display: flex; flex-direction: column; gap: 8px; padding: 0 14px 12px; }
  .meas { font-size: 15px; }
  ul { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
  li { display: flex; gap: 10px; justify-content: space-between; font-size: 14px; color: var(--tx2); }
  li small { color: var(--tx3); flex: none; }
  .filled { align-self: flex-start; padding: 9px 16px; border-radius: 999px; background: var(--tx); color: var(--bg); font: 600 14.5px var(--display); }
  .filled:disabled { opacity: .5; }
  .dim { color: var(--tx2); font-size: 14px; padding: 0 14px; }
  .det .dim { padding: 0; }
  .note { font-size: 14px; color: var(--tx2); }
</style>
