import { describe, expect, it } from 'vitest'
import { barNeeds, barStep, modelText, quotaText, quotaTone, ringTone, STEPS, statusWidth, type BarParts } from './statusBar'

// app-barra-ibrida-28-11: le regole dello stato nella capsula (H1), uguali a StatusBarTest.kt.
const tipica: BarParts = { model: 'Opus 5', effort: 'high', used: 11, ctx: 34 }

describe('stato nella capsula', () => {
  it('la quota si scrive come la pillola di Utilizzo, quanto è usato', () => {
    expect(quotaText(11)).toBe('5h 11%')
    expect(quotaText(0)).toBe('5h 0%')
    expect(quotaText(99.6)).toBe('5h 100%')
  })

  it('la quota è ambra dal 70 % usato e rossa dal 90 %', () => {
    expect(quotaTone(69)).toBe('neutral')
    expect(quotaTone(70)).toBe('warn')
    expect(quotaTone(89)).toBe('warn')
    expect(quotaTone(90)).toBe('alert')
    expect(quotaTone(100)).toBe('alert')
  })

  it("l'anellino del contesto è ambra dal 70 %", () => {
    expect(ringTone(69)).toBe('neutral')
    expect(ringTone(70)).toBe('warn')
  })

  it("prima via l'effort, poi il modello resta finché c'è posto, poi niente", () => {
    expect(modelText('Opus 5', 'high', 'full')).toBe('Opus 5 · high')
    expect(modelText('Opus 5', 'high', 'noEffort')).toBe('Opus 5')
    expect(modelText('Opus 5', 'high', 'small')).toBe('Opus 5')
    expect(modelText('Opus 5', 'high', 'noModel')).toBe('')
    expect(modelText('Opus 5', 'high', 'noPct')).toBe('')
    expect(modelText('Opus 5', null, 'full')).toBe('Opus 5')
  })

  it('ogni passo stringe lo stato, nelle larghezze scritte nel commento', () => {
    const w = STEPS.map((s) => Math.round(statusWidth(s, tipica)))
    expect(w).toEqual([214, 168, 153, 111, 51])
  })

  it('con tutto lo spazio lo stato è intero, e si riduce in ordine stringendo la riga', () => {
    expect(barStep(800, 190, tipica)).toBe('full')
    let last = 0
    for (let x = 800; x >= 200; x -= 1) {
      const i = STEPS.indexOf(barStep(x, 190, tipica))
      expect(i).toBeGreaterThanOrEqual(last)
      last = i
    }
    expect(barStep(200, 190, tipica)).toBe('noPct')
  })

  it('a 360, con le tre schede e il nome più lungo aperto, lo stato resta senza modello; in una colonna da 407 col testo piccolo', () => {
    // Le tre schede misurate sulla web a 360: 47 l'una chiuse, 2 fra l'una e l'altra, Modifiche aperta 59 più 8.
    expect(barStep(360, 212, tipica)).toBe('noModel')
    expect(barStep(407, 212, tipica)).toBe('small')
    expect(barStep(420, 212, tipica)).toBe('noEffort')
  })

  it('al passo scelto la riga ci sta per intero, margini compresi', () => {
    for (let x = 260; x <= 700; x += 9) {
      const s = barStep(x, 190, tipica)
      if (s !== 'noPct') expect(barNeeds(s, 190, tipica)).toBeLessThanOrEqual(x)
    }
  })

  it('col testo grande lo stato si riduce prima', () => {
    expect(statusWidth('full', tipica, 1.3)).toBeGreaterThan(statusWidth('full', tipica))
    expect(STEPS.indexOf(barStep(420, 190, tipica, 1.3))).toBeGreaterThan(STEPS.indexOf(barStep(420, 190, tipica)))
  })

  it('senza quota né contesto lo stato è il solo modello, e senza niente non c\'è', () => {
    expect(statusWidth('full', { ...tipica, used: null, ctx: null })).toBeCloseTo(15 + 6 + 13 * 0.6 * 11, 6)
    expect(statusWidth('noModel', { ...tipica, used: null, ctx: null })).toBe(0)
  })
})
