<script lang="ts">
  import { getContext } from 'svelte'
  import type { MonitorCard } from './contract'
  import { outcomeOf, refreshOf, type RefreshCtx } from './monitors'
  import { t } from './t'
  // «Aggiorna adesso» (app-monitoraggi-aggiorna-10-11, contratto 1.133): il tasto (`part` button) o la riga dell'ultimo aggiornamento
  // (`part` outcome), lo stesso nella riga e nella scheda. Fermo; gira e non si ritocca; spento prima di retry_at; manca se il
  // monitoraggio non si aggiorna o il relay non ha l'op. Il tocco non apre la scheda. Come MonitorRefreshButton in Kotlin.
  let { m, part = 'button' }: { m: MonitorCard; part?: 'button' | 'outcome' } = $props()
  const ctx = getContext<RefreshCtx | undefined>('monitor-refresh')
  const st = $derived(ctx ? refreshOf(m, ctx.ops, ctx.now, ctx.asked.includes(m.id)) : null)
  const out = $derived(ctx && refreshOf(m, ctx.ops, ctx.now) ? outcomeOf(m, ctx.failed[m.id]) : null)
  const hm = (at: number) => { const d = new Date(at * 1000), p = (n: number) => String(n).padStart(2, '0'); return `${p(d.getHours())}:${p(d.getMinutes())}` }
</script>

{#if part === 'button' && st}
  <button class="rf" class:spin={st.kind === 'running'} disabled={st.kind !== 'ready'} aria-label={t.monitorRefresh} title={t.monitorRefresh}
    data-testid="monitor-aggiorna-{m.id}" onclick={(e) => { e.stopPropagation(); ctx?.onRefresh(m.id) }}>
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path d="M20 12a8 8 0 1 1-2.34-5.66M20 4v5h-5" /></svg>
  </button>
{:else if part === 'outcome' && out}
  {#if out.kind === 'done'}<small class="ok" data-testid="monitor-aggiornato-{m.id}">{t.monitorRefreshed(hm(out.at))}</small>
  {:else}<small class="err" data-testid="monitor-errore-{m.id}">{out.message ?? (out.timeout ? t.monitorRefreshTimeout : t.monitorRefreshFailed)}</small>{/if}
{/if}

<style>
  .rf { flex: none; width: 40px; height: 40px; display: grid; place-items: center; border-radius: 50%; cursor: pointer; }
  .rf:hover:not(:disabled) { background: var(--s2); }
  .rf:disabled { cursor: default; }
  .rf path { fill: none; stroke: var(--tx); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  .rf:disabled path { stroke: var(--tx3); }
  .rf.spin svg { animation: giro 0.9s linear infinite; }
  @keyframes giro { to { transform: rotate(360deg); } }
  @media (prefers-reduced-motion: reduce) { .rf.spin svg { animation: none; } }
  small { display: block; font-size: 14px; line-height: 19px; white-space: nowrap; overflow: hidden; }
  .ok { color: var(--tx2); }
  .err { color: var(--red); }
</style>
