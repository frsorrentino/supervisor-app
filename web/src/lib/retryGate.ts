// Una lettura che torna in errore non riparte subito (app-agenda-raffica-13-11): sul relay di un collega, con l'agenda non
// configurata, un effetto che guardava «niente pagina e niente lettura in corso» la richiedeva appena tornava l'errore, circa 175
// volte al secondo. Dopo un errore si riprova da soli con un'attesa crescente e ci si ferma dopo pochi errori uguali; da lì riparte
// solo chi la chiede a mano. Un errore che dice «la funzione non c'è» la spegne finché il relay non la offre di nuovo. Porta di
// RetryGate.kt.
export const DELAYS_MS = [5_000, 30_000, 120_000]
export const MAX_SAME = 3

export type Gate = { fails: number; error: string | null; until: number; off: boolean }
/** Chi chiede la lettura: un effetto (`auto`), il nuovo tentativo programmato (`retry`), la persona (`user`). */
export type By = 'auto' | 'retry' | 'user'

export const open = (): Gate => ({ fails: 0, error: null, until: 0, off: false })

/** Dopo un errore: lo stesso di prima conta uno in più, uno diverso riparte da uno; `off` = il relay dice che la funzione non c'è. */
export function afterError(g: Gate, error: string, now: number, off = false): Gate {
  const fails = g.error === error ? g.fails + 1 : 1
  return { fails, error, until: now + DELAYS_MS[Math.min(fails, DELAYS_MS.length) - 1], off: g.off || off }
}

/** Niente più tentativi da soli: funzione spenta, o troppi errori uguali di fila. */
export const stopped = (g: Gate): boolean => g.off || g.fails >= MAX_SAME

/**
 * Si può leggere adesso? Senza errori sì. Dopo un errore un effetto non riprova mai da sé; il tentativo programmato solo passata
 * l'attesa e finché non ci si è fermati; la persona passata l'attesa. Spenta, nessuno.
 */
export function mayRead(g: Gate, now: number, by: By): boolean {
  if (g.off) return false
  if (g.fails === 0) return true
  if (by === 'auto' || now < g.until) return false
  return by === 'user' || !stopped(g)
}

/** Fra quanto il nuovo tentativo; null se ci si è fermati. */
export const retryIn = (g: Gate, now: number): number | null => (stopped(g) ? null : Math.max(0, g.until - now))

export type Answer = { ok: boolean; text: string }
export type Reader = { read: (by?: By) => Promise<void>; reset: () => void; gate: () => Gate }

/**
 * Una lettura col suo cancello: `send` chiede, `ok` riceve il testo (se lancia, vale come errore), `fail` l'errore (null = nessuna
 * risposta). Una sola lettura per volta; il nuovo tentativo lo programma lei.
 */
export function gated(o: {
  send: () => Promise<Answer>; start?: () => void; ok: (text: string) => void; fail: (text: string | null) => void
  /** L'errore con cui il relay dice che la funzione non c'è. */
  off?: (text: string) => boolean
  now?: () => number; timer?: (fn: () => void, ms: number) => unknown
}): Reader {
  const now = o.now ?? (() => Date.now())
  const timer = o.timer ?? ((fn, ms) => setTimeout(fn, ms))
  let g = open()
  let busy = false
  // Un tentativo programmato alla volta; `reset` lo annulla cambiando il turno.
  let round = 0
  async function read(by: By = 'auto') {
    if (busy || !mayRead(g, now(), by)) return
    busy = true
    o.start?.()
    let err: string | null = null
    let failed = false
    try {
      const r = await o.send()
      if (r.ok) { try { o.ok(r.text) } catch { failed = true; err = 'unreadable answer' } } else { failed = true; err = r.text }
    } catch { failed = true }
    busy = false
    if (!failed) { g = open(); return }
    g = afterError(g, err ?? '', now(), err != null && !!o.off?.(err))
    o.fail(err === 'unreadable answer' ? null : err)
    const wait = retryIn(g, now())
    const mine = ++round
    if (wait != null) timer(() => { if (mine === round) void read('retry') }, wait)
  }
  return { read, reset: () => { g = open(); round++ }, gate: () => g }
}
