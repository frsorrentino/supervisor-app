package it.pixelbox.cmwatch.rules

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Un'azione che toglie un elemento (Franz 02/11 15:38: «non voglio elementi che scompaiono o che non danno alcun feedback»),
 * porta di leave.ts. Voce 11 e voce 7 delle animazioni del 31/10, nello stesso ordine su telefono e web: al tocco il tasto
 * scelto diventa la conferma (`pick`), il comando parte subito, dopo la conferma l'elemento esce (`hide`, con la sua
 * transizione) e chi sta sotto sale. Non si aspetta il PC per uscire; se il PC dice no, l'elemento torna e il tasto si spegne.
 */
object Leave {
    /** Quanto resta in vista la conferma sul tasto prima che l'elemento esca. */
    fun confirmMs(animationsOff: Boolean): Long = if (animationsOff) 0 else 350

    /**
     * Quando un elemento è uscito l'ultima volta (app-prova-ok-sparisce-04-11): la lista sale, e un tocco ripetuto nel punto dove
     * c'era il tasto finiva sulla testata di una categoria, che si apriva e chiudeva a ogni tocco (12 home_order in 30 s alle
     * 18:16). Per [SETTLE_MS] le testate ignorano il tocco.
     */
    @Volatile var lastHideAt = 0L
    const val SETTLE_MS = 800L
    fun settling(now: Long): Boolean = now - lastHideAt < SETTLE_MS

    interface Hooks { fun pick(ids: List<String>, choice: String?); fun hide(ids: List<String>, hidden: Boolean) }

    suspend fun run(ids: List<String>, choice: String, send: suspend () -> Boolean, h: Hooks, wait: suspend () -> Unit): Boolean = coroutineScope {
        h.pick(ids, choice)
        val sent = async { runCatching { send() }.getOrDefault(false) }
        wait()
        if (sent.isCompleted && !sent.await()) { h.pick(ids, null); return@coroutineScope false }
        h.hide(ids, true); lastHideAt = System.currentTimeMillis()
        val ok = sent.await()
        if (!ok) { h.hide(ids, false); h.pick(ids, null) }
        ok
    }
}
