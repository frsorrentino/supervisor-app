// La strada remota (fase 3 del piano web): lo stesso bus RTDB del telefono, con le buste AES-GCM della chiave del relay,
// come FirebaseTransport.kt del core Android. Dalla web app si usa quando la pagina non è quella servita dal relay.

import type { Bus, SseEvent } from './bus'
import { decodeState, type Cmd, type CmdResult, type Event, type State } from './contract'
import { fromB64, openBlob, openBlobBytes, sealBlob, sealBytes, toB64, toHex } from './crypto'
import { resultTimeoutMs, TransportError, type FileBlob, type Transport } from './transport'

type Bytes = Uint8Array<ArrayBuffer>
type Limits = NonNullable<State['share']>
type Opts = {
  key: Bytes
  uid: string
  pollMs?: number
  timeoutMs?: (op: Cmd['op']) => number
  backoffMs?: number[]
  /** 1 MiB come sul telefono (contratto 1.48: consigliato, non obbligatorio). */
  partBytes?: number
}

const PART_BYTES = 1_048_576
const MAX_PARTS = 50
const BACKOFF_MS = [1000, 2000, 5000, 15000, 30000]

const isBlob = (v: unknown): v is { v: unknown; enc: string } => !!v && typeof v === 'object' && 'enc' in v
const sha256 = async (b: Bytes) => toHex(new Uint8Array(await crypto.subtle.digest('SHA-256', b)))

export class RemoteTransport implements Transport {
  /** I limiti di `state.share` dell'ultimo stato letto: decidono pezzi o nodo unico. */
  limits: Limits | null = null
  private readonly o: Required<Opts>

  constructor(private readonly bus: Bus, o: Opts) {
    this.o = { pollMs: 1000, timeoutMs: resultTimeoutMs, backoffMs: BACKOFF_MS, partBytes: PART_BYTES, ...o }
  }

  private open = async (doc: unknown) => {
    try { return await openBlob(JSON.stringify(doc), this.o.key) } catch { throw new TransportError(0, 'cannot decrypt') }
  }
  private seal = async (plain: string) => JSON.parse(await sealBlob(plain, this.o.key))

  private decodeState(plain: string) {
    const s = decodeState(plain)
    this.limits = s.share ?? null
    return s
  }

  /** Contratto 1.20: «ho ricevuto», con l'ora del server; mai d'ostacolo alla lettura. */
  private markSeen() { this.bus.put(`seen/${this.o.uid}`, { '.sv': 'timestamp' }).catch(() => {}) }

  async fetchState(): Promise<State> {
    const doc = await this.bus.get('state')
    if (!doc) throw new TransportError(0, 'no state on the bus')
    const s = this.decodeState(await this.open(doc))
    this.markSeen()
    return s
  }

  /**
   * Lo stream di /state, riaperto con un token fresco dopo `auth_revoked`, `cancel`, un errore o il silenzio, con
   * l'attesa 1-2-5-15-30 s che riparte dal primo stato ricevuto.
   */
  subscribe(onState: (s: State) => void, onDown?: () => void): () => void {
    let stopped = false
    let attempt = 0
    let close = () => {}
    let timer: ReturnType<typeof setTimeout> | undefined
    let marked = false
    const reopen = () => {
      if (stopped) return
      close()
      onDown?.()
      const base = this.o.backoffMs[Math.min(attempt++, this.o.backoffMs.length - 1)]
      timer = setTimeout(start, base + base * (Math.random() - 0.5) * 0.4)
    }
    const on = (e: SseEvent) => {
      if (e.event === 'auth_revoked' || e.event === 'cancel') { reopen(); return }
      if (e.event !== 'put' && e.event !== 'patch') return
      let m: { path?: string; data?: unknown }
      try { m = JSON.parse(e.data) } catch { return }
      if (m.path !== '/' || !isBlob(m.data)) return
      this.open(m.data).then(plain => {
        if (stopped) return
        const s = this.decodeState(plain)
        attempt = 0
        if (!marked) { marked = true; this.markSeen() }
        onState(s)
      }, () => { /* uno stato che non si apre si salta */ })
    }
    const start = () => { if (!stopped) close = this.bus.stream('state', {}, on, reopen) }
    start()
    return () => { stopped = true; clearTimeout(timer); close() }
  }

  /** Gli ultimi 200 del bus, come il telefono; qui solo quelli dopo `since`, dal più recente. */
  async fetchEvents(since = 0): Promise<Event[]> {
    const all = await this.bus.get('events', { orderBy: '"$key"', limitToLast: '200' }) as Record<string, unknown> | null
    const out: Event[] = []
    for (const doc of Object.values(all ?? {})) {
      if (!isBlob(doc)) continue
      try {
        const e = JSON.parse(await this.open(doc)) as Event
        if (e.ts > since) out.push(e)
      } catch { /* una voce rotta si salta */ }
    }
    return out.sort((a, b) => b.ts - a.ts)
  }

  async send(cmd: Cmd): Promise<CmdResult> {
    await this.bus.put(`cmd/${cmd.id}`, await this.seal(JSON.stringify(cmd)))
    const deadline = Date.now() + this.o.timeoutMs(cmd.op)
    while (Date.now() <= deadline) {
      const doc = await this.bus.get(`result/${cmd.id}`).catch(() => null)
      if (doc) return JSON.parse(await this.open(doc)) as CmdResult
      await new Promise(r => setTimeout(r, this.o.pollMs))
    }
    throw new TransportError(0, 'timeout')
  }

  /** Contratto 1.48: con `parts` i pezzi da 1 MiB e per ultimo il manifesto; altrimenti il nodo unico {mime, data, name}. */
  async share(id: string, mime: string, data: Uint8Array, name?: string | null) {
    const bytes = data as Bytes
    const l = this.limits
    if (l?.parts && l.max_parts_bytes) {
      const n = Math.max(1, Math.ceil(bytes.length / this.o.partBytes))
      if (bytes.length > l.max_parts_bytes || n > MAX_PARTS) throw new TransportError(413, `too large: ${bytes.length}`)
      for (let i = 0; i < n; i++) {
        const part = bytes.slice(i * this.o.partBytes, (i + 1) * this.o.partBytes)
        await this.bus.put(`share/${id}/parts/${i}`, JSON.parse(await sealBytes(part, this.o.key)))
      }
      const meta = { n, size: bytes.length, sha256: await sha256(bytes), mime, name: name ?? null }
      await this.bus.put(`share/${id}/meta`, await this.seal(JSON.stringify(meta)))
      return
    }
    const plain: Record<string, string> = { mime, data: toB64(bytes) }
    if (name) plain.name = name
    const doc = await this.seal(JSON.stringify(plain))
    if (l?.max_bytes && doc.enc.length > l.max_bytes) throw new TransportError(413, `too large: ${doc.enc.length}`)
    await this.bus.put(`share/${id}`, doc)
  }

  /** Contratto 1.34: i pezzi in ordine con il controllo di misura e sha256, o il nodo unico; letto, si cancella. */
  async fetchFile(id: string): Promise<FileBlob | null> {
    const metaDoc = await this.bus.get(`file/${id}/meta`)
    if (metaDoc) {
      try {
        const meta = JSON.parse(await this.open(metaDoc)) as { n: number; size: number; sha256: string; mime: string; name?: string | null }
        const out = new Uint8Array(meta.size)
        let at = 0
        for (let k = 0; k < meta.n; k++) {
          const doc = await this.bus.get(`file/${id}/parts/${k}`)
          if (!doc) throw new TransportError(0, `file ${id}: part ${k} missing`)
          let part: Bytes
          try { part = await openBlobBytes(JSON.stringify(doc), this.o.key) } catch { throw new TransportError(0, 'cannot decrypt') }
          if (at + part.length > meta.size) throw new TransportError(0, `file ${id}: size mismatch`)
          out.set(part, at)
          at += part.length
        }
        if (at !== meta.size || await sha256(out) !== meta.sha256) throw new TransportError(0, `file ${id}: size or sha256 mismatch`)
        return { data: out, mime: meta.mime, name: meta.name ?? null }
      } finally { await this.bus.del(`file/${id}`).catch(() => {}) }
    }
    const doc = await this.bus.get(`file/${id}`)
    if (!doc) return null
    const plain = JSON.parse(await this.open(doc)) as { mime: string; data: string; name?: string }
    await this.bus.del(`file/${id}`).catch(() => {})
    return { data: fromB64(plain.data), mime: plain.mime, name: plain.name ?? null }
  }
}
