import type { Row } from './chatTurns'

// app-trova-messaggi-20-11 (Franz 20/11 20:56: non ritrovava un suo messaggio; 21:12 «ok A»): il salto fra i miei messaggi nella
// conversazione, due frecce e il contatore «3 di 12». Le fermate sono le richieste dei turni: i messaggi di Franz, anche quelli non
// ancora arrivati (il mandato), mai i turni partiti da soli. Porta di MineJump.kt.

/** Le fermate, dalla più vecchia: l'id della voce della trascrizione, o del mandato se la voce non c'è ancora. */
export function mineStops(rows: Row[]): string[] {
  return rows.flatMap(r => {
    if (r.type !== 'turn' || r.auto || !r.request) return []
    const q = r.request
    if (q.type === 'user') return [q.entry.id]
    if (q.type === 'mine') return [q.entry?.id ?? q.sent.id]
    return []
  })
}

/** La fermata dopo un tocco: `at` la voce mostrata ora (null = si comincia dall'ultima), `dir` -1 prima, +1 dopo; ai capi si resta fermi. */
export function mineStep(stops: string[], at: string | null, dir: -1 | 1): string | null {
  if (!stops.length) return null
  const i = at == null ? -1 : stops.indexOf(at)
  if (i < 0) return stops[stops.length - 1]
  return stops[Math.min(stops.length - 1, Math.max(0, i + dir))]
}

/** «3 di 12»: la posizione dalla più vecchia, su quante sono caricate; null se la voce non c'è. */
export function minePosition(stops: string[], at: string | null): [number, number] | null {
  const i = at == null ? -1 : stops.indexOf(at)
  return i < 0 ? null : [i + 1, stops.length]
}
