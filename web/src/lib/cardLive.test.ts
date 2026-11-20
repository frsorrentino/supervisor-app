import { describe, expect, it } from 'vitest'
import type { ProvaOne, WaitItem } from './contract'
import { closedProve, openProve, provaAlreadyDone, provaDoneLine, provaView, staleProve, whenText } from './cardLive'

// Gli stessi casi di CardLiveTest.kt (app-carte-stato-vivo-20-11).
const p = (id: number, state: ProvaOne['state'] = 'todo', at = 1_000, text: string | null = null): ProvaOne =>
  ({ id, task: '', short: null, title: null, session: null, project: null, build: '', dove: null, fai: '', atteso: '', state, text, at })
const verify = { id: 'w1', label: 'verify', tests: [{ id: 1349, build: '', fai: '', atteso: '', state: 'todo' }, { id: 1347, build: '', fai: '', atteso: '', state: 'ok' }] } as unknown as WaitItem
const texts = { provaDoneOk: 'Confermata: va', provaDoneKo: 'Non va', provaStateRed: 'Confermata, controllo rosso', provaNotOpen: 'Non è più da fare' }

describe('carte delle prove: stato vivo', () => {
  it('le aperte vengono dalle verifiche aperte', () => {
    expect(openProve([verify, { ...verify, id: 'w2', state: 'closed', tests: [{ id: 1348, build: '', fai: '', atteso: '', state: 'todo' }] }])).toEqual(new Set([1349]))
    expect(openProve(null)).toBeNull()
    // Le voci del relay e della demo non hanno `tests`: il numero sta nel comando dei tasti.
    expect(openProve([{ id: 'w3', label: 'verify', answer: { op: 'prova', arg: '1412,1413' }, actions: [{ label: 'Va', style: 'primary', cmd: { op: 'prova', arg: '1' } }] } as unknown as WaitItem])).toEqual(new Set([1, 1412, 1413]))
  })
  it('una prova aperta ha il suo tasto', () => expect(provaView(p(1349), new Set()).kind).toBe('open'))
  // Lo screenshot delle 20:37: 1347 confermata altrove; la carta dice l'esito, senza tasto.
  it("decide l'esito letto, non l'assenza", () => {
    expect(provaView(p(1347, 'ok'), new Set([1347]))).toEqual({ kind: 'ok', at: 1_000, text: null })
    expect(provaView(p(1347, 'ko', 1_000, 'il totale va a capo'), new Set()).kind).toBe('ko')
    expect(provaView(p(1347, 'red'), new Set()).kind).toBe('red')
  })
  it('fuori dalle aperte ma ancora da fare non è confermata', () => {
    const closed = closedProve(new Set([1348, 1349]), new Set([1349]))
    expect(closed).toEqual(new Set([1348]))
    expect(provaView(p(1348), closed).kind).toBe('not_open')
  })
  it('senza attese la carta resta come dice la prova', () => expect(provaView(p(1348), closedProve(new Set([1348]), null)).kind).toBe('open'))
  // Il caso della demo: una prova aperta che `attese` non ha mai nominato non si dice chiusa.
  it('una prova mai vista fra le aperte tiene il suo tasto', () => expect(provaView(p(1), closedProve(new Set([1349]), new Set([1349]))).kind).toBe('open'))
  it('si rileggono le carte che mostrano ancora da fare una prova non più aperta', () =>
    expect(staleProve({ 1347: p(1347), 1348: p(1348), 1349: p(1349), 1350: p(1350, 'ok'), 1351: null }, new Set([1347, 1348, 1350]))).toEqual([1347, 1348]))
  it('la riga della carta risolta, uguale al telefono', () => {
    const now = 1_000 + 3_600
    const hm = whenText(1_000, now)
    expect(provaDoneLine({ kind: 'ok', at: 1_000 }, now, texts)).toBe(`Confermata: va · ${hm}`)
    expect(provaDoneLine({ kind: 'ko', at: 1_000, text: 'il totale va a capo' }, now, texts)).toBe(`Non va · ${hm}: il totale va a capo`)
    expect(provaDoneLine({ kind: 'not_open' }, now, texts)).toBe('Non è più da fare')
    expect(provaDoneLine({ kind: 'open' }, now, texts)).toBeNull()
  })
  it("una prova già confermata non è un errore", () => {
    expect(provaAlreadyDone("ERR prova 1347: non c'è più o è già confermata")).toBe(true)
    expect(provaAlreadyDone('ERR prova 1347: build not installed')).toBe(false)
    expect(provaAlreadyDone('kb is no longer running')).toBe(false)
  })
})
