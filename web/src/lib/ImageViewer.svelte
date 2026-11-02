<script lang="ts">
  // Il visore delle immagini della chat (app-chat-zoom-02-11), come ImageViewer di SessionSheet.kt: due dita ingrandiscono
  // attorno alle dita, ingrandita si sposta col dito, il doppio tocco porta a tutta larghezza o torna intera. Col mouse la
  // rotella con Ctrl e il pizzico del trackpad (che il browser manda come rotella con Ctrl) ingrandiscono attorno al
  // puntatore; la rotella da sola sposta l'immagine ingrandita. Il calcolo sta in zoomPan.ts, con i test.
  import { onMount, untrack } from 'svelte'
  import { t } from './t'
  import { clamp, doubleTap, fit, IDENTITY, maxScale, step, type View } from './zoomPan'

  let { src, name, onClose }: { src: string; name: string; onClose: () => void } = $props()
  let box: HTMLDivElement | undefined = $state()
  // Nel livello più alto della pagina (showModal): niente le passa sopra, nemmeno la striscia della demo.
  let dlg: HTMLDialogElement | undefined = $state()
  let vw = $state(0), vh = $state(0), iw = $state(0), ih = $state(0)
  let view = $state<View>(IDENTITY)
  const content = () => fit(iw, ih, vw, vh)
  const go = (zoom: number, px: number, py: number, cx: number, cy: number) => {
    const [cw, ch] = content()
    view = step(view, zoom, px, py, cx, cy, cw, ch, vw, vh, maxScale(cw, vw))
  }
  const local = (e: { clientX: number; clientY: number }) => { const r = box!.getBoundingClientRect(); return [e.clientX - r.left, e.clientY - r.top] as const }

  // I puntatori sullo schermo: uno sposta (da ingrandita), due ingrandiscono e spostano col loro centro.
  const pts = new Map<number, { x: number; y: number }>()
  const mid = () => { const p = [...pts.values()]; return { x: (p[0].x + p[1].x) / 2, y: (p[0].y + p[1].y) / 2, d: Math.hypot(p[0].x - p[1].x, p[0].y - p[1].y) } }
  function down(e: PointerEvent) { try { box?.setPointerCapture(e.pointerId) } catch { /* puntatore già sparito */ } const [x, y] = local(e); pts.set(e.pointerId, { x, y }) }
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
    }
  }
  function up(e: PointerEvent) { pts.delete(e.pointerId) }
  function dbl(e: MouseEvent) { const [x, y] = local(e); const [cw, ch] = content(); view = doubleTap(view, x, y, cw, ch, vw, vh) }

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

<dialog class="iv" bind:this={dlg} aria-label={name} data-testid="visore-immagine" oncancel={(e) => { e.preventDefault(); onClose() }}>
  <div class="ivbar"><span class="ivname">{name}</span><button class="ivclose" aria-label={t.closeWord} onclick={onClose}>✕</button></div>
  <div class="ivbox" bind:this={box} bind:clientWidth={vw} bind:clientHeight={vh} onpointerdown={down} onpointermove={move} onpointerup={up} onpointercancel={up} ondblclick={dbl} role="presentation">
    <img {src} alt={name} draggable="false" onload={(e) => { iw = e.currentTarget.naturalWidth; ih = e.currentTarget.naturalHeight }} style:transform="translate({view.x}px, {view.y}px) scale({view.scale})" />
  </div>
</dialog>

<style>
  .iv { position: fixed; inset: 0; width: 100vw; height: 100dvh; max-width: none; max-height: none; margin: 0; padding: 0; border: 0; background: #000; display: flex; flex-direction: column; }
  .iv::backdrop { background: #000; }
  .ivbar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; color: var(--text2); font-size: 14px; }
  .ivname { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; }
  .ivclose { width: 40px; height: 40px; border-radius: 50%; background: var(--high); color: var(--text); font-size: 18px; }
  .ivbox { flex: 1; min-height: 0; overflow: hidden; touch-action: none; cursor: zoom-in; user-select: none; }
  .ivbox img { display: block; width: 100%; height: 100%; object-fit: contain; transform-origin: center; will-change: transform; }
</style>
