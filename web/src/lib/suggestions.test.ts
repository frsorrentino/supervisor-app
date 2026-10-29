import { describe, expect, it } from 'vitest'
import type { State, Suggestion } from './contract'
import { KINDS, known, options, visible } from './suggestions'

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
    expect(KINDS.length).toBe(11)
    // Contratto 1.94: le note di claude-observe di un plugin, «Note del plugin».
    expect(known('observe')).toBe(true)
    expect(known('unsaved')).toBe(true)
    expect(known('zzz')).toBe(false)
  })
  // Contratto 1.88: la coda per rank (assente o fuori 0-9 = 5), poi dalla più recente; al massimo due dello stesso tipo.
  const q = (id: string, rank: number | null, at: number, kind = 'cleanup', options: string[] | null = null): Suggestion => ({ id, kind, key: `k-${id}`, text: id, at, rank, options })
  it('rank prima di at', () => {
    const v = visible(st([q('a', 6, 50), q('b', 2, 10, 'idea'), q('c', 4, 40, 'unsaved'), q('d', null, 60, 'waiting')]), new Set(), 0)
    expect(v.shown.map(s => s.id)).toEqual(['b', 'c', 'd'])
    expect(v.total).toBe(4)
  })
  it('rank assente o fuori 0-9 vale 5', () => {
    const v = visible(st([q('a', 6, 90, 'idea'), q('x', 12, 80, 'waiting'), q('d', null, 60, 'unsaved'), q('c', 4, 10, 'choice')]), new Set(), 0)
    expect(v.shown.map(s => s.id)).toEqual(['c', 'x', 'd'])
  })
  it('al massimo due dello stesso tipo fra le tre', () => {
    const v = visible(st([q('u1', 4, 30, 'unsaved'), q('u2', 4, 20, 'unsaved'), q('u3', 4, 10, 'unsaved'), q('i', 6, 5, 'idea')]), new Set(), 0)
    expect(v.shown.map(s => s.id)).toEqual(['u1', 'u2', 'i'])
    expect(v.total).toBe(4)
  })
  it('una coda di un tipo solo ne mostra comunque tre', () => {
    const v = visible(st([1, 2, 3, 4, 5].map(i => q(`u${i}`, 4, 100 - i, 'unsaved'))), new Set(), 0)
    expect(v.shown.map(s => s.id)).toEqual(['u1', 'u2', 'u3'])
    expect(v.total).toBe(5)
  })
  it('le opzioni solo sulle choice con 2-4 etichette', () => {
    expect(options(q('c', 2, 0, 'choice', ['A', 'B']))).toEqual(['A', 'B'])
    expect(options(q('c', 2, 0, 'choice', ['A']))).toEqual([])
    expect(options(q('c', 2, 0, 'choice', ['A', 'B', 'C', 'D', 'E']))).toEqual([])
    expect(options(q('i', 2, 0, 'idea', ['A', 'B']))).toEqual([])
  })
})
