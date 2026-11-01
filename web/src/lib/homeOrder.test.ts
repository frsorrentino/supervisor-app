import { describe, expect, it } from 'vitest'
import { DEFAULT_ORDER, dragTarget, move, normalize, orderArg, type HomeSection } from './homeOrder'
import { dropAt, orderArg as argWithPin, overSlot, pinned, release, slotKey, withPin } from './homeOrder'

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

describe('slot del tabellone', () => {
  it("l'aggancio sta nella lista e fuori dall'ordine", () => {
    const saved = withPin(DEFAULT_ORDER, 'suggestions')
    expect(saved[saved.length - 1]).toBe('pin:suggestions')
    expect(normalize(saved)).toEqual(DEFAULT_ORDER)
    expect(pinned(saved)).toBe('suggestions')
    expect(JSON.parse(argWithPin(DEFAULT_ORDER, 'suggestions')!)).toEqual(saved)
  })
  it('solo le voci ammesse', () => {
    expect(pinned(['pin:sessions'])).toBeNull()
    expect(pinned(['pin:esiti'])).toBeNull()
    expect(withPin(DEFAULT_ORDER, 'sessions')).toEqual(DEFAULT_ORDER)
  })
  it("l'allarme prende lo slot e lo rende", () => {
    expect(slotKey('system', true)).toBe('usage')
    expect(slotKey('system', false)).toBe('system')
    expect(slotKey(null, false)).toBeNull()
  })
})

describe('slot_rilascio', () => {
  it('slot_rilascio: solo dentro il riquadro', () => {
    expect(overSlot('usage', 150, 100, 200, false)).toBe(true)
    expect(overSlot('usage', 201, 100, 200, false)).toBe(false)
    expect(overSlot('usage', 99, 100, 200, false)).toBe(false)
    expect(overSlot('sessions', 150, 100, 200, false)).toBe(false)
    expect(overSlot('usage', 150, 100, 200, true)).toBe(false)
  })
  it('slot_rilascio: chi esce torna sotto il dito', () => {
    const o: HomeSection[] = ['sessions', 'lavori', 'usage', 'esiti']
    const mids = { sessions: 100, lavori: 200, esiti: 300 }
    expect(dropAt(o, 'usage', 50, mids)).toEqual(['usage', 'sessions', 'lavori', 'esiti'])
    expect(dropAt(o, 'usage', 150, mids)).toEqual(['sessions', 'usage', 'lavori', 'esiti'])
    expect(dropAt(o, 'usage', 350, mids)).toEqual(['sessions', 'lavori', 'esiti', 'usage'])
  })
  it('slot_rilascio: finisce dove lasci il dito', () => {
    const o: HomeSection[] = ['sessions', 'usage', 'esiti']
    const moved: HomeSection[] = ['usage', 'sessions', 'esiti']
    const mids = { sessions: 100, esiti: 300 }
    expect(release(o, moved, 'usage', null, false, true, 0, mids)).toEqual({ order: o, pin: 'usage' })
    expect(release(o, o, 'usage', 'usage', false, false, 400, mids)).toEqual({ order: ['sessions', 'esiti', 'usage'], pin: null })
    expect(release(o, moved, 'usage', null, false, false, 50, mids)).toEqual({ order: moved, pin: null })
    expect(release(o, moved, 'lavori', 'usage', false, false, 50, mids)).toEqual({ order: moved, pin: 'usage' })
  })
})
