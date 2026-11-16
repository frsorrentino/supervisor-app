import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { anchorOk, listEnd, viewportVars } from './viewport'

// app-ios-scatti-tastiera-12-11: le due regole, coi numeri di un iPhone 15 (844 px di finestra, tastiera da 336).
describe('viewport', () => {
  it('content-visibility solo col ancoraggio dello scorrimento', () => {
    expect(anchorOk(r => r === 'overflow-anchor: auto')).toBe(true)
    expect(anchorOk(() => false)).toBe(false)
    expect(anchorOk(undefined)).toBe(false)
    expect(anchorOk(() => { throw new Error('x') })).toBe(false)
  })
  it('senza tastiera: tutta la finestra, in cima', () => {
    expect(viewportVars({ height: 844, offsetTop: 0, scale: 1 }, 844)).toEqual({ vvh: 844, vvt: 0, kb: false, zoomed: false })
    // La barra degli indirizzi di Safari non è una tastiera.
    expect(viewportVars({ height: 760, offsetTop: 0, scale: 1 }, 844).kb).toBe(false)
  })
  it('con la tastiera: area visibile e scostamento di Safari, niente margine sicuro', () => {
    expect(viewportVars({ height: 508, offsetTop: 0, scale: 1 }, 844)).toEqual({ vvh: 508, vvt: 0, kb: true, zoomed: false })
    // Safari fa scorrere la pagina per mostrare il campo: #app segue l'area visibile, non resta in cima.
    expect(viewportVars({ height: 508, offsetTop: 336, scale: 1 }, 844)).toEqual({ vvh: 508, vvt: 336, kb: true, zoomed: false })
    expect(viewportVars({ height: 508.4, offsetTop: 335.6, scale: 1 }, 844)).toEqual({ vvh: 508, vvt: 336, kb: true, zoomed: false })
  })
  it('zoom delle dita: lo scostamento non sposta #app', () => {
    expect(viewportVars({ height: 422, offsetTop: 200, scale: 2 }, 844)).toEqual({ vvh: 844, vvt: 0, kb: false, zoomed: true })
  })
  it('senza visualViewport: la finestra', () => {
    expect(viewportVars(null, 700)).toEqual({ vvh: 700, vvt: 0, kb: false, zoomed: false })
  })
})

// app-zoom-pan-laterale-16-11 (Franz 16/11 sul Chromebook): dopo lo zoom delle dita l'inquadratura andava solo in verticale. La
// prova nel browser è web/scripts/zoom-pan.mjs (rotella e dito su una griglia di Home, sessione e schermo largo); qui le regole.
describe("zoom delle dita: l'inquadratura va anche di lato", () => {
  const read = (f: string) => readFileSync(new URL(f, import.meta.url), 'utf8')
  it('ingrandita solo con lo zoom delle dita, non per un arrotondamento', () => {
    expect(viewportVars({ height: 527, offsetTop: 0, scale: 1.6 }, 844).zoomed).toBe(true)
    expect(viewportVars({ height: 844, offsetTop: 0, scale: 1 }, 844).zoomed).toBe(false)
    expect(viewportVars({ height: 840, offsetTop: 0, scale: 1.005 }, 844).zoomed).toBe(false)
  })
  it("il fondo di una lista non va oltre quello vero quando l'altezza è frazionaria", () => {
    // La chat della prova: alta 180,438 px, scrollHeight 3175; il compositore la ferma a 2994, non a 2995.
    expect(listEnd(3175, 180.438)).toBe(2994)
    expect(listEnd(3175, 180)).toBe(2995)
    expect(listEnd(120, 180)).toBe(0)
    // La chat porta in fondo solo così, mai con scrollHeight.
    const chat = read('./Chat.svelte')
    expect(chat).not.toMatch(/scrollTop = [a-z]+\.scrollHeight|top: [a-z]+\.scrollHeight/)
    expect(chat.match(/endOf\((el|list)\)/g)?.length).toBe(4)
  })
  it('main.ts mette html.zoomed', () => {
    expect(read('../main.ts')).toMatch(/root\.classList\.toggle\('zoomed', zoomed\)/)
  })
  it('con lo zoom niente trattiene lo scorrimento laterale, la striscia non cambia pagina, il dito sposta dovunque tranne nel visore', () => {
    const css = read('../app.css')
    expect(css).toMatch(/^html\.zoomed body \* \{ overscroll-behavior-x: auto !important; \}$/m)
    expect(css).toMatch(/^html\.zoomed \.strip \{ overflow-x: hidden !important; \}$/m)
    expect(css).toMatch(/^html\.zoomed body \*:not\(\.ivbox\) \{ touch-action: manipulation !important; \}$/m)
    // Il visore delle immagini ha il suo zoom: il suo riquadro tiene touch-action: none.
    expect(read('./ImageViewer.svelte')).toMatch(/\.ivbox \{[^}]*touch-action: none/)
    // html e body restano senza rimbalzo, anche con lo zoom: niente «indietro» dal bordo.
    expect(css).not.toMatch(/html\.zoomed( body)? \{[^}]*overscroll/)
  })
})

// app-ios-chat-scroll-13-11: su iPhone un trascinamento su una chat corta faceva rimbalzare tutta la pagina, e la chat stava in cima
// con lo spazio vuoto sotto (misura in docs/verifiche/2026-11-13-ios-chat-scroll).
describe('iPhone: la pagina non rimbalza e una chat corta sta in fondo', () => {
  const read = (f: string) => readFileSync(new URL(f, import.meta.url), 'utf8')
  it('html e body senza rimbalzo', () => {
    expect(read('../app.css')).toMatch(/^html, body \{ overscroll-behavior: none; \}$/m)
  })
  it('la lista della chat tiene lo scorrimento per sé e mette in fondo una chat corta, senza justify-content', () => {
    const chat = read('./Chat.svelte')
    expect(chat).toMatch(/^  \.lines \{ overscroll-behavior: contain; \}$/m)
    expect(chat).toMatch(/^  \.lines:not\(\.dots\) > :global\(:first-child\) \{ margin-top: auto; \}$/m)
    expect(chat.split('\n').filter(l => l.trimStart().startsWith('.lines')).join(' ')).not.toContain('justify-content')
  })
})
