import { describe, expect, it } from 'vitest'
import type { Session, State } from './contract'
import { sessionMenu, unseenResults } from './sessionMenu'

// Un menu della sessione, lo stesso da home, chat e plancia (app-menu-sessione-03-11). Gli stessi casi di SessionMenuTest.kt.
const s = (x: Partial<Session> = {}): Session => ({ id: 'kb', name: 'kb', account: 'personal', project: 'p', state: 'idle', since: 0, link: '', handoff: null, ...x }) as Session
const st = (ss: Session[], x: Partial<State> = {}): State => ({ v: 1, ts: 0, host: 'pc', sessions: ss, ...x }) as State
const kinds = (m: { kind: string }[]) => m.map(i => i.kind)

describe('sessionMenu', () => {
  it('ogni voce al suo posto quando la sessione ha tutto', () => {
    const kb = s({ link: 'https://claude.ai/code/x', handoff: '/fable-director:handoff' })
    const m = sessionMenu(kb, st([kb, s({ name: 'master' })], { slash: ['clear'] }), { prove: 2, decidi: 1, guarda: 3, seguiti: 1 }, { changes: true, canExit: true })
    expect(kinds(m)).toEqual(['changes', 'prove', 'decidi', 'guarda', 'seguiti', 'follow', 'ask_master', 'open_in_claude', 'restart_clean', 'exit'])
    expect(m.filter(i => i.n != null).map(i => i.n)).toEqual([2, 1, 3, 1])
  })
  it('le voci vuote non compaiono', () => {
    const kb = s()
    expect(kinds(sessionMenu(kb, st([kb]), { prove: 0 }))).toEqual(['follow'])
  })
  it('la master non si fa controllare da sé, e senza master aperta la voce non c’è', () => {
    const m = s({ name: 'master' })
    expect(kinds(sessionMenu(m, st([m])))).toEqual(['follow'])
    expect(kinds(sessionMenu(s(), st([s(), s({ name: 'master', state: 'gone' })])))).toEqual(['follow'])
  })
  it('Riparti pulito vuole un comando di handoff e /clear', () => {
    const kb = s({ handoff: '/h' })
    expect(kinds(sessionMenu(kb, st([kb])))).not.toContain('restart_clean')
    expect(kinds(sessionMenu(s(), st([s()], { slash: ['clear'] })))).not.toContain('restart_clean')
    expect(kinds(sessionMenu(kb, st([kb], { slash: ['clear'] })))).toContain('restart_clean')
  })
  it('una sessione chiusa tiene le voci che funzionano ancora, ma non riparte e non si chiude', () => {
    const gone = s({ state: 'gone', link: 'https://claude.ai/code/x', handoff: '/h' })
    expect(kinds(sessionMenu(gone, st([gone], { slash: ['clear'] }), {}, { canExit: true }))).toEqual(['follow', 'open_in_claude'])
  })
  it('i risultati non visti contano solo quelli di questa sessione', () => {
    const item = (id: string, session: string, seen_at: number | null = null) => ({ id, session, title: '', kind: 'other', path: '', at: 0, version: 1, versions: 1, seen_at })
    const r = { n_unseen: 4, items: [item('1', 'kb'), item('2', 'kb', 5), item('3', 'web'), item('4', 'kb')] }
    expect(unseenResults(st([s()], { results: r } as Partial<State>), 'kb')).toBe(2)
    expect(unseenResults(st([s()]), 'kb')).toBe(0)
  })
})
