<script module lang="ts">
  export type Page = 'launch' | 'lavori' | 'night' | 'overview' | 'search' | 'settings' | 'homeorder' | 'usage' | 'queue' | 'box-aspetta' | 'box-lavoro' | 'box-esiti'
</script>

<script lang="ts">
  import type { State } from './contract'
  import { personal } from './badge'
  import { freshness } from './durations'
  import { t } from './t'
  import { resetLabel } from './header'
  import { shown } from './quotaLine'

  // La testata della home (PageHeader dell'app): «Master · N aperte», la lente e il menu ≡; sotto, la quota in una riga
  // (QuotaLine): per ogni account la sua forma, una barra fina delle 5 ore e la percentuale; ambra da 75 %, rossa da 90 %.
  // `menuOnly`: solo il tasto ≡ col suo pannello, nella testata della master a tutto schermo (Franz, 08/10 18:15).
  // `quota`: falso nella home in stile «Studio e Diretta» (30/10), dove la quota sta nel tabellone.
  let { st, now, openCount, onPage, menuOnly = false, quota = true }: { st: State; now: number; openCount: number; onPage: (p: Page) => void; menuOnly?: boolean; quota?: boolean } = $props()
  let menu = $state(false)
  // Niente più «N per te» qui: chi aspetta lo dice il primo box della home (app-home-riordino-29-10).
  // Il pannello scende dal tasto ≡, non dal bordo dello schermo: sulla plancia con la home a sinistra resta accanto al tasto.
  let panelRight = $state(12)
  function openMenu(e: MouseEvent) { panelRight = Math.max(12, innerWidth - (e.currentTarget as HTMLElement).getBoundingClientRect().right); menu = true }
  const fresh = $derived(freshness(st, now))
  const updated = $derived(fresh.stale ? t.updatedAgo(fresh.minutes) : fresh.slowS != null ? t.updatedSlow(fresh.slowS) : t.updatedNow)
  // Con un account non aggiornato si vede solo l'altro, a tutta larghezza e con l'ora in cui si azzera.
  const rings = $derived(shown(Object.entries(st.quota)))
  const tone = (pct: number, stale: boolean) => (stale ? 'var(--text2)' : pct >= 90 ? 'var(--b-alert)' : pct >= 75 ? 'var(--b-warn)' : 'var(--b-ring)')
  function go(p: Page) { menu = false; onPage(p) }
  let liveNote = $state(false)
  const entries: { page: Page; title: string; sub: string; d: string[] }[] = [
    { page: 'launch', title: t.menuLaunch, sub: t.menuLaunchSub, d: ['M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z', 'm12 15-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z', 'M9 12H4s.55-3.03 2-4c1.62-1.08 5 0 5 0', 'M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5'] },
    // La pagina unica «Lavori» (22/10): al posto di Registro e Recap.
    { page: 'lavori', title: t.menuLavori, sub: t.menuLavoriSub, d: ['M13 5h8', 'M13 12h8', 'M13 19h8', 'm3 17 2 2 4-4', 'M3 4h6v6H3z'] },
    { page: 'night', title: t.menuNight, sub: t.menuNightSub, d: ['M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9z'] },
    { page: 'overview', title: t.menuQuadro, sub: t.menuQuadroSub, d: ['M21 12a9 9 0 1 1-9-9', 'M12 7a5 5 0 1 0 5 5'] },
    { page: 'search', title: t.menuSearch, sub: t.menuSearchSub, d: ['m21 21-4.34-4.34', 'M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0'] },
  ]
</script>

<svelte:window onkeydown={(e) => e.key === 'Escape' && (menu = false)} />

{#if menuOnly}
  <button class="ib only" aria-label={t.menu} title={t.menu} aria-expanded={menu} onclick={(e) => { e.stopPropagation(); openMenu(e) }}>
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
  </button>
{:else}
<header>
  <div class="bar">
    <span class="title"><b>{quota ? t.summaryTitle : t.appTitle}</b><span class="ver" data-testid="versione-titolo">{__APP_VERSION__}</span><span class="open">{t.summaryOpen(openCount)}</span></span>
    <button class="ib" aria-label={t.search} title={t.search} onclick={() => onPage('search')}>
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round"><path d="m21 21-4.34-4.34" /><circle cx="11" cy="11" r="8" /></svg>
    </button>
    <button class="ib" aria-label={t.menu} title={t.menu} aria-expanded={menu} onclick={openMenu}>
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
    </button>
  </div>
  {#if quota && rings.length}
    <button class="quota" onclick={() => onPage('overview')} aria-label={t.menuQuadro}>
      {#each rings as [name, r]}
        {@const pct = Math.min(100, Math.max(0, r.h5 ?? 0))}
        {@const stale = !!r.stale}
        {@const reset = rings.length === 1 && !stale ? resetLabel(r.reset_h5, now) : null}
        <span class="q">
          <svg viewBox="0 0 14 14" width="12" height="12" aria-label={personal(name, r.kind) ? t.badgePersonal : t.badgeWork}>
            {#if personal(name, r.kind)}<circle cx="7" cy="7" r="5.5" fill="none" stroke="var(--text2)" stroke-width="2" />{:else}<rect x="1" y="1" width="12" height="12" rx="3.2" fill="none" stroke="var(--text2)" stroke-width="2" />{/if}
          </svg>
          <span class="mono win">{t.quotaLineWindow}</span>
          <span class="track">{#if !stale}<i style="width:{pct}%;background:{tone(pct, stale)}"></i>{/if}</span>
          <span class="mono" style="color:{pct >= 75 && !stale ? tone(pct, stale) : 'var(--text2)'}">{stale ? t.quotaLineStale : `${pct}%`}</span>
          {#if reset}<span class="mono">· {t.quotaResetsAt(reset)}</span>{/if}
        </span>
      {/each}
    </button>
  {/if}
</header>
{#if fresh.stale}<p class="stalebar">{t.staleBanner(fresh.minutes)}</p>{/if}
{/if}

{#if menu}
  <div class="scrim" role="presentation" onclick={(e) => { e.stopPropagation(); menu = false }}></div>
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <div class="panel v3" data-testid="menu-v3" role="menu" tabindex="-1" style:right="{panelRight}px" onclick={(e) => e.stopPropagation()}>
    <div class="ph">
      <i class="led" class:stale={fresh.stale}></i>
      <span>{t.menuConnected(st.host || t.menuPc, updated)}</span>
      <button class="ib" aria-label={t.closeWord} onclick={() => (menu = false)}><svg viewBox="0 0 24 24" width="22" height="22"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
    </div>
    {#each entries as e}
      <button class="mi" role="menuitem" onclick={() => go(e.page)}>
        <span class="mic"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{#each e.d as d}<path {d} />{/each}</svg></span>
        <span class="mt"><b>{e.title}</b><small>{e.sub}</small></span>
      </button>
    {/each}
    <!-- La Modalità live dell'app (AppMenuPanel.kt) vive di un servizio Android e dell'orologio: qui la voce dice dove si trova
         (Franz, 15/10 21:48: «voce che rimanda all'app»). -->
    <button class="mi" role="menuitem" aria-expanded={liveNote} onclick={() => (liveNote = !liveNote)}>
      <span class="mic"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3" /></svg></span>
      <span class="mt"><b>{t.liveMenu}</b><small class:wrap={liveNote}>{liveNote ? t.liveOnlyApp : t.liveMenuSub}</small></span>
    </button>
    {#if liveNote}<a class="mi applink" href="https://github.com/frsorrentino/supervisor-app" target="_blank" rel="noopener"><span class="mic"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /></svg></span><span class="mt"><b>{t.liveAppLink}</b></span></a>{/if}
    <hr />
    <button class="foot" role="menuitem" onclick={() => go('settings')}>
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" /><circle cx="12" cy="12" r="3" /></svg>
      <span>{t.settingsTitle}</span>
    </button>
  </div>
{/if}

<style>
  .applink { text-decoration: none; color: var(--icon); }
  .applink b { color: var(--icon); }
  .stalebar { margin: 0; padding: 6px 20px; background: var(--brief-warn, #5a4300); color: var(--brief-warn-ink, #ffd77a); font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  header { padding: 4px 8px 6px; }
  .bar { display: flex; align-items: center; min-height: 56px; }
  .title { flex: 1; display: flex; align-items: baseline; gap: 10px; padding-left: 12px; min-width: 0; }
  .title b { font-size: 22px; font-weight: 400; }
  .open { font-size: 14px; color: var(--text2); white-space: nowrap; }
  .ib { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; flex: none; }
  .ib:hover { background: var(--surface); }
  .ib.only:hover { background: var(--master-key); }
  .win { color: var(--text2); }
  .quota { display: flex; gap: 16px; width: calc(100% - 16px); margin: 0 8px; padding: 4px 0; border-radius: 8px; }
  .q { flex: 1; display: flex; align-items: center; gap: 8px; min-width: 0; }
  .q .mono { white-space: nowrap; }
  .track { flex: 1; height: 3px; border-radius: 2px; background: var(--b-track); overflow: hidden; }
  .track i { display: block; height: 100%; }
  .scrim { position: fixed; inset: 0; z-index: 20; background: rgb(0 0 0 / .55); animation: fade .15s; }
  .panel { position: fixed; z-index: 21; top: 56px; left: 12px; right: 12px; max-height: calc(100% - 80px); overflow-y: auto; background: var(--surface); border-radius: 28px; padding: 8px 0; box-shadow: 0 12px 40px rgb(0 0 0 / .5); transform-origin: top right; animation: pop .2s cubic-bezier(.2, .8, .2, 1); }
  /* Su tablet e desktop largo 380 dalla parte del tasto che lo apre. */
  @media (min-width: 600px) { .panel { left: auto; width: 380px; } }
  @keyframes pop { from { opacity: 0; scale: .96; } }
  @keyframes fade { from { opacity: 0; } }
  .ph { display: flex; align-items: center; gap: 10px; padding: 0 8px 0 20px; font-size: 14px; color: var(--text2); }
  .ph span { flex: 1; white-space: nowrap; overflow: hidden; }
  .led { width: 8px; height: 8px; border-radius: 50%; background: var(--idle); flex: none; }
  .led.stale { background: var(--wait); }
  .mi { display: flex; align-items: center; gap: 16px; width: 100%; min-height: 64px; padding: 10px 20px; text-align: left; }
  .mi:hover, .foot:hover { background: var(--high); }
  .mic { width: 40px; height: 40px; border-radius: 50%; background: var(--high); display: grid; place-items: center; flex: none; }
  .mt { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
  .mt b { font-size: 16px; font-weight: 500; white-space: nowrap; overflow: hidden; }
  .mt small { font-size: 12.5px; color: var(--text2); white-space: nowrap; overflow: hidden; }
  .mt small.wrap { white-space: normal; line-height: 1.35; }
  hr { border: 0; height: 1px; background: var(--line); margin: 4px 0; }
  .foot { display: flex; align-items: center; gap: 20px; width: 100%; padding: 14px 28px; font-size: 16px; color: var(--text2); }
  @media (prefers-reduced-motion: reduce) { .panel, .scrim { animation: none; } }
  /* Il menu nella forma della v3 (mockup approvato da Franz il 30/10 alle 19:25): icone grigie nel binario senza cerchio, una
     voce per riga col filo, «Lancia una sessione» come le altre (Franz 31/10 01:03). */
  .panel { background: var(--s1); border: 1px solid var(--line); border-radius: 14px; padding: 0; }
  .ph { padding: 8px 8px 8px 16px; border-bottom: 1px solid var(--line); color: var(--tx2); }
  .led { width: 7px; height: 7px; background: var(--good); }
  .mi, .foot { display: flex; align-items: center; gap: 8px; width: 100%; min-height: 58px; padding: 9px 12px 9px 0; border-top: 1px solid var(--line); text-align: left; font-size: inherit; color: var(--tx); }
  .mi:hover, .foot:hover { background: var(--s2); }
  .mic { width: 52px; height: auto; background: none; border-radius: 0; }
  .mt b { font: 600 17px/1.2 var(--display); white-space: normal; }
  .mt small { font-size: 14px; line-height: 1.3; color: var(--tx2); white-space: normal; }
  .foot svg { margin: 0 15px; }
  .foot span { font: 600 17px var(--display); }
  hr { display: none; }
  /* Nella home in stile «Studio e Diretta» (30/10): il titolo in Barlow Semi Condensed, le icone chiare. */
  .ver { font: 400 12.5px var(--mono); color: var(--tx2); margin-left: 8px; }
  /* Accanto al titolo solo se c'è spazio (Franz 30/10 22:01). */
  @media (max-width: 399px) { .ver { display: none; } }
  :global(.v3) .title b { font: 700 22px var(--display); letter-spacing: .005em; }
  :global(.v3) .open { font-size: 15px; color: var(--tx2); }
  :global(.v3) header { padding: 4px 4px 2px; }
</style>
