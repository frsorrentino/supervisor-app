import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import type { WaitItem } from './contract'
import { silenceOffer, silencePending, silenceWhenLong, silenceWhenShort } from './silence'
import { doneFromPages } from './lavori'
import { outcomeOf, roundsOf } from './scelta'
import { voceOf } from './perTe'
import { silenceOf } from './voceRow'

// Gli stessi casi di SilenceTest.kt (app-silenzio-assenso-23-11, contratto 1.159), sulla fixture delle attese.
const list = JSON.parse(readFileSync(new URL('../../../contract/attese-rd-sample.json', import.meta.url), 'utf8')).attese.items as WaitItem[]
const ops = ['attese', 'silence']
const item = (kind: string) => list.find(w => w.kind === kind)!

describe('il silenzio assenso', () => {
  // Contratto 1.160: la regola è del registro, `silence_ok`; l'app non ne ha una sua.
  it('solo dove il relay dice silence_ok', () => {
    const yes = { ...item('ok'), silence_ok: true }
    expect(silenceOffer(yes, ops)).toBe(true)
    expect(silenceOffer({ ...item('prova'), silence_ok: true }, ops)).toBe(true)
    expect(silenceOffer({ ...item('ok'), silence_ok: false }, ops)).toBe(false)
    // Un relay prima della 1.160 non ha il campo: niente tasto (la fixture ha «pubblica 1.4 sul Play Store»).
    expect(list.filter(w => silenceOffer(w, ops))).toEqual([])
    expect(silenceOffer(yes, ['attese'])).toBe(false)
  })
  it('mai su una carta chiusa o senza compito', () => {
    expect(silenceOffer({ ...item('ok'), silence_ok: true, task: null }, ops)).toBe(false)
    expect(silenceOffer({ ...item('ok'), silence_ok: true, state: 'approved' } as WaitItem, ops)).toBe(false)
  })
  it('le date', () => {
    const until = Date.UTC(2026, 10, 26, 12, 30) / 1000 // 13:30 a Roma
    expect(silenceWhenLong(until, false, 'Europe/Rome')).toBe('giovedì 26/11 alle 13:30')
    expect(silenceWhenLong(until, true, 'Europe/Rome')).toBe('Thursday 26/11 at 13:30')
    expect(silenceWhenShort(until, false, 'Europe/Rome')).toBe('gio 26/11')
    expect(silenceWhenShort(until, true, 'Europe/Rome')).toBe('Thu 26/11')
    expect(silencePending(until, until - 1)).toBe(true)
    expect(silencePending(until, until)).toBe(false)
    expect(silencePending(null, 0)).toBe(false)
  })
})

// Come la cronologia e le schede riconoscono un compito chiuso per silenzio assenso (contratto 1.159).
describe('i chiusi per silenzio assenso', () => {
  it('la riga dei chiusi porta la scadenza; le altre no', () => {
    const rows = doneFromPages({ '2026-11-26': { date: '2026-11-26', more: false, days: [], rows: [
      { task: 'atlas-chi-siamo', title: 'Bozza della pagina Chi siamo', at: 1795700400, outcome: 'green', silence: { kind: 'ok', until: 1795699800 } },
      { task: 'app-trova', title: 'Trova messaggi', at: 1795690000, outcome: 'green' },
    ] } }, 'Europe/Rome')[0].rows
    expect(rows[0].silence).toBe(1795699800)
    expect(rows[0].outcome).toBe('green')
    expect('silence' in rows[1]).toBe(false)
  })
  it('la scheda e i giri: «silence», mai approvata', () => {
    expect(outcomeOf({ state: 'silence', answered: { by: 'silenzio assenso', at: 1795700400, reason: 'silenzio assenso: nessuna risposta entro le 26/11 13:30' } } as WaitItem).state).toBe('silence')
    expect(roundsOf({ rounds: [{ id: 'r1', at: 1795400000, text: 'Bozza', state: 'silence', by: 'silenzio assenso', answered_at: 1795700400 }] } as unknown as WaitItem)[0].state).toBe('silence')
  })
  it('la riga di Per te dice quando si chiude, finché la scadenza non è passata', () => {
    const w = { ...item('ok'), silence_until: Date.UTC(2026, 10, 26, 12, 30) / 1000 }
    expect(silenceOf(voceOf(w), w.silence_until - 60)).toMatch(/^si chiude da sola \S+ 26\/11$/)
    expect(silenceOf(voceOf(w), w.silence_until)).toBeNull()
    expect(silenceOf(voceOf(item('ok')), 0)).toBeNull()
  })
})
