<script lang="ts">
  import { t } from './t'
  // Una pagina che il PC non ha mandato (app-pwa-decisioni-03-11): invece di «In arrivo dal PC» per sempre, lo dice e si riprova.
  let { onRetry }: { onRetry: () => void } = $props()
</script>

<div class="lf" data-testid="lettura-fallita"><p>{t.pageNoAnswer}</p><button class="tonal" onclick={onRetry}>{t.watchRetry}</button></div>

<style>
  .lf { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 4px 0; }
  .lf p { margin: 0; color: var(--tx2); }
</style>
