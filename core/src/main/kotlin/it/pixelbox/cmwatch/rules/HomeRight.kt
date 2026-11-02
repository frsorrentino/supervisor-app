package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.DecisionsSummary
import it.pixelbox.cmwatch.contract.PrimaRaccoltaSummary
import it.pixelbox.cmwatch.contract.ProveSummary
import it.pixelbox.cmwatch.contract.SeguitiSummary

/**
 * Il testo a destra delle voci della home (Franz 02/11 09:41: «le voci nuove devono avere un testo informativo come le altre»): il numero sta accanto al
 * nome, a destra quello che il numero non dice. Solo le parti diverse da zero; l'app le mette in parole. homeRight.ts nella web.
 */
object HomeRight {
    enum class Part { BUILDS, KO, HERE, UNBLOCKS, BEHIND, ELSEWHERE, REMINDERS }
    data class Bit(val part: Part, val n: Int)
    private fun bits(vararg b: Bit) = b.filter { it.n > 0 }

    /** Da provare: su quante versioni, e quante non vanno. */
    fun prove(p: ProveSummary) = bits(Bit(Part.BUILDS, p.builds), Bit(Part.KO, p.ko + p.red))
    /** Da decidere: quante si fanno da qui, quanti compiti sbloccano. */
    fun decisions(d: DecisionsSummary) = bits(Bit(Part.HERE, d.here), Bit(Part.UNBLOCKS, d.tasks))
    /** Da seguire: le tre cose che contiene. */
    fun seguiti(s: SeguitiSummary) = bits(Bit(Part.BEHIND, s.rimasto), Bit(Part.ELSEWHERE, s.altrove), Bit(Part.REMINDERS, s.promemoria))
    /** Prima raccolta: a che punto è. */
    fun prima(p: PrimaRaccoltaSummary): String = p.status
}
