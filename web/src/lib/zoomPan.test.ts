import { describe, expect, it } from 'vitest'
import { doubleTap, fit, IDENTITY, MAX, maxScale, step } from './zoomPan'

// Gli stessi casi di ZoomPanTest.kt: una vista 1000×2000 con un'immagine adattata 1000×600.
const w = 1000, h = 2000, cw = 1000, ch = 600
const go = (v = IDENTITY, zoom = 1, px = 0, py = 0, cx = 500, cy = 1000) => step(v, zoom, px, py, cx, cy, cw, ch, w, h)

describe('il visore delle immagini (app-chat-zoom-02-11)', () => {
  it('a 4x lo spostamento segue il dito 1:1', () => {
    const v = go(IDENTITY, 4)
    expect(go(v, 1, 100).x).toBeCloseTo(v.x + 100)
    expect(go(v, 1, 0, -50).y).toBeCloseTo(v.y - 50)
  })
  it('lo zoom resta attorno alle dita', () => {
    const v = go(IDENTITY, 2, 0, 0, 700, 1000)
    expect(w / 2 + v.x + (700 - w / 2) * v.scale).toBeCloseTo(700)
  })
  it("l'immagine non esce dai bordi", () => {
    const v = go(IDENTITY, 2)
    expect(go(v, 1, 5000).x).toBeCloseTo(500)
    expect(go(v, 1, -5000).x).toBeCloseTo(-500)
    expect(go(v, 1, 0, 800).y).toBeCloseTo(0)
    expect(go(IDENTITY, 1, 300).x).toBeCloseTo(0)
  })
  it('lo zoom sta fra 1 e 6', () => {
    expect(go(IDENTITY, 0.3).scale).toBeCloseTo(1)
    expect(go(IDENTITY, 20).scale).toBeCloseTo(6)
  })
  it('il doppio tocco ingrandisce sul punto e torna', () => {
    const z = doubleTap(IDENTITY, 800, 1000, cw, ch, w, h)
    expect(z.scale).toBeCloseTo(2.5)
    expect(z.x).toBeCloseTo(-450)
    expect(doubleTap(z, 0, 0, cw, ch, w, h)).toEqual(IDENTITY)
  })
  it('la misura adattata', () => {
    expect(fit(2000, 1200, 1000, 2000)).toEqual([1000, 600])
    expect(fit(1000, 4000, 1000, 2000)).toEqual([500, 2000])
  })
  it('un mockup alto va a tutta larghezza col doppio tocco e oltre col pizzico', () => {
    const [tw, th] = fit(824, 6652, 1080, 2400)
    const z = doubleTap(IDENTITY, 540, 300, tw, th, 1080, 2400)
    expect(z.scale).toBeCloseTo(1080 / tw)
    expect(z.x).toBeCloseTo(0)
    const max = maxScale(tw, 1080)
    expect(max).toBeCloseTo(3 * 1080 / tw)
    expect(step(z, 20, 0, 0, 540, 300, tw, th, 1080, 2400, max).scale).toBeCloseTo(max)
  })
  it('una immagine larga tiene i limiti di prima', () => { expect(maxScale(cw, w)).toBe(MAX) })
})
