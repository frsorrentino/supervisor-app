// Voce 5 del terzo giro (Jelly slider; app-animazioni-terzo-c-26-11), la gelatina b: le due forme che il filtro dell'originale non
// serve più a fare, calcolate come sul telefono (JellyRate.kt).

/** L'elastico oltre le estremità, dall'originale: asintoto `d` oltre il bordo, pendenza 0,55. */
export function rubber(x: number, lo: number, hi: number, d = 18): number {
  if (x < lo) return lo - (1 - 1 / ((lo - x) * 0.55 / d + 1)) * d
  if (x > hi) return hi + (1 - 1 / ((x - hi) * 0.55 / d + 1)) * d
  return x
}

/** Il collo fra due cerchi (metaball) come percorso SVG; null se sono troppo lontani o uno sta dentro l'altro. */
export function neck(x1: number, y1: number, r1: number, x2: number, y2: number, r2: number, maxDist: number, v = 0.5, handle = 2.4): string | null {
  const d = Math.hypot(x2 - x1, y2 - y1)
  if (d > maxDist || d <= Math.abs(r1 - r2) || r1 <= 0 || r2 <= 0) return null
  const cl = (n: number) => Math.max(-1, Math.min(1, n))
  const [u1, u2] = d < r1 + r2 ? [Math.acos(cl((r1 * r1 + d * d - r2 * r2) / (2 * r1 * d))), Math.acos(cl((r2 * r2 + d * d - r1 * r1) / (2 * r2 * d)))] : [0, 0]
  const a = Math.atan2(y2 - y1, x2 - x1), spread = Math.acos(cl((r1 - r2) / d)), pi = Math.PI
  const a1 = a + u1 + (spread - u1) * v, a2 = a - u1 - (spread - u1) * v
  const a3 = a + pi - u2 - (pi - u2 - spread) * v, a4 = a - pi + u2 + (pi - u2 - spread) * v
  const on = (x: number, y: number, r: number, t: number): [number, number] => [x + r * Math.cos(t), y + r * Math.sin(t)]
  const p1 = on(x1, y1, r1, a1), p2 = on(x1, y1, r1, a2), p3 = on(x2, y2, r2, a3), p4 = on(x2, y2, r2, a4)
  const d2 = Math.min(v * handle, Math.hypot(p1[0] - p3[0], p1[1] - p3[1]) / (r1 + r2)) * Math.min(1, (d * 2) / (r1 + r2))
  const h1 = on(p1[0], p1[1], r1 * d2, a1 - pi / 2), h2 = on(p2[0], p2[1], r1 * d2, a2 + pi / 2)
  const h3 = on(p3[0], p3[1], r2 * d2, a3 + pi / 2), h4 = on(p4[0], p4[1], r2 * d2, a4 - pi / 2)
  const f = (p: [number, number]) => `${p[0].toFixed(2)} ${p[1].toFixed(2)}`
  return `M${f(p1)}C${f(h1)} ${f(h3)} ${f(p3)}L${f(p4)}C${f(h4)} ${f(h2)} ${f(p2)}Z`
}
