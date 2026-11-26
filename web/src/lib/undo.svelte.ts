import { UNDO_MS } from './animazioni'

// Voce 4 di app-animazioni-seconde-26-11 (Annulla col tempo, scelta di Franz del 26/11 alle 12:49): un gesto che toglie qualcosa
// (Non ora, Mai, Visto, Chiudi senza salvare) cambia subito quello che si vede, ma il comando parte dopo UNDO_MS; fino ad allora
// «Annulla» lo ferma e rimette com'era. Un avviso per volta: un gesto nuovo fa partire subito quello di prima.

type Pending = { id: number; text: string; go: () => void; back: () => void; timer: ReturnType<typeof setTimeout> }
let pending: Pending | null = null
let seq = 0

/** L'avviso a schermo: il testo e un id che cambia a ogni gesto (l'anello riparte). */
export const undoState = $state<{ shown: { id: number; text: string } | null }>({ shown: null })

function clear() { pending = null; undoState.shown = null }

/** `go` è il comando vero, `back` rimette l'interfaccia com'era se si annulla. */
export function later(text: string, go: () => void, back: () => void = () => {}) {
  flush()
  const id = ++seq
  const timer = setTimeout(() => { if (pending?.id === id) { const p = pending; clear(); p.go() } }, UNDO_MS)
  pending = { id, text, go, back, timer }
  undoState.shown = { id, text }
}

/** «Annulla»: il comando non parte e l'interfaccia torna com'era. */
export function cancel() {
  if (!pending) return
  const p = pending
  clearTimeout(p.timer)
  clear()
  p.back()
}

/** Il comando in attesa parte subito (un gesto nuovo, la pagina che si chiude). */
export function flush() {
  if (!pending) return
  const p = pending
  clearTimeout(p.timer)
  clear()
  p.go()
}

if (typeof window !== 'undefined') window.addEventListener('pagehide', flush)
