import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type AgendaPage, type Approval, type Commitment, type Event } from './contract'
import { MIN_BAR, axis, bars, done, doneFromPages, forYou, inProgress, midnight } from './lavori'
import type { DonePage } from './contract'

// Gli stessi casi di LavoriTest in Kotlin: la pagina unica «Lavori» (mockup approvato il 22/10).
const fixture = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')
const zone = 'Europe/Rome'
const idle = decodeState(fixture('state-2-idle.json'))
const stale = decodeState(fixture('state-3-stale.json'))
const night = JSON.parse(fixture('night-report-sample.json')) as { commitments: Commitment[] }
const events = JSON.parse(fixture('events-sample.json')) as Event[]
const root = JSON.parse(fixture('cmd-result-sample.json'))
const agenda: AgendaPage = JSON.parse(root.result.find((r: { id: string }) => r.id.endsWith('0386')).text)
const today = '2026-10-13'
const okRelease: Approval = { task: 'ledger-release-14', title: 'Release 1.4 di ledger-api', what: 'tag v1.4 e push', where: 'github.com/example/ledger-api', deploy: false, requested_at: 1791332460 }
const okLoose: Approval = { task: 'loose-task', title: 'Loose ok', what: 'push', where: 'master', deploy: false, requested_at: 1789214000 }
const st = { ...idle, commitments: [...night.commitments, ...idle.commitments!], approvals: [okRelease, okLoose] }
const eventDay = '2026-09-12'

describe('lavori', () => {
  it('le fixture danno i quattro blocchi', () => {
    const fy = forYou(st, agenda, idle.ts, today)
    expect(fy.ok.map(r => r.title)).toEqual(['Loose ok', 'Release 1.4 di ledger-api'])
    expect([fy.ok[0].approval, fy.ok[0].commitment, fy.ok[0].who]).toEqual([okLoose, null, 'master'])
    expect([fy.ok[1].commitment!.task, fy.ok[1].approval, fy.ok[1].who]).toEqual(['ledger-release-14', okRelease, 'ledger-api'])
    expect(fy.outside.map(r => r.title)).toEqual(['Confirm the 6 client ids with a candidate'])
    expect([fy.outside[0].who, fy.outside[0].kind, fy.outside[0].agenda]).toEqual([agenda.rows[0].ref, 'outside', agenda.rows[0]])
    expect(fy.claude.map(r => [r.title, r.kind])).toEqual([['Move the docs site to the new host', 'claude']])
    expect(fy.total).toBe(4)
    const ip = inProgress(st, agenda, idle.ts, zone, today)
    expect(ip.active.map(w => w.row!.c.task)).toEqual(['notes-gantt-apk', 'ledger-migrations-011', 'atlas-seo-pages'])
    expect(ip.active.every(w => w.bars.length === 1 && !w.bars[0].dashed)).toBe(true)
    expect(ip.paused).toEqual([])
    expect(ip.others.map(w => w.agenda!.title)).toEqual(['Staging of atlas-shop not reachable from the CLI'])
    const d = done(events, zone, eventDay)
    expect(d.length).toBe(1); expect(d[0].date).toBe(eventDay)
    expect(d[0].rows).toEqual([{ task: 'atlas-release-2-4', title: 'Release 2.4 of atlas-shop', session: 'atlas-shop', at: 1789199700, proof: null }])
  })

  it('un ok non compare mai due volte', () => {
    const fy = forYou({ ...idle, commitments: night.commitments, approvals: [okRelease] }, null, idle.ts, today)
    expect(fy.ok.length).toBe(1); expect([fy.ok[0].approval, fy.ok[0].commitment!.task]).toEqual([okRelease, 'ledger-release-14'])
  })

  it('una pausa senza person resta in corso', () => {
    const s = { ...stale, commitments: [{ ...stale.commitments![0], waits: { who: 'person', on: 'orbit-docs' } }] } // relay 1.76
    expect(forYou(s, null, stale.ts, today).total).toBe(0)
    const ip = inProgress(s, null, stale.ts, zone, today)
    expect(ip.active).toEqual([]); expect(ip.paused.map(w => w.row!.c.task)).toEqual(['orbit-docs-site'])
  })

  it('una pausa che aspetta la persona è per te', () => {
    const c = stale.commitments![0] // 1.77: waits.on = person, paused.at = l'inizio vero
    const fy = forYou(stale, null, stale.ts, today)
    expect(fy.ok).toEqual([])
    expect(fy.outside.length).toBe(1)
    expect([fy.outside[0].title, fy.outside[0].who, fy.outside[0].at, fy.outside[0].commitment]).toEqual(["after the client's review", 'orbit-docs', 1789100000, c])
    expect(inProgress(stale, null, stale.ts, zone, today).paused.map(w => w.row!.c.task)).toEqual(['orbit-docs-site'])
  })

  it("l'asse copre sette giorni fino a domani", () => {
    const ax = axis(idle.ts, zone) // 12/09/2026 14:00 a Roma
    expect(ax.start).toBe(Date.UTC(2026, 8, 5, 22) / 1000)
    expect(ax.end).toBe(Date.UTC(2026, 8, 12, 22) / 1000)
    expect(ax.days.map(d => d[0])).toEqual(['6', '7', '8', '9', '10', '11', '12'])
    expect(ax.days[1][1]).toBeCloseTo(1 / 7, 9)
    expect(ax.now).toBeCloseTo((6 * 24 + 14) / (7 * 24), 9)
    expect(midnight('2026-09-06', zone)).toBe(ax.start)
  })

  it("una barra più vecchia dell'asse parte dal bordo", () => {
    const ax = axis(idle.ts, zone)
    const c = { ...idle.commitments![1], approved_at: ax.start - 86400 }
    expect(bars(c, ax, idle.ts)).toEqual([{ from: 0, to: ax.now, dashed: false }])
  })

  it('una barra in pausa è piena poi tratteggiata', () => {
    const ax = axis(stale.ts, zone)
    const f = (t: number) => (t - ax.start) / (ax.end - ax.start)
    const fixture = stale.commitments![0] // ok 1789120000, pausa dal 1789100000: in pausa da prima dell'ok, tutta tratteggiata
    expect(bars(fixture, ax, stale.ts)).toEqual([{ from: f(1789120000), to: f(stale.ts), dashed: true }])
    const c = { ...fixture, paused: { ...fixture.paused!, at: 1789190000 } }
    expect(bars(c, ax, stale.ts)).toEqual([{ from: f(1789120000), to: f(1789190000), dashed: false }, { from: f(1789190000), to: f(stale.ts), dashed: true }])
    const old = { ...c, approved_at: ax.start - 2 * 86400, paused: { ...c.paused!, at: ax.start - 86400 } }
    expect(bars(old, ax, stale.ts)).toEqual([{ from: 0, to: f(stale.ts), dashed: true }])
  })

  it('una barra appena nata ha una larghezza minima', () => {
    const ax = axis(idle.ts, zone)
    const [b] = bars({ ...idle.commitments![1], approved_at: idle.ts - 10 }, ax, idle.ts)
    expect(b.to - b.from).toBeCloseTo(MIN_BAR, 9); expect(b.to <= 1).toBe(true)
  })

  it("fatto tiene l'ultimo controllo di un compito", () => {
    const later: Event = { key: 'x_1', kind: 'task_check', session: 'ledger-api', ts: 1789200360, title: '✓ ledger-migrations-011: check green', body: 'Migrations 008-011 on staging\nall green', ref: 'ledger-migrations-011' }
    const earlier: Event = { key: 'x_0', kind: 'task_check', session: 'atlas-shop', ts: 1789199000, title: '✓ atlas-release-2-4: check green', body: 'Release 2.4 of atlas-shop', ref: 'atlas-release-2-4' }
    const [day] = done([...events, later, earlier], zone, eventDay)
    expect(day.rows.map(r => [r.task, r.at])).toEqual([['ledger-migrations-011', 1789200360], ['atlas-release-2-4', 1789199700]])
    expect(day.rows[0].title).toBe('Migrations 008-011 on staging')
    expect(done(events, zone, '2026-09-19')).toEqual([])
    expect(done(events, zone, '2026-09-13').length).toBe(1)
  })

  it("l'op done porta la prova e i giorni prima", () => {
    const page: DonePage = JSON.parse(root.result.find((r: { id: string }) => r.id.endsWith('0760')).text)
    expect([page.date, page.rows.length, page.days]).toEqual(['2026-02-10', 4, [{ date: '2026-02-09', n: 3 }, { date: '2026-02-07', n: 1 }]])
    const days = doneFromPages({ [page.date]: page }, zone)
    expect(days.map(d => [d.date, d.n, d.rows.length])).toEqual([['2026-02-10', 4, 4], ['2026-02-09', 3, 0], ['2026-02-07', 1, 0]])
    const rows = days[0].rows
    expect(rows.map(r => r.task)).toEqual(['atlas-release-2-3', 'ledger-migrations-010', 'notes-night-report', 'orbit-docs-lint'])
    expect(rows.map(r => r.proof)).toEqual(['release v2.3', 'commit 9f8e7d6', 'docs/notte/2026-02-10-field-notes.md', 'npm run lint'])
    expect([rows[0].url, rows[2].url]).toEqual(['https://github.com/example/atlas-shop/releases/tag/v2.3', null])
    expect(rows.map(r => r.outcome)).toEqual(['green', 'green', 'cancelled', 'failed'])
    expect([rows[0].title, rows[0].session, rows[2].session, rows[0].at]).toEqual(['Release 2.3 of atlas-shop', 'atlas-shop', null, 1789211400])
    const older = { ...page, date: '2026-02-09', rows: page.rows.slice(0, 3), days: [] }
    const merged = doneFromPages({ [page.date]: page, [older.date]: older }, zone)
    expect(merged.map(d => d.n)).toEqual([4, 3, 1]); expect(merged[1].rows.length).toBe(3)
  })

  it('con un relay vecchio niente righe', () => {
    const s = { ...idle, commitments: null }
    expect(forYou(s, null, idle.ts, today).total).toBe(0)
    const ip = inProgress(s, null, idle.ts, zone, today)
    expect([ip.active, ip.paused, ip.others]).toEqual([[], [], []])
    expect(done(events, zone, eventDay).length).toBe(1)
  })
})
