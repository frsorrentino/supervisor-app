import { describe, expect, it } from 'vitest'
import { elapsedPct, peakHour, W7_S, waitsLine, weekDelta } from './utilizzo'

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
