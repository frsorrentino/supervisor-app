import { describe, expect, it } from 'vitest'
import { bounceEase, finishScrollAllowed, pressFrame, type FinishScene } from './motion'

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

describe('pressFrame: il cambio forma dei menu', () => {
  const menu = { left: 100, top: 200, width: 300, height: 400 }
  it('a 0 è il rettangolo del tasto, centrato sul punto premuto, mai a misura zero', () => {
    const press = { x: 380, y: 170 }
    const f = pressFrame(menu, press, 0)
    const w = menu.width - f.left - f.right, h = menu.height - f.top - f.bottom
    expect([w, h]).toEqual([44, 44])
    expect(menu.left + f.left + f.dx).toBeCloseTo(press.x - 22)
    expect(menu.top + f.top + f.dy).toBeCloseTo(press.y - 22)
  })
  it('a 1 è il menu intero al suo posto, nessun taglio e nessuno spostamento', () => {
    const f = pressFrame(menu, { x: 380, y: 170 }, 1)
    for (const v of [f.top, f.right, f.bottom, f.left, f.dx, f.dy]) expect(v).toBeCloseTo(0)
  })
  it('a metà sta tra il tasto e il menu', () => {
    const f = pressFrame(menu, { x: 380, y: 170 }, 0.5)
    expect(menu.width - f.left - f.right).toBeCloseTo(172)
  })
  it('senza un tocco noto parte dall\'angolo in alto a destra', () => {
    const f = pressFrame(menu, null, 0)
    expect(f.left + f.dx).toBeCloseTo(256)
    expect(f.top + f.dy).toBeCloseTo(0)
  })
})
