import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { MemoryBus, type SseEvent } from './bus'
import { fromHex, openBlob, sealBlob } from './crypto'
import { RemoteTransport } from './remote'
import { TransportError } from './transport'
import type { Cmd } from './contract'

const raw = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')
const fixture = (n: string) => JSON.parse(raw(n))
const KEY = fromHex(fixture('file-parts.json').key)
const env = async (plain: string) => JSON.parse(await sealBlob(plain, KEY))

/** Il bus in memoria con gli stream: il test fa arrivare gli eventi SSE a mano. */
class StreamBus extends MemoryBus {
  streams: { path: string; query: Record<string, string>; on: (e: SseEvent) => void; down: (w: string) => void; open: boolean }[] = []
  stream(path: string, query: Record<string, string>, on: (e: SseEvent) => void, down: (w: string) => void) {
    const s = { path, query, on, down, open: true }
    this.streams.push(s)
    return () => { s.open = false }
  }
}

const transport = (bus: MemoryBus, o: Partial<ConstructorParameters<typeof RemoteTransport>[1]> = {}) =>
  new RemoteTransport(bus, { key: KEY, uid: 'webUid', pollMs: 1, ...o })

describe('strada remota: stato', () => {
  it('fetchState apre la busta di /state e segna /seen con l\'ora del server', async () => {
    const bus = new MemoryBus({ state: await env(raw('state-2-idle.json')) })
    const s = await transport(bus).fetchState()
    expect(s.host).toBe(fixture('state-2-idle.json').host)
    await new Promise(r => setTimeout(r, 0))
    expect(bus.data['seen/webUid']).toEqual({ '.sv': 'timestamp' })
  })

  it('subscribe: un put alla radice è uno stato; auth_revoked riapre lo stream', async () => {
    const bus = new StreamBus({})
    const got: string[] = []
    const stop = transport(bus, { backoffMs: [1] }).subscribe(s => got.push(s.host), () => got.push('down'))
    expect(bus.streams[0].path).toBe('state')
    bus.streams[0].on({ event: 'put', data: JSON.stringify({ path: '/', data: await env(raw('state-1-question.json')) }) })
    await new Promise(r => setTimeout(r, 5))
    expect(got).toEqual([fixture('state-1-question.json').host])
    bus.streams[0].on({ event: 'auth_revoked', data: 'credential is no longer valid' })
    await new Promise(r => setTimeout(r, 10))
    expect(bus.streams[0].open).toBe(false)
    expect(bus.streams.length).toBe(2)
    expect(got).toContain('down')
    stop()
    expect(bus.streams[1].open).toBe(false)
  })
})

describe('strada remota: eventi', () => {
  it('gli ultimi 200 dal bus, solo dopo since, dal più recente; una busta rotta si salta', async () => {
    const sample = fixture('events-sample.json') as { key: string; ts: number }[]
    const nodes: Record<string, unknown> = { broken: { v: 1, enc: 'AAAA' } }
    for (const e of sample) nodes[e.key] = await env(JSON.stringify(e))
    const bus = new MemoryBus({ events: nodes })
    const since = sample[Math.floor(sample.length / 2)].ts
    const got = await transport(bus).fetchEvents(since)
    expect(got.length).toBe(sample.filter(e => e.ts > since).length)
    expect(got.every(e => e.ts > since)).toBe(true)
    expect(got[0].ts).toBeGreaterThanOrEqual(got.at(-1)!.ts)
  })
})

describe('strada remota: comandi', () => {
  const cmd: Cmd = { id: 'c1', op: 'prompt', session: 'ledger-api', arg: 'ciao', issued: 1, by: 'web', device: 'web' }

  it('scrive /cmd/<id> cifrato e legge /result/<id>', async () => {
    const bus = new MemoryBus({})
    bus.onPut = async (path, v) => {
      if (path !== 'cmd/c1') return
      expect(JSON.parse(await openBlob(JSON.stringify(v), KEY))).toEqual(cmd)
      bus.data['result/c1'] = await env(JSON.stringify({ id: 'c1', ok: true, text: 'sent', at: 2 }))
    }
    expect(await transport(bus).send(cmd)).toMatchObject({ id: 'c1', ok: true, text: 'sent' })
  })

  it('senza risultato entro il tempo: errore timeout', async () => {
    const t = transport(new MemoryBus({}), { timeoutMs: () => 15 })
    await expect(t.send(cmd)).rejects.toEqual(new TransportError(0, 'timeout'))
  })
})

describe('strada remota: file', () => {
  it('file a pezzi (1.34, file-parts.json): li legge in ordine, controlla sha256 e cancella /file/<id>', async () => {
    const f = fixture('file-parts.json')
    const bus = new MemoryBus({ 'file/x/meta': f.meta, 'file/x/parts/0': f.parts[0], 'file/x/parts/1': f.parts[1] })
    const got = await transport(bus).fetchFile('x')
    expect(new TextDecoder().decode(got!.data)).toBe(f.file)
    expect(got!.mime).toBe('text/plain')
    expect(got!.name).toBe('hello.txt')
    expect(bus.deleted).toContain('file/x')
  })

  it('un pezzo scambiato: errore, e /file/<id> si cancella lo stesso', async () => {
    const f = fixture('file-parts.json')
    const bus = new MemoryBus({ 'file/x/meta': f.meta, 'file/x/parts/0': f.parts[1], 'file/x/parts/1': f.parts[0] })
    await expect(transport(bus).fetchFile('x')).rejects.toBeInstanceOf(TransportError)
    expect(bus.deleted).toContain('file/x')
  })

  it('il nodo unico di prima, {mime, data}', async () => {
    const bus = new MemoryBus({ 'file/y': await env(JSON.stringify({ mime: 'image/png', data: btoa('png!') })) })
    const got = await transport(bus).fetchFile('y')
    expect(new TextDecoder().decode(got!.data)).toBe('png!')
    expect(got!.mime).toBe('image/png')
  })

  it('nessun file: null', async () => {
    expect(await transport(new MemoryBus({})).fetchFile('z')).toBeNull()
  })

  it('condivisione a pezzi (1.48, share-parts.json): pezzi e per ultimo il manifesto', async () => {
    const f = fixture('share-parts.json')
    const bus = new MemoryBus({})
    const order: string[] = []
    bus.onPut = path => order.push(path)
    const t = transport(bus, { partBytes: f.part_bytes })
    t.limits = { max_bytes: 100, parts: true, max_parts_bytes: 52428800 }
    await t.share('s1', f.meta_plain.mime, new TextEncoder().encode(f.file), f.meta_plain.name)
    expect(order).toEqual(['share/s1/parts/0', 'share/s1/parts/1', 'share/s1/parts/2', 'share/s1/meta'])
    expect(JSON.parse(await openBlob(JSON.stringify(bus.data['share/s1/meta']), KEY))).toEqual(f.meta_plain)
    for (let i = 0; i < 3; i++) {
      expect(await openBlob(JSON.stringify(bus.data[`share/s1/parts/${i}`]), KEY)).toBe(await openBlob(JSON.stringify(f.parts[i]), KEY))
    }
  })

  it('senza i pezzi: il nodo unico, e oltre il limite niente si scrive', async () => {
    const bus = new MemoryBus({})
    const t = transport(bus)
    t.limits = { max_bytes: 200 }
    await t.share('s2', 'text/plain', new TextEncoder().encode('ciao'), 'a.txt')
    expect(JSON.parse(await openBlob(JSON.stringify(bus.data['share/s2']), KEY))).toEqual({ mime: 'text/plain', data: btoa('ciao'), name: 'a.txt' })
    await expect(t.share('s3', 'text/plain', new Uint8Array(500), null)).rejects.toBeInstanceOf(TransportError)
    expect(bus.data['share/s3']).toBeUndefined()
  })
})
