import { describe, expect, it } from 'vitest'
import { canCopy, copyKind, inlineSafe, PAGE_SANDBOX, pageDoc } from './fileActions'

describe('azioni sul file', () => {
  it('copia il testo dei file di testo', () => {
    for (const m of ['text/plain', 'text/markdown', 'application/json', 'application/xml', 'image/svg+xml', 'application/javascript', 'text/csv; charset=utf-8']) expect(copyKind(m)).toBe('text')
  })
  it("copia l'immagine delle immagini raster", () => {
    for (const m of ['image/png', 'image/jpeg', 'image/webp']) expect(copyKind(m)).toBe('image')
  })
  it('sugli altri «Copia» non c\'è (Franz, 15/10 22:19)', () => {
    for (const m of ['application/pdf', 'application/zip', 'video/mp4', 'application/octet-stream']) { expect(copyKind(m)).toBeNull(); expect(canCopy(m)).toBe(false) }
    expect(canCopy('text/plain')).toBe(true); expect(canCopy('image/png')).toBe(true)
  })
  // Franz, 16/10 11:18: le anteprime HTML si vedono come pagina. Mai nella pagina dell'app (leggerebbero il token): in un
  // riquadro sandbox con l'origine opaca. «Apri» continua a scaricarli.
  it('HTML, SVG e XML non si aprono nella pagina: HTML e SVG si vedono in un riquadro sandbox', () => {
    for (const m of ['text/html', 'image/svg+xml', 'application/xml', 'application/xhtml+xml']) expect(inlineSafe(m)).toBe(false)
    for (const m of ['image/png', 'application/pdf', 'text/plain']) expect(inlineSafe(m)).toBe(true)
    expect(pageDoc('text/html', '<h1>Hi</h1>')).toBe('<h1>Hi</h1>')
    expect(pageDoc('image/svg+xml', '<svg xmlns="http://www.w3.org/2000/svg"></svg>')).toBe('<!doctype html><body style="margin:0"><svg xmlns="http://www.w3.org/2000/svg"></svg>')
  })
  it('il riquadro sandbox: script sì, stessa origine, navigazione della pagina e popup no', () => {
    const flags = PAGE_SANDBOX.split(/\s+/)
    expect(flags).toEqual(['allow-scripts'])
    for (const f of ['allow-same-origin', 'allow-top-navigation', 'allow-popups', 'allow-forms', 'allow-modals']) expect(flags).not.toContain(f)
  })
})
