import { describe, expect, it } from 'vitest'
import { boardFold } from './kpi'

// app-tabellone-filtro-25-11 (Franz 25/11 17:04): la casella scelta apre Sessioni; togliendo il filtro si torna come prima.
describe('tabellone filtro', () => {
  const shut = { open: false, byFilter: false }
  it('scegliere una casella apre Sessioni', () => expect(boardFold('aspetta', shut)).toEqual({ open: true, byFilter: true }))
  it('togliere il filtro richiude quello che aveva aperto', () => expect(boardFold(null, boardFold('lavoro', shut))).toEqual(shut))
  it('aperta a mano resta aperta', () => {
    const hand = { open: true, byFilter: false }
    expect(boardFold('aspetta', hand)).toEqual(hand)
    expect(boardFold(null, hand)).toEqual(hand)
  })
  it('cambiare casella non richiude', () => { const a = boardFold('aspetta', shut); expect(boardFold('lavoro', a)).toEqual(a) })
})
