import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import type { ResultGroup, ResultItem, ResultsPage, State } from './contract'
import { lastKey, line, note, ofSession, older, pos, seenByPath, shown, splitSeen, thumb } from './guardare'

// Gemello di GuardareTest.kt (contratto 1.106, app-da-guardare-02-11), con le fixture vere.
const fixture = (n: string) => JSON.parse(readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8'))
const state = fixture('state-3-stale.json') as State
const page = JSON.parse(fixture('cmd-result-sample.json').result.find((r: { id: string }) => r.id.includes('0919')).text) as ResultsPage
const item = (x: Partial<ResultItem>): ResultItem => ({ id: 'x', title: '', kind: 'other', path: '', at: 0, version: 1, versions: 1, ...x })

describe('guardare', () => {
  it('lo stato porta i risultati non visti', () => {
    expect(state.results!.n_unseen).toBe(1)
    expect(line(state.results!.items[0])).toEqual({ title: 'Docs tour', version: 3, session: 'orbit-docs', kind: 'video', seconds: 37, at: 1789199800, unseen: true })
  })
  it('la pagina raggruppa le versioni con le note', () => {
    const g = page.groups[0]
    expect(g.items.map(i => i.version)).toEqual([3, 2])
    const v2 = older(g)[0]
    expect(v2.notes).toEqual([{ at: 1789197100, text: 'The logo flickers here', pos_s: 12.4 }])
    expect(line(v2).unseen).toBe(false)
  })
  it('una versione sola non ha numero, un titolo vuoto prende il nome del file', () => {
    expect(line(item({ kind: 'mockup' })).version).toBeNull()
    expect(line(item({ title: ' ', path: '/a/b/tour.mp4' })).title).toBe('tour.mp4')
  })
  it('miniature solo per le immagini', () => {
    const png = { path: '/m/a.png', mime: 'image/png', size: 10 }
    expect(thumb(item({ kind: 'mockup', path: '/m/a.html', mime: 'text/html', files: [png] }))).toEqual(png)
    expect(thumb(item({ kind: 'image', path: '/i/b.jpg', mime: 'image/jpeg' }))?.path).toBe('/i/b.jpg')
    expect(thumb(item({ kind: 'video', path: '/v.mp4', mime: 'video/mp4' }))).toBeNull()
    expect(thumb(item({ kind: 'mockup', path: '/m/a.html', mime: 'text/html' }))).toBeNull()
  })
  it('la riga della sessione elenca i suoi lavori', () => {
    expect(ofSession(page, 'orbit-docs').latest?.version).toBe(3)
    expect(ofSession(page, 'atlas-shop').groups).toEqual([])
    expect(ofSession(null, 'x').latest).toBeNull()
  })
  it('la X chiude la riga fino al risultato successivo (app-risultati-x-05-11)', () => {
    const l = ofSession(page, 'orbit-docs')
    const key = lastKey(l)!
    expect(shown(l, null)).toBe(true); expect(shown(l, key)).toBe(false)
    const g0 = l.groups[0]
    const next = { groups: [...l.groups, { ...g0, at: g0.at + 60, items: g0.items.map(i => ({ ...i, id: i.id + '-n', at: i.at + 60 })) }] }
    expect(shown(next, key)).toBe(true)
    expect(shown(ofSession(page, 'atlas-shop'), null)).toBe(false)
  })
  it('app-risultati-visti-03-11: in cima alla chat solo un lavoro non visto, altrimenti nessuna anteprima', () => {
    const seen = { ...page!, groups: page!.groups.map(g => ({ ...g, unseen: false })) }
    expect(ofSession(seen, 'orbit-docs').latest).toBeNull()
    expect(ofSession(seen, 'orbit-docs').groups.length).toBeGreaterThan(0)
    const g0 = page!.groups.find(g => g.session === 'orbit-docs')!
    const mixed = { ...page!, groups: [{ ...g0, unseen: false, at: g0.at + 100, title: 'vecchio visto' }, { ...g0, unseen: true }] }
    expect(ofSession(mixed, 'orbit-docs').latest).toEqual(g0.items[0])
  })
  it('posizioni e note', () => {
    expect([pos(36.7), pos(6), pos(65.2)]).toEqual(['36,7 s', '6 s', '1:05'])
    expect(note('  ')).toBeNull(); expect(note('x'.repeat(2500))!.length).toBe(2000)
  })
  // app-risultati-visti-03-11 (Franz 03/11 15:26): in Risultati solo quello che non ha visto; Visto lo toglie; aperto altrove conta come visto.
  it('risultati visti: in elenco solo i non visti', () => {
    const grp = (id: string, seen: number | null, old?: string) => ({ title: id, items: [item({ id, seen_at: seen }), ...(old ? [item({ id: old, seen_at: 5 })] : [])] }) as ResultGroup
    const s = splitSeen([grp('a', null), grp('b', 9), grp('c', null, 'c0')], new Set(['c']))
    expect(s.fresh.map(g => g.title)).toEqual(['a'])
    expect(s.seen.map(g => g.title)).toEqual(['b', 'c'])
  })
  it('risultati visti: un file aperto altrove lo segna', () => {
    const png = { path: '/m/a.png', mime: 'image/png', size: 10 }
    const items = [item({ id: 'm', path: '/m/a.html', files: [png] }), item({ id: 'i', path: '/i/b.jpg' }), item({ id: 'v', path: '/m/a.png', seen_at: 3 })]
    expect(seenByPath(items, '/m/a.png')).toEqual(['m'])
    expect(seenByPath(items, '/i/b.jpg')).toEqual(['i'])
    expect(seenByPath(items, '/x')).toEqual([])
  })
})
