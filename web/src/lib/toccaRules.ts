// Tocca a te con un conto solo (app-pulizia-tocca-03-11, contratto 1.113), come ToccaRules in Kotlin: home, menu e registro danno
// lo stesso numero perché lo calcola il relay. Con un relay precedente restano i conti delle pagine.
import type { State, ToccaState } from './contract'

export const toccaCounts = (s: State): { decidi: number; prove: number } =>
  s.tocca ? { decidi: s.tocca.decidi, prove: s.tocca.prove } : { decidi: s.decisions?.n ?? 0, prove: s.prove?.n ?? 0 }

/** Le voci uscite da sole oggi; null se nessuna o con un relay precedente. */
export const closedToday = (s: State): NonNullable<ToccaState['today']> | null => (s.tocca?.today && s.tocca.today.n > 0 ? s.tocca.today : null)
