// Tocca a te con un conto solo (app-pulizia-tocca-03-11, contratto 1.113), come ToccaRules in Kotlin: home, menu e registro danno
// lo stesso numero perché lo calcola il relay. Con un relay precedente restano i conti delle pagine.
import type { State, ToccaState } from './contract'

export const toccaCounts = (s: State): { decidi: number; prove: number } =>
  s.tocca ? { decidi: s.tocca.decidi, prove: s.tocca.prove } : { decidi: s.decisions?.n ?? 0, prove: s.prove?.n ?? 0 }

/**
 * app-verifica-conto-elenco-12-11 (Franz 12/11 13:28: «segnala 30 verifiche da fare ma ne vedo solo 1»): le prove di una sessione aperta
 * stanno nella sua scheda (app-sessioni-compatte-06-11), ma il numero di Verifica è il conto unico del relay e le conta tutte. Nella
 * sezione resta una riga per ognuna di quelle sessioni, col suo numero: elenco e numero dicono la stessa cosa. La più piena prima.
 */
export const proveInSessions = (todo: Record<string, { d: number; v: number }>): { session: string; n: number }[] =>
  Object.entries(todo).filter(([, c]) => c.v > 0).map(([session, c]) => ({ session, n: c.v })).sort((a, b) => b.n - a.n || a.session.localeCompare(b.session))

/** Le voci uscite da sole oggi; null se nessuna o con un relay precedente. */
export const closedToday = (s: State): NonNullable<ToccaState['today']> | null => (s.tocca?.today && s.tocca.today.n > 0 ? s.tocca.today : null)
