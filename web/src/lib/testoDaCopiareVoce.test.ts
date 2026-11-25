import { describe, expect, it } from 'vitest'
import { clean, forPhone, isCopyText } from './speechRules'

// Come TestoDaCopiareVoceTest in Kotlin (Franz, 25/11 22:53): un blocco ```text si legge per intero, il codice si annuncia.
const bozza = 'Ecco la bozza:\n\n```text\nBuongiorno,\nvi scrivo per il preventivo.\n1. Due licenze\n2. Il rinnovo\nGrazie\n```\n\nFatto.'
describe('testo da copiare a voce', () => {
  it('un blocco text si legge per intero', () => {
    const t = forPhone(bozza, 'Codice')
    expect(t).toBe('Ecco la bozza:\nBuongiorno.\nvi scrivo per il preventivo.\n1. Due licenze.\n2. Il rinnovo.\nGrazie.\nFatto.')
    expect(t).not.toContain('Codice')
  })
  it('il codice e i blocchi senza lingua si annunciano', () => {
    expect(clean('Patch:\n```kotlin\nval x = 1\n```\nSenza lingua:\n```\nrm -rf build\n```', 'Codice')).toBe('Patch:\nCodice.\nSenza lingua:\nCodice.')
  })
  it('le lingue del testo da copiare', () => {
    expect(isCopyText('text')).toBe(true); expect(isCopyText('TXT ')).toBe(true); expect(isCopyText('plaintext')).toBe(true)
    expect(isCopyText('')).toBe(false); expect(isCopyText('bash')).toBe(false)
  })
})
