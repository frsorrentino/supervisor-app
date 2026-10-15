<script lang="ts">
  import { chromeIntent, inApp } from './inApp'
  import { t } from './t'

  // Dentro il browser di un'app (X, Telegram, Instagram): una barra che porta fuori, chiudibile (Franz, 15/10 21:09). Su
  // Android apre la stessa pagina in Chrome, su iPhone dice come aprirla in Safari.
  const inside = inApp(navigator.userAgent)
  let closed = $state((() => { try { return sessionStorage.getItem('cm.inapp') === '1' } catch { return false } })())
  function close() { closed = true; try { sessionStorage.setItem('cm.inapp', '1') } catch { /* per questa visita */ } }
</script>

{#if inside && !closed}
  <div class="inapp" role="status">
    {#if inside.ios}<span class="iat"><b>{t.inAppWhere(inside.app)}</b><small>{t.inAppIos1} {t.inAppIos2}</small></span>
    {:else}<span class="iat"><b>{t.inAppWhere(inside.app)}</b></span><a class="iago" href={chromeIntent(new URL(location.href))}>{t.inAppChrome}</a>{/if}
    <button class="iax" aria-label={t.closeWord} onclick={close}>
      <svg viewBox="0 0 24 24" width="18" height="18"><path d="M6 6l12 12M18 6 6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg></button>
  </div>
{/if}

<style>
  .inapp { flex: none; display: flex; align-items: center; gap: 10px; padding: 8px 8px 8px 16px; background: var(--surface); border-bottom: 1px solid var(--line); }
  .iat { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; font-size: 14px; }
  .iat small { color: var(--text2); font-size: 13px; }
  .iago { flex: none; padding: 8px 14px; border-radius: 999px; background: var(--primary); color: var(--on-primary); font-size: 14px; font-weight: 500; text-decoration: none; }
  .iax { flex: none; width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
</style>
