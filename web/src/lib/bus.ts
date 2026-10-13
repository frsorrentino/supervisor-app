// Il bus remoto: Firebase Realtime Database via REST (`<path>.json?auth=<token>`) e stream SSE, come Rtdb.kt del core
// Android, senza SDK. Il token è quello dell'accesso anonimo di Firebase Auth (REST di Identity Toolkit), come sul telefono.

export type SseEvent = { event: string; data: string }

export interface Bus {
  /** Il nodo già letto come JSON, o null se non c'è. */
  get(path: string, query?: Record<string, string>): Promise<unknown>
  put(path: string, value: unknown): Promise<void>
  del(path: string): Promise<void>
  /** Gli eventi SSE del nodo (`put`, `patch`, `keep-alive`, `cancel`, `auth_revoked`); `down` alla caduta. */
  stream(path: string, query: Record<string, string>, on: (e: SseEvent) => void, down: (why: string) => void): () => void
}

export class BusError extends Error {
  constructor(readonly status: number, what: string) { super(`${what}: HTTP ${status}`) }
}

/** Il bus in memoria dei test: un nodo per percorso esatto, `onPut` per far rispondere il «PC». */
export class MemoryBus implements Bus {
  onPut: (path: string, value: unknown) => void = () => {}
  readonly deleted: string[] = []
  constructor(readonly data: Record<string, unknown>) {}
  async get(path: string) { return this.data[path] ?? null }
  async put(path: string, value: unknown) { this.data[path] = value; this.onPut(path, value) }
  async del(path: string) {
    this.deleted.push(path)
    for (const k of Object.keys(this.data)) if (k === path || k.startsWith(path + '/')) delete this.data[k]
  }
  stream(): () => void { return () => {} }
}

/** Le REST brevi entro 10 s, i pezzi dei file entro 120 s, come Rtdb.kt; lo stream è morto dopo 90 s di silenzio. */
const SHORT_MS = 10_000
const SLOW_MS = 120_000
const SILENCE_MS = 90_000

export class RestBus implements Bus {
  constructor(
    private readonly base: string,
    private readonly token: () => Promise<string>,
    private readonly http: typeof fetch = (...a) => fetch(...a),
    private readonly Source: typeof EventSource = globalThis.EventSource,
  ) {}

  private async url(path: string, query: Record<string, string> = {}) {
    const q = new URLSearchParams({ ...query, auth: await this.token() })
    return `${this.base}/${path}.json?${q}`
  }

  private slow = (path: string) => path.includes('/parts/') || path.startsWith('share/') || path.startsWith('file/')

  private async call(method: string, path: string, body?: unknown, query?: Record<string, string>) {
    const r = await this.http(await this.url(path, query), {
      method, body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(this.slow(path) ? SLOW_MS : SHORT_MS),
    })
    if (!r.ok) throw new BusError(r.status, `${method} ${path}`)
    return r
  }

  async get(path: string, query?: Record<string, string>) { return (await this.call('GET', path, undefined, query)).json() }
  async put(path: string, value: unknown) { await this.call('PUT', path, value) }
  async del(path: string) { await this.call('DELETE', path) }

  stream(path: string, query: Record<string, string>, on: (e: SseEvent) => void, down: (why: string) => void): () => void {
    let es: EventSource | null = null
    let closed = false
    let silence: ReturnType<typeof setTimeout> | undefined
    const stop = (why: string | null) => {
      if (closed) return
      closed = true
      clearTimeout(silence)
      es?.close()
      if (why) down(why)
    }
    const alive = () => { clearTimeout(silence); silence = setTimeout(() => stop('silence'), SILENCE_MS) }
    this.url(path, query).then(u => {
      if (closed) return
      es = new this.Source(u)
      alive()
      for (const name of ['put', 'patch', 'keep-alive', 'cancel', 'auth_revoked']) {
        es.addEventListener(name, (m: MessageEvent) => { alive(); on({ event: name, data: String(m.data) }) })
      }
      // EventSource riprova da sé con lo stesso URL, cioè con un token che scade: meglio chiudere e riaprire da fuori.
      es.onerror = () => stop('error')
    }, () => stop('auth'))
    return () => stop(null)
  }
}

/** L'accesso anonimo di Firebase Auth via REST: un uid per browser, il token si rinnova prima dell'ora. */
export type Session = { uid: string; refresh: string; idToken: string; expires: number }

export class AnonAuth {
  constructor(
    private readonly apiKey: string,
    private session: Session | null,
    private readonly saved: (s: Session) => void,
    private readonly http: typeof fetch = (...a) => fetch(...a),
    private readonly now: () => number = Date.now,
  ) {}

  get uid() { return this.session?.uid ?? null }

  async token(): Promise<string> {
    const s = this.session
    if (s && s.idToken && this.now() < s.expires - 60_000) return s.idToken
    this.session = s ? await this.refresh(s) : await this.signUp()
    this.saved(this.session)
    return this.session.idToken
  }

  private async signUp(): Promise<Session> {
    const r = await this.http(`https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${encodeURIComponent(this.apiKey)}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ returnSecureToken: true }),
    })
    if (!r.ok) throw new BusError(r.status, 'signUp')
    const j = await r.json()
    return { uid: j.localId, refresh: j.refreshToken, idToken: j.idToken, expires: this.now() + Number(j.expiresIn) * 1000 }
  }

  private async refresh(s: Session): Promise<Session> {
    const r = await this.http(`https://securetoken.googleapis.com/v1/token?key=${encodeURIComponent(this.apiKey)}`, {
      method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ grant_type: 'refresh_token', refresh_token: s.refresh }).toString(),
    })
    if (!r.ok) throw new BusError(r.status, 'refresh')
    const j = await r.json()
    return { uid: j.user_id, refresh: j.refresh_token, idToken: j.id_token, expires: this.now() + Number(j.expires_in) * 1000 }
  }
}
