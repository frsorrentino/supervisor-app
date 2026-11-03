import { describe, expect, it } from 'vitest'
import { BAR, COLUMN, current, landingAt, zones } from './masterLanding'

// I punti di atterraggio della master trascinata nella PWA larga (app-master-trascina-03-11).
describe('masterLanding', () => {
  const W = 1920, H = 1000
  const at = (x: number, y: number, under = true) => { const z = landingAt(zones(W, H, under), x, y); return z ? `${z.side}:${z.place}` : null }

  it('la colonna esterna di ogni lato: colonna intera, poi sotto la home a metà e compatta', () => {
    expect(at(W - 10, 100)).toBe('right:column')
    expect(at(W - 10, (H - BAR) * 0.6)).toBe('right:under')
    expect(at(W - 10, H - BAR - 50)).toBe('right:compact')
    expect(at(10, 100)).toBe('left:column')
    expect(at(10, (H - BAR) * 0.6)).toBe('left:under')
    expect(at(10, H - BAR - 50)).toBe('left:compact')
  })
  it('senza sessioni aperte la colonna esterna è tutta «colonna intera»', () => {
    expect(at(W - 10, H - BAR - 50, false)).toBe('right:column')
    expect(zones(W, H, false).some(z => z.place === 'under' || z.place === 'compact')).toBe(false)
  })
  it('verso il centro, fino a metà schermo: la master larga a metà', () => {
    expect(at(W - COLUMN - 10, 300)).toBe('right:half')
    expect(at(W / 2 + 5, 300)).toBe('right:half')
    expect(at(COLUMN + 10, 300)).toBe('left:half')
    expect(at(W / 2 - 5, 300)).toBe('left:half')
  })
  it('in fondo, a tutta larghezza, la riga', () => {
    expect(at(W / 2, H - 20)).toBe('right:bar')
    expect(at(5, H - 1)).toBe('right:bar')
  })
  it('le zone non si sovrappongono e coprono la pagina', () => {
    const zs = zones(W, H, true)
    for (let x = 0; x < W; x += 37) for (let y = 0; y < H; y += 41) expect(zs.filter(z => x >= z.rect.x && x < z.rect.x + z.rect.w && y >= z.rect.y && y < z.rect.y + z.rect.h).length).toBe(1)
  })
  it('la posizione di adesso', () => {
    expect(current({ open: false, left: true, under: false, half: true })).toEqual({ side: 'right', place: 'bar' })
    expect(current({ open: true, left: true, under: true, half: true })).toEqual({ side: 'left', place: 'under' })
    expect(current({ open: true, left: true, under: true, half: true, compact: true })).toEqual({ side: 'left', place: 'compact' })
    expect(current({ open: true, left: false, under: false, half: true })).toEqual({ side: 'right', place: 'column' })
    expect(current({ open: true, left: false, under: false, half: false })).toEqual({ side: 'right', place: 'half' })
  })
})
