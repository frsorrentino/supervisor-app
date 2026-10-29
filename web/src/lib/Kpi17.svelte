<script lang="ts">
  import type { KpiWaits } from './contract'
  import KpiCard from './KpiCard.svelte'
  import KpiDays from './KpiDays.svelte'
  import { waitsClosed, waitsTop } from './kpi'
  import { t } from './t'
  // Proposta 17 (25/10, contratto 1.96), in «Utilizzo»: chi ti aspetta adesso e, per giorno, la mediana dei minuti fra
  // la richiesta di una sessione e la tua risposta, scritta sopra la colonna.
  let { w, now }: { w: KpiWaits; now: number } = $props()
  const closed = $derived(waitsClosed(w))
  const top = $derived(Math.max(1, waitsTop(w)))
</script>

<KpiCard kpi="kpi-17" title={t.kpi17Title}>
  {#snippet right()}<span class="d">{t.kpi17Closed(closed)}</span>{/snippet}
  {#each w.now.slice().sort((a, b) => a.since - b.since) as x (x.session + x.kind)}
    <p class="now">{t.kpi17Now(x.session, t.kpi17Kind[x.kind] ?? x.kind, Math.max(1, Math.round((now - x.since) / 60)))}</p>
  {/each}
  <KpiDays aria={t.kpi17Caption} cols={w.week.map(d => ({ date: d.date, label: d.median_min != null ? String(Math.round(d.median_min)) : '–', parts: [{ f: (d.median_min ?? 0) / top, color: 'var(--wait)' }] }))} />
  <p>{t.kpi17Caption}</p>
</KpiCard>

<style>
  .d { color: var(--text2); }
  p { margin: 0; font-size: 12.5px; color: var(--text2); overflow-wrap: anywhere; }
  p.now { color: var(--wait); }
</style>
