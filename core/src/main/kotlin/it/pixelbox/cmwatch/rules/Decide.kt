package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*

/**
 * La fase A del programma di Franz del 01/11 (contratto 1.99): decidere meno e meglio. Tutto qui è regola pura: i tasti stanno nelle schermate,
 * che mandano `prova`, `decisione` e `approve` come ogni altro comando.
 */
object BancoProva {
    enum class Build { INSTALLED, TO_INSTALL, UNKNOWN }

    /**
     * L'app confronta da sé la build con quello che ha: il relay non sa cosa è installato. Una release pubblicata non dopo l'installazione è
     * dentro; una più nuova va installata prima. Un tag che non è fra le release si prova lo stesso, dicendo che non si sa.
     */
    fun build(tag: String, releases: List<AppUpdate.Release>, installedAt: Long): Build {
        val r = releases.firstOrNull { it.tag == tag } ?: return Build.UNKNOWN
        return if (r.publishedAt <= installedAt) Build.INSTALLED else Build.TO_INSTALL
    }

    /** L'argomento di «Tutte ok»: gli id mostrati, così una prova scritta nel frattempo non si conferma senza vederla. */
    fun allOk(g: ProveGroup): String = g.prove.joinToString(",") { it.id.toString() }

    /** Dove porta `dove`: una chiave libera; solo quelle che l'app conosce aprono una schermata. */
    private val SCREENS = setOf("queue", "box-aspetta", "box-lavoro", "box-esiti", "lavori", "launch", "night", "search", "settings", "homeorder", "usage", "home")
    fun screen(dove: String?): String? = dove?.trim()?.takeIf { it in SCREENS }
}

/** Contratto 1.100, la prima raccolta (funzione 10 del programma del 01/11): regole pure, i tasti stanno nelle schermate. */
object PrimaRaccolta {
    const val RUNNING = "running"; const val ASK = "ask"; const val INTERRUPTED = "interrupted"; const val DONE = "done"; const val NONE = "none"

    /** All'abbinamento la pagina si apre da sola, una volta: `seen` lo tiene il dispositivo. A raccolta finita resta la riga della home. */
    fun autoOpen(s: PrimaRaccoltaSummary?, seen: Boolean): Boolean = !seen && (s?.status == RUNNING || s?.status == ASK)
    fun askConsent(p: PrimaRaccoltaPage): Boolean = p.consent == null && !p.askText.isNullOrBlank()
    fun canStart(p: PrimaRaccoltaPage): Boolean = p.status == NONE || p.status == INTERRUPTED
    /** Il primo giro consuma quota: prima dice quanto, parte con un tocco e una volta sola. */
    fun firstRoundOffer(p: PrimaRaccoltaPage): String? = p.firstRound?.takeIf { p.status == DONE && it.at == null }?.cost?.takeIf { it.isNotBlank() }
    fun finished(p: PrimaRaccoltaPage): Boolean = p.status == DONE
    fun empty(s: PrimaRaccoltaSection): Boolean = s.n == 0 || s.text.isNullOrBlank()
}

object CinqueMinuti {
    /** Le prossime decisioni: l'ordine è quello del relay, da qui prima e le più pesanti; quelle già toccate escono. */
    fun next(page: DecisionsPage, touched: Set<String>, howMany: Int): List<DecisionGroup> = page.groups.filter { it.key !in touched }.take(howMany)

    /** Le ragioni di «Lascia perdere» non possono essere vuote: il relay rifiuterebbe. */
    fun canDrop(reason: String): Boolean = reason.isNotBlank()

    enum class Act { APPROVE, DONE, NONE }

    /** Cosa fa il tasto principale di una voce: un ok si approva, una pausa o una richiesta si dicono fatte. */
    fun act(i: DecisionItem): Act = when (i.kind) {
        "ok" -> if (i.task != null) Act.APPROVE else Act.NONE
        "pause" -> if (i.task != null) Act.DONE else Act.NONE
        "needs_you" -> if (i.session != null) Act.DONE else Act.NONE
        else -> Act.NONE
    }
}

object OkInformato {
    enum class Undo { YES, NO, PARTLY, UNDECLARED }

    /** Le righe della card: quello che manca resta `null` o UNDECLARED e la card dice «non dichiarato», non si inventa. */
    data class Lines(val untested: String?, val undo: Undo, val undoHow: String?, val precedent: ApprovalPrecedent?)

    fun lines(a: Approval): Lines = Lines(a.untested?.takeIf { it.isNotBlank() }, when (a.undo) { "si" -> Undo.YES; "no" -> Undo.NO; "in-parte" -> Undo.PARTLY; else -> Undo.UNDECLARED }, a.undoHow?.takeIf { it.isNotBlank() }, a.precedent?.takeIf { it.task.isNotBlank() })

    /** Gli id da mandare con l'ok: quelli mostrati. Null se la card non ne ha, così il comando resta com'era. */
    fun also(a: Approval): List<String>? = a.also?.map { it.task }?.takeIf { it.isNotEmpty() }
}

object ChatEsito {
    /** Il testo sopra e l'ultima riga, se è l'esito; altrimenti tutto sopra. Solo l'ultima riga piena conta, scritta con l'etichetta. */
    data class Split(val body: String, val outcome: String?)

    fun split(text: String, label: String): Split {
        val lines = text.trimEnd().split('\n')
        val last = lines.lastOrNull()?.trim().orEmpty()
        // Le sessioni scrivono «Esito:» anche quando l'app è in inglese.
        if (listOf(label, "Esito").none { last.startsWith("$it:") || last.startsWith("$it ") }) return Split(text, null)
        return Split(lines.dropLast(1).joinToString("\n").trimEnd(), last)
    }
}

/** Il titolo della notte (Franz 01/11): a cavallo di due mesi ogni giorno dice il suo, non «November 31-1». */
object NightTitle {
    data class Parts(val dayA: Int, val monthA: String?, val dayB: Int, val monthB: String)
    fun parts(before: java.time.LocalDate, day: java.time.LocalDate, name: (java.time.Month) -> String): Parts =
        Parts(before.dayOfMonth, name(before.month).takeIf { before.month != day.month }, day.dayOfMonth, name(day.month))
}

object HeadsUpNote {
    /** La nota da mostrare: quelle già viste su questo dispositivo restano nascoste finché il testo non cambia. */
    fun visible(s: Session, dismissed: Set<String>): HeadsUp? = s.headsUp?.takeIf { it.text.isNotBlank() && key(s.name, it.text) !in dismissed }

    fun key(session: String, text: String): String = "$session|$text"
}
