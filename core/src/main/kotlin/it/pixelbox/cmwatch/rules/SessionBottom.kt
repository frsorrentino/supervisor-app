package it.pixelbox.cmwatch.rules

import kotlin.math.max

/**
 * In basso nella sessione (app-posizioni-sistema-27-11 gruppo 3, app-posizioni-basso-27-11; proposta in dalla-master/posizioni-2026-11-27,
 * «Cosa sta in basso»: in alto si consulta, in basso si scrive). Le Azioni sono una riga di chip sopra il campo, che c'è solo a campo
 * vuoto; il contesto è un anellino con la percentuale dentro il campo, e dal 70 % il chip «Contesto 72% · Handoff» apre la riga. Qui le
 * regole che non sono disegno, uguali sul web (web/src/lib/sessionBottom.ts).
 */
object SessionBottom {
    /** Da qui il chip «Contesto 72% · Handoff» apre la riga delle Azioni e l'anellino nel campo diventa ambra. */
    const val CHIP_FROM = 70

    /** La percentuale dell'anellino nel campo, mai negativa; null senza contesto, e allora l'anellino non c'è. */
    fun ringPct(context: Int?): Int? = context?.let { max(0, it) }

    /** La parte piena dell'anellino, da 0 a 1: oltre il 100 % resta pieno. */
    fun ringFraction(pct: Int): Float = (pct / 100f).coerceIn(0f, 1f)

    /** L'anellino è ambra dalla soglia del chip, come il chip. */
    fun ringHigh(pct: Int?): Boolean = pct != null && pct >= CHIP_FROM

    /**
     * Il chip del contesto, primo nella riga delle Azioni: sessione ferma (come la proposta di prima, contratto 1.37), contesto dal 70 % e
     * un comando di handoff (contratto 1.51). Torna la percentuale da scrivere sul chip, o null.
     */
    fun ctxChip(context: Int?, idle: Boolean, canHandoff: Boolean): Int? = context?.takeIf { idle && canHandoff && it >= CHIP_FROM }

    /** La riga delle Azioni sopra il campo: c'è con almeno un'azione o col chip del contesto, e solo a campo vuoto. */
    fun rowShown(actions: Int, chip: Boolean, draft: String): Boolean = draft.isBlank() && (actions > 0 || chip)
}
