import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { capable, draftIn, draftOf, moves, plays, previewOf } from './anteprimeAnimate'

// app-anteprime-webp-27-11: le anteprime che si muovono e la bozza HTML della loro immagine. Gli stessi casi di AnteprimeAnimateTest
// in Kotlin, dallo stesso file.
type Case = { name: string; out: unknown }
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/anteprime-animate-cases.json', import.meta.url), 'utf8')) as {
  capable: (Case & { mime: string | null; path: string })[]
  moves: (Case & { b64: string })[]
  plays: (Case & { visible: boolean; reduced: boolean })[]
  draft: (Case & { image: string; mime: string | null; files: string[] })[]
  preview: (Case & { draft: string; files: { path: string; mime: string | null }[] })[]
}
const bytes = (b64: string) => new Uint8Array(Buffer.from(b64, 'base64'))
const demo = (name: string) => new Uint8Array(readFileSync(new URL(`../../public/demo-files/${name}`, import.meta.url)))

describe('anteprime animate: si può muovere', () => {
  for (const c of file.capable) it(c.name, () => expect(capable(c.mime, c.path)).toBe(c.out))
})

describe('anteprime animate: si muove davvero', () => {
  for (const c of file.moves) it(c.name, () => expect(moves(bytes(c.b64))).toBe(c.out))
  it('il WebP animato della demo si muove, le schermate PNG no', () => {
    expect(moves(demo('entrata-home.webp'))).toBe(true)
    expect(moves(demo('cart-a.png'))).toBe(false)
  })
})

describe('anteprime animate: quando gira', () => {
  for (const c of file.plays) it(c.name, () => expect(plays(c.visible, c.reduced)).toBe(c.out))
})

describe('anteprime animate: la bozza della sua immagine', () => {
  for (const c of file.draft) it(c.name, () => expect(draftOf(c.image, c.mime, c.files)).toBe(c.out))
  for (const c of file.preview) it(c.name, () => expect(previewOf(c.draft, c.files)).toBe(c.out))
  it('fra i file di una carta la bozza torna come file, col suo tipo', () => {
    const webp = { path: '/w/docs/entrata-home.webp', name: 'entrata-home.webp', mime: 'image/webp' }
    const html = { path: '/w/docs/entrata-home.html', name: 'entrata-home.html', mime: 'text/html' }
    const png = { path: '/w/docs/scheda-a.png', name: 'scheda-a.png', mime: 'image/png' }
    expect(draftIn(webp, [png, webp, html])).toBe(html)
    expect(draftIn(png, [png, webp, html])).toBeNull()
    expect(draftIn(webp, [webp])).toBeNull()
  })
})
