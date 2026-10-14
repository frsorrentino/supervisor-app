import { describe, expect, it } from 'vitest'
import type { Event, Session } from './contract'
import { green, openLimit, passArg, passTargets, ways } from './alertEvents'

// Contratti 1.57 e 1.58, con i testi della specifica di supervisor del 14/10 (le fixture arrivano con la 0.8.20).
const limit: Event = {
  key: 'k1', kind: 'quota_limit', session: 'atlas-shop', account: 'personal', ts: 1000, title: '⛔ atlas-shop: limite 5h finito, reset alle 18:00',
  body: '', ref: 'atlas-shop', reset_at: 5000, window: '5h', other_account: 'work', actions: ['other_account', 'at_reset', 'pass'],
}
const s = (name: string, state: Session['state'] = 'idle', turn: number | null = 900): Session =>
  ({ id: name, name, account: 'a', project: 'p', state, since: 1, turn_started: turn })
const w = { otherAccount: (a: string) => `continua su ${a}`, atReset: (t: string) => `riprendi alle ${t}`, pass: "passa a un'altra sessione" }

describe('eventi con tasti', () => {
  it('task_check verde o rosso', () => {
    const e: Event = { key: 'k', kind: 'task_check', ts: 1, title: '✓ t-1: check green', ref: 't-1' }
    expect(green(e)).toBe(true)
    expect(green({ ...e, title: '✗ t-1: check red' })).toBe(false)
  })
  it('quota_limit: le strade del relay, solo con i dati per il tasto', () => {
    expect(ways(limit, w, () => '18:00').map(x => x.label)).toEqual(['continua su work', 'riprendi alle 18:00', "passa a un'altra sessione"])
    expect(ways({ ...limit, other_account: null, reset_at: null, actions: ['other_account', 'at_reset', 'pass', 'x'] }, w, () => '').map(x => x.action)).toEqual(['pass'])
    expect(passArg('ledger-api')).toBe('pass:ledger-api')
    expect(passTargets([s('atlas-shop'), s('ledger-api'), s('old', 'gone')], 'atlas-shop')).toEqual(['ledger-api'])
  })
  it('resta fino al reset o a un turno nuovo', () => {
    expect(openLimit([limit], s('atlas-shop'), 2000)).toBe(limit)
    expect(openLimit([limit], s('atlas-shop'), 5000)).toBeNull()
    expect(openLimit([limit], s('atlas-shop', 'busy', 1200), 2000)).toBeNull()
  })
})
