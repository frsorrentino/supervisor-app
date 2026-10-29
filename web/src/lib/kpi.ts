import type { Session, State, TimelineEvent, TimelinePage } from './contract'
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
