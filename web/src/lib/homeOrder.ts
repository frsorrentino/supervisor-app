// L'ordine delle sezioni della home (Franz 30/10 17:22, compito app-riordino-sezioni-30-10; HomeOrder.kt nell'app): un ordine
// unico sul relay, `state.home_order`. Tabellone e «Ti aspettano» non hanno chiave: restano fissi in cima.
export type HomeSection = 'sessions' | 'suggestions' | 'lavori' | 'esiti' | 'usage' | 'system' | 'plugins' | 'closed'
/** Le righe separabili (Franz 31/10 15:50) sostituiscono `altro`. */
export const ROWS: HomeSection[] = ['usage', 'system', 'plugins', 'closed']
export const DEFAULT_ORDER: HomeSection[] = ['sessions', 'suggestions', 'lavori', 'esiti', 'usage', 'system', 'plugins', 'closed']

/** Le chiavi sconosciute si ignorano e le mancanti si accodano, così una sezione nuova non chiede una release del relay. */
export function normalize(saved: string[] | null | undefined): HomeSection[] {
  // Un ordine salvato prima della separazione: `altro` si espande al suo posto.
  const known = [...new Set((saved ?? []).flatMap(k => (k === 'altro' ? ROWS as string[] : [k])).filter((k): k is HomeSection => (DEFAULT_ORDER as string[]).includes(k)))]
  return [...known, ...DEFAULT_ORDER.filter(k => !known.includes(k))]
}

/** Le righe contigue stanno nella stessa card; una sezione o una riga separata fa gruppo da sola. */
export function groups(order: HomeSection[]): HomeSection[][] {
  const out: HomeSection[][] = []
  for (const k of order) {
    const last = out[out.length - 1]
    if (ROWS.includes(k) && last && ROWS.includes(last[0])) last.push(k)
    else out.push([k])
  }
  return out
}

/** Sposta la voce `from` al posto `to`; fuori dai limiti la lista resta com'è. */
export function move<T>(list: T[], from: number, to: number): T[] {
  if (from === to || from < 0 || to < 0 || from >= list.length || to >= list.length) return list
  const out = [...list]
  out.splice(to, 0, ...out.splice(from, 1))
  return out
}

/**
 * Il trascinamento sul posto (Franz 31/10 14:27): la voce presa scavalca la vicina solo quando il puntatore incrocia la metà della
 * sua testata; dopo lo scambio la condizione opposta è falsa, quindi la voce non rimbalza. HomeOrder.dragTarget nell'app.
 */
export function dragTarget(order: HomeSection[], held: HomeSection, y: number, mids: Partial<Record<HomeSection, number>>): HomeSection[] {
  const i = order.indexOf(held)
  if (i < 0) return order
  const next = order.slice(i + 1).find(k => mids[k] !== undefined)
  if (next && y > mids[next]!) return move(order, i, order.indexOf(next))
  const prev = order.slice(0, i).reverse().find(k => mids[k] !== undefined)
  if (prev && y < mids[prev]!) return move(order, i, order.indexOf(prev))
  return order
}

/** L'arg dell'op `home_order`: l'array JSON delle chiavi; null = torna al predefinito. */
export const orderArg = (order: HomeSection[]): string | null => (order.join() === DEFAULT_ORDER.join() ? null : JSON.stringify(order))
