import { describe, expect, it } from 'vitest'
import { blobShape, progress, STRETCH_MAX } from './morphBlob'

// Il blocco della master che cambia forma col dito (app-master-morph-m3-06-11).
const bar = { x: 0, y: 900, w: 1500, h: 60 }
const col = { x: 1080, y: 0, w: 420, h: 960 }

describe('il morph della master', () => {
  it('l\'avanzamento va da 0 alla partenza a 1 sul posto d\'arrivo, e non oltre', () => {
    expect(progress(bar, col, 0)).toBe(0)
    expect(progress(bar, col, -450)).toBe(0.5)
    expect(progress(bar, col, -2000)).toBe(1)
    expect(progress(bar, col, 300)).toBe(0)
  })
  it('alla partenza è la riga, all\'arrivo la colonna, con gli angoli tondi solo in mezzo', () => {
    const a = blobShape(bar, col, 0, 750, 930)
    expect([a.x, a.y, a.w, a.h]).toEqual([0, 900, 1500, 60])
    expect(a.rBottom).toBe(0)
    const z = blobShape(bar, col, 1, 1290, 0)
    expect([Math.round(z.x), z.y, z.w, z.h]).toEqual([1080, 0, 420, 960])
    expect(blobShape(bar, col, 0.5, 900, 500).rBottom).toBeGreaterThan(0)
  })
  it('la goccia: il bordo verso il dito si allunga fino a un massimo e si arrotonda', () => {
    const near = blobShape(bar, col, 0.1, 750, 700)
    const far = blobShape(bar, col, 0.1, 750, 100)
    const plain = blobShape(bar, col, 0.1, 750, 900)
    expect(near.y).toBeLessThan(plain.y)
    expect(plain.y - far.y).toBe(STRETCH_MAX)
    expect(far.rTop).toBeGreaterThan(plain.rTop)
  })
})
