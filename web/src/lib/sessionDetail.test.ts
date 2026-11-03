import { describe, expect, it } from 'vitest'
import type { Session } from './contract'
import { detailOf } from './sessionDetail'

// Gemello di SessionDetailTest.kt (app-attesa-dettagli-02-11).
const s = (state: Session['state'] = 'idle'): Session => ({ id: 'a', name: 'a', account: 'personale', project: 'a', state, since: 100 } as Session)
const full = "Ho confrontato le due strade: l'unione tiene un solo elenco, la separazione due.\n" +
  "Consiglio l'unione.\nEsito: analisi della home valutata: 6 punti\nTipo: aspetta\nProssimi: Ok ai consigliati, con l'unione · Solo i punti 1-3 · Rifai l'analisi"
const waiting: Session = {
  ...s('waiting'),
  outcome: { short: 'analisi della home valutata: 6 punti', full, at: 500 },
  next_steps: [{ text: "Ok ai consigliati, con l'unione", blocking: true }, { text: 'Solo i punti 1-3', blocking: false }, { text: "Rifai l'analisi", blocking: false }],
  turn_blocking: { text: "Ok ai consigliati, con l'unione", at: 500 },
}

describe('detailOf', () => {
  it("la ferma mostra l'ultimo messaggio senza le righe di chiusura", () => {
    const d = detailOf(waiting, 'waiting', "Ok ai consigliati, con l'unione")
    expect(d.said).toBe("analisi della home valutata: 6 punti\nHo confrontato le due strade: l'unione tiene un solo elenco, la separazione due.\nConsiglio l'unione.")
    expect(d.saidAt).toBe(500)
  })
  it('le altre voci di Prossimi diventano tasti', () =>
    expect(detailOf(waiting, 'waiting', 'x').steps).toEqual(['Solo i punti 1-3', "Rifai l'analisi"]))
  it('con needs-you il tasto è Fatto e le voci restano tutte', () =>
    expect(detailOf({ ...waiting, needs_you: { reason: 'rinnova il certificato', at: 600 }, turn_blocking: null }, 'waiting', 'rinnova il certificato').steps).toHaveLength(3))
  it('con una domanda non aggiunge niente', () => {
    const d = detailOf({ ...waiting, question: { id: 'q', kind: 'ask', text: 'Pubblico?', options: [], tier: 'low', asked_at: 700 } } as Session, 'waiting', 'Pubblico?')
    expect(d.said).toBeNull(); expect(d.steps).toEqual([])
  })
  it('niente testo se ripete la riga in testa', () =>
    expect(detailOf({ ...waiting, outcome: { short: 'Ok?', full: 'Esito: Ok?\nProssimi: !Ok', at: 500 } }, 'waiting', 'Ok?').said).toBeNull())
  it('chi lavora dice cosa sta facendo nel turno', () => {
    const d = detailOf({ ...s('busy'), tool: 'Bash', tool_note: 'Lancio i test del core', turn_started: 900, outcome: { short: 'prima', full: 'prima', at: 500 } }, 'working', 'prima')
    expect(d.doing).toBe('Bash · Lancio i test del core'); expect(d.said).toBeNull(); expect(d.steps).toEqual([])
  })
  it('senza strumento, o a turno finito, niente', () => {
    expect(detailOf({ ...s('busy'), turn_started: 900 }, 'working', null).doing).toBeNull()
    expect(detailOf({ ...s(), tool: 'Bash', turn_started: 400, outcome: { short: 'x', full: 'x', at: 500 } }, 'still', 'x').doing).toBeNull()
  })
})
