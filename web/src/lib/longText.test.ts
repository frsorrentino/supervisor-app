import { describe, expect, it } from 'vitest'
import { overOne, wordsBeforeBlock } from './longText'

// Il tasto ▶ accanto ai testi oltre una riga (app-play-lettura-ovunque-27-11; prima oltre due, app-tasto-play-03-11).
describe('overOne', () => {
  it('una riga no, due sì', () => {
    expect(overOne(19, 19)).toBe(false)
    expect(overOne(21, 19)).toBe(false)
    expect(overOne(38, 19)).toBe(true)
  })
  it('interlinea «normal» (NaN) vale 20 px', () => {
    expect(overOne(20, NaN)).toBe(false)
    expect(overOne(40, NaN)).toBe(true)
  })
})

describe('wordsBeforeBlock', () => {
  it('le parole dei paragrafi prima di quello letto', () => {
    expect(wordsBeforeBlock([3, 5, 2], 0)).toBe(0)
    expect(wordsBeforeBlock([3, 5, 2], 2)).toBe(8)
  })
})
