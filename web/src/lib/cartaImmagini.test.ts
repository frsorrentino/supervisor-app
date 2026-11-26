import { describe, expect, it } from 'vitest'
import type { WaitFile, WaitItem, WaitOption } from './contract'
import { thumbStrip } from './scelta'
import { others } from './guardare'
import type { ResultItem, TranscriptFile } from './contract'

// La carta con molte immagini (app-carta-immagini-26-11, Franz 26/11 11:43: «non vedo tutti i mockup»): oltre quattro, tre miniature
// in ordine e una casella «+N». Gli stessi casi di CartaImmaginiTest in Kotlin.
const nomi = ['01-copia', '02-lettura', '03-al-lavoro', '04-cifra', '05-passi', '06-tieni-premuto', '07-anello', '08-orologio']
const png = (n: string): WaitFile => ({ path: `/p/${n}.png`, name: `${n}.png`, mime: 'image/png', size: 30000, kind: 'image', session: null })
const ok = (files: WaitFile[]): WaitItem => ({ id: 'w61eeb4cc67', type: 'carta', kind: 'ok', label: 'ok', label_text: 'Ok', text: 'Animazioni', at: 0, files })
describe('carta con molte immagini', () => {
  it('otto immagini: tre miniature e +5', () => {
    const s = thumbStrip(ok(nomi.map(png)))
    expect(s.thumbs.map(x => x.file.name)).toEqual(['01-copia.png', '02-lettura.png', '03-al-lavoro.png'])
    expect(s.more).toBe(5)
  })
  it('fino a quattro tutte, senza casella', () => {
    const s = thumbStrip(ok(nomi.slice(0, 4).map(png)))
    expect(s.thumbs.map(x => x.file.name)).toEqual(nomi.slice(0, 4).map(n => `${n}.png`))
    expect(s.more).toBe(0)
    expect(thumbStrip(ok(nomi.slice(0, 5).map(png))).more).toBe(2)
  })
  it('i documenti non contano', () => {
    const pdf: WaitFile = { path: '/p/piano.pdf', name: 'piano.pdf', mime: 'application/pdf', size: 1000, kind: 'document', session: null }
    const s = thumbStrip(ok([...nomi.slice(0, 4).map(png), pdf]))
    expect(s.thumbs.length).toBe(4); expect(s.more).toBe(0)
  })
  it('una scelta conta le opzioni con immagini', () => {
    const options: WaitOption[] = [1, 2, 3, 4, 5, 6].map(n => ({ n, label: `Opzione ${n}`, value: `${n}`, files: [png(nomi[n - 1])] }))
    const s = thumbStrip({ ...ok([]), kind: 'choice', options })
    expect(s.thumbs.map(x => x.letter)).toEqual(['A', 'B', 'C'])
    expect(s.more).toBe(3)
  })
})

// Un SendUserFile con l'HTML e gli otto PNG (11:30): il visore mostrava solo 01-copia.png. Ora gli altri file stanno sotto, tutti.
const html: TranscriptFile = { path: '/p/animazioni-proposte.html', mime: 'text/html', size: 9000 }
const pngs = (): TranscriptFile[] => nomi.map(n => ({ path: `/p/${n}.png`, mime: 'image/png', size: 30000 }))
const res = (o: Partial<ResultItem>): ResultItem => ({ id: 'r', title: '', kind: 'image', path: '', at: 0, version: 1, versions: 1, ...o })
describe('invio con più file', () => {
  it("mockup: gli altri sette PNG, l'HTML resta la pagina da aprire", () => {
    expect(others(res({ kind: 'mockup', mime: 'text/html', path: html.path, files: [html, ...pngs()] })).map(f => f.path)).toEqual(nomi.slice(1).map(n => `/p/${n}.png`))
  })
  it("immagine principale: l'HTML e le altre immagini", () => {
    expect(others(res({ mime: 'image/png', path: '/p/01-copia.png', files: [html, ...pngs()] })).map(f => f.path)).toEqual([html.path, ...nomi.slice(1).map(n => `/p/${n}.png`)])
  })
  it('un file solo: niente altri', () => {
    expect(others(res({ mime: 'image/png', path: '/p/01-copia.png', files: [{ path: '/p/01-copia.png', mime: 'image/png', size: 30000 }] }))).toEqual([])
  })
})
