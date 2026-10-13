import { describe, expect, it } from 'vitest'
import { AnonAuth, BusError, RestBus, type Session } from './bus'

type Call = { url: string; method: string; body: string | null; type: string | null }
function http(reply: (c: Call) => Response, calls: Call[] = []) {
  return (async (url: string | URL | Request, init: RequestInit = {}) => {
    const c = { url: String(url), method: init.method ?? 'GET', body: init.body == null ? null : String(init.body), type: new Headers(init.headers).get('Content-Type') }
    calls.push(c)
    return reply(c)
  }) as typeof fetch
}
const json = (v: unknown, status = 200) => new Response(JSON.stringify(v), { status, headers: { 'Content-Type': 'application/json' } })

describe('accesso anonimo di Firebase Auth via REST', () => {
  it('la prima volta signUp, poi il token salvato finché non scade, poi il rinnovo', async () => {
    const calls: Call[] = []
    let now = 1_000_000
    const saved: Session[] = []
    const auth = new AnonAuth('KEY', null, s => saved.push(s), http(c => c.url.includes('signUp')
      ? json({ localId: 'uid1', idToken: 'tok1', refreshToken: 'ref1', expiresIn: '3600' })
      : json({ user_id: 'uid1', id_token: 'tok2', refresh_token: 'ref2', expires_in: '3600' }), calls), () => now)
    expect(await auth.token()).toBe('tok1')
    expect(auth.uid).toBe('uid1')
    expect(calls[0].url).toBe('https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=KEY')
    expect(JSON.parse(calls[0].body!)).toEqual({ returnSecureToken: true })
    expect(await auth.token()).toBe('tok1')
    expect(calls.length).toBe(1)
    now += 3600_000 - 30_000
    expect(await auth.token()).toBe('tok2')
    expect(calls[1].url).toBe('https://securetoken.googleapis.com/v1/token?key=KEY')
    expect(calls[1].body).toBe('grant_type=refresh_token&refresh_token=ref1')
    expect(saved.map(s => s.refresh)).toEqual(['ref1', 'ref2'])
  })

  it('una sessione salvata si rinnova senza un uid nuovo', async () => {
    const calls: Call[] = []
    const auth = new AnonAuth('KEY', { uid: 'old', refresh: 'r', idToken: '', expires: 0 }, () => {},
      http(() => json({ user_id: 'old', id_token: 't', refresh_token: 'r2', expires_in: '3600' }), calls))
    expect(await auth.token()).toBe('t')
    expect(calls.length).toBe(1)
    expect(calls[0].url).toContain('securetoken')
  })
})

describe('RTDB via REST', () => {
  const bus = (calls: Call[], reply: (c: Call) => Response = () => json(null)) =>
    new RestBus('https://db.example', async () => 'TOK', http(reply, calls))

  it('GET con la query e il token; null se il nodo non c\'è', async () => {
    const calls: Call[] = []
    expect(await bus(calls).get('events', { orderBy: '"$key"', limitToLast: '200' })).toBeNull()
    const u = new URL(calls[0].url)
    expect(u.origin + u.pathname).toBe('https://db.example/events.json')
    expect(u.searchParams.get('orderBy')).toBe('"$key"')
    expect(u.searchParams.get('limitToLast')).toBe('200')
    expect(u.searchParams.get('auth')).toBe('TOK')
  })

  it('PUT del JSON e DELETE; un rifiuto diventa BusError con lo stato', async () => {
    const calls: Call[] = []
    const b = bus(calls, c => c.method === 'DELETE' ? json({ error: 'Permission denied' }, 401) : json({}))
    await b.put('seen/u', { '.sv': 'timestamp' })
    expect(calls[0].method).toBe('PUT')
    expect(JSON.parse(calls[0].body!)).toEqual({ '.sv': 'timestamp' })
    await expect(b.del('file/x')).rejects.toEqual(new BusError(401, 'DELETE file/x'))
  })

  it('lo stream apre l\'EventSource col token e chiude alla caduta, una volta', async () => {
    const made: FakeSource[] = []
    class FakeSource {
      onerror: (() => void) | null = null
      listeners: Record<string, (m: MessageEvent) => void> = {}
      closed = false
      constructor(readonly url: string) { made.push(this) }
      addEventListener(n: string, f: (m: MessageEvent) => void) { this.listeners[n] = f }
      close() { this.closed = true }
    }
    const b = new RestBus('https://db.example', async () => 'TOK', fetch, FakeSource as unknown as typeof EventSource)
    const got: string[] = []
    const downs: string[] = []
    b.stream('state', {}, e => got.push(e.event), w => downs.push(w))
    await new Promise(r => setTimeout(r, 0))
    expect(made[0].url).toBe('https://db.example/state.json?auth=TOK')
    made[0].listeners.put({ data: '{"path":"/","data":null}' } as MessageEvent)
    made[0].onerror!()
    made[0].onerror!()
    expect(got).toEqual(['put'])
    expect(downs).toEqual(['error'])
    expect(made[0].closed).toBe(true)
  })
})
