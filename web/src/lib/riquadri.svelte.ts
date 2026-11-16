// I riquadri della sessione aperti o chiusi, per sessione (app-riquadri-chiusi-16-11): la regola in riquadri.ts, qui lo stato
// condiviso da tutte le colonne e ricordato in questo browser. Senza memoria resta per la visita.
import { isOpen, parseBoxes, toggle, type BoxState, type Kind } from './riquadri'

const KEY = 'cm.riquadri'
const read = (): Record<string, BoxState> => { try { return parseBoxes(localStorage.getItem(KEY)) } catch { return {} } }
export const boxes = $state<{ all: Record<string, BoxState> }>({ all: read() })

export const boxOpen = (session: string, kind: Kind, waiting: readonly string[] = []): boolean => isOpen(boxes.all[session] ?? {}, kind, waiting)

export function boxToggle(session: string, kind: Kind, waiting: readonly string[] = []) {
  boxes.all = { ...boxes.all, [session]: toggle(boxes.all[session] ?? {}, kind, waiting) }
  try { localStorage.setItem(KEY, JSON.stringify(boxes.all)) } catch { /* resta per questa visita */ }
}
