import { describe, expect, it } from 'vitest'
import { WINDOW_S, type Sample } from './quotaHistory'
import { rhythm } from './rhythm5h'

// Gli stessi casi di Rhythm5hTest in Kotlin. La finestra del mockup 1A/1B: 08:40-13:40, ora 11:20, quota al 62%.
const reset = 50_000
const start = reset - WINDOW_S
const now = start + 160 * 60
const S = (ts: number, pct: number): Sample => ({ ts, pct })

describe('ritmo delle cinque ore', () => {
  it('tempo passato e quota avanti o indietro', () => {
    const v = rhythm([], 62, reset, now)
    expect(v.elapsed).toBe(53)
    expect(v.ahead).toBe(9)
    expect(rhythm([], 50, reset, now).ahead).toBe(-3)
    expect(rhythm([], 10, reset, reset + 60).elapsed).toBe(100)
  })
  it("il ritmo dell'ultima ora dà l'ora del 100%", () => {
    const v = rhythm([S(start, 0), S(now - 7200, 30), S(now - 3600, 42), S(now, 62)], 62, reset, now)
    expect(v.fullAt).toBe(now + 114 * 60)
    expect(v.idleMin).toBe(26)
    expect(v.projected).toBe(100)
  })
  it('a ritmo lento la proiezione è al reset', () => {
    const v = rhythm([S(now - 3600, 50), S(now, 56)], 56, reset, now)
    expect(v.fullAt).toBeNull()
    expect(v.idleMin).toBeNull()
    expect(v.projected).toBe(70)
  })
  it('niente ritmo sotto i 10 minuti o senza salita', () => {
    expect(rhythm([S(now - 300, 50), S(now, 60)], 60, reset, now).projected).toBeNull()
    expect(rhythm([S(now - 3600, 60), S(now, 60)], 60, reset, now).projected).toBeNull()
    expect(rhythm([S(now, 60)], 60, reset, now).projected).toBeNull()
    expect(rhythm([S(start - 600, 10), S(now, 60)], 60, reset, now).projected).toBeNull()
  })
  it('già al 100% niente ora del 100%', () => {
    const v = rhythm([S(now - 3600, 80), S(now, 100)], 100, reset, now)
    expect(v.fullAt).toBeNull()
    expect(v.projected).toBe(100)
  })
})
