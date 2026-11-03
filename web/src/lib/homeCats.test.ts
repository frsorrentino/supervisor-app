import { describe, expect, it } from 'vitest'
import { DEFAULT_CATS, EDGE, REV, catFor, catKeys, catOf, catOrder, dragCats, flat, fromTop, reorder, decode, encode, moveCat, moveSection, settle, toTop, toggle, top, type Cat, type HomeKey } from './homeCats'
import { pinned } from './homeOrder'

// La home per categorie (compito app-home-categorie-02-11); gli stessi casi in HomeCatsTest.kt.
const ids = (c: Cat[]) => c.map(x => x.id)
describe('homeCats', () => {
  it('il predefinito: quattro categorie, due aperte', () => {
    const d = decode(null)
    expect(ids(d)).toEqual(['you', 'work', 'out', 'tools'])
    // Tocca a te accorpa Prova, Decidi, Guarda e Segui (app-sezioni-accorpate-03-11, scelta A); Prima raccolta sta dentro Decidi.
    expect(d.map(c => c.sections)).toEqual([['prove', 'decisioni', 'da_guardare', 'seguiti', 'suggestions'], ['sessions', 'lavori'], ['esiti', 'rendiconti'], ['usage', 'system', 'pagella']])
    expect(d.map(c => c.open)).toEqual([true, true, false, false])
    expect(decode([])).toEqual(DEFAULT_CATS)
  })
  it('il predefinito si salva come null', () => expect(encode(DEFAULT_CATS, null)).toBeNull())
  it('un ordine salvato torna uguale, con lo slot', () => {
    const cats = toggle(moveCat(DEFAULT_CATS, 1, 0), 'you')
    const saved = JSON.parse(encode(cats, 'usage')!) as string[]
    expect(pinned(saved)).toBe('usage')
    // Lo slot di prima sale sul tabellone (app-tabellone-quattro-02-11): Utilizzo esce da Numeri e strumenti.
    expect(top(saved)).toEqual(['usage'])
    expect(decode(saved)).toEqual(cats.map(c => ({ ...c, sections: c.sections.filter(k => k !== 'usage') })))
    expect(saved.filter(k => k.startsWith('shut:'))).toEqual(['shut:you,out,tools'])
  })
  it('anche nessuna chiusa si salva', () => {
    const all = DEFAULT_CATS.map(c => ({ ...c, open: true }))
    expect(decode(JSON.parse(encode(all, null)!))).toEqual(all)
  })
  it("l'ordine piatto di ieri cade nelle categorie", () => {
    const d = decode(['sessions', 'closed', 'usage', 'prove', 'plugins', 'pin:usage'])
    // Utilizzo, agganciato allo slot, sale sul tabellone; senza il marcatore le categorie riprendono l'ordine dei riquadri
    // e Tocca a te quello di partenza (app-home-nomi-02-11).
    expect(ids(d)).toEqual(['you', 'work', 'out', 'tools'])
    expect(d.find(c => c.id === 'you')!.sections).toEqual(['prove', 'decisioni', 'da_guardare', 'seguiti', 'suggestions'])
    expect(d.flatMap(c => c.sections)).not.toContain('closed')
    expect(d.flatMap(c => c.sections)).not.toContain('plugins')
    expect(d.map(c => [c.id, c.open])).toEqual([['you', true], ['work', true], ['out', false], ['tools', false]])
  })
  it('la lista salvata sta nei limiti del relay', () => {
    const worst = [...DEFAULT_CATS].reverse().map(c => ({ ...c, open: false, sections: [...c.sections].reverse() }))
    const saved = JSON.parse(encode(worst, 'suggestions')!) as string[]
    expect(saved.length).toBeLessThanOrEqual(16)
    expect(saved.every(k => k.length <= 32)).toBe(true)
  })
  it('una sezione non esce dalla sua categoria', () => {
    expect(moveSection(DEFAULT_CATS, 'you', 1, 9)).toEqual(DEFAULT_CATS)
    expect(moveSection(DEFAULT_CATS, 'you', 2, 0)[0].sections).toEqual(['da_guardare', 'prove', 'decisioni', 'seguiti', 'suggestions'])
    expect(catOf('rendiconti')).toBe('out')
    expect(catOf('closed')).toBeNull()
  })
  it("un ordine piatto trascinato tiene categorie e chiuse", () => {
    const r = reorder(toggle(DEFAULT_CATS, 'work'), ['decisioni', 'sessions', 'prove', 'esiti'])
    expect(ids(r)).toEqual(['you', 'work', 'out', 'tools'])
    // Le chiavi che la lista trascinata non ha (non disegnate) tengono il loro posto.
    expect(r[0].sections).toEqual(['decisioni', 'prove', 'da_guardare', 'seguiti', 'suggestions'])
    expect(r.map(c => c.open)).toEqual([true, false, false, false])
  })
  it('le chiavi non disegnate tengono il posto dopo un trascinamento', () => {
    expect(reorder(DEFAULT_CATS, ['system', 'usage']).find(c => c.id === 'tools')!.sections).toEqual(['system', 'usage', 'pagella'])
  })
  it('un trascinamento non esce dalla sua categoria', () => {
    const order = DEFAULT_CATS.flatMap(c => c.sections) as string[]
    const mids = { decisioni: 100, suggestions: 200, sessions: 300 }
    expect(dragCats(order as never, 'suggestions' as never, 350, mids)).toEqual(order)
    expect(dragCats(order as never, 'suggestions' as never, 90, mids).slice(0, 4)).toEqual(['prove', 'suggestions', 'decisioni', 'da_guardare'])
    expect(catFor('closed')).toBe('work')
    expect(catFor('plugins')).toBe('tools')
  })
  it('una categoria trascinata porta con sé sezioni e interruttore', () => {
    const cats = toggle(moveSection(DEFAULT_CATS, 'you', 2, 0), 'out')
    const r = catOrder(cats, ['cat:out', 'cat:you', 'cat:work', 'cat:tools'])
    expect(ids(r)).toEqual(['out', 'you', 'work', 'tools'])
    expect(r[1]).toEqual(cats.find(c => c.id === 'you'))
    expect(r[0].open).toBe(true)
    expect(ids(catOrder(DEFAULT_CATS, ['cat:tools', 'boh']))).toEqual(['tools', 'you', 'work', 'out'])
    expect(catKeys(DEFAULT_CATS)).toEqual(['cat:you', 'cat:work', 'cat:out', 'cat:tools'])
  })

  // Lo spazio in cima (app-home-in-cima-02-11); gli stessi casi in HomeCatsTest.kt.
  it('le voci in cima sostituiscono la loro chiave ed escono dalla categoria', () => {
    const saved = JSON.parse(encode(toTop(decode(null), [], 'sessions').cats, null, ['sessions'])!) as string[]
    expect(saved[0]).toBe('top:sessions')
    expect(saved).not.toContain('sessions')
    expect(top(saved)).toEqual(['sessions'])
    expect(decode(saved)[1].sections).toEqual(['lavori'])
    expect(saved.length <= 16 && saved.every(k => k.length <= 32)).toBe(true)
    const all = DEFAULT_CATS.flatMap(c => c.sections)
    expect((JSON.parse(encode(DEFAULT_CATS.map(c => ({ ...c, sections: [] })), 'usage', all)!) as string[]).length).toBe(15)
    expect(top(['sessions', 'shut:out'])).toEqual([])
    expect(top(['top:esiti', 'top:boh', 'top:esiti'])).toEqual(['esiti'])
    expect(encode(DEFAULT_CATS, null, [])).toBeNull()
  })
  it('la lista trascinata ha il bordo dopo la cima', () => {
    const f = flat(['esiti'], decode(['top:esiti']))
    expect(f.slice(0, 3)).toEqual(['esiti', EDGE, 'prove'])
    expect(f.length).toBe(13)
  })
  it('settle divide al bordo', () => {
    const cats = decode(null)
    const up = ['esiti', EDGE, ...flat([], cats).filter(k => k !== 'esiti' && k !== EDGE)]
    const r1 = settle(cats, up)
    expect(r1.top).toEqual(['esiti'])
    expect(r1.cats[2].sections).toEqual(['rendiconti'])
    const down = [EDGE, 'suggestions', 'esiti', ...flat(r1.top, r1.cats).filter(k => !['esiti', EDGE, 'suggestions'].includes(k))]
    const r2 = settle(r1.cats, down)
    expect(r2.top).toEqual([])
    expect(r2.cats[0].sections).toEqual(['suggestions', 'prove', 'decisioni', 'da_guardare', 'seguiti'])
    expect(r2.cats[2].sections).toContain('esiti')
    expect(r2.cats.map(c => c.open)).toEqual(cats.map(c => c.open))
    const r3 = settle(cats, ['lavori', EDGE, 'sessions'])
    expect(r3.top).toEqual(['lavori'])
    expect(r3.cats[1].sections).toEqual(['sessions'])
    expect(r3.cats[3]).toEqual(cats[3])
  })
  it('una sezione scavalca solo la cima, il bordo e la sua categoria', () => {
    const order = ['esiti', EDGE, 'decisioni', 'prove', 'sessions', 'lavori']
    const mids: Record<string, number> = { esiti: 10, [EDGE]: 30, decisioni: 50, prove: 70, lavori: 110 }
    expect(dragCats(order, 'sessions', 60, mids)).toEqual(order)
    expect(dragCats(order, 'sessions', 25, mids)).toEqual(['esiti', 'sessions', EDGE, 'decisioni', 'prove', 'lavori'])
    const t = ['esiti', 'sessions', EDGE, 'decisioni']
    expect(dragCats(t, 'sessions', 5, { esiti: 10, [EDGE]: 30 })).toEqual(['sessions', 'esiti', EDGE, 'decisioni'])
  })
  it('Riordina mette e toglie dalla cima', () => {
    const a = toTop(DEFAULT_CATS, ['esiti'], 'prove')
    expect(a.top).toEqual(['esiti', 'prove'])
    expect(a.cats[0].sections).toEqual(['decisioni', 'da_guardare', 'seguiti', 'suggestions'])
    const b = fromTop(a.cats, a.top, 'prove')
    expect(b.top).toEqual(['esiti'])
    // Torna al suo posto, non in fondo (app-cima-rilascio-reset-03-11).
    expect(b.cats[0].sections).toEqual(['prove', 'decisioni', 'da_guardare', 'seguiti', 'suggestions'])
    expect(toTop(a.cats, a.top, 'prove')).toEqual(a)
    expect(fromTop(b.cats, b.top, 'prove')).toEqual(b)
  })

  // Il tabellone a quattro voci (app-tabellone-quattro-02-11); gli stessi casi in HomeCatsTest.kt.
  it('il tabellone tiene quattro voci e lo slot di prima ci sale sopra', () => {
    let t: HomeKey[] = [], c = DEFAULT_CATS
    for (const k of ['esiti', 'sessions', 'usage', 'prove', 'decisioni'] as HomeKey[]) { const r = toTop(c, t, k); t = r.top; c = r.cats }
    expect(t).toEqual(['esiti', 'sessions', 'usage', 'prove'])
    expect(c[0].sections).toContain('decisioni')
    expect(top(JSON.parse(encode(c, null, t)!))).toEqual(t)
    expect(top(['sessions', 'pin:usage'])).toEqual(['usage'])
    expect(top(['top:esiti', 'pin:usage'])).toEqual(['esiti'])
    expect((JSON.parse(encode(decode(['pin:usage']), null, ['usage'])!) as string[]).some(k => k.startsWith('pin:'))).toBe(false)
    expect(top(['top:esiti', 'top:sessions', 'top:usage', 'top:prove', 'top:lavori'])).toHaveLength(4)
  })
  it('una sezione presa nella sua categoria non passa il bordo', () => {
    const order = ['esiti', EDGE, 'sessions', 'lavori']
    expect(dragCats(order as never, 'sessions', 5, { esiti: 10, [EDGE]: 30, lavori: 110 } as never, false)).toEqual(order)
    expect(dragCats(['esiti', 'sessions', EDGE, 'lavori'] as never, 'sessions', 5, { esiti: 10, [EDGE]: 30 } as never, true)).toEqual(['sessions', 'esiti', EDGE, 'lavori'])
  })
})

describe("app-home-nomi-02-11: l'ordine nuovo una volta", () => {
  it("un ordine salvato prima del marcatore riprende l'ordine dei riquadri e quello di partenza", () => {
    const old = ['top:esiti', 'usage', 'sessions', 'lavori', 'seguiti', 'suggestions', 'prove', 'decisioni', 'prima_raccolta', 'system', 'pagella', 'da_guardare', 'rendiconti', 'shut:tools']
    const d = decode(old)
    expect(d.map(c => c.id)).toEqual(['you', 'work', 'out', 'tools'])
    expect(d[0].sections).toEqual(['prove', 'decisioni', 'da_guardare', 'seguiti', 'suggestions'])
    expect(d[1].sections).toEqual(['sessions', 'lavori'])
    expect(d[2].sections).toEqual(['rendiconti'])
    expect(d[3].sections).toEqual(['usage', 'system', 'pagella'])
    expect(d.map(c => c.open)).toEqual([true, true, true, false])
    expect(top(old)).toEqual(['esiti'])
    const saved = JSON.parse(encode(moveCat(d, 3, 0), null, ['esiti'])!) as string[]
    expect(saved).toContain(REV)
    expect(saved.length).toBeLessThanOrEqual(16)
    expect(decode(saved).map(c => c.id)).toEqual(['tools', 'you', 'work', 'out'])
  })
})

describe('app-sezioni-accorpate-03-11: Tocca a te riprende una volta il suo ordine', () => {
  it("un ordine rev:2 tiene categorie, chiuse e cima; dentro Tocca a te riparte da Prova", () => {
    const old = ['top:da_guardare', 'usage', 'system', 'pagella', 'sessions', 'lavori', 'seguiti', 'suggestions', 'prove', 'decisioni', 'prima_raccolta', 'esiti', 'rendiconti', 'shut:out', 'rev:2']
    const d = decode(old)
    expect(d.map(c => c.id)).toEqual(['tools', 'work', 'you', 'out'])
    expect(d.find(c => c.id === 'you')!.sections).toEqual(['prove', 'decisioni', 'seguiti', 'suggestions'])
    expect(d.find(c => c.id === 'work')!.sections).toEqual(['sessions', 'lavori'])
    expect(d.map(c => c.open)).toEqual([true, true, true, false])
    expect(top(old)).toEqual(['da_guardare'])
    const saved = JSON.parse(encode(moveSection(d, 'you', 3, 0), null, top(old))!) as string[]
    expect(saved).toContain(REV)
    expect(REV).toBe('rev:3')
    expect(decode(saved).find(c => c.id === 'you')!.sections).toEqual(['suggestions', 'prove', 'decisioni', 'seguiti'])
  })
})
