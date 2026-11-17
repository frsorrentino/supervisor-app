import { describe, expect, it } from 'vitest'
import { resetLine, resetText, resetWhen } from './utilizzo'

// app-quota-azzeramento-17-11: la riga sotto ogni percentuale. 1795053600 è il rinnovo vero dell'account personale di questa settimana.
const RESET = 1795053600
const NOW = RESET - 40 * 3600
const TZ = 'Europe/Rome'

describe('la riga di azzeramento', () => {
  it("l'ora come i titoli dei limiti, i minuti solo se ci sono", () => {
    expect(resetWhen(RESET, TZ)).toBe('gio 19/11 alle 3')
    expect(resetWhen(RESET + 30 * 60 - 3 * 86400, TZ)).toBe('lun 16/11 alle 3:30')
    expect(resetWhen(RESET + 11 * 3600 + 5 * 60, TZ)).toBe('gio 19/11 alle 14:05')
  })
  it('ora futura: si azzera', () => {
    expect(resetText(resetLine(RESET, NOW, false)!, TZ)).toBe('si azzera gio 19/11 alle 3')
  })
  it("ora passata: lo dice invece dell'ora", () => {
    expect(resetText(resetLine(RESET, RESET + 60, false)!, TZ)).toBe('azzerata, aspetto la lettura nuova')
  })
  it("lettura vecchia: l'età se si sa, altrimenti non aggiornata, mai l'ora", () => {
    expect(resetText(resetLine(RESET, NOW, false, 2 * 3600 + 5 * 60)!, TZ)).toBe('lettura di 2 h 05 fa')
    expect(resetText(resetLine(RESET, NOW, true)!, TZ)).toBe('non aggiornata')
  })
  it('senza ora niente riga', () => {
    expect(resetLine(null, NOW, false)).toBeNull()
  })
})
