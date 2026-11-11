import { describe, expect, it } from 'vitest'
import { LightTransport, readKey } from './lightReads'
import type { Cmd, CmdResult } from './contract'
import type { Transport } from './transport'

// Letture leggere (app-letture-leggere-11-11, contratto 1.135): etag, «unchanged», letture uguali unite, scheda nascosta.
type Reply = (c: Cmd) => CmdResult | Promise<CmdResult>
const inner = (reply: Reply) => {
  const sent: Cmd[] = []
  const t = { sent, send: async (c: Cmd) => { sent.push(c); return reply(c) } } as unknown as Transport & { sent: Cmd[] }
  return t
}
const visible = { hidden: () => false, onVisible: () => {} }
let n = 0
const read = (op: Cmd['op'] = 'decisioni', arg: string | null = null): Cmd => ({ id: `c${++n}`, op, session: null, arg, issued: n, by: 'web' })

describe('letture leggere', () => {
  it('la seconda lettura uguale porta l\'etag; «unchanged» rende la risposta che si aveva', async () => {
    const tr = inner(c => c.etag ? { id: c.id, ok: true, text: '', at: 2, etag: 'aaaa', unchanged: true } : { id: c.id, ok: true, text: '{"groups":[]}', at: 1, etag: 'aaaa' })
    const lt = new LightTransport(tr, visible)
    const a = await lt.send(read())
    const b2 = read()
    const b = await lt.send(b2)
    expect(tr.sent[0].etag).toBeUndefined()
    expect(tr.sent[1].etag).toBe('aaaa')
    expect(b).toMatchObject({ id: b2.id, ok: true, text: a.text })
    expect(lt.stats.unchanged).toBe(1)
  })

  it('un relay di prima ignora l\'etag: ogni risposta arriva intera e si usa così', async () => {
    let k = 0
    const tr = inner(c => ({ id: c.id, ok: true, text: `pagina ${++k}`, at: k }))
    const lt = new LightTransport(tr, visible)
    expect((await lt.send(read())).text).toBe('pagina 1')
    expect((await lt.send(read())).text).toBe('pagina 2')
    expect(tr.sent.every(c => c.etag === undefined)).toBe(true)
  })

  it('l\'etag vale solo per la stessa lettura: un altro arg parte senza', async () => {
    const tr = inner(c => ({ id: c.id, ok: true, text: 'x', at: 1, etag: `e-${c.arg}` }))
    const lt = new LightTransport(tr, visible)
    await lt.send(read('prove', '518'))
    await lt.send(read('prove', '527'))
    await lt.send(read('prove', '518'))
    expect(tr.sent.map(c => c.etag)).toEqual([undefined, undefined, 'e-518'])
    expect(readKey(read('prove', '518'))).toBe(readKey(read('prove', '518')))
  })

  it('una risposta di errore toglie l\'etag: la lettura dopo chiede tutto', async () => {
    let fail = false
    const tr = inner(c => fail ? { id: c.id, ok: false, text: 'no', at: 1 } : { id: c.id, ok: true, text: 'x', at: 1, etag: 'e1' })
    const lt = new LightTransport(tr, visible)
    await lt.send(read()); fail = true; await lt.send(read()); fail = false; await lt.send(read())
    expect(tr.sent.map(c => c.etag)).toEqual([undefined, 'e1', undefined])
  })

  it('due letture uguali insieme sono una sola; le azioni partono sempre', async () => {
    const tr = inner(c => new Promise(ok => setTimeout(() => ok({ id: c.id, ok: true, text: 'r', at: 1 }), 5)))
    const lt = new LightTransport(tr, visible)
    const [x, y] = await Promise.all([lt.send(read('results')), lt.send(read('results'))])
    expect(tr.sent.length).toBe(1)
    expect(x.id).not.toBe(y.id)
    await Promise.all([lt.send({ ...read('prompt', 'ciao') }), lt.send({ ...read('prompt', 'ciao') })])
    expect(tr.sent.filter(c => c.op === 'prompt').length).toBe(2)
  })

  it('con la scheda nascosta una lettura aspetta che torni visibile; un\'azione no', async () => {
    let hidden = true, wake = () => {}
    const page = { hidden: () => hidden, onVisible: (f: () => void) => { wake = f } }
    const tr = inner(c => ({ id: c.id, ok: true, text: 'ok', at: 1 }))
    const lt = new LightTransport(tr, page)
    const r = lt.send(read('monitors'))
    await lt.send(read('prompt', 'vai'))
    await new Promise(ok => setTimeout(ok, 5))
    expect(tr.sent.map(c => c.op)).toEqual(['prompt'])
    hidden = false; wake()
    expect((await r).text).toBe('ok')
    expect(tr.sent.map(c => c.op)).toEqual(['prompt', 'monitors'])
  })
})
