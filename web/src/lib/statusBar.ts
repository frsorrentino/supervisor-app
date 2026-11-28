// app-barra-ibrida-28-11 (Franz 28/11, H1 di dalla-master/barra-ibrida-28-11.html; la barra C sottile di app-barra-stato-28-11 era «troppo
// stretta e non buona esteticamente»): sotto il titolo delle sessioni e del foglio aperto della master la capsula della riga di icone di
// prima (IconRow: alta 50, la pillola elastica del Rubber dock, il nome sulla scheda aperta, i numeri), con dentro, dopo un separatore
// sottile, lo stato: modello · effort, la quota delle 5 ore e l'anellino del contesto, che aprono Dettagli. Qui le regole che non sono
// disegno, uguali sul telefono (StatusBar.kt): i colori e quanto si riduce lo stato con poco spazio, finché la riga ci sta (come fit() del
// mockup): prima via l'effort, poi il testo più piccolo, poi il nome del modello (resta in Dettagli), infine le percentuali. Le icone coi
// numeri restano sempre; mai a capo, mai tagliato.
// Franz, dal telefono: «non mi piace la batteria, oltre che non capisco cosa rappresenti»: la quota è un orologio e «5h 11%», quanto è
// usato, come la pillola di Utilizzo; ambra dal 70 %, rossa dal 90 %.
import { CHIP_FROM } from './sessionBottom'

/** I passi, uno dopo l'altro: tutto; senza effort; il testo più piccolo; senza il modello; senza le percentuali. */
export type BarStep = 'full' | 'noEffort' | 'small' | 'noModel' | 'noPct'
export const STEPS: readonly BarStep[] = ['full', 'noEffort', 'small', 'noModel', 'noPct']

/** Geist Mono: ogni carattere è largo 0,6 della sua altezza (misurato nel browser, uguale a 400 e a 500). */
export const ADV = 0.6
/** Lo stato: 11 px, e 10 dal passo «small»; fra modello, quota e anellino 9, poi 6. */
export const TEXT = 11
export const TEXT_SMALL = 10
export const ITEM_GAP = 9
export const ITEM_GAP_SMALL = 6
/** Il separatore prima dello stato: 4 di margine, la riga da 1, 10 dopo; 6 in fondo, prima del bordo della capsula. */
export const SEP = 15
export const END = 6
/** L'orologio della quota e l'anellino del contesto, da 12; fra il glifo e il suo testo 3. */
export const GLYPH = 12
export const GLYPH_GAP = 3
/** La riga attorno alla capsula: 12 per lato; la capsula: 5 per lato (il bordo da 1 è disegnato dentro). */
export const ROW_PAD = 12
export const CAPSULE_PAD = 5
/** Un margine per gli arrotondamenti del disegno: meglio un passo prima che un testo che tocca il bordo. */
export const SAFE = 2

export type BarTone = 'neutral' | 'warn' | 'alert'
/** La quota delle 5 ore, quanto è usato: ambra dal 70 %, rossa dal 90 %. */
export const QUOTA_WARN = 70
export const QUOTA_ALERT = 90
export const quotaTone = (used: number): BarTone => (used >= QUOTA_ALERT ? 'alert' : used >= QUOTA_WARN ? 'warn' : 'neutral')
/** L'anellino del contesto è ambra dal 70 %, come il chip «Contesto N% · Handoff» (sessionBottom.CHIP_FROM). */
export const ringTone = (ctx: number): BarTone => (ctx >= CHIP_FROM ? 'warn' : 'neutral')

/** Quello che lo stato scrive del modello: modello · effort; dal secondo passo il modello solo; dal quarto niente (resta in Dettagli). */
export function modelText(model: string | null, effort: string | null, step: BarStep): string {
  if (step === 'noModel' || step === 'noPct') return ''
  if (step === 'full') return [model, effort].filter(Boolean).join(' · ')
  return model ?? ''
}

export type BarParts = {
  model: string | null
  effort: string | null
  /** La quota delle 5 ore usata, in %; null senza quota. Si scrive «5h 11%». */
  used: number | null
  /** Il contesto in %; null senza. */
  ctx: number | null
}

const mono = (chars: number, size: number, scale: number): number => chars * ADV * size * scale
/** Il testo della quota, «5h 11%», come la pillola di Utilizzo. */
export const quotaText = (used: number): string => `5h ${Math.round(used)}%`

/** La larghezza dello stato al passo `step`: il separatore, modello, quota e anellino coi loro spazi, il margine in fondo. */
export function statusWidth(step: BarStep, p: BarParts, scale = 1): number {
  const small = step !== 'full' && step !== 'noEffort'
  const size = small ? TEXT_SMALL : TEXT
  const pct = step !== 'noPct'
  const w: number[] = []
  const m = modelText(p.model, p.effort, step)
  if (m) w.push(mono(m.length, size, scale))
  if (p.used != null) w.push(GLYPH + (pct ? GLYPH_GAP + mono(quotaText(p.used).length, size, scale) : 0))
  if (p.ctx != null) w.push(GLYPH + (pct ? GLYPH_GAP + mono(`${Math.round(p.ctx)}%`.length, size, scale) : 0))
  if (!w.length) return 0
  return SEP + END + w.reduce((a, b) => a + b, 0) + (w.length - 1) * (small ? ITEM_GAP_SMALL : ITEM_GAP)
}

/** Quanto deve essere larga la riga per il passo `step`, con le schede larghe `tabs` (a riposo, col nome più lungo aperto). */
export const barNeeds = (step: BarStep, tabs: number, p: BarParts, scale = 1): number => ROW_PAD * 2 + CAPSULE_PAD * 2 + tabs + statusWidth(step, p, scale) + SAFE

/**
 * Il passo dello stato nella riga larga `width` (px CSS sul web, dp sul telefono), con le schede larghe `tabs` a riposo col nome più lungo
 * aperto (così il passo non cambia aprendo e chiudendo le schede) e la scala del testo `scale` (sul telefono quella di sistema; sul web 1):
 * il primo che ci sta per intero; se non ci sta niente, l'ultimo. Con «Opus 5 · high», «5h 11%» e «34%» lo stato è largo 214 a tutto, 168
 * senza effort, 153 col testo piccolo, 111 senza modello, 51 coi soli glifi. Le tre schede a 360 sono larghe 212 col nome più lungo aperto
 * (Modifiche, misurato sulla web): la riga vuole 359, resta lo stato senza modello; a 407 (una colonna a 1280) il modello col testo piccolo.
 */
export function barStep(width: number, tabs: number, p: BarParts, scale = 1): BarStep {
  return STEPS.find((s) => barNeeds(s, tabs, p, scale) <= width) ?? 'noPct'
}
