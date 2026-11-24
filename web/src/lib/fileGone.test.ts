import { describe, expect, it } from 'vitest'
import { GoneFiles, fileGone, fileName } from './fileGone'

// app-file-mancante-24-11: il rifiuto del relay per un file sparito dal PC, il nome per l'avviso e il segno che evita di richiederlo.
describe('file sparito dal PC', () => {
  it('riconosce il rifiuto del relay', () => {
    expect(fileGone('missing or unreadable')).toBe(true)
    expect(fileGone('too large: 3630371')).toBe(false)
    expect(fileGone(null)).toBe(false)
  })
  it("il nome è l'ultima parte del percorso", () =>
    expect(fileName('rino/legale/reinvio-Provincia-2026-11/Verbale-prescrizione-06-2025_pag1_scanner.pdf')).toBe('Verbale-prescrizione-06-2025_pag1_scanner.pdf'))
  it('resta segnato per sessione e percorso', () => {
    const g = new GoneFiles()
    g.add('legale', 'a/b.pdf')
    expect(g.has('legale', 'a/b.pdf')).toBe(true)
    expect(g.has('altro', 'a/b.pdf')).toBe(false)
  })
})
