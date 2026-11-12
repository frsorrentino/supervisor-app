import { describe, expect, it } from 'vitest'
import { applyAnswered, live, markAnswer, settle } from './answered'
import { blankState, type CmdResult, type Session, type State } from './contract'
import { waitingRows } from './kpi'

// app-web-risposta-toglie-attesa-12-11: l'ok scritto in chat toglie subito la voce da «Ti aspettano».
const ses = (name: string, x: Partial<Session> = {}): Session => ({ id: name, name, account: 'a', project: name, state: 'waiting', since: 100, ...x })
const at = (ts: number, sessions: Session[], attention: State['attention'] = null): State => ({ ...blankState(ts), sessions, attention })
const res = (ok: boolean, a: number): CmdResult => ({ id: 'c', ok, text: ok ? 'sent' : 'no', at: a })
const q = { id: 'q1', kind: 'ask' as const, text: 'Procedo?', options: [], tier: 'low' as const, asked_at: 90 }
const ny = { reason: 'ok procedi con GTM4WP', at: 95 }

describe('risposta che toglie l\'attesa', () => {
  it('la ferma risposta in chat sparisce subito e la sessione passa al lavoro', () => {
    const st = at(200, [ses('caffe', { needs_you: ny })], [{ kind: 'needs_you', session: 'caffe', title: ny.reason, since: 95 }])
    const m = { caffe: markAnswer(st, 'caffe')! }
    const v = applyAnswered(st, m)
    expect(waitingRows(v)).toEqual([])
    expect(v.sessions[0].state).toBe('busy')
    expect(v.attention).toEqual([])
  })

  it('anche la domanda risposta col tasto', () => {
    const st = at(200, [ses('s', { question: q })])
    const v = applyAnswered(st, { s: markAnswer(st, 's')! })
    expect(v.sessions[0].question).toBeNull()
    expect(waitingRows(v)).toEqual([])
  })

  it('una sessione che non aspettava non ha marca', () => {
    expect(markAnswer(at(200, [ses('s', { state: 'idle' })]), 's')).toBeNull()
  })

  it('uno stato partito prima dell\'esito del PC non la fa tornare', () => {
    const st = at(200, [ses('s', { question: q })])
    let m = { s: markAnswer(st, 's')! }
    m = settle(m, 's', res(true, 210))
    const old = at(205, [ses('s', { question: q })])
    expect(live(m, old)).toEqual(m)
    expect(waitingRows(applyAnswered(old, live(m, old)))).toEqual([])
  })

  it('torna solo se lo stato dopo la dice ancora aperta', () => {
    let m = { s: markAnswer(at(200, [ses('s', { question: q })]), 's')! }
    m = settle(m, 's', res(true, 210))
    const after = at(215, [ses('s', { question: q })])
    const l = live(m, after)
    expect(l).toEqual({})
    expect(waitingRows(applyAnswered(after, l)).map(r => r.session)).toEqual(['s'])
  })

  it('se il PC rifiuta o non risponde la voce torna', () => {
    const st = at(200, [ses('s', { question: q })])
    const m = { s: markAnswer(st, 's')! }
    expect(settle(m, 's', res(false, 210))).toEqual({})
    expect(settle(m, 's', null)).toEqual({})
  })

  it('una domanda nuova arrivata dopo si vede subito', () => {
    const st = at(200, [ses('s', { question: q })])
    const m = { s: markAnswer(st, 's')! }
    const nuova = at(205, [ses('s', { question: { ...q, id: 'q2', asked_at: 204 } })], [{ kind: 'question', session: 's', title: 'altra', since: 204 }])
    const v = applyAnswered(nuova, m)
    expect(v.sessions[0].question?.id).toBe('q2')
    expect(v.attention?.length).toBe(1)
  })

  it('le altre sessioni restano come sono', () => {
    const st = at(200, [ses('s', { question: q }), ses('t', { needs_you: ny })])
    const v = applyAnswered(st, { s: markAnswer(st, 's')! })
    expect(waitingRows(v).map(r => r.session)).toEqual(['t'])
  })
})
