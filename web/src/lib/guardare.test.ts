import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import type { ResultItem, ResultsPage, State } from './contract'
import { line, note, ofSession, older, pos, thumb } from './guardare'

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
  it('posizioni e note', () => {
    expect([pos(36.7), pos(6), pos(65.2)]).toEqual(['36,7 s', '6 s', '1:05'])
    expect(note('  ')).toBeNull(); expect(note('x'.repeat(2500))!.length).toBe(2000)
  })
})
