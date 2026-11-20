import { describe, expect, it } from 'vitest'
import type { Row } from './chatTurns'
import type { TranscriptEntry } from './contract'
import { minePosition, mineStep, mineStops } from './mineJump'

// Gli stessi casi di MineJumpTest.kt (app-trova-messaggi-20-11).
const e = (id: string, role: string) => ({ id, role, text: id }) as TranscriptEntry
const rows = [
  { type: 'turn', key: 't1', request: { type: 'user', entry: e('u1', 'user') }, work: [], answer: e('a1', 'assistant') },
  // Un turno partito da solo (un avviso): non è un messaggio di Franz.
  { type: 'turn', key: 't2', request: null, work: [], answer: e('a2', 'assistant'), auto: true },
  { type: 'turn', key: 't3', request: { type: 'user', entry: e('u3', 'user') }, work: [], answer: e('a3', 'assistant') },
  // Mandato e mai arrivato (il caso del 20/11): si ferma anche lì, sul mandato.
  { type: 'turn', key: 't4', request: { type: 'mine', sent: { id: 's4', session: 'master', text: 'Su Pixel si può installare Claude?', sentAt: 0 }, status: 'failed', entry: null }, work: [], answer: null },
] as unknown as Row[]

describe('il salto fra i miei messaggi', () => {
  it('le fermate sono solo i messaggi di Franz', () => expect(mineStops(rows)).toEqual(['u1', 'u3', 's4']))
  it("si comincia dall'ultimo e ai capi si resta fermi", () => {
    const s = mineStops(rows)
    expect(mineStep(s, null, -1)).toBe('s4')
    expect(mineStep(s, 's4', -1)).toBe('u3')
    expect(mineStep(s, 'u1', -1)).toBe('u1')
    expect(mineStep(s, 's4', 1)).toBe('s4')
    expect(mineStep([], null, -1)).toBeNull()
  })
  it('il contatore conta dal più vecchio', () => {
    const s = mineStops(rows)
    expect(minePosition(s, 'u3')).toEqual([2, 3])
    expect(minePosition(s, 'a1')).toBeNull()
  })
})
