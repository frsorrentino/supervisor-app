import { describe, expect, it } from 'vitest'
import type { Approval, WaitItem } from './contract'
import { COPY_HOLD_MS, CYCLE_MS, HOLD_MS, needsHold, needsHoldWait, rolls, wordCount, wordIndex, words } from './animazioni'

// Le otto animazioni scelte da Franz il 26/11 (app-animazioni-otto-26-11): gli stessi casi di AnimazioniOttoTest in Kotlin.
describe('animazioni otto', () => {
  it('tempi comuni', () => { expect([COPY_HOLD_MS, HOLD_MS, CYCLE_MS]).toEqual([1600, 600, 1600]) })
  it('tenere premuto solo per la produzione', () => {
    expect(needsHold({ task: 't', title: 'Release', deploy: true } as Approval)).toBe(true)
    expect(needsHold({ task: 't', title: 'Pre' } as Approval)).toBe(false)
    expect(needsHold(null)).toBe(false)
    expect(needsHoldWait({ id: 'w', card: { deploy: true } } as WaitItem)).toBe(true)
    expect(needsHoldWait({ id: 'w' } as WaitItem)).toBe(false)
  })
  it('la cifra scorre solo per un gesto', () => {
    expect(rolls(3, 2, true)).toBe(true); expect(rolls(3, 2, false)).toBe(false); expect(rolls(2, 2, true)).toBe(false); expect(rolls(null, 2, true)).toBe(false)
  })
  it('la parola segue la voce', () => {
    const shown = 'I blocchi di **codice** veri restano annunciati'
    const spoken = 'I blocchi di codice veri restano annunciati.'
    const n = words(shown).length
    expect(n).toBe(7)
    expect(wordIndex(spoken, 0, 0, n)).toBe(0)
    expect(wordIndex(spoken, spoken.indexOf('codice'), 0, n)).toBe(3)
    expect(wordIndex(spoken, spoken.indexOf('codice') + 2, 0, n)).toBe(3)
    expect(wordIndex(spoken, spoken.length + 5, 0, n)).toBe(6)
    expect(wordIndex('restano annunciati.', 0, 5, n)).toBe(5)
    expect(wordIndex('restano annunciati.', 0, 40, n)).toBe(6)
    expect(wordIndex(spoken, 0, 0, 0)).toBeNull()
    expect(wordCount('uno due  tre')).toBe(3)
  })
})
