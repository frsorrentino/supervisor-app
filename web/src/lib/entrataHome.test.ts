import { describe, expect, it } from 'vitest'
import { BAR_EASE, BAR_MS, barDelayMs, BLUR_PX, curve, delayMs, FADE_MS, keyframes, Once, plays, RISE_PX, SCALE_FROM, SPRING } from './entrataHome'
import { ratio } from './iconRow'

// L'entrata della home all'avvio (app-entrata-home-27-11, variante B). Gli stessi casi di EntrataHomeTest in Kotlin, più la curva della
// molla che sul web corre in WAAPI.
describe('entrata della home', () => {
  it('il primo blocco parte dopo 70 ms e ognuno 45 ms dopo quello sopra', () => {
    expect([0, 1, 2, 3, 4].map(delayMs)).toEqual([70, 115, 160, 205, 250])
    expect(delayMs(12)).toBe(70 + 45 * 12)
    // Una posizione che non c'è vale la prima.
    expect(delayMs(-3)).toBe(70)
  })

  it('la barra della settimana parte 220 ms dopo il suo riquadro', () => {
    expect(barDelayMs(0)).toBe(290)
    // Il tabellone sotto una domanda aperta: secondo blocco.
    expect(barDelayMs(1)).toBe(335)
    expect(BAR_MS).toBe(800)
    expect(BAR_EASE).toBe('cubic-bezier(.34, 1.56, .64, 1)')
  })

  it('i valori della variante B', () => {
    expect(FADE_MS).toBe(400)
    expect(BLUR_PX).toBe(5)
    expect(RISE_PX).toBe(24)
    expect(SCALE_FROM).toBe(0.94)
    expect(SPRING.k).toBe(220)
    expect(ratio(SPRING)).toBeCloseTo(0.506, 3)
    // Con «riduci movimento» niente molla: la home compare ferma.
    expect(ratio(SPRING, true)).toBe(1)
  })

  it('entra una volta sola, anche se quella volta era ferma', () => {
    const once = new Once()
    expect(once.taken).toBe(false)
    expect(plays(once.take(), false)).toBe(true)
    expect(once.taken).toBe(true)
    // Tornando alla home da una sessione non si ripete.
    expect(plays(once.take(), false)).toBe(false)
    // Con «riduci movimento» la prima volta passa ferma, e dopo resta ferma.
    const still = new Once()
    expect(plays(still.take(), true)).toBe(false)
    expect(plays(still.take(), false)).toBe(false)
  })

  it('la molla del motore sale oltre il posto del 15 % circa e si ferma entro un secondo', () => {
    const xs = curve()
    expect(xs[0]).toBe(1)
    expect(xs.at(-1)).toBe(0)
    // A 60 campioni al secondo: meno di un secondo.
    expect(xs.length - 1).toBeLessThanOrEqual(60)
    // Oltre il posto di 3,6 px su 24, verso 0,25 s (la soluzione esatta del mockup: 15,9 % a 0,246 s).
    const min = Math.min(...xs)
    expect(min).toBeGreaterThan(-0.17)
    expect(min).toBeLessThan(-0.14)
    expect(xs.indexOf(min) / 60).toBeCloseTo(0.24, 1)
    // A 0,1 s vicina alla soluzione esatta (0,401).
    expect(Math.abs(xs[6] - 0.401)).toBeLessThan(0.03)
  })

  it('i fotogrammi chiave partono 24 px sotto a 0,94 e arrivano al posto a 1', () => {
    expect(keyframes([1, 0.5, -0.15, 0])).toEqual([
      { offset: 0, translate: '0 24.00px', scale: '0.9400' },
      { offset: 1 / 3, translate: '0 12.00px', scale: '0.9700' },
      { offset: 2 / 3, translate: '0 -3.60px', scale: '1.0090' },
      { offset: 1, translate: '0 0.00px', scale: '1.0000' },
    ])
  })
})
