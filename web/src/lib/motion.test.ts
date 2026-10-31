import { describe, expect, it } from 'vitest'
import { bounceEase, finishScrollAllowed, type FinishScene } from './motion'

const ok: FinishScene = { fingerDown: false, listMoving: false, sinceTouchMs: 2000, wasOnScreen: true, writing: false, sheetOpen: false, distancePx: 800, viewportPx: 800, sinceLastMs: 10000 }

describe('motion', () => {
  it('il rimbalzo sfora di circa il 5% e si posa a 1', () => {
    const top = Math.max(...Array.from({ length: 200 }, (_, i) => bounceEase(i / 200)))
    expect(top).toBeGreaterThan(1.04); expect(top).toBeLessThan(1.06)
    expect(bounceEase(0)).toBeCloseTo(0); expect(bounceEase(1)).toBe(1)
  })
  it('scorre solo se tutte le condizioni reggono', () => {
    expect(finishScrollAllowed(ok)).toBe(true)
    for (const bad of [{ fingerDown: true }, { listMoving: true }, { sinceTouchMs: 1999 }, { wasOnScreen: false }, { writing: true }, { sheetOpen: true }, { distancePx: -801 }, { sinceLastMs: 9999 }]) expect(finishScrollAllowed({ ...ok, ...bad })).toBe(false)
  })
})
