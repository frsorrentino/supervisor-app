<script lang="ts">
  import type { Alert, Away } from './elsewhere'
  import { age } from './summary'
  import { PATHS } from './badge'
  import { t } from './t'
  // L'avviso delle altre sessioni sotto la barra della chat (ElsewherePill dell'app): una riga sola, la mano ambra per chi
  // ti aspetta, la bandierina verde per un turno finito, con ✕. Il clic apre la sessione, o la coda delle domande quando
  // sono più d'una. Franz 23/10: anche chi ti aspetta si chiude, con la ✕ o trascinando di lato; torna con una richiesta nuova.
  // Dentro Studio (`/embed`, Franz 24/10 21:38) `away` c'è: la scheda dice la richiesta e «Apri ↗» porta alla web app.
  let { alert, onOpen, onDismiss, away = null, now = 0 }: { alert: Alert; onOpen: () => void; onDismiss: () => void; away?: Away | null; now?: number } = $props()
  const waiting = $derived(alert.type === 'waiting')
  /** Oltre questo trascinamento la pastiglia è chiusa. */
  const SWIPE_PX = 80
  let dx = $state(0)
  let startX: number | null = null
  let swiped = false
  function down(e: PointerEvent) { startX = e.clientX; swiped = false; (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId) }
  function move(e: PointerEvent) { if (startX != null) dx = e.clientX - startX }
  function up() {
    if (startX == null) return
    swiped = Math.abs(dx) > SWIPE_PX
    startX = null; dx = 0
    if (swiped) onDismiss()
  }
  function open() { if (!swiped) onOpen() }
  const tone = $derived(waiting ? 'var(--b-warn)' : 'var(--b-good)')
  const text = $derived(alert.type === 'waiting' ? (alert.sessions.length === 1 ? t.elsewhereWaiting(alert.sessions[0]) : t.elsewhereWaitingMany(alert.sessions.length)) : t.elsewhereFinished(alert.session))
  const FLAG = ['M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z', 'M4 22v-7']
</script>

{#if away}
<div class="away v3" style="--t:{tone}">
  <div class="top">
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="var(--t)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{#each PATHS.hand as d}<path {d} />{/each}</svg>
    <b>{text}</b>
    {#if away.at && now}<span class="m">{age(away.at, now)}</span>{/if}
    <button class="x" aria-label={t.closeWord} onclick={onDismiss}><svg viewBox="0 0 24 24" width="18" height="18"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
  </div>
  {#if away.text}<p class="req">{away.text}</p>{/if}
  <div class="row">
    <small>{away.more ? t.awayMore(away.more) : t.awayWhere}</small>
    <a class="open" href={away.href} target="_blank" rel="noopener">{t.awayOpen}<svg viewBox="0 0 24 24" width="16" height="16"><path d="M7 17 17 7M8 7h9v9" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></a>
  </div>
</div>
{:else}
<div class="pill v3" style="--t:{tone}; translate: {dx}px 0; opacity: {1 - Math.min(1, Math.abs(dx) / (SWIPE_PX * 2))}" role="button" tabindex="0" onclick={open} onkeydown={(e) => e.key === 'Enter' && onOpen()}
  onpointerdown={down} onpointermove={move} onpointerup={up} onpointercancel={up}>
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="var(--t)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{#each waiting ? PATHS.hand : FLAG as d}<path {d} />{/each}</svg>
  <span class="text">{text}</span>
  <span class="act">{waiting ? t.elsewhereReply : t.elsewhereOpen}</span>
  {#if waiting}<svg viewBox="0 0 24 24" width="20" height="20"><path d="M10 7l5 5-5 5" fill="none" stroke="var(--tx2)" stroke-width="2" stroke-linecap="round" /></svg>{/if}
  <button class="x" aria-label={t.closeWord} onclick={(e) => { e.stopPropagation(); onDismiss() }} onpointerdown={(e) => e.stopPropagation()}><svg viewBox="0 0 24 24" width="18" height="18"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
</div>
{/if}

<style>
  .pill { display: flex; align-items: center; gap: 10px; margin: 8px 12px; min-height: 44px; padding: 0 4px 0 14px; border-radius: 14px; cursor: pointer; outline: none; touch-action: pan-y;
    background: var(--s2); animation: in .25s cubic-bezier(.2, .8, .2, 1); }
  .pill:hover, .pill:focus-visible { background: var(--s3); }
  svg { flex: none; }
  .text { flex: 1; min-width: 0; font-size: 16px; font-weight: 500; white-space: nowrap; overflow: hidden; }
  .act { color: var(--tx); font: 600 14px var(--display); }
  .x { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; }
  .x:hover { background: rgb(255 255 255 / .06); }
  .away { margin: 8px 12px; padding: 12px 6px 12px 14px; border-radius: 14px; display: flex; flex-direction: column; gap: 6px;
    background: var(--s2); animation: in .25s cubic-bezier(.2, .8, .2, 1); }
  .away .top { display: flex; align-items: center; gap: 10px; }
  .away b { flex: 1; min-width: 0; font-size: 15.5px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .away .m { font: 12px var(--mono); color: var(--text2); white-space: nowrap; }
  .req { font-size: 14.5px; line-height: 1.4; padding-right: 8px; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; text-overflow: clip; }
  .away .row { display: flex; align-items: center; gap: 8px; padding-right: 8px; }
  .away small { flex: 1; min-width: 0; color: var(--text2); font-size: 12.5px; }
  .open { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 999px; background: var(--high); color: var(--text); font-weight: 500; font-size: 14px; text-decoration: none; white-space: nowrap; }
  .open:hover { filter: brightness(1.2); }
  @keyframes in { from { opacity: 0; translate: 0 -6px; } }
  @media (prefers-reduced-motion: reduce) { .pill, .away { animation: none; } }
</style>
