import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { FORMS, formOf, isLook, lastIndex, line, looking, maxIndex, meter, minIndex, monitorOf, nearest, scale, span, times, up, values, xOf, yOf } from './monitors'
import { decodeMonitor, decodeMonitors } from './decideDecode'
import type { MonitorsPage } from './contract'

// Gli stessi casi di MonitorsTest in Kotlin (app-monitoraggi-09-11, contratto 1.132): la fixture del relay e quella dei mockup.
const mock = (JSON.parse(readFileSync(new URL('../../../core/src/test/resources/monitors-mockup.json', import.meta.url), 'utf8')) as MonitorsPage).items
const one = (id: string) => mock.find(m => m.id === id)!
const sample = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')) as { cmd: { id: string; op: string; arg: string | null }[]; result: { id: string; ok: boolean; text: string }[] }
const res = (arg: string | null) => sample.result.find(r => r.id === sample.cmd.find(c => c.op === 'monitors' && c.arg === arg)!.id)!

describe('monitors', () => {
  it('la fixture del relay', () => {
    const page = decodeMonitors(res(null).text)
    expect(page.items.map(m => m.id)).toEqual(['adozione', 'prezzi-portatili'])
    const p = page.items[1]
    expect(p.form).toBe('multiples'); expect(isLook(p)).toBe(true); expect(p.value).toBe('1.749 €')
    expect(p.series.map(s => s.name)).toEqual(['Yoga 9i', 'Zenbook 14'])
    expect(p.hits[0].text).toBe('sotto 1.800 € · Yoga 9i')
    expect(monitorOf(p.link!)).toBe('prezzi-portatili')
    expect(decodeMonitor(res('prezzi-portatili').text)).toEqual(p)
    expect(res('vecchio').ok).toBe(false)
  })
  it('i mockup coprono tutto il catalogo', () => {
    expect(new Set(mock.map(formOf))).toEqual(new Set(FORMS))
    expect(formOf({ form: 'pie' })).toBe(null)
  })
  it('solo una soglia scattata è da guardare', () => {
    expect(mock.filter(isLook).map(m => m.id)).toEqual(['prezzi-portatili', 'quota-gemini'])
    expect(looking(null, mock)).toBe(2); expect(looking({ n: 9, watch: 3, at: 0 }, mock)).toBe(3); expect(looking(null, null)).toBe(0)
  })
  it('la frase sotto il titolo', () => {
    expect(line(one('utenti-store'))).toBe('Settimana stabile: 130 utenti nello Store, −1 in 7 giorni.')
    expect(line(one('adozione'))).toBe('−1 in 7 giorni')
  })
  it('una scala per grafico, circa tre tacche', () => {
    const users = values(one('utenti-store').series[0])
    expect(scale(users)).toEqual({ lo: 100, hi: 140, ticks: [100, 120, 140] })
    expect(scale(values(one('download-npm').series[0]), true)).toEqual({ lo: 0, hi: 1000, ticks: [0, 500, 1000] })
    expect(yOf(scale(users), 130)).toBeCloseTo(0.75); expect(yOf(scale([7, 7]), 7)).toBeCloseTo(0.5)
  })
  it("i pallini sul minimo, sul massimo e sull'ultimo valore", () => {
    const yoga = values(one('prezzi-portatili').series[0])
    expect(minIndex(yoga)).toBe(yoga.length - 1); expect(lastIndex(yoga)).toBe(yoga.length - 1)
    const msi = values(one('prezzi-portatili').series[1])
    expect(minIndex(msi)).toBe(msi.length - 2)
    const u = values(one('utenti-store').series[0])
    expect(u[maxIndex(u)!]).toBe(135)
  })
  it("il tocco prende il punto più vicino sull'asse comune", () => {
    const c = one('prezzi-portatili'), sp = span(c), t = times(c.series[0])
    expect(nearest(t, 0, sp)).toBe(0); expect(nearest(t, 1.3, sp)).toBe(t.length - 1); expect(nearest(t, 0.5, sp)).toBe(10)
    expect(xOf(t[t.length - 1], sp)).toBe(1)
  })
  it('il metro si riempie fino a pct con la soglia segnata', () => {
    const [fill, mark] = meter(one('quota-gemini'))
    expect(fill).toBeCloseTo(0.82); expect(mark).toBeCloseTo(0.8)
    expect(up(0)).toBe(false); expect(up(1)).toBe(true)
  })
  it('i link', () => {
    expect(monitorOf('https://supervisor.example.com/#/monitor/prezzi-portatili')).toBe('prezzi-portatili')
    expect(monitorOf('https://supervisor.example.com/?demo#/monitor/adozione')).toBe('adozione')
    expect(monitorOf('https://supervisor.example.com/#/attesa/w123')).toBe(null)
    expect(monitorOf('https://supervisor.example.com/#/monitor/')).toBe(null)
  })
})
