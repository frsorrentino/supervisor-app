<script lang="ts">
  import type { KpiChecksDay } from './contract'
  import KpiCard from './KpiCard.svelte'
  import KpiDays from './KpiDays.svelte'
  import { checksTotals } from './kpi'
  import { t } from './t'
  // Proposta 9 (25/10, contratto 1.96): i controlli dei compiti per giorno, i verdi in colonna e i rossi sopra, in fondo a «Fatto».
  let { days }: { days: KpiChecksDay[] } = $props()
  const tot = $derived(checksTotals(days))
  const top = $derived(Math.max(1, ...days.map(d => d.green + d.red)))
</script>

<KpiCard kpi="kpi-9" title={t.kpi9Title}>
  {#snippet right()}<span class="g">{t.kpi9Green(tot[0])}</span> · <span class:r={tot[1] > 0}>{t.kpi9Red(tot[1])}</span>{/snippet}
  <KpiDays aria={`${t.kpi9Title}: ${t.kpi9Green(tot[0])}, ${t.kpi9Red(tot[1])}`} cols={days.map(d => ({ date: d.date, label: String(d.green), red: d.red, parts: [{ f: d.green / top, color: 'var(--good)' }, { f: d.red / top, color: 'var(--b-alert)' }] }))} />
  <p>{t.kpi9Caption}</p>
</KpiCard>

<style>
  .g { color: var(--good); } .r { color: var(--b-alert); }
  p { margin: 0; font-size: 12.5px; color: var(--text2); }
</style>
