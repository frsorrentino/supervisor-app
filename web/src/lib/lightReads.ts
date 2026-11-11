// Letture leggere verso il PC (app-letture-leggere-11-11, contratto 1.135; Franz 11/11 14:38: «correggi tutto il possibile purché non pesi
// sulla qualità dell'app»). Il progetto Firebase del relay era sopra le soglie gratuite: circa 1.000 comandi l'ora dalla web, esiti da
// 13-39 KB. Questo trasporto avvolge l'altro:
// - ogni lettura riparte con l'etag dell'ultima risposta allo stesso comando; un relay 1.135 risponde «unchanged» (corta) e qui torna
//   la risposta che si aveva, così le schermate restano identiche. Un relay di prima ignora l'etag e manda tutto, come oggi;
// - due letture uguali partite insieme (una riga per colonna, due schede) diventano una sola;
// - con la scheda nascosta le letture aspettano che torni visibile e partono allora: al ritorno i dati sono freschi come prima, e
//   intanto il PC non lavora per nessuno. Le azioni non aspettano mai;
// - conta i comandi per op, per misurare (in demo e in sviluppo `globalThis.__cmStats`).
import type { Cmd, CmdResult } from './contract'
import type { Transport } from './transport'

/** Il comando senza ciò che cambia a ogni invio: due comandi con la stessa chiave chiedono la stessa cosa. */
export const readKey = (c: Cmd): string => { const { id: _i, issued: _t, uid: _u, etag: _e, ...rest } = c as Cmd & { uid?: string; etag?: string }; return JSON.stringify(rest, Object.keys(rest).sort()) }

export type Stats = { since: number; total: number; byOp: Record<string, number>; unchanged: number }

/** Le op di sola lettura: solo queste si uniscono e aspettano la scheda visibile. */
export const READ_OPS = new Set<string>(['screen', 'last', 'transcript', 'projects', 'timeline', 'agenda', 'changes', 'diff', 'night', 'nights', 'done',
  'plugins', 'kpi', 'results', 'decisioni', 'prove', 'monitors', 'thumb', 'attese', 'attesa', 'seguiti', 'prima_raccolta', 'conti'])

type Page = { hidden: () => boolean; onVisible: (f: () => void) => void }
const browserPage: Page = {
  hidden: () => typeof document !== 'undefined' && document.hidden,
  onVisible: (f) => { const h = () => { if (!document.hidden) { document.removeEventListener('visibilitychange', h); f() } }; document.addEventListener('visibilitychange', h) },
}

/** Quante risposte tenere per l'etag: le letture della web sono qualche decina (pagine, sessioni, monitoraggi). */
const KEEP = 80

export class LightTransport implements Transport {
  readonly stats: Stats = { since: Date.now(), total: 0, byOp: {}, unchanged: 0 }
  /** Le azioni in volo (non le letture): una ricarica per la versione nuova aspetta che finiscano (app-web-versione-nuova-11-11). */
  writing = 0
  private last = new Map<string, CmdResult & { etag: string }>()
  private flying = new Map<string, Promise<CmdResult>>()
  constructor(private inner: Transport, private page: Page = browserPage) {
    this.fetchTail = inner.fetchTail ? (id) => inner.fetchTail!(id) : undefined
  }
  fetchState() { return this.inner.fetchState() }
  subscribe(onState: Parameters<Transport['subscribe']>[0], onDown?: () => void) { return this.inner.subscribe(onState, onDown) }
  fetchEvents(since?: number) { return this.inner.fetchEvents(since) }
  share(id: string, mime: string, data: Uint8Array, name?: string | null) { return this.inner.share(id, mime, data, name) }
  fetchFile(id: string) { return this.inner.fetchFile(id) }
  fetchTail?: Transport['fetchTail']

  async send(cmd: Cmd): Promise<CmdResult> {
    if (!READ_OPS.has(cmd.op)) { this.writing++; try { return await this.fetch(cmd) } finally { this.writing-- } }
    const key = readKey(cmd)
    const same = this.flying.get(key)
    if (same) return same.then(r => ({ ...r, id: cmd.id }))
    const p = (async () => {
      if (this.page.hidden()) await new Promise<void>(ok => this.page.onVisible(ok))
      return this.fetch(cmd, key)
    })().finally(() => this.flying.delete(key))
    this.flying.set(key, p)
    return p
  }

  private async fetch(cmd: Cmd, key = readKey(cmd)): Promise<CmdResult> {
    this.stats.total++
    this.stats.byOp[cmd.op] = (this.stats.byOp[cmd.op] ?? 0) + 1
    const had = this.last.get(key)
    const r = await this.inner.send(had ? { ...cmd, etag: had.etag } : cmd)
    if (r.unchanged && had) { this.stats.unchanged++; this.last.delete(key); this.last.set(key, had); return { ...had, id: r.id, at: r.at || had.at } }
    if (r.ok && r.etag) {
      this.last.delete(key); this.last.set(key, r as CmdResult & { etag: string })
      if (this.last.size > KEEP) this.last.delete(this.last.keys().next().value!)
    } else this.last.delete(key)
    return r
  }
}
