import { describe, expect, it } from 'vitest'
import type { Commitment, KpiTurn } from './contract'
import { before, nextStep, task, todaySessions } from './sessionK'

// Gli stessi casi di SessionKTest in Kotlin: i turni veri di joyconceptstore del 19/11.
const t = (h: number, m: number, min: number, outcome = 'ok', text?: string): KpiTurn => {
  const s = h * 3600 + m * 60
  return { start: s, end: s + min * 60, outcome, tokens: 0, cache_read: 0, ...(text ? { text } : {}) }
}
const joy = [t(16, 40, 25, 'ok', 'categorie di 252 prodotti corrette'), t(9, 4, 14), t(9, 28, 1), t(9, 35, 6), t(9, 49, 9), t(16, 34, 2, 'fail')]
const c = (task: string, session: string, idle_s: number, next?: string) => ({ task, title: task, session, idle_s, state: 'running', age_s: 0, ...(next ? { next } : {}) }) as unknown as Commitment

describe('la scheda K', () => {
  it('Prima mostra gli ultimi tre, dal più vecchio', () => {
    const b = before(joy)!
    expect(b.turns.map(x => x.start)).toEqual([9 * 3600 + 49 * 60, 16 * 3600 + 34 * 60, 16 * 3600 + 40 * 60])
    expect([b.hidden, b.total, b.minutes, b.failed]).toEqual([3, 6, 57, 1])
    expect(b.turns.map(x => x.ok)).toEqual([true, false, true])
    expect(b.turns[2].text).toBe('categorie di 252 prodotti corrette')
    expect(b.turns[0].text).toBeNull()
  })
  it('tutti i turni, e un turno breve vale un minuto', () => {
    expect(before(joy, true)!.turns.length).toBe(6)
    expect(before([{ start: 0, end: 10, outcome: 'ok', tokens: 0, cache_read: 0 }])!.turns[0].minutes).toBe(1)
    expect(before([])).toBeNull()
  })
  it('il compito è il più fresco della sessione, il passo viene dal registro', () => {
    const l = [c('joy-capi-19-11', 'joy', 60, 'controllare su Meta gli eventi server'), c('joy-old', 'joy', 9000), c('other', 'kb', 1)]
    expect(task(l, 'joy')?.task).toBe('joy-capi-19-11')
    expect(task(l, 'nessuna')).toBeNull()
    expect(nextStep(task(l, 'joy'), ['altro'])).toBe('controllare su Meta gli eventi server')
    expect(nextStep(null, [' rileggi il log '])).toBe('rileggi il log')
    expect(nextStep(null, [])).toBeNull()
  })
})

describe('la pagina Utilizzo dopo la K', () => {
  it('elenca chi ha lavorato oggi, il più recente in cima', () => {
    const ev = (at: number) => ({ at, kind: 'stop', text: '' })
    const page = { since: 0, sessions: [
      { session: 'ieri', live: false, events: [ev(50)] },
      { session: 'supervisor', live: true, events: [ev(150), ev(400)] },
      { session: 'app', live: true, events: [ev(300)] },
    ] }
    const data = { at: 0, kpi_tasks: null, kpi_checks_week: null, kpi_health: null, kpi_suggestions_7d: null, kpi_waits: null,
      kpi_turns_today: [{ session: 'joyconceptstore', turns: joy }] }
    expect(todaySessions(page, data, 100)).toEqual(['joyconceptstore', 'supervisor', 'app'])
    expect(todaySessions(null, null, 100)).toEqual([])
  })
})
