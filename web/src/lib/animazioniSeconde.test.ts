import { describe, expect, it } from 'vitest'
import { DOCK_PEEK_MS, STAGGER_MS, UNDO_MS, dots, fill, ghost, limit, newPill, stackEdges } from './animazioni'

// Il secondo giro di animazioni (app-animazioni-seconde-26-11): gli stessi casi di AnimazioniSecondeTest in Kotlin.
describe('animazioni seconde', () => {
  it('tempi comuni', () => { expect([UNDO_MS, DOCK_PEEK_MS, STAGGER_MS]).toEqual([5000, 3000, 40]) })
  it('anello del limite', () => {
    expect([limit(100, 300), limit(280, 300), limit(300, 300), limit(301, 300)]).toEqual(['ok', 'near', 'near', 'over'])
    expect(fill(150, 300)).toBeCloseTo(0.5)
    expect(fill(400, 300)).toBe(1)
  })
  it("puntini attorno all'immagine di adesso", () => {
    expect(dots(5, 2)).toEqual([0, 5])
    expect(dots(12, 1)).toEqual([0, 7])
    expect(dots(12, 6)).toEqual([3, 10])
    expect(dots(12, 11)).toEqual([5, 12])
    expect(dots(0, 0)).toEqual([0, 0])
  })
  it('bordi della pila', () => { expect([stackEdges(1), stackEdges(2), stackEdges(5), stackEdges(0)]).toEqual([0, 1, 2, 0]) })
  it('suggerimento solo a campo vuoto', () => {
    expect(ghost([' build della seconda proposta ', 'porta le 8'], '')).toBe('build della seconda proposta')
    expect(ghost(['build'], 'b')).toBeNull()
    expect(ghost([], '')).toBeNull()
    expect(ghost([' ', 'porta le 8'], '')).toBe('porta le 8')
  })
  it('pillola dei nuovi', () => {
    expect(newPill(false, 1)).toBe(true)
    expect(newPill(true, 3)).toBe(false)
    expect(newPill(false, 0)).toBe(false)
  })
})
