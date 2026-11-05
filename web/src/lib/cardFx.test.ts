import { describe, expect, it } from 'vitest'
import { LIFT_PX, NECK_MS, neckPath, nubPath, waistAt, WAIST } from './cardFx'

// Il collo della scheda che diventa colonna (app-effetti-movimenti-04-11, punto 1).
describe('cardFx', () => {
  it('la vita va dal 100 % al 25 % della larghezza', () => {
    expect(waistAt(0)).toBe(1)
    expect(waistAt(1)).toBeCloseTo(WAIST)
    expect(waistAt(0.5)).toBeGreaterThan(waistAt(0.9))
    expect(waistAt(2)).toBeCloseTo(WAIST)
  })
  it('il ponte tocca i due bordi a tutta larghezza e si stringe al centro', () => {
    const d = neckPath(200, 0, 20, 0.25)
    expect(d.startsWith('M0 0 ')).toBe(true)
    expect(d).toContain('L200 20')
    // I due fianchi passano per il centro a 75 e 125 (25 % di 200).
    expect(d).toContain('75 10')
    expect(d).toContain('125 10')
  })
  it('un capo spezzato è una goccia appesa al suo bordo', () => {
    expect(nubPath(200, 0, 5)).toBe('M60 0 Q100 10 140 0 Z')
    expect(nubPath(200, 22, -5)).toBe('M60 22 Q100 12 140 22 Z')
  })
  it('il collo è breve e basso: il volo parte subito (Franz 05/11 10:23)', () => {
    expect(NECK_MS).toBeLessThanOrEqual(140)
    expect(LIFT_PX).toBeLessThanOrEqual(10)
  })
})
