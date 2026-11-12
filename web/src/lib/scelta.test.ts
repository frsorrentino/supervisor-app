import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeWait, decodeWaits } from './decideDecode'
import type { WaitFile, WaitItem, WaitOption } from './contract'
import { stripWaitLinks } from './sessionLinks'
import { choiceOf, frameRatio, isChoice, lettersOf, outcomeOf, redoReady, roundsOf, sizeText, splitFiles, swipeTo, thumbsOf, waitFileKey, REDO_MAX } from './scelta'

// La scheda di scelta (app-scheda-scelta-12-11, contratto 1.137): gli stessi casi di SceltaTest in Kotlin, dallo stesso file, e la
// fixture del contratto (…1300 attese, …1301 attesa con un giro scartato, …1390 la scheda dall'archivio).
const cases = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/scelta-cases.json', import.meta.url), 'utf8')) as {
  letters: { name: string; labels: string[]; out: [string, string][] }[]
  outcome: { name: string; state: string | null; answered: { choice: string | null; reason: string | null } | null; options: [string, string][]; out: { state: string; letter: string | null; name: string | null; reason: string | null } }[]
  files: { name: string; files: [string, string, string][]; images: string[]; docs: string[] }[]
  key: { name: string; path: string; size: number | null; session: string | null; out: string }[]
  strip: { name: string; text: string; out: string; ids: string[] }[]
}
const sample = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')) as { cmd: { id: string; op: string; arg: string | null; archive?: boolean }[]; result: { id: string; ok: boolean; text: string }[] }
const result = (pick: (c: { op: string; arg: string | null; archive?: boolean }) => boolean) => sample.result.find(r => r.id === sample.cmd.find(pick)!.id)!
const open = () => decodeWait(result(c => c.op === 'attesa' && c.arg === 'wd2f87e85e2' && !c.archive).text)
const archived = () => decodeWait(result(c => c.op === 'attesa' && !!c.archive).text)
const opts = (o: [string, string][]): WaitOption[] => o.map(([label, value], i) => ({ n: i + 1, label, value }))
const item = (o: Partial<WaitItem>): WaitItem => ({ id: 'w1', type: 'carta', kind: 'choice', label: 'choose', label_text: 'Scegli', text: 'x', at: 0, ...o })

describe('scelta: lettere e nomi', () => {
  for (const c of cases.letters) it(c.name, () => expect(lettersOf(c.labels).map(x => [x.letter, x.name])).toEqual(c.out))
})

describe('scelta: com\'è finita', () => {
  for (const c of cases.outcome) it(c.name, () => {
    const o = outcomeOf(item({ state: c.state, answered: c.answered, options: opts(c.options) }))
    expect({ state: o.state, letter: o.letter, name: o.name, reason: o.reason }).toEqual(c.out)
  })
  it('the archived card of the fixture: chosen A, by the phone', () => {
    const o = outcomeOf(archived())
    expect(o).toMatchObject({ state: 'chosen', letter: 'A', name: 'Release now', by: 'phone-pixel8', at: 1789216500 })
  })
})

describe('scelta: i file', () => {
  for (const c of cases.files) it(c.name, () => {
    const s = splitFiles(c.files.map(([name, mime, kind]): WaitFile => ({ path: `/x/${name}`, name, mime, kind })))
    expect(s.images.map(f => f.name)).toEqual(c.images); expect(s.docs.map(f => f.name)).toEqual(c.docs)
  })
  for (const c of cases.key) it(c.name, () => expect(waitFileKey({ path: c.path, name: 'x', size: c.size, kind: 'image', session: c.session })).toBe(c.out))
})

describe('scelta: la scheda della fixture', () => {
  it('a choice with images is a choice card; a question in chat with bare options is not', () => {
    const all = decodeWaits(result(c => c.op === 'attese').text).items
    expect(all.map(isChoice)).toEqual([true, false, false])
  })
  it('each option has its letter, its line, its files and the action that picks it', () => {
    const c = choiceOf(open())
    expect(c.options.map(o => [o.letter, o.name, o.line, o.recommended, o.images.map(f => f.name), o.docs.map(f => f.name), o.action])).toEqual([
      ['A', 'Release now', 'tested everywhere but Safari', true, ['home-a.png'], [], 0],
      ['B', 'Tomorrow morning', 'one more day, Safari tested', false, ['home-b.png'], ['home-b.pdf'], 1],
    ])
  })
  it('«Nessuna, rifare» is the redo action, the rest stays below; the files of the options are not repeated', () => {
    const w = open(), c = choiceOf(w)
    expect(c.redo).toBe(2); expect(w.actions![c.redo!].cmd.op).toBe('approval_redo')
    expect(c.rest).toEqual([3]); expect(c.extra).toEqual([])
  })
  it('a file of the card that no option has stays apart', () => {
    const w = open()
    const more: WaitFile = { path: '/home/user/work/atlas-shop/docs/notes.md', name: 'notes.md', mime: 'text/markdown', size: 10, kind: 'document', session: null }
    expect(choiceOf({ ...w, files: [...w.files!, more] }).extra).toEqual([more])
  })
  it('the action of an option: by its value, then by its label, then by its place', () => {
    const w = open()
    const noChoice = { ...w, actions: w.actions!.map(a => ({ ...a, cmd: { ...a.cmd, choice: undefined } })) }
    expect(choiceOf(noChoice).options.map(o => o.action)).toEqual([0, 1])
    const noLabel = { ...noChoice, actions: noChoice.actions.map((a, i) => (i < 2 ? { ...a, label: `x${i}` } : a)) }
    expect(choiceOf(noLabel).options.map(o => o.action)).toEqual([0, 1])
  })
  it('from the archive there are no actions: no buttons', () => {
    const c = choiceOf(archived())
    expect(c.options.map(o => o.action)).toEqual([null, null]); expect(c.redo).toBeNull(); expect(c.rest).toEqual([])
  })
  it('the earlier rounds, the latest first, each with its number and why it was dropped', () => {
    const r = roundsOf(open())
    expect(r.map(x => [x.n, x.state, x.reason, x.options.map(o => o.letter), x.options[0].images.map(f => f.name)])).toEqual([
      [1, 'redo', 'neither: the old one looks broken on mobile (from the phone)', ['A', 'B'], ['round1-a.png']],
    ])
    expect(roundsOf(item({ rounds: [] }))).toEqual([]); expect(roundsOf(item({}))).toEqual([])
  })
  it('the reason of «Nessuna, rifare»: required, at most 300 characters', () => {
    expect(REDO_MAX).toBe(300)
    expect(redoReady('')).toBe(false); expect(redoReady('   \n')).toBe(false); expect(redoReady('troppo grande')).toBe(true)
    expect(redoReady('x'.repeat(300))).toBe(true); expect(redoReady('x'.repeat(301))).toBe(false)
  })
})

describe('scelta: la scheda compatta e il visore', () => {
  it('thumbnails: the first image of each option with its letter', () => {
    expect(thumbsOf(open()).map(x => [x.letter, x.name, x.file.name])).toEqual([['A', 'Release now', 'home-a.png'], ['B', 'Tomorrow morning', 'home-b.png']])
  })
  it('thumbnails of a card with no options: its own images, no letters', () => {
    const files: WaitFile[] = [{ path: '/x/a.png', name: 'a.png', mime: 'image/png', kind: 'image' }, { path: '/x/b.pdf', name: 'b.pdf', mime: 'application/pdf', kind: 'document' }]
    expect(thumbsOf(item({ kind: 'prova', options: null, files })).map(x => [x.letter, x.file.name])).toEqual([[null, 'a.png']])
    expect(thumbsOf(item({ kind: 'prova', options: null, files: null }))).toEqual([])
    // La verifica della fixture (0.8.155): le sue immagini, senza lettere; il pdf resta fuori dalle miniature.
    const prova = decodeWaits(result(c => c.op === 'attese').text).items.find(w => w.kind === 'prova')!
    expect(thumbsOf(prova).map(x => [x.letter, x.name, x.file.name])).toEqual([[null, '', 'checkout-before.png']])
  })
  it('the frame takes the shape of the first image that has arrived, between 9:16 and 16:9', () => {
    expect(frameRatio([undefined, undefined])).toBeCloseTo(4 / 3)
    expect(frameRatio([undefined, 1.6])).toBeCloseTo(1.6)
    expect(frameRatio([412 / 915, 1.6])).toBeCloseTo(9 / 16)
    expect(frameRatio([3])).toBeCloseTo(16 / 9)
    expect(frameRatio([NaN, 0, 1])).toBe(1)
  })
  it('a swipe in the viewer: far enough or quick, and sideways', () => {
    expect(swipeTo(-80, 5, 300)).toBe(1); expect(swipeTo(80, 0, 300)).toBe(-1)
    expect(swipeTo(-30, 0, 400)).toBe(0); expect(swipeTo(-30, 0, 50)).toBe(1)
    expect(swipeTo(-60, -90, 200)).toBe(0); expect(swipeTo(0, 0, 0)).toBe(0)
  })
  it('sizes in words', () => { expect(sizeText(48213)).toBe('48 KB'); expect(sizeText(1_200_000)).toBe('1,2 MB'); expect(sizeText(900)).toBe('900 B') })
})

describe('scelta: i link delle richieste escono dal testo della chat', () => {
  for (const c of cases.strip) it(c.name, () => expect(stripWaitLinks(c.text)).toEqual({ text: c.out, ids: c.ids }))
})
