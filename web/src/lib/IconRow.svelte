<script lang="ts">
  import { untrack } from 'svelte'
  import CatIcon from './CatIcon.svelte'
  import { ferma, molla, motore, ridotto, type Molla } from './molla'
  import {
    altDigit, badge, damping, direction, DRAG, edges, FOLLOW, keyMove, NAME, NAME_GAP, nameAlpha, nameGap, pill, POP, PUSH, ROW, settle,
    spoken, stretch, target, under, type IconTab, type Span, type Spring,
  } from './iconRow'

  // La riga di icone (app-posizioni-sistema-27-11, gruppo 1) con la pillola elastica del Rubber dock (app-dock-elastico-27-11, Franz
  // 27/11 20:46), come riga-elastica.html della master. Ogni scheda ha icona e numero; quella aperta anche il nome, che si apre con la
  // molla 300/26 mentre quello della lasciata si chiude. La pillola ha due bordi su due molle (edges in iconRow.ts): davanti rigido
  // (--dock-lead 640/38), dietro pigro (--dock-trail 150/16), così si allunga fra le schede; col dito (1100/56 e 320/26) si trascina,
  // e oltre la prima e l'ultima scheda tira come un elastico (rubber di dock.js) con la riga che segue al 35 % (300/18). L'icona scelta
  // fa un piccolo salto (--dock-pop 520/16, spinta -3,2). Posto e forma della pillola li decide solo `pill`, sulla meta di `target`.
  // Moto ridotto: molle critiche, niente salto, e il motore di molla.ts le porta subito alla meta. Le schede non cambiano mai ordine.
  let { tabs, active, onPick, onEsc, label, keyScope = null }: {
    tabs: IconTab[]
    /** La scheda aperta, con la sua `key`. */
    active: string | null
    /** Un tocco su una scheda, anche su quella aperta (decide chi chiama: la apre o chiude la tendina), o il dito che si ferma su un'altra. */
    onPick: (key: string) => void
    /** Esc: torna alla conversazione. */
    onEsc?: () => void
    /** Il nome della riga per i lettori di schermo. */
    label?: string
    /** Dove valgono Alt+1…5 ed Esc: col fuoco dentro questo elemento (la colonna della riga). Senza, in tutta la pagina, se nessuno li ha già presi. */
    keyScope?: HTMLElement | null
  } = $props()

  let root = $state<HTMLElement>()
  let hl = $state<HTMLElement>()

  const tune = (s: Molla, sp: Spring) => { s.k = sp.k; s.c = damping(sp, ridotto()) }
  // I due bordi della pillola e la riga che segue l'elastico; per ogni scheda il nome (da 0 chiuso a 1 aperto) e il salto dell'icona.
  const L = molla(0), R = molla(0), N = molla(0)
  tune(N, ROW)
  const per = new Map<string, { w: Molla; p: Molla }>()
  const all = (): Molla[] => [L, R, N, ...[...per.values()].flatMap((s) => [s.w, s.p])]
  let eng: { vai: () => void; stop: () => void } | null = null
  // La scheda sotto la pillola: quella aperta, o quella sotto il dito mentre trascina (`preview`, scelta solo al rilascio).
  let shown: string | null = untrack(() => active)
  let preview: string | null = null
  let extra = 0
  let placed = false

  function retune(dir: number, drag: boolean) { const [a, b] = edges(dir, drag); tune(L, a); tune(R, b) }
  retune(0, false)

  const items = (): HTMLElement[] => (root ? [...root.querySelectorAll<HTMLElement>(':scope > .it')] : [])
  const spanOf = (el: HTMLElement): Span => ({ left: el.offsetLeft, right: el.offsetLeft + el.offsetWidth })
  const indexOf = (key: string | null): number => (key == null ? -1 : tabs.findIndex((t) => t.key === key))

  /** La meta dei due bordi: la scheda sotto la pillola com'è adesso nel layout, più l'elastico. */
  function aim(els = items()): Span | null {
    const el = els.find((e) => e.dataset.k === shown)
    const g = target(el ? spanOf(el) : null, extra)
    if (g) { L.t = g.left; R.t = g.right }
    return g
  }

  function render() {
    if (!root || !hl) return
    const els = items()
    const still = all().every(ferma)
    // Prima le letture, dal layout del fotogramma prima, poi le scritture: il layout si ricalcola una volta sola per fotogramma.
    const fulls = els.map((el) => (el.querySelector<HTMLElement>('.nmi')?.offsetWidth ?? 0) + NAME_GAP)
    const top = els[0]?.offsetTop ?? 0
    let goal = still ? null : aim(els)
    els.forEach((el, j) => {
      const s = per.get(el.dataset.k ?? '')
      const nm = el.querySelector<HTMLElement>('.nm'), ic = el.querySelector<HTMLElement>('.ic')
      if (!s || !nm || !ic) return
      const f = s.w.x
      nm.style.width = f === 1 && s.w.v === 0 ? 'auto' : `${Math.max(0, f * fulls[j]).toFixed(2)}px`
      nm.style.paddingLeft = `${(nameGap(f) * NAME_GAP).toFixed(2)}px`
      nm.style.opacity = nameAlpha(f).toFixed(3)
      ic.style.transform = s.p.x === 1 ? '' : `scale(${s.p.x.toFixed(4)})`
      el.classList.toggle('on', el.dataset.k === shown)
    })
    // Ferme le molle, la meta si prende dal layout appena scritto: la pillola sta esatta sulla scheda.
    if (still) { goal = aim(els); L.x = L.t; R.x = R.t; L.v = R.v = 0 }
    root.style.transform = Math.abs(N.x) > 0.05 ? `translateX(${N.x.toFixed(2)}px)` : ''
    if (!goal) { hl.style.opacity = '0'; return }
    const g = pill(L.x, R.x, goal)
    hl.style.opacity = ''
    hl.style.width = `${g.width.toFixed(2)}px`
    hl.style.height = `${g.height.toFixed(2)}px`
    hl.style.transform = `translate3d(${g.x.toFixed(2)}px, ${(top + g.top).toFixed(2)}px, 0)`
  }

  /** Tutto al suo posto senza moto, alla prima misura. */
  function place() {
    for (const [k, s] of per) { s.w.x = s.w.t = k === shown ? 1 : 0; s.w.v = 0; s.p.x = s.p.t = 1; s.p.v = 0 }
    N.x = N.t = 0
    N.v = 0
    placed = true
    render()
  }

  /** La pillola, il nome e il salto vanno sulla scheda `key`; `drag`: col dito. */
  function go(key: string | null, drag: boolean) {
    if (key === shown) return
    const from = indexOf(shown), to = indexOf(key)
    shown = key
    for (const [k, s] of per) s.w.t = k === key ? 1 : 0
    if (!placed || !eng) return
    // Da nessuna scheda aperta la pillola nasce dal centro di quella nuova.
    const el = from < 0 ? items()[to] : undefined
    if (el) { L.x = R.x = el.offsetLeft + el.offsetWidth / 2; L.v = R.v = 0 }
    retune(direction(from, to), drag)
    const s = key == null ? undefined : per.get(key)
    if (s && !ridotto()) s.p.v += PUSH
    aim()
    eng.vai()
  }

  const keys = $derived(tabs.map((t) => t.key).join('\n'))
  // Le molle per le schede che ci sono, e un motore nuovo quando cambiano.
  $effect(() => {
    void keys
    untrack(() => {
      const live = new Set(tabs.map((t) => t.key))
      for (const k of [...per.keys()]) if (!live.has(k)) per.delete(k)
      for (const t of tabs) {
        if (per.has(t.key)) continue
        const open = t.key === shown ? 1 : 0
        per.set(t.key, { w: molla(open, NAME.k, damping(NAME, ridotto())), p: molla(1, POP.k, damping(POP, ridotto())) })
      }
      eng = motore(all(), render)
      if (!placed) place()
      else { retune(0, false); aim(); eng.vai() }
    })
    return () => eng?.stop()
  })

  $effect(() => {
    const a = active
    untrack(() => { if (preview == null) go(a, false) })
  })

  // Un numero che cambia, il carattere che arriva, la colonna che si allarga: a molle ferme la pillola si rimette sulla scheda.
  $effect(() => {
    void keys
    const r = root
    if (!r || typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver(() => { if (placed && all().every(ferma)) render() })
    ro.observe(r)
    for (const el of items()) ro.observe(el)
    return () => ro.disconnect()
  })

  // Il dito: oltre DRAG px di corsa la pillola segue la scheda sotto il dito con le molle «col dito»; la scheda si sceglie al rilascio.
  let pid: number | null = null
  let x0 = 0
  let lastX = 0
  let live = false
  let suppress = false
  function down(e: PointerEvent) {
    if (!e.isPrimary || e.button !== 0) return
    pid = e.pointerId
    x0 = e.clientX
    live = false
  }
  function move(e: PointerEvent) {
    if (e.pointerId !== pid || !root) return
    if (!live) {
      if (Math.abs(e.clientX - x0) <= DRAG) return
      live = true
      lastX = e.clientX
      try { root.setPointerCapture(e.pointerId) } catch { /* il puntatore è già andato */ }
      root.classList.add('dragging')
    }
    const dx = e.clientX - lastX
    lastX = e.clientX
    const els = items()
    const spans = els.map(spanOf)
    // Il punto nella riga com'è nel layout, senza lo spostamento dell'elastico.
    const x = e.clientX - (root.getBoundingClientRect().left - N.x)
    extra = stretch(x, spans)
    N.t = extra * FOLLOW
    const j = under(spans, x, els.findIndex((el) => el.dataset.k === shown))
    const k = j >= 0 ? (els[j].dataset.k ?? null) : null
    if (k != null && k !== shown) { preview = k; go(k, true) }
    if (Math.abs(dx) > 0.5) retune(dx > 0 ? 1 : -1, true)
    aim()
    eng?.vai()
  }
  function end(e: PointerEvent, commit: boolean) {
    if (e.pointerId !== pid) return
    pid = null
    if (!live) return
    live = false
    root?.classList.remove('dragging')
    // Il clic che segue il rilascio non è un tocco.
    suppress = true
    setTimeout(() => { suppress = false }, 0)
    const back = settle(extra)
    extra = 0
    N.t = 0
    const k = preview
    preview = null
    if (commit && k != null && k !== active) onPick(k)
    // Se chi chiama non l'ha presa, la pillola torna sulla scheda aperta; se no si posa da dov'è.
    if (shown !== active) go(active, false)
    else retune(back, false)
    aim()
    eng?.vai()
  }
  function tap(key: string) {
    if (!suppress) onPick(key)
  }
  /** Le frecce dentro la riga: la vicina si apre e prende il fuoco; Inizio e Fine le estremità. */
  function listKey(e: KeyboardEvent) {
    if (e.altKey || e.ctrlKey || e.metaKey || e.shiftKey) return
    const cur = indexOf(active)
    const j = keyMove(e.key, cur, tabs.length)
    if (j == null) return
    e.preventDefault()
    if (j !== cur) onPick(tabs[j].key)
    items()[j]?.focus()
  }
  /** Alt+1…5 ed Esc col fuoco dentro `keyScope`, o in tutta la pagina se nessuno li ha già presi; mai da un dialogo aperto. */
  function pageKey(e: KeyboardEvent) {
    if (e.defaultPrevented || e.isComposing || e.ctrlKey || e.metaKey) return
    if (e.target instanceof Element && e.target.closest('dialog[open]')) return
    if (e.key === 'Escape') {
      if (!onEsc || e.altKey || e.shiftKey) return
      e.preventDefault()
      onEsc()
      return
    }
    if (!e.altKey || e.repeat) return
    const m = /^Digit(\d)$/.exec(e.code)
    const i = m ? altDigit(Number(m[1]), tabs.length) : null
    if (i == null) return
    e.preventDefault()
    onPick(tabs[i].key)
  }

  $effect(() => {
    const r = root
    if (!r) return
    const up = (e: PointerEvent) => end(e, true)
    const cancel = (e: PointerEvent) => end(e, false)
    r.addEventListener('pointerdown', down)
    r.addEventListener('pointermove', move)
    r.addEventListener('pointerup', up)
    r.addEventListener('pointercancel', cancel)
    r.addEventListener('keydown', listKey)
    return () => {
      r.removeEventListener('pointerdown', down)
      r.removeEventListener('pointermove', move)
      r.removeEventListener('pointerup', up)
      r.removeEventListener('pointercancel', cancel)
      r.removeEventListener('keydown', listKey)
    }
  })
  $effect(() => {
    const t: HTMLElement | Window = keyScope ?? window
    const on = (e: Event) => pageKey(e as KeyboardEvent)
    t.addEventListener('keydown', on)
    return () => t.removeEventListener('keydown', on)
  })
</script>

<div class="ir" role="tablist" aria-label={label} bind:this={root} data-testid="riga-icone">
  <span class="hl" bind:this={hl} aria-hidden="true"></span>
  {#each tabs as tab, i (tab.key)}
    {@const on = tab.key === active}
    {@const b = badge(tab.count ?? 0, tab.tone ?? 'count')}
    <button class="it" type="button" role="tab" aria-selected={on} tabindex={on || (i === 0 && indexOf(active) < 0) ? 0 : -1}
      aria-label={spoken(tab.label, tab.count ?? 0)} data-k={tab.key} data-testid="riga-icone-{tab.key}" onclick={() => tap(tab.key)}>
      <span class="ic">{#if tab.icon.startsWith('svg:')}<CatIcon name={tab.icon.slice(4)} />{:else}<span class="ms" aria-hidden="true">{tab.icon}</span>{/if}</span>
      <span class="nm"><span class="nmi">{tab.label}</span></span>
      {#if b}<span class="n {b.tone}">{b.text}</span>{/if}
    </button>
  {/each}
</div>

<style>
  /* Le misure e i colori v3 del mockup della master (riga-elastica.html): riga alta 50 col bordo di 1, schede da 40, pillola un gradino sopra. */
  .ir { position: relative; display: inline-flex; align-items: center; gap: 2px; padding: 5px; border-radius: 25px; background: var(--s2);
    box-shadow: inset 0 0 0 1px var(--line); max-width: 100%; user-select: none; -webkit-user-select: none; -webkit-tap-highlight-color: transparent;
    touch-action: pan-y; }
  .ir:global(.dragging) { cursor: grabbing; }
  .hl { position: absolute; left: 0; top: 0; width: 0; height: 40px; border-radius: 999px; background: var(--s3);
    box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--tx) 12%, transparent); will-change: transform, width, height; pointer-events: none; }
  .it { position: relative; z-index: 1; display: flex; align-items: center; flex: none; height: 40px; padding: 0 11px; border-radius: 999px;
    color: var(--tx2); white-space: nowrap; transition: color .25s; }
  .it:global(.on) { color: var(--tx); }
  .it:focus-visible { outline: 2px solid var(--tx2); outline-offset: -2px; }
  .ic { display: flex; flex: none; will-change: transform; }
  .ic .ms { font-size: 22px; }
  .nm { display: block; box-sizing: border-box; overflow: hidden; white-space: nowrap; width: 0; opacity: 0; font: 600 15px/40px var(--display); letter-spacing: .005em; }
  .nmi { display: inline-block; }
  .n { margin-left: 5px; font: 500 12.5px/1 var(--mono); font-variant-numeric: tabular-nums; color: var(--tx2); }
  .n.wait { color: var(--wait); }
  .n.new { color: var(--good); }
  @media (max-width: 400px) {
    .it { padding: 0 7px; }
    .ic .ms { font-size: 20px; }
    .ic :global(svg) { width: 20px; height: 20px; }
  }
  @media (prefers-reduced-motion: reduce) { .it { transition: none; } }
</style>
