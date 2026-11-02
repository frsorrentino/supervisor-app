import { describe, expect, it } from 'vitest'
import { DEFAULT_CATS, catOf, decode, encode, moveCat, moveSection, toggle, type Cat } from './homeCats'
import { pinned } from './homeOrder'

// La home per categorie (compito app-home-categorie-02-11); gli stessi casi in HomeCatsTest.kt.
const ids = (c: Cat[]) => c.map(x => x.id)
describe('homeCats', () => {
  it('il predefinito: quattro categorie, due aperte', () => {
    const d = decode(null)
    expect(ids(d)).toEqual(['you', 'work', 'out', 'tools'])
    expect(d.map(c => c.sections)).toEqual([['prima_raccolta', 'decisioni', 'prove', 'suggestions'], ['sessions', 'seguiti', 'lavori'], ['esiti', 'da_guardare', 'rendiconti'], ['usage', 'pagella', 'system']])
    expect(d.map(c => c.open)).toEqual([true, true, false, false])
    expect(decode([])).toEqual(DEFAULT_CATS)
  })
  it('il predefinito si salva come null', () => expect(encode(DEFAULT_CATS, null)).toBeNull())
  it('un ordine salvato torna uguale, con lo slot', () => {
    const cats = toggle(moveCat(DEFAULT_CATS, 1, 0), 'you')
    const saved = JSON.parse(encode(cats, 'usage')!) as string[]
    expect(decode(saved)).toEqual(cats)
    expect(pinned(saved)).toBe('usage')
    expect(saved.filter(k => k.startsWith('shut:'))).toEqual(['shut:you,out,tools'])
  })
  it('anche nessuna chiusa si salva', () => {
    const all = DEFAULT_CATS.map(c => ({ ...c, open: true }))
    expect(decode(JSON.parse(encode(all, null)!))).toEqual(all)
  })
  it("l'ordine piatto di ieri cade nelle categorie", () => {
    const d = decode(['sessions', 'closed', 'usage', 'prove', 'plugins', 'pin:usage'])
    expect(ids(d)).toEqual(['work', 'tools', 'you', 'out'])
    expect(d.find(c => c.id === 'you')!.sections).toEqual(['prove', 'prima_raccolta', 'decisioni', 'suggestions'])
    expect(d.flatMap(c => c.sections)).not.toContain('closed')
    expect(d.flatMap(c => c.sections)).not.toContain('plugins')
    expect(d.map(c => [c.id, c.open])).toEqual([['work', true], ['tools', false], ['you', true], ['out', false]])
  })
  it('la lista salvata sta nei limiti del relay', () => {
    const worst = [...DEFAULT_CATS].reverse().map(c => ({ ...c, open: false, sections: [...c.sections].reverse() }))
    const saved = JSON.parse(encode(worst, 'prima_raccolta')!) as string[]
    expect(saved.length).toBeLessThanOrEqual(16)
    expect(saved.every(k => k.length <= 32)).toBe(true)
  })
  it('una sezione non esce dalla sua categoria', () => {
    expect(moveSection(DEFAULT_CATS, 'you', 1, 9)).toEqual(DEFAULT_CATS)
    expect(moveSection(DEFAULT_CATS, 'you', 2, 0)[0].sections).toEqual(['prove', 'prima_raccolta', 'decisioni', 'suggestions'])
    expect(catOf('rendiconti')).toBe('out')
    expect(catOf('closed')).toBeNull()
  })
})
