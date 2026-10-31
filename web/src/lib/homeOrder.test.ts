import { describe, expect, it } from 'vitest'
import { DEFAULT_ORDER, dragTarget, move, normalize, orderArg, type HomeSection } from './homeOrder'

describe('homeOrder', () => {
  it('senza un ordine salvato, il predefinito', () => {
    expect(normalize(null)).toEqual(DEFAULT_ORDER)
    expect(normalize([])).toEqual(DEFAULT_ORDER)
  })
  it('ignora sconosciute e doppie, accoda le mancanti', () => {
    expect(normalize(['esiti', 'nuova', 'esiti', 'sessions'])).toEqual(['esiti', 'sessions', 'suggestions', 'lavori', 'usage', 'system', 'plugins', 'closed'])
  })
  it('sposta e sposta le altre', () => {
    expect(move(DEFAULT_ORDER, 0, 2)).toEqual(['suggestions', 'lavori', 'sessions', 'esiti', 'usage', 'system', 'plugins', 'closed'])
    expect(move(DEFAULT_ORDER, 0, 9)).toEqual(DEFAULT_ORDER)
  })
  it("l'arg è un array JSON, null per il predefinito", () => {
    expect(orderArg(DEFAULT_ORDER)).toBeNull()
    expect(orderArg(['esiti', 'sessions', 'suggestions', 'lavori', 'usage', 'system', 'plugins', 'closed'])).toBe('["esiti","sessions","suggestions","lavori","usage","system","plugins","closed"]')
  })
})

describe('trascinamento sul posto', () => {
  const abc: HomeSection[] = ['sessions', 'lavori', 'esiti']
  const m = { sessions: 10, lavori: 90, esiti: 200 }
  it('scavalca la vicina solo quando il puntatore la incrocia', () => {
    expect(dragTarget(abc, 'sessions', 89, m)).toEqual(abc)
    expect(dragTarget(abc, 'sessions', 91, m)).toEqual(['lavori', 'sessions', 'esiti'])
    expect(dragTarget(abc, 'esiti', 91, m)).toEqual(abc)
    expect(dragTarget(abc, 'esiti', 89, m)).toEqual(['sessions', 'esiti', 'lavori'])
  })
  it('una sezione assente non fa da ostacolo', () => {
    expect(dragTarget(abc, 'sessions', 91, { sessions: 10, esiti: 90 })).toEqual(['lavori', 'esiti', 'sessions'])
  })
  it('dopo lo scambio resta ferma', () => {
    const s: HomeSection[] = ['lavori', 'sessions', 'esiti']
    expect(dragTarget(s, 'sessions', 91, { lavori: 40, sessions: 140, esiti: 260 })).toEqual(s)
  })
})

describe('righe separabili (Franz 31/10 15:50)', () => {
  it('in un ordine già salvato «altro» si espande al suo posto', () => {
    expect(normalize(['altro', 'sessions', 'suggestions', 'lavori', 'esiti'])).toEqual(['usage', 'system', 'plugins', 'closed', 'sessions', 'suggestions', 'lavori', 'esiti'])
  })
  it('le righe già separate restano dove sono, e le mancanti si accodano', () => {
    expect(normalize(['system', 'sessions', 'usage'])).toEqual(['system', 'sessions', 'usage', 'suggestions', 'lavori', 'esiti', 'plugins', 'closed'])
  })
})
