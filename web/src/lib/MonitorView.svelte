<script lang="ts">
  import MonitorChart from './MonitorChart.svelte'
  import { isLook, type Monitor } from './monitors'
  import { t } from './t'
  // La scheda di un monitoraggio (app-monitoraggi-09-11, mockup B e C), come MonitorPage in Kotlin: il titolo, la fonte e l'ora
  // dell'ultimo valore; il riquadro con lo stato e la frase; i grafici; gli avvisi; la nota «Esempio»; Ferma e Chiedi alla master,
  // un solo bottone pieno. `wide`: nella colonna della plancia, con la × al posto della freccia.
  let { m, wide = false, onBack, onStop, onAsk, onEditAlerts }: {
    m: Monitor; wide?: boolean; onBack: () => void; onStop: () => void; onAsk: () => void; onEditAlerts: () => void
  } = $props()
  const dm = (at: number) => {
    const d = new Date(at * 1000), p = (n: number) => String(n).padStart(2, '0')
    return `${p(d.getDate())}/${p(d.getMonth() + 1)} ${p(d.getHours())}:${p(d.getMinutes())}`
  }
</script>

<section class="mv" class:wide data-testid="monitor-scheda-{m.id}">
  <header>
    {#if !wide}<button class="ic" aria-label={t.back} onclick={onBack}><svg viewBox="0 0 24 24" width="24" height="24"><path d="M15 5l-7 7 7 7" fill="none" stroke="var(--tx)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>{/if}
    <span class="tt"><b>{m.name}</b><small>{[m.source, m.at ? t.monitorLast(dm(m.at)) : null].filter(Boolean).join(' · ')}</small></span>
    {#if wide}<button class="ic" aria-label={t.closeWord} onclick={onBack}><svg viewBox="0 0 24 24" width="20" height="20"><path d="M6 6l12 12M18 6L6 18" fill="none" stroke="var(--tx2)" stroke-width="2" stroke-linecap="round" /></svg></button>{/if}
  </header>
  <div class="body">
    <div class="state" class:look={isLook(m)}><p class="lb"><i></i>{isLook(m) ? t.monitorStateLook : t.monitorStateOk}</p><p>{m.summary ?? m.text}</p></div>
    {#each m.charts ?? [] as c, i (i)}<MonitorChart {c} />{/each}
    {#if m.alerts}<p class="alerts"><span>{t.monitorAlerts} · {m.alerts}</span><button data-testid="monitor-avvisi" onclick={onEditAlerts}>{t.monitorEdit}</button></p>{/if}
    {#if m.example}<p class="ex"><em>{t.monitorExample}</em> {m.example}</p>{/if}
    {#each m.notes ?? [] as n}<p class="note">{n}</p>{/each}
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
  .look .lb i { background: var(--wait); }
  .alerts { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-radius: 14px; border: 1px solid var(--s3); color: var(--tx2); font-size: 15.5px; line-height: 21px; }
  .alerts span { flex: 1; }
  .alerts button { font: 600 15px var(--display); color: var(--tx); }
  .ex { font-size: 14px; line-height: 19px; color: var(--tx3); }
  .ex em { font-style: normal; font-weight: 500; color: var(--wait); padding: 1px 10px; margin-right: 6px; border-radius: 999px; background: color-mix(in srgb, var(--wait) 14%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 50%, transparent); }
  .note { font-size: 14px; line-height: 19px; color: var(--tx3); }
  footer { display: flex; gap: 10px; padding: 12px 14px; }
  .btn { flex: 1; min-height: 48px; padding: 8px 14px; border-radius: 999px; background: var(--s3); color: var(--tx); font: 600 16px var(--display); }
  .btn.main { color: var(--wait); background: color-mix(in srgb, var(--wait) 14%, transparent); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 60%, transparent); }
</style>
