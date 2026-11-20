package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.contract.WaitItem

/**
 * app-carte-stato-vivo-20-11 (Franz 20/11 20:38: «ci sono ancora incoerenze dopo approvazioni in un punto risultano da approvare in
 * altri»): la carta di una prova nei messaggi della chat legge lo stesso stato di «Per te» e «Dalla master». L'aperto viene da `attese`
 * (contratto 1.151, riletta a ogni `state.attese.rev`); l'esito lo decide la prova letta con `prove N`, mai la sola assenza dalle aperte:
 * una prova uscita dalle aperte ma ancora `todo` (ritirata, compito chiuso, o non ancora riletta) non ha tasti e dice che non è più da
 * fare, senza dirla confermata. La stessa regola in web/src/lib/cardLive.ts.
 */
object CardLive {
    enum class Kind { LOADING, OPEN, OK, KO, RED, NOT_OPEN }

    data class View(val kind: Kind, val at: Long? = null, val text: String? = null) {
        /** Solo una prova aperta ha il tasto. */
        val actionable get() = kind == Kind.OPEN
    }

    /** Le prove aperte secondo `attese`: le prove `todo` delle verifiche ancora aperte. null = non si sa (relay senza `attese`, lista non letta). */
    fun openProve(items: List<WaitItem>?): Set<Long>? = items?.filter { (it.state ?: "open") == "open" }
        ?.flatMap { w -> w.tests.orEmpty().filter { t -> t.state == "todo" }.map { t -> t.id } }?.toSet()

    /** La carta della prova [p] (null = in arrivo) con le aperte [open]. */
    fun prova(p: ProvaOne?, open: Set<Long>?): View = when {
        p == null -> View(Kind.LOADING)
        p.state == "ok" -> View(Kind.OK, p.at.takeIf { it > 0 }, p.text?.takeIf { it.isNotBlank() })
        p.state == "ko" -> View(Kind.KO, p.at.takeIf { it > 0 }, p.text?.takeIf { it.isNotBlank() })
        p.state == "red" -> View(Kind.RED, p.at.takeIf { it > 0 }, p.text?.takeIf { it.isNotBlank() })
        open != null && p.id !in open -> View(Kind.NOT_OPEN)
        else -> View(Kind.OPEN)
    }

    /** L'ora dell'esito: «20:31» oggi, «12/11 20:31» un altro giorno. Come `whenText` in cardLive.ts. */
    fun whenText(at: Long, now: Long, zone: java.time.ZoneId): String {
        val t = java.time.Instant.ofEpochSecond(at).atZone(zone)
        val hm = "%02d:%02d".format(t.hour, t.minute)
        return if (t.toLocalDate() == java.time.Instant.ofEpochSecond(now).atZone(zone).toLocalDate()) hm else "${t.dayOfMonth}/${t.monthValue} $hm"
    }

    /** Le carte in cache da rileggere: quelle che la carta mostra ancora come aperte ma `attese` non ha più fra le aperte. */
    fun stale(cached: Map<Long, ProvaOne?>, open: Set<Long>?): List<Long> =
        if (open == null) emptyList() else cached.filter { (n, p) -> p != null && p.state == "todo" && n !in open }.keys.sorted()
}
