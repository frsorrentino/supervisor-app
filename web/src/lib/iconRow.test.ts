import { describe, expect, it } from 'vitest'
import {
  altDigit, badge, damping, direction, DRAG_LEAD, DRAG_TRAIL, edges, FOLLOW, keyMove, LEAD, NAME, nameAlpha, nameGap, pill, POP, ratio, ROW,
  rubber, settle, SETTLE, spoken, stretch, target, TRAIL, under, type Span,
} from './iconRow'

// La riga di icone con la pillola elastica (app-posizioni-sistema-27-11, gruppo 1, e app-dock-elastico-27-11). Gli stessi casi di
// IconRowRulesTest in Kotlin.
describe('riga di icone', () => {
  // Tre schede: chiusa, aperta col nome, chiusa; 2 di spazio fra l'una e l'altra.
  const spans: Span[] = [{ left: 0, right: 44 }, { left: 46, right: 110 }, { left: 112, right: 156 }]

  it('il numero sparisce a zero e tiene il colore del suo stato', () => {
    expect(badge(7, 'wait')).toEqual({ text: '7', tone: 'wait' })
    expect(badge(1, 'new')).toEqual({ text: '1', tone: 'new' })
    expect(badge(4, 'count')).toEqual({ text: '4', tone: 'count' })
    expect(badge(0, 'wait')).toBeNull()
    expect(badge(-2, 'new')).toBeNull()
    expect(spoken('Per te', 7)).toBe('Per te 7')
    expect(spoken('Chat', 0)).toBe('Chat')
  })

  it('le molle hanno lo smorzamento relativo dei valori della master', () => {
    expect(ratio(NAME)).toBeCloseTo(0.751, 2)
    expect(ratio(LEAD)).toBeCloseTo(0.751, 3)
    expect(ratio(TRAIL)).toBeCloseTo(0.653, 3)
    expect(ratio(DRAG_LEAD)).toBeCloseTo(0.844, 3)
    expect(ratio(DRAG_TRAIL)).toBeCloseTo(0.727, 3)
    expect(ratio(SETTLE)).toBeCloseTo(0.898, 3)
    expect(ratio(POP)).toBeCloseTo(0.351, 3)
    expect(ratio(ROW)).toBeCloseTo(0.52, 2)
  })

  it('con riduci movimento lo smorzamento diventa critico', () => {
    expect(ratio(TRAIL, true)).toBe(1)
    expect(damping(TRAIL, true)).toBeCloseTo(2 * Math.sqrt(150), 6)
    expect(damping(TRAIL)).toBe(16)
  })

  it('guida il bordo verso cui va la pillola, quello dietro resta indietro', () => {
    expect(edges(1, false)).toEqual([TRAIL, LEAD])
    expect(edges(-1, false)).toEqual([LEAD, TRAIL])
    expect(edges(1, true)).toEqual([DRAG_TRAIL, DRAG_LEAD])
    expect(edges(-1, true)).toEqual([DRAG_LEAD, DRAG_TRAIL])
    expect(edges(0, false)).toEqual([SETTLE, SETTLE])
    expect(edges(0, true)).toEqual([SETTLE, SETTLE])
    expect([direction(0, 3), direction(3, 1), direction(2, 2), direction(-1, 2)]).toEqual([1, -1, 0, 0])
  })

  it("la meta della pillola è la scheda scelta, spostata dall'elastico", () => {
    expect(target(spans[1])).toEqual({ left: 46, right: 110 })
    expect(target(spans[1], -10)).toEqual({ left: 36, right: 100 })
    expect(target(undefined)).toBeNull()
  })

  it('ferma la pillola è la scheda, alta 40', () => {
    expect(pill(46, 110, spans[1])).toEqual({ x: 46, width: 64, height: 40, top: 0 })
    // I bordi incrociati si leggono nell'ordine giusto.
    expect(pill(110, 46, spans[1])).toEqual({ x: 46, width: 64, height: 40, top: 0 })
    // Mai più stretta di 24.
    expect(pill(50, 52, { left: 50, right: 52 })).toEqual({ x: 50, width: 24, height: 40, top: 0 })
    // Le misure del telefono in px: alta 110 a densità 2,75.
    expect(pill(0, 100, { left: 0, right: 100 }, 110, 66).height).toBeCloseTo(110, 4)
  })

  it('tirata si assottiglia ad area costante fra 0,66 e 1,1', () => {
    const mild = pill(0, 100, { left: 0, right: 64 })
    expect([mild.height, mild.top, mild.width]).toEqual([32, 4, 100])
    const far = pill(0, 256, { left: 192, right: 256 })
    expect(far.height).toBeCloseTo(26.4, 4)
    expect(far.top).toBeCloseTo(6.8, 4)
    const squeezed = pill(0, 50, { left: 0, right: 64 })
    expect(squeezed.height).toBeCloseTo(44, 4)
    expect(squeezed.top).toBeCloseTo(-2, 4)
  })

  it('oltre le estremità la pillola tira come un elastico e non passa mai 46', () => {
    expect(rubber(50, 0, 100)).toBe(50)
    expect(rubber(146, 0, 100)).toBeCloseTo(116.32258, 3)
    expect(rubber(-46, 0, 100)).toBeCloseTo(-16.32258, 3)
    expect(rubber(100_000, 0, 100)).toBeLessThan(146)
    // I centri della prima e dell'ultima scheda sono 22 e 134.
    expect(stretch(60, spans)).toBe(0)
    expect(stretch(134, spans)).toBe(0)
    expect(stretch(180, spans)).toBeCloseTo(16.32258, 3)
    expect(stretch(-24, spans)).toBeCloseTo(-16.32258, 3)
    expect(stretch(500, [])).toBe(0)
    // La riga segue al 35 %.
    expect(stretch(180, spans) * FOLLOW).toBeCloseTo(5.7129, 3)
  })

  it('col dito la pillola passa alla scheda sotto, con 6 di tolleranza', () => {
    expect([under(spans, 50, 0), under(spans, 51, 0), under(spans, 45, 0), under(spans, 111, 1), under(spans, 117, 1)]).toEqual([0, 1, 0, 1, 2])
    // Fuori dalla riga conta la scheda all'estremità.
    expect([under(spans, 500, 0), under(spans, -30, 2)]).toEqual([2, 0])
    // Fra due schede resta quella di adesso.
    expect(under(spans, 111, 0)).toBe(0)
    expect(under([], 10, 0)).toBe(-1)
  })

  it('al rilascio guida il bordo che torna verso la riga', () => {
    expect([settle(16), settle(-5), settle(0)]).toEqual([-1, 1, 0])
  })

  it('il nome compare dopo un terzo della corsa e lo spazio cresce con lui', () => {
    expect([nameAlpha(0), nameAlpha(0.2), nameAlpha(1.05)]).toEqual([0, 0, 1])
    expect(nameAlpha(0.5)).toBeCloseTo(0.3, 5)
    expect(nameAlpha(1)).toBeCloseTo(1, 5)
    expect([nameGap(-0.1), nameGap(0.5), nameGap(1.2)]).toEqual([0, 0.5, 1])
  })

  it("Alt e una cifra da 1 a 5 sceglie la scheda che c'è", () => {
    expect([altDigit(1, 5), altDigit(5, 5), altDigit(5, 4), altDigit(6, 9), altDigit(0, 5)]).toEqual([0, 4, null, null, null])
  })

  it('le frecce passano alla vicina senza giro, Inizio e Fine alle estremità', () => {
    expect([keyMove('ArrowRight', 1, 5), keyMove('ArrowRight', 4, 5), keyMove('ArrowLeft', 0, 5), keyMove('Home', 3, 5), keyMove('End', 0, 5)]).toEqual([2, 4, 0, 0, 4])
    expect([keyMove('a', 0, 5), keyMove('ArrowRight', 0, 0)]).toEqual([null, null])
  })
})
