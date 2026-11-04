// Un solo riconoscitore per le maniglie della master (app-master-gesti-senza-conflitti-04-11, Franz 04/11 18:07: «il trascinamento
// col mouse va un po' in conflitto col doppio click che la rende compatta»): oltre DRAG_PX è un trascinamento, che annulla i clic
// in sospeso e fa ignorare il click e il dblclick che il browser manda dopo il rilascio; fermo, al rilascio è un clic; due clic
// fermi entro DOUBLE_MS sullo stesso posto sono un doppio clic, cioè un gesto solo e non due; se la seconda pressione diventa un
// trascinamento vince il trascinamento. Le stesse regole al tocco sul tablet (rules/MasterGestures.kt).
export const DRAG_PX = 6
export const DOUBLE_MS = 300
/** Quanto a lungo dopo un trascinamento si ignorano click e dblclick del browser. */
export const SWALLOW_MS = 400

export const isDrag = (dx: number, dy: number) => Math.hypot(dx, dy) > DRAG_PX

type Pending = { key: string; at: number; id: ReturnType<typeof setTimeout> | null }

/**
 * I clic delle maniglie. `tap(key, now, single, double)`: senza `double` il clic agisce subito e un secondo clic fermo entro
 * DOUBLE_MS sullo stesso posto non ripete l'azione (il doppio clic è un gesto solo); con `double` il primo clic aspetta DOUBLE_MS
 * per sapere se ne arriva un secondo. `dragged(now)`: è partito un trascinamento.
 */
export function masterTaps(timer: { set: typeof setTimeout; clear: typeof clearTimeout } = { set: setTimeout, clear: clearTimeout }) {
  let last: Pending | null = null
  let swallowUntil = -Infinity
  const drop = () => { if (last?.id != null) timer.clear(last.id); last = null }
  return {
    tap(key: string, now: number, single: () => void, double?: () => void) {
      if (last && last.key === key && now - last.at <= DOUBLE_MS) {
        const waiting = last.id != null
        drop()
        if (double) double()
        else if (waiting) single()
        return
      }
      drop()
      if (double) { const p: Pending = { key, at: now, id: null }; p.id = timer.set(() => { if (last === p) last = null; single() }, DOUBLE_MS); last = p }
      else { last = { key, at: now, id: null }; single() }
    },
    /** Un trascinamento: i clic in sospeso non valgono più, e il click del browser dopo il rilascio si ignora. */
    dragged(now: number) { drop(); swallowUntil = now + SWALLOW_MS },
    /** Rilasciato il trascinamento: da qui conta l'attesa per ignorare il click del browser. */
    released(now: number) { swallowUntil = now + SWALLOW_MS },
    swallow(now: number) { return now < swallowUntil },
  }
}
