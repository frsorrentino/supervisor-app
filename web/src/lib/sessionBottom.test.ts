import { describe, expect, it } from 'vitest'
import { CHIP_FROM, ctxChip, RING_C, ringDash, ringFraction, ringHigh, ringPct, rowShown } from './sessionBottom'

// In basso nella sessione (app-posizioni-sistema-27-11 gruppo 3): la riga delle Azioni, il chip del contesto e l'anellino nel campo.
// Gli stessi casi di SessionBottomTest in Kotlin.
describe('in basso nella sessione', () => {
  it("l'anellino mostra la percentuale del contesto e senza contesto non c'è", () => {
    expect(ringPct(18)).toBe(18)
    expect(ringPct(72)).toBe(72)
    expect(ringPct(104)).toBe(104)
    expect(ringPct(-3)).toBe(0)
    expect(ringPct(17.6)).toBe(18)
    expect(ringPct(null)).toBeNull()
    expect(ringPct(undefined)).toBeNull()
    expect(ringPct(Number.NaN)).toBeNull()
  })

  it("l'arco dell'anellino resta fra vuoto e pieno", () => {
    expect(ringFraction(18)).toBeCloseTo(0.18)
    expect(ringFraction(0)).toBe(0)
    expect(ringFraction(104)).toBe(1)
    expect(ringFraction(-5)).toBe(0)
  })

  it("l'arco si disegna sulla circonferenza da 47,12, come nel mockup", () => {
    expect(RING_C).toBeCloseTo(47.12, 2)
    expect(ringDash(18)).toBe('8.48 47.12')
    expect(ringDash(72)).toBe('33.93 47.12')
    expect(ringDash(130)).toBe('47.12 47.12')
  })

  it("dal 70 per cento l'anellino è ambra", () => {
    expect(CHIP_FROM).toBe(70)
    expect(ringHigh(18)).toBe(false)
    expect(ringHigh(69)).toBe(false)
    expect(ringHigh(70)).toBe(true)
    expect(ringHigh(72)).toBe(true)
    expect(ringHigh(null)).toBe(false)
  })

  it("il chip del contesto c'è dal 70 per cento, a sessione ferma e con un comando di handoff", () => {
    expect(ctxChip(72, true, true)).toBe(72)
    expect(ctxChip(70, true, true)).toBe(70)
    expect(ctxChip(69, true, true)).toBeNull()
    expect(ctxChip(72, false, true)).toBeNull()
    expect(ctxChip(72, true, false)).toBeNull()
    expect(ctxChip(null, true, true)).toBeNull()
  })

  it("la riga delle Azioni c'è con un'azione o col chip, solo a campo vuoto", () => {
    expect(rowShown(2, false, '')).toBe(true)
    expect(rowShown(0, true, '')).toBe(true)
    expect(rowShown(1, true, '  \n')).toBe(true)
    expect(rowShown(0, false, '')).toBe(false)
    expect(rowShown(2, true, 'scegli')).toBe(false)
    expect(rowShown(2, false, 'a')).toBe(false)
  })
})
