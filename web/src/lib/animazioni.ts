import type { Approval, WaitItem } from './contract'

// Le regole delle otto animazioni scelte da Franz il 26/11 alle 11:50 (app-animazioni-otto-26-11), porta di Animazioni.kt: i
// tempi comuni e le decisioni che non sono disegno. Il movimento lo fanno le variabili --spring-* di app.css.

/** 1, Copia che risponde: quanto resta «Copiato» prima di tornare Copia. */
export const COPY_HOLD_MS = 1800
/** 6, Tieni premuto: quanto si tiene il dito su un'approvazione di produzione. */
export const HOLD_MS = 1200
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

// ---- Secondo giro (app-animazioni-seconde-26-11, scelta D di Franz il 26/11 alle 12:49), come Animazioni.kt ----

/** 4, Annulla: quanto aspetta un'azione che toglie qualcosa prima di partire. */
export const UNDO_MS = 5000
/** 2, Barra della master: quanto resta allungata all'arrivo di una risposta. */
export const DOCK_PEEK_MS = 3000
/** 8, Domanda al polso: lo scarto fra le parti. */
export const STAGGER_MS = 40
/** 7, Limite ad anello: gli ultimi caratteri in ambra. */
export const NEAR = 20
export type Limit = 'ok' | 'near' | 'over'
export const limit = (length: number, max: number): Limit => (length > max ? 'over' : max - length <= NEAR ? 'near' : 'ok')
export const fill = (length: number, max: number): number => (max <= 0 ? 1 : Math.min(1, Math.max(0, length / max)))
/** 5, Puntini: con più di `max` immagini i `max` attorno a quella di adesso, come [inizio, fine). */
export function dots(count: number, current: number, max = 7): [number, number] {
  if (count <= 0) return [0, 0]
  if (count <= max) return [0, count]
  const start = Math.min(count - max, Math.max(0, current - Math.floor(max / 2)))
  return [start, start + max]
}
/** 3, Per te a pila: i bordi sotto la prima voce, al massimo due. */
export const stackEdges = (items: number): number => Math.min(2, Math.max(0, items - 1))
/** 6, Suggerimento nel campo: il primo passo, solo a campo vuoto. */
export const ghost = (steps: string[], typed: string): string | null => (typed.length ? null : steps.find(x => x.trim())?.trim() ?? null)
/** 1, Messaggio che entra: la pillola dei nuovi solo se chi legge non è in fondo e c'è qualcosa di nuovo. */
export const newPill = (atBottom: boolean, unseen: number): boolean => !atBottom && unseen > 0

// Terzo giro, voce 8 dopo la prova 1391 (Franz 27/11): il punto della barra dei paragrafi sta all'inizio del paragrafo che la voce legge;
// durante il trascinamento segue il dito, al rilascio si aggancia al paragrafo che comincia più vicino. Come Animazioni.scrubAt e scrubNearest.
export const scrubAt = (block: number, blocks: number): number => (blocks <= 1 ? 0 : Math.min(blocks - 1, Math.max(0, block)) / blocks)
export const scrubNearest = (f: number, blocks: number): number =>
  blocks <= 1 ? 0 : Math.min(blocks - 1, Math.max(0, Math.floor(Math.min(1, Math.max(0, f)) * blocks + 0.5)))

// Step progress nella chiusura (app-chiusura-step-progress-27-11, Franz 27/11 12:00), come Animazioni.step, stepLine e closeStep: il passo
// è fatto se viene prima dell'attuale, attuale se è lui; l'attuale è fatto solo a lavoro finito (scelta a della master: «Chiusa» raggiunta
// aspetta la conferma del PC). La linea verde arriva al pallino dell'attuale.
export type Step = 'done' | 'current' | 'todo'
export const step = (k: number, at: number, finished = false): Step => (k < at || (finished && k === at) ? 'done' : k === at ? 'current' : 'todo')
export const stepLine = (at: number, n: number): number => (n <= 1 ? 0 : Math.min(n - 1, Math.max(0, at)) / (n - 1))
export const closeStep = (save: boolean, s: string | null, delivered: boolean): number => (save ? (s === 'exit' ? 1 : 0) : delivered ? 1 : 0)
