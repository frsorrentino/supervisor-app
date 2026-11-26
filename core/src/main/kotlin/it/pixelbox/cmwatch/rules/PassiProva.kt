package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ProofStep
import it.pixelbox.cmwatch.contract.ProofSteps

/**
 * Le tappe della prova su un filo (voce 5 della prima proposta di animazioni, app-animazioni-passi-26-11, contratto 1.162): Test,
 * CI, Pre-release, Prova del compito in verifica di una sessione. Il filo si riempie fino alla tappa a cui si è arrivati, le tappe
 * passate sono spunte, quella in corso ha l'arco che gira, una fallita è rossa. Uguale sul web (web/src/lib/passiProva.ts).
 */
object PassiProva {
    enum class Look { DONE, NOW, FAILED, TODO }

    fun look(s: ProofStep): Look = when (s.state) { "ok" -> Look.DONE; "run" -> Look.NOW; "ko" -> Look.FAILED; else -> Look.TODO }

    /** Fin dove arriva il filo: la prima tappa non passata (tutte passate = l'ultima). */
    fun reach(items: List<ProofStep>): Int = items.indexOfFirst { it.state != "ok" }.let { if (it < 0) (items.size - 1).coerceAtLeast(0) else it }

    /** Il filo pieno da 0 a 1, dalla prima tappa all'ultima. */
    fun fill(items: List<ProofStep>): Float = if (items.size <= 1) 1f else reach(items).toFloat() / (items.size - 1)

    /** La riga sotto il filo: quale tappa e in che stato; null senza tappe. */
    enum class Say { RUNNING, FAILED, TODO, ALL_DONE }
    data class Line(val say: Say, val label: String)

    fun line(steps: ProofSteps?): Line? {
        val items = steps?.items?.takeIf { it.isNotEmpty() } ?: return null
        items.firstOrNull { it.state == "ko" }?.let { return Line(Say.FAILED, it.label) }
        items.firstOrNull { it.state == "run" }?.let { return Line(Say.RUNNING, it.label) }
        items.firstOrNull { it.state != "ok" }?.let { return Line(Say.TODO, it.label) }
        return Line(Say.ALL_DONE, "")
    }
}
