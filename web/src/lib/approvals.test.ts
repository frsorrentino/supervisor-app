import { describe, expect, it } from 'vitest'
import type { Approval, Commitment, State } from './contract'
import { settled, withoutApproved } from './approvals'

// Gli stessi casi di ApprovalsTest in Kotlin.
const base: State = { v: 1, ts: 1_000_000, host: 'pc', sessions: [], quota: {}, projects: [], night: { queued: 0 }, recap: { date: '', items: [] } }
const ok = (task: string): Approval => ({ task, title: task, what: '', where: '', deploy: false, requested_at: 10 })
const c = (task: string, state: string, waitsOn: string | null = 'person'): Commitment =>
  ({ task, title: task, state, approved: state === 'approved', waits: waitsOn ? { on: waitsOn } : null } as unknown as Commitment)

describe('Approva ottimistica (Franz 23/10 21:24: «dopo aver approvato non deve apparire nulla del genere»)', () => {
  it('al tocco la voce sparisce subito: niente ok e impegno già approvato, senza attesa della persona', () => {
    const st: State = { ...base, approvals: [ok('a'), ok('b')], commitments: [c('a', 'awaiting_ok'), c('b', 'awaiting_ok'), c('x', 'running', null)] }
    const v = withoutApproved(st, new Set(['a']))
    expect(v.approvals!.map(a => a.task)).toEqual(['b'])
    expect(v.commitments!.find(x => x.task === 'a')).toMatchObject({ approved: true, state: 'approved', waits: null })
    expect(v.commitments!.find(x => x.task === 'b')).toMatchObject({ approved: false, state: 'awaiting_ok' })
    expect(withoutApproved(st, new Set())).toBe(st)
  })
  it('settled: i compiti che il registro ha già tolto dagli ok si dimenticano, gli altri restano nascosti', () => {
    const st: State = { ...base, approvals: [ok('b')], commitments: [c('a', 'approved', null), c('b', 'awaiting_ok')] }
    expect(settled(st, new Set(['a', 'b']))).toEqual(new Set(['a']))
  })
})
