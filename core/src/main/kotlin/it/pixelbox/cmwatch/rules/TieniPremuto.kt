package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.Question
import it.pixelbox.cmwatch.contract.QuestionKind
import it.pixelbox.cmwatch.contract.WaitAction
import it.pixelbox.cmwatch.contract.WaitItem
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.sqrt

/**
 * Il tasto «tieni premuto» (app-tieni-premuto-28-11, Franz 28/11 16:22, «Tutti e tre»; mockup tieni-premuto-28-11.html, dal Hold to
 * publish di designbookmark con le sue molle): le regole che non sono disegno, uguali sul web (web/src/lib/tieniPremuto.ts, stessi casi
 * nei test). Tenendo premuto la pillola si riempie da sinistra, lineare nel tempo del suo posto; lasciata prima torna indietro, fa «no»
 * con una scossa e non parte niente; arrivata in fondo diventa il cerchio verde con la spunta, poi parte l'azione.
 *
 * Correzione della master del 28/11 (dopo la nota di Franz delle 16:33): si tiene premuto solo nei tre posti scelti da Franz (Chiudi la
 * sessione, Riparti pulito, l'ok di una carta che pubblica). Ogni altro «Approva» e ogni «Va» restano a un tocco, ma col finale dello
 * stesso componente: la pillola diventa il cerchio verde e la spunta si disegna, poi la carta o la riga esce come prima.
 */
object TieniPremuto {
    /** I posti del componente: i tre da tenere premuto e i due a un tocco col finale del cerchio verde. */
    enum class Place { CHIUDI, RIPARTI, PUBBLICA, APPROVA, VA }

    /** Tenere premuto (con il pieno) o toccare (solo il finale). */
    enum class Mode { HOLD, TAP }

    fun modeOf(p: Place): Mode = when (p) {
        Place.CHIUDI, Place.RIPARTI, Place.PUBBLICA -> Mode.HOLD
        Place.APPROVA, Place.VA -> Mode.TAP
    }

    /**
     * Quanto si tiene premuto, in ms: 1,2 s per chiudere (il tempo del componente; chiudere si disfa da «Chiuse»), 1 s per ripartire
     * (l'handoff conserva il contesto), 1,5 s per pubblicare (una pubblicazione non si ritira); 0 per i posti a un tocco.
     */
    fun holdMs(p: Place): Long = when (p) {
        Place.CHIUDI -> 1200L
        Place.RIPARTI -> 1000L
        Place.PUBBLICA -> 1500L
        Place.APPROVA, Place.VA -> 0L
    }

    /** Il pieno dopo [elapsedMs] di pressione: lineare da 0 a 1, è una barra di avanzamento; un posto a un tocco è subito pieno. */
    fun progress(elapsedMs: Long, holdMs: Long): Float = if (holdMs <= 0L) 1f else (elapsedMs.toFloat() / holdMs).coerceIn(0f, 1f)

    enum class Release { EARLY, COMPLETE }

    /** Il dito alzato dopo [elapsedMs]: prima del tempo è «lasciato prima» e non parte niente; dal tempo in poi è fatto. */
    fun release(elapsedMs: Long, holdMs: Long): Release = if (elapsedMs >= holdMs) Release.COMPLETE else Release.EARLY

    /**
     * La spinta della scossa «no» lasciando prima, in px/s sul web e dp/s sul telefono: 260·min(1, 2p + 0,3), così anche una pressione
     * cortissima fa il suo «no»; niente con il moto ridotto.
     */
    fun kick(progress: Float, reduced: Boolean = false): Float = if (reduced) 0f else 260f * minOf(1f, progress.coerceAtLeast(0f) * 2f + 0.3f)

    // Le molle dell'originale (motion.js e tokens.css: massa 1, a = -k(x - t) - c·v), rigidità e smorzamento.
    /** La schiacciata mentre si tiene premuto: a 0,96 con la molla 520 20. */
    const val SQUASH = 0.96f
    const val PRESS_K = 520f
    const val PRESS_C = 20f
    /** Il pieno che torna indietro lasciando prima, appena oltre lo zero. */
    const val RELEASE_K = 260f
    const val RELEASE_C = 14f
    /** La scossa «no». */
    const val SHAKE_K = 700f
    const val SHAKE_C = 9f
    /** La pillola che diventa cerchio (e torna, con 0,8 della rigidità). */
    const val MORPH_K = 320f
    const val MORPH_C = 20f

    /** La spunta parte dopo 160 ms e si disegna in 450 ms con (.65, 0, .35, 1); l'azione parte a spunta disegnata. */
    const val CHECK_DELAY_MS = 160L
    const val CHECK_DRAW_MS = 450L
    const val FIRE_MS = CHECK_DELAY_MS + CHECK_DRAW_MS

    /** Dopo un ok a un tocco la carta esce a spunta disegnata e vista intera un attimo, non subito. */
    const val LEAVE_MS = FIRE_MS + 190L

    /** Quanto resta il cerchio verde quando nessuno toglie il tasto, prima di tornare pillola. */
    const val DONE_MS = 1700L

    /** Con il moto ridotto lo smorzamento è critico (2√k): la molla arriva senza rimbalzo. */
    fun damping(k: Float, c: Float, reduced: Boolean): Float = if (reduced) 2f * sqrt(k) else c

    // ---- Quale ok si tiene premuto: una carta che pubblica (il posto PUBBLICA) ----

    // Le parole di chi fa uscire qualcosa: pubblicare, rilasciare, release, publish, deploy, git push, un push su origin, main o in
    // produzione, mandare o portare in produzione, andare online. Gli indirizzi e i nomi di ramo (release/2.9) non contano, né le
    // note di rilascio.
    private val PUBLISH = Regex(
        """(?i)\b(?:pubblic(?!it)\w*|rilasc\w*|releas\w*|publish\w*|deploy\w*|git\s+push|push\w*\s+(?:-{1,2}\w+\s+)*(?:to|in|su|on|into|verso)\s+(?:origin|main|master|prod\w*|produzione)\b|go[\s-]live|(?:mett\w*|mand\w*|port\w*|andare|andr\w*|andiamo|va|vanno|vado)\s+(?:\S+\s+){0,3}(?:online|in\s+produzione)\b|(?:promot|mov|send|ship|roll)\w*\s+(?:\S+\s+){0,3}(?:to|into)\s+prod(?:uction)?\b)""",
    )
    private val NOT_PUBLISH = Regex("""(?i)https?://\S+|\breleases?/\S*|\brelease[\s-]notes?\b|\bnote\s+di\s+rilascio\b""")

    /** I testi nominano un'uscita (vedi le parole sopra). */
    fun publishes(texts: List<String?>): Boolean = texts.any { t -> t != null && PUBLISH.containsMatchIn(NOT_PUBLISH.replace(t, " ")) }

    // Un'opzione che dice di no o rimanda non diventa mai «Tieni premuto per pubblicare».
    private val REFUSES = Regex(
        """(?i)^\s*(?:\d+\s*·\s*)?(?:no\b|non\b|niente\b|nessun\w*|aspett\w*|rimand\w*|annull\w*|lascia\s+perdere|più\s+tardi|dopo\b|domani\b|domattina\b|stasera\b|wait\b|later\b|not\b|don'?t\b|cancel\w*|skip\w*|stop\b|tomorrow\b)""",
    )

    /** L'etichetta di un tasto che rifiuta o rimanda. */
    fun refuses(label: String): Boolean = REFUSES.containsMatchIn(label)

    private fun opOf(a: WaitAction): String? = (a.cmd["op"] as? JsonPrimitive)?.content
    private fun resultOf(a: WaitAction): String? = (a.cmd["result"] as? JsonPrimitive)?.content

    /**
     * Il tasto della carta [w] che si tiene premuto per pubblicare, o null. È il tasto dell'ok (il pieno) o, senza, la scelta
     * consigliata; mai uno che chiede un testo, mai un «no» o un «rimanda», mai su una verifica. La carta pubblica se il contratto la
     * segna per la produzione (`card.deploy`, contratto 1.131) o se titolo, testo, descrizione, cosa, dove o l'etichetta del tasto
     * nominano un'uscita.
     */
    fun holdIndex(w: WaitItem): Int? {
        if (w.label == "verify") return null
        val acts = w.actions.orEmpty()
        val i = acts.indexOfFirst { it.style == "primary" }.takeIf { it >= 0 } ?: acts.indexOfFirst { it.recommended }.takeIf { it >= 0 } ?: return null
        val a = acts[i]
        if (a.textField != null || refuses(a.label)) return null
        val out = w.card?.deploy == true || publishes(listOf(w.title, w.text, w.description, w.card?.what, w.card?.where, a.label))
        return if (out) i else null
    }

    /** L'ok di [state.approvals]: si tiene premuto se esce in produzione (`deploy`) o se titolo, cosa o dove nominano un'uscita. */
    fun holds(a: Approval): Boolean = a.deploy || publishes(listOf(a.title, a.what, a.where))

    /**
     * L'opzione di una domanda che si tiene premuta per pubblicare (il suo numero), o null: la consigliata o, su un permesso senza
     * consigliata, la prima che non dice di no; mai un «no» o un «rimanda». La domanda pubblica se cosa si approva, il dialogo, il testo o
     * l'opzione nominano un'uscita (il «Pubblica il deploy 2.8.0: ./deploy.sh --env production» del mockup); le altre opzioni restano.
     */
    fun holdOption(q: Question): Int? {
        val o = q.options.firstOrNull { it.recommended }
            ?: q.options.firstOrNull { q.kind == QuestionKind.PERMISSION && !refuses(it.label) }
            ?: return null
        if (refuses(o.label)) return null
        return if (publishes(listOf(q.what, q.body, q.text, o.label, o.description))) o.n else null
    }

    /**
     * Il posto del tasto [i] della carta [w]: PUBBLICA per l'ok che pubblica (da tenere premuto); VA per il «Va» di una prova (`prova`
     * con `result` ok, anche «Ora va»); APPROVA per ogni altro ok (`approve`, anche la scelta di un'opzione); null per gli altri tasti,
     * che restano com'erano.
     */
    fun placeOf(w: WaitItem, i: Int): Place? {
        val a = w.actions?.getOrNull(i) ?: return null
        if (holdIndex(w) == i) return Place.PUBBLICA
        val op = opOf(a)
        if (op == "prova" && resultOf(a) == "ok") return Place.VA
        if (op == "approve") return Place.APPROVA
        return null
    }
}
