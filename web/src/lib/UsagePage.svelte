<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { KpiData, State, TimelinePage } from './contract'
  import { dayFigures, split } from './kpi'
  import Kpi4 from './Kpi4.svelte'
  import Kpi17 from './Kpi17.svelte'
  import Kpi18 from './Kpi18.svelte'
  // La pagina «Utilizzo e limiti» (scelta D, 30/10 20:11): quello che il blocco della home non mostra più.
  let { panels, st, page, data, now }: { panels: Snippet; st: State; page: TimelinePage | null; data: KpiData | null; now: number } = $props()
</script>

<div class="usage v3" data-testid="usage-page">
  {@render panels()}
  {#each Object.keys(st.quota) as a (a)}{@const sp = split(st, a)}{#if sp}<Kpi4 s={sp} />{/if}{/each}
  {#if page}<Kpi18 f={dayFigures(page, st, now)} />{/if}
  {#if data?.kpi_waits}<Kpi17 w={data.kpi_waits} {now} />{/if}
</div>

<style>
  .usage { display: flex; flex-direction: column; gap: 12px; padding: 0 12px 24px; max-width: 760px; }
</style>
