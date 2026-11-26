import type { Approval, WaitItem } from './contract'

// Le regole delle otto animazioni scelte da Franz il 26/11 alle 11:50 (app-animazioni-otto-26-11), porta di Animazioni.kt: i
// tempi comuni e le decisioni che non sono disegno. Il movimento lo fanno le variabili --spring-* di app.css.

/** 1, Copia che risponde: quanto resta «Copiato» prima di tornare Copia. */
export const COPY_HOLD_MS = 1600
/** 6, Tieni premuto: quanto si tiene il dito su un'approvazione di produzione. */
export const HOLD_MS = 600
/** 3, Al lavoro: il ciclo del riflesso, lo stesso della linea del lavoro in corso. */
export const CYCLE_MS = 1600

/** 6: solo le approvazioni che escono in produzione chiedono di tenere premuto. */
export const needsHold = (a: Approval | null | undefined): boolean => a?.deploy === true
export const needsHoldWait = (w: WaitItem | null | undefined): boolean => w?.card?.deploy === true

/** 4: la cifra scorre solo quando cambia per un gesto di Franz, mai per un aggiornamento dal PC. */
export const rolls = (prev: number | null | undefined, next: number, byGesture: boolean): boolean => byGesture && prev != null && prev !== next

/** 2: le parole di un paragrafo come [inizio, fine] (fine compresa) nel testo mostrato. */
export function words(text: string): [number, number][] {
  return [...text.matchAll(/\S+/g)].map(m => [m.index!, m.index! + m[0].length - 1])
}
/** 2: la parola che la voce sta dicendo, contata nel pezzo pulito più le parole dei pezzi già letti; null senza parole. */
export function wordIndex(spokenChunk: string, offset: number, before: number, shownWords: number): number | null {
  if (shownWords <= 0) return null
  const ws = words(spokenChunk)
  let at = ws.findIndex(([, end]) => offset <= end)
  if (at < 0) at = Math.max(0, ws.length - 1)
  return Math.min(shownWords - 1, Math.max(0, before + at))
}
export const wordCount = (text: string): number => words(text).length
