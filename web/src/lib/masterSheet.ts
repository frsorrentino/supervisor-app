// Il foglio della master (app-posizioni-sistema-27-11 gruppo 4; proposta B «Foglio» in dalla-master/posizioni-2026-11-27, mockup
// m/b-foglio.html): sale dal basso con tre misure, riga (la riga della master con il suo stato e ▶, e il campo: circa 170 px con la
// maniglia), metà e intera, uguali a ogni larghezza. Trascinato dalla maniglia o dalla sua riga segue il dito, oltre la riga e oltre
// l'intera con l'elastico; al rilascio la velocità si proietta come nei fogli di iOS (decelerazione 0,998) e il foglio scatta alla
// misura più vicina al punto proiettato, con la molla senza rimbalzo della master (MasterGestures: rigidità 300, smorzamento
// critico, gesture.landSpring). Il tocco sulla riga passa alla misura dopo (riga → metà → intera → riga, come il mockup); Esc, e ↓ a
// campo vuoto o senza fuoco, riportano alla riga. Le stesse regole in Kotlin (rules/MasterSheet.kt). Le misure sono altezze dal
// fondo del posto che lo contiene, in px CSS (sul telefono in dp); le velocità in px al secondo, positive verso l'alto.

export type Stop = 'row' | 'half' | 'full'
export const STOPS: readonly Stop[] = ['row', 'half', 'full']

/** La riga prima di misurarla: maniglia, la riga della master e il campo. */
export const ROW_PX = 170
/**
 * app-foglio-master-testata-28-11 (Franz 28/11 14:37, «tra la master e l'header supervisor c'è un po' di spazio che non dovrebbe esserci»):
 * all'intera il foglio arriva alla barra dell'app, senza stacco; prima restavano 8 px e sotto si vedeva una riga della home.
 */
export const GAP_PX = 0
/** Il raggio degli angoli in alto del foglio. */
export const CORNER = 22
/**
 * Metà: il foglio alto il 62 % del posto. Era il 58 % del mockup approvato (m/b-foglio.html, `meta: H * .42`); con la barra di stato
 * (app-barra-stato-28-11) e la maniglia nella sua fascia (app-foglio-master-testata-28-11) la testata aperta è cresciuta di 39 px, e al
 * 62 % alla conversazione della master tornano circa 200 px a 360 × 780, fra la barra, i chip delle Azioni e il campo.
 */
export const HALF_SHARE = 0.62
/** La decelerazione di iOS per millisecondo (UIScrollView.DecelerationRate.normal). */
export const RATE = 0.998
/** L'elastico oltre la riga e oltre l'intera: la costante di UIScrollView. */
export const RUBBER = 0.55
/** La molla della master, senza rimbalzo: rigidità 300 (MasterGestures.SIDE_SPRING_STIFFNESS), massa 1. */
export const STIFFNESS = 300
/** La preferenza unica: l'ultima misura del foglio (prima erano sei, cm.master_*). */
export const PREF = 'cm.master_sheet'

/** Le altezze delle tre misure. */
export type Anchors = Record<Stop, number>

/**
 * Le tre altezze dentro `avail` (l'altezza del posto, sotto la barra in alto): la riga è `row`, misurata; l'intera tutto meno lo
 * stacco; la metà il 62 % del posto (HALF_SHARE), mai sotto la riga né sopra l'intera. In un posto troppo basso coincidono con la riga.
 */
export function anchors(avail: number, row: number, gap = GAP_PX): Anchors {
  const r = Math.max(0, row)
  const full = Math.max(r, avail - gap)
  const half = Math.min(full, Math.max(r, avail * HALF_SHARE))
  return { row: r, half, full }
}

/** Dove arriverebbe il foglio rallentando come iOS: posizione + (v / 1000) · 0,998 / (1 − 0,998), cioè v · 0,499 s. */
export const project = (pos: number, v: number, rate = RATE): number => pos + ((v / 1000) * rate) / (1 - rate)

/** La misura più vicina a un'altezza; a pari distanza la più bassa. */
export function nearest(a: Anchors, h: number): Stop {
  let best: Stop = 'row'
  for (const s of STOPS) if (Math.abs(a[s] - h) < Math.abs(a[best] - h)) best = s
  return best
}

/** Al rilascio: la misura più vicina al punto proiettato. */
export const snap = (a: Anchors, h: number, v: number): Stop => nearest(a, project(h, v))

/** L'elastico di iOS: `over` px oltre il bordo diventano meno, mai più di `dim`. */
export const band = (over: number, dim: number, k = RUBBER): number => (over <= 0 || dim <= 0 ? 0 : (1 - 1 / ((over * k) / dim + 1)) * dim)

/** L'altezza col dito: libera fra `lo` e `hi`, oltre con l'elastico (`dim`: l'altezza del posto). */
export function rubber(h: number, lo: number, hi: number, dim: number, k = RUBBER): number {
  if (h < lo) return lo - band(lo - h, dim, k)
  if (h > hi) return hi + band(h - hi, dim, k)
  return h
}

/** Il tocco sulla riga: alla misura dopo, e dall'intera di nuovo alla riga. */
export const tap = (s: Stop): Stop => (s === 'row' ? 'half' : s === 'half' ? 'full' : 'row')

/** ↑ sulla riga del foglio: una misura più su; dall'intera resta lì. */
export const up = (s: Stop): Stop => (s === 'row' ? 'half' : 'full')

/** La preferenza letta: una misura nota, altrimenti la riga. */
export const readStop = (raw: string | null | undefined): Stop => (raw === 'half' || raw === 'full' ? raw : 'row')

// app-posizioni-sistema-27-11, prova 1447 (Franz: «sembra quasi che si sta alzando solo l'header»; «una sfumatura verso il nero tra il
// bordo della master superiore e la home e qualche animazione di movimento che fa comprendere bene l'azione di sovrapposizione»): il
// foglio è un blocco solo, testata e conversazione insieme; la home dietro si vela e arretra mentre lui sale, e sopra il bordo c'è un'ombra.

/**
 * app-foglio-master-testata-28-11: gli angoli in alto, tondi (CORNER), si squadrano negli ultimi CORNER px prima dell'intera: lì il foglio
 * tocca la barra dell'app e dietro un angolo tondo si vedrebbe la home.
 */
export const corner = (a: Anchors, h: number): number => Math.min(CORNER, Math.max(0, a.full - h))

/** Quanto è salito il foglio: 0 alla riga, 1 all'intera. Guida il velo, l'arretramento della home e l'ombra sopra il bordo. */
export function progress(a: Anchors, h: number): number {
  if (a.full <= a.row) return 0
  return Math.min(1, Math.max(0, (h - a.row) / (a.full - a.row)))
}

/** Il velo nero sulla home all'intera: 40 %. */
export const DIM_MAX = 0.4
/** La home arretra fino a 0,96 all'intera, dal centro in alto. */
export const RECEDE = 0.04
/** L'ombra sopra il bordo del foglio: nero che sfuma verso l'alto, dal 30 % alla riga al 55 % all'intera, alta SHADE_PX. */
export const SHADE_MIN = 0.3
export const SHADE_MAX = 0.55
export const SHADE_PX = 40

export type Cues = { dim: number; scale: number; shade: number }

/**
 * Il velo sulla home, la sua scala e l'ombra sopra il bordo per l'avanzamento `p`. Con il movimento ridotto la home non arretra: restano
 * il velo e l'ombra, che non si muovono.
 */
export function cues(p: number, reduced = false): Cues {
  const q = Math.min(1, Math.max(0, p))
  return { dim: DIM_MAX * q, scale: reduced ? 1 : 1 - RECEDE * q, shade: SHADE_MIN + (SHADE_MAX - SHADE_MIN) * q }
}

/**
 * La conversazione rigida (prova 1447): quanta tenerne ferma (px) all'inizio di un movimento, col foglio alto `h`, partito da `from`,
 * verso `to` (null quando lo tiene il dito). Dalla riga, dove non se ne vede niente, quanta se ne vedrà all'arrivo (col dito, alla misura
 * sopra): sale tutta insieme alla testata e il campo la copre finché non arriva. Altrimenti quanta se ne vede adesso: scendendo resta
 * rigida e passa sotto il campo; salendo si allunga col foglio e l'ultimo messaggio resta sopra il campo, perché sotto l'ultimo non c'è
 * altro da mostrare (alzarla tutta subito farebbe saltare quello che si vede). Da fermo torna alta quanto si vede.
 */
export function freezeAt(a: Anchors, h: number, from: Stop, to: Stop | null): number {
  const vis = (x: number) => Math.max(0, x - a.row)
  return h <= a.row + 0.5 ? vis(a[to ?? up(from)]) : vis(h)
}

/** Esc riporta sempre alla riga; ↓ solo a campo vuoto o col fuoco fuori dal campo. */
export const toRow = (key: string, inField: boolean, fieldEmpty: boolean): boolean => key === 'Escape' || (key === 'ArrowDown' && (!inField || fieldEmpty))

/**
 * La molla d'arrivo senza rimbalzo (MasterGestures.land, gesture.landSpring): x(t) = (x0 + (v0 + ω·x0)·t)·e^(−ωt), x0 la distanza
 * dall'arrivo. Torna la distanza e la velocità al tempo `t` (s).
 */
export function settleAt(from: number, v: number, w: number, t: number): { x: number; v: number } {
  const e = Math.exp(-w * t), b = v + w * from
  return { x: (from + b * t) * e, v: (b - w * (from + b * t)) * e }
}
