import type { Height, Side } from './masterLanding'

// Spostare la master col suo tasto tondo (app-sposta-master-a-13-11; mockup app-mockup-sposta-master-12-11, giro E per il tasto e
// giro F variante A per l'arrivo, scelta di Franz del 13/11 alle 12:07), porta di MasterKey.kt. Col puntatore dentro il tasto
// compare una freccia sola: quella della direzione possibile dalla parte del tasto dove sta il puntatore; al centro, e dove non
// c'è una direzione possibile, resta il chevron che apre e chiude. Il clic sposta.
export type Dir = 'E' | 'SE' | 'S' | 'SW' | 'W' | 'NW' | 'N' | 'NE'
/** Dove sta la master: il lato, l'altezza (riga in basso, colonna, sotto la home a metà o compatta) e se la Home è in riga. */
export type Spot = { side: Side; height: Height; homeRow: boolean }
export type Move = { dir: Dir; to: Spot }

/** I gradi di ogni direzione, in senso orario da destra, come sullo schermo (giù = 90). */
export const DEG: Record<Dir, number> = { E: 0, SE: 45, S: 90, SW: 135, W: 180, NW: 225, N: 270, NE: 315 }

/**
 * Da dove sta la master, le direzioni possibili e il posto d'arrivo di ognuna; `sessionsOpen` = ci sono sessioni aperte accanto
 * (senza, la master non va sotto la home e la Home non va in riga). Le direzioni seguono lo schermo: «dentro» è verso le sessioni,
 * «fuori» verso il bordo dove sta la master.
 *  - colonna: dentro = l'altro lato; giù = sotto la home, a metà; giù e dentro = una riga in basso; giù e fuori = Home in riga.
 *  - colonna con la Home in riga: dentro = l'altro lato; su = la Home torna su; giù e dentro = una riga in basso.
 *  - sotto la home a metà: su = colonna intera; giù = compatta; dentro = l'altro lato; giù e dentro = una riga in basso.
 *  - sotto la home compatta: su = a metà; giù = una riga in basso; dentro = l'altro lato.
 *  - una riga in basso: su a sinistra e su a destra = colonna da quel lato; su = sotto la home, a metà, sul lato di adesso.
 */
export function moves(now: Spot, sessionsOpen: boolean): Move[] {
  const inn: 'E' | 'W' = now.side === 'right' ? 'W' : 'E', out: 'E' | 'W' = now.side === 'right' ? 'E' : 'W'
  const other: Side = now.side === 'right' ? 'left' : 'right'
  const at = (height: Height, side: Side = now.side, homeRow = false): Spot => ({ side, height, homeRow })
  const m: Move[] = []
  if (now.height === 'bar') {
    m.push({ dir: 'NW', to: at('column', 'left') }, { dir: 'NE', to: at('column', 'right') })
    if (sessionsOpen) m.push({ dir: 'N', to: at('half') })
  } else if (now.height === 'column') {
    const row = now.homeRow && sessionsOpen
    m.push({ dir: inn, to: at('column', other, row) })
    if (row) m.push({ dir: 'N', to: at('column') })
    else if (sessionsOpen) m.push({ dir: 'S', to: at('half') }, { dir: `S${out}`, to: at('column', now.side, true) })
    m.push({ dir: `S${inn}`, to: at('bar') })
  } else if (now.height === 'half') {
    m.push({ dir: 'N', to: at('column') }, { dir: 'S', to: at('compact') }, { dir: inn, to: at('half', other) }, { dir: `S${inn}`, to: at('bar') })
  } else {
    m.push({ dir: 'N', to: at('half') }, { dir: 'S', to: at('bar') }, { dir: inn, to: at('compact', other) })
  }
  return m
}

/** Il centro del tasto, dove resta il chevron: 8 px col mouse, 30 col dito trascinato. */
export const DEAD_MOUSE = 8
export const DEAD_FINGER = 30

/**
 * La direzione dalla posizione del puntatore rispetto al centro del tasto (`dx`, `dy` in px, giù positivo): dentro `dead` nessuna;
 * fuori, la più vicina fra quelle possibili, se sta entro 45° dal puntatore; altrimenti nessuna.
 */
export function dirAt(dx: number, dy: number, dead: number, dirs: Dir[]): Dir | null {
  if (Math.hypot(dx, dy) < dead || !dirs.length) return null
  const a = (Math.atan2(dy, dx) * 180 / Math.PI + 360) % 360
  const gap = (d: Dir) => { const x = Math.abs(a - DEG[d]); return Math.min(x, 360 - x) }
  const best = dirs.reduce((p, d) => (gap(d) < gap(p) ? d : p))
  return gap(best) <= 45 ? best : null
}

/** Dalla tastiera: una freccia, o due insieme per una diagonale (su o giù più destra o sinistra); l'ultima premuta se non si combinano. */
export function comboDir(held: ('N' | 'S' | 'E' | 'W')[]): Dir | null {
  if (!held.length) return null
  if (held.length === 1) return held[0]
  const v = held.includes('N') ? 'N' : held.includes('S') ? 'S' : null, h = held.includes('E') ? 'E' : held.includes('W') ? 'W' : null
  return v && h ? (`${v}${h}` as Dir) : held[held.length - 1]
}

/** Il nome di un posto d'arrivo, per la zona accesa: `homeRow` e `home` sono la Home che va in riga e che torna su. */
export function nameOf(from: Spot, to: Spot): 'left' | 'right' | 'column' | 'half' | 'compact' | 'bar' | 'homeRow' | 'home' {
  if (to.homeRow && !from.homeRow) return 'homeRow'
  if (from.homeRow && !to.homeRow && to.height === 'column' && to.side === from.side) return 'home'
  // Da un'altra colonna o dalla riga in basso una colonna si chiama col suo lato.
  if (to.height === 'column' && (from.height === 'column' || from.height === 'bar')) return to.side
  return to.height
}
