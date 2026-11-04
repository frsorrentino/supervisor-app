import { describe, expect, it } from 'vitest'
import { overTwo } from './longText'

// Il tasto ▶ accanto ai testi oltre due righe (app-tasto-play-03-11).
describe('overTwo', () => {
  it('due righe no, tre sì', () => {
    expect(overTwo(38, 19)).toBe(false)
    expect(overTwo(40, 19)).toBe(false)
    expect(overTwo(57, 19)).toBe(true)
  })
  it('interlinea «normal» (NaN) vale 20 px', () => {
    expect(overTwo(40, NaN)).toBe(false)
    expect(overTwo(60, NaN)).toBe(true)
  })
})
