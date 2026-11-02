package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.AltroveRun
import it.pixelbox.cmwatch.contract.PromemoriaFired
import it.pixelbox.cmwatch.contract.RimastoItem
import it.pixelbox.cmwatch.contract.SeguitiSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * La fase B del programma di Franz del 01/11 (contratto 1.102): quello che resta fuori dal codice. `rimasto_indietro`, `stesso_difetto` e `promemoria` sono
 * regole pure. Un valore che non si conosce resta non conosciuto: `stale` null non è «allineata», e un errore o un giro parziale non sono mai un negativo.
 */
object Seguiti {
    enum class Age { BEHIND, ALIGNED, UNKNOWN }
    /** Perché una voce è indietro o non si può sapere: `paths:N`, `check`, oppure no_git, no_project, check_timeout. */
    enum class Why { PATHS, CHECK, NO_GIT, NO_PROJECT, CHECK_TIMEOUT, OTHER }

    fun total(s: SeguitiSummary?): Int = s?.let { it.rimasto + it.altrove + it.promemoria } ?: 0
    fun age(i: RimastoItem): Age = when (i.stale) { true -> Age.BEHIND; false -> Age.ALIGNED; null -> Age.UNKNOWN }
    fun why(i: RimastoItem): Why = when {
        i.why?.startsWith("paths:") == true -> Why.PATHS
        i.why == "check" -> Why.CHECK
        i.why == "no_git" -> Why.NO_GIT
        i.why == "no_project" -> Why.NO_PROJECT
        i.why == "check_timeout" -> Why.CHECK_TIMEOUT
        else -> Why.OTHER
    }
    fun changedPaths(i: RimastoItem): Int? = i.why?.removePrefix("paths:")?.takeIf { i.why.startsWith("paths:") }?.toIntOrNull()
    /** Prima chi è indietro, poi chi non si può sapere, in fondo chi è allineato. */
    fun ordered(items: List<RimastoItem>): List<RimastoItem> = items.sortedBy { age(it).let { a -> if (a == Age.BEHIND) 0 else if (a == Age.UNKNOWN) 1 else 2 } }
    /** Con un compito già aperto non se ne apre un altro. */
    fun canAlign(i: RimastoItem) = i.stale != false && i.task == null
    fun canAccept(i: RimastoItem) = i.stale != false

    /** «Nessun altro caso» si dice solo a giro completo, senza errori e con qualcosa di controllato. */
    fun clean(r: AltroveRun) = r.complete && r.total > 0 && r.positive.isEmpty() && r.errors.isEmpty()
    fun partial(r: AltroveRun) = !r.complete
    fun fired(p: PromemoriaFired) = p.outcome == "fired"

    private val FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm")
    fun whenText(epoch: Long, zone: ZoneId = ZoneId.systemDefault()): String = FMT.format(Instant.ofEpochSecond(epoch).atZone(zone))
}
