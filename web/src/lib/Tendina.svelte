<script lang="ts">
  import { tick, untrack, type Snippet } from 'svelte'
  import { ridotto } from './molla'
  import { closesOnDrag, dragOffset, fastOutSlowIn, rowDelay, SHADE_MS } from './sessionTabs'
  import { t } from './t'

  // La tendina della sessione (app-posizioni-sistema-27-11, gruppo 2): scende dalla barra di stato sopra la chat, e il campo resta sempre
  // in vista e usabile. app-barra-stato-28-11 (Franz 14:11, «serve che si apra fino dove c'è contenuto»): è alta quanto il suo contenuto
  // più la maniglia, e arriva al campo, col contenuto che scorre dentro, solo quando non ci sta (sessionTabs.shadeHeight: qui lo fa il
  // max-height); sotto una tendina corta la chat resta in vista, con l'ombra sotto il bordo. È solo il contenitore: il contenuto lo passa chi la usa, e il centro notifiche
  // (app-centro-notifiche-26-11) cambierà il contenuto, non il posto. Scende e sale in 360 ms con FAST_OUT_SLOW_IN; le righe del
  // contenuto (i figli degli elementi segnati `data-rows`, quelli in vista) compaiono a 32 ms l'una dall'altra. Si chiude tirandola
  // su dalla maniglia in fondo, o col tocco sulla maniglia. Moto ridotto: niente discesa, compare al suo posto.
  // Il contenuto resta montato anche a tendina chiusa: chi lo usa ci tiene i conteggi della riga di icone.
  let { open, key, label, onClose, children }: {
    /** Aperta: ogni scheda tranne Chat. */
    open: boolean
    /** La sezione mostrata: cambiandola a tendina aperta le sue righe ricompaiono una dopo l'altra. */
    key: string
    /** Il nome della sezione per i lettori di schermo. */
    label: string
    /** La tendina tirata su, o il tocco sulla maniglia: chi la usa torna alla Chat. */
    onClose: () => void
    children: Snippet
  } = $props()

  const EASE = 'cubic-bezier(0.4, 0, 0.2, 1)'
  let panel = $state<HTMLElement>()
  let content = $state<HTMLElement>()
  let shown = $state(untrack(() => open))
  // Da 0 (su, nascosta) a 1 (giù, aperta), e lo spostamento del dito in px: scritti a ogni fotogramma, fuori dalla reattività.
  let p = untrack(() => (open ? 1 : 0))
  let drag = 0
  let raf = 0

  function paint() {
    if (!panel) return
    panel.style.transform = p >= 1 && drag === 0 ? 'none' : `translateY(calc(${((p - 1) * 100).toFixed(3)}% + ${drag.toFixed(1)}px))`
  }
  /** Verso `to` (0 o 1) da dove si è, dito compreso; la corsa intera dura 360 ms, un pezzo meno. */
  function run(to: number) {
    cancelAnimationFrame(raf)
    const h = panel?.offsetHeight || 1
    const from = Math.min(1, Math.max(0, p + drag / h))
    drag = 0
    p = from
    const dur = Math.abs(to - from) * SHADE_MS
    if (ridotto() || dur < 16) { p = to; settled(); return }
    const t0 = performance.now()
    const step = (now: number) => {
      const k = Math.min(1, (now - t0) / Math.max(120, dur))
      p = from + (to - from) * fastOutSlowIn(k)
      paint()
      if (k < 1) raf = requestAnimationFrame(step)
      else settled()
    }
    paint()
    raf = requestAnimationFrame(step)
  }
  function settled() { paint(); if (p <= 0) shown = false }

  /** Le righe della sezione mostrata compaiono una dopo l'altra, a 32 ms di distanza. */
  function cascade() {
    if (!content || ridotto() || !open) return
    // Solo quelle che si vedono: le sezioni nascoste restano montate (i loro conteggi) ma non hanno riquadri.
    const rows = [...content.querySelectorAll<HTMLElement>('[data-rows] > *')].filter((el) => el.getClientRects().length > 0)
    rows.forEach((el, i) => el.animate?.([{ opacity: 0, transform: 'translateY(-6px)' }, { opacity: 1, transform: 'none' }], { duration: SHADE_MS, delay: rowDelay(i), easing: EASE, fill: 'backwards' }))
  }

  let last = { open: untrack(() => open), key: untrack(() => key) }
  $effect(() => {
    const o = open, k = key
    untrack(() => {
      const was = last
      last = { open: o, key: k }
      if (o && !was.open) { shown = true; content?.scrollTo?.({ top: 0 }); run(1); void tick().then(cascade) }
      else if (!o && was.open) run(0)
      else if (o && k !== was.key) { content?.scrollTo?.({ top: 0 }); void tick().then(cascade) }
    })
  })
  $effect(() => { if (panel) paint() })
  $effect(() => () => cancelAnimationFrame(raf))

  // La maniglia: la tendina segue il dito verso l'alto; al rilascio si chiude oltre un quarto o con uno slancio, se no torna giù.
  let pid: number | null = null
  let y0 = 0, lastY = 0, lastT = 0, vel = 0
  let moved = false
  function down(e: PointerEvent) {
    if (!e.isPrimary || e.button !== 0 || !open) return
    pid = e.pointerId
    y0 = lastY = e.clientY
    lastT = performance.now()
    vel = 0
    moved = false
    cancelAnimationFrame(raf)
    try { (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId) } catch { /* il puntatore è già andato */ }
  }
  function move(e: PointerEvent) {
    if (e.pointerId !== pid || !panel) return
    const now = performance.now(), dt = now - lastT
    if (dt > 0) vel = ((e.clientY - lastY) / dt) * 1000
    lastY = e.clientY
    lastT = now
    const dy = e.clientY - y0
    if (Math.abs(dy) > 4) moved = true
    drag = dragOffset(dy, panel.offsetHeight)
    paint()
  }
  function up(e: PointerEvent, cancel = false) {
    if (e.pointerId !== pid || !panel) return
    pid = null
    if (!cancel && closesOnDrag(e.clientY - y0, vel, panel.offsetHeight)) onClose()
    else run(1)
  }
</script>

<div class="tdw" class:on={shown} inert={!open} aria-hidden={!open} data-testid="tendina">
  <div class="td" bind:this={panel} role="tabpanel" aria-label={label}>
    <div class="tdc" bind:this={content}>{@render children()}</div>
    <button class="grip" type="button" aria-label={t.shadeClose} title={t.shadeClose} data-testid="tendina-maniglia"
      onpointerdown={down} onpointermove={move} onpointerup={(e) => up(e)} onpointercancel={(e) => up(e, true)} onclick={() => { if (!moved) onClose(); moved = false }}><i></i></button>
  </div>
</div>

<style>
  /* Sopra la chat e sotto la barra di stato: quello che sale oltre il bordo in alto non si vede. */
  .tdw { position: absolute; inset: 0; z-index: 6; overflow: hidden; pointer-events: none; visibility: hidden; }
  .tdw.on { visibility: visible; }
  .td { position: absolute; left: 0; right: 0; top: 0; max-height: 100%; display: flex; flex-direction: column; background: var(--bg); border-bottom: 1px solid var(--line);
    border-radius: 0 0 18px 18px; box-shadow: 0 10px 24px rgb(0 0 0 / .45); pointer-events: auto; transform: translateY(-100%); will-change: transform; }
  .tdc { flex: 0 1 auto; min-height: 0; overflow-y: auto; overscroll-behavior: contain; padding: 10px 12px 12px; }
  .grip { flex: none; height: 26px; display: grid; place-items: center; touch-action: none; cursor: grab; border-radius: 0 0 18px 18px; }
  .grip:active { cursor: grabbing; }
  .grip i { width: 36px; height: 4px; border-radius: 2px; background: var(--tx3); }
  .grip:focus-visible { outline: 2px solid var(--tx2); outline-offset: -4px; }
</style>
