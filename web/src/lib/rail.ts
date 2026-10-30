import type { Session } from './contract'
import { waitingSince } from './kpi'
import type { Group } from './summary'

// La colonna dei minuti dello stile «Studio e Diretta» (terza versione, approvata da Franz il 30/10 alle 09:49): a sinistra
// di ogni riga il glifo dello stato e, sotto, da quanto aspetta o da quanto dura il turno, corto come in una diretta.

export type Span = { n: number; unit: 'min' | 'h' | 'd' }
export type GlyphKind = 'waiting' | 'busy' | 'done' | 'red' | 'still'

/** Da `fromS` a `nowS` in minuti interi, poi ore, poi giorni; mai negativo. */
export function span(fromS: number, nowS: number): Span {
  const m = Math.max(0, Math.floor((nowS - fromS) / 60))
  if (m < 60) return { n: m, unit: 'min' }
  const h = Math.floor(m / 60)
  return h < 24 ? { n: h, unit: 'h' } : { n: Math.floor(h / 24), unit: 'd' }
}

/** Il glifo come le icone di Telegram: ❓ chi aspetta te (anche se ha un turno in corso), ▶ chi lavora, ✓ chi ha finito e
 *  non l'hai ancora visto, il tratteggio per chi è fermo o chiuso. Il rosso viene dagli esiti della cronologia. */
export function glyphOf(s: Session, group?: Group): GlyphKind {
  if (s.state === 'waiting' || waitingSince(s) != null) return 'waiting'
  if (s.state === 'busy' || s.state === 'awaiting') return 'busy'
  return group === 'finished' ? 'done' : 'still'
}
