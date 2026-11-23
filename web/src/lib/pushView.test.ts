import { describe, expect, it } from 'vitest'
import { isClosed, sameItem } from './pushView'

// Contratto 1.160 (app-per-te-ordine-23-11): `attention_closed` chiude le notifiche della stessa voce, per ref o wid.
describe('una voce risolta altrove', () => {
  const p = { kind: 'attention_closed', ref: 'w57c9b42c2a', wid: 'w57c9b42c2a', quiet: true, title: 'Risolto altrove' }
  it('si riconosce dal kind', () => {
    expect(isClosed(p)).toBe(true)
    expect(isClosed({ kind: 'attention_reminder' })).toBe(false)
  })
  it('chiude solo le notifiche della stessa voce', () => {
    expect(sameItem({ ref: 'w57c9b42c2a' }, p)).toBe(true)
    expect(sameItem({ wid: 'w57c9b42c2a', ref: 'albo' }, { kind: 'attention_closed', ref: 'albo-x', wid: 'w57c9b42c2a' })).toBe(true)
    expect(sameItem({ ref: 'altro' }, p)).toBe(false)
    expect(sameItem(null, p)).toBe(false)
    expect(sameItem({ ref: null }, { kind: 'attention_closed', ref: null, wid: null })).toBe(false)
  })
})
