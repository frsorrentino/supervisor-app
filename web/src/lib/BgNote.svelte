<script lang="ts">
  import type { Session } from './contract'
  import { bgWarns } from './decide'
  import { t } from './t'
  // L'avviso fisso nella scheda di una sessione lanciata con --bg (contratto 1.146, app-sessione-bg-avviso-14-11): lì /logout scollega
  // tutte le sessioni del PC. Non si chiude e non ha tasti: resta finché la sessione è viva.
  let { s }: { s: Session } = $props()
</script>

{#if bgWarns(s)}
  <div class="bg" data-testid="bg-avviso" role="note">
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 4 3 20h18L12 4z" /><path d="M12 10v5M12 17.5h.01" /></svg>
    <p>{t.bgLogoutWarning}</p>
  </div>
{/if}

<style>
  .bg { display: flex; gap: 8px; color: var(--text2); padding: 6px 12px; }
  .bg svg { flex: none; margin-top: 2px; color: var(--wait); }
  p { font-size: 14px; }
</style>
