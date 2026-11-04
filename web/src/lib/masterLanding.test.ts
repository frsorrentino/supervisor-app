import { describe, expect, it } from 'vitest'
import { clampWidth, COLUMN, DWELL_MS, heightAt, heightZones, MIN_W, sideAt, sideZones, snapWidth, steady, type Height } from './masterLanding'

// La master si sposta con la testata (solo il lato) e si misura dai bordi (app-master-trascina-03-11, Franz 03/11 22:08).
describe('masterLanding', () => {
  it('la testata lasciata nella metà sinistra o destra sceglie il lato', () => {
    expect(sideAt(1920, 100)).toBe('left')
    expect(sideAt(1920, 959)).toBe('left')
    expect(sideAt(1920, 960)).toBe('right')
    expect(sideZones(1920, 1000).map(z => z.key)).toEqual(['left', 'right'])
  })
  it('la larghezza si ferma sugli scatti vicini e resta libera fra i due', () => {
    expect(snapWidth(COLUMN + 20, 1920)).toBe(COLUMN)
    expect(snapWidth(940, 1920)).toBe(960)
    expect(snapWidth(650, 1920)).toBe(650)
  })
  it('la larghezza non scende sotto il minimo e non passa il 60 % dello schermo', () => {
    expect(clampWidth(100, 1920)).toBe(MIN_W)
    expect(clampWidth(1800, 1920)).toBe(1152)
  })
  it('il bordo superiore: colonna intera, a metà, compatta, in fondo la riga', () => {
    const zs = heightZones({ x: 0, y: 0, w: 420, h: 1000 }, true)
    expect([50, 400, 700, 950, 1200].map(y => heightAt(zs, y))).toEqual(['column', 'half', 'compact', 'bar', 'bar'])
  })
  it('senza sessioni aperte la master non sta sotto la home: colonna o riga', () => {
    const zs = heightZones({ x: 0, y: 100, w: 420, h: 900 }, false)
    expect(zs.map(z => z.key)).toEqual(['column', 'bar'])
    expect(heightAt(zs, 500)).toBe('column')
    expect(heightAt(zs, 990)).toBe('bar')
  })
})

describe('la sosta prima di cambiare bersaglio (app-trascina-rifinitura-04-11)', () => {
  it('un bersaglio nuovo vince solo dopo DWELL_MS di fila sotto il dito', () => {
    const s = steady<Height>('bar')
    expect(s.feed('compact', 0)).toEqual({ on: 'bar', wait: DWELL_MS })
    expect(s.feed('compact', 100)).toEqual({ on: 'bar', wait: DWELL_MS - 100 })
    expect(s.feed('compact', DWELL_MS)).toEqual({ on: 'compact', wait: null })
    expect(s.current).toBe('compact')
  })
  it('uno sfarfallio fra due bande non cambia niente', () => {
    const s = steady<Height>('bar')
    for (let t = 0; t < 1000; t += 50) s.feed(t % 100 === 0 ? 'compact' : 'bar', t)
    expect(s.current).toBe('bar')
  })
  it('un candidato diverso ricomincia la sosta', () => {
    const s = steady<Height>('bar')
    s.feed('compact', 0)
    expect(s.feed('half', 100).wait).toBe(DWELL_MS)
    expect(s.feed('half', 100 + DWELL_MS).on).toBe('half')
  })
})
