import { describe, expect, it } from 'vitest'
import type { Question } from './contract'
import { allowAllVisible, inline, needsLongPress, optionLabel, optionNote, primaryIndex, refusalText, textArg } from './questionRules'

// Gli stessi casi di QuestionRulesTest in Kotlin.
const q: Question = { id: 'q', kind: 'ask', text: 'Deploy?', options: [{ n: 1, label: 'yes' }, { n: 2, label: 'no' }], tier: 'low', asked_at: 0 }
describe('domanda', () => {
  it('il rischio alto vuole la pressione lunga', () => {
    expect(needsLongPress('high')).toBe(true); expect(needsLongPress('medium')).toBe(false); expect(needsLongPress('low')).toBe(false)
  })
  it('Consenti tutto solo per i permessi sotto high e con le opzioni', () => {
    expect(allowAllVisible({ ...q, kind: 'permission', options: [] })).toBe(false)
    expect(allowAllVisible(q)).toBe(false)
    expect(allowAllVisible({ ...q, kind: 'permission' })).toBe(true)
    expect(allowAllVisible({ ...q, kind: 'permission', tier: 'high' })).toBe(false)
  })
  it("l'etichetta senza il riquadro", () => {
    expect(optionLabel({ n: 1, label: 'Due binari (Consigliata)\n┌──────────┐\n│ FRANCESCO │\n└──────────┘' })).toBe('1 · Due binari (Consigliata)')
    expect(optionLabel({ n: 3, label: "Prima l'e-commerce │           │" })).toBe("3 · Prima l'e-commerce")
    expect(optionLabel({ n: 2, label: '┌───┐\nPrima il builder' })).toBe('2 · Prima il builder')
    expect(optionLabel(q.options[0])).toBe('1 · yes')
  })
  it('opzioni brevi su una riga, lunghe, quattro o una sola una sotto l\'altra', () => {
    expect(inline([{ n: 1, label: 'yes' }, { n: 2, label: 'no' }])).toBe(true)
    expect(inline([{ n: 1, label: "Yes, and don't ask again for this command" }, { n: 2, label: 'No' }])).toBe(false)
    expect(inline([1, 2, 3, 4].map(n => ({ n, label: `o${n}` })))).toBe(false)
    expect(inline([{ n: 1, label: 'ok' }])).toBe(false)
  })
  it('la risposta a parole su una riga', () => expect(textArg(' sì\n  ma dopo \n')).toBe('text:sì ma dopo'))
})

describe('opzioni spiegate (contratto 1.53)', () => {
  const yes = { n: 1, label: 'Yes', description: 'Ship it to production now.', recommended: true }
  const no = { n: 2, label: 'No' }
  it('una riga sotto il tasto, la consigliata piena, mai in riga con una spiegazione', () => {
    expect(optionNote(yes, 'consigliata')).toBe('consigliata · Ship it to production now.')
    expect(optionNote(no, 'consigliata')).toBeNull()
    expect(optionNote({ n: 1, label: 'a', description: 'first\nsecond' }, 'x')).toBe('first')
    expect(primaryIndex([no, { ...yes, n: 2 }])).toBe(1)
    expect(primaryIndex([no])).toBe(0)
    expect(inline([yes, no])).toBe(false)
    expect(inline([{ n: 1, label: 'Yes' }, no])).toBe(true)
  })
})

describe('risolta al terminale (contratto 1.55)', () => {
  const r = { id: 'c', ok: false, text: 'ledger-api: the question changed after your answer, nothing was pressed', at: 1 }
  const ses = [{ id: 'x', name: 'ledger-api', account: 'a', project: 'p', state: 'idle', since: 1, resolved: { id: 'q-1-1', text: 'Deploy?', at: 2, by: 'terminal' } }] as never
  const word = (n: string) => `${n}: già risolta al PC`
  it('il rifiuto dice che al PC era già stata risolta, solo per quella domanda', () => {
    expect(refusalText(r, 'q-1-1', ses, word)).toBe('ledger-api: già risolta al PC')
    expect(refusalText(r, 'q-0-1', ses, word)).toBe(r.text)
    expect(refusalText(r, null, ses, word)).toBe(r.text)
  })
})
