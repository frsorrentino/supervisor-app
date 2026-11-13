import { describe, expect, it } from 'vitest'
import { anchorOk, viewportVars } from './viewport'

// app-ios-scatti-tastiera-12-11: le due regole, coi numeri di un iPhone 15 (844 px di finestra, tastiera da 336).
describe('viewport', () => {
  it('content-visibility solo col ancoraggio dello scorrimento', () => {
    expect(anchorOk(r => r === 'overflow-anchor: auto')).toBe(true)
    expect(anchorOk(() => false)).toBe(false)
    expect(anchorOk(undefined)).toBe(false)
    expect(anchorOk(() => { throw new Error('x') })).toBe(false)
  })
  it('senza tastiera: tutta la finestra, in cima', () => {
    expect(viewportVars({ height: 844, offsetTop: 0, scale: 1 }, 844)).toEqual({ vvh: 844, vvt: 0, kb: false })
    // La barra degli indirizzi di Safari non è una tastiera.
    expect(viewportVars({ height: 760, offsetTop: 0, scale: 1 }, 844).kb).toBe(false)
  })
  it('con la tastiera: area visibile e scostamento di Safari, niente margine sicuro', () => {
    expect(viewportVars({ height: 508, offsetTop: 0, scale: 1 }, 844)).toEqual({ vvh: 508, vvt: 0, kb: true })
    // Safari fa scorrere la pagina per mostrare il campo: #app segue l'area visibile, non resta in cima.
    expect(viewportVars({ height: 508, offsetTop: 336, scale: 1 }, 844)).toEqual({ vvh: 508, vvt: 336, kb: true })
    expect(viewportVars({ height: 508.4, offsetTop: 335.6, scale: 1 }, 844)).toEqual({ vvh: 508, vvt: 336, kb: true })
  })
  it('zoom delle dita: lo scostamento non sposta #app', () => {
    expect(viewportVars({ height: 422, offsetTop: 200, scale: 2 }, 844)).toEqual({ vvh: 844, vvt: 0, kb: false })
  })
  it('senza visualViewport: la finestra', () => {
    expect(viewportVars(null, 700)).toEqual({ vvh: 700, vvt: 0, kb: false })
  })
})
