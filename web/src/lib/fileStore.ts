import type { Fetched } from './fileActions'

// I file della chat già scaricati, per la durata della chat (Franz, 16/10 15:59: «il condividi da pwa non funziona, dice
// sempre file pronto tocca di nuovo»). Condividi e copia vogliono l'attivazione del tocco ancora viva: su iOS Safari dura
// pochissimo e un await di rete la consuma. Col file già pronto `tap` fa l'azione nello stesso giro del tocco, senza
// nessun await prima; il file lo prepara l'anteprima o l'apertura della card, così basta il primo tocco.
// Le chiavi sono quelle di `fileKey`: cominciano col nome della sessione.
export class FileStore {
  private pending = new Map<string, Promise<Fetched | null>>()
  private ready = new Map<string, Fetched>()

  /** Il file già pronto, senza aspettare. */
  peek(key: string): Fetched | undefined {
    return this.ready.get(key)
  }

  /** Il file: pronto, in arrivo o da chiedere con `load`. Un fallimento non resta: il tocco dopo riprova. */
  get(key: string, load: () => Promise<Fetched | null>): Promise<Fetched | null> {
    const r = this.ready.get(key)
    if (r) return Promise.resolve(r)
    let p = this.pending.get(key)
    if (!p) {
      p = load().catch(() => null)
      this.pending.set(key, p)
      p.then(v => {
        this.pending.delete(key)
        if (v) this.ready.set(key, v)
      })
    }
    return p
  }

  /** L'azione del tocco: col file pronto parte subito, nello stesso giro; altrimenti dopo il file (e il tocco può essere scaduto). */
  tap<T>(key: string, load: () => Promise<Fetched | null>, run: (f: Fetched) => T): T | Promise<T | null> {
    const r = this.ready.get(key)
    if (r) return run(r)
    return this.get(key, load).then(f => (f ? run(f) : null))
  }

  /** Tiene solo i file delle chat a schermo. */
  keep(sessions: Iterable<string>) {
    const live = new Set(sessions)
    for (const k of this.ready.keys()) if (!live.has(k.split('\n')[0])) this.ready.delete(k)
  }
}
