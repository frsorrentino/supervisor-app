<script lang="ts">
  import type { Alert } from './elsewhere'
  import { PATHS } from './badge'
  import { t } from './t'
  // L'avviso delle altre sessioni sotto la barra della chat (ElsewherePill dell'app): una riga sola, la mano ambra per chi
  // ti aspetta, la bandierina verde per un turno finito, con ✕. Il clic apre la sessione, o la coda delle domande quando
  // sono più d'una. Franz 23/10: anche chi ti aspetta si chiude, con la ✕ o trascinando di lato; torna con una richiesta nuova.
  let { alert, onOpen, onDismiss }: { alert: Alert; onOpen: () => void; onDismiss: () => void } = $props()
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

<div class="pill" style="--t:{tone}; translate: {dx}px 0; opacity: {1 - Math.min(1, Math.abs(dx) / (SWIPE_PX * 2))}" role="button" tabindex="0" onclick={open} onkeydown={(e) => e.key === 'Enter' && onOpen()}
  onpointerdown={down} onpointermove={move} onpointerup={up} onpointercancel={up}>
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="var(--t)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{#each waiting ? PATHS.hand : FLAG as d}<path {d} />{/each}</svg>
  <span class="text">{text}</span>
  <span class="act">{waiting ? t.elsewhereReply : t.elsewhereOpen}</span>
  {#if waiting}<svg viewBox="0 0 24 24" width="20" height="20"><path d="M10 7l5 5-5 5" fill="none" stroke="var(--t)" stroke-width="2" stroke-linecap="round" /></svg>{/if}
  <button class="x" aria-label={t.closeWord} onclick={(e) => { e.stopPropagation(); onDismiss() }} onpointerdown={(e) => e.stopPropagation()}><svg viewBox="0 0 24 24" width="18" height="18"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
</div>

<style>
  .pill { display: flex; align-items: center; gap: 10px; margin: 8px 12px; min-height: 44px; padding: 0 4px 0 14px; border-radius: 22px; cursor: pointer; outline: none; touch-action: pan-y;
    background: color-mix(in srgb, var(--t) 16%, transparent); animation: in .25s cubic-bezier(.2, .8, .2, 1); }
  .pill:hover, .pill:focus-visible { background: color-mix(in srgb, var(--t) 22%, transparent); }
  svg { flex: none; }
  .text { flex: 1; min-width: 0; font-size: 16px; font-weight: 500; white-space: nowrap; overflow: hidden; }
  .act { color: var(--t); font-size: 14px; font-weight: 500; }
  .x { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; }
  .x:hover { background: rgb(255 255 255 / .06); }
  @keyframes in { from { opacity: 0; translate: 0 -6px; } }
  @media (prefers-reduced-motion: reduce) { .pill { animation: none; } }
</style>
