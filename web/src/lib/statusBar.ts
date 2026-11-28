// app-barra-stato-28-11 (Franz 28/11 14:07, variante C di dalla-master/barra-stato-28-11.html): una sola barra di stato, alta 28 con la
// riga sottile sotto, subito sotto il titolo delle sessioni e del foglio aperto della master, al posto della riga di icone. A sinistra
// Per te, Risultati e Modifiche col loro numero (a zero solo l'icona), che fanno scendere la loro tendina; a destra modello · effort, la
// quota delle 5 ore come batteria e il contesto come anellino, che aprono Dettagli. Qui le regole che non sono disegno, uguali sul
// telefono (StatusBar.kt): quanto resta della quota, i colori, e quanto si stringe la barra con poco spazio («ok c ma quando c'è poco
// spazio deve ridursi anche il testo», «o comunque deve disporsi meglio»): il testo non va mai a capo, non si taglia e non esce dai glifi.
import { CHIP_FROM } from './sessionBottom'

/**
 * I passi, dal più largo: tutto («Opus 5 · high», batteria e anellino con la loro percentuale accanto); il testo più piccolo; il modello
 * senza effort; le percentuali dentro i glifi; solo i glifi. I numeri delle icone di sinistra restano a ogni passo.
 */
export type BarStep = 'full' | 'small' | 'noEffort' | 'inside' | 'glyphs'
export const STEPS: readonly BarStep[] = ['full', 'small', 'noEffort', 'inside', 'glyphs']

/** Geist Mono: ogni carattere è largo 0,6 della sua altezza (misurato nel browser, uguale a 400 e a 500). */
export const ADV = 0.6
/** L'altezza della barra; sotto, la riga sottile da 1. */
export const BAR_H = 28
/**
 * Il margine a sinistra e a destra, e lo spazio minimo fra le icone e lo stato. Nel foglio della master i margini sono 16, come le sue
 * schede (app-foglio-master-testata-28-11: «il padding esiguo tra testo e bordo»).
 */
export const PAD = 12
export const PAD_SHEET = 16
export const SIDES_GAP = 12
/** Le icone di sinistra (17), lo spazio fra un'icona e il suo numero (3) e fra due icone (10); il numero, sempre a 11,5. */
export const ICON = 17
export const NUM_GAP = 3
export const ICON_GAP = 10
export const NUM = 11.5
/** Il testo di destra: 10,5, e 9 dal passo «small». */
export const TEXT = 10.5
export const TEXT_SMALL = 9
/** Fra modello, batteria e anellino (8); fra un glifo e la sua percentuale (3). */
export const ITEM_GAP = 8
export const GLYPH_GAP = 3
/** La batteria, 19 più la punta da 3,5, e l'anellino da 12; col numero dentro la batteria è larga 24 più la punta, l'anellino 18. */
export const BAT = 22.5
export const RING = 12
export const BAT_IN = 27.5
export const RING_IN = 18
/** Il numero dentro i glifi: 7,5, fisso come i glifi, così non ne esce mai, anche col testo grande. */
export const IN_TEXT = 7.5
/** Un margine per gli arrotondamenti del disegno: meglio un passo prima che un testo che tocca il bordo. */
export const SAFE = 6

/** Quanto resta della quota delle 5 ore, come la batteria del telefono: 100 meno l'usato, fra 0 e 100. */
export const remaining = (used: number): number => Math.min(100, Math.max(0, 100 - Math.round(used)))

export type BarTone = 'neutral' | 'warn' | 'alert'
/** La batteria diventa ambra quando resta il 30 % o meno, rossa col 10 % o meno. */
export const BAT_WARN = 30
export const BAT_ALERT = 10
export const batteryTone = (left: number): BarTone => (left <= BAT_ALERT ? 'alert' : left <= BAT_WARN ? 'warn' : 'neutral')
/** L'anellino del contesto è ambra dal 70 %, come il chip «Contesto N% · Handoff» (sessionBottom.CHIP_FROM). */
export const ringTone = (ctx: number): BarTone => (ctx >= CHIP_FROM ? 'warn' : 'neutral')

/** Quello che la barra scrive a destra: modello · effort ai primi due passi, poi il modello solo; coi soli glifi niente. */
export function modelText(model: string | null, effort: string | null, step: BarStep): string {
  if (step === 'glyphs') return ''
  if (step === 'full' || step === 'small') return [model, effort].filter(Boolean).join(' · ')
  return model ?? ''
}

export type BarParts = {
  /** I numeri di Per te, Risultati e Modifiche, nell'ordine; a zero solo l'icona. */
  counts: readonly number[]
  model: string | null
  effort: string | null
  /** Quanto resta della quota delle 5 ore (`remaining`); null senza quota. */
  left: number | null
  /** Il contesto in %; null senza. */
  ctx: number | null
}

const mono = (chars: number, size: number, scale: number): number => chars * ADV * size * scale
const pct = (n: number): string => `${Math.round(n)}%`

/** La larghezza delle icone di sinistra coi loro numeri. */
export function leftWidth(counts: readonly number[], scale = 1): number {
  return counts.reduce((w, n, i) => w + (i > 0 ? ICON_GAP : 0) + ICON + (n > 0 ? NUM_GAP + mono(String(Math.floor(n)).length, NUM, scale) : 0), 0)
}

/** La larghezza dello stato di destra al passo `step`. */
export function rightWidth(step: BarStep, p: BarParts, scale = 1): number {
  const size = step === 'full' ? TEXT : TEXT_SMALL
  const w: number[] = []
  const m = modelText(p.model, p.effort, step)
  if (m) w.push(mono(m.length, size, scale))
  const glyph = (base: number, inside: number, n: number) => (step === 'inside' ? inside : step === 'glyphs' ? base : base + GLYPH_GAP + mono(pct(n).length, size, scale))
  if (p.left != null) w.push(glyph(BAT, BAT_IN, p.left))
  if (p.ctx != null) w.push(glyph(RING, RING_IN, p.ctx))
  return w.reduce((a, b) => a + b, 0) + Math.max(0, w.length - 1) * ITEM_GAP
}

/** Quanto deve essere larga la barra per il passo `step`: margini, icone coi numeri, lo spazio fra i due lati, lo stato. */
export const barNeeds = (step: BarStep, p: BarParts, scale = 1, pad = PAD): number => pad * 2 + leftWidth(p.counts, scale) + SIDES_GAP + SAFE + rightWidth(step, p, scale)

/**
 * Il passo della barra larga `width` (px CSS sul web, dp sul telefono) con la scala del testo `scale` (sul telefono quella di sistema; sul
 * web 1: i caratteri sono in px e lo zoom cambia anche la larghezza): il primo che ci sta per intero; se non ci sta niente, solo i glifi.
 * Con «Opus 5 · high», quota e contesto a due cifre e tre numeri a una cifra, a scala 1: tutto da 319, il testo piccolo da 302, il
 * modello senza effort da 264, le percentuali dentro da 237, sotto solo i glifi. A scala 1,3 tutto da 361: a 360 il testo piccolo.
 */
export function barStep(width: number, p: BarParts, scale = 1, pad = PAD): BarStep {
  return STEPS.find((s) => barNeeds(s, p, scale, pad) <= width) ?? 'glyphs'
}
