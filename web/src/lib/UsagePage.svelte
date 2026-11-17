<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { KpiData, State, TimelinePage } from './contract'
  import { dayFigures, split } from './kpi'
  import Kpi4 from './Kpi4.svelte'
  import Kpi17 from './Kpi17.svelte'
  import Kpi18 from './Kpi18.svelte'
  import Pagella from './Pagella.svelte'
  import { pagellaRows, type Pagella as PagellaData } from './conti'
  import { t } from './t'
  // La pagina «Utilizzo e limiti» (scelta D, 30/10 20:11): quello che il blocco della home non mostra più.
  let { panels, st, page, data, now, pagella = null }: { panels: Snippet; st: State; page: TimelinePage | null; data: KpiData | null; now: number; pagella?: PagellaData | null } = $props()
</script>

<div class="usage v3" data-testid="usage-page">
  {@render panels()}
  {#each Object.keys(st.quota) as a (a)}{@const sp = split(st, a)}{#if sp}<Kpi4 s={sp} />{/if}{/each}
  {#if page}<Kpi18 f={dayFigures(page, st, now)} />{/if}
  {#if data?.kpi_waits}<Kpi17 w={data.kpi_waits} {now} />{/if}
  <!-- app-rendiconti-2-02-11: la pagella anche qui, solo con righe da 10 casi in su. -->
  {#if pagellaRows(pagella).length}<section class="pg"><h3>{t.pagellaTitle}</h3><Pagella pagella={pagella!} /></section>{/if}
</div>

<style>
  .pg { border-radius: 14px; background: var(--s1); border: 1px solid var(--line); padding-top: 12px; }
  .pg h3 { font: 600 17px var(--display); padding: 0 14px 6px; }
  .usage { display: flex; flex-direction: column; gap: 12px; padding: 0 12px 24px; max-width: 760px; }
</style>
