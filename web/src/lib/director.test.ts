import { describe, expect, it } from 'vitest'
import { boxAt, invert, landAt, nested, reflowAt, wave, type Box } from './director'

// Il regista dei movimenti della plancia (app-fluidita-06-11).
const parse = (tf: string) => { const n = tf.match(/-?[\d.]+(e-?\d+)?/g)!.map(Number); return { dx: n[0], dy: n[1], sx: n[2], sy: n[3] } }
/** Dove si vede `b` sotto la transform `tf` con l'origine in alto a sinistra. */
const apply = (tf: string, b: Box): Box => { const t = parse(tf); return { x: b.x + t.dx, y: b.y + t.dy, w: b.w * t.sx, h: b.h * t.sy } }
const near = (a: Box, b: Box) => { for (const k of ['x', 'y', 'w', 'h'] as const) expect(a[k]).toBeCloseTo(b[k], 3) }

describe('il regista', () => {
  it('l\'inversa porta il riquadro d\'arrivo su quello di partenza', () => {
    const a = { x: 10, y: 20, w: 100, h: 50 }, b = { x: 300, y: 40, w: 400, h: 200 }
    near(apply(invert(a, b), b), a)
  })
  it('le curve partono da 0 e arrivano a 1, la molla senza rimbalzo mai oltre', () => {
    expect([reflowAt(0), reflowAt(1), landAt(0), landAt(1)]).toEqual([0, 1, 0, 1])
    for (let t = 0; t <= 1; t += 0.05) expect(landAt(t)).toBeLessThanOrEqual(1)
    expect(reflowAt(0.5)).toBeGreaterThan(0.8)
  })
  it('l\'onda parte dall\'angolo in alto a sinistra, 18 ms a passo e al massimo 160', () => {
    const w = wave([['b', { x: 30, y: 0, w: 1, h: 1 }], ['a', { x: 0, y: 0, w: 1, h: 1 }], ...Array.from({ length: 12 }, (_, i) => [`z${i}`, { x: 0, y: 100 + i, w: 1, h: 1 }] as [string, Box])])
    expect(w.get('a')).toBe(0); expect(w.get('b')).toBe(18); expect(w.get('z11')).toBe(160)
  })
  it('una scheda dentro una sezione che si muove fa sullo schermo il suo percorso, non quello della sezione', () => {
    const up = { from: { x: 0, y: 0, w: 800, h: 600 }, to: { x: 1000, y: 100, w: 400, h: 500 }, delay: 0, dur: 500, ease: reflowAt }
    const m = { from: { x: 20, y: 40, w: 300, h: 80 }, to: { x: 1010, y: 160, w: 380, h: 90 }, delay: 54, dur: 500, ease: reflowAt }
    const k = nested(m, up, 20)
    const end = Math.max(m.delay + m.dur, up.delay + up.dur)
    for (const [i, f] of k.entries()) {
      const ms = (end * i) / 20
      // La sezione al tempo ms, poi la scheda dentro di lei con la sua transform: dove si vede sullo schermo.
      const p = boxAt(up, ms), sx = p.w / up.to.w, sy = p.h / up.to.h
      const local = apply(f.transform as string, m.to)
      const seenAt = { x: p.x + (local.x - up.to.x) * sx, y: p.y + (local.y - up.to.y) * sy, w: local.w * sx, h: local.h * sy }
      near(seenAt, boxAt(m, ms))
    }
  })
})
