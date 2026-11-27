import { describe, expect, it } from 'vitest'
import { scrubAt, scrubNearest } from './animazioni'

// Il terzo giro di animazioni dopo la prova 1391: la barra dei paragrafi. Gli stessi casi di AnimazioniTerzeTest in Kotlin.
describe('animazioni terze', () => {
  it("il punto sta all'inizio del paragrafo che la voce legge", () => {
    expect([scrubAt(0, 4), scrubAt(2, 4), scrubAt(9, 4), scrubAt(0, 1)]).toEqual([0, 0.5, 0.75, 0])
  })
  it('al rilascio si aggancia al paragrafo che comincia più vicino', () => {
    expect([scrubNearest(0.1, 4), scrubNearest(0.2, 4), scrubNearest(0.125, 4), scrubNearest(0.6, 4), scrubNearest(1, 4), scrubNearest(-0.3, 4), scrubNearest(0.9, 1)]).toEqual([0, 1, 1, 2, 3, 0, 0])
    for (let n = 2; n <= 9; n++) for (let k = 0; k < n; k++) expect(scrubNearest(scrubAt(k, n), n)).toBe(k)
  })
})
