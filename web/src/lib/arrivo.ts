/**
 * Che cosa arriva con un'animazione nella chat, come StreamGate in ArrivoRisposta.kt: una volta sola, solo quello che nasce a chat aperta
 * e ferma. Franz 27/11 15:17 («l'animazione del testo quando un compito è terminato e si apre la sessione… solo quando si ferma nella
 * posizione finale», «mantiene uno spazio che poi risulta essere quello dell'esito»): aprendo la sessione di un compito appena finito, la
 * risposta già scritta passava per nuova, perché la sua ora (orologio del PC) si confrontava con l'apertura sull'orologio del browser.
 * Ora una risposta arriva solo se la chat è ferma al suo posto (`settle`, finita l'apertura), da lì la sessione è stata vista lavorare
 * (`working`, con l'inizio del turno sull'orologio del PC) e la risposta è nata da quel turno in poi. Quello che compare prima, o senza
 * ora, è di prima: si vede intero e fermo. Lo stesso per le righe che entrano (`live`).
 */
export const OPENING_MS = 1000

export type ArrivalGate = {
  prime(ids: Iterable<string>): void
  settle(): void
  working(state: string, started: number | null | undefined): void
  claim(id: string, at: number | null | undefined): boolean
  readonly settled: boolean
  readonly live: boolean
}

export function arrivalGate(): ArrivalGate {
  const known = new Set<string>()
  let primed = false
  let settled = false
  let from: number | null = null
  return {
    prime(ids) { if (primed) return; for (const id of ids) known.add(id); primed = true },
    settle() { settled = true },
    working(state, started) { if (settled && from == null && started != null && (state === 'busy' || state === 'awaiting')) from = started },
    claim(id, at) {
      if (!primed || known.has(id)) return false
      known.add(id)
      return settled && from != null && at != null && at >= from
    },
    get settled() { return settled },
    get live() { return settled && from != null },
  }
}
