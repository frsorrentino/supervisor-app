import { describe, expect, it } from 'vitest'
import { COLUMN, DWELL_MS, heightAt, heightZones, sideAt, sideZones, steady, widthAt, widthStops, WIDTH_MARGIN, type Height } from './masterLanding'

// La master si sposta con la testata (solo il lato) e si misura dai bordi (app-master-trascina-03-11, Franz 03/11 22:08).
describe('masterLanding', () => {
  it('la testata lasciata nella metà sinistra o destra sceglie il lato', () => {
    expect(sideAt(1920, 100)).toBe('left')
    expect(sideAt(1920, 959)).toBe('left')
    expect(sideAt(1920, 960)).toBe('right')
    expect(sideZones(1920, 1000).map(z => z.key)).toEqual(['left', 'right'])
  })
  // Franz 04/11 10:29-10:30 (app-trascina-rifinitura-04-11): posti fissi, la calamita aggancia il più vicino.
  it('la larghezza ha due posti fissi: colonna e metà schermo', () => {
    expect(widthStops(1920)).toEqual([COLUMN, 960])
    expect(widthStops(800)).toEqual([COLUMN])
  })
  it('la larghezza va sempre al posto fisso più vicino, mai nel mezzo', () => {
    expect([100, 440, 650, 689, 691, 940, 1800].map(px => widthAt(px, 1920))).toEqual([COLUMN, COLUMN, COLUMN, COLUMN, 960, 960, 960])
  })
  it('il posto di adesso tiene finché il dito non passa il punto di mezzo di WIDTH_MARGIN', () => {
    const mid = (COLUMN + 960) / 2
    expect(widthAt(mid + 10, 1920, COLUMN)).toBe(COLUMN)
    expect(widthAt(mid + WIDTH_MARGIN, 1920, COLUMN)).toBe(960)
    expect(widthAt(mid - 10, 1920, 960)).toBe(960)
    expect(widthAt(mid - WIDTH_MARGIN, 1920, 960)).toBe(COLUMN)
  })
  it('una misura di adesso che non è un posto fisso non tiene', () => {
    expect(widthAt(700, 1920, 650)).toBe(960)
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
