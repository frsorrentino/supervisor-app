import { describe, expect, it } from 'vitest'
import { DEFAULT_CATS, EDGE, FOLDING, REV, catFor, catKeys, catOf, catOrder, dragCats, flat, fromTop, reorder, decode, encode, moveCat, moveSection, settle, toTop, toggle, top, type Cat, type HomeKey } from './homeCats'
import { pinned } from './homeOrder'

// La home per categorie (compito app-home-categorie-02-11); gli stessi casi in HomeCatsTest.kt.
const ids = (c: Cat[]) => c.map(x => x.id)
describe('homeCats', () => {
  it('nelle colonne della plancia ogni voce di Dalla master si apre e si chiude col titolo, Monitoraggi compresi (app-monitoraggi-testata-10-11)', () => {
    const you = DEFAULT_CATS.find(c => c.id === 'you')!.sections
    expect(you.filter(k => !FOLDING.has(k))).toEqual([])
    expect(FOLDING.has('monitors')).toBe(true)
  })
  it('il predefinito: tre categorie, aperta solo Dalla master (app-home-sessioni-a-se-08-11)', () => {
    const d = decode(null)
    expect(ids(d)).toEqual(['you', 'work', 'tools'])
    // Tocca a te in quattro sezioni (app-tocca-sezioni-06-11): Decidi, Verifica (prove e Da guardare), Riprendi, Suggerimenti.
    expect(d.map(c => c.sections)).toEqual([['decisioni', 'prove', 'seguiti', 'suggestions', 'monitors'], ['notte', 'lavori', 'esiti', 'rendiconti'], ['usage', 'system', 'pagella']])
    // Le sessioni stanno a sé, fuori dalle categorie; Lavori parte chiusa (tavola A).
    expect(catOf('sessions')).toBeNull()
    expect(d.map(c => c.open)).toEqual([false, false, false])
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
    expect(saved.filter(k => k.startsWith('shut:'))).toEqual(['shut:work,tools'])
  })
  it('anche nessuna chiusa si salva', () => {
    const all = DEFAULT_CATS.map(c => ({ ...c, open: true }))
    expect(decode(JSON.parse(encode(all, null)!))).toEqual(all)
  })
  it("l'ordine piatto di ieri cade nelle categorie", () => {
    const d = decode(['sessions', 'closed', 'usage', 'prove', 'plugins', 'pin:usage'])
    // Utilizzo, agganciato allo slot, sale sul tabellone; senza il marcatore le categorie riprendono l'ordine dei riquadri
    // e Tocca a te quello di partenza (app-home-nomi-02-11).
    expect(ids(d)).toEqual(['you', 'work', 'tools'])
    expect(d.find(c => c.id === 'you')!.sections).toEqual(['decisioni', 'prove', 'seguiti', 'suggestions', 'monitors'])
    expect(d.flatMap(c => c.sections)).not.toContain('closed')
    expect(d.flatMap(c => c.sections)).not.toContain('plugins')
    expect(d.flatMap(c => c.sections)).not.toContain('sessions')
    expect(d.map(c => [c.id, c.open])).toEqual([['you', false], ['work', false], ['tools', false]])
  })
  it('la lista salvata sta nei limiti del relay', () => {
    const worst = [...DEFAULT_CATS].reverse().map(c => ({ ...c, open: false, sections: [...c.sections].reverse() }))
    const saved = JSON.parse(encode(worst, 'suggestions')!) as string[]
    expect(saved.length).toBeLessThanOrEqual(16)
    expect(saved.every(k => k.length <= 32)).toBe(true)
  })
  it('una sezione non esce dalla sua categoria', () => {
    expect(moveSection(DEFAULT_CATS, 'you', 1, 9)).toEqual(DEFAULT_CATS)
    expect(moveSection(DEFAULT_CATS, 'you', 2, 0)[0].sections).toEqual(['seguiti', 'decisioni', 'prove', 'suggestions', 'monitors'])
    expect(catOf('rendiconti')).toBe('work')
    expect(catOf('notte')).toBe('work')
    expect(catOf('closed')).toBeNull()
  })
  it("un ordine piatto trascinato tiene categorie e chiuse", () => {
    const r = reorder(toggle(DEFAULT_CATS, 'work'), ['prove', 'lavori', 'decisioni', 'esiti'])
    expect(ids(r)).toEqual(['you', 'work', 'tools'])
    // Le chiavi che la lista trascinata non ha (non disegnate) tengono il loro posto.
    expect(r[0].sections).toEqual(['prove', 'decisioni', 'seguiti', 'suggestions', 'monitors'])
    expect(r.map(c => c.open)).toEqual([false, true, false])
  })
  it('le chiavi non disegnate tengono il posto dopo un trascinamento', () => {
    expect(reorder(DEFAULT_CATS, ['system', 'usage']).find(c => c.id === 'tools')!.sections).toEqual(['system', 'usage', 'pagella'])
  })
  it('un trascinamento non esce dalla sua categoria', () => {
    const order = DEFAULT_CATS.flatMap(c => c.sections) as string[]
    const mids = { decisioni: 100, suggestions: 200, lavori: 300 }
    expect(dragCats(order as never, 'suggestions' as never, 350, mids)).toEqual(order)
    expect(dragCats(order as never, 'suggestions' as never, 90, mids).slice(0, 4)).toEqual(['suggestions', 'decisioni', 'prove', 'seguiti'])
    // Chiuse sta con le sessioni, fuori dalle categorie (app-home-sessioni-a-se-08-11).
    expect(catFor('closed')).toBeNull()
    expect(catFor('sessions')).toBeNull()
    expect(catFor('plugins')).toBe('tools')
  })
  it('una categoria trascinata porta con sé sezioni e interruttore', () => {
    const cats = toggle(moveSection(DEFAULT_CATS, 'you', 2, 0), 'tools')
    const r = catOrder(cats, ['cat:tools', 'cat:you', 'cat:work'])
    expect(ids(r)).toEqual(['tools', 'you', 'work'])
    expect(r[1]).toEqual(cats.find(c => c.id === 'you'))
    expect(r[0].open).toBe(true)
    expect(ids(catOrder(DEFAULT_CATS, ['cat:tools', 'boh']))).toEqual(['tools', 'you', 'work'])
    expect(catKeys(DEFAULT_CATS)).toEqual(['cat:you', 'cat:work', 'cat:tools'])
  })

  // Lo spazio in cima (app-home-in-cima-02-11); gli stessi casi in HomeCatsTest.kt.
  it('le voci in cima sostituiscono la loro chiave ed escono dalla categoria', () => {
    const saved = JSON.parse(encode(toTop(decode(null), [], 'lavori').cats, null, ['lavori'])!) as string[]
    expect(saved[0]).toBe('top:lavori')
    expect(saved).not.toContain('lavori')
    expect(top(saved)).toEqual(['lavori'])
    expect(decode(saved)[1].sections).toEqual(['notte', 'esiti', 'rendiconti'])
    // Le sessioni stanno a sé, sotto il tabellone: non ci salgono più (app-home-sessioni-a-se-08-11).
    expect(top(['top:sessions', 'rev:6'])).toEqual([])
    expect(toTop(DEFAULT_CATS, [], 'sessions' as HomeKey)).toEqual({ top: [], cats: DEFAULT_CATS })
    expect(saved.length <= 16 && saved.every(k => k.length <= 32)).toBe(true)
    const all = DEFAULT_CATS.flatMap(c => c.sections)
    expect((JSON.parse(encode(DEFAULT_CATS.map(c => ({ ...c, sections: [] })), 'usage', all)!) as string[]).length).toBe(15)
    expect(top(['sessions', 'shut:tools'])).toEqual([])
    expect(top(['top:esiti', 'top:boh', 'top:esiti'])).toEqual(['esiti'])
    expect(encode(DEFAULT_CATS, null, [])).toBeNull()
  })
  it('la lista trascinata ha il bordo dopo la cima', () => {
    const f = flat(['esiti'], decode(['top:esiti']))
    expect(f.slice(0, 3)).toEqual(['esiti', EDGE, 'decisioni'])
    expect(f.length).toBe(13)
  })
  it('settle divide al bordo', () => {
    const cats = decode(null)
    const up = ['esiti', EDGE, ...flat([], cats).filter(k => k !== 'esiti' && k !== EDGE)]
    const r1 = settle(cats, up)
    expect(r1.top).toEqual(['esiti'])
    expect(r1.cats[1].sections).toEqual(['notte', 'lavori', 'rendiconti'])
    const down = [EDGE, 'suggestions', 'esiti', ...flat(r1.top, r1.cats).filter(k => !['esiti', EDGE, 'suggestions'].includes(k))]
    const r2 = settle(r1.cats, down)
    expect(r2.top).toEqual([])
    expect(r2.cats[0].sections).toEqual(['suggestions', 'decisioni', 'prove', 'seguiti', 'monitors'])
    expect(r2.cats[1].sections).toContain('esiti')
    expect(r2.cats.map(c => c.open)).toEqual(cats.map(c => c.open))
    const r3 = settle(cats, ['lavori', EDGE, 'esiti'])
    expect(r3.top).toEqual(['lavori'])
    expect(r3.cats[1].sections).toEqual(['notte', 'esiti', 'rendiconti'])
    expect(r3.cats[2]).toEqual(cats[2])
  })
  it('una sezione scavalca solo la cima, il bordo e la sua categoria', () => {
    const order = ['esiti', EDGE, 'decisioni', 'prove', 'notte', 'lavori']
    const mids: Record<string, number> = { esiti: 10, [EDGE]: 30, decisioni: 50, prove: 70, lavori: 110 }
    expect(dragCats(order, 'notte', 60, mids)).toEqual(order)
    expect(dragCats(order, 'notte', 25, mids)).toEqual(['esiti', 'notte', EDGE, 'decisioni', 'prove', 'lavori'])
    const t = ['esiti', 'notte', EDGE, 'decisioni']
    expect(dragCats(t, 'notte', 5, { esiti: 10, [EDGE]: 30 })).toEqual(['notte', 'esiti', EDGE, 'decisioni'])
  })
  it('Riordina mette e toglie dalla cima', () => {
    const a = toTop(DEFAULT_CATS, ['esiti'], 'prove')
    expect(a.top).toEqual(['esiti', 'prove'])
    expect(a.cats[0].sections).toEqual(['decisioni', 'seguiti', 'suggestions', 'monitors'])
    const b = fromTop(a.cats, a.top, 'prove')
    expect(b.top).toEqual(['esiti'])
    // Torna al suo posto, non in fondo (app-cima-rilascio-reset-03-11).
    expect(b.cats[0].sections).toEqual(['decisioni', 'prove', 'seguiti', 'suggestions', 'monitors'])
    expect(toTop(a.cats, a.top, 'prove')).toEqual(a)
    expect(fromTop(b.cats, b.top, 'prove')).toEqual(b)
  })

  // Il tabellone a quattro voci (app-tabellone-quattro-02-11); gli stessi casi in HomeCatsTest.kt.
  it('il tabellone tiene quattro voci e lo slot di prima ci sale sopra', () => {
    let t: HomeKey[] = [], c = DEFAULT_CATS
    for (const k of ['esiti', 'lavori', 'usage', 'prove', 'decisioni'] as HomeKey[]) { const r = toTop(c, t, k); t = r.top; c = r.cats }
    expect(t).toEqual(['esiti', 'lavori', 'usage', 'prove'])
    expect(c[0].sections).toContain('decisioni')
    expect(top(JSON.parse(encode(c, null, t)!))).toEqual(t)
    expect(top(['sessions', 'pin:usage'])).toEqual(['usage'])
    expect(top(['top:esiti', 'pin:usage'])).toEqual(['esiti'])
    expect((JSON.parse(encode(decode(['pin:usage']), null, ['usage'])!) as string[]).some(k => k.startsWith('pin:'))).toBe(false)
    expect(top(['top:esiti', 'top:notte', 'top:usage', 'top:prove', 'top:lavori'])).toHaveLength(4)
  })
  it('una sezione presa nella sua categoria non passa il bordo', () => {
    const order = ['esiti', EDGE, 'notte', 'lavori']
    expect(dragCats(order as never, 'notte', 5, { esiti: 10, [EDGE]: 30, lavori: 110 } as never, false)).toEqual(order)
    expect(dragCats(['esiti', 'notte', EDGE, 'lavori'] as never, 'notte', 5, { esiti: 10, [EDGE]: 30 } as never, true)).toEqual(['notte', 'esiti', EDGE, 'lavori'])
  })
})

describe("app-home-nomi-02-11: l'ordine nuovo una volta", () => {
  it("un ordine salvato prima del marcatore riprende l'ordine dei riquadri e quello di partenza", () => {
    const old = ['top:esiti', 'usage', 'sessions', 'lavori', 'seguiti', 'suggestions', 'prove', 'decisioni', 'prima_raccolta', 'system', 'pagella', 'da_guardare', 'rendiconti', 'shut:tools']
    const d = decode(old)
    expect(d.map(c => c.id)).toEqual(['you', 'work', 'tools'])
    expect(d[0].sections).toEqual(['decisioni', 'prove', 'seguiti', 'suggestions', 'monitors'])
    expect(d[1].sections).toEqual(['notte', 'lavori', 'rendiconti'])
    expect(d[2].sections).toEqual(['usage', 'system', 'pagella'])
    expect(d.map(c => c.open)).toEqual([false, false, false])
    expect(top(old)).toEqual(['esiti'])
    const saved = JSON.parse(encode(moveCat(d, 2, 0), null, ['esiti'])!) as string[]
    expect(saved).toContain(REV)
    expect(saved.length).toBeLessThanOrEqual(16)
    expect(decode(saved).map(c => c.id)).toEqual(['tools', 'you', 'work'])
  })
})

describe('app-home-tre-categorie-06-11: Sessioni porta Notte, In corso ed Esiti', () => {
  it('un ordine rev:4 tiene categorie, chiuse e cima; dentro Sessioni riparte dalla lista, Esiti dopo In corso', () => {
    const old = ['top:sessions', 'esiti', 'rendiconti', 'decisioni', 'prove', 'seguiti', 'suggestions', 'lavori', 'usage', 'system', 'pagella', 'shut:you,tools', 'rev:4']
    const d = decode(old)
    expect(d.map(c => c.id)).toEqual(['you', 'work', 'tools'])
    expect(d.find(c => c.id === 'work')!.sections).toEqual(['notte', 'lavori', 'esiti', 'rendiconti'])
    expect(d.map(c => c.open)).toEqual([false, false, false])
    // Chiuso del tutto (app-home-voci-chiuse-10-11) è il predefinito, che si salva come null; altrimenti col marcatore di oggi.
    const enc = encode(d, null, top(old))
    const saved = enc ? (JSON.parse(enc) as string[]) : []
    if (enc) expect(saved).toContain(REV)
    expect(decode(saved)).toEqual(d)
  })
})

describe('app-sezioni-accorpate-03-11: Tocca a te riprende una volta il suo ordine', () => {
  it("un ordine rev:2 tiene categorie, chiuse e cima; dentro Tocca a te riparte da Decidi", () => {
    const old = ['top:esiti', 'usage', 'system', 'pagella', 'sessions', 'lavori', 'seguiti', 'suggestions', 'prove', 'decisioni', 'prima_raccolta', 'esiti', 'rendiconti', 'shut:out', 'rev:2']
    const d = decode(old)
    // Lavori va subito dopo Dalla master (app-home-sessioni-a-se-08-11).
    expect(d.map(c => c.id)).toEqual(['tools', 'you', 'work'])
    expect(d.find(c => c.id === 'you')!.sections).toEqual(['decisioni', 'prove', 'seguiti', 'suggestions', 'monitors'])
    expect(d.find(c => c.id === 'work')!.sections).toEqual(['notte', 'lavori', 'rendiconti'])
    expect(d.map(c => c.open)).toEqual([false, false, false])
    expect(top(old)).toEqual(['esiti'])
    const saved = JSON.parse(encode(moveSection(d, 'you', 3, 0), null, top(old))!) as string[]
    expect(saved).toContain(REV)
    expect(REV).toBe('rev:7')
    expect(decode(saved).find(c => c.id === 'you')!.sections).toEqual(['suggestions', 'decisioni', 'prove', 'seguiti', 'monitors'])
  })
  // app-tocca-sezioni-06-11: Da guardare non è più una voce (sta in Verifica) e Tocca a te riparte una volta da Decidi.
  it("un ordine rev:3 tiene categorie, chiuse e cima; Da guardare sparisce e Tocca a te riparte da Decidi", () => {
    const old = ['top:sessions', 'usage', 'system', 'pagella', 'lavori', 'suggestions', 'seguiti', 'da_guardare', 'prove', 'decisioni', 'esiti', 'rendiconti', 'shut:out', 'rev:3']
    const d = decode(old)
    expect(d.map(c => c.id)).toEqual(['tools', 'you', 'work'])
    expect(d.find(c => c.id === 'you')!.sections).toEqual(['decisioni', 'prove', 'seguiti', 'suggestions', 'monitors'])
    expect(d.flatMap(c => c.sections)).not.toContain('da_guardare')
    expect(d.map(c => c.open)).toEqual([false, false, false])
    // Le sessioni non stanno più sul tabellone: sono a sé, sotto (app-home-sessioni-a-se-08-11).
    expect(top(old)).toEqual([])
    expect(top(['top:da_guardare', 'rev:3'])).toEqual([])
  })
})

describe('app-home-sessioni-a-se-08-11: le sessioni a sé, Lavori dopo Dalla master', () => {
  // Scelta A di Franz, 08/11 18:35; gli stessi casi in HomeCatsTest.kt.
  it('un ordine rev:5 porta una volta Lavori subito dopo Dalla master, chiusa; il resto resta di Franz', () => {
    const old = ['sessions', 'notte', 'esiti', 'lavori', 'rendiconti', 'decisioni', 'prove', 'seguiti', 'suggestions', 'usage', 'system', 'pagella', 'shut:tools', 'rev:5']
    const d = decode(old)
    expect(ids(d)).toEqual(['you', 'work', 'tools'])
    expect(d.find(c => c.id === 'work')!.sections).toEqual(['notte', 'esiti', 'lavori', 'rendiconti'])
    expect(d.map(c => c.open)).toEqual([false, false, false])
    expect(top(['top:sessions', 'rev:5'])).toEqual([])
    // Il tabellone e le chiuse di Franz restano.
    const withTop = ['top:esiti', 'sessions', 'lavori', 'shut:you', 'rev:5']
    expect(top(withTop)).toEqual(['esiti'])
    expect(decode(withTop).map(c => c.open)).toEqual([false, false, false])
    // Da rev:6 in poi Lavori resta dove Franz lo mette, anche prima di Dalla master.
    expect(ids(decode(JSON.parse(encode(moveCat(d, 1, 0), null)!)))).toEqual(['work', 'you', 'tools'])
    expect(ids(decode(['usage', 'lavori', 'decisioni', 'rev:5']))).toEqual(['tools', 'you', 'work'])
  })
})

describe('app-monitoraggi-09-11: Monitoraggi in fondo a Dalla master', () => {
  const ids = (c: Cat[]) => c.map(x => x.id)
  it('un ordine rev:6 la prende in fondo a Dalla master; il resto resta di Franz', () => {
    const old = ['top:esiti', 'notte', 'lavori', 'rendiconti', 'suggestions', 'decisioni', 'seguiti', 'prove', 'usage', 'system', 'pagella', 'shut:you,tools', 'rev:6']
    const d = decode(old)
    expect(ids(d)).toEqual(['work', 'you', 'tools'])
    expect(d.find(c => c.id === 'you')!.sections).toEqual(['suggestions', 'decisioni', 'seguiti', 'prove', 'monitors'])
    expect(d.map(c => c.open)).toEqual([false, false, false])
    expect(top(old)).toEqual(['esiti'])
    const moved = JSON.parse(encode(moveSection(d, 'you', 4, 0), null, top(old))!) as string[]
    expect(decode(moved).find(c => c.id === 'you')!.sections).toEqual(['monitors', 'suggestions', 'decisioni', 'seguiti', 'prove'])
  })
})

// app-home-voci-chiuse-10-11 (Franz 10/11 21:28: «Di base, le voci della home devono apparire chiuse»): le categorie partono chiuse; un
// ordine salvato fino a rev:6 le chiude una volta (Dalla master aperta non l'aveva scelta Franz); da rev:7 aperte e chiuse si ricordano.
describe('app-home-voci-chiuse-10-11: la home parte chiusa', () => {
  it('le categorie partono chiuse e un ordine di prima si chiude una volta', () => {
    expect(DEFAULT_CATS.every(c => !c.open)).toBe(true)
    expect(decode(['decisioni', 'shut:work,tools', 'rev:6']).map(c => c.open)).toEqual([false, false, false])
  })
  it('da rev:7 quello che Franz apre resta aperto', () => {
    const opened = toggle(DEFAULT_CATS, 'you')
    const saved = JSON.parse(encode(opened, null)!) as string[]
    expect(saved).toContain('rev:7')
    expect(decode(saved).map(c => [c.id, c.open])).toEqual([['you', true], ['work', false], ['tools', false]])
  })
})
