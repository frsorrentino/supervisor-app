// I Monitoraggi (app-monitoraggi-09-11, mockup approvati da Franz il 09/11 alle 08:32, sezione in «Dalla master» dalle 09:08): cose
// esterne che la master tiene d'occhio restituite come grafici. Contratto 1.132: `/state.monitors` col conto, l'op `monitors` con
// le schede, una forma per scheda da un catalogo chiuso. Le stesse regole di Monitors.kt, con gli stessi casi.
import type { MonitorCard, MonitorSeries, MonitorsSummary } from './contract'

/** Il catalogo chiuso delle forme; una forma che l'app non conosce non si disegna. */
export const FORMS = ['tile', 'line', 'multiples', 'bars', 'meter', 'table', 'status'] as const
export type Form = (typeof FORMS)[number]
export const formOf = (c: Pick<MonitorCard, 'form'>): Form | null => ((FORMS as readonly string[]).includes(c.form) ? (c.form as Form) : null)

/** Da guardare: una soglia è scattata adesso (`status: watch`), l'unico caso in ambra. */
export const isLook = (c: Pick<MonitorCard, 'status'>) => c.status === 'watch'
/** Quanti sono da guardare: dallo stato se c'è, se no dalle schede. */
export const looking = (summary: MonitorsSummary | null | undefined, items: MonitorCard[] | null | undefined) => summary?.watch ?? (items ?? []).filter(isLook).length

export const values = (s: MonitorSeries): (number | null)[] => s.points.map(p => p[1] ?? null)
export const times = (s: MonitorSeries): number[] => s.points.map(p => p[0] ?? 0)
/** La sparkline di una riga: i valori della prima serie. */
export const spark = (c: MonitorCard): number[] => (c.series[0] ? values(c.series[0]).filter((v): v is number => v != null) : [])
/** La frase sotto il titolo: quella della master se c'è, altrimenti la variazione. */
export const line = (c: MonitorCard): string => (c.verdict?.trim() ? c.verdict : c.delta ?? '')

/** Una scala per grafico, mai due assi: circa tre tacche, passo 1, 2, 2,5 o 5 per una potenza di dieci; le barre da zero. */
export type Scale = { lo: number; hi: number; ticks: number[] }
export function scale(vals: (number | null)[], fromZero = false): Scale {
  const vs = vals.filter((v): v is number => v != null)
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

const idx = (vals: (number | null)[], pick: (a: number, b: number) => boolean): number | null => {
  let best: number | null = null
  vals.forEach((v, i) => { if (v != null && (best == null || pick(v, vals[best]!))) best = i })
  return best
}
/** Il punto più basso, il più alto e l'ultimo di una serie, dove il grafico mette il pallino. */
export const minIndex = (vals: (number | null)[]) => idx(vals, (a, b) => a < b)
export const maxIndex = (vals: (number | null)[]) => idx(vals, (a, b) => a > b)
export const lastIndex = (vals: (number | null)[]) => { for (let i = vals.length - 1; i >= 0; i--) if (vals[i] != null) return i; return null }

/** L'asse del tempo, lo stesso per tutte le serie di una scheda; dove sta un istante, da 0 a 1. */
export function span(c: MonitorCard): [number, number] {
  const all = c.series.flatMap(times)
  return all.length ? [Math.min(...all), Math.max(...all)] : [0, 0]
}
export const xOf = (t: number, sp: [number, number]) => (sp[1] <= sp[0] ? 1 : (t - sp[0]) / (sp[1] - sp[0]))
/** Al tocco, il punto più vicino al dito: `x` da 0 a 1 sull'asse del tempo. */
export function nearest(ts: number[], x: number, sp: [number, number]): number | null {
  const at = Math.min(1, Math.max(0, x))
  let best: number | null = null
  ts.forEach((t, i) => { if (best == null || Math.abs(xOf(t, sp) - at) < Math.abs(xOf(ts[best], sp) - at)) best = i })
  return best
}

/** Quanto è pieno il metro, da 0 a 1 (`pct` del relay, se no l'ultimo valore su `max`); la soglia, se c'è, nella stessa scala. */
export function meter(c: MonitorCard): [number, number | null] {
  const last = c.series[0]?.last ?? null
  const fill = c.pct != null ? c.pct / 100 : last != null && (c.max ?? 0) > 0 ? last / c.max! : 0
  const a = c.alerts.find(x => x.kind === 'above' || x.kind === 'below')
  const mark = a?.value != null && (c.max ?? 0) > 0 ? Math.min(1, Math.max(0, a.value / c.max!)) : null
  return [Math.min(1, Math.max(0, fill)), mark]
}
/** Status: sì (1) o no (0) per ogni giorno. */
export const up = (v: number | null | undefined) => (v ?? 0) > 0

/** Contratto 1.132: l'id di un monitoraggio da un link `<web_app_url>/#/monitor/<id>`; null per ogni altro indirizzo. */
export const monitorOf = (link: string): string | null => /#\/monitor\/([A-Za-z0-9][A-Za-z0-9_-]{0,63})$/.exec(link.trim())?.[1] ?? null

/**
 * «Aggiorna adesso» (app-monitoraggi-aggiorna-10-11, contratto 1.133): il tasto adesso (`now` in secondi), o null se non c'è (monitoraggio
 * non aggiornabile o relay senza `monitor_refresh`). `asked`: il tocco è partito e la risposta non è arrivata. Come Monitors.refresh in Kotlin.
 */
export type Refresh = { kind: 'ready' } | { kind: 'running' } | { kind: 'wait'; until: number }
export function refreshOf(c: MonitorCard, ops: string[] | null | undefined, now: number, asked = false): Refresh | null {
  if (!c.refreshable || !ops?.includes('monitor_refresh')) return null
  const r = c.refresh
  if (asked || r?.state === 'running') return { kind: 'running' }
  return r?.retry_at != null && r.retry_at > now ? { kind: 'wait', until: r.retry_at } : { kind: 'ready' }
}
/** L'esito dell'ultimo aggiornamento, o null; `failed` è il rifiuto del relay al tocco, che vince. Come Monitors.outcome. */
export type Outcome = { kind: 'done'; at: number } | { kind: 'failed'; message: string | null; timeout: boolean }
export function outcomeOf(c: MonitorCard, failed?: string | null): Outcome | null {
  if (failed?.trim()) return { kind: 'failed', message: failed.split('\n')[0].slice(0, 200), timeout: false }
  const r = c.refresh
  if (!r) return null
  if (r.state === 'ok') { const at = r.ended ?? c.at; return at == null ? null : { kind: 'done', at } }
  if (r.state === 'error') return { kind: 'failed', message: r.message?.trim() ? r.message.split('\n')[0].slice(0, 200) : null, timeout: r.rc === 124 }
  return null
}
/** Ogni quanti secondi rileggere la scheda mentre gira, e fino a quando al più. */
export const POLL_S = 3
export const pollUntil = (c: MonitorCard, now: number) => (c.refresh?.started ?? now) + (c.refresh?.timeout_s ?? 120) + POLL_S
/** Cosa serve al tasto, dato da App col contesto `monitor-refresh`: l'op del relay, l'ora, i tocchi in corso e i rifiuti per id. */
export type RefreshCtx = { ops: string[] | null; now: number; asked: string[]; failed: Record<string, string>; onRefresh: (id: string) => void }
/**
 * La sezione Monitoraggi nella home (app-monitoraggi-attesa-10-11): c'è appena `/state.monitors` dice n > 0, col numero del riassunto e le
 * righe in arrivo finché le schede non ci sono. Come Monitors.section in Kotlin.
 */
export function sectionOf(summary: MonitorsSummary | null | undefined, items: MonitorCard[] | null | undefined): { count: number; loading: boolean } | null {
  if (items?.length) return { count: items.length, loading: false }
  return (summary?.n ?? 0) > 0 ? { count: summary!.n, loading: true } : null
}
/** Dopo quanti secondi riprovare la lettura delle schede al tentativo `attempt` (0 il primo): 5, 10, 20, 40, poi ogni 60. */
export const retryAfter = (attempt: number) => Math.min(60, 5 * 2 ** Math.min(4, Math.max(0, attempt)))

/**
 * Contratto 1.138 (app-monitor-link-12-11, Franz 12/11 15:19: «Sarebbe sufficiente che fosse cliccabile e che mi portasse alla pagina
 * con il nuovo prezzo»): il link della cosa guardata, solo http e https, su una riga; altrimenti null e niente si apre. Come
 * Monitors.pageUrl.
 */
export function pageUrl(u: string | null | undefined): string | null {
  const v = (u ?? '').trim()
  if (!v || /\s/.test(v) || v.length > 2000) return null
  try { const x = new URL(v); return x.protocol === 'https:' || x.protocol === 'http:' ? v : null } catch { return null }
}
