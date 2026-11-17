import { describe, expect, it } from 'vitest'
import { waitLoad, waitShown } from './tiAspetta'

// app-attesa-caricamento-17-11: la scheda di una voce parte da quello che l'app sa già.
const list = [{ id: 'wd0b4265780', title: 'preventivi: Asiano o Bianchi?' }, { id: 'w2', title: 'altro' }]

describe('la scheda mentre legge', () => {
  it("una voce nell'elenco si mostra subito, prima della risposta del PC", () => {
    expect(waitShown('wd0b4265780', null, list, false)).toEqual(list[0])
  })
  it('la risposta del PC prende il suo posto', () => {
    const full = { id: 'wd0b4265780', title: 'preventivi: Asiano o Bianchi?', msg: 'da dove viene' }
    expect(waitShown('wd0b4265780', full, list, false)).toBe(full)
  })
  it('una voce che il PC dice chiusa non si mostra più dall\'elenco; una sconosciuta non c\'è', () => {
    expect(waitShown('wd0b4265780', null, list, true)).toBeNull()
    expect(waitShown('w9', null, list, false)).toBeNull()
  })
  it('il segno: dettagli in arrivo, oltre 10 s PC lento, lettura fallita, niente a lettura finita', () => {
    expect(waitLoad(false, false, false, false)).toBe('details')
    expect(waitLoad(false, false, false, true)).toBe('slow')
    expect(waitLoad(false, true, false, true)).toBe('failed')
    expect(waitLoad(true, false, false, false)).toBeNull()
    expect(waitLoad(false, false, true, false)).toBeNull()
  })
})
