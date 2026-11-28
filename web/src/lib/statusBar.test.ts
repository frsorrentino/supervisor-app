import { describe, expect, it } from 'vitest'
import {
  BAT, barNeeds, barStep, batteryTone, ITEM_GAP, leftWidth, modelText, PAD_SHEET, remaining, RING, rightWidth, ringTone, STEPS, type BarParts,
} from './statusBar'

// app-barra-stato-28-11: le regole della barra di stato, uguali a StatusBarTest.kt.
const tipica: BarParts = { counts: [3, 1, 4], model: 'Opus 5', effort: 'high', left: 89, ctx: 34 }

describe('barra di stato', () => {
  it('la batteria dice quanto resta delle 5 ore, fra 0 e 100', () => {
    expect(remaining(11)).toBe(89)
    expect(remaining(0)).toBe(100)
    expect(remaining(100)).toBe(0)
    expect(remaining(120)).toBe(0)
    expect(remaining(-5)).toBe(100)
    expect(remaining(10.4)).toBe(90)
  })

  it('la batteria è ambra quando resta il 30 % o meno e rossa col 10 % o meno', () => {
    expect(batteryTone(31)).toBe('neutral')
    expect(batteryTone(30)).toBe('warn')
    expect(batteryTone(11)).toBe('warn')
    expect(batteryTone(10)).toBe('alert')
    expect(batteryTone(0)).toBe('alert')
  })

  it("l'anellino del contesto è ambra dal 70 %", () => {
    expect(ringTone(69)).toBe('neutral')
    expect(ringTone(70)).toBe('warn')
    expect(ringTone(100)).toBe('warn')
  })

  it("il modello con l'effort ai primi due passi, poi senza, e coi soli glifi niente", () => {
    expect(modelText('Opus 5', 'high', 'full')).toBe('Opus 5 · high')
    expect(modelText('Opus 5', 'high', 'small')).toBe('Opus 5 · high')
    expect(modelText('Opus 5', 'high', 'noEffort')).toBe('Opus 5')
    expect(modelText('Opus 5', 'high', 'inside')).toBe('Opus 5')
    expect(modelText('Opus 5', 'high', 'glyphs')).toBe('')
    expect(modelText('Opus 5', null, 'full')).toBe('Opus 5')
    expect(modelText(null, null, 'full')).toBe('')
  })

  it('con tutto lo spazio la barra scrive tutto, a 360 come in una colonna larga', () => {
    expect(barStep(360, tipica)).toBe('full')
    expect(barStep(407, tipica)).toBe('full')
  })

  it('stringendo passa per i passi in ordine, alle soglie scritte nel commento', () => {
    expect(barStep(319, tipica)).toBe('full')
    expect(barStep(318, tipica)).toBe('small')
    expect(barStep(302, tipica)).toBe('small')
    expect(barStep(301, tipica)).toBe('noEffort')
    expect(barStep(264, tipica)).toBe('noEffort')
    expect(barStep(263, tipica)).toBe('inside')
    expect(barStep(237, tipica)).toBe('inside')
    expect(barStep(236, tipica)).toBe('glyphs')
    expect(barStep(120, tipica)).toBe('glyphs')
    let last = 0
    for (let w = 420; w >= 120; w -= 1) {
      const i = STEPS.indexOf(barStep(w, tipica))
      expect(i).toBeGreaterThanOrEqual(last)
      last = i
    }
  })

  it('al passo scelto il testo ci sta per intero, margini compresi', () => {
    for (let w = 190; w <= 420; w += 7) {
      const s = barStep(w, tipica)
      expect(barNeeds(s, tipica)).toBeLessThanOrEqual(w)
    }
  })

  it('con il testo grande la barra si stringe prima: a 360 il testo piccolo', () => {
    expect(barStep(360, tipica, 1.3)).toBe('small')
    expect(barStep(360, tipica, 2)).toBe('noEffort')
    expect(leftWidth(tipica.counts, 1.3)).toBeGreaterThan(leftWidth(tipica.counts))
  })

  it('i numeri delle icone restano a ogni passo, anche coi soli glifi', () => {
    const conNumeri = leftWidth([3, 1, 4])
    expect(conNumeri).toBeGreaterThan(leftWidth([0, 0, 0]))
    expect(barNeeds('glyphs', tipica) - rightWidth('glyphs', tipica)).toBeCloseTo(barNeeds('full', tipica) - rightWidth('full', tipica), 6)
    expect(rightWidth('glyphs', tipica)).toBe(BAT + ITEM_GAP + RING)
  })

  it('a zero solo l\'icona: un numero a due cifre allarga la sinistra', () => {
    expect(leftWidth([0, 0, 0])).toBe(17 * 3 + 10 * 2)
    expect(leftWidth([12, 0, 0])).toBeGreaterThan(leftWidth([1, 0, 0]))
  })

  it('nel foglio della master i margini sono 16: lo stesso testo passa al passo dopo 8 px prima', () => {
    expect(barNeeds('full', tipica, 1, PAD_SHEET) - barNeeds('full', tipica)).toBe(8)
    expect(barStep(319, tipica)).toBe('full')
    expect(barStep(319, tipica, 1, PAD_SHEET)).toBe('small')
    expect(barStep(327, tipica, 1, PAD_SHEET)).toBe('full')
  })

  it('senza quota né contesto a destra resta solo il modello', () => {
    const solo: BarParts = { ...tipica, left: null, ctx: null }
    expect(rightWidth('inside', solo)).toBeCloseTo(6 * 0.6 * 9, 6)
    expect(rightWidth('glyphs', solo)).toBe(0)
    expect(barStep(300, solo)).toBe('full')
  })
})
