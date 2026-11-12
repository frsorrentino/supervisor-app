<script lang="ts">
  // Il visore delle immagini della chat (app-chat-zoom-02-11), come ImageViewer di SessionSheet.kt: due dita ingrandiscono
  // attorno alle dita, ingrandita si sposta col dito, il doppio tocco porta a tutta larghezza o torna intera. Col mouse la
  // rotella con Ctrl e il pizzico del trackpad (che il browser manda come rotella con Ctrl) ingrandiscono attorno al
  // puntatore; la rotella da sola sposta l'immagine ingrandita. Il calcolo sta in zoomPan.ts, con i test.
  // Con `items` (app-scheda-scelta-12-11) le immagini sono più d'una, quelle delle opzioni di una scelta: da intera il dito la
  // trascina di lato e passa alla vicina, le frecce della tastiera e i due tasti fanno lo stesso; ingrandita il dito la sposta.
  import { onMount, untrack } from 'svelte'
  import { t } from './t'
  import { swipeTo } from './scelta'
  import { clamp, doubleTap, fit, IDENTITY, maxScale, step, type View } from './zoomPan'

  let { src = '', name = '', items = null, start = 0, onClose }: { src?: string; name?: string; items?: { src: string; name: string }[] | null; start?: number; onClose: () => void } = $props()
  // svelte-ignore state_referenced_locally
  let at = $state(Math.min(Math.max(0, start), Math.max(0, (items?.length ?? 1) - 1)))
  const many = $derived((items?.length ?? 0) > 1)
  const cur = $derived(items?.length ? items[Math.min(at, items.length - 1)] : { src, name })
  let box: HTMLDivElement | undefined = $state()
  // Nel livello più alto della pagina (showModal): niente le passa sopra, nemmeno la striscia della demo.
  let dlg: HTMLDialogElement | undefined = $state()
  let vw = $state(0), vh = $state(0), iw = $state(0), ih = $state(0)
  let view = $state<View>(IDENTITY)
  // Da intera il dito trascina l'immagine di lato: `drag` è di quanto, `settle` il ritorno al centro dopo il rilascio.
  let drag = $state(0), settle = $state(false)
  const content = () => fit(iw, ih, vw, vh)
  const go = (zoom: number, px: number, py: number, cx: number, cy: number) => {
    const [cw, ch] = content()
    view = step(view, zoom, px, py, cx, cy, cw, ch, vw, vh, maxScale(cw, vw))
  }
  const local = (e: { clientX: number; clientY: number }) => { const r = box!.getBoundingClientRect(); return [e.clientX - r.left, e.clientY - r.top] as const }
  function show(i: number) {
    if (!items || i < 0 || i >= items.length || i === at) return
    at = i; view = IDENTITY; iw = 0; ih = 0
  }

  // I puntatori sullo schermo: uno sposta (da ingrandita) o trascina di lato (da intera), due ingrandiscono e spostano col loro centro.
  const pts = new Map<number, { x: number; y: number }>()
  let from: { x: number; y: number; t: number } | null = null
  const mid = () => { const p = [...pts.values()]; return { x: (p[0].x + p[1].x) / 2, y: (p[0].y + p[1].y) / 2, d: Math.hypot(p[0].x - p[1].x, p[0].y - p[1].y) } }
  function down(e: PointerEvent) {
    try { box?.setPointerCapture(e.pointerId) } catch { /* puntatore già sparito */ }
    const [x, y] = local(e); pts.set(e.pointerId, { x, y })
    from = pts.size === 1 && many && view.scale === 1 ? { x, y, t: performance.now() } : null
    settle = false
    if (pts.size > 1) drag = 0
  }
  function move(e: PointerEvent) {
    const was = pts.get(e.pointerId)
    if (!was) return
    const [x, y] = local(e)
    if (pts.size >= 2) {
      const a = mid()
      pts.set(e.pointerId, { x, y })
      const b = mid()
      if (a.d > 0) go(b.d / a.d, b.x - a.x, b.y - a.y, b.x, b.y)
    } else {
      pts.set(e.pointerId, { x, y })
      if (view.scale > 1) go(1, x - was.x, y - was.y, x, y)
      // Ai capi il dito tira ma l'immagine cede un terzo: non c'è niente oltre.
      else if (from) { const dx = x - from.x, edge = (dx > 0 && at === 0) || (dx < 0 && at === (items?.length ?? 1) - 1); drag = edge ? dx / 3 : dx }
    }
  }
  function up(e: PointerEvent) {
    const was = pts.get(e.pointerId)
    pts.delete(e.pointerId)
    if (from && was && !pts.size) {
      const to = swipeTo(was.x - from.x, was.y - from.y, performance.now() - from.t)
      if (to && items && at + to >= 0 && at + to < items.length) { drag = 0; show(at + to) } else { settle = true; drag = 0 }
    }
    from = null
  }
  function dbl(e: MouseEvent) { const [x, y] = local(e); const [cw, ch] = content(); view = doubleTap(view, x, y, cw, ch, vw, vh) }
  // Le frecce passano all'immagine vicina; Esc chiude da sé (oncancel del dialogo).
  function key(e: KeyboardEvent) {
    if (!many || e.altKey || e.ctrlKey || e.metaKey) return
    if (e.key === 'ArrowRight') { e.preventDefault(); show(at + 1) } else if (e.key === 'ArrowLeft') { e.preventDefault(); show(at - 1) }
  }

  onMount(() => {
    // Non passiva: la rotella con Ctrl ingrandirebbe tutta la pagina.
    const wheel = (e: WheelEvent) => {
      e.preventDefault()
      const [x, y] = local(e)
      if (e.ctrlKey) go(Math.exp(-e.deltaY * 0.01), 0, 0, x, y)
      else if (view.scale > 1) go(1, -e.deltaX, -e.deltaY, x, y)
    }
    dlg?.showModal()
    box?.addEventListener('wheel', wheel, { passive: false })
    return () => { box?.removeEventListener('wheel', wheel); dlg?.close() }
  })
  // Se la finestra cambia misura l'immagine resta dentro i bordi.
  $effect(() => { const [cw, ch] = content(); untrack(() => { view = clamp(view, cw, ch, vw, vh) }) })
</script>

<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<dialog class="iv" bind:this={dlg} aria-label={cur.name} data-testid="visore-immagine" oncancel={(e) => { e.preventDefault(); onClose() }} onkeydown={key}>
  <div class="ivbar">
    <span class="ivname">{cur.name}</span>
    {#if many}<span class="ivat" aria-live="polite">{t.viewerAt(at + 1, items!.length)}</span>{/if}
    <button class="ivclose" aria-label={t.closeWord} onclick={onClose}><svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg></button>
  </div>
  <div class="ivbox" class:zoomed={view.scale > 1} bind:this={box} bind:clientWidth={vw} bind:clientHeight={vh} onpointerdown={down} onpointermove={move} onpointerup={up} onpointercancel={up} ondblclick={dbl} role="presentation">
    {#key cur.src}<img src={cur.src} alt={cur.name} draggable="false" class:settle onload={(e) => { const im = e.currentTarget as HTMLImageElement; iw = im.naturalWidth; ih = im.naturalHeight }} style:transform="translate({view.x + drag}px, {view.y}px) scale({view.scale})" />{/key}
  </div>
  {#if many}
    <button class="ivstep prev" aria-label={t.viewerPrev} disabled={at === 0} onclick={() => show(at - 1)}><svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><path d="M15 5l-7 7 7 7" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
    <button class="ivstep next" aria-label={t.viewerNext} disabled={at === items!.length - 1} onclick={() => show(at + 1)}><svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><path d="M9 5l7 7-7 7" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
  {/if}
</dialog>

<style>
  .iv { position: fixed; inset: 0; width: 100vw; height: 100dvh; max-width: none; max-height: none; margin: 0; padding: 0; border: 0; background: #000; display: flex; flex-direction: column; }
  .iv::backdrop { background: #000; }
  .ivbar { display: flex; align-items: center; gap: 12px; padding: 8px 12px; padding-top: calc(8px + env(safe-area-inset-top, 0px)); color: var(--text2); font-size: 14px; }
  .ivname { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; color: var(--text); font: 600 16px var(--display, inherit); }
  .ivat { flex: none; font: 13px var(--mono); }
  .ivclose { flex: none; width: 44px; height: 44px; border-radius: 50%; background: var(--high); color: var(--text); display: grid; place-items: center; touch-action: manipulation; }
  .ivbox { flex: 1; min-height: 0; overflow: hidden; touch-action: none; cursor: zoom-in; user-select: none; -webkit-user-select: none; }
  .ivbox.zoomed { cursor: grab; }
  .ivbox img { display: block; width: 100%; height: 100%; object-fit: contain; transform-origin: center; will-change: transform; -webkit-touch-callout: none; }
  /* Rilasciata senza cambiare immagine torna al centro; mentre il dito la tiene segue il dito, senza transizione. */
  .ivbox img.settle { transition: transform 200ms cubic-bezier(0.23, 1, 0.32, 1); }
  .ivstep { position: absolute; top: 50%; width: 48px; height: 48px; margin-top: -24px; border-radius: 50%; background: color-mix(in srgb, #000 55%, transparent); color: #fff; display: grid; place-items: center; touch-action: manipulation; transition: opacity 160ms ease-out, transform 120ms ease-out; }
  .ivstep.prev { left: calc(10px + env(safe-area-inset-left, 0px)); }
  .ivstep.next { right: calc(10px + env(safe-area-inset-right, 0px)); }
  .ivstep:disabled { opacity: 0; pointer-events: none; }
  .ivstep:active { transform: scale(.94); }
  .ivclose:focus-visible, .ivstep:focus-visible { outline: 2px solid #fff; outline-offset: 2px; }
  @media (hover: hover) and (pointer: fine) { .ivstep:hover { background: color-mix(in srgb, #fff 18%, #000); } }
  @media (prefers-reduced-motion: reduce) { .ivbox img.settle, .ivstep { transition: none; } }
</style>
