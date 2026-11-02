import type { AltroveRun, PromemoriaFired, RimastoItem, SeguitiSummary } from './contract'

// Quello che resta fuori dal codice (contratto 1.102), porta di Seguiti.kt. Un valore che non si conosce resta non conosciuto: `stale` null non è «allineata»,
// un errore o un giro parziale non sono mai un negativo.
export const total = (s: SeguitiSummary | null | undefined) => (s ? (s.rimasto ?? 0) + (s.altrove ?? 0) + (s.promemoria ?? 0) : 0)
export type Age = 'behind' | 'aligned' | 'unknown'
export const age = (i: RimastoItem): Age => (i.stale === true ? 'behind' : i.stale === false ? 'aligned' : 'unknown')
export const changedPaths = (i: RimastoItem): number | null => { const m = /^paths:(\d+)$/.exec(i.why ?? ''); return m ? Number(m[1]) : null }
/** Prima chi è indietro, poi chi non si può sapere, in fondo chi è allineato. */
export const ordered = (items: RimastoItem[]) => { const r: Record<Age, number> = { behind: 0, unknown: 1, aligned: 2 }; return [...items].sort((a, b) => r[age(a)] - r[age(b)]) }
/** Con un compito già aperto non se ne apre un altro. */
export const canAlign = (i: RimastoItem) => i.stale !== false && !i.task
export const canAccept = (i: RimastoItem) => i.stale !== false
/** «Nessun altro caso» si dice solo a giro completo, senza errori e con qualcosa di controllato. */
export const clean = (r: AltroveRun) => r.complete && r.total > 0 && r.positive.length === 0 && r.errors.length === 0
export const fired = (p: PromemoriaFired) => p.outcome === 'fired'
const two = (n: number) => String(n).padStart(2, '0')
export const whenText = (epoch: number) => { const d = new Date(epoch * 1000); return `${two(d.getDate())}/${two(d.getMonth() + 1)} ${two(d.getHours())}:${two(d.getMinutes())}` }
