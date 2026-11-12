import { describe, expect, it } from 'vitest'
import { FileQueue, TooSlow } from './fileQueue'

// app-web-anteprime-in-fila-12-11: venti anteprime chieste insieme diventavano una, perché il relay tiene solo l'ultima
// lettura uguale della stessa sessione. In fila arrivano tutte.
const deferred = () => { let res!: (v: string) => void; const p = new Promise<string>(r => (res = r)); return { p, res } }
const tick = () => new Promise(r => setTimeout(r, 0))

describe('FileQueue', () => {
  it('una lettura alla volta per sessione, e tutte arrivano', async () => {
    const q = new FileQueue()
    let inFlight = 0, most = 0
    const job = (x: string) => async () => { inFlight++; most = Math.max(most, inFlight); await tick(); inFlight--; return x }
    const all = await Promise.all(['a', 'b', 'c', 'd'].map(x => q.run('s', job(x))))
    expect(all).toEqual(['a', 'b', 'c', 'd'])
    expect(most).toBe(1)
    expect(q.size('s')).toBe(0)
  })

  it('sessioni diverse non si aspettano', async () => {
    const q = new FileQueue()
    const a = deferred(), log: string[] = []
    void q.run('s', () => a.p.then(v => { log.push(v); return v }))
    await q.run('t', async () => { log.push('t'); return 't' })
    expect(log).toEqual(['t'])
    a.res('s'); await tick(); await tick()
    expect(log).toEqual(['t', 's'])
  })

  it('fra le anteprime va prima l\'ultima chiesta; un tocco passa davanti', async () => {
    const q = new FileQueue()
    const first = deferred(), order: string[] = []
    const job = (x: string) => async () => { order.push(x); return x }
    const p0 = q.run('s', () => first.p.then(v => { order.push(v); return v }))
    const ps = [q.run('s', job('img1')), q.run('s', job('img2')), q.run('s', job('img3')), q.run('s', job('apri'), true)]
    first.res('img0')
    await Promise.all([p0, ...ps])
    expect(order).toEqual(['img0', 'apri', 'img3', 'img2', 'img1'])
  })

  it('un\'anteprima che il PC non dà entro il limite libera la fila; la sua risposta tardiva non conta', async () => {
    const q = new FileQueue()
    const late = deferred()
    const slow = q.run('s', () => late.p, false, 20)
    const next = q.run('s', async () => 'dopo')
    await expect(slow).rejects.toBeInstanceOf(TooSlow)
    expect(await next).toBe('dopo')
    late.res('tardi')
    await tick()
    expect(q.size('s')).toBe(0)
  })

  it('un tocco senza limite aspetta il PC quanto serve', async () => {
    const q = new FileQueue()
    const d = deferred()
    const tap = q.run('s', () => d.p, true)
    await new Promise(r => setTimeout(r, 30))
    d.res('file')
    expect(await tap).toBe('file')
  })

  it('un errore non ferma la fila', async () => {
    const q = new FileQueue()
    const bad = q.run('s', async () => { throw new Error('rete') })
    const good = q.run('s', async () => 'ok')
    await expect(bad).rejects.toThrow('rete')
    expect(await good).toBe('ok')
  })
})
