import type { KpiChecksDay, KpiData, KpiSuggestions, KpiTask, KpiTasks, KpiTurn, KpiWaits, Session, State, TimelineEvent, TimelinePage } from './contract'
import { stop } from './strongAlert'

// I KPI coi dati che il relay ha già (proposte 6, 11, 15 e 18 del 25/10, compito app-kpi-medie-25-10), porta di Kpi.kt:
// tutto dalla cronologia di oggi (contratto 1.29) e dallo stato. La cronologia non registra le attese: «ti aspetta» vale
// solo per il tratto in corso, letto dallo stato (master, 29/10 08:07).

export const HOURS = 6
/** Due eventi a meno di 20 minuti l'uno dall'altro sono lo stesso tratto di lavoro. */
export const GAP_S = 20 * 60
/** Un evento isolato vale almeno 10 minuti di lavoro. */
export const MIN_WORK_S = 10 * 60
/** Contratto 1.29: al massimo 40 eventi per sessione; con tanti la cifra è un minimo. */
export const EVENTS_CAP = 40

export type Band = 'work' | 'wait' | 'still'
export type Span = { from: number; to: number; band: Band }

const ACTIVITY = new Set(['prompt', 'test', 'commit', 'outcome', 'compact', 'task'])
const activity = (events: TimelineEvent[]) => events.filter(e => ACTIVITY.has(e.kind))

/** Da quando una sessione aspetta Franz adesso: una domanda aperta o una ferma; null se non aspetta. */
export const waitingSince = (s: Session): number | null => s.question?.asked_at ?? stop(s)?.at ?? null

/** La mezzanotte di oggi nel fuso del browser (o in quello dato, per i test). */
export function midnight(now: number, timeZone?: string): number {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone, hourCycle: 'h23', hour: 'numeric', minute: 'numeric', second: 'numeric' }).formatToParts(new Date(now * 1000))
  const n = (t: string) => Number(parts.find(p => p.type === t)?.value ?? 0)
  return now - (n('hour') * 3600 + n('minute') * 60 + n('second'))
}

/** Da dove chiedere la cronologia: la mezzanotte di oggi o sei ore fa, la più vecchia. */
export const since = (now: number, timeZone?: string) => Math.min(midnight(now, timeZone), now - HOURS * 3600)

/** 6 · le ultime sei ore, un conto per ora (la più vecchia prima): prompt, test, commit ed esiti. */
export function hourly(events: TimelineEvent[], now: number, hours = HOURS): number[] {
  const out = new Array<number>(hours).fill(0)
  for (const e of activity(events)) {
    const back = Math.floor((now - e.at) / 3600)
    if (e.at <= now && back < hours) out[hours - 1 - back]++
  }
  return out
}

function merge(spans: Span[]): Span[] {
  const acc: Span[] = []
  for (const s of spans) {
    const last = acc.at(-1)
    if (last && last.band === s.band && last.to === s.from) acc[acc.length - 1] = { ...last, to: s.to }
    else acc.push(s)
  }
  return acc
}

/** 11 · la giornata a fasce, da `from` a `now`: lavoro dove gli eventi sono vicini, ferma negli intervalli; con `waiting` l'ultimo tratto è «ti aspetta». */
export function day(events: TimelineEvent[], from: number, now: number, waiting: number | null): Span[] {
  const work: [number, number][] = []
  for (const at of activity(events).map(e => e.at).filter(at => at >= from && at <= now).sort((a, b) => a - b)) {
    const last = work.at(-1)
    if (last && at - last[1] <= GAP_S) last[1] = Math.max(last[1], at)
    else work.push([at, at])
  }
  const spans: Span[] = []
  let t = from
  for (const [a, b] of work) {
    const end = Math.min(now, Math.max(b, a + MIN_WORK_S))
    if (a > t) spans.push({ from: t, to: a, band: 'still' })
    if (end > Math.max(a, t)) spans.push({ from: Math.max(a, t), to: end, band: 'work' })
    t = Math.max(t, end)
  }
  if (t < now) spans.push({ from: t, to: now, band: 'still' })
  if (waiting == null) return merge(spans)
  const w = Math.max(waiting, from)
  if (w >= now) return merge(spans)
  const cut = spans.flatMap(s => (s.to <= w ? [s] : s.from < w ? [{ ...s, to: w }] : []))
  return merge([...cut, { from: w, to: now, band: 'wait' }])
}

export const workSeconds = (spans: Span[]) => spans.filter(s => s.band === 'work').reduce((n, s) => n + s.to - s.from, 0)

/** 15 · sei caselle, una per ora: lavoro se ci sono eventi, ferma altrimenti; l'ora in corso è ambra per chi aspetta adesso. */
export const cells = (events: TimelineEvent[], now: number, waitingNow: boolean): Band[] =>
  hourly(events, now).map((n, i) => (i === HOURS - 1 && waitingNow ? 'wait' : n > 0 ? 'work' : 'still'))

/** 18 · la giornata in cifre. `atLeast` = qualche sessione è al tetto di eventi del relay: le cifre sono un minimo. */
export type DayFigures = {
  prompts: number; commits: number; atLeast: boolean; tokens: number; topSession: string | null; topShare: number
  waitingNow: number; oldestWaitS: number | null; perHour: [number, number][]; firstHour: number
}

export function dayFigures(page: TimelinePage | null, st: State | null, now: number, timeZone?: string): DayFigures {
  const from = midnight(now, timeZone)
  const today = (page?.sessions ?? []).flatMap(s => s.events).filter(e => e.at >= from && e.at <= now)
  const prompts = today.filter(e => e.kind === 'prompt')
  const commits = today.filter(e => e.kind === 'commit')
  const usage = st?.usage_today ?? []
  const total = usage.reduce((n, u) => n + u.tokens, 0)
  const top = usage.reduce<(typeof usage)[number] | null>((a, u) => (!a || u.tokens > a.tokens ? u : a), null)
  const waits = (st?.sessions ?? []).map(waitingSince).filter((x): x is number => x != null)
  const hourOf = (at: number) => Math.floor((at - from) / 3600)
  const nowHour = hourOf(now)
  const first = Math.min(nowHour, ...[...prompts, ...commits].map(e => hourOf(e.at)))
  const perHour: [number, number][] = []
  for (let h = first; h <= nowHour; h++) perHour.push([prompts.filter(e => hourOf(e.at) === h).length, commits.filter(e => hourOf(e.at) === h).length])
  return {
    prompts: prompts.length, commits: commits.length,
    atLeast: !!page?.more || (page?.sessions ?? []).some(s => s.events.length >= EVENTS_CAP),
    tokens: total, topSession: top?.session ?? null, topShare: top ? top.tokens / Math.max(1, total) : 0,
    waitingNow: waits.length, oldestWaitS: waits.length ? now - Math.min(...waits) : null, perHour, firstHour: first,
  }
}

// ---- Proposte piccole (4, 5, 7; compito app-kpi-piccole-25-10), porta di Kpi.kt ----

/** 5 · la soglia di handoff sul contesto (fable-director: handoff dal 60%, rosso dall'80%). */
export const HANDOFF = 60

export type Slice = { session: string; pct: number }
export type Split = { account: string; h5: number; slices: Slice[]; others: number }

/** 4 · la quota 5h di un account divisa per sessione, in proporzione alla parte di token di oggi; le prime tre, il resto insieme. */
export function split(st: State, account: string, top = 3): Split | null {
  const q = st.quota[account]
  if (q?.h5 == null) return null
  const mine = (st.usage_today ?? []).filter(u => u.account === account || (!!q.kind && u.account === q.kind)).sort((a, b) => b.share - a.share)
  if (!mine.length) return null
  const slices = mine.slice(0, top).map(u => ({ session: u.session, pct: Math.round(q.h5! * u.share) }))
  return { account, h5: q.h5, slices, others: mine.length > top ? Math.max(0, q.h5 - slices.reduce((n, x) => n + x.pct, 0)) : 0 }
}

/** 5 · la parte di token di oggi della sessione sul suo account, in percento; null senza dati. */
export const todayShare = (st: State, session: string): number | null => {
  const u = st.usage_today?.find(x => x.session === session)
  return u ? Math.round(u.share * 100) : null
}

/** 7 · tre cifre in testa alla home: chi ti aspetta (e da quanto la prima), chi lavora, gli esiti di oggi e i rossi. */
export type Head = { waiting: number; oldestWaitS: number | null; busy: number; outcomes: number | null; red: number | null }

export function head(st: State, page: TimelinePage | null, now: number, timeZone?: string): Head {
  const live = st.sessions.filter(s => s.state !== 'gone')
  const waits = live.map(waitingSince).filter((x): x is number => x != null)
  const from = midnight(now, timeZone)
  const today = page ? page.sessions.flatMap(s => s.events).filter(e => e.at >= from && e.at <= now) : null
  return {
    waiting: waits.length, oldestWaitS: waits.length ? now - Math.min(...waits) : null, busy: live.filter(s => s.state === 'busy').length,
    outcomes: today ? today.filter(e => e.kind === 'outcome').length : null, red: today ? today.filter(e => e.kind === 'task' && e.ok === false).length : null,
  }
}

// ---- Proposte coi dati nuovi del relay (8, 9, 10, 12, 13, 17; contratto 1.96, compito app-kpi-dati-relay-25-10) ----

/** 8 · la barra d'età di un compito: piena da un giorno in su. */
export const ageFraction = (ageS: number) => Math.min(1, Math.max(0, ageS / 86400))

/** 8 · i compiti fermi prima, poi i più vecchi. */
export const tasksOrder = (t: KpiTasks): KpiTask[] => t.tasks.slice().sort((a, b) => Number(b.stalled) - Number(a.stalled) || b.age_s - a.age_s)

/** 9 · i verdi e i rossi della settimana. */
export const checksTotals = (days: KpiChecksDay[]): [number, number] => [days.reduce((n, d) => n + d.green, 0), days.reduce((n, d) => n + d.red, 0)]

/** 10 · i turni di oggi di una sessione; vuoto se il relay non ne ha. */
export const turnsOf = (data: KpiData | null, session: string): KpiTurn[] => data?.kpi_turns_today?.find(x => x.session === session)?.turns ?? []

export type TurnsSummary = { n: number; minutes: number; failed: number; tokensFirst: number; tokensLast: number; tokensMax: number }

/** 10 · quanti turni, i minuti in tutto, i falliti e il costo del primo, dell'ultimo e del più caro. */
export function turnsSummary(turns: KpiTurn[]): TurnsSummary {
  const secs = turns.reduce((n, x) => n + Math.max(0, x.end - x.start), 0)
  return {
    n: turns.length, minutes: Math.floor((secs + 30) / 60), failed: turns.filter(x => x.outcome === 'fail').length,
    tokensFirst: turns[0]?.tokens ?? 0, tokensLast: turns.at(-1)?.tokens ?? 0, tokensMax: Math.max(0, ...turns.map(x => x.tokens)),
  }
}

export const suggestionsTotal = (s: KpiSuggestions) => s.accepted + s.discussed + s.later + s.never + s.expired

/** 13 · la parte accettata in percento; null senza suggerimenti nei sette giorni. */
export function acceptRate(s: KpiSuggestions): number | null {
  const total = suggestionsTotal(s)
  return total ? Math.round((s.accepted * 100) / total) : null
}

/** 17 · le attese chiuse nella settimana e la mediana più alta, per la scala delle colonne. */
export const waitsClosed = (w: KpiWaits) => w.week.reduce((n, d) => n + d.n, 0)
export const waitsTop = (w: KpiWaits) => Math.max(0, ...w.week.map(d => d.median_min ?? 0))
