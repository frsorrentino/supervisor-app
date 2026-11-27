<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { Head } from './kpi'
  import type { Box } from './HomeBox.svelte'
  import type { State } from './contract'
  import { t } from './t'
  import { rolls } from './animazioni'
  import { lastGestureAt } from './attese.svelte'
  import { reducedMotion } from './motion'
  import { clamp, ferma, molla, motore, type Molla } from './molla'
  // Proposta 7 (25/10): tre cifre in testa alla home. Ognuna apre la sua pagina, anche a zero (app-box-cliccabili-29-10).
  // Lo stile «Studio e Diretta» (terza versione, 30/10): il tabellone, con la quota delle 5 ore sotto le cifre al posto della
  // riga nella testata. Si colora solo chi ti aspetta, l'unico avviso della home; i rossi restano rossi. La barra della quota
  // è neutra e diventa ambra da 75 % e rossa da 90 %; la tacca bianca è quanto è passato della finestra delle 5 ore.
  // quota-senza-cinque-ore: dove il dato manca, per quell'account, la settimana col giorno e il ritmo.
  // Variante B del mockup del 13/11 (app-tabellone-utilizzo-b-13-11, Franz 12:40): il terzo blocco è l'utilizzo, la percentuale
  // grande come le altre cifre, sotto la finestra che dice, e sul bordo in basso la barra con la tacca del tempo passato; il tocco
  // apre e chiude i dettagli sotto. Gli esiti di oggi restano nelle voci della Home, al loro posto (Franz 13/11 15:41 e 15:46).
  // Senza il dato dell'utilizzo il terzo blocco resta «esiti oggi».
  // app-tabellone-filtri-14-11 (Franz 14/11 14:22): «ferme» e «al lavoro» filtrano la lista delle sessioni sotto; la casella del
  // filtro acceso resta accesa, e a 0 non si tocca e non ha la freccia.
  let { h, st = null, now = 0, onBox = () => {}, onQuota = () => {}, slot, usage = null, onUsage = () => {}, filter = null }: { h: Head; filter?: 'aspetta' | 'lavoro' | null; st?: State | null; now?: number; onBox?: (b: Box) => void; onQuota?: () => void; slot?: Snippet
    usage?: { pct: number; tick: number | null; tone: string; label: string; open: boolean; reset?: string | null } | null; onUsage?: () => void } = $props()
  // Voce 4, fedele all'originale (designbookmark.com/motion, Word roll; app-animazioni-fedeli-26-11): il numero di chi ti aspetta
  // cambia lettera per lettera solo se è cambiato per una tua risposta degli ultimi secondi; per un aggiornamento dal PC cambia e basta
  // (regola del 31/10). Le cifre vecchie salgono via a scalare di 22 ms, le nuove entrano da sotto dopo 60 ms a scalare di 28 ms, molla
  // 420 26 (105 % dell'altezza, opacità 1 - 0,9·|y|); la larghezza va alla cifra nuova con la 320 30. Moto ridotto: al posto.
  const GESTURE_MS = 4000
  function rollOnGesture(el: HTMLElement, value: number) {
    type Word = { el: HTMLElement; chs: HTMLElement[]; ys: Molla[] }
    const make = (v: number, y: number): Word => {
      const w = document.createElement('span')
      w.className = 'rw'
      const chs = [...String(v)].map(c => { const x = document.createElement('span'); x.className = 'ch'; x.textContent = c; w.append(x); return x })
      return { el: w, chs, ys: chs.map(() => molla(y, 420, 26)) }
    }
    let last: number | null = value
    let words: Word[] = [make(value, 0)]
    el.replaceChildren(words[0].el)
    const Wd = molla(0, 320, 30)
    const all: Molla[] = [Wd]
    const timers: ReturnType<typeof setTimeout>[] = []
    const render = () => {
      for (const w of words) w.chs.forEach((c, k) => {
        const y = w.ys[k].x
        c.style.transform = `translateY(${(y * 105).toFixed(2)}%)`
        c.style.opacity = clamp(1 - Math.abs(y) * 0.9, 0, 1).toFixed(3)
      })
      if (words.length > 1) el.style.width = Wd.x.toFixed(2) + 'px'
    }
    const m = motore(all, render)
    const settle = () => { words = words.slice(-1); el.replaceChildren(words[0].el); el.style.width = ''; all.length = 1 }
    return {
      update(v: number) {
        const go = rolls(last, v, Date.now() - lastGestureAt() < GESTURE_MS)
        last = v
        timers.forEach(clearTimeout); timers.length = 0
        if (!go || reducedMotion()) { words = [make(v, 0)]; settle(); return }
        const cur = words[words.length - 1]
        const from = el.offsetWidth
        const next = make(v, 1)
        words = [cur, next]
        el.replaceChildren(cur.el, next.el)
        Wd.x = from; Wd.v = 0; Wd.t = next.el.offsetWidth
        all.length = 1; all.push(...cur.ys, ...next.ys)
        cur.ys.forEach((s, k) => timers.push(setTimeout(() => { s.t = -1; m.vai() }, k * 22)))
        next.ys.forEach((s, k) => timers.push(setTimeout(() => { s.t = 0; m.vai() }, 60 + k * 28)))
        timers.push(setTimeout(settle, 1000))
        render()
        m.vai()
      },
      destroy() { timers.forEach(clearTimeout); m.stop() },
    }
  }
  // Voce 7, fedele all'originale (designbookmark.com/motion, Tab pill; app-animazioni-fedeli-26-11): cambiando filtro il filo si
  // allunga verso la casella nuova e poi richiama la coda. Due bordi, due molle: quello davanti 620 40, quello dietro 170 19.
  // Durante il viaggio il filo è un elemento del tabellone; fermo, è quello della casella, con i suoi angoli. Moto ridotto: al posto.
  function slideRing(board: HTMLElement, value: unknown) {
    const tr = board.querySelector<HTMLElement>('.tr')
    const L = molla(0), R = molla(0)
    type Edge = { l: number; r: number; top: number; h: number; c: HTMLElement }
    const edges = (): Edge | null => {
      const c = board.querySelector<HTMLElement>('.cell.on')
      return c ? { l: c.offsetLeft, r: c.offsetLeft + c.offsetWidth, top: c.offsetTop, h: c.offsetHeight, c } : null
    }
    const roundBottom = () => { const sl = board.querySelector('.slot'); return !sl || !sl.textContent?.trim() && !sl.childElementCount }
    const render = () => {
      const e = edges()
      if (!tr || !e) return
      tr.style.left = L.x.toFixed(2) + 'px'
      tr.style.width = Math.max(0, R.x - L.x).toFixed(2) + 'px'
      tr.style.top = e.top + 'px'
      tr.style.height = e.h + 'px'
      const rad = Math.max(0, 13 - L.x)
      tr.style.borderTopLeftRadius = rad + 'px'
      tr.style.borderBottomLeftRadius = (roundBottom() ? rad : 0) + 'px'
      board.classList.toggle('moving', !(ferma(L) && ferma(R)))
    }
    const m = motore([L, R], render)
    let last = edges()
    if (last) { L.x = L.t = last.l; R.x = R.t = last.r }
    void value
    return {
      update() {
        const now = edges()
        if (now && last && now.c !== last.c && !reducedMotion()) {
          const right = now.l > last.l
          L.t = now.l; R.t = now.r
          L.k = right ? 170 : 620; L.c = right ? 19 : 40
          R.k = right ? 620 : 170; R.c = right ? 40 : 19
          m.vai()
        } else if (now) { L.x = L.t = now.l; R.x = R.t = now.r; L.v = R.v = 0 }
        last = now
      },
      destroy() { m.stop() },
    }
  }
  // app-livello-riga-27-11 (Franz 27/11 21:33, «il livello sulla riga… solo il pezzo interno al riquadro»): la linea della settimana va su
  // tutta la riga, da un bordo all'altro del tabellone. Resta nella terza casella ma comincia dal bordo sinistro, alla distanza misurata;
  // riempimento e segno di adesso stanno in proporzione alla larghezza intera.
  function aTuttaRiga(node: HTMLElement) {
    const set = () => node.style.setProperty('--usex', `${node.offsetLeft + node.clientLeft}px`)
    set()
    const ro = new ResizeObserver(set)
    ro.observe(node.parentElement ?? node)
    return { destroy: () => ro.disconnect() }
  }
</script>

<div class="board lit" class:wk={!!usage} data-kpi="kpi-7" use:slideRing={filter}>
  <button class="cell" class:on={filter === 'aspetta'} aria-pressed={filter === 'aspetta'} disabled={!h.waiting} data-testid="home-box-aspetta" onclick={() => onBox('aspetta')}><span class="ring" aria-hidden="true"></span><b class="n" class:wait={h.waiting > 0}><span class="roll" use:rollOnGesture={h.waiting}></span>{#if h.open}<small class="of">/{h.open}</small>{/if}</b><span class="l">{h.waiting ? t.kpi7Waiting(h.waiting, Math.max(1, Math.round((h.oldestWaitS ?? 0) / 60))) : t.kpi7NoneWaiting}</span>{#if h.waiting}<span class="ms chev" aria-hidden="true">chevron_right</span>{/if}</button>
  <button class="cell" class:on={filter === 'lavoro'} aria-pressed={filter === 'lavoro'} disabled={!h.busy} data-testid="home-box-lavoro" onclick={() => onBox('lavoro')}><span class="ring" aria-hidden="true"></span><b class="n">{h.busy}{#if h.open}<small class="of">/{h.open}</small>{/if}</b><span class="l">{t.kpi7Busy}</span>{#if h.busy}<span class="ms chev" aria-hidden="true">chevron_right</span>{/if}</button>
  {#if usage}
    <button class="cell use" data-testid="home-box-utilizzo" aria-expanded={usage.open} onclick={onUsage} use:aTuttaRiga><b class="n" style:color={usage.tone === 'var(--tx2)' ? null : usage.tone}>{usage.pct}<sup>%</sup></b><span class="l" class:one={!!usage.reset}>{usage.label}</span>{#if usage.reset}<span class="rs" data-testid="home-box-utilizzo-azzera">{usage.reset}</span>{/if}<span class="ms chev" aria-hidden="true">{usage.open ? 'expand_less' : 'expand_more'}</span>
      <span class="edge" style:color={usage.tone}><i style="width:{Math.min(100, usage.pct)}%"></i>{#if usage.tick != null}<u style="left:{usage.tick}%"></u>{/if}</span></button>
  {:else}
  <button class="cell" data-testid="home-box-esiti" onclick={() => onBox('esiti')}><b class="n">{h.outcomes ?? '–'}</b><span class="l">{#if h.outcomes == null}{t.kpi7OutcomesUnknown}{:else if h.red}{t.kpi7Outcomes}, <em>{t.kpi7Red(h.red)}</em>{:else}{t.kpi7Outcomes}{/if}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  {/if}
  <!-- La barra fissa delle 5 ore non c'è più (app-utilizzo-una-barra-03-11): le finestre d'uso stanno nella riga Utilizzo, e una vicina al
       limite entra da sola nel tabellone. -->
  {#if slot}<div class="slot">{@render slot()}</div>{/if}
  <span class="tr" aria-hidden="true"></span>
</div>

<style>
  .slot { grid-column: 1 / -1; min-width: 0; }
  .slot:empty { display: none; }
  .board { position: relative; flex: none; display: grid; grid-template-columns: 1.15fr 1fr 1.15fr; margin: 4px 0 2px; background: var(--s1); border: 1px solid var(--line); border-radius: 14px; overflow: hidden; }
  /* In alto, non al centro come fa un button: i tre numeri sulla stessa linea; l'etichetta ha sempre due righe (Franz 03/11 09:38,
     app-tabellone-numeri-03-11). */
  .cell { position: relative; display: flex; flex-direction: column; justify-content: flex-start; padding: 12px 12px 13px; border-left: 1px solid var(--line); text-align: left; color: var(--tx); cursor: pointer; min-width: 0; }
  .cell:first-child { border-left: 0; }
  .cell:hover { background: var(--s2); }
  /* A 0 la casella non si tocca; accesa, il filtro si vede dal fondo e dal filo sul bordo. */
  .cell:disabled { cursor: default; }
  .cell:disabled:hover { background: none; }
  .cell.on { background: var(--s2); }
  /* L'anello è un elemento suo, così può viaggiare da una casella all'altra (voce 7); prende gli angoli della casella. */
  .ring { position: absolute; inset: 0; z-index: 1; border-radius: inherit; box-shadow: inset 0 0 0 2px var(--tx2); pointer-events: none; opacity: 0; transition: opacity 150ms ease-out; }
  .cell.on .ring { opacity: 1; }
  .tr { position: absolute; z-index: 2; box-shadow: inset 0 0 0 2px var(--tx2); pointer-events: none; opacity: 0; }
  .board:global(.moving) .tr { opacity: 1; }
  .board:global(.moving) .cell.on .ring { opacity: 0; transition: none; }
  /* app-tabellone-filtro-25-11: il filo segue gli angoli smussati del tabellone, che prima ne tagliava gli spigoli. */
  .cell.on:first-child { border-top-left-radius: 13px; }
  .board:has(.slot:empty) .cell.on:first-child, .board:not(:has(.slot)) .cell.on:first-child { border-bottom-left-radius: 13px; }
  .cell.on .chev { color: var(--tx); transform: rotate(90deg); }
  .cell:focus-visible { outline: 2px solid var(--tx); outline-offset: -2px; }
  .n { display: block; font: 700 42px/1 var(--display); letter-spacing: -0.01em; }
  .roll { position: relative; display: inline-block; clip-path: inset(0 -0.5em); }
  .roll :global(.rw) { display: inline-block; white-space: nowrap; }
  .roll :global(.rw + .rw) { position: absolute; left: 0; top: 0; }
  .roll :global(.ch) { display: inline-block; will-change: transform, opacity; }
  .n.wait { color: var(--wait); }
  /* La frazione sulle sessioni aperte (Franz 13/11 13:56: «al lavoro 1/5 e ferme 3/5»): più piccola e neutra, accanto alla cifra. */
  .n .of { font-size: 22px; font-weight: 600; letter-spacing: 0; color: var(--tx2); margin-left: 2px; }
  .l { display: block; min-height: 2.5em; font-size: 14px; line-height: 1.25; color: var(--tx2); margin-top: 6px; overflow-wrap: anywhere; }
  .l em { font-style: normal; font-weight: 500; color: var(--red); }
  .chev { position: absolute; top: 12px; right: 8px; color: var(--tx3); }
  /* Il blocco dell'utilizzo: il segno % più piccolo, la barra sul bordo in basso con la tacca del tempo passato. */
  .n sup { font-size: 22px; font-weight: 600; vertical-align: 14px; margin-left: 2px; color: var(--tx2); }
  .cell.use[aria-expanded='true'] { background: var(--s2); }
  /* app-quota-azzeramento-17-11: l'ora di azzeramento sotto la percentuale, piccola, sopra la barra. */
  /* app-casella-settimana-27-11 (Franz 27/11 16:41, scelta A): la casella dell'utilizzo è alta quanto le altre due. La terza colonna è un
     po' più larga e quella in mezzo un po' più stretta (la prima resta com'era: «nessuna sessione ferma» su due righe), così a 360 px
     «della settimana» e «si azzera mer 23:30» stanno su una riga ciascuna; le due righe insieme occupano le
     due della didascalia delle altre; la barra col segno di adesso è una linea di 3 px sul bordo basso, dentro il raggio del tabellone. */
  .board.wk { grid-template-columns: 1.15fr 0.92fr 1.23fr; }
  .cell.use { padding-right: 8px; }
  .l.one { min-height: 0; }
  .rs { display: block; font-size: 12px; line-height: 1.25; color: var(--tx3); margin: 0; overflow-wrap: anywhere; }
  .edge { position: absolute; left: calc(-1 * var(--usex, 0px)); right: 0; bottom: 0; height: 3px; pointer-events: none; background: color-mix(in srgb, var(--tx) 14%, transparent); }
  .edge i { position: absolute; left: 0; top: 0; bottom: 0; background: currentColor; }
  .edge u { position: absolute; top: -3px; bottom: 0; width: 2px; border-radius: 1px; background: var(--tx); }
</style>
