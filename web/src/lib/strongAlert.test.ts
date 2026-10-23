import { describe, expect, it } from 'vitest'
import type { Session, State } from './contract'
import { forMaster, stop } from './strongAlert'
import { build } from './summary'
import { alert } from './elsewhere'

// Gli stessi casi di StrongAlertTest e SummaryTest (contratto 1.68).
const s = (name: string, x: Partial<Session> = {}): Session => ({ id: name, name, account: 'personale', project: name, state: 'idle', since: 0, ...x })
const st = (sessions: Session[], attention: State['attention'] = null) => ({ v: 1, ts: 0, host: 'pc', sessions, quota: {}, attention } as unknown as State)

describe('avviso forte (1.68)', () => {
  it('needs_you prima della voce bloccante; una chiusa non è ferma per te', () => {
    expect(stop(s('kb', { needs_you: { reason: 'login su win', at: 20 }, turn_blocking: { text: 'ok al deploy', at: 10 } }))).toEqual({ text: 'login su win', at: 20 })
    expect(stop(s('kb', { turn_blocking: { text: 'ok al deploy', at: 10 } }))).toEqual({ text: 'ok al deploy', at: 10 })
    expect(stop(s('kb', { needs_you: { reason: ' ', at: 5 } }))).toBeNull()
    expect(stop(s('kb', { state: 'gone', needs_you: { reason: 'login', at: 1 } }))).toBeNull()
  })

  it('in cima con le domande, col motivo, dalla più vecchia; una volta sola', () => {
    const m = build(st([
      s('busy', { state: 'busy' }),
      s('asks', { state: 'waiting', question: { id: '1', kind: 'ask', text: 'Pubblico?', options: [], tier: 'low', asked_at: 14 } }),
      s('login', { needs_you: { reason: 'login su win', at: 13 } }),
      s('deploy', { turn_blocking: { text: 'ok al deploy', at: 15 }, outcome: { short: 'ok', full: 'ok', at: 15 }, followed: true }),
    ]), [], 20, new Set())
    expect(m.rows.map(r => r.session.name)).toEqual(['login', 'asks', 'deploy', 'busy'])
    expect(m.rows.map(r => r.group)).toEqual(['waiting', 'waiting', 'waiting', 'working'])
    expect(m.rows.slice(0, 3).map(r => r.text)).toEqual(['login su win', 'Pubblico?', 'ok al deploy'])
  })

  it('il banner della master conta solo le altre sessioni', () => {
    expect(forMaster(st([], [
      { kind: 'question', session: 'kb', ref: 'q1', title: '?', since: 1 }, { kind: 'needs_you', session: 'master', ref: 'master', title: 'login', since: 2 },
      { kind: 'task_red', session: null, ref: 't1', title: 'rosso', since: 3 },
    ])).length).toBe(2)
    expect(forMaster(st([])).length).toBe(0)
  })

  it('l\'avviso delle altre sessioni conta chi è ferma per te', () => {
    expect(alert(st([s('kb', { needs_you: { reason: 'login', at: 5 } }), s('open')]), 'open', 10, new Set(), new Set())).toEqual({ type: 'waiting', sessions: ['kb'], requests: ['w:kb@5'] })
  })
})
