import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type KpiData, type Session, type State, type TimelineEvent, type TimelinePage } from './contract'
import { acceptRate, ageFraction, cells, checksTotals, day, dayFigures, head, hourly, outcomes, split, stillRows, suggestionsTotal, tasksOrder, todayShare, turnsOf, turnsSummary, waitingRows, waitsClosed, waitsTop, workingRows, workSeconds } from './kpi'

const read = (f: string) => readFileSync(new URL(`../../../contract/${f}`, import.meta.url), 'utf8')
const sample = JSON.parse(read('cmd-result-sample.json'))
const tid = sample.cmd.find((c: { op: string }) => c.op === 'timeline').id
const page: TimelinePage = JSON.parse(sample.result.find((r: { id: string }) => r.id === tid).text)
const now = 1789210000
const ev = (at: number, kind = 'prompt'): TimelineEvent => ({ at, kind, text: 'x' })
const of = (name: string) => page.sessions.find(s => s.session === name)!.events

describe('KPI coi dati già nel relay (proposte 6, 11, 15, 18)', () => {
  it('kpi-6: le ultime sei ore, un conto per ora, la più vecchia prima', () => {
    expect(hourly(of('ledger-api'), now)).toEqual([0, 0, 0, 0, 2, 4])
    expect(hourly(of('field-notes'), now)).toEqual([0, 0, 0, 0, 0, 6])
    expect(hourly([ev(now - 7 * 3600), ev(now + 60)], now)).toEqual([0, 0, 0, 0, 0, 0])
  })

  it('kpi-11: lavoro dove gli eventi sono vicini, ferma in mezzo, ti aspetta solo per il tratto in corso', () => {
    const from = now - 4 * 3600
    const events = [ev(from + 600), ev(from + 900), ev(from + 1500), ev(from + 3 * 3600)]
    const spans = day(events, from, now, null)
    expect(spans.map(s => s.band)).toEqual(['still', 'work', 'still', 'work', 'still'])
    expect([spans[1].from, spans[1].to]).toEqual([from + 600, from + 1500])
    expect(workSeconds(spans)).toBe(900 + 600)
    const waiting = day(events, from, now, now - 1800)
    expect(waiting.map(s => s.band)).toEqual(['still', 'work', 'still', 'work', 'still', 'wait'])
    expect([waiting.at(-1)!.from, waiting.at(-1)!.to]).toEqual([now - 1800, now])
    for (let i = 1; i < waiting.length; i++) expect(waiting[i].from).toBe(waiting[i - 1].to)
  })

  it('kpi-15: sei caselle, ambra nell\'ora in corso per chi aspetta adesso', () => {
    expect(cells([ev(now - 4000)], now, true)).toEqual(['still', 'still', 'still', 'still', 'work', 'wait'])
    expect(cells([ev(now - 4000)], now, false)).toEqual(['still', 'still', 'still', 'still', 'work', 'still'])
  })

  it('kpi-18: la giornata in cifre, e un minimo al tetto di 40 eventi', () => {
    const st = decodeState(read('state-2-idle.json'))
    const f = dayFigures(page, st, now, 'Europe/Rome')
    expect([f.prompts, f.commits, f.atLeast]).toEqual([7, 1, false])
    expect(f.tokens).toBe(st.usage_today!.reduce((n, u) => n + u.tokens, 0))
    expect(f.topSession).toBe('atlas-shop')
    expect(f.perHour.reduce((n, [p]) => n + p, 0)).toBe(7)
    const full = { ...page, sessions: [{ ...page.sessions[0], events: Array.from({ length: 40 }, () => ev(now - 100)) }] }
    expect(dayFigures(full, st, now, 'Europe/Rome').atLeast).toBe(true)
  })

  it('kpi-4, kpi-5, kpi-7: la quota per sessione, la parte di oggi, le tre cifre', () => {
    const st = decodeState(read('state-2-idle.json'))
    expect(split(st, 'personal')).toEqual({ account: 'personal', h5: 24, slices: [{ session: 'atlas-shop', pct: 15 }, { session: 'master', pct: 9 }], others: 0 })
    expect(split(st, 'nobody')).toBeNull()
    expect(todayShare(st, 'atlas-shop')).toBe(62)
    const h = head(st, page, now, 'Europe/Rome')
    expect([h.waiting, h.busy, h.outcomes, h.red]).toEqual([1, 0, 1, 0])
    expect(head(st, null, now, 'Europe/Rome').outcomes).toBeNull()
  })
})

describe('i box della home aperti (app-box-cliccabili-29-10)', () => {
  const q = decodeState(read('state-1-question.json'))
  const s = (name: string, extra: Partial<Session> = {}): Session => ({ id: name, name, account: 'personale', project: name, state: 'idle', since: 0, ...extra })
  const st = (sessions: Session[]): State => ({ ...q, ts: now, sessions })

  it('ti aspettano: le sessioni della cifra, la più vecchia prima, senza segreti', () => {
    expect(waitingRows(q)).toEqual([{ session: 'ledger-api', kind: 'question', text: "Deploy ready with DEPLOY_TOKEN=\u2022\u2022\u2022, waiting for the client's ok. Deploy now?", since: 1789210500, closesItself: false }])
    const x = st([s('a', { turn_blocking: { text: 'ok to publish', at: now - 60 } }), s('b', { needs_you: { reason: 'type the code', at: now - 600, until: true } }), s('c', { state: 'gone', needs_you: { reason: 'gone', at: now - 900 } })])
    expect(waitingRows(x).map(r => [r.session, r.kind, r.closesItself])).toEqual([['b', 'needs_you', true], ['a', 'blocking', false]])
    expect(waitingRows(x).length).toBe(head(x, null, now).waiting)
    expect(waitingRows(q).length).toBe(head(q, null, q.ts).waiting)
  })

  it('al lavoro: obiettivo, strumento, turno; poi le ferme senza niente per te', () => {
    expect(workingRows(q, q.ts)).toEqual([{ session: 'atlas-shop', goal: 'All checkout tests green and the release tagged', tool: 'Bash pytest -q tests', toolNote: 'Run the test suite', turnS: 100, model: 'Sonnet 5', context: 18 }])
    expect(workingRows(q, q.ts).length).toBe(head(q, null, q.ts).busy)
    expect(stillRows(q)).toEqual([{ session: 'field-notes', outcome: 'README rewritten', at: 1789121000 }])
  })

  it('esiti di oggi: i rossi in cima, il più nuovo prima, solo da mezzanotte', () => {
    const p: TimelinePage = { since: 0, sessions: [
      { session: 'a', live: true, events: [{ at: now - 300, kind: 'outcome', text: 'first' }, { at: now - 200, kind: 'task', text: 'check red', ok: false, ref: 't1' }, { at: now - 100, kind: 'task', text: 'check green', ok: true, ref: 't2' }] },
      { session: 'b', live: true, events: [{ at: now - 50, kind: 'outcome', text: 'second' }, { at: now - 30 * 3600, kind: 'outcome', text: 'yesterday' }] },
    ] }
    const o = outcomes(p, now, 'Europe/Rome')!
    expect(o.red).toEqual([{ session: 'a', text: 'check red', at: now - 200, ref: 't1' }])
    expect(o.done.map(d => [d.session, d.text])).toEqual([['b', 'second'], ['a', 'first']])
    expect(o.atLeast).toBe(false)
    const h = head(st([]), p, now, 'Europe/Rome')
    expect([o.done.length, o.red.length]).toEqual([h.outcomes, h.red])
    expect(outcomes(null, now)).toBeNull()
    expect(outcomes({ ...p, more: true }, now, 'Europe/Rome')!.atLeast).toBe(true)
  })
})

describe('contratto 1.96, op kpi (proposte 8, 9, 10, 12, 13, 17)', () => {
  const kid = sample.cmd.find((c: { op: string }) => c.op === 'kpi').id
  const data: KpiData = JSON.parse(sample.result.find((r: { id: string }) => r.id === kid).text)

  it('8 · barra piena da un giorno, i fermi prima', () => {
    expect(ageFraction(43200)).toBeCloseTo(0.5)
    expect(ageFraction(172800)).toBe(1)
    const t = data.kpi_tasks!
    expect([t.open, t.waiting_you, t.stalled, t.closed_today]).toEqual([3, 1, 1, 2])
    expect(tasksOrder(t).map(x => x.task)).toEqual(['atlas-release-2-4', 'ledger-vendor-ticket'])
    expect(tasksOrder({ ...t, tasks: t.tasks.map(x => ({ ...x, stalled: false })) }).map(x => x.task)).toEqual(['ledger-vendor-ticket', 'atlas-release-2-4'])
  })
  it('9 · i totali della settimana', () => {
    expect(checksTotals(data.kpi_checks_week!)).toEqual([33, 4])
    expect(checksTotals([])).toEqual([0, 0])
  })
  it('10 · i turni di una sessione e la sintesi', () => {
    const turns = turnsOf(data, 'atlas-shop')
    expect(turns).toHaveLength(3)
    expect(turnsOf(data, 'ledger-api')).toEqual([])
    expect(turnsOf(null, 'atlas-shop')).toEqual([])
    // 240 + 420 + 700 s = 22 min 40, arrotondati a 23; un fallito.
    expect(turnsSummary(turns)).toEqual({ n: 3, minutes: 23, failed: 1, tokensFirst: 18200, tokensLast: 41000, tokensMax: 41000 })
  })
  it('12 · la salute arriva intera', () => {
    const h = data.kpi_health!
    expect([h.latency?.p50_s, h.sessions_here, h.sessions_max, h.hosts[0].ram_pct]).toEqual([2.1, 4, 5, 71])
  })
  it('13 · il tasso di accettazione', () => {
    expect(acceptRate(data.kpi_suggestions_7d!)).toBe(50)
    expect(suggestionsTotal(data.kpi_suggestions_7d!)).toBe(14)
    expect(acceptRate({ accepted: 0, discussed: 0, later: 0, never: 0, expired: 0 })).toBeNull()
  })
  it('17 · attese chiuse e scala', () => {
    expect(waitsClosed(data.kpi_waits!)).toBe(5)
    expect(waitsTop(data.kpi_waits!)).toBe(11.5)
    expect(waitsTop({ now: [], week: [] })).toBe(0)
  })
})
