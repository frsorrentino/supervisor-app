// I Monitoraggi (app-monitoraggi-09-11, mockup approvati da Franz il 09/11 alle 08:32, sezione in «Dalla master» dalle 09:08): cose
// esterne che la master tiene d'occhio restituite come grafici. Le stesse regole di Monitors.kt, coi dati dei mockup in
// core/src/test/resources/monitors-mockup.json. Il modello è provvisorio finché non esce il contratto `monitors` del relay.

export type Mark = { day: string; label: string }
export type Series = { name?: string | null; sub?: string | null; values: (number | null)[]; value?: string | null; note?: string | null; delta?: string | null; look?: boolean }
export type Row = { name: string; value: string; note?: string | null; look?: boolean }
export type Chart = {
  form: string; title?: string | null; sub?: string | null; unit?: string | null; step?: boolean; days?: string[]; marks?: Mark[]
  series?: Series[]; value?: string | null; delta?: string | null; limit?: number | null; rows?: Row[]
}
/** `status`: `ok` (pallino verde) o `look` (da guardare: una soglia è scattata, l'unico caso in ambra). */
export type Monitor = {
  id: string; name: string; status: string; text: string; value?: string | null; spark?: number[]; source?: string | null; at?: number | null
  summary?: string | null; alerts?: string | null; example?: string | null; notes?: string[]; charts?: Chart[]
}
export type MonitorsPage = { items: Monitor[] }

/** Il catalogo chiuso delle forme; una forma che l'app non conosce non si disegna. */
export const FORMS = ['tile', 'line', 'multiples', 'bars', 'meter', 'table', 'status'] as const
export type Form = (typeof FORMS)[number]
export const formOf = (c: Chart): Form | null => ((FORMS as readonly string[]).includes(c.form) ? (c.form as Form) : null)

export const isLook = (m: Monitor) => m.status === 'look'
/** Quanti sono da guardare: la riga chiusa di «Dalla master» nomina i Monitoraggi solo allora, in ambra. */
export const looking = (items: Monitor[]) => items.filter(isLook).length

/** Una scala per grafico, mai due assi: circa tre tacche, passo 1, 2, 2,5 o 5 per una potenza di dieci; le barre da zero. */
export type Scale = { lo: number; hi: number; ticks: number[] }
export function scale(values: (number | null)[], fromZero = false): Scale {
  const vs = values.filter((v): v is number => v != null)
  if (!vs.length) return { lo: 0, hi: 1, ticks: [0, 1] }
  const min = fromZero ? Math.min(0, ...vs) : Math.min(...vs)
  const max = Math.max(...vs)
  if (max === min) return { lo: min - 1, hi: max + 1, ticks: [min] }
  const raw = (max - min) / 2
  const p = 10 ** Math.floor(Math.log10(raw))
  const step = [1, 2, 2.5, 5, 10].find(k => k * p >= raw - 1e-9)! * p
  const lo = Math.floor(min / step) * step, hi = Math.ceil(max / step) * step
  const n = Math.round((hi - lo) / step)
  return { lo, hi, ticks: Array.from({ length: n + 1 }, (_, i) => lo + i * step) }
}
/** Da 0 (in basso) a 1 (in alto). */
export const yOf = (s: Scale, v: number) => (s.hi <= s.lo ? 0.5 : (v - s.lo) / (s.hi - s.lo))

const idx = (values: (number | null)[], pick: (a: number, b: number) => boolean): number | null => {
  let best: number | null = null
  values.forEach((v, i) => { if (v != null && (best == null || pick(v, values[best]!))) best = i })
  return best
}
/** Il punto più basso, il più alto e l'ultimo di una serie, dove il grafico mette il pallino. */
export const minIndex = (values: (number | null)[]) => idx(values, (a, b) => a < b)
export const maxIndex = (values: (number | null)[]) => idx(values, (a, b) => a > b)
export const lastIndex = (values: (number | null)[]) => { for (let i = values.length - 1; i >= 0; i--) if (values[i] != null) return i; return null }

/** Al tocco, il giorno più vicino al dito: `x` da 0 a 1, `n` giorni. */
export const dayAt = (x: number, n: number) => (n <= 1 ? 0 : Math.round(Math.min(1, Math.max(0, x)) * (n - 1)))

/** Quanto è pieno il metro (il limite ai quattro quinti) e se il valore l'ha passato. */
export const meter = (value: number, limit: number): [number, boolean] => [limit <= 0 ? 1 : Math.min(1, Math.max(0, value / limit / 1.25)), value >= limit]
