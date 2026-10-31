import { describe, expect, it } from 'vitest'
import { elapsedPct, H5_S, peakHour, quotaBar, W7_S, waitsLine, weekDay, weekDelta } from './utilizzo'

describe('utilizzo', () => {
  it('quanto è passato della finestra', () => {
    expect(elapsedPct(3000, 5000, 1000)).toBeCloseTo(60)
    expect(elapsedPct(1000, 5000, 1000)).toBeNull()
    expect(elapsedPct(null, 5000, 1000)).toBeNull()
  })
  it('la settimana contro il ritmo pari', () => {
    expect(weekDelta(41, 1000 + W7_S / 2, 1000)).toBe(-9)
    expect(weekDelta(60, 1000 + W7_S / 2, 1000)).toBe(10)
    expect(weekDelta(null, 1000 + W7_S / 2, 1000)).toBeNull()
  })
  it("l'ora di punta gira a mezzanotte e ignora una giornata vuota", () => {
    expect(peakHour([[1, 0], [2, 3], [1, 1]], 23)).toBe(0)
    expect(peakHour([[0, 0]], 8)).toBeNull()
  })
  it("l'ultima mediana e le chiuse", () => {
    expect(waitsLine([{ median_min: 9, n: 5 }, { median_min: 4.2, n: 7 }, { median_min: null, n: 0 }])).toEqual({ medianMin: 4, closed: 12 })
    expect(waitsLine([])).toBeNull()
  })
})

// quota-senza-cinque-ore: Anthropic toglie il limite a tappe, per account. Conta il dato assente, mai il nome del piano.
describe('barra della quota senza il dato delle 5 ore', () => {
  const now = 1_000_000
  const resetW7 = now + 5 * 86400 // passati 2 giorni su 7: ritmo pari al 29 %
  it('senza h5 mostra la settimana, con il giorno e il ritmo', () => {
    expect(quotaBar(null, 41, null, resetW7, now)).toEqual({ week: true, pct: 41, resetAt: resetW7, windowS: W7_S, delta: 12, day: 3 })
  })
  it('con h5 resta la barra delle 5 ore, anche a zero', () => {
    expect(quotaBar(0, 41, now + 3600, resetW7, now)).toEqual({ week: false, pct: 0, resetAt: now + 3600, windowS: H5_S, delta: null, day: null })
  })
  it('due account: il dato manca su uno solo', () => {
    expect(quotaBar(34, 50, now + 3600, resetW7, now)?.week).toBe(false)
    expect(quotaBar(undefined, 50, null, resetW7, now)?.week).toBe(true)
  })
  it('senza rinnovo la settimana resta, senza ritmo', () => {
    expect(quotaBar(null, 41, null, null, now)).toEqual({ week: true, pct: 41, resetAt: null, windowS: W7_S, delta: null, day: null })
  })
  it('senza nessuno dei due dati non c\'è barra: mai uno 0 % inventato', () => {
    expect(quotaBar(null, null, null, null, now)).toBeNull()
  })
  it('il giorno resta fra 1 e 7', () => {
    expect(weekDay(now + 7 * 86400, now)).toBe(1)
    expect(weekDay(now + 1, now)).toBe(7)
    expect(weekDay(now, now)).toBeNull()
  })
})
