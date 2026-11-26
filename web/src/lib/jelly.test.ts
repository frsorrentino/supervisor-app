import { describe, expect, it } from 'vitest'
import { neck, rubber } from './jelly'

// Voce 5 del terzo giro (gelatina b): l'elastico e il collo calcolato, gli stessi del telefono.
describe('jelly', () => {
  it("l'elastico lascia stare dentro la corsa e non supera 18 px fuori", () => {
    expect(rubber(50, 12, 100)).toBe(50)
    expect(rubber(500, 12, 100)).toBeGreaterThan(100)
    expect(rubber(500, 12, 100)).toBeLessThan(118)
    expect(rubber(-500, 12, 100)).toBeGreaterThan(-6)
  })
  it('il collo c\'è fra pollice e goccia vicini, non quando sono lontani', () => {
    expect(neck(0, 30, 12, 0, -8, 18, 46)).toMatch(/^M.*C.*L.*C.*Z$/)
    expect(neck(0, 30, 12, 0, -200, 18, 46)).toBeNull()
  })
})
