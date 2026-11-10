import { describe, expect, it } from 'vitest'
import { genieContent, genieRect } from './motion'

// Il Genie dei menu (app-genie-menu-09-11): gli stessi casi di GenieMenuTest.shapeGoesFromDropToPanel in Kotlin.
describe('Genie', () => {
  it('va dalla goccia sotto il tasto al pannello', () => {
    const w = 1000, h = 1500, cx = 900, drop = 120, r = 38
    const start = genieRect(0, w, h, cx, drop, r)
    expect(start.left < cx && start.right > cx && start.top === 0 && start.right - start.left < drop).toBe(true)
    expect(start.radius).toBeCloseTo((start.right - start.left) / 2, 2)
    const mid = genieRect(0.6, w, h, cx, drop, r)
    expect(mid.right - mid.left > drop && mid.right - mid.left < w && mid.bottom < h).toBe(true)
    const end = genieRect(1, w, h, cx, drop, r)
    expect([end.left, end.right, end.bottom, end.radius].map(v => +v.toFixed(2))).toEqual([0, w, h, r])
    expect(genieContent(0.5)).toBe(0)
    expect(genieContent(1)).toBe(1)
  })
})
