// «Sessioni al massimo» nelle impostazioni (app-tabellone-frazioni-13-11, Franz 13/11 13:56: «il numero di sessioni massime gestite
// deve essere selezionabile dall'utente nelle impostazioni»; contratto 1.145), porta di MaxSessions.kt. Il valore di adesso è
// `kpi_health.sessions_max`; si cambia con l'op `max_sessions_set`, presente in `ops` solo nei relay che lo sanno fare: il numero
// in `arg` come intero in testo, da 1 a 20; la risposta buona è `{"max_sessions": N}`. Senza l'op la voce si legge e basta.
export const OP = 'max_sessions_set'
export const MIN = 1
export const MAX = 20

export const canSet = (ops: string[] | null | undefined): boolean => !!ops?.includes(OP)
export const valid = (n: number): boolean => Number.isInteger(n) && n >= MIN && n <= MAX

/** I numeri fra cui scegliere; un valore di adesso fuori dall'intervallo (messo a mano nella config) resta in elenco al suo posto. */
export function choices(now: number | null): number[] {
  const all = Array.from({ length: MAX - MIN + 1 }, (_, i) => MIN + i)
  return now != null && Number.isInteger(now) && !valid(now) ? [...all, now].sort((a, b) => a - b) : all
}

/** Il numero confermato dal relay, dal testo della risposta; null se non si legge. */
export function answer(text: string): number | null {
  try { const n = (JSON.parse(text) as { max_sessions?: unknown }).max_sessions; return typeof n === 'number' && Number.isInteger(n) ? n : null } catch { return null }
}
