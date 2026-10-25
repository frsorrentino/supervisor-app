import { describe, expect, it } from 'vitest'
import type { Event, Session } from './contract'
import { cleanEvent, green, openFailure, openLimit, passArg, passTargets, ways } from './alertEvents'

// Contratti 1.57 e 1.58, con i testi della specifica di supervisor del 14/10 (le fixture arrivano con la 0.8.20).
const limit: Event = {
  key: 'k1', kind: 'quota_limit', session: 'atlas-shop', account: 'personal', ts: 1000, title: '⛔ atlas-shop: limite 5h finito, reset alle 18:00',
  body: '', ref: 'atlas-shop', reset_at: 5000, window: '5h', other_account: 'work', actions: ['other_account', 'at_reset', 'pass'],
}
const s = (name: string, state: Session['state'] = 'idle', turn: number | null = 900): Session =>
  ({ id: name, name, account: 'a', project: 'p', state, since: 1, turn_started: turn })
const w = { otherAccount: (a: string) => `continua su ${a}`, atReset: (t: string) => `riprendi alle ${t}`, pass: "passa a un'altra sessione", otherModel: (m: string) => `continua con ${m}` }

describe('eventi con tasti', () => {
  it('task_check verde o rosso', () => {
    const e: Event = { key: 'k', kind: 'task_check', ts: 1, title: '✓ t-1: check green', ref: 't-1' }
    expect(green(e)).toBe(true)
    expect(green({ ...e, title: '✗ t-1: check red' })).toBe(false)
    // Contratto 1.78: l'esito sta in `ok` e il titolo non porta più il segno; `ok` vince sul titolo.
    expect(green({ ...e, title: 't-1: check green', ok: true })).toBe(true)
    expect(green({ ...e, title: 't-1: check red', ok: false })).toBe(false)
    expect(green({ ...e, ok: false })).toBe(false)
  })

  // Franz, 22/10 13:36: niente emoji nell'app: l'evento si pulisce all'arrivo e l'esito del «✓» passa in `ok`.
  it('un evento arriva senza simboli e con l\'esito in ok', () => {
    const c = cleanEvent({ ...limit, body: '🚀 deploy fatto', preview: { title: '✓ x', body: 'token ••• 🎉' } })
    expect(c.title).toBe('atlas-shop: limite 5h finito, reset alle 18:00'); expect(c.body).toBe('deploy fatto')
    expect(c.preview).toEqual({ title: 'x', body: 'token •••' }); expect(c.ok).toBeUndefined()
    const old = cleanEvent({ key: 'k', kind: 'task_check', ts: 1, title: '✓ t-1: check green', ref: 't-1' })
    expect(old.title).toBe('t-1: check green'); expect(old.ok).toBe(true); expect(green(old)).toBe(true)
    expect(cleanEvent({ key: 'k', kind: 'task_check', ts: 1, title: 't-1: check red', ok: false, ref: 't-1' }).ok).toBe(false)
  })
  it('quota_limit: le strade del relay, solo con i dati per il tasto', () => {
    expect(ways(limit, w, () => '18:00').map(x => x.label)).toEqual(['continua su work', 'riprendi alle 18:00', "passa a un'altra sessione"])
    expect(ways({ ...limit, other_account: null, reset_at: null, actions: ['other_account', 'at_reset', 'pass', 'x'] }, w, () => '').map(x => x.action)).toEqual(['pass'])
    // Limite di un modello: il tasto «continua con Opus 5.5», arg other_model; senza il campo il tasto non c'è.
    const model = { ...limit, window: 'model', other_account: null, other_model: 'Opus 5.5', actions: ['other_model', 'at_reset'] }
    expect(ways(model, w, () => '18:00').map(x => [x.label, x.arg])).toEqual([['continua con Opus 5.5', 'other_model'], ['riprendi alle 18:00', 'at_reset']])
    expect(ways({ ...model, other_model: null }, w, () => '18:00').map(x => x.action)).toEqual(['at_reset'])
    expect(passArg('ledger-api')).toBe('pass:ledger-api')
    expect(passTargets([s('atlas-shop'), s('ledger-api'), s('old', 'gone')], 'atlas-shop')).toEqual(['ledger-api'])
  })
  it('resta fino al reset o a un turno nuovo', () => {
    expect(openLimit([limit], s('atlas-shop'), 2000)).toBe(limit)
    expect(openLimit([limit], s('atlas-shop'), 5000)).toBeNull()
    expect(openLimit([limit], s('atlas-shop', 'busy', 1200), 2000)).toBeNull()
  })
})

describe('turno fallito (contratto 1.61)', () => {
  const f: Event = { key: 'k', kind: 'turn_failed', session: 'atlas-shop', ts: 1000, title: '⚠ atlas-shop: turn failed — 529', ref: 'atlas-shop', reason: '529', continue_text: 'carry on from where you stopped' }
  it('«Continua» finché la sessione non riparte', () => {
    expect(openFailure([f], s('atlas-shop', 'idle', 900), 1060)).toBe(f)
    expect(openFailure([f], s('atlas-shop', 'idle', 1100), 1160)).toBeNull()
    expect(openFailure([f], s('atlas-shop', 'idle', 900), 1000 + 7 * 3600)).toBeNull()
  })
})

// 16/10 (Franz alle 16:03, «va corretto ora»): «master: turno fallito» con Continua tornava sotto risposte riuscite, perché a
// turno finito turn_started è null. Si chiude anche con un esito dopo l'evento, senza la voce in attention, con settled_at.
describe('turno fallito chiuso', () => {
  const f: Event = { key: 'k', kind: 'turn_failed', session: 'atlas-shop', ts: 1000, title: '⚠ atlas-shop: turn failed — 529', ref: 'atlas-shop', reason: '529', continue_text: 'carry on' }
  const done = (at: number): Session => ({ ...s('atlas-shop', 'idle', null), outcome: { short: 'ok', full: 'ok', at } })
  const voice = [{ kind: 'turn_failed', session: 'atlas-shop', ref: 'atlas-shop', title: '⚠', since: 1000 }]
  it('turno fallito chiuso da un esito dopo l\'evento', () => {
    expect(openFailure([f], done(900), 1060)).toBe(f)
    expect(openFailure([f], done(1100), 1160)).toBeNull()
  })
  it('turno fallito chiuso quando attention non ha più la voce', () => {
    expect(openFailure([f], s('atlas-shop', 'idle', null), 1060, voice)).toBe(f)
    expect(openFailure([f], s('atlas-shop', 'idle', null), 1060, [])).toBeNull()
    expect(openFailure([f], s('atlas-shop', 'idle', null), 1060, [{ ...voice[0], ref: 'ledger-api' }])).toBeNull()
  })
  it('turno fallito chiuso con settled_at o senza continue_text', () => {
    expect(openFailure([{ ...f, settled_at: 1100 }], s('atlas-shop', 'idle', null), 1160)).toBeNull()
    expect(openFailure([{ ...f, continue_text: null }], s('atlas-shop', 'idle', null), 1160)).toBeNull()
  })
  it('quota finita chiusa allo stesso modo', () => {
    expect(openLimit([limit], done(900), 2000)).toBe(limit)
    expect(openLimit([limit], done(1100), 2000)).toBeNull()
    expect(openLimit([limit], s('atlas-shop', 'idle', null), 2000, [{ ...voice[0], kind: 'quota_limit' }])).toBe(limit)
    expect(openLimit([limit], s('atlas-shop', 'idle', null), 2000, voice)).toBeNull()
    expect(openLimit([{ ...limit, settled_at: 1500 }], s('atlas-shop', 'idle', null), 2000)).toBeNull()
  })
})
