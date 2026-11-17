import { describe, expect, it } from 'vitest'
import { afterAnswer, sentWhat } from './tiAspetta'

// app-attesa-dopo-scelta-17-11: dopo una risposta dalla scheda di una voce, la riga di conferma e dove si va.
const live = (s: string) => s === 'preventivi'

describe('dopo una risposta', () => {
  it('la scelta su «preventivi: Asiano» dice cosa è partito e porta alla sessione', () => {
    expect(afterAnswer({ session: 'preventivi' }, sentWhat('Asiano', null), 'preventivi', live)).toEqual({ line: 'Mandato «Asiano» a preventivi', session: 'preventivi' })
  })
  it('col testo scritto nel campo lo dice', () => {
    expect(sentWhat('Rispondi con parole tue', '  il secondo, ma più corto ')).toBe('Rispondi con parole tue: il secondo, ma più corto')
  })
  it('una voce senza sessione: alla master, poi la home', () => {
    expect(afterAnswer({ session: null }, 'Approva', null, live)).toEqual({ line: 'Mandato «Approva» a master', session: null })
  })
  it('la sessione della voce chiusa: la home, il nome resta nella riga', () => {
    expect(afterAnswer({ session: 'ledger-api' }, 'Va', null, live)).toEqual({ line: 'Mandato «Va» a ledger-api', session: null })
  })
})
