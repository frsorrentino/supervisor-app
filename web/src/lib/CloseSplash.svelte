<script lang="ts">
  import { locale } from './lang'
  import { CLOSED_SHOW_MS, type Phase } from './closeSplash'
  import StepProgress from './StepProgress.svelte'
  import { closeStep } from './animazioni'
  import { t } from './t'
  // Il pannello di chiusura sopra la pagina della sessione (CloseSplash.kt; Franz, 08/10 18:29, variante A): la pagina resta
  // sotto, velata. «Si sta chiudendo» con i passi veri; «è chiusa» con l'ora, «Riapri» e «Torna alla home», e dopo 3 s il
  // ritorno da solo, con la barra che si consuma. In una colonna della plancia toglie la colonna.
  // Rifiutata dal relay (app-chiudi-esito-04-11): il motivo resta finché non lo togli con `onDismiss`, sulla pagina com'era.
  // Contratto 1.120: rifiutata per lavoro, domanda o sottofondo, «Chiudi quando finisce» e «Chiudi comunque» con conferma.
  // Contratto 1.155 (app-chiudi-salva-19-11): «Salva e chiudi» mostra i passi del relay (closing.mode save, poi exit) e, chiusa,
  // se c'è il passaggio di consegne e dove, o perché non c'è.
  // app-chiudi-scheda-20-11 (bozza C, senza tasto bianco): niente velo; la carta sta in fondo, al posto del campo di scrittura,
  // coi tasti della chiusura di v3.css (.ck). Come CloseSplash.kt.
  let { phase, onHome, onDismiss = onHome, onReopen = null, onArm = null, column = false }: { phase: Phase; onHome: () => void; onDismiss?: () => void; onReopen?: (() => void) | null; onArm?: ((when: 'after' | 'force') => void) | null; column?: boolean } = $props()
  let forceAsk = $state(false)
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
  $effect(() => {
    if (phase.kind !== 'closed') return
    const id = setTimeout(onHome, CLOSED_SHOW_MS)
    return () => clearTimeout(id)
  })
</script>

<div class="dock" role="presentation">
  <div class="card" role="status">
    <svg class="pw" class:off={phase.kind === 'closed'} class:no={phase.kind === 'refused'} viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 3v9" /><path d="M6.3 6.8a8 8 0 1 0 11.4 0" /></svg>
    {#if phase.kind === 'closing'}
      <h3>{t.splashClosing(phase.name)}</h3>
      {#if phase.save}
        <p>{t.splashSaveSentAt(hm(phase.sentAt))}</p>
        <!-- app-chiusura-step-progress-27-11 (Franz 27/11 12:00): i passi della chiusura sono lo Step progress dell'originale, coi nomi
             sotto i pallini; «Chiusa» raggiunta resta attuale finché il PC non conferma. -->
        <StepProgress names={[t.splashStepSave, t.splashStepExit, t.splashStepClosed]} at={closeStep(true, phase.step, phase.delivered)} />
      {:else}
        <p>{t.splashSentAt(hm(phase.sentAt))}</p>
        <StepProgress names={[t.splashStepSent, t.splashStepClosed]} at={closeStep(false, null, phase.delivered)} />
      {/if}
      <div class="bar"><b class="run"></b></div>
      {#if phase.slow}
        <small class="slow">{t.splashSlow}</small>
        <div class="ck-keys"><button class="ck" onclick={onHome}>{column ? t.splashDrop : t.splashHome}</button></div>
      {:else}<small>{phase.save ? t.splashSaveUsually : phase.force ? t.splashForceUsually : t.splashUsually}</small>{/if}
    {:else if phase.kind === 'refused' && forceAsk && onArm}
      <h3>{t.closeForceAsk(phase.name)}</h3>
      <p>{t.closeForceWhat}</p>
      <div class="ck-keys"><div class="ck-pair"><button class="ck" onclick={() => (forceAsk = false)}>{t.closeForceCancel}</button><button class="ck red" onclick={() => { forceAsk = false; onArm?.('force') }}>{t.closeForce}</button></div></div>
    {:else if phase.kind === 'refused'}
      <h3>{t.splashRefused(phase.name)}</h3>
      <p>{phase.why}</p>
      {#if phase.armable && onArm}
        <div class="ck-keys">
          <button class="ck main" onclick={() => onArm?.('after')}>{t.closeAfter}</button>
          <div class="ck-pair"><button class="ck" onclick={onDismiss}>{t.closeKeep}</button><button class="ck red" onclick={() => (forceAsk = true)}>{t.closeForce}</button></div>
        </div>
      {:else}
        <div class="ck-keys"><button class="ck main" onclick={onDismiss}>{t.splashRefusedOk}</button></div>
      {/if}
    {:else}
      <h3>{t.splashClosed(phase.name)}</h3>
      <p>{phase.byMe ? t.splashClosedLine(hm(phase.at)) : t.splashClosedOther(hm(phase.at))}</p>
      {#if phase.handoff}
        <p class="ho ok">{t.splashHandoff}</p>
        {#if phase.file}<code class="file" title={phase.file}><bdi>{phase.file}</bdi></code>{/if}
      {:else if phase.reason}<p class="ho no">{t.splashExpired(phase.reason)}</p>
      {:else if phase.save}<p class="ho">{t.splashNoHandoff}</p>{/if}
      {#key phase}<div class="bar"><b class="left" style:animation-duration="{CLOSED_SHOW_MS}ms"></b></div>{/key}
      <small>{column ? t.splashDropSoon : t.splashBackSoon}</small>
      <div class="ck-keys">
        {#if onReopen}<div class="ck-pair"><button class="ck" onclick={onReopen}>{t.splashReopen}</button><button class="ck main" onclick={onHome}>{column ? t.splashDrop : t.splashHome}</button></div>
        {:else}<button class="ck main" onclick={onHome}>{column ? t.splashDrop : t.splashHome}</button>{/if}
      </div>
    {/if}
  </div>
</div>

<style>
  .dock { position: absolute; inset: auto 0 0; z-index: 15; display: flex; justify-content: center; padding: 0 12px max(12px, env(safe-area-inset-bottom)); pointer-events: none; }
  .card { pointer-events: auto; width: min(560px, 100%); background: var(--s2); border: 1px solid var(--line); border-radius: 20px; padding: 18px 18px 14px; display: flex; flex-direction: column; align-items: flex-start; gap: 6px;
    box-shadow: 0 -10px 28px rgb(0 0 0 / .55); animation: up 250ms cubic-bezier(.23, 1, .32, 1); }
  .pw { color: var(--busy); }
  .pw.off { color: var(--text2); }
  .pw.no { color: var(--wait); }
  h3 { margin: 0; font: 600 21px/1.2 var(--display); }
  p { margin: 0; color: var(--text2); font-size: 14px; }
  .ho { color: var(--text); }
  .ho.ok { color: var(--good); }
  .ho.no { color: var(--wait); }
  .file { align-self: stretch; font: 12.5px var(--mono); color: var(--text2); white-space: nowrap; overflow: hidden; direction: rtl; text-align: left; mask-image: linear-gradient(to right, transparent, #000 12%); }
  small { font-size: 12.5px; color: var(--text2); }
  small.slow { color: var(--wait); }
  .bar { align-self: stretch; height: 4px; border-radius: 2px; background: var(--high); overflow: hidden; margin-top: 8px; }
  .bar b { display: block; height: 100%; background: var(--icon); }
  .bar .run { width: 40%; animation: run 1.2s ease-in-out infinite; }
  .bar .left { width: 100%; transform-origin: left; animation: left linear forwards; }
  @keyframes run { from { translate: -100% 0; } to { translate: 250% 0; } }
  @keyframes left { to { scale: 0 1; } }
  @keyframes up { from { opacity: 0; translate: 0 16px; } }
  @media (prefers-reduced-motion: reduce) { .card, .bar .run { animation: none; } }
</style>
