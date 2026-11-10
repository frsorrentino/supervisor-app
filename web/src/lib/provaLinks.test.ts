import { describe, expect, it } from 'vitest'
import { provaLink, provaOf, provaRefs, readProva } from './provaLinks'

// Gli stessi casi di ProvaLinksTest.kt (app-prova-link-10-11, contratto 1.134).
const refs = (t: string) => provaRefs(t).map(r => [t.slice(r.start, r.end), r.n])
describe('prove citate', () => {
  it('una prova citata è un link', () => {
    expect(refs('La prova #518 è segnata «non va».')).toEqual([['prova #518', 518]])
    expect(refs('Prova #518 segnata «non va» con le sue parole')).toEqual([['Prova #518', 518]])
    expect(refs('prove 543 ok; il controllo gira')).toEqual([['prove 543', 543]])
  })
  it('più prove nella stessa frase sono ognuna un link', () => {
    expect(refs('le prove #518 e #527 sono da Franz')).toEqual([['prove #518', 518], ['#527', 527]])
    expect(refs('prove 518, 527 e 543 passate')).toEqual([['prove 518', 518], ['527', 527], ['543', 543]])
  })
  it("quello che non è una prova resta testo", () => {
    expect(refs('prova 10 minuti e poi dimmi')).toEqual([])
    expect(refs('approva #518')).toEqual([])
    expect(refs('la prova è passata')).toEqual([])
  })
  it('il link apre la prova', () => {
    expect(provaLink(518)).toBe('#/prova/518')
    expect(provaOf('https://supervisor.example.com/?demo#/prova/518')).toBe(518)
    expect(provaOf('#/prova/518')).toBe(518)
    expect(provaOf('https://supervisor.example.com/#/monitor/prezzi')).toBe(null)
    expect(provaOf('#/prova/x')).toBe(null)
  })
  it('la lettura di una prova per numero', () => {
    const f = readProva(true, '{"id": 518, "task": "app-x", "short": "X", "title": null, "session": "s", "project": "p", "build": "b", "dove": "home", "fai": "tocca", "atteso": "vede", "state": "ko", "text": "non va", "at": 100}')
    expect(f.kind === 'found' && f.prova.state).toBe('ko')
    expect(readProva(true, '{"at": 1, "n": 2, "builds": 1, "groups": [], "ko": [], "red": []}')).toEqual({ kind: 'unsupported' })
    expect(readProva(false, 'prova 999: non esiste')).toEqual({ kind: 'refused', message: 'prova 999: non esiste' })
  })
})
