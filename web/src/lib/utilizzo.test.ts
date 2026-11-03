import { describe, expect, it } from 'vitest'
import { elapsedPct, usageAlarm, weekSlot, fiveHourLive, H5_S, peakHour, quotaBar, W7_S, waitsLine, weekDay, weekDelta } from './utilizzo'

describe('utilizzo', () => {
  it('quanto è passato della finestra', () => {
    expect(elapsedPct(3000, 5000, 1000)).toBeCloseTo(60)
    expect(elapsedPct(1000, 5000, 1000)).toBeNull()
    expect(elapsedPct(null, 5000, 1000)).toBeNull()
  })
  it('la settimana contro il ritmo pari', () => {
    expect(weekDelta(41, 1000 + W7_S / 2, 1000)).toBe(-9)
    expect(weekDelta(60, 1000 + W7_S / 2, 1000)).toBe(10)
    expect(weekDelta(null, 1000 + W7_S / 2, 1000)).toBeNull()
  })
  it("l'ora di punta gira a mezzanotte e ignora una giornata vuota", () => {
    expect(peakHour([[1, 0], [2, 3], [1, 1]], 23)).toBe(0)
    expect(peakHour([[0, 0]], 8)).toBeNull()
  })
  it("l'ultima mediana e le chiuse", () => {
    expect(waitsLine([{ median_min: 9, n: 5 }, { median_min: 4.2, n: 7 }, { median_min: null, n: 0 }])).toEqual({ medianMin: 4, closed: 12 })
    expect(waitsLine([])).toBeNull()
  })
})

// quota-senza-cinque-ore: Anthropic toglie il limite a tappe, per account. Conta il dato assente, mai il nome del piano.
describe('barra della quota senza il dato delle 5 ore', () => {
  const now = 1_000_000
  const resetW7 = now + 5 * 86400 // passati 2 giorni su 7: ritmo pari al 29 %
  it('senza h5 mostra la settimana, con il giorno e il ritmo', () => {
    expect(quotaBar(null, 41, null, resetW7, now)).toEqual({ week: true, pct: 41, resetAt: resetW7, windowS: W7_S, delta: 12, day: 3 })
  })
  it('con h5 resta la barra delle 5 ore, anche a zero', () => {
    expect(quotaBar(0, 41, now + 3600, resetW7, now)).toEqual({ week: false, pct: 0, resetAt: now + 3600, windowS: H5_S, delta: null, day: null })
  })
  it('due account: il dato manca su uno solo', () => {
    expect(quotaBar(34, 50, now + 3600, resetW7, now)?.week).toBe(false)
    expect(quotaBar(undefined, 50, null, resetW7, now)?.week).toBe(true)
  })
  it('senza rinnovo la settimana resta, senza ritmo', () => {
    expect(quotaBar(null, 41, null, null, now)).toEqual({ week: true, pct: 41, resetAt: null, windowS: W7_S, delta: null, day: null })
  })
  it('senza nessuno dei due dati non c\'è barra: mai uno 0 % inventato', () => {
    expect(quotaBar(null, null, null, null, now)).toBeNull()
  })
  it('il giorno resta fra 1 e 7', () => {
    expect(weekDay(now + 7 * 86400, now)).toBe(1)
    expect(weekDay(now + 1, now)).toBe(7)
    expect(weekDay(now, now)).toBeNull()
  })
})

describe('letture vecchie', () => {
  it('a finestra finita le 5 ore non valgono più, e resta la settimana', () => {
    expect(fiveHourLive(40, 900, true, 1000)).toBeNull()
    expect(fiveHourLive(40, null, true, 1000)).toBeNull()
    expect(fiveHourLive(40, 2000, true, 1000)).toBe(40)
    expect(fiveHourLive(40, 900, false, 1000)).toBe(40)
    expect(quotaBar(40, 25, 900, 5000, 1000, true)).toMatchObject({ week: true, pct: 25 })
  })
})

describe('allarme della settimana', () => {
  it('dal 75%', () => {
    expect(weekSlot({ a: { w7: 74 } }, 0)?.alarm).toBe(false)
    expect(weekSlot({ a: { w7: 75 } }, 0)?.alarm).toBe(true)
  })
  it('da 10 punti sopra il ritmo', () => {
    expect(weekSlot({ a: { w7: 59, reset_w7: W7_S / 2 } }, 0)?.alarm).toBe(false)
    expect(weekSlot({ a: { w7: 60, reset_w7: W7_S / 2 } }, 0)?.alarm).toBe(true)
  })
  it('mai da una lettura vecchia', () => {
    expect(weekSlot({ a: { w7: 95, stale: true } }, 0)?.alarm).toBe(false)
  })
  it('con due account il più carico fra i freschi', () => {
    expect(weekSlot({ a: { w7: 30 }, b: { w7: 82 }, c: { w7: 99, stale: true } }, 0)).toMatchObject({ account: 'b', pct: 82, alarm: true, many: true })
    expect(weekSlot({}, 0)).toBeNull()
  })
})

// app-utilizzo-una-barra-03-11: la barra fissa delle 5 ore lascia il tabellone; una finestra vicina al limite ci entra da sola in ambra.
describe('allarme d\'uso nel tabellone', () => {
  const q5 = (h5: number, o: { reset?: number; stale?: boolean; w7?: number } = {}) => ({ h5, reset_h5: o.reset ?? 3600, stale: o.stale ?? false, w7: o.w7 ?? null })
  it('le 5 ore dal 75%', () => {
    expect(usageAlarm({ a: q5(74) }, 0)).toBeNull()
    expect(usageAlarm({ a: q5(75) }, 0)).toEqual({ account: 'a', week: false, pct: 75, resetAt: 3600 })
  })
  it('la settimana come lo slot', () => {
    expect(usageAlarm({ a: { w7: 80 } }, 0)).toEqual({ account: 'a', week: true, pct: 80, resetAt: null })
    expect(usageAlarm({ a: { w7: 40 } }, 0)).toBeNull()
  })
  it('mai da una lettura vecchia', () => { expect(usageAlarm({ a: q5(95, { stale: true }) }, 0)).toBeNull() })
  it('la finestra più carica vince, a pari le 5 ore', () => {
    expect(usageAlarm({ a: { w7: 80 }, b: q5(90) }, 0)).toEqual({ account: 'b', week: false, pct: 90, resetAt: 3600 })
    expect(usageAlarm({ a: q5(76, { w7: 92 }) }, 0)).toEqual({ account: 'a', week: true, pct: 92, resetAt: null })
    expect(usageAlarm({ a: q5(80, { w7: 80 }) }, 0)?.week).toBe(false)
  })
})
