import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOUBLE_MS, isDrag, masterTaps, springFrames, step, SWALLOW_MS } from './gesture'

// Le maniglie della master senza conflitti fra clic, doppio clic e trascinamento (app-master-gesti-senza-conflitti-04-11).
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

describe('il riconoscitore delle maniglie della master', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('oltre 6 px è un trascinamento, fermo no', () => {
    expect(isDrag(4, 4)).toBe(false)
    expect(isDrag(5, 4)).toBe(true)
  })
  it('1. fermo, al rilascio è un clic', () => {
    const g = masterTaps(), single = vi.fn()
    g.tap('stack', 0, single)
    expect(single).toHaveBeenCalledTimes(1)
  })
  it('2. due clic fermi entro 300 ms sono un doppio clic: un gesto solo', () => {
    const g = masterTaps(), single = vi.fn(), double = vi.fn()
    g.tap('stack', 0, single)
    g.tap('stack', 200, single)
    expect(single).toHaveBeenCalledTimes(1)
    const s2 = vi.fn()
    g.tap('head', 1000, s2, double)
    g.tap('head', 1000 + DOUBLE_MS - 10, s2, double)
    vi.advanceTimersByTime(DOUBLE_MS)
    expect(double).toHaveBeenCalledTimes(1)
    expect(s2).not.toHaveBeenCalled()
    g.tap('stack', 5000, single)
    g.tap('stack', 5000 + DOUBLE_MS + 1, single)
    expect(single).toHaveBeenCalledTimes(3)
  })
  it('3. un trascinamento annulla il clic in sospeso e fa ignorare il click del browser dopo il rilascio', () => {
    const g = masterTaps(), single = vi.fn(), double = vi.fn()
    g.tap('head', 0, single, double)
    g.dragged(50)
    vi.advanceTimersByTime(DOUBLE_MS * 2)
    expect(single).not.toHaveBeenCalled()
    g.released(900)
    expect(g.swallow(900 + SWALLOW_MS - 1)).toBe(true)
    expect(g.swallow(900 + SWALLOW_MS)).toBe(false)
  })
  it('4. se la seconda pressione diventa un trascinamento vince il trascinamento', () => {
    const g = masterTaps(), single = vi.fn(), double = vi.fn()
    g.tap('head', 0, single, double)
    g.dragged(150)
    g.tap('head', 250, single, double)
    vi.advanceTimersByTime(DOUBLE_MS)
    expect(double).not.toHaveBeenCalled()
    expect(single).toHaveBeenCalledTimes(1)
  })
})

// La corsa breve (app-master-corsa-breve-05-11), gli stessi casi di MasterGesturesTest.kt.
describe('step', () => {
  const stops = [0, 300, 700, 1000]
  it('conta la direzione oltre 24 px', () => {
    expect(step(stops, 1, 10)).toBe(1)
    expect(step(stops, 1, -23)).toBe(1)
    expect(step(stops, 1, 24)).toBe(2)
    expect(step(stops, 1, -30)).toBe(0)
    expect(step(stops, 1, 650)).toBe(3)
    expect(step(stops, 3, 80)).toBe(3)
  })
  it('il colpo conta e si proietta', () => {
    expect(step(stops, 1, 5, 600)).toBe(2)
    expect(step(stops, 1, 30, -800)).toBe(0)
    expect(step(stops, 1, 40, 1500)).toBe(3)
    expect(step(stops, 1, 5, 400)).toBe(1)
  })
})
