import { describe, expect, it } from 'vitest'
import { DRAG_PX, springFrames } from './gesture'

// app-posizioni-sistema-27-11 gruppi 5 e 6: restano le molle e la soglia del trascinamento del foglio; via i clic delle maniglie, la corsa
// breve e la soglia della barra di prima.
describe('il cambio di lato che continua il gesto (app-lato-animazione-06-11)', () => {
  it('parte da dove l\'ha lasciato il dito, finisce a 0 senza rimbalzo, in circa 300 ms', () => {
    const f = springFrames(400, 0)
    expect(f[0]).toBe(400)
    expect(f[f.length - 1]).toBe(0)
    expect(f.every(x => x >= 0)).toBe(true)
    expect(f.findIndex(x => x < 400 * 0.05) / 60).toBeLessThan(0.3)
  })
  it('la velocità del dito si vede all\'inizio: verso il posto accelera, al contrario prima prosegue un poco', () => {
    expect(springFrames(400, -2000)[1]).toBeLessThan(springFrames(400, 0)[1])
    expect(springFrames(-100, -1500)[1]).toBeLessThan(-100)
  })
  // app-altezza-senza-rimbalzo-06-11: con qualunque rilascio la molla arriva e si ferma, mai oltre l'arrivo né indietro.
  it("non supera mai l'arrivo, anche con un colpo veloce verso il posto", () => {
    for (const from of [-400, -40, -3, 3, 40, 400]) for (const v of [-6000, -3000, -800, 0, 800, 3000, 6000]) {
      const f = springFrames(from, v)
      expect(f.every(x => x * from >= 0), `da ${from} a ${v} px/s`).toBe(true)
      // Dall'ultimo tratto verso l'arrivo non torna indietro.
      const turn = f.findIndex((x, i) => i > 0 && Math.abs(x) < Math.abs(f[i - 1]))
      if (turn > 0) for (let i = turn + 1; i < f.length; i++) expect(Math.abs(f[i])).toBeLessThanOrEqual(Math.abs(f[i - 1]) + 1e-9)
    }
    // Da 40 px a 3000 px/s: la molla più rigida (ω = 75) parte con la velocità del dito, x = 40·e^(−75t).
    expect(springFrames(40, -3000)[1]).toBeCloseTo(40 * Math.exp(-75 / 60), 6)
  })
})

describe('il trascinamento del foglio della master', () => {
  it('comincia oltre 6 px, come MasterSheet', () => {
    expect(DRAG_PX).toBe(6)
  })
})
