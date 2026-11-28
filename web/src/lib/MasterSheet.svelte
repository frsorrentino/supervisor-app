<script lang="ts">
  import type { Snippet } from 'svelte'
  import { untrack } from 'svelte'
  import { locale } from './lang'
  import type { Session } from './contract'
  import { hero as heroOf, type Entry } from './masterHome'
  import { anchors, openness, ROW_PX, rubber, settleAt, snap, STIFFNESS, tap, toRow, up, type Anchors, type Stop } from './masterSheet'
  import { DRAG_PX, landSpring, velocity } from './gesture'
  import { ridotto } from './molla'
  import { OPENING_MS } from './arrivo'
  import { DOCK_PEEK_MS } from './animazioni'
  import RollTime from './RollTime.svelte'
  import StateGlyph from './StateGlyph.svelte'
  import { glyphOf } from './rail'
  import { personal } from './badge'
  import { speech } from './speech.svelte'
  import { heads } from './headMore.svelte'
  import { t } from './t'

  // app-posizioni-sistema-27-11 gruppo 4 (B «Foglio»): la master è un foglio che sale dal basso e prende il posto della sua barra di prima,
  // in tre misure (masterSheet.ts): riga, cioè maniglia, la riga della master col suo stato e ▶, e il campo; metà; intera. Dal gruppo 6 è
  // anche la master dentro Studio (DockEmbed, a tutta larghezza).
  // Sta sopra la pagina e non la sposta mai: chi lo contiene gli dà il posto (`position: relative`) e lascia sotto la riga lo spazio
  // `rowH`. Trascinato dalla maniglia o dalla riga segue il dito, con l'elastico oltre la riga e l'intera; al rilascio scatta alla misura
  // più vicina al punto proiettato, con la molla senza rimbalzo che parte dalla velocità del dito (anche con le animazioni ridotte:
  // smorzamento critico, niente oltre la misura). Il tocco sulla riga passa alla misura dopo; Esc, e ↓ a campo vuoto o fuori dal campo,
  // tornano alla riga. Dentro, la conversazione della master com'è (`chat`): alla riga se ne vede solo il fondo, il campo; il resto sta
  // sopra, tagliato dal bordo, e compare mentre il foglio sale.
  let { master, entries, stop, onStop, onSpeak, chat, inset = false, rowH = $bindable(ROW_PX), screen = false, onHeight = null }: {
    master: Session; entries: Entry[]; stop: Stop; onStop: (s: Stop) => void; onSpeak: (text: string) => void
    /** La conversazione della master (Chat), o il posto dove arriva quella spostata (la web larga ne ha una sola). */
    chat: Snippet
    /** Sulla colonna della web larga il foglio sta a 8 px dai bordi, come nel mockup. */
    inset?: boolean
    /** L'altezza della riga, misurata: chi contiene il foglio la lascia libera sotto la pagina. */
    rowH?: number
    /**
     * app-posizioni-sistema-27-11 gruppo 6, dentro Studio (`/embed?dock=1`): il riquadro cresce e cala col dito, quindi il gesto si misura
     * sullo schermo (screenY), come la barra di prima; nella pagina la posizione del dito si sposterebbe col riquadro.
     */
    screen?: boolean
    /** L'altezza di adesso a ogni disegno, con la misura dove sta ('move' mentre il dito o la molla lo portano): DockEmbed la dice a Studio. */
    onHeight?: ((h: number, at: Stop | 'move') => void) | null
  } = $props()

  let el = $state<HTMLElement>()
  let grab = $state<HTMLElement>()
  let body = $state<HTMLElement>()
  // La misura a cui il foglio va o sta; `moving` mentre il dito lo tiene o la molla lo porta.
  let shown = $state<Stop>(untrack(() => stop))
  let moving = $state(false)
  // Fuori dalla reattività, scritti a ogni fotogramma: l'altezza di adesso, il posto, le misure.
  let h = 0
  let avail = 0
  let a: Anchors = anchors(0, ROW_PX)
  let ready = false
  let raf = 0
  let anim: { from: number; v: number; w: number; t0: number } | null = null

  function paint() {
    if (!el) return
    el.style.height = `${Math.max(0, h).toFixed(1)}px`
    el.style.setProperty('--open', openness(a, h).toFixed(3))
    onHeight?.(Math.max(0, h), moving ? 'move' : shown)
  }
  // La posizione del dito: nella pagina, o sullo schermo dentro Studio (`screen`).
  const yOf = (e: PointerEvent) => (screen ? e.screenY : e.clientY)

  // La riga è la maniglia con la riga della master più il fondo della conversazione (il campo, o la barra dei miei messaggi, con le
  // ricorrenti aperte sopra); `--chat-min` è la conversazione senza la lista: sotto quella misura se ne vede solo il fondo.
  function measure() {
    if (!el?.isConnected || !el.parentElement) return
    avail = el.parentElement.clientHeight
    const sec = body?.querySelector<HTMLElement>('section.chat')
    let block = 0, min = 0, jumping = false
    if (sec) for (const c of sec.children) {
      if (!(c instanceof HTMLElement)) continue
      const cs = getComputedStyle(c)
      if (cs.display === 'none' || cs.position === 'absolute' || cs.position === 'fixed') continue
      const outer = c.offsetHeight + (parseFloat(cs.marginTop) || 0) + (parseFloat(cs.marginBottom) || 0)
      if (!c.classList.contains('stage')) min += outer
      if (c.matches('.bar, .mjbar, .blk.box')) block += outer
      if (c.classList.contains('mjbar')) jumping = true
    }
    const g = grab?.offsetHeight ?? 0
    const row = g + (block || ROW_PX - g)
    if (Math.abs(row - rowH) > 0.5) rowH = row
    body?.style.setProperty('--chat-min', `${Math.round(min)}px`)
    a = anchors(avail, row)
    if (!ready && avail > 0) { ready = true; h = a[shown] }
    if (!drag && !anim) h = a[shown]
    paint()
    // I miei messaggi si scorrono nella conversazione: aperti dalla riga, il foglio sale a metà.
    if (jumping && shown === 'row' && !drag) go('half')
  }

  // La molla d'arrivo senza rimbalzo (gesture.landSpring, MasterGestures.land), calcolata e non integrata: segue la misura anche se
  // questa cambia a metà corsa (la finestra, la tastiera, il campo che va a capo).
  function animate(v: number) {
    cancelAnimationFrame(raf)
    const from = h - a[shown]
    if (!ready || (Math.abs(from) < 0.5 && Math.abs(v) < 1)) { anim = null; h = a[shown]; moving = false; paint(); return }
    const s = landSpring(from, v, STIFFNESS)
    anim = { from, v: s.v, w: s.w, t0: performance.now() }
    moving = true
    raf = requestAnimationFrame(frame)
  }
  function frame(now: number) {
    if (!anim) return
    const s = settleAt(anim.from, anim.v, anim.w, Math.max(0, now - anim.t0) / 1000)
    h = a[shown] + s.x
    paint()
    if (Math.abs(s.x) < 0.5 && Math.abs(s.v) < 8) { anim = null; h = a[shown]; moving = false; paint(); return }
    raf = requestAnimationFrame(frame)
  }
  function go(to: Stop, v = 0) {
    shown = to
    if (to !== stop) onStop(to)
    animate(v)
  }
  // Una misura chiesta da fuori (aprire la master da una scheda, Indietro, la preferenza): ci va con la molla, da fermo.
  $effect(() => { const s = stop; untrack(() => { if (s !== shown) { shown = s; animate(0) } }) })

  // Il dito: oltre DRAG_PX è un trascinamento, 1:1 fra la riga e l'intera e con l'elastico oltre; fermo, al rilascio è un tocco.
  let drag: { id: number; y0: number; h0: number; moved: boolean; speed: ReturnType<typeof velocity> } | null = null
  function down(e: PointerEvent) {
    if (!e.isPrimary || e.button !== 0) return
    // Il ▶ e il ⋮ col suo menu tengono i loro tocchi.
    if ((e.target as Element | null)?.closest?.('button:not(.gr), .more')) return
    cancelAnimationFrame(raf); anim = null
    drag = { id: e.pointerId, y0: yOf(e), h0: h, moved: false, speed: velocity() }
    drag.speed.add(yOf(e), e.timeStamp)
  }
  function move(e: PointerEvent) {
    if (!drag || e.pointerId !== drag.id) return
    drag.speed.add(yOf(e), e.timeStamp)
    const dy = yOf(e) - drag.y0
    if (!drag.moved) {
      if (Math.abs(dy) <= DRAG_PX) return
      drag.moved = true; moving = true
      try { grab?.setPointerCapture(e.pointerId) } catch { /* il puntatore è già andato */ }
      document.body.style.userSelect = 'none'
    }
    h = rubber(drag.h0 - dy, a.row, a.full, avail)
    paint()
  }
  function release(e: PointerEvent, cancelled = false) {
    if (!drag || e.pointerId !== drag.id) return
    const d = drag
    drag = null
    document.body.style.userSelect = ''
    if (!d.moved) { if (!cancelled) go(tap(shown)); else animate(0); return }
    if (cancelled) { go(shown); return }
    d.speed.add(yOf(e), e.timeStamp)
    const v = -d.speed.speed()
    go(snap(a, h, v), v)
  }
  // Dalla tastiera: Invio e spazio sulla maniglia come il tocco, ↑ una misura più su; Esc, e ↓ a campo vuoto o fuori dal campo, alla riga.
  function keys(e: KeyboardEvent) {
    if (e.defaultPrevented || e.isComposing) return
    const tgt = e.target as HTMLElement
    // Dentro il menu del ⋮ Esc chiude il menu, non il foglio.
    if (tgt.closest?.('.more')) return
    const onGrip = !!grab?.contains(tgt)
    const field = tgt instanceof HTMLTextAreaElement ? tgt : null
    if (onGrip && e.key === 'ArrowUp') { e.preventDefault(); go(up(shown)); return }
    // ↓ nella lista la fa scorrere: vale solo sulla riga del foglio e nel campo.
    if (e.key === 'ArrowDown' && !onGrip && !field) return
    if (shown !== 'row' && toRow(e.key, !!field, !field?.value.trim())) { e.preventDefault(); go('row') }
  }
  // Col fuoco fuori da tutto (dopo un tocco sulla pagina) Esc e ↓ valgono lo stesso, per il foglio che si vede.
  function windowKeys(e: KeyboardEvent) {
    if (e.defaultPrevented || !el?.isConnected || shown === 'row') return
    const f = document.activeElement
    if (f && f !== document.body) return
    if (toRow(e.key, false, true)) { e.preventDefault(); go('row') }
  }

  $effect(() => {
    if (!el || !body || !grab) return
    const ro = new ResizeObserver(() => measure())
    const watched = new Set<Element>()
    const watch = () => {
      for (const x of watched) ro.unobserve(x)
      watched.clear()
      for (const x of [el!.parentElement, grab!, ...(body!.querySelector('section.chat')?.children ?? [])]) {
        if (!x || x.classList?.contains('stage')) continue
        ro.observe(x); watched.add(x)
      }
      measure()
    }
    // La conversazione arriva dopo (la web larga la sposta qui) e cambia i suoi pezzi (le ricorrenti, la barra dei miei messaggi):
    // si guarda solo chi entra ed esce attorno a lei, non i messaggi.
    let queued = 0
    const mo = new MutationObserver((list) => {
      if (queued || !list.some(m => m.target === body || (m.target as Element).parentElement === body || (m.target as Element).matches?.('section.chat'))) return
      queued = requestAnimationFrame(() => { queued = 0; watch() })
    })
    mo.observe(body, { childList: true, subtree: true })
    // Fuori dalla reattività: la misura legge `shown`, e rifare questo effetto a ogni cambio di misura fermerebbe la molla.
    untrack(watch)
    return () => { ro.disconnect(); mo.disconnect(); cancelAnimationFrame(queued); cancelAnimationFrame(raf) }
  })

  // La riga della master: il glifo dello stato, il nome, l'ora dell'ultima risposta che scorre e il suo titolo, ▶ per ascoltarla.
  const hh = $derived(heroOf(entries, master))
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
  const when = $derived(hh?.at ? hm(hh.at) : null)
  const attention = $derived(master.state === 'waiting' || master.state === 'gone')
  const label = $derived([t.dockMaster, when, master.context != null ? t.ctx(master.context) : null].filter(Boolean).join(' · '))
  const said = $derived(hh ? [hh.headline, hh.body].filter(Boolean).join('\n') : '')
  const reading = $derived(!!said && speech.text === said && !speech.paused)
  // Una risposta nuova a foglio sulla riga: il punto ambra per DOCK_PEEK_MS, l'ora che scorre e il titolo che entra, come la barra di prima.
  let fresh = $state(false)
  let seenAt: number | null | undefined = undefined
  let freshTimer: ReturnType<typeof setTimeout> | undefined
  $effect(() => {
    const at = hh?.at ?? null
    untrack(() => {
      if (seenAt !== undefined && at != null && at !== seenAt && shown === 'row') { fresh = true; clearTimeout(freshTimer); freshTimer = setTimeout(() => (fresh = false), DOCK_PEEK_MS) }
      seenAt = at
    })
  })
  $effect(() => () => clearTimeout(freshTimer))
  let roll = $state(false)
  $effect(() => { const id = setTimeout(() => (roll = true), OPENING_MS); return () => clearTimeout(id) })
  function nuova(node: HTMLElement, at: number | null | undefined) {
    let last = at
    return { update(x: number | null | undefined) { if (x !== last && x != null && last != null && roll && !ridotto()) node.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 150, easing: 'cubic-bezier(.3, 0, .2, 1)' }); last = x } }
  }
  const clean = (s: string) => s.replace(/\*\*|__|`/g, '')
  // Il ⋮ della master (gruppo 4): quello della sua testata, che nel foglio non c'è; lo presta Header (`inSheet`, headMore.svelte.ts).
  const more = $derived(heads.more[master.name] ?? null)
</script>

<svelte:window onkeydown={windowKeys} />

<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<section class="msheet v3" class:inset class:moving data-at={moving ? 'move' : shown} bind:this={el} aria-label={t.summaryTitle} onkeydown={keys}>
  <!-- Il dito sulla maniglia e sulla riga; dalla tastiera fa lo stesso il tasto della maniglia. -->
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="grab" bind:this={grab} onpointerdown={down} onpointermove={move} onpointerup={(e) => release(e)} onpointercancel={(e) => release(e, true)}>
    <button class="gr" type="button" aria-label={t.sheetTap[shown]} title={t.sheetGrip} aria-expanded={shown !== 'row'} data-testid="foglio-maniglia"
      onclick={(e) => { if (e.detail === 0) go(tap(shown)) }}><i aria-hidden="true"></i></button>
    <div class="mh">
      <StateGlyph kind={glyphOf(master)} ctx={master.context} size={20} square={!personal(master.account, master.account_kind)} label={attention ? master.state : label} />
      <span class="tt">
        <span class="l1"><b>{t.summaryTitle}</b>{#if fresh}<i class="nd" aria-hidden="true" data-testid="master-nuova"></i>{/if}</span>
        {#if when || hh}<small class="st">{#if when}<span class="mono"><RollTime value={when} {roll} /></span>{/if}{#if when && hh}<span aria-hidden="true">{' · '}</span>{/if}{#if hh}<span use:nuova={hh.at}>{clean(hh.headline)}</span>{/if}</small>{/if}
      </span>
      {#if hh}
        <button class="ib" type="button" aria-label={reading ? t.stopReading : t.readAloud} title={reading ? t.stopReading : t.readAloud} onclick={() => onSpeak(said)}>
          {#if reading}<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" /></svg>
          {:else}<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>{/if}
        </button>
      {/if}
      {#if more}<span class="more" data-testid="foglio-altro">{@render more()}</span>{/if}
    </div>
  </div>
  <div class="sbody" bind:this={body}>{@render chat()}</div>
</section>

<style>
  /* Il foglio sta sopra la pagina, ancorato in fondo al posto che lo contiene; cambia altezza, mai posto: niente transform, così i
     menu e la scelta di «Cita» dentro la conversazione restano dove li mette il dito. */
  .msheet { position: absolute; left: 0; right: 0; bottom: 0; z-index: 6; display: flex; flex-direction: column; min-height: 0; overflow: hidden;
    height: 170px; background: var(--s1); border-radius: 22px 22px 0 0; box-shadow: 0 -1px 0 var(--line), 0 -18px 40px rgb(0 0 0 / .55); }
  :global(html.theme-day) .msheet { box-shadow: 0 -1px 0 var(--line), 0 -10px 28px rgb(0 0 0 / .12); }
  .msheet.inset { left: 8px; right: 8px; }
  /* La maniglia e la riga della master: si trascinano insieme, e un tocco passa alla misura dopo. */
  .grab { flex: none; touch-action: none; cursor: grab; user-select: none; -webkit-user-select: none; }
  .msheet.moving .grab { cursor: grabbing; }
  .gr { display: grid; place-items: center; width: 100%; height: 14px; padding-top: 6px; border-radius: 22px 22px 0 0; cursor: inherit; }
  .gr i { display: block; width: 40px; height: 4px; border-radius: 2px; background: var(--tx3); transition: background-color .15s; }
  .grab:hover .gr i { background: var(--tx2); }
  .gr:focus-visible { outline: none; }
  .gr:focus-visible i { outline: 2px solid var(--tx2); outline-offset: 3px; }
  .mh { display: flex; align-items: center; gap: 10px; height: 52px; padding: 0 8px 0 14px; }
  .tt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
  .l1 { display: flex; align-items: center; gap: 8px; min-width: 0; }
  .l1 b { font: 700 15.5px/1.2 var(--display); color: var(--tx); }
  /* Lo stato su una riga: quello che non ci sta sfuma sul bordo, senza puntini. */
  .st { display: block; white-space: nowrap; overflow: hidden; font-size: 13px; line-height: 1.3; color: var(--tx2);
    -webkit-mask-image: linear-gradient(to right, #000 calc(100% - 24px), transparent); mask-image: linear-gradient(to right, #000 calc(100% - 24px), transparent); }
  .st .mono { font: 400 12.5px var(--mono); letter-spacing: 0; }
  .nd { flex: none; width: 8px; height: 8px; border-radius: 4px; background: var(--wait); animation: nd-in var(--spring-bounce-t) var(--spring-bounce) both; }
  @keyframes nd-in { from { transform: scale(.4); opacity: 0; } }
  .ib { flex: none; width: 44px; height: 44px; border-radius: 22px; display: grid; place-items: center; color: var(--tx); position: relative; transition: transform 140ms cubic-bezier(.23, 1, .32, 1); }
  .ib::after { content: ''; position: absolute; inset: -2px; }
  .ib:hover { background: var(--s2); }
  .ib:active { transform: scale(.97); }
  /* Il ⋮ prestato dalla testata della master: il suo menu sale dal fondo della finestra, sopra il foglio, e scorre se è lungo. */
  .more { flex: none; display: flex; }
  .more :global(.anchor > .panel) { position: fixed; top: auto; left: auto; right: 12px; bottom: calc(12px + env(safe-area-inset-bottom, 0px));
    width: min(380px, calc(100vw - 24px)); max-height: calc(100dvh - 24px); overflow-y: auto; overscroll-behavior: contain; }
  @media (prefers-reduced-motion: reduce) { .ib:active { transform: none; } .nd { animation: none; } }
  /* La conversazione: ancorata in fondo, alta almeno quanto le sue parti fisse (`--chat-min`); sotto quella misura il bordo del foglio
     ne taglia la cima e resta il fondo, il campo. */
  .sbody { position: relative; flex: 1; min-height: 0; overflow: hidden; }
  .sbody :global(section.chat) { position: absolute; left: 0; right: 0; bottom: 0; height: max(100%, var(--chat-min, 0px)); background: transparent; }
  .sbody :global(section.chat.master > .bar) { margin-top: 0; border-radius: 0; }
  /* Fra la riga e la metà quello che sta sopra il campo compare con il foglio; alla riga non si tocca e non si legge. */
  /* Le finestre (dialog: il contesto, modello ed effort, Chiudi) restano fuori: si aprono anche dalla riga, dall'anellino o dal ⋮. */
  .msheet:not([data-at='half'], [data-at='full']) .sbody :global(section.chat > :not(.bar, .mjbar, .blk.box, .dropzone, .qpick, .chgwrap, dialog)) { opacity: var(--open, 0); }
  .msheet[data-at='row'] .sbody :global(section.chat > :not(.bar, .mjbar, .blk.box, .dropzone, .qpick, .chgwrap, dialog)) { visibility: hidden; }
</style>
