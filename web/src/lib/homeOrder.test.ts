import { describe, expect, it } from 'vitest'
import { DEFAULT_ORDER, move, normalize, orderArg } from './homeOrder'

describe('homeOrder', () => {
  it('senza un ordine salvato, il predefinito', () => {
    expect(normalize(null)).toEqual(DEFAULT_ORDER)
    expect(normalize([])).toEqual(DEFAULT_ORDER)
  })
  it('ignora sconosciute e doppie, accoda le mancanti', () => {
    expect(normalize(['esiti', 'nuova', 'esiti', 'sessions'])).toEqual(['esiti', 'sessions', 'suggestions', 'lavori', 'altro'])
  })
  it('sposta e sposta le altre', () => {
    expect(move(DEFAULT_ORDER, 0, 2)).toEqual(['suggestions', 'lavori', 'sessions', 'esiti', 'altro'])
    expect(move(DEFAULT_ORDER, 0, 9)).toEqual(DEFAULT_ORDER)
  })
  it("l'arg è un array JSON, null per il predefinito", () => {
    expect(orderArg(DEFAULT_ORDER)).toBeNull()
    expect(orderArg(['esiti', 'sessions', 'suggestions', 'lavori', 'altro'])).toBe('["esiti","sessions","suggestions","lavori","altro"]')
  })
})
