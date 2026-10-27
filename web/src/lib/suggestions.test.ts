import { describe, expect, it } from 'vitest'
import type { State, Suggestion } from './contract'
import { KINDS, known, visible } from './suggestions'

const sg = (id: string, at: number, expires_at: number | null = null): Suggestion => ({ id, kind: 'cleanup', key: `k-${id}`, text: id, prompt: `p ${id}`, at, expires_at })
const st = (suggestions?: Suggestion[]) => ({ suggestions } as unknown as State)

describe('Suggerimenti della master (contratto 1.84, scheda A)', () => {
  it('al massimo tre, dal più recente, senza i toccati e gli scaduti; il totale per «3 di N»', () => {
    const v = visible(st([sg('a', 10), sg('b', 40), sg('c', 30), sg('d', 20), sg('e', 50, 90), sg('f', 60)]), new Set(['f']), 100)
    expect(v.shown.map(s => s.id)).toEqual(['b', 'c', 'd'])
    expect(v.total).toBe(4)
    expect(visible(st(), new Set(), 100)).toEqual({ shown: [], total: 0 })
  })
  it('i dieci tipi; un tipo sconosciuto non è fra i noti', () => {
    expect(KINDS.length).toBe(10)
    expect(known('unsaved')).toBe(true)
    expect(known('zzz')).toBe(false)
  })
})
