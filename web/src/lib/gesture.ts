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

// La corsa breve (app-master-corsa-breve-05-11, Franz 05/11 11:23; rules/MasterGestures.kt, `step`): fra posizioni fisse al
// rilascio conta la direzione. Oltre COMMIT_PX nella direzione presa, o con un colpo oltre FLICK_PX_S, la posizione dopo in
// quella direzione; sotto si torna dov'era. La velocità si proietta come nei fogli di iOS: tratti e colpi lunghi saltano di più.
export const COMMIT_PX = 24
export const FLICK_PX_S = 500
export const PROJECT_S = 0.499

/** La posizione al rilascio fra `stops` (in ordine crescente lungo l'asse), da `from`, con lo spostamento e la velocità (px/s). */
export function step(stops: number[], from: number, delta: number, velocity = 0): number {
  if (stops.length < 2) return from
  const dir = Math.abs(velocity) >= FLICK_PX_S ? Math.sign(velocity) : Math.abs(delta) >= COMMIT_PX ? Math.sign(delta) : 0
  if (!dir) return from
  const next = Math.min(stops.length - 1, Math.max(0, from + dir))
  const at = stops[from] + delta + velocity * PROJECT_S
  let near = 0
  for (let i = 1; i < stops.length; i++) if (Math.abs(stops[i] - at) < Math.abs(stops[near] - at)) near = i
  return dir > 0 ? Math.max(next, near) : Math.min(next, near)
}

/** La velocità del dito negli ultimi 80 ms, in px/s: `add(x, t)` a ogni movimento, `speed()` al rilascio. */
export function velocity(windowMs = 80) {
  const pts: { x: number; t: number }[] = []
  return {
    add(x: number, t: number) { pts.push({ x, t }); while (pts.length > 2 && t - pts[0].t > windowMs) pts.shift() },
    speed(): number { if (pts.length < 2) return 0; const a = pts[0], b = pts[pts.length - 1]; return b.t > a.t ? ((b.x - a.x) / (b.t - a.t)) * 1000 : 0 },
  }
}

/**
 * Il cambio di lato che continua il gesto (MasterGestures.SIDE_SPRING_*, app-lato-animazione-06-11): una molla smorzata senza
 * rimbalzo da `from` a 0 che parte con la velocità `v` del dito (px/s); le posizioni ogni sessantesimo di secondo, per WAAPI.
 */
export const SIDE_SPRING_STIFFNESS = 300
export function springFrames(from: number, v: number, stiffness = SIDE_SPRING_STIFFNESS): number[] {
  const w = Math.sqrt(stiffness), out: number[] = []
  for (let i = 0; ; i++) {
    const t = i / 60, x = (from + (v + w * from) * t) * Math.exp(-w * t)
    out.push(x)
    if ((i > 3 && Math.abs(x) < 0.5) || t > 1.2) break
  }
  out.push(0)
  return out
}

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
