package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.contract.WaitItem

/**
 * app-carte-stato-vivo-20-11 (Franz 20/11 20:38: «ci sono ancora incoerenze dopo approvazioni in un punto risultano da approvare in
 * altri»): la carta di una prova nei messaggi della chat legge lo stesso stato di «Per te» e «Dalla master». L'aperto viene da `attese`
 * (contratto 1.151, riletta a ogni `state.attese.rev`); l'esito lo decide la prova letta con `prove N`, mai la sola assenza dalle aperte:
 * una prova uscita dalle aperte ma ancora `todo` (ritirata, compito chiuso, o non ancora riletta) non ha tasti e dice che non è più da
 * fare, senza dirla confermata. «Uscita» vuol dire vista fra le aperte e poi non più: una prova che `attese` non ha mai nominato resta
 * come la dice la prova letta. La stessa regola in web/src/lib/cardLive.ts.
 */
object CardLive {
    enum class Kind { LOADING, OPEN, OK, KO, RED, NOT_OPEN }

    data class View(val kind: Kind, val at: Long? = null, val text: String? = null) {
        /** Solo una prova aperta ha il tasto. */
        val actionable get() = kind == Kind.OPEN
    }

    /**
     * Le prove aperte secondo `attese`, dalle voci ancora aperte: i numeri nei comandi `prova` dei tasti e della risposta (le voci del relay
     * hanno `arg` = «1412» o «1412,1413»), più le prove `todo` di `tests` (contratto 1.151). null = non si sa (relay senza `attese`).
     */
    fun openProve(items: List<WaitItem>?): Set<Long>? = items?.filter { (it.state ?: "open") == "open" }?.flatMap { w ->
        val args = w.actions.orEmpty().filter { a -> (a.cmd["op"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "prova" }
            .mapNotNull { a -> (a.cmd["arg"] as? kotlinx.serialization.json.JsonPrimitive)?.content } +
            listOfNotNull(w.answer?.takeIf { it.op == "prova" }?.arg)
        args.flatMap { s -> s.split(',').mapNotNull { x -> x.trim().toLongOrNull() } } + w.tests.orEmpty().filter { t -> t.state == "todo" }.map { t -> t.id }
    }?.toSet()

    /** Le prove uscite dalle aperte: viste fra le aperte ([seen]) e ora non più fra quelle di [open]. */
    fun closedProve(seen: Set<Long>, open: Set<Long>?): Set<Long> = if (open == null) emptySet() else seen - open

    /** La carta della prova [p] (null = in arrivo); [closed] le prove uscite dalle aperte ([closedProve]). */
    fun prova(p: ProvaOne?, closed: Set<Long>): View = when {
        p == null -> View(Kind.LOADING)
        p.state == "ok" -> View(Kind.OK, p.at.takeIf { it > 0 }, p.text?.takeIf { it.isNotBlank() })
        p.state == "ko" -> View(Kind.KO, p.at.takeIf { it > 0 }, p.text?.takeIf { it.isNotBlank() })
        p.state == "red" -> View(Kind.RED, p.at.takeIf { it > 0 }, p.text?.takeIf { it.isNotBlank() })
        p.id in closed -> View(Kind.NOT_OPEN)
        else -> View(Kind.OPEN)
    }

    /** L'ora dell'esito: «20:31» oggi, «12/11 20:31» un altro giorno. Come `whenText` in cardLive.ts. */
    fun whenText(at: Long, now: Long, zone: java.time.ZoneId): String {
        val t = java.time.Instant.ofEpochSecond(at).atZone(zone)
        val hm = "%02d:%02d".format(t.hour, t.minute)
        return if (t.toLocalDate() == java.time.Instant.ofEpochSecond(now).atZone(zone).toLocalDate()) hm else "${t.dayOfMonth}/${t.monthValue} $hm"
    }

    /** Le carte in cache da rileggere: quelle che mostrano ancora da fare una prova uscita dalle aperte. */
    fun stale(cached: Map<Long, ProvaOne?>, closed: Set<Long>): List<Long> = cached.filter { (n, p) -> p != null && p.state == "todo" && n in closed }.keys.sorted()
}
