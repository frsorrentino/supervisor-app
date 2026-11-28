// In basso nella sessione (app-posizioni-sistema-27-11 gruppo 3, app-posizioni-basso-27-11; proposta in dalla-master/posizioni-2026-11-27,
// «Cosa sta in basso»: in alto si consulta, in basso si scrive). Le Azioni sono una riga di chip sopra il campo, che c'è solo a campo
// vuoto; il contesto è un anellino con la percentuale dentro il campo, e dal 70 % il chip «Contesto 72% · Handoff» apre la riga. Qui le
// regole che non sono disegno, uguali sul telefono (SessionBottom.kt).

/** Da qui il chip «Contesto 72% · Handoff» apre la riga delle Azioni e l'anellino nel campo diventa ambra. */
export const CHIP_FROM = 70

/** La circonferenza dell'anellino (raggio 7,5 nel riquadro da 18): l'arco si disegna con stroke-dasharray. */
export const RING_C = 2 * Math.PI * 7.5

/** La percentuale dell'anellino nel campo, intera e mai negativa; null senza contesto, e allora l'anellino non c'è. */
export function ringPct(ctx: number | null | undefined): number | null {
  if (ctx == null || !Number.isFinite(ctx)) return null
  return Math.max(0, Math.round(ctx))
}

/** La parte piena dell'anellino, da 0 a 1: oltre il 100 % resta pieno. */
export const ringFraction = (pct: number): number => Math.min(1, Math.max(0, pct / 100))

/** L'arco dell'anellino come stroke-dasharray: la parte piena e la circonferenza, «8.48 47.12» al 18 %. */
export const ringDash = (pct: number): string => `${(ringFraction(pct) * RING_C).toFixed(2)} ${RING_C.toFixed(2)}`

/** L'anellino è ambra dalla soglia del chip, come il chip. */
export const ringHigh = (pct: number | null | undefined): boolean => pct != null && pct >= CHIP_FROM

/**
 * Il chip del contesto, primo nella riga delle Azioni: sessione ferma (come la proposta di prima, contratto 1.37), contesto dal 70 % e
 * un comando di handoff (contratto 1.51). Torna la percentuale da scrivere sul chip, o null.
 */
export function ctxChip(ctx: number | null | undefined, idle: boolean, canHandoff: boolean): number | null {
  const p = ringPct(ctx)
  return p != null && idle && canHandoff && p >= CHIP_FROM ? p : null
}

/** La riga delle Azioni sopra il campo: c'è con almeno un'azione o col chip del contesto, e solo a campo vuoto. */
export const rowShown = (actions: number, chip: boolean, draft: string): boolean => !draft.trim() && (actions > 0 || chip)
