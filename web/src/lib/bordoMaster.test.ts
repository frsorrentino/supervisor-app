import { describe, expect, it } from 'vitest'
import { ANELLO_FERMO, ANELLO_RIDOTTO, ANGOLO_PX, anello, COLORE, gira, GIRO_MS, posizione, RAGGIO_PX, SFUMA_MS, SPESSORE_PX } from './bordoMaster'

// Il bordo della master (app-bordo-master-27-11). Gli stessi casi di BordoMasterTest in Kotlin.
describe('bordo della master', () => {
  it('i valori della nota del mockup', () => {
    expect(COLORE).toBe('#93A696')
    expect(SPESSORE_PX).toBe(1.5)
    expect(ANGOLO_PX).toBe(24)
    expect(RAGGIO_PX).toBe(220)
    expect(GIRO_MS).toBe(15_000)
    expect(SFUMA_MS).toBe(240)
    expect([ANELLO_FERMO, ANELLO_RIDOTTO]).toEqual([0.12, 0.35])
  })

  it("l'anello al 12 % sempre, al 35 % mentre lavora con «riduci movimento»", () => {
    expect(anello(false, false)).toBe(0.12)
    expect(anello(true, false)).toBe(0.12)
    expect(anello(false, true)).toBe(0.12)
    expect(anello(true, true)).toBe(0.35)
  })

  it('il raggio gira solo mentre la master lavora e il movimento non è ridotto', () => {
    expect(gira(true, false)).toBe(true)
    expect(gira(true, true)).toBe(false)
    expect(gira(false, false)).toBe(false)
    expect(gira(false, true)).toBe(false)
  })

  it("un giro ogni 15 secondi verso antiorario, dall'inizio del lato di sopra", () => {
    expect(posizione(0)).toBe(0)
    // Verso antiorario la frazione del bordo, misurata in senso orario, cala.
    expect(posizione(3_750)).toBeCloseTo(0.75, 6)
    expect(posizione(7_500)).toBeCloseTo(0.5, 6)
    expect(posizione(11_250)).toBeCloseTo(0.25, 6)
    expect(posizione(12_000)).toBeCloseTo(0.2, 6)
    expect(posizione(15_000)).toBe(0)
    // Lineare: un secondo vale sempre un quindicesimo di giro.
    expect(posizione(1_000) - posizione(2_000)).toBeCloseTo(1 / 15, 6)
  })

  it("il giro dall'orologio, anche con un tempo grande o prima dello zero", () => {
    // Le 22:21 del 27/11/2026, ora di Roma, in ms: un multiplo di 15 s, il raggio è all'inizio.
    expect(posizione(1_795_814_460_000)).toBe(0)
    expect(new Date(1_795_814_460_000).toISOString()).toBe('2026-11-27T21:21:00.000Z')
    expect(posizione(1_795_814_463_750)).toBeCloseTo(0.75, 6)
    expect(posizione(-3_750)).toBeCloseTo(0.25, 6)
    for (const ms of [-20_000, -1, 1, 14_999, 1_795_814_469_999]) {
      const p = posizione(ms)
      expect(p >= 0 && p < 1, `${ms}: ${p}`).toBe(true)
    }
  })
})
