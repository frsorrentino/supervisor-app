import { describe, expect, it } from 'vitest'
import { cause, summarize } from './frameStats'

// Il contatore dei fotogrammi nascosto (app-fluidita-06-11).
describe('il contatore dei fotogrammi', () => {
  it('la causa: lo script se pesa metà, poi stile e impaginazione se un terzo, se no il disegno', () => {
    expect(cause({ at: 0, d: 400, layoutFrom: 380, scripts: [{ name: 'flush', d: 250 }, { name: 'x', d: 10 }] })).toEqual({ kind: 'script', name: 'flush' })
    expect(cause({ at: 0, d: 300, layoutFrom: 120, scripts: [{ name: 'flush', d: 40 }] })).toEqual({ kind: 'layout' })
    expect(cause({ at: 100, d: 90, layoutFrom: 180, scripts: [] })).toEqual({ kind: 'paint' })
    expect(cause({ at: 0, d: 90, layoutFrom: null, scripts: [] })).toEqual({ kind: 'paint' })
  })
  it('il riassunto: il fotogramma più lungo, quanti oltre 33 ms, i tre fotogrammi lunghi più lunghi', () => {
    const s = summarize([16.6, 16.7, 50, 120.4, 17], [
      { at: 0, d: 120, layoutFrom: 20, scripts: [] },
      { at: 200, d: 60, layoutFrom: null, scripts: [{ name: 'a', d: 40 }] },
      { at: 400, d: 55, layoutFrom: null, scripts: [] },
      { at: 600, d: 300, layoutFrom: 590, scripts: [{ name: 'fly', d: 280 }] },
    ])
    expect([s.frames, s.max, s.over33]).toEqual([5, 120, 2])
    expect(s.loaf.map(l => l.d)).toEqual([300, 120, 60])
    expect(s.loaf[0].cause).toEqual({ kind: 'script', name: 'fly' })
    expect(summarize([], []).max).toBe(0)
  })
})
