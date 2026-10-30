// L'ordine delle sezioni della home (Franz 30/10 17:22, compito app-riordino-sezioni-30-10; HomeOrder.kt nell'app): un ordine
// unico sul relay, `state.home_order`. Tabellone e «Ti aspettano» non hanno chiave: restano fissi in cima.
export type HomeSection = 'sessions' | 'suggestions' | 'lavori' | 'esiti' | 'altro'
export const DEFAULT_ORDER: HomeSection[] = ['sessions', 'suggestions', 'lavori', 'esiti', 'altro']

/** Le chiavi sconosciute si ignorano e le mancanti si accodano, così una sezione nuova non chiede una release del relay. */
export function normalize(saved: string[] | null | undefined): HomeSection[] {
  const known = [...new Set((saved ?? []).filter((k): k is HomeSection => (DEFAULT_ORDER as string[]).includes(k)))]
  return [...known, ...DEFAULT_ORDER.filter(k => !known.includes(k))]
}

/** Sposta la voce `from` al posto `to`; fuori dai limiti la lista resta com'è. */
export function move<T>(list: T[], from: number, to: number): T[] {
  if (from === to || from < 0 || to < 0 || from >= list.length || to >= list.length) return list
  const out = [...list]
  out.splice(to, 0, ...out.splice(from, 1))
  return out
}

/** L'arg dell'op `home_order`: l'array JSON delle chiavi; null = torna al predefinito. */
export const orderArg = (order: HomeSection[]): string | null => (order.join() === DEFAULT_ORDER.join() ? null : JSON.stringify(order))
