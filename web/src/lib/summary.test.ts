import { describe, expect, it } from 'vitest'
import type { Approval, Commitment, Session, State } from './contract'
import { attachedTasks, build } from './summary'

// Gli stessi casi di SummaryTest in Kotlin (la parte degli ok nella scheda).
const now = 1_000_000
const s = (name: string, state: Session['state'] = 'idle'): Session =>
  ({ id: name, name, account: 'personale', project: name, state, since: 0, question: null, outcome: null, followed: false })
const st = (...sessions: Session[]): State => ({ v: 1, ts: now, host: 'pc', sessions, quota: {}, projects: [], night: { queued: 0 }, recap: { date: '', items: [] } })
const ok = (task: string, title: string, at: number): Approval => ({ task, title, what: '', where: '', deploy: false, requested_at: at })
const c = (task: string, p: Partial<Commitment>): Commitment => ({ task, title: task, state: 'awaiting_ok', approved: false, ...p } as Commitment)

describe('Summary: gli ok nella scheda della sessione', () => {
  // Franz 23/10 19:34: «i tasti di riapprovazione starebbero meglio nelle schede della sessione».
  it('ogni ok sta nella card della sessione che lo chiede (impegno per sessione o per progetto), dal più vecchio; una chiusa non li prende', () => {
    const a1 = ok('t1', 'supervisor-app: uno', now - 100)
    const a2 = ok('t2', 'supervisor-app: due', now - 50)
    const a3 = ok('t3', 'curriculum: ok', now - 80)
    const a4 = ok('t4', 'orfano', now - 60)
    const state: State = { ...st(s('supervisor-app', 'waiting'), s('curriculum'), s('gone', 'gone')), approvals: [a2, a1, a3, a4], commitments: [
      c('t1', { session: 'supervisor-app' }), c('t2', { project: '/home/franz/personali/supervisor-app' }), c('t3', { session: 'curriculum' }), c('t4', { session: 'gone' }),
    ] }
    const m = build(state, [], now, new Set())
    expect(m.rows.find(r => r.session.name === 'supervisor-app')!.approvals).toEqual([a1, a2])
    expect(m.rows.find(r => r.session.name === 'curriculum')!.approvals).toEqual([a3])
    expect(attachedTasks(m.rows)).toEqual(new Set(['t1', 't2', 't3']))
  })

  // Franz 24/10 18:49 (ok-master-dove-24-10): la master ha una scheda solo con un ok o una richiesta per Franz.
  it('gli ok della master nella sua scheda, fra quelle che aspettano; senza niente per Franz la master resta fuori', () => {
    const a1 = { ...ok('ads', 'Ads Widget su Play', now - 300), options: ['Non etichettare', 'Etichetta come AI'] }
    const q = { id: 'q', text: '2', options: [], asked_at: now - 200 } as unknown as Session['question']
    const base = st(s('master', 'busy'), { ...s('a', 'waiting'), question: q })
    expect(build(base, [], now, new Set()).rows.map(r => r.session.name)).toEqual(['a'])
    const m = build({ ...base, approvals: [a1], commitments: [c('ads', { session: 'master' })] }, [], now, new Set())
    expect(m.rows.map(r => r.session.name)).toEqual(['master', 'a'])
    expect(m.rows[0].approvals).toEqual([a1])
    expect(attachedTasks(m.rows)).toEqual(new Set(['ads']))
    const blocked = build(st({ ...s('master'), turn_blocking: { text: 'ok al push', at: now - 10 } } as Session), [], now, new Set())
    expect(blocked.rows.map(r => [r.session.name, r.text])).toEqual([['master', 'ok al push']])
  })
})

