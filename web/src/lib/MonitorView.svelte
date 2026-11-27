<script lang="ts">
  import MonitorChart from './MonitorChart.svelte'
  import type { MonitorCard } from './contract'
  import { alertLine, isLook, pageUrl, valueText } from './monitors'
  import { t } from './t'
  import MonitorRefresh from './MonitorRefresh.svelte'
  import LongText from './LongText.svelte'
  // La scheda di un monitoraggio (app-monitoraggi-09-11, mockup B e C), come MonitorPage in Kotlin: il titolo, la fonte e l'ora
  // dell'ultimo valore; il riquadro con lo stato e la frase; i grafici; gli avvisi; la nota «Esempio»; Ferma e Chiedi alla master,
  // un solo bottone pieno. `wide`: nella colonna della plancia, con la × al posto della freccia.
  let { m, wide = false, onBack, onStop, onAsk, onEditAlerts }: {
    m: MonitorCard; wide?: boolean; onBack: () => void; onStop: () => void; onAsk: () => void; onEditAlerts: () => void
  } = $props()
  const dm = (at: number) => {
    const d = new Date(at * 1000), p = (n: number) => String(n).padStart(2, '0')
    return `${p(d.getDate())}/${p(d.getMonth() + 1)} ${p(d.getHours())}:${p(d.getMinutes())}`
  }
  // app-play-lettura-ovunque-27-11: la frase del riquadro e gli avvisi col ▶ accanto, se vanno oltre una riga; si leggono questi testi.
  const verdict = $derived(m.verdict ?? [valueText(m), m.delta].filter(Boolean).join(' · '))
  const alertsText = $derived(alertLine(m).map(a => a.text).join(' · '))
</script>

<section class="mv" class:wide data-testid="monitor-scheda-{m.id}">
  <header>
    {#if !wide}<button class="ic" aria-label={t.back} onclick={onBack}><svg viewBox="0 0 24 24" width="24" height="24"><path d="M15 5l-7 7 7 7" fill="none" stroke="var(--tx)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>{/if}
    <span class="tt"><b>{m.title}</b><small>{[m.source, m.every ? t.monitorEvery(m.every) : null, m.at ? t.monitorLast(dm(m.at)) : null].filter(Boolean).join(' · ')}</small><MonitorRefresh {m} part="outcome" /></span>
    <MonitorRefresh {m} />
    {#if wide}<button class="ic" aria-label={t.closeWord} onclick={onBack}><svg viewBox="0 0 24 24" width="20" height="20"><path d="M6 6l12 12M18 6L6 18" fill="none" stroke="var(--tx2)" stroke-width="2" stroke-linecap="round" /></svg></button>{/if}
  </header>
  <div class="body">
    <!-- Contratto 1.138 (app-monitor-link-12-11): con la pagina della cosa guardata il riquadro si tocca e la apre nel browser. -->
    <!-- app-play-lettura-ovunque-27-11: il ▶ non può stare dentro un link. Il riquadro si tocca ancora tutto: il link è «Apri la pagina»,
         steso su tutto il riquadro (::after), e il ▶ gli sta sopra. -->
    {#if pageUrl(m.url)}
      <div class="state link" class:look={isLook(m)}><p class="lb"><i></i>{isLook(m) ? t.monitorStateLook : t.monitorStateOk}</p><div class="vd"><LongText text={verdict} /></div><a class="go" href={pageUrl(m.url)} target="_blank" rel="noopener" data-testid="monitor-pagina">{t.monitorOpenPage}<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M8 16L16 8M9 8h7v7" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></a></div>
    {:else}
      <div class="state" class:look={isLook(m)}><p class="lb"><i></i>{isLook(m) ? t.monitorStateLook : t.monitorStateOk}</p><LongText text={verdict} /></div>
    {/if}
    <MonitorChart c={m} />
    {#if m.alerts.length}<div class="alerts"><div class="al"><LongText text={alertsText}>{#snippet body()}{t.monitorAlerts} · {#each alertLine(m) as a, i}{#if i}{' · '}{/if}<span class:hit={a.hit}>{a.text}</span>{/each}{/snippet}</LongText></div><button data-testid="monitor-avvisi" onclick={onEditAlerts}>{t.monitorEdit}</button></div>{/if}
    {#if m.example}<p class="ex"><em>{t.monitorExample}</em> {t.monitorExampleText}</p>{/if}
  </div>
  <footer>
    <button class="btn" data-testid="monitor-ferma" onclick={onStop}>{t.monitorStop}</button>
    <button class="btn main" data-testid="monitor-chiedi" onclick={onAsk}>{t.monitorAsk}</button>
  </footer>
</section>

<style>
  .mv { display: flex; flex-direction: column; height: 100%; background: var(--bg); color: var(--tx); }
  .wide { background: transparent; }
  header { display: flex; align-items: center; gap: 6px; padding: 10px 6px 6px; }
  .wide header { padding-left: 18px; }
  .ic { width: 44px; height: 44px; display: grid; place-items: center; border-radius: 50%; flex: none; }
  .tt { flex: 1; min-width: 0; display: flex; flex-direction: column; }
  .tt b { font: 700 22px/26px var(--display); }
  .tt small { font-size: 14px; line-height: 19px; color: var(--tx2); }
  .body { flex: 1; overflow-y: auto; padding: 8px 14px 16px; display: flex; flex-direction: column; gap: 12px; }
  .state { padding: 14px; border-radius: 14px; background: var(--s1); border: 1px solid var(--line); display: flex; flex-direction: column; gap: 6px; font-size: 15.5px; line-height: 21px; }
  .lb { display: flex; align-items: center; gap: 8px; font: 600 15px var(--display); letter-spacing: .04em; text-transform: uppercase; color: var(--good); }
  .lb i { width: 9px; height: 9px; border-radius: 50%; background: var(--good); }
  .look .lb { color: var(--wait); }
  /* Il riquadro col link: il tocco ovunque apre la pagina (il ::after di «Apri la pagina» lo copre tutto), il ▶ sta sopra. */
  .state.link { position: relative; color: var(--tx); transition: border-color 160ms ease-out; }
  .state.link .go { touch-action: manipulation; }
  .state.link .go::after { content: ''; position: absolute; inset: 0; border-radius: 14px; }
  .state.link .go:focus-visible { outline: none; }
  .state.link:has(.go:focus-visible) { outline: 2px solid var(--tx); outline-offset: 2px; }
  .vd :global(.play) { position: relative; z-index: 1; }
  .go { display: inline-flex; align-items: center; gap: 4px; align-self: flex-start; margin-top: 2px; font: 600 15px var(--display); color: var(--tx); text-decoration: underline; text-decoration-color: var(--tx3); text-underline-offset: 3px; }
  @media (hover: hover) and (pointer: fine) { .state.link:hover { border-color: var(--tx3); } .state.link:hover .go { text-decoration-color: var(--tx); } }
  .look .lb i { background: var(--wait); }
  .alerts { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-radius: 14px; border: 1px solid var(--s3); color: var(--tx2); font-size: 15.5px; line-height: 21px; }
  .alerts > .al { flex: 1; min-width: 0; }
  .alerts .hit { color: var(--wait); }
  .alerts button { font: 600 15px var(--display); color: var(--tx); }
  .ex { font-size: 14px; line-height: 19px; color: var(--tx3); }
  .ex em { font-style: normal; font-weight: 500; color: var(--wait); padding: 1px 10px; margin-right: 6px; border-radius: 999px; background: color-mix(in srgb, var(--wait) 14%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 50%, transparent); }
  footer { display: flex; gap: 10px; padding: 12px 14px; }
  .btn { flex: 1; min-height: 48px; padding: 8px 14px; border-radius: 999px; background: var(--s3); color: var(--tx); font: 600 16px var(--display); }
  .btn.main { color: var(--wait); background: color-mix(in srgb, var(--wait) 14%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 60%, transparent); }
</style>
