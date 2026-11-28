// La riga di icone (app-posizioni-sistema-27-11, gruppo 1; proposta in dalla-master/posizioni-2026-11-27, «La riga di icone») con la
// pillola elastica del Rubber dock (app-dock-elastico-27-11, scelta di Franz del 27/11 20:46; mockup riga-elastica.html e il codice
// originale fonte-designbookmark/dock.js). Porta di IconRowRules.kt: il numero, le molle, la meta e la forma della pillola, l'elastico
// oltre le estremità, la scheda sotto il dito, i tasti. Le misure sono in px CSS (sul telefono in dp).

/** Il colore del numero, come nel resto dell'app: ambra se qualcosa aspetta Franz, verde un risultato nuovo, grigio un conteggio. */
export type Tone = 'wait' | 'new' | 'count'

/** Una scheda della riga: icona e numero sempre, il nome solo sulla scheda aperta. `icon`: un nome di Material Symbols o `svg:<nome>` di CatIcon. */
export type IconTab = { key: string; icon: string; label: string; count?: number; tone?: Tone }

/** A zero il numero sparisce e l'icona resta al suo posto. */
export const badge = (count: number, tone: Tone): { text: string; tone: Tone } | null => (count > 0 ? { text: String(count), tone } : null)

/** Il nome della scheda per i lettori di schermo: il nome e, se c'è, il numero. */
export const spoken = (label: string, count: number): string => (count > 0 ? `${label} ${count}` : label)

/** Una molla con massa 1, a = -k(x - t) - c·v, come shared/motion.js di designbookmark. */
export type Spring = { k: number; c: number }
/** Lo smorzamento relativo di Compose, c / (2·√k); con «riduci movimento» critico: niente rimbalzo. */
export const ratio = (s: Spring, reduced = false): number => (reduced ? 1 : s.c / (2 * Math.sqrt(s.k)))
/** Lo smorzamento per il motore di molla.ts; con «riduci movimento» critico, 2·√k, come `tune` di motion.js. */
export const damping = (s: Spring, reduced = false): number => (reduced ? 2 * Math.sqrt(s.k) : s.c)

/** Il nome della scheda scelta che si apre e quello della lasciata che si chiude: 300/26 di bar.js (proposta), 0,75. */
export const NAME: Spring = { k: 300, c: 26 }
/** Il bordo della pillola che guida il passo: --dock-lead di tokens.css, 640/38 (0,751). Prende il posto di X/W 340/28 del piano. */
export const LEAD: Spring = { k: 640, c: 38 }
/** Il bordo che resta indietro e tira la pillola: --dock-trail, 150/16 (0,653). */
export const TRAIL: Spring = { k: 150, c: 16 }
/** Col dito: --dock-drag-lead 1100/56 (0,844) e --dock-drag-trail 320/26 (0,727). */
export const DRAG_LEAD: Spring = { k: 1100, c: 56 }
export const DRAG_TRAIL: Spring = { k: 320, c: 26 }
/** Senza direzione (al rilascio dentro la riga, o alla prima scheda): i due bordi 600/44 (0,898), moveTo di dock.js. */
export const SETTLE: Spring = { k: 600, c: 44 }
/** Il salto dell'icona scelta: --dock-pop 520/16 (0,351), con la spinta PUSH sulla velocità come setActive di dock.js. */
export const POP: Spring = { k: 520, c: 16 }
export const PUSH = -3.2
/** La riga che segue la pillola tirata oltre le estremità: nudge di dock.js, 300/18, al FOLLOW dell'allungamento. */
export const ROW: Spring = { k: 300, c: 18 }
export const FOLLOW = 0.35

/** L'altezza della pillola ferma e la larghezza minima (dock.js). */
export const PILL_H = 40
export const PILL_MIN_W = 24
/** La pillola tirata si assottiglia ad area costante, fra questi due rapporti dell'altezza (dock.js). */
export const PILL_THIN = 0.66
export const PILL_THICK = 1.1
/** L'elastico oltre la prima e l'ultima scheda: la funzione rubber di dock.js, d = 46 e 0,55. */
export const BAND = 46
export const BAND_K = 0.55
/** Col dito la pillola resta sulla scheda di adesso finché il dito non ne esce di più di 6 (riga-elastica.html). */
export const SLACK = 6
/** Il dito diventa un trascinamento oltre 4 px di corsa (dock.js). */
export const DRAG = 4
/** Lo spazio fra l'icona e il nome aperto. */
export const NAME_GAP = 8
/** Alt+1…5 sceglie la scheda (proposta, «Tastiera fisica»). */
export const MAX_KEYS = 5

const clamp = (v: number, a: number, b: number): number => Math.max(a, Math.min(b, v))

/** La direzione del passo da una scheda all'altra: 1 verso destra, -1 verso sinistra, 0 se una delle due manca o sono la stessa. */
export const direction = (from: number, to: number): -1 | 0 | 1 => (from < 0 || to < 0 ? 0 : to > from ? 1 : to < from ? -1 : 0)

/**
 * Le molle dei due bordi, [sinistro, destro], per un passo nella direzione `dir`: davanti quello verso cui si va, rigido; dietro l'altro,
 * pigro, e la differenza allunga la pillola. Senza direzione tutti e due SETTLE. `drag`: col dito.
 */
export function edges(dir: number, drag: boolean): [Spring, Spring] {
  const lead = drag ? DRAG_LEAD : LEAD
  const trail = drag ? DRAG_TRAIL : TRAIL
  return dir > 0 ? [trail, lead] : dir < 0 ? [lead, trail] : [SETTLE, SETTLE]
}

/** Un tratto della riga: il bordo sinistro e il destro di una scheda, misurati nel layout della riga. */
export type Span = { left: number; right: number }

/** Dove vanno i due bordi della pillola: quelli della scheda scelta, spostati di `extra` dall'elastico oltre le estremità. */
export const target = (span: Span | null | undefined, extra = 0): Span | null => (span ? { left: span.left + extra, right: span.right + extra } : null)

/** La pillola disegnata: da `x` larga `width`, alta `height`, scesa di `top` dentro l'altezza ferma. */
export type Pill = { x: number; width: number; height: number; top: number }

/**
 * La forma della pillola, il punto solo da cui la disegnano telefono e web: i due bordi come stanno adesso (`left`, `right`), la meta
 * `goal`. Larga quanto i bordi (almeno `minWidth`); tirata più della meta si assottiglia ad area costante, stretta si gonfia:
 * altezza = `height` · clamp(1/√(larghezza / larghezza della meta), 0,66, 1,1), sempre centrata.
 */
export function pill(left: number, right: number, goal: Span, height = PILL_H, minWidth = PILL_MIN_W): Pill {
  const a = Math.min(left, right)
  const b = Math.max(left, right)
  const w = Math.max(b - a, minWidth)
  const rest = Math.max(goal.right - goal.left, minWidth)
  const h = height * clamp(1 / Math.sqrt(w / rest), PILL_THIN, PILL_THICK)
  return { x: a, width: w, height: h, top: (height - h) / 2 }
}

/** L'elastico di dock.js: dentro lo..hi il punto resta dov'è, fuori resiste sempre di più e non supera mai `d`. */
export const rubber = (x: number, lo: number, hi: number, d = BAND): number =>
  x < lo ? lo - (1 - 1 / ((lo - x) * BAND_K / d + 1)) * d : x > hi ? hi + (1 - 1 / ((x - hi) * BAND_K / d + 1)) * d : x

/**
 * Quanto la pillola va oltre le estremità col dito in `x`: l'elastico fra il centro della prima scheda e quello dell'ultima, meno la
 * parte dentro. 0 dentro la riga; la riga intera segue al FOLLOW di questo.
 */
export function stretch(x: number, spans: Span[], d = BAND): number {
  if (!spans.length) return 0
  const first = spans[0], last = spans[spans.length - 1]
  const a = (first.left + first.right) / 2, b = (last.left + last.right) / 2
  const lo = Math.min(a, b), hi = Math.max(a, b)
  const c = rubber(x, lo, hi, d)
  return c - clamp(c, lo, hi)
}

/**
 * La scheda sotto il dito in `x` mentre trascina, partendo da `current`: resta quella finché il dito non ne esce di più di `slack`;
 * fuori dalla riga conta la scheda all'estremità; fra due schede resta quella di adesso. -1 senza schede.
 */
export function under(spans: Span[], x: number, current: number, slack = SLACK): number {
  if (!spans.length) return -1
  const lo = spans[0].left
  const xc = clamp(x, lo, Math.max(lo, spans[spans.length - 1].right - 1))
  const on = current >= 0 ? spans[current] : undefined
  if (on && xc >= on.left - slack && xc <= on.right + slack) return current
  const j = spans.findIndex((s) => xc >= s.left && xc < s.right)
  return j >= 0 ? j : on ? current : -1
}

/** Al rilascio la pillola torna dentro: guida il bordo verso la riga (-1 se era oltre la fine, 1 se prima dell'inizio, 0 dentro). */
export const settle = (extra: number): -1 | 0 | 1 => (extra > 0 ? -1 : extra < 0 ? 1 : 0)

/** Il nome che si apre, con `f` da 0 (chiuso) a 1 (aperto): compare solo dopo poco meno di un terzo della corsa (riga-elastica.html). */
export const nameAlpha = (f: number): number => clamp(f * 1.4 - 0.4, 0, 1)
/** Quanto dello spazio NAME_GAP fra icona e nome c'è già, da 0 a 1. */
export const nameGap = (f: number): number => clamp(f, 0, 1)

/** Alt+`digit` su `size` schede: l'indice della scheda, o null se quel tasto non ne sceglie una. */
export const altDigit = (digit: number, size: number): number | null => (digit >= 1 && digit <= Math.min(size, MAX_KEYS) ? digit - 1 : null)

/**
 * Le frecce dentro la riga, come le schede del WAI-ARIA (solo sul web: sul telefono il fuoco passa da una scheda all'altra da solo):
 * → e ← la vicina, senza giro, Inizio e Fine le estremità. null per gli altri tasti.
 */
export function keyMove(key: string, current: number, size: number): number | null {
  if (size <= 0) return null
  if (key === 'ArrowRight') return clamp(current + 1, 0, size - 1)
  if (key === 'ArrowLeft') return clamp(current - 1, 0, size - 1)
  if (key === 'Home') return 0
  if (key === 'End') return size - 1
  return null
}
