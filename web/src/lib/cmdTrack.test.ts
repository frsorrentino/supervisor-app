import { describe, expect, it } from 'vitest'
import type { CmdResult, Session } from './contract'
import { MAX_S, final, phase } from './cmdTrack'
import { DemoTransport } from './demoTransport'

// Gli stessi casi di CmdTrackTest in Kotlin (app-tasti-riscontro-20-11, i comandi appesi).
const ok: CmdResult = { id: 'c', ok: true, text: 'interrupted', at: 0 }
const queued: CmdResult = { id: 'c', ok: true, text: 'queued: kb is working, it reads it when the turn ends', at: 0 }
const s = (state: Session['state'], started?: number): Session => ({ id: 'kb', name: 'kb', account: 'personale', project: 'kb', state, since: 0, turn_started: started })

describe('a che punto è un comando', () => {
  it('nei primi secondi: Mando al PC', () => expect(phase(2, null, null)).toBe('sending'))
  it('ancora sul bus: non ancora preso', () => expect(phase(6, null, false)).toBe('not_taken'))
  it('preso senza esito: ci sta lavorando', () => expect(phase(25, null, true)).toBe('working'))
  it('senza sapere: resta Mando al PC', () => expect(phase(25, null, null)).toBe('sending'))
  it("l'esito tardi vale come fatto", () => expect(phase(40, ok, true)).toBe('done'))
  it('il no del PC', () => expect(phase(3, { id: 'c', ok: false, text: 'kb is not running', at: 0 }, null)).toBe('refused'))
  it('oltre il massimo: nessuna risposta', () => {
    expect(phase(MAX_S - 1, null, true)).toBe('working')
    expect(phase(MAX_S, null, true)).toBe('no_answer')
    expect(final('no_answer')).toBe(true)
  })
  it('in coda finché la sessione non lo prende', () => {
    expect(phase(5, queued, true, s('busy', 900), 1000)).toBe('queued')
    expect(phase(60, queued, true, s('busy', 1050), 1000)).toBe('delivered')
    expect(phase(60, queued, true, s('idle'), 1000)).toBe('delivered')
    expect(phase(60, queued, true, s('waiting', 900), 1000)).toBe('queued')
    expect(final('queued')).toBe(false)
  })
  it('il comando appeso della demo: in /cmd 8 s, preso, esito dopo 30 s', async () => {
    let clock = 1000
    const bus = new DemoTransport(() => clock)
    void bus.send({ id: 'hang-1', op: 'interrupt', session: 'storefront', arg: null, issued: clock, by: 'web' } as never).catch(() => {})
    clock += 5
    expect(await bus.taken('hang-1')).toBe(false)
    expect(phase(5, await bus.peekResult('hang-1'), await bus.taken('hang-1'))).toBe('not_taken')
    clock += 5
    expect(phase(10, await bus.peekResult('hang-1'), await bus.taken('hang-1'))).toBe('working')
    clock += 25
    expect(phase(35, await bus.peekResult('hang-1'), await bus.taken('hang-1'))).toBe('done')
    expect(bus.state.sessions.find(x => x.name === 'storefront')!.state).toBe('idle')
  })
})
