import type { DecisionsSummary, ProveSummary, SeguitiSummary } from './contract'

// Il testo a destra delle voci della home (Franz 02/11 09:41), porta di HomeRight.kt: il numero accanto al nome, a destra quello che il numero non dice.
export type Part = 'builds' | 'ko' | 'here' | 'unblocks' | 'behind' | 'elsewhere' | 'reminders'
export type Bit = { part: Part; n: number }
const bits = (...b: Bit[]) => b.filter(x => x.n > 0)
export const prove = (p: ProveSummary) => bits({ part: 'builds', n: p.builds }, { part: 'ko', n: p.ko + p.red })
export const decisions = (d: DecisionsSummary) => bits({ part: 'here', n: d.here }, { part: 'unblocks', n: d.tasks })
export const seguiti = (s: SeguitiSummary) => bits({ part: 'behind', n: s.rimasto }, { part: 'elsewhere', n: s.altrove }, { part: 'reminders', n: s.promemoria })
