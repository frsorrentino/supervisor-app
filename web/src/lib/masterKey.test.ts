import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { comboDir, DEAD_FINGER, DEAD_MOUSE, dirAt, moves, nameOf, type Dir, type Spot } from './masterKey'

// Gli stessi casi di MasterKeyTest.kt.
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/master-key-cases.json', import.meta.url), 'utf8')) as {
  moves: { name: string; now: Spot; sessionsOpen: boolean; out: string[] }[]
  dirAt: { name: string; dx: number; dy: number; dead: number; dirs: Dir[]; out: Dir | null }[]
  combo: { name: string; held: ('N' | 'S' | 'E' | 'W')[]; out: Dir | null }[]
}
const show = (from: Spot) => (m: { dir: Dir; to: Spot }) => `${m.dir}>${m.to.side}/${m.to.height}/${m.to.homeRow ? 'H' : '-'}:${nameOf(from, m.to)}`

describe('tasto tondo della master: da dove sta, le frecce possibili', () => {
  for (const c of file.moves) it(c.name, () => expect(moves(c.now, c.sessionsOpen).map(show(c.now))).toEqual(c.out))
  it('mai due frecce nella stessa direzione, e mai il posto di adesso', () => {
    for (const side of ['left', 'right'] as const) for (const height of ['bar', 'column', 'half', 'compact'] as const) for (const homeRow of [false, true]) for (const open of [false, true]) {
      const now: Spot = { side, height, homeRow: homeRow && height === 'column' }, ms = moves(now, open)
      expect(new Set(ms.map(m => m.dir)).size).toBe(ms.length)
      expect(ms.some(m => m.to.side === now.side && m.to.height === now.height && m.to.homeRow === now.homeRow)).toBe(false)
    }
  })
})
describe('tasto tondo della master: la freccia sotto il puntatore', () => {
  for (const c of file.dirAt) it(c.name, () => expect(dirAt(c.dx, c.dy, c.dead, c.dirs)).toBe(c.out))
  for (const c of file.combo) it(`tastiera: ${c.name}`, () => expect(comboDir(c.held)).toBe(c.out))
  it('il centro: 8 px col mouse, 30 col dito', () => expect([DEAD_MOUSE, DEAD_FINGER]).toEqual([8, 30]))
})
