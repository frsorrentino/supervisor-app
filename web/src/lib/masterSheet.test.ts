import { describe, expect, it } from 'vitest'
import { anchors, band, cues, DIM_MAX, freezeAt, GAP_PX, HALF_SHARE, nearest, progress, project, RATE, readStop, RECEDE, rubber, settleAt, SHADE_MAX, SHADE_MIN, snap, tap, toRow, up } from './masterSheet'
import { landSpring } from './gesture'

// Il foglio della master (app-posizioni-sistema-27-11 gruppo 4). Gli stessi casi di MasterSheetTest in Kotlin.
describe('foglio della master', () => {
  // Un telefono da 780: sotto la barra in alto restano 700 px; la riga misura 170.
  const a = anchors(700, 170)

  it('le tre misure: la riga misurata, la metà al 58 % del posto come nel mockup, tutto meno lo stacco', () => {
    expect(HALF_SHARE).toBe(0.58)
    expect(a.row).toBe(170)
    expect(a.half).toBeCloseTo(406, 6)
    expect(a.full).toBe(700 - GAP_PX)
    // La colonna della home a 1280 (800 di schermo, 64 di barra).
    const col = anchors(736, 170)
    expect(col.half).toBeCloseTo(426.88, 6)
    expect(col.full).toBe(728)
  })

  it('in un posto basso la metà non scende sotto la riga e le misure coincidono', () => {
    expect(anchors(250, 170)).toEqual({ row: 170, half: 170, full: 242 })
    expect(anchors(150, 170)).toEqual({ row: 170, half: 170, full: 170 })
    expect(anchors(0, -4)).toEqual({ row: 0, half: 0, full: 0 })
  })

  it('la proiezione di iOS: la velocità per 0,499 s', () => {
    expect(RATE).toBe(0.998)
    expect(project(300, 0)).toBe(300)
    expect(project(300, 1000)).toBeCloseTo(799, 6)
    expect(project(300, -1000)).toBeCloseTo(-199, 6)
    expect(project(0, 2000)).toBeCloseTo(2 * 0.998 / 0.002, 6)
  })

  it('senza slancio scatta alla misura più vicina; a pari distanza la più bassa', () => {
    expect(nearest(a, 200)).toBe('row')
    expect(nearest(a, 300)).toBe('half')
    expect(nearest(a, 600)).toBe('full')
    expect(nearest({ row: 170, half: 350, full: 692 }, 260)).toBe('row')
    expect(snap(a, 340, 0)).toBe('half')
  })

  it('un colpo veloce salta la misura vicina, uno lento no', () => {
    // Dalla riga un colpo in su da 1000 px/s porta a 170 + 499: oltre la metà, all'intera.
    expect(snap(a, 180, 1000)).toBe('full')
    // Lo stesso tratto lento resta a metà.
    expect(snap(a, 300, 100)).toBe('half')
    // Dall'intera in giù con forza fino alla riga.
    expect(snap(a, 680, -1200)).toBe('row')
    // In giù piano dall'intera: alla metà.
    expect(snap(a, 620, -400)).toBe('half')
    // Oltre i bordi la proiezione resta sulla misura estrema.
    expect(snap(a, 120, -3000)).toBe('row')
    expect(snap(a, 720, 3000)).toBe('full')
  })

  it("l'elastico: libero fra la riga e l'intera, oltre sempre meno", () => {
    expect(rubber(400, 170, 692, 700)).toBe(400)
    const below = rubber(70, 170, 692, 700)
    expect(below).toBeLessThan(170)
    expect(below).toBeGreaterThan(70)
    expect(170 - below).toBeCloseTo(band(100, 700), 6)
    expect(rubber(800, 170, 692, 700)).toBeGreaterThan(692)
    expect(rubber(800, 170, 692, 700)).toBeLessThan(800)
    // Il doppio della corsa oltre il bordo dà meno del doppio, e mai più del posto.
    expect(band(200, 700)).toBeLessThan(2 * band(100, 700))
    expect(band(1e9, 700)).toBeLessThanOrEqual(700)
    expect(band(0, 700)).toBe(0)
    expect(band(50, 0)).toBe(0)
  })

  it('il tocco sulla riga passa alla misura dopo, ↑ sale fino all\'intera', () => {
    expect(tap('row')).toBe('half')
    expect(tap('half')).toBe('full')
    expect(tap('full')).toBe('row')
    expect([up('row'), up('half'), up('full')]).toEqual(['half', 'full', 'full'])
  })

  it('Esc riporta alla riga; ↓ solo a campo vuoto o fuori dal campo', () => {
    expect(toRow('Escape', true, false)).toBe(true)
    expect(toRow('ArrowDown', false, false)).toBe(true)
    expect(toRow('ArrowDown', true, true)).toBe(true)
    expect(toRow('ArrowDown', true, false)).toBe(false)
    expect(toRow('ArrowUp', false, true)).toBe(false)
    expect(toRow('Enter', false, true)).toBe(false)
  })

  it('la preferenza: una misura nota, altrimenti la riga', () => {
    expect(readStop('half')).toBe('half')
    expect(readStop('full')).toBe('full')
    expect(readStop('row')).toBe('row')
    expect(readStop(null)).toBe('row')
    expect(readStop('compact')).toBe('row')
  })

  // app-posizioni-sistema-27-11, prova 1447: il blocco rigido e la home che si vela e arretra.
  it("l'avanzamento va da 0 alla riga a 1 all'intera, e resta fra i due", () => {
    expect(progress(a, 170)).toBe(0)
    expect(progress(a, a.full)).toBe(1)
    expect(progress(a, (170 + a.full) / 2)).toBeCloseTo(0.5, 6)
    expect(progress(a, 90)).toBe(0)
    expect(progress(a, 900)).toBe(1)
    expect(progress(anchors(150, 170), 170)).toBe(0)
  })

  it("all'intera la home è velata al 40 % e arretra a 0,96; alla riga niente", () => {
    expect(DIM_MAX).toBe(0.4)
    expect(RECEDE).toBe(0.04)
    expect(cues(0)).toEqual({ dim: 0, scale: 1, shade: SHADE_MIN })
    const full = cues(1)
    expect(full.dim).toBeCloseTo(0.4, 6)
    expect(full.scale).toBeCloseTo(0.96, 6)
    expect(full.shade).toBeCloseTo(SHADE_MAX, 6)
    const half = cues(0.5)
    expect(half.dim).toBeCloseTo(0.2, 6)
    expect(half.scale).toBeCloseTo(0.98, 6)
    expect(cues(1.4)).toEqual(cues(1))
    expect(cues(-1)).toEqual(cues(0))
  })

  it('con il movimento ridotto la home non arretra: resta il velo', () => {
    const r = cues(1, true)
    expect(r.scale).toBe(1)
    expect(r.dim).toBeCloseTo(0.4, 6)
    expect(r.shade).toBeCloseTo(SHADE_MAX, 6)
  })

  it("dalla riga la conversazione si ferma a quanta se ne vedrà all'arrivo, e col dito a quanta alla metà", () => {
    const b = anchors(700, 170)
    expect(freezeAt(b, 170, 'row', 'half')).toBe(b.half - 170)
    expect(freezeAt(b, 170, 'row', 'full')).toBe(b.full - 170)
    expect(freezeAt(b, 170, 'row', null)).toBe(b.half - 170)
    expect(freezeAt(b, 120, 'half', 'row')).toBe(0)
  })

  it('fuori dalla riga resta quanta se ne vede adesso: scendendo rigida, salendo si allunga col foglio', () => {
    const b = anchors(700, 170)
    expect(freezeAt(b, b.half, 'half', 'full')).toBe(b.half - 170)
    expect(freezeAt(b, b.half, 'half', null)).toBe(b.half - 170)
    expect(freezeAt(b, b.full, 'full', 'row')).toBe(b.full - 170)
    expect(freezeAt(b, 300, 'row', 'full')).toBe(130)
  })

  it('la molla arriva senza superare la misura, anche partendo dalla velocità del dito', () => {
    // Da 180 px sotto l'arrivo, il dito verso l'arrivo a 4000 px/s: la molla si fa più rigida e non passa oltre.
    const from = -180, v = 4000
    const { w, v: v0 } = landSpring(from, v)
    let past = false
    for (let t = 0; t <= 1.2; t += 1 / 240) if (settleAt(from, v0, w, t).x > 0.5) past = true
    expect(past).toBe(false)
    expect(Math.abs(settleAt(from, v0, w, 1.2).x)).toBeLessThan(0.5)
    // Senza velocità parte da ferma e arriva in meno di mezzo secondo al 99 %.
    const still = landSpring(-300, 0)
    expect(settleAt(-300, still.v, still.w, 0).v).toBe(0)
    expect(Math.abs(settleAt(-300, still.v, still.w, 0.5).x)).toBeLessThan(3)
  })
})
