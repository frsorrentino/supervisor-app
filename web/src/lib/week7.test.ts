import { describe, expect, it } from 'vitest'
import type { WeekPoints } from './contract'
import { BUDGET, WEEK_S, samplesFrom, weekChart, weekDays } from './week7'

// Gli stessi casi di Week7Test in Kotlin. Settimana da giovedì 03:00 (ora 0), contratto 1.86: una voce a ogni cambio.
const start = 1_789_005_600
const reset = start + WEEK_S
const h = (n: number) => start + n * 3600
const late: WeekPoints = { start, steps: [[110, 50], [112, 53], [120, 58], [130, 62], [140, 65]] }

describe('settimana ora per ora', () => {
  it('la linea è a gradini fino ad adesso', () => {
    const c = weekChart({ start, steps: [[0, 2], [24, 30], [25, 38]] }, 38, reset, h(30))
    expect(c.points).toEqual([[0, 2], [24 / 168, 2], [24 / 168, 30], [25 / 168, 30], [25 / 168, 38], [30 / 168, 38]])
    expect(c.nowX).toBeCloseTo(30 / 168)
  })
  it('proiezione al ritmo delle ultime 48 ore', () => {
    const c = weekChart(late, 65, reset, h(140))
    expect(c.fullAt).toBeNull()
    expect(c.projected).toBe(79)
  })
  it("a ritmo veloce l'ora del 100% prima del rinnovo", () => {
    const c = weekChart({ start, steps: [[100, 40], [124, 64], [136, 76]] }, 76, reset, h(136))
    expect(c.fullAt).toBe(h(160))
    expect(c.projected).toBe(100)
  })
  it('sotto le 12 ore di campioni la media della settimana', () => {
    const c = weekChart({ start, steps: [[100, 40], [104, 44]] }, 44, reset, h(110))
    expect(c.projected).toBe(67)
    expect(c.fullAt).toBeNull()
  })
  it('i giorni contro il budget', () => {
    const d = weekDays(late, 65, h(140))
    expect(d).toHaveLength(7)
    expect(d.slice(0, 5).map(x => x.spent)).toEqual([null, null, null, null, null])
    expect(d[4].partial).toBe(true)
    expect(d[5].spent).toBe(12)
    expect(d[5].today).toBe(true)
    expect(d[6].future).toBe(true)
    expect(samplesFrom(late)).toBe(h(110))
  })
  it('dal rinnovo ogni giorno conta', () => {
    const p: WeekPoints = { start, steps: [[0, 2], [2, 4], [3, 5], [24, 30], [25, 38]] }
    const d = weekDays(p, 38, h(30))
    expect(d[0].spent).toBe(5)
    expect(d[1].spent).toBe(33)
    expect(samplesFrom(p)).toBeNull()
    expect(BUDGET).toBeCloseTo(100 / 7)
  })
})
