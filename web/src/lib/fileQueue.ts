// app-web-anteprime-in-fila-12-11 (Franz, 12/11: «molte immagini non si caricano»). Il relay, di più letture uguali della
// stessa sessione in coda, tiene solo l'ultima e chiude le altre come «superseded» (cm-relay.py, `same()` = op e sessione):
// venti anteprime chieste insieme diventavano una. Qui i file di una sessione si chiedono uno alla volta. Un tocco (apri,
// salva, condividi) passa davanti alle anteprime; fra le anteprime va prima l'ultima chiesta, che è la più in basso nella
// chat, cioè quella a schermo.

type Job = { run: () => Promise<unknown>; urgent: boolean; resolve: (v: unknown) => void; reject: (e: unknown) => void }

export class FileQueue {
  private lanes = new Map<string, { busy: boolean; jobs: Job[] }>()

  /** `job` parte quando la fila della sessione è libera; `urgent` = un tocco della persona. */
  run<T>(session: string, job: () => Promise<T>, urgent = false): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      const lane = this.lanes.get(session) ?? { busy: false, jobs: [] }
      this.lanes.set(session, lane)
      lane.jobs.push({ run: job, urgent, resolve: resolve as (v: unknown) => void, reject })
      this.next(session)
    })
  }

  /** Quanti aspettano o sono in volo per la sessione (per i test e il debug). */
  size(session: string): number {
    const lane = this.lanes.get(session)
    return lane ? lane.jobs.length + (lane.busy ? 1 : 0) : 0
  }

  private next(session: string) {
    const lane = this.lanes.get(session)
    if (!lane || lane.busy) return
    if (!lane.jobs.length) { this.lanes.delete(session); return }
    const u = lane.jobs.findIndex(j => j.urgent)
    const [j] = lane.jobs.splice(u >= 0 ? u : lane.jobs.length - 1, 1)
    lane.busy = true
    let out: Promise<unknown>
    try { out = j.run() } catch (e) { out = Promise.reject(e) }
    out.then(j.resolve, j.reject).finally(() => { lane.busy = false; this.next(session) })
  }
}
