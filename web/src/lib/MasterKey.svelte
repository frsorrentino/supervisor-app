<script lang="ts">
  import { comboDir, DEAD_FINGER, DEAD_MOUSE, DEG, dirAt, moves, type Dir, type Move, type Spot } from './masterKey'
  import { t } from './t'
  // Il tasto tondo della master (app-sposta-master-a-13-11; mockup giro E, Franz 13/11 07:48: «ripristinare il tasto della master
  // come quello su telefono, ma che al passaggio del mouse mostrasse la freccia all'interno del tasto stesso nella direzione
  // possibile a seconda della posizione del mouse all'interno del tasto»). A riposo il chevron, che apre e chiude. Col mouse sopra,
  // una freccia sola: quella possibile dalla parte del tasto dove sta il puntatore; al centro (8 px) e verso un lato dove non si
  // può andare resta il chevron. Il clic sposta. Dito e tastiera (scelte nostre, dal giro D): pressione lunga e trascinamento, poi
  // il rilascio; frecce (anche due insieme), Invio, Esc. `onPreview` accende la zona d'arrivo; `onMove` sposta.
  let { now, sessionsOpen, up, label, onToggle, onPreview, onMove }: {
    now: Spot; sessionsOpen: boolean
    /** Il chevron: su = apre, giù = chiude. */
    up: boolean; label: string
    onToggle: () => void; onPreview: (m: Move | null, all: Move[]) => void; onMove: (m: Move) => void
  } = $props()
  const all = $derived(moves(now, sessionsOpen))
  let dir = $state<Dir | null>(null)
  let mode: 'mouse' | 'finger' | 'keys' | null = null
  let el: HTMLButtonElement | undefined = $state()
  const moveOf = (d: Dir | null) => all.find(m => m.dir === d) ?? null
  function show(d: Dir | null) {
    const m = moveOf(d)
    if ((m?.dir ?? null) === dir && (m != null || mode == null)) return
    dir = m?.dir ?? null
    onPreview(m, all)
  }
  function done() { mode = null; held.clear(); if (dir != null) { dir = null; onPreview(null, all) } }
  function at(e: PointerEvent, dead: number): Dir | null {
    const r = el!.getBoundingClientRect()
    return dirAt(e.clientX - (r.left + r.width / 2), e.clientY - (r.top + r.height / 2), dead, all.map(m => m.dir))
  }
  // Mouse: il passaggio dentro il tasto sceglie, il clic sposta; col chevron il clic apre e chiude.
  let timer: ReturnType<typeof setTimeout> | undefined
  let skipClick = false
  function pmove(e: PointerEvent) {
    if (e.pointerType === 'mouse') { mode = 'mouse'; show(at(e, DEAD_MOUSE)) }
    else if (mode === 'finger') show(at(e, DEAD_FINGER))
  }
  function pleave(e: PointerEvent) { if (e.pointerType === 'mouse' && mode === 'mouse') done() }
  // Dito: tocco corto apre e chiude; tenuto premuto 450 ms, il trascinamento sceglie la direzione e il rilascio sposta;
  // rilasciato sul tasto annulla.
  function pdown(e: PointerEvent) {
    e.stopPropagation()
    if (e.pointerType === 'mouse') return
    el!.setPointerCapture(e.pointerId)
    timer = setTimeout(() => { mode = 'finger'; navigator.vibrate?.(12) }, 450)
  }
  function pup(e: PointerEvent) {
    if (e.pointerType === 'mouse') return
    clearTimeout(timer)
    if (mode !== 'finger') return
    skipClick = true
    const m = moveOf(dir)
    if (m) { mode = null; dir = null; onMove(m) } else done()
  }
  function click(e: MouseEvent) {
    e.stopPropagation()
    if (skipClick) { skipClick = false; return }
    const m = moveOf(dir)
    if (m) { mode = null; dir = null; onMove(m) } else onToggle()
  }
  // Tastiera: col tasto a fuoco una freccia, o due insieme, sceglie; Invio sposta, Esc annulla; Invio senza freccia apre e chiude.
  const held = new Set<'N' | 'S' | 'E' | 'W'>()
  const KEY: Record<string, 'N' | 'S' | 'E' | 'W'> = { ArrowUp: 'N', ArrowDown: 'S', ArrowLeft: 'W', ArrowRight: 'E' }
  function kdown(e: KeyboardEvent) {
    e.stopPropagation()
    if (KEY[e.key]) { e.preventDefault(); held.add(KEY[e.key]); mode = 'keys'; show(comboDir([...held])) }
    else if (e.key === 'Escape' && dir != null) { e.preventDefault(); done() }
  }
  function kup(e: KeyboardEvent) { if (KEY[e.key]) held.delete(KEY[e.key]) }
</script>

<button class="mkey" class:arrow={dir != null} bind:this={el} data-testid="master-tasto" data-dir={dir ?? ''}
  aria-label={dir ? t.masterKeyMove(label) : up ? t.dockConversation : t.dockCollapse} title={up ? t.dockConversation : t.dockCollapse} aria-keyshortcuts="ArrowUp ArrowDown ArrowLeft ArrowRight"
  onpointermove={pmove} onpointerleave={pleave} onpointerdown={pdown} onpointerup={pup} onpointercancel={() => { clearTimeout(timer); done() }}
  onclick={click} onkeydown={kdown} onkeyup={kup} onblur={() => { if (mode === 'keys') done() }}>
  {#if dir}
    <svg viewBox="0 0 24 24" width="26" height="26" style:transform="rotate({DEG[dir] + 90}deg)" aria-hidden="true"><path d="M12 19V5M6 11l6-6 6 6" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  {:else}
    <svg viewBox="0 0 24 24" width="26" height="26" aria-hidden="true"><path d={up ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  {/if}
</button>

<style>
  /* Tondo, 52 px, come sul telefono. Con la freccia prende l'ambra della zona d'arrivo. */
  .mkey { flex: none; width: 52px; height: 52px; border-radius: 50%; background: var(--s3); color: var(--tx); display: grid; place-items: center; touch-action: none; transition: background 120ms, transform 120ms; }
  .mkey svg { transition: transform 140ms cubic-bezier(.23, 1, .32, 1); }
  .mkey.arrow { background: var(--wait); color: var(--bg); }
  .mkey:active { transform: scale(.95); }
  .mkey:focus-visible { outline: 2px solid var(--busy); outline-offset: 3px; }
  @media (prefers-reduced-motion: reduce) { .mkey, .mkey svg { transition-duration: 1ms; } }
</style>
