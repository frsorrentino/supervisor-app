<script lang="ts">
  import { untrack } from 'svelte'
  import { molla, motore, ridotto, clamp } from './molla'
  import { scrubAt, scrubNearest } from './animazioni'
  import { t } from './t'
  // Voce 8 del terzo giro, fedele agli originali (Scrubber e Range slider; app-animazioni-terzo-c-26-11): sotto la pillola della
  // lettura una barra con un segno per pezzo letto. La capsula del punto è spenta (0,15, scala 0,7) e si accende mentre trascini
  // (0,8, 1) sulla molla di Scrubber (ζ 0,9, rigidità 1150); sopra il dito un'etichetta dice dove arrivi e si inclina con la velocità
  // come la bolla di Range slider (-0,02° per px/s, fino a 12°).
  // Dopo la prova 1391 (Franz, 27/11: «troppo a scatti e poco fluido»): il punto sta all'inizio del pezzo che la voce legge e quando
  // la voce passa al dopo ci va in 150 ms con (0.23, 1, 0.32, 1). Il dito prende il punto appena tocca e lo porta uno a uno; i segni
  // sono solo disegnati. Al rilascio il punto si aggancia al pezzo che comincia più vicino con la molla del pannello (400 40, senza
  // rimbalzo, come CmMotion.panelFloat) e la lettura riparte da lì. Mentre trascini solo trasformazioni scritte a mano, e la barra si
  // misura una volta, al tocco. Moto ridotto: salti secchi.
  let { count, at, onSeek }: { count: number; at: number; onSeek: (i: number) => void } = $props()
  let bar = $state<HTMLElement>()
  let fillEl = $state<HTMLElement>()
  let capW = $state<HTMLElement>()
  let capEl = $state<HTMLElement>()
  let lblW = $state<HTMLElement>()
  let lblEl = $state<HTMLElement>()
  let w = $state(0)
  let dragging = $state(false)
  let target = $state(0)
  let left = 0, width = 0, lastX = 0, lastT = 0, tilt = 0
  // Il pezzo scelto al rilascio, finché la voce non ci arriva (al più 3 s): intanto il punto non torna su quello di prima.
  let landing: number | null = null
  let landTimer: ReturnType<typeof setTimeout> | undefined
  const P = molla(untrack(() => scrubAt(at, count)), 400, 40)
  const C = molla(0, 1150, 2 * 0.9 * Math.sqrt(1150))
  function paint() {
    const f = clamp(P.x, 0, 1)
    const px = (f * (dragging ? width : w)).toFixed(2)
    if (fillEl) fillEl.style.transform = `translateX(${((f - 1) * 100).toFixed(3)}%)`
    if (capW) capW.style.transform = `translateX(${px}px)`
    if (lblW) lblW.style.transform = `translateX(${px}px)`
    if (lblEl) lblEl.style.rotate = `${tilt.toFixed(2)}deg`
    if (capEl) { const c = clamp(C.x, 0, 1); capEl.style.opacity = (0.15 + 0.65 * c).toFixed(3); capEl.style.scale = (0.7 + 0.3 * c).toFixed(3) }
    // Finite le molle, i salti della voce tornano a scorrere in 150 ms.
    if (!dragging && P.x === P.t && P.v === 0 && C.x === C.t && C.v === 0) bar?.classList.remove('live')
  }
  const m = motore([P, C], paint)
  $effect(() => () => { m.stop(); clearTimeout(landTimer) })
  // Prima pittura e cambi di larghezza.
  $effect(() => { void w; void fillEl; void capW; untrack(paint) })
  function follow(a: number, n: number) {
    if (dragging) return
    if (landing != null) { if (a === landing) { landing = null; clearTimeout(landTimer) } return }
    const to = scrubAt(a, n)
    if (P.t === to) return
    P.x = P.t = to
    P.v = 0
    paint()
  }
  $effect(() => { const a = at, n = count; untrack(() => follow(a, n)) })
  function down(e: PointerEvent) {
    if (!bar) return
    bar.setPointerCapture(e.pointerId)
    const r = bar.getBoundingClientRect()
    left = r.left
    width = r.width || 1
    bar.classList.add('live')
    dragging = true
    lastX = e.clientX
    lastT = e.timeStamp
    tilt = 0
    P.x = P.t = clamp((e.clientX - left) / width, 0, 1)
    P.v = 0
    target = scrubNearest(P.x, count)
    C.t = 1
    m.vai()
    paint()
  }
  function move(e: PointerEvent) {
    if (!dragging) return
    P.x = P.t = clamp((e.clientX - left) / width, 0, 1)
    P.v = 0
    const dt = (e.timeStamp - lastT) / 1000
    if (dt > 0) tilt = clamp(((e.clientX - lastX) / dt) * -0.02, -12, 12)
    lastX = e.clientX
    lastT = e.timeStamp
    const k = scrubNearest(P.x, count)
    if (k !== target) target = k
    paint()
  }
  function up() {
    if (!dragging) return
    const k = scrubNearest(P.x, count)
    dragging = false
    tilt = 0
    P.t = scrubAt(k, count)
    C.t = 0
    landing = k
    clearTimeout(landTimer)
    landTimer = setTimeout(() => { landing = null; follow(at, count) }, 3_000)
    m.vai()
    onSeek(k)
  }
  function cancel() {
    if (!dragging) return
    dragging = false
    tilt = 0
    P.x = P.t = scrubAt(at, count)
    P.v = 0
    C.t = 0
    m.vai()
  }
</script>

<div class="scrub" bind:this={bar} bind:clientWidth={w} class:still={ridotto()} role="slider" tabindex="0" aria-label={t.readingSeek(at + 1, count)}
  aria-valuemin={1} aria-valuemax={count} aria-valuenow={at + 1} data-testid="lettura-scrubber"
  onpointerdown={down} onpointermove={move} onpointerup={up} onpointercancel={cancel}
  onkeydown={(e) => { if (e.key === 'ArrowRight' && at < count - 1) onSeek(at + 1); else if (e.key === 'ArrowLeft' && at > 0) onSeek(at - 1); else if (e.key === 'Home') onSeek(0); else if (e.key === 'End') onSeek(count - 1) }}>
  <span class="track"><span class="fill" bind:this={fillEl}></span></span>
  {#each Array.from({ length: count - 1 }, (_, i) => i + 1) as i}<span class="tk" style:left="{(i / count) * 100}%"></span>{/each}
  <span class="capw" bind:this={capW}><span class="cap" bind:this={capEl}></span></span>
  <span class="lblw" class:on={dragging} bind:this={lblW} aria-hidden="true"><span class="lbl" bind:this={lblEl}>{t.readingSeek(target + 1, count)}</span></span>
</div>

<style>
  .scrub { position: relative; flex: 1 0 100%; height: 24px; margin: 0 8px 2px; touch-action: none; cursor: pointer; }
  .track { position: absolute; left: 0; right: 0; top: 8px; height: 8px; border-radius: 4px; background: var(--s3); overflow: hidden; }
  .fill { position: absolute; inset: 0; border-radius: 4px; background: rgb(255 255 255 / .14); transform: translateX(-100%); }
  .tk { position: absolute; top: 9px; width: 1px; height: 6px; border-radius: 999px; background: var(--tx3); translate: -50% 0; }
  .capw, .lblw { position: absolute; left: 0; width: 0; height: 0; will-change: transform; }
  .capw { top: 50%; }
  .cap { position: absolute; left: -2px; top: -11px; width: 4px; height: 22px; border-radius: 999px; background: var(--tx); opacity: .15; scale: .7; }
  .lblw { bottom: 40px; visibility: hidden; }
  .lblw.on { visibility: visible; }
  .lbl { position: absolute; bottom: 0; left: 0; translate: -50% 0; transform-origin: 50% 100%; padding: 5px 10px; border-radius: 10px; background: var(--icon); color: var(--bg); font: 600 12.5px var(--sans, sans-serif); white-space: nowrap; pointer-events: none; }
  /* I salti della voce scorrono in 150 ms; col dito e con la molla dell'aggancio (classe live, messa a mano al tocco) niente transizioni. */
  .fill, .capw { transition: transform 150ms cubic-bezier(0.23, 1, 0.32, 1); }
  .scrub:global(.live) .fill, .scrub:global(.live) .capw, .still .fill, .still .capw { transition: none; }
</style>
