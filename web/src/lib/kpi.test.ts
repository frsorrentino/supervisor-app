import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type KpiData, type Session, type State, type TimelineEvent, type TimelinePage } from './contract'
import { groupsBySession, groupsByType, midnight, outcomeType, acceptRate, ageFraction, cells, checksTotals, day, dayFigures, head, hourly, outcomes, split, stillRows, suggestionsTotal, tasksOrder, todayShare, turnsOf, turnsSummary, waitingRows, waitsClosed, waitsTop, workingRows, workSeconds } from './kpi'

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
    expect(workingRows(q, q.ts)).toEqual([{ session: 'atlas-shop', goal: 'Checkout tests green, release tagged', tool: 'Bash pytest -q tests', toolNote: 'Run the test suite', turnS: 100, model: 'Sonnet 5', context: 18 }])
    expect(workingRows(q, q.ts).length).toBe(head(q, null, q.ts).busy)
    expect(stillRows(q)).toEqual([{ session: 'field-notes', outcome: 'README rewritten', at: 1789121000 }])
  })

  it('chi lavora e intanto aspetta te conta solo in «ti aspettano» (app-home-riordino-29-10)', () => {
    const x = st([s('a', { state: 'busy', needs_you: { reason: 'type the code', at: now - 60 } }), s('b', { state: 'busy' })])
    const h = head(x, null, now)
    expect([h.waiting, h.busy]).toEqual([1, 1])
    expect(workingRows(x, now).map(w => w.session)).toEqual(['b'])
  })

  const esito = (at: number, type: string | null): TimelineEvent => ({ at, kind: 'outcome', text: `t${at}`, type })
  const zone = 'Europe/Rome'
  it('esiti_tipo: i gruppi hanno l\'ordine dato, i vuoti non ci sono', () => {
    const all = ['running', null, 'answer', 'done', 'shipped', 'waiting', 'problem'].map((x, i) => esito(now - i, x))
    const o = outcomes({ since: 0, sessions: [{ session: 'a', live: true, events: all }] }, now, zone)!
    expect(groupsByType(o.done).map(g => g.type)).toEqual(['problem', 'waiting', 'shipped', 'done', 'answer', 'none', 'running'])
    expect(groupsByType(o.done).filter(g => g.startsClosed).map(g => g.type)).toEqual(['running'])
    const few = outcomes({ since: 0, sessions: [{ session: 'a', live: true, events: [esito(now - 1, 'done')] }] }, now, zone)!
    expect(groupsByType(few.done).map(g => g.type)).toEqual(['done'])
  })
  it('esiti_tipo: null e sconosciuto vanno in Senza tipo', () => {
    expect([outcomeType(null), outcomeType(undefined), outcomeType('pubblicato'), outcomeType('none'), outcomeType('shipped'), outcomeType('running')]).toEqual(['none', 'none', 'none', 'none', 'shipped', 'running'])
  })
  it('esiti_tipo: per sessione «in corso» chiuso in fondo e di tutte', () => {
    const o = outcomes({ since: 0, sessions: [
      { session: 'a', live: true, events: [esito(now - 50, 'running'), esito(now - 60, null)] },
      { session: 'b', live: true, events: [esito(now - 10, 'done'), esito(now - 5, 'running')] },
    ] }, now, zone)!
    const g = groupsBySession(o.done)
    expect(g.map(x => x.session)).toEqual(['b', 'a', null])
    expect(g.map(x => x.startsClosed)).toEqual([false, false, true])
    expect(g[2].rows.map(r => r.at)).toEqual([now - 5, now - 50])
    expect(g.map(x => x.rows.length)).toEqual([1, 1, 2])
  })
  it('esiti_tipo: il numero non conta «in corso» e conta i senza tipo, dagli eventi', () => {
    const p: TimelinePage = { since: 0, sessions: [{ session: 'a', live: true, events: [esito(now - 1, 'running'), esito(now - 2, null), esito(now - 3, 'done')] }] }
    expect(head(st([]), p, now, zone).outcomes).toBe(2)
    const o = outcomes(p, now, zone)!
    expect([o.settled, o.done.length, o.settledRows.length]).toEqual([2, 3, 2])
  })
  it('esiti_tipo: i conti del relay valgono solo da mezzanotte', () => {
    const today = midnight(now, zone)
    const sessions = [{ session: 'a', live: true, events: [esito(now - 1, 'running'), esito(now - 2, 'done')] }]
    const p: TimelinePage = { since: today, sessions, more: true, outcomes_n: 9, outcomes_settled_n: 7 }
    expect(head(st([]), p, now, zone).outcomes).toBe(7)
    expect(outcomes(p, now, zone)).toMatchObject({ settled: 7, cut: true, atLeast: false })
    const older = { ...p, since: today - 3600 }
    expect(head(st([]), older, now, zone).outcomes).toBe(1)
    expect(outcomes(older, now, zone)).toMatchObject({ settled: 1, cut: false, atLeast: true })
    expect(head(st([]), { since: today, sessions }, now, zone).outcomes).toBe(1)
    expect(outcomes({ since: today, sessions, outcomes_n: 2, outcomes_settled_n: 1 }, now, zone)!.cut).toBe(false)
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
    expect([h.latency?.p50_s, h.latency?.p95_s, h.sessions_here, h.sessions_max, h.hosts[0].ram_pct]).toEqual([2.1, 6.4, 4, 5, 71])
    expect(h.commands).toEqual({ p50_s: 0.4, p95_s: 2.6, n: 268 })
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

// app-tabellone-frazioni-13-11 (Franz 13/11 13:56): il denominatore delle frazioni del tabellone.
describe('tabellone: le sessioni aperte, denominatore delle frazioni', () => {
  it('conta le sessioni vive senza la master, le stesse fra cui si contano ferme e al lavoro; le chiuse no', async () => {
    const { head } = await import('./kpi')
    const s = (name: string, state: string, extra: Partial<Session> = {}): Session => ({ id: name, name, account: 'p', state, since: 0, ...extra } as Session)
    const st = { v: 1, ts: 100, sessions: [s('a', 'busy'), s('b', 'waiting', { question: { id: 'q', text: '?', asked_at: 50, options: [] } } as Partial<Session>), s('c', 'idle'), s('d', 'gone'), s('master', 'busy')], quota: {} } as unknown as State
    const h = head(st, null, 100, 'UTC')
    // La master al lavoro non conta né sopra né sotto la riga della frazione.
    expect([h.open, h.busy, h.waiting]).toEqual([3, 1, 1])
    expect(head({ ...st, sessions: [s('d', 'gone')] } as State, null, 100, 'UTC').open).toBe(0)
  })
})

// app-tabellone-filtri-14-11: gli stessi casi di BoardFilterTest.kt.
describe('tabellone_filtri', async () => {
  const { boardMatch, boardShown, boardTap } = await import('./kpi')
  type F = 'aspetta' | 'lavoro'
  const c = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/board-filter-cases.json', import.meta.url), 'utf8')) as {
    state: unknown; quiet: unknown; aspetta: string[]; lavoro: string[]
    taps: { name: string; cur: F | null; tap: F; out: F | null }[]; quietTaps: { name: string; cur: F | null; tap: F; out: F | null }[]; quietShown: { name: string; cur: F | null; out: F | null }[]
  }
  const st = decodeState(JSON.stringify(c.state)), quiet = decodeState(JSON.stringify(c.quiet))
  const names = (f: F) => st.sessions.filter(s => boardMatch(s, f)).map(s => s.name)
  it('la lista filtrata è quella contata dal tabellone', () => {
    const h = head(st, null, 1000)
    expect(names('aspetta')).toEqual(c.aspetta); expect(names('lavoro')).toEqual(c.lavoro)
    expect([h.waiting, h.busy]).toEqual([c.aspetta.length, c.lavoro.length])
  })
  for (const x of c.taps) it(x.name, () => expect(boardTap(x.cur, x.tap, head(st, null, 1000))).toBe(x.out))
  for (const x of c.quietTaps) it(x.name, () => expect(boardTap(x.cur, x.tap, head(quiet, null, 1000))).toBe(x.out))
  for (const x of c.quietShown) it(x.name, () => expect(boardShown(x.cur, head(quiet, null, 1000))).toBe(x.out))
})
