// La testata della sessione con la riga di icone e la tendina (app-posizioni-sistema-27-11, gruppo 2; proposta in
// dalla-master/posizioni-2026-11-27, «La riga di icone» e la tabella di «Cosa sta in basso»). Le schede sono sempre cinque, nello
// stesso ordine: Chat · Per te · Risultati · Modifiche · Dettagli. Qui le regole che non sono disegno, uguali sul telefono
// (SessionTabs.kt): i numeri delle schede dagli stessi dati delle pillole e della riga dei dati di prima, il tocco che apre o chiude
// la tendina, i tempi del centro notifiche (360 ms FAST_OUT_SLOW_IN, 32 ms fra una riga e l'altra), la tendina tirata su.
import type { Tone } from './iconRow'

export type SessionTab = 'chat' | 'perte' | 'risultati' | 'modifiche' | 'dettagli'
/** L'ordine delle schede, che non cambia mai: Alt+1…5 le sceglie in quest'ordine. */
export const TABS: readonly SessionTab[] = ['chat', 'perte', 'risultati', 'modifiche', 'dettagli']

/** I numeri delle schede con un numero; Chat e Dettagli non ne hanno. */
export type TabCounts = { perte: number; risultati: number; modifiche: number }

const n0 = (x: number | null | undefined) => Math.max(0, Math.floor(x ?? 0))

/**
 * I numeri delle schede, dagli stessi dati che usavano le pillole e la riga dei dati: Per te = le voci che in questa sessione
 * aspettano Franz più i suoi avvisi (la pillola «Per te» e la pillola «Avvisi»); Risultati = i risultati nuovi, cioè i lavori con
 * l'ultima versione non vista; Modifiche = i file cambiati (la pillola «4 file · +8 −2»). Mai negativi.
 */
export function tabCounts(voci: number, notes: number, newResults: number, files: number | null | undefined): TabCounts {
  return { perte: n0(voci) + n0(notes), risultati: n0(newResults), modifiche: n0(files) }
}

/** Gli avvisi della sessione, come la pillola «Avvisi»: l'avviso delle sessioni --bg (contratto 1.146) e la nota di Claude Code. */
export const notesCount = (bg: boolean, headsUp: boolean): number => (bg ? 1 : 0) + (headsUp ? 1 : 0)

/** I risultati nuovi di una sessione: i lavori con l'ultima versione non vista (il puntino della riga «Risultati»). */
export const newResults = (groups: readonly { unseen?: boolean | null }[]): number => groups.filter(g => !!g.unseen).length

/** Il colore del numero di una scheda: ambra chi aspetta Franz, verde un risultato nuovo, grigio il conteggio dei file. */
export const toneOf = (tab: SessionTab): Tone => (tab === 'perte' ? 'wait' : tab === 'risultati' ? 'new' : 'count')

/** Il numero di una scheda; 0 per Chat e Dettagli, che non ne hanno (a zero la riga non lo mostra). */
export const countOf = (tab: SessionTab, c: TabCounts): number => (tab === 'perte' ? c.perte : tab === 'risultati' ? c.risultati : tab === 'modifiche' ? c.modifiche : 0)

/**
 * Il tocco su una scheda (o Alt+1…5): la scheda aperta toccata di nuovo, o Chat, chiude la tendina e torna alla conversazione; un'altra
 * la apre, o prende il posto di quella aperta. Una chiave sconosciuta lascia le cose come stanno.
 */
export function pick(open: SessionTab, tapped: string): SessionTab {
  const t = TABS.find(x => x === tapped)
  if (!t) return open
  return t === open || t === 'chat' ? 'chat' : t
}

/** La tendina è aperta su ogni scheda tranne Chat. */
export const shadeOpen = (tab: SessionTab): boolean => tab !== 'chat'

/** Il centro notifiche (docs/proposte/2026-11-26-centro-notifiche/fonti.md): ANIMATION_DURATION_STANDARD 360 ms, FAST_OUT_SLOW_IN. */
export const SHADE_MS = 360
/** Fra una riga e l'altra della tendina, come le righe del centro notifiche: 32 ms. */
export const ROW_MS = 32
/** Dopo tante righe il ritardo non cresce più: le ultime di una lista lunga non aspettano. */
export const MAX_STAGGER = 8
/** Il ritardo della riga `i` (da 0) mentre la tendina scende. */
export const rowDelay = (i: number): number => Math.min(Math.max(0, Math.floor(i)), MAX_STAGGER) * ROW_MS

/** Tirata su oltre questa parte della sua altezza, la tendina si chiude. */
export const DRAG_CLOSE = 0.25
/** O con uno slancio verso l'alto oltre questa velocità (px CSS al secondo sul web, dp al secondo sul telefono). */
export const FLING = 800

/** Lo spostamento della tendina col dito: solo verso l'alto, mai oltre la sua altezza. */
export const dragOffset = (dy: number, height: number): number => Math.min(0, Math.max(-Math.max(0, height), dy))

/** Al rilascio: la tendina tirata su (`dy` negativo) si chiude oltre un quarto della sua altezza o con uno slancio verso l'alto. */
export function closesOnDrag(dy: number, velocity: number, height: number): boolean {
  if (dy >= 0) return false
  return -dy >= Math.max(0, height) * DRAG_CLOSE || velocity <= -FLING
}

/**
 * La curva FAST_OUT_SLOW_IN, cubic-bezier(0.4, 0, 0.2, 1), che Compose chiama FastOutSlowInEasing: `t` da 0 a 1 nel tempo, il
 * risultato da 0 a 1 nello spazio. Serve al motore della tendina sul web, che segue anche il dito e riparte da dov'è.
 */
export function fastOutSlowIn(t: number): number {
  if (t <= 0) return 0
  if (t >= 1) return 1
  const x1 = 0.4, y1 = 0, x2 = 0.2, y2 = 1
  const bx = (u: number) => 3 * (1 - u) * (1 - u) * u * x1 + 3 * (1 - u) * u * u * x2 + u * u * u
  const by = (u: number) => 3 * (1 - u) * (1 - u) * u * y1 + 3 * (1 - u) * u * u * y2 + u * u * u
  // u tale che x(u) = t, per bisezione: x(u) cresce sempre fra 0 e 1.
  let lo = 0, hi = 1, u = t
  for (let k = 0; k < 40; k++) {
    u = (lo + hi) / 2
    if (bx(u) < t) lo = u; else hi = u
  }
  return by(u)
}
