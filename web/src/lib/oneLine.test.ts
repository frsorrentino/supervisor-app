import { describe, expect, it } from 'vitest'
import { count, parts, short, SEP } from './oneLine'

// Il riassunto a destra su una riga (app-riassunti-una-riga-03-11); gli stessi casi in OneLineTest.kt.
describe('oneLine', () => {
  const p = ['22 da decidere', '50 da provare', '5 suggerimenti']
  const w = (k: number) => p.slice(0, k).join(SEP).length
  it('all parts when they fit', () => expect(count(p.length, 100, w)).toBe(3))
  it('the parts fall whole from the last', () => {
    expect(count(p.length, w(2), w)).toBe(2)
    expect(count(p.length, w(3) - 1, w)).toBe(2)
    expect(count(p.length, w(2) - 1, w)).toBe(1)
  })
  it('zero when not even the first fits', () => expect(count(p.length, w(1) - 1, w)).toBe(0))
  it('nothing to fit', () => expect(count(0, 100, w)).toBe(0))
  it('the first part shrinks to its number', () => {
    expect(short('22 da decidere')).toBe('22')
    expect(short('carico 0,2')).toBe('0,2')
    expect(short('47%')).toBe('47%')
    expect(short('3 ti aspettano')).toBe('3')
    expect(short('Da decidere 2')).toBe('2')
    expect(short('niente oggi')).toBe('niente oggi')
  })
  it('the summary is split on the dots', () => {
    expect(parts('1 al lavoro · 1 finita')).toEqual(['1 al lavoro', '1 finita'])
    expect(parts('30 oggi')).toEqual(['30 oggi'])
    expect(parts(' ')).toEqual([])
  })
})
