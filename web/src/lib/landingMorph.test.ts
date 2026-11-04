import { describe, expect, it } from 'vitest'
import { markBox } from './landingMorph'

// Il segno d'arrivo che si allarga nel posto della master (app-trascina-morphing-03-11).
describe('markBox', () => {
  it('un segno verticale: 4 px di larghezza, centrato sul suo punto', () => {
    const b = markBox({ x: 100, y: 400, v: true })
    expect(b.w).toBe(4)
    expect(b.x + b.w / 2).toBe(100)
    expect(b.y + b.h / 2).toBe(400)
    expect(b.h).toBeCloseTo(100.8)
  })
  it('un segno orizzontale: 4 px di altezza', () => {
    const b = markBox({ x: 300, y: 24, v: false })
    expect(b.h).toBe(4)
    expect(b.x + b.w / 2).toBe(300)
  })
})
