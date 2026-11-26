import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type Question, type Session, type State } from './contract'
import { allowAll, chat, close, night, reopen, slash, type, typedIn } from './vociAssenti'

// Le voci che l'app non offre più (app-issue4-cause-app-26-11, contratto 1.161): gli stessi casi di VociAssentiTest in Kotlin.
const q = (o: Partial<Question> = {}): Question => ({ id: 'q1', kind: 'permission', text: 'Esegui?', options: [{ n: 1, label: 'Sì' }, { n: 2, label: 'No' }], tier: 'low', asked_at: 0, ...o })
const fixture = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const sess = (state: Session['state'], attached = false, at_pc = false): Session => ({ ...fixture.sessions[0], name: 's1', state, attached, at_pc })

describe('voci assenti', () => {
  it('chat e scrivi seguono foot', () => {
    expect(chat(q())).toBe(true); expect(type(q())).toBe(true)
    expect(chat(q({ foot: ['type', 'chat'] }))).toBe(true)
    expect(chat(q({ foot: ['type'] }))).toBe(false)
    expect(type(q({ foot: ['chat'] }))).toBe(false)
    expect(chat(q({ foot: [] }))).toBe(false)
  })
  it('consenti sempre solo se c’è', () => {
    expect(allowAll(q())).toBe(true); expect(allowAll(q({ allow_all: true }))).toBe(true); expect(allowAll(q({ allow_all: false }))).toBe(false)
  })
  it('la notte solo con l’op', () => {
    expect(night(null)).toBe(true); expect(night(['answer', 'night'])).toBe(true); expect(night(['answer', 'agenda'])).toBe(false)
  })
  it('riapri solo una chiusa', () => {
    expect(reopen(sess('gone'))).toBe(true); expect(reopen(sess('idle'))).toBe(false); expect(reopen(null)).toBe(true)
  })
  it('slash solo a una sessione viva', () => {
    const st: State = { ...fixture, sessions: [sess('idle'), { ...sess('gone'), name: 'gone' }] }
    expect(slash(st, 's1')).toBe(true); expect(slash(st, 'gone')).toBe(false); expect(slash(st, 'sparita')).toBe(false); expect(slash(null, 's1')).toBe(false)
  })
  it('chiudi non su attaccate né in sottofondo', () => {
    // app-chiudi-sessioni-ferme-26-11: attaccata e ferma si chiude; con Franz al PC (at_pc) no.
    expect(close(sess('idle'))).toBe(true); expect(close(sess('idle', true))).toBe(true); expect(close(sess('idle', true, true))).toBe(false)
    for (const b of ['busy', 'question', 'background', 'gone']) expect(close(sess('idle', true), b)).toBe(false)
    expect(close(sess('idle', true), 'attached')).toBe(true)
  })
  it('il rifiuto del testo digitato', () => {
    expect(typedIn("supervisor talk: nella casella di «atlas-shop» c'è del testo digitato: «ciao»")).toBe('atlas-shop')
    expect(typedIn('atlas-shop is not running')).toBeNull(); expect(typedIn(null)).toBeNull()
  })
})
