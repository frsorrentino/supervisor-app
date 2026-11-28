// I gesti della master (rules/MasterGestures.kt, app-master-gesti-senza-conflitti-04-11): oltre DRAG_PX il dito sul foglio è un
// trascinamento, fermo al rilascio è un tocco (MasterSheet); la velocità del dito e la molla senza rimbalzo dei rilasci.
// app-posizioni-sistema-27-11 gruppi 5 e 6: via il riconoscitore dei clic sulle maniglie, la soglia della barra di prima e la corsa
// breve fra posti fissi, che servivano solo alle posizioni di prima (anche in MasterGestures.kt).
export const DRAG_PX = 6

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
export const LAND_MAX_STIFFNESS = 10000

/**
 * Ogni rilascio della master arriva senza rimbalzo (app-altezza-senza-rimbalzo-06-11, Franz 06/11 10:59: «dopo il
 * ridimensionamento della master c'è ancora il rimbalzo»; MasterGestures.land): col dito verso l'arrivo più veloce di ω·|from| la
 * molla passava l'arrivo e tornava (da 40 px a 3000 px/s, 36 px oltre). Allora si fa più rigida, ω = |v| / |from|, e parte con la
 * velocità del dito senza superare l'arrivo; oltre LAND_MAX_STIFFNESS resta quella e la velocità si limita.
 */
export function landSpring(from: number, v: number, stiffness = SIDE_SPRING_STIFFNESS): { w: number; v: number } {
  const w = Math.sqrt(stiffness)
  if (!from || from * v >= 0 || Math.abs(v) <= w * Math.abs(from)) return { w, v }
  const wMax = Math.sqrt(Math.max(stiffness, LAND_MAX_STIFFNESS)), need = Math.abs(v) / Math.abs(from)
  return need <= wMax ? { w: need, v } : { w: wMax, v: -wMax * from }
}

export function springFrames(from: number, v0: number, stiffness = SIDE_SPRING_STIFFNESS): number[] {
  const { w, v } = landSpring(from, v0, stiffness), out: number[] = []
  for (let i = 0; ; i++) {
    const t = i / 60, x = (from + (v + w * from) * t) * Math.exp(-w * t)
    out.push(x)
    if ((i > 3 && Math.abs(x) < 0.5) || t > 1.2) break
  }
  out.push(0)
  return out
}
