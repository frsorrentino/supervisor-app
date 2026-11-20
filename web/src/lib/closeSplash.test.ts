import { describe, expect, it } from 'vitest'
import type { Session, SessionState } from './contract'
import { closeRefusals, closeSplash, FORCE_S, ghostSession, SLOW_S } from './closeSplash'

// Gli stessi casi di CloseSplashTest.kt.
const s = (n: string, state: SessionState, since = 100): Session => ({ id: n, name: n, account: 'personale', project: n, state, since })
const sent = (delivered = false) => ({ name: 'kb', sentAt: 1_000, delivered })

describe('closeSplash', () => {
  it('a live session without exit shows nothing', () => expect(closeSplash('kb', [s('kb', 'idle')], null, null, 1_010)).toBeNull())
  it('after exit the session is closing until the PC confirms', () => {
    expect(closeSplash('kb', [s('kb', 'idle')], sent(), null, 1_002)).toEqual({ kind: 'closing', name: 'kb', sentAt: 1_000, delivered: false, slow: false, force: false, save: false, step: null })
    expect(closeSplash('kb', [s('kb', 'idle')], sent(true), null, 1_004)).toEqual({ kind: 'closing', name: 'kb', sentAt: 1_000, delivered: true, slow: false, force: false, save: false, step: null })
  })
  it('a closing that takes longer than usual says so', () =>
    expect(closeSplash('kb', [s('kb', 'idle')], sent(true), null, 1_000 + SLOW_S)).toMatchObject({ kind: 'closing', slow: true }))
  it('gone after my exit is closed by me at its last sighting', () =>
    expect(closeSplash('kb', [s('kb', 'gone', 1_003)], sent(), null, 1_005)).toEqual({ kind: 'closed', name: 'kb', at: 1_003, byMe: true, save: false, handoff: false, file: null, reason: null }))
  it('vanished after my exit is closed by me', () =>
    expect(closeSplash('kb', [], sent(), s('kb', 'gone', 1_003), 1_005)).toEqual({ kind: 'closed', name: 'kb', at: 1_003, byMe: true, save: false, handoff: false, file: null, reason: null }))
  it('gone on its own stays as a page', () => expect(closeSplash('kb', [s('kb', 'gone')], null, null, 1_005)).toBeNull())
  it('vanished on its own is closed not by me, now', () =>
    expect(closeSplash('kb', [], null, s('kb', 'idle', 900), 1_005)).toEqual({ kind: 'closed', name: 'kb', at: 1_005, byMe: false, save: false, handoff: false, file: null, reason: null }))
  it('a question after exit shows the question, not the panel', () =>
    expect(closeSplash('kb', [{ ...s('kb', 'waiting'), question: { id: 'q1', kind: 'ask', text: 'Background tasks are running', options: [{ n: 3, label: 'Stay' }], tier: 'low', asked_at: 1_010 } }], sent(true), null, 1_030)).toBeNull())
  it("another session's exit does not count", () => expect(closeSplash('atlas', [s('atlas', 'idle')], sent(), null, 1_005)).toBeNull())
})

// 23/10: la pagina (colonna appuntata o pagina della striscia) di una sessione che non è più nello stato né tra le viste
// faceva crollare l'app su `s.state` (App.svelte:844); la sessione fantasma esiste sempre.
describe('ghostSession', () => {
  it('a session seen before comes back as it was, gone and without question', () =>
    expect(ghostSession('kb', { kb: { ...s('kb', 'busy', 900), question: { id: 'q1', kind: 'ask', text: 'x', options: [], tier: 'low', asked_at: 1 } } }))
      .toEqual({ ...s('kb', 'gone', 900), question: null }))
  it('a session never seen is a gone placeholder, never null', () =>
    expect(ghostSession('kb', {})).toEqual({ id: 'kb', name: 'kb', account: '', project: '', state: 'gone', since: 0, question: null }))
})

// app-chiudi-esito-04-11: il Chiudi rifiutato dice perché, e il pannello resta finché non lo togli.
describe('close refused', () => {
  const texts = { busy: 'resta aperta: sta lavorando', background: 'resta aperta: ha un lavoro in sottofondo' }
  const other = (r: string) => `resta aperta: ${r}`
  it('a refused exit shows the reason instead of closing', () =>
    expect(closeSplash('kb', [s('kb', 'idle')], { ...sent(true), refused: 'resta aperta: ha un lavoro in sottofondo' }, null, 1_002))
      .toEqual({ kind: 'refused', name: 'kb', why: 'resta aperta: ha un lavoro in sottofondo', armable: false }))
  it('gone anyway after a refusal is closed', () =>
    expect(closeSplash('kb', [s('kb', 'gone', 1_003)], { ...sent(), refused: 'x' }, null, 1_005)).toMatchObject({ kind: 'closed' }))
  it('reads the background refusal the relay sends today', () =>
    expect(closeRefusals('ERR kb not closed: a job is running in the background', texts, other)).toEqual([{ name: 'kb', why: 'resta aperta: ha un lavoro in sottofondo', armable: true }]))
  it('reads only the refused lines of a close with names', () =>
    expect(closeRefusals('closed: field-notes\natlas-shop not closed: it is working', texts, other)).toEqual([{ name: 'atlas-shop', why: 'resta aperta: sta lavorando', armable: true }]))
  it('an unknown reason stays as the relay wrote it', () =>
    expect(closeRefusals('kb not closed: something else', texts, other)).toEqual([{ name: 'kb', why: 'resta aperta: something else', armable: false }]))
  it('no refusal in a plain close', () => expect(closeRefusals('closed: kb', texts, other)).toEqual([]))
  // Relay 0.8.196 (app-chiudi-scheda-20-11): l'esito in italiano, una riga per sessione dopo «chiusa: …».
  it('reads the Italian refusals of the relay', () => {
    const text = "chiusa: field-notes\natlas-shop non chiusa: sta lavorando\nkb non chiusa: c'è una domanda in attesa\n" +
      "notes non chiusa: c'è un lavoro in sottofondo\nold non chiusa: non è in esecuzione\nstuck non chiusa: non si riesce a fermarla"
    expect(closeRefusals(text, texts, other).map(r => [r.name, r.armable])).toEqual([['atlas-shop', true], ['kb', true], ['notes', true], ['old', false], ['stuck', false]])
    expect(closeRefusals('kb si chiude appena finisce', texts, other)).toEqual([])
  })
})

// Contratto 1.120: «Chiudi quando finisce» e «Chiudi comunque» solo dove il relay arma la chiusura.
describe('close armed', () => {
  const texts = { busy: 'sta lavorando', question: 'ha una domanda', background: 'sottofondo', attached: 'aperta al PC' }
  it('a question waiting can be armed, an attached session cannot', () => {
    expect(closeRefusals('kb not closed: a question is waiting', texts, x => x)[0].armable).toBe(true)
    expect(closeRefusals('kb not closed: it is attached to a terminal', texts, x => x)[0].armable).toBe(false)
  })
  it('a refused panel says whether it can be armed', () =>
    expect(closeSplash('kb', [s('kb', 'busy')], { ...sent(true), refused: 'sta lavorando', armable: true }, null, 1_002)).toMatchObject({ kind: 'refused', armable: true }))
  it('a forced close is not slow before its 180 s', () => {
    expect(closeSplash('kb', [s('kb', 'busy')], { ...sent(true), force: true }, null, 1_000 + FORCE_S)).toMatchObject({ kind: 'closing', slow: false, force: true })
    expect(closeSplash('kb', [s('kb', 'busy')], { ...sent(true), force: true }, null, 1_000 + FORCE_S + SLOW_S)).toMatchObject({ slow: true })
  })
})

// Contratto 1.155 (app-chiudi-salva-19-11): «Salva e chiudi» segue i passi del relay e dice com'è finita.
describe('close with save', () => {
  const saving = { ...sent(true), save: true }
  const arm = (mode: 'save' | 'exit') => ({ ...s('kb', 'idle'), closing: { mode, at: 1_001, deadline: mode === 'save' ? 1_181 : null } })
  it('before the relay arms it, the save has no step yet', () =>
    expect(closeSplash('kb', [s('kb', 'idle')], saving, null, 1_002)).toMatchObject({ kind: 'closing', save: true, step: null }))
  it('closing.mode save is writing the handoff, exit is closing', () => {
    expect(closeSplash('kb', [arm('save')], saving, null, 1_010)).toMatchObject({ save: true, step: 'save' })
    expect(closeSplash('kb', [arm('exit')], saving, null, 1_020)).toMatchObject({ save: true, step: 'exit' })
  })
  it('a plain exit ignores closing.mode', () =>
    expect(closeSplash('kb', [arm('save')], sent(true), null, 1_010)).toMatchObject({ save: false, step: null }))
  it('a save is not slow before its 180 s', () => {
    expect(closeSplash('kb', [arm('save')], saving, null, 1_000 + FORCE_S)).toMatchObject({ slow: false })
    expect(closeSplash('kb', [arm('save')], saving, null, 1_000 + FORCE_S + SLOW_S)).toMatchObject({ slow: true })
  })
  it('gone with a handoff says where it is', () =>
    expect(closeSplash('kb', [{ ...s('kb', 'gone', 1_050), closed: { at: 1_050, handoff: true, file: 'docs/handoff-2026-11-20.md' } }], saving, null, 1_060))
      .toEqual({ kind: 'closed', name: 'kb', at: 1_050, byMe: true, save: true, handoff: true, file: 'docs/handoff-2026-11-20.md', reason: null }))
  it('gone after the save expired carries the reason', () =>
    expect(closeSplash('kb', [{ ...s('kb', 'gone', 1_190), closed: { at: 1_190, reason: 'salvataggio scaduto' } }], saving, null, 1_200))
      .toMatchObject({ kind: 'closed', save: true, handoff: false, reason: 'salvataggio scaduto' }))
  it('vanished after the save reads the last sighting', () =>
    expect(closeSplash('kb', [], saving, { ...s('kb', 'gone', 1_050), closed: { at: 1_050, handoff: true, file: 'h.md' } }, 1_060))
      .toMatchObject({ kind: 'closed', handoff: true, file: 'h.md' }))
})
