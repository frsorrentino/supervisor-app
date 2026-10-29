<script lang="ts">
  import { t } from './t'
  // Proposta 6 (25/10): sei colonnine, una per ora, l'ultima a destra; l'altezza è il numero di eventi dell'ora.
  let { counts }: { counts: number[] } = $props()
  const top = $derived(Math.max(1, ...counts))
</script>

<span class="spark" data-kpi="kpi-6" role="img" aria-label={t.kpi6Aria(counts.reduce((n, c) => n + c, 0))}>
  {#each counts as c, i (i)}<i style:height="{c ? Math.max(3, Math.round((c / top) * 16)) : 2}px" class:zero={!c}></i>{/each}
</span>

<style>
  .spark { display: inline-flex; align-items: flex-end; gap: 2px; height: 16px; flex: none; }
  .spark i { width: 4px; border-radius: 1px; background: var(--busy); }
  .spark i.zero { background: var(--b-track); }
</style>
