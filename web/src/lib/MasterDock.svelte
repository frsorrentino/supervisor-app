<script lang="ts">
  import { locale } from './lang'
  import type { Snippet } from 'svelte'
  import type { Session } from './contract'
  import { hero as heroOf, type Entry } from './masterHome'
  import { t } from './t'
  import StateGlyph from './StateGlyph.svelte'
  import { glyphOf } from './rail'
  import { personal } from './badge'
  // La master nella home (MasterDock.kt): ridotta è la barra agganciata in fondo, «MASTER · ora · modello · contesto» e il
  // titolo dell'ultimo esito, ▶ per ascoltarlo e ▲ per espanderla; espansa la stessa barra sta in cima con ▼ e la riduce.
  // `screen`: la master a tutto schermo sul telefono (Franz, 08/10 18:15), una testata sola: ⌄ a sinistra, angoli dritti, a
  // destra il menu ≡ della home (`trailing`). `toggleUp` e `onToggleKey`: il tasto tondo nelle tre misure della plancia,
  // dove a metà dice ▲ e porta a tutto schermo. `onClose`: a metà, «Chiudi» ▼ accanto ad «Apri» ▲ (Franz, 09/10 21:02):
  // chiudere non chiede più il trascinamento, ogni gesto ha un tocco che fa lo stesso (WCAG 2.5.1).
  // `onDrag`: il gesto passo per passo (`/embed?dock=1`, dove Studio fa seguire al riquadro il dito), con lo spostamento in
  // px dall'inizio, e `null` alla fine; allora decide `onDrop` invece di `onToggle`.
  let { master, entries, onToggle, onSpeak, expanded = false, screen = false, side = false, trailing, toggleUp, onToggleKey, onClose, onDrag, onDrop, bar = false, mirror = false }: {
    master: Session; entries: Entry[]; onToggle: () => void; onSpeak: (text: string) => void; expanded?: boolean
    /** Il pannello a destra della plancia (pwa2, 30/10): il tasto tondo allarga o stringe (frecce di lato), Chiudi è una ×. */
    side?: boolean
    /** La barra in fondo alla plancia (pwa2, 30/10): solo stato, nome ed esito; il tasto che apre sta dopo il campo. */
    bar?: boolean
    /** Il pannello sta a sinistra: le frecce di lato si invertono, allargare resta verso le colonne. */
    mirror?: boolean
    screen?: boolean; trailing?: Snippet; toggleUp?: boolean; onToggleKey?: () => void; onClose?: () => void
    onDrag?: (dy: number) => void; onDrop?: (dy: number) => void
  } = $props()
  const up = $derived(toggleUp ?? !expanded)
  const key = $derived(onToggleKey ?? onToggle)
  const h = $derived(heroOf(entries, master))
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
  // Aperta, il modello e il contesto stanno già nella riga sotto la linguetta: qui resta l'ora. Chiusa, l'ora e il contesto,
  // che conta per l'handoff: il modello cambia di rado, e con lui la riga non stava nella larghezza (Franz, 06/10 22:27).
  const label = $derived((expanded ? [t.dockMaster, h?.at ? hm(h.at) : null] : [t.dockMaster, h?.at ? hm(h.at) : null, master.context != null ? t.ctx(master.context) : null]).filter(Boolean).join(' · '))
  // Il badge di stato solo quando la master ti aspetta o è chiusa; altrimenti la scintilla di Claude, che gira mentre lavora.
  // Il cerchio rosso era l'elemento più saturo della schermata e nel Material 3 il rosso è il ruolo «errore» (22:27).
  const attention = $derived(master.state === 'waiting' || master.state === 'gone')
  // Stile «Studio e Diretta» (30/10): al posto della scintilla e del badge il glifo dello stato, con l'anello del contesto.
  const when = $derived(h?.at ? hm(h.at) : null)
  // Trascinare la barra oltre 40 px (MasterDock.kt): in su espande, in giù riduce; il tocco resta, il gesto lo affianca.
  // screenY e non clientY: dentro Studio il riquadro si sposta col dito, e le coordinate della pagina con lui.
  let y0: number | null = null
  let dragged = false
  function down(e: PointerEvent) { y0 = e.screenY; dragged = false; if (onDrag) (e.currentTarget as Element).setPointerCapture?.(e.pointerId) }
  function move(e: PointerEvent) {
    if (y0 == null || !onDrag) return
    const dy = e.screenY - y0
    if (Math.abs(dy) > 8) { dragged = true; onDrag(dy) }
  }
  function release(e: PointerEvent) {
    if (y0 == null) return
    const dy = e.screenY - y0
    y0 = null
    if (onDrop) { if (dragged) onDrop(dy); return }
    if ((dy < -40 && !expanded) || (dy > 40 && expanded)) { dragged = true; onToggle() }
  }
</script>

{#snippet toggleKey()}
  <button class="ib" aria-label={up ? t.dockConversation : t.dockCollapse} title={up ? t.dockConversation : t.dockCollapse} onclick={(e) => { e.stopPropagation(); key() }}>
    <svg viewBox="0 0 24 24" width="22" height="22"><path d={side ? (up !== mirror ? 'M14 7l-5 5 5 5' : 'M10 7l5 5-5 5') : up ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  </button>
{/snippet}

<div class="dock" class:expanded class:screen role="button" tabindex="0" aria-expanded={expanded} onclick={() => { if (dragged) { dragged = false; return } onToggle() }} onkeydown={(e) => e.key === 'Enter' && onToggle()}
  onpointerdown={down} onpointermove={move} onpointerup={release} onpointercancel={() => { if (y0 != null && dragged) onDrop?.(0); y0 = null }}>
  <!-- La freccia sempre a sinistra, aperta o chiusa (Franz, 08/10 20:42). -->
  {#if !bar}{@render toggleKey()}{/if}
  {#if onClose}
    <button class="ib" aria-label={t.dockClose} title={t.dockClose} onclick={(e) => { e.stopPropagation(); onClose() }}>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d={side ? 'M7 7l10 10M17 7L7 17' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
    </button>
  {/if}
  <StateGlyph kind={glyphOf(master)} ctx={master.context} size={18} square={!personal(master.account, master.account_kind)} label={attention ? master.state : label} />
  <span class="col">
    {#if expanded}
      <span class="l1"><b class="mn">{t.summaryTitle}</b>{#if when}<span class="mono">{when}</span>{/if}</span>
      {#if h}<span class="hl">{h.headline.replace(/\*\*|__|`/g, '')}</span>{/if}
    {:else}
      <span class="l1"><b class="mn">{t.summaryTitle}</b>{#if h}<span class="hl">{h.headline.replace(/\*\*|__|`/g, '')}</span>{/if}{#if when}<span class="mono">{when}</span>{/if}</span>
    {/if}
  </span>
  {#if h && !bar}
    <button class="ib" aria-label={t.dockListen} onclick={(e) => { e.stopPropagation(); onSpeak([h.headline, h.body].filter(Boolean).join('\n')) }}>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>
    </button>
  {/if}
  {@render trailing?.()}
</div>

<style>
  /* La master è la superficie più alta dell'app, velata del celeste d'accento, piatta come il resto (Celeste velato, Franz
     06/10 21:23). Aperta resta una linguetta col verso di quando è chiusa, angoli tondi in alto (08:48). */
  .dock { touch-action: none; display: flex; align-items: center; gap: 12px; padding: 10px 12px 8px 8px; border-radius: 26px 26px 0 0; cursor: pointer; outline: none; background: var(--master-highest); }
  /* Il filtro dell'hover farebbe da contenitore al pannello del menu ≡, che è fisso: a tutto schermo niente filtro. */
  .dock.screen { border-radius: 0; padding: 10px 4px 8px 8px; }
  .dock.screen:hover, .dock.screen:focus-visible { filter: none; }
  .dock:hover, .dock:focus-visible { filter: brightness(1.08); }
  @media (prefers-reduced-motion: reduce) { .ib { transition: none; } }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .col > * { white-space: nowrap; overflow: hidden; }
  b { font-size: 16px; font-weight: 600; }
  /* Premuti, i tasti cambiano forma come quelli del Material 3 Expressive: da cerchio a quadrato smussato, con la molla. */
  .ib { flex: none; width: 44px; height: 44px; border-radius: 22px; display: grid; place-items: center; background: var(--master-key); color: var(--text);
    transition: border-radius .35s var(--spring); position: relative; }
  /* Il tocco prende 48 px, il tasto se ne vede 44 (Franz, 09/10 21:02). */
  .ib::after { content: ''; position: absolute; inset: -2px; }
  .ib:active { border-radius: 12px; }
  .ib:hover { filter: brightness(1.15); }
  /* Nella home in stile «Studio e Diretta» (30/10): la barra sta sul fondo della home, senza velo; il nome in Barlow Semi
     Condensed, l'ultimo esito in grigio, l'ora in mono; i tasti tondi tonali. */
  :global(.v3) .dock { background: transparent; gap: 10px; }
  :global(.v3) .dock:hover, :global(.v3) .dock:focus-visible { filter: none; }
  .l1 { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
  .mn { font: 700 15px var(--display, inherit); flex: none; }
  .hl { flex: 1; min-width: 0; font-size: 14px; color: var(--text2); white-space: nowrap; overflow: hidden; }
  .l1 .mono { flex: none; font: 400 12.5px var(--mono); color: var(--text2); letter-spacing: 0; }
  .col > .hl { display: block; }
</style>
