import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { FORMS, dayAt, formOf, lastIndex, looking, maxIndex, meter, minIndex, scale, yOf, type MonitorsPage } from './monitors'

// Gli stessi casi di MonitorsTest in Kotlin, dalla stessa fixture dei mockup (app-monitoraggi-09-11).
const page = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/monitors-mockup.json', import.meta.url), 'utf8')) as MonitorsPage
const one = (id: string) => page.items.find(m => m.id === id)!

describe('monitors', () => {
  it('i mockup coprono tutto il catalogo', () => {
    expect(page.items.map(m => m.id)).toEqual(['prezzi-portatili', 'adozione-plugin', 'stelle-github', 'quota-gemini', 'sito-online'])
    expect(new Set(page.items.flatMap(m => m.charts ?? []).map(formOf))).toEqual(new Set(FORMS))
    expect(formOf({ form: 'pie' })).toBe(null)
  })
  it("solo una soglia scattata è da guardare", () => {
    expect(looking(page.items)).toBe(2)
    expect(page.items.filter(m => m.status === 'look').map(m => m.id)).toEqual(['prezzi-portatili', 'quota-gemini'])
  })
  it('una scala per grafico, circa tre tacche', () => {
    const users = one('adozione-plugin').charts![0].series![0].values
    expect(scale(users)).toEqual({ lo: 100, hi: 140, ticks: [100, 120, 140] })
    expect(scale(one('adozione-plugin').charts![1].series![0].values, true)).toEqual({ lo: 0, hi: 1000, ticks: [0, 500, 1000] })
    expect(yOf(scale(users), 130)).toBeCloseTo(0.75)
    expect(yOf(scale([7, 7]), 7)).toBeCloseTo(0.5)
  })
  it("i pallini sul minimo, sul massimo e sull'ultimo valore", () => {
    const c = one('prezzi-portatili').charts![0]
    const days = c.days!
    expect(minIndex(c.series![0].values)).toBe(days.indexOf('28/11'))
    expect(lastIndex(c.series![0].values)).toBe(days.indexOf('28/11'))
    expect(minIndex(c.series![1].values)).toBe(days.indexOf('27/11'))
    const u = one('adozione-plugin').charts![0].series![0].values
    expect(u[maxIndex(u)!]).toBe(135)
    expect(lastIndex([null, null])).toBe(null)
  })
  it('il tocco prende il giorno più vicino', () => {
    expect(dayAt(0, 24)).toBe(0); expect(dayAt(1.2, 24)).toBe(23); expect(dayAt(0.5, 24)).toBe(12); expect(dayAt(0.7, 1)).toBe(0)
  })
  it('il metro mette il limite ai quattro quinti', () => {
    expect(meter(80, 80)[0]).toBeCloseTo(0.8); expect(meter(80, 80)[1]).toBe(true)
    expect(meter(60, 80)[1]).toBe(false); expect(meter(500, 80)[0]).toBe(1)
  })
})
