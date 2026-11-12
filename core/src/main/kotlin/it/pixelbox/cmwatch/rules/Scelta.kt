package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptFile
import it.pixelbox.cmwatch.contract.WaitAction
import it.pixelbox.cmwatch.contract.WaitAnswered
import it.pixelbox.cmwatch.contract.WaitFile
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.contract.WaitOption
import kotlinx.serialization.json.JsonPrimitive

/**
 * La scheda di scelta (app-scheda-scelta-12-11, contratto 1.137; descrizione approvata da Franz il 12/11 alle 15:10): le opzioni con
 * la loro lettera, la riga di pro e contro, le immagini e il tasto che le sceglie; «Nessuna, rifare» col motivo; com'è finita
 * (dall'archivio); i giri precedenti col motivo per cui sono stati scartati; le miniature della scheda chiusa. Le stesse regole di
 * web/src/lib/scelta.ts, coi casi di core/src/test/resources/scelta-cases.json.
 */
object Scelta {
    data class Lettered(val letter: String, val name: String)

    private fun letterAt(n: Int): String = if (n in 1..26) ('A' + (n - 1)).toString() else n.toString()
    // Una maiuscola da sola, o seguita da un separatore e dal nome: «A», «A frecce», «A: frecce», «B) pannello», «C - a scomparsa».
    private val OWN = Regex("""^([A-Z])(?:[\s.:)·–—-]+([\s\S]*))?$""")

    /**
     * La lettera e il nome di ogni opzione, in ordine. Se ogni etichetta porta la sua lettera e sono tutte diverse valgono quelle
     * («B pannello» resta B anche al primo posto), e il nome è il resto; altrimenti la lettera viene dal posto e il nome è l'etichetta.
     */
    fun lettersOf(labels: List<String>): List<Lettered> {
        val own = labels.map { l -> OWN.matchEntire(l.trim())?.let { m -> Lettered(m.groupValues[1], m.groupValues[2].trim()) } }
        val all = own.isNotEmpty() && own.all { it != null } && own.map { it!!.letter }.toSet().size == own.size
        return labels.mapIndexed { i, l -> if (all) own[i]!! else Lettered(letterAt(i + 1), l.trim()) }
    }

    data class Files(val images: List<WaitFile>, val docs: List<WaitFile>)

    /** Immagini e documenti di un elenco di file: decide `kind` del relay (un svg è un documento); senza, il tipo. */
    fun splitFiles(files: List<WaitFile>?): Files {
        fun image(f: WaitFile) = if (f.kind.isNotEmpty()) f.kind == "image" else f.mime?.startsWith("image/") == true && f.mime != "image/svg+xml"
        val all = files.orEmpty()
        return Files(all.filter(::image), all.filterNot(::image))
    }

    /** La corsia dei file dichiarati in una richiesta (`session` null): nelle chiavi della cache stanno a parte. */
    const val WAIT_LANE = "\u0001attesa"

    /** Il file di una richiesta come un file della chat, per la cache e l'apertura. */
    fun transcriptFile(f: WaitFile): TranscriptFile = TranscriptFile(f.path, f.mime, f.size)

    /**
     * La chiave di un file di una richiesta nella cache (`FileCache`): con la sessione è la stessa della sua scheda in chat, così
     * l'immagine già arrivata non si richiede; un file dichiarato sta nella corsia delle richieste. Sul web la chiave della chat
     * comincia anche col nome della sessione.
     */
    fun fileKey(f: WaitFile): String {
        val k = FileCache.key(transcriptFile(f), "attesa")
        return if (f.session == null) "$WAIT_LANE\n$k" else k
    }

    /** Un'opzione: la lettera, il nome, la riga di pro e contro, le immagini, i documenti e il posto del suo tasto in `actions`. */
    data class Opt(
        val letter: String, val name: String, val n: Int, val label: String, val value: String, val line: String?, val recommended: Boolean,
        val images: List<WaitFile>, val docs: List<WaitFile>, val action: Int? = null,
    )

    private fun optsOf(options: List<WaitOption>?): List<Opt> {
        val list = options.orEmpty()
        val names = lettersOf(list.map { it.label })
        return list.mapIndexed { i, o ->
            val f = splitFiles(o.files)
            Opt(names[i].letter, names[i].name, o.n, o.label, o.value, o.description?.trim()?.takeIf { it.isNotEmpty() }, o.recommended, f.images, f.docs)
        }
    }

    /** Una scheda di scelta: almeno due opzioni di un ok con scelta, o di qualunque richiesta le cui opzioni portano file. */
    fun isChoice(w: WaitItem): Boolean = (w.options?.size ?: 0) >= 2 && (w.kind == "choice" || w.options!!.any { !it.files.isNullOrEmpty() })

    /** I tasti della scheda: quello di ogni opzione, «Nessuna, rifare», gli altri in ordine; e i file della scheda che nessuna opzione ha. */
    data class Choice(val options: List<Opt>, val extra: List<WaitFile>, val redo: Int?, val rest: List<Int>)

    private fun opOf(a: WaitAction) = (a.cmd["op"] as? JsonPrimitive)?.content
    private fun choiceArg(a: WaitAction) = (a.cmd["choice"] as? JsonPrimitive)?.takeIf { it.isString }?.content

    fun choiceOf(w: WaitItem): Choice {
        val acts = w.actions.orEmpty()
        val taken = mutableSetOf<Int>()
        fun free(pick: (WaitAction) -> Boolean): Int? = acts.indices.firstOrNull { j -> j !in taken && opOf(acts[j]) != "approval_redo" && pick(acts[j]) }
        // Il tasto dell'opzione: quello che manda il suo valore; se no quello con la sua etichetta; se no il primo tasto di scelta libero.
        val options = optsOf(w.options).map { o ->
            val i = free { a -> choiceArg(a) != null && choiceArg(a) == o.value }
                ?: free { a -> TiAspetta.lookOf(a) == TiAspetta.Look.OPTION && a.label == o.label }
                ?: free { a -> TiAspetta.lookOf(a) == TiAspetta.Look.OPTION }
            i?.let { taken += it }
            o.copy(action = i)
        }
        val redo = acts.indices.firstOrNull { opOf(acts[it]) == "approval_redo" }
        val mine = options.flatMap { it.images + it.docs }
        val extra = w.files.orEmpty().filter { f -> mine.none { it.path == f.path && it.session == f.session } }
        return Choice(options, extra, redo, acts.indices.filter { it !in taken && it != redo })
    }

    enum class State(val wire: String) { OPEN("open"), CHOSEN("chosen"), APPROVED("approved"), REDO("redo"), CLOSED("closed") }

    /** Com'è finita una scheda: lo stato, l'opzione scelta con la sua lettera (o le parole, se non è un'opzione), il motivo, chi e quando. */
    data class Outcome(val state: State, val letter: String?, val name: String?, val reason: String?, val by: String?, val at: Long?)

    fun outcomeOf(state: String?, answered: WaitAnswered?, options: List<WaitOption>?): Outcome {
        val s = if (state == null) State.OPEN else State.entries.firstOrNull { it.wire == state } ?: if (answered != null) State.CLOSED else State.OPEN
        val none = Outcome(s, null, null, answered?.reason?.trim()?.takeIf { it.isNotEmpty() }, answered?.by, answered?.at)
        val choice = answered?.choice
        if (s != State.CHOSEN || choice.isNullOrEmpty()) return none
        val list = options.orEmpty()
        val i = list.indexOfFirst { it.value == choice }.takeIf { it >= 0 } ?: list.indexOfFirst { it.label == choice }.takeIf { it >= 0 }
            ?: return none.copy(name = choice)
        val l = lettersOf(list.map { it.label })[i]
        return none.copy(letter = l.letter, name = l.name)
    }

    fun outcomeOf(w: WaitItem): Outcome = outcomeOf(w.state, w.answered, w.options)

    /** Un giro precedente, col suo numero dal primo; `state` redo, closed o replaced. */
    data class Round(
        val id: String, val n: Int, val at: Long, val text: String, val description: String?, val options: List<Opt>, val extra: List<WaitFile>,
        val state: String, val reason: String?, val by: String?, val answeredAt: Long?,
    )

    /** I giri precedenti della scheda, dal più recente. */
    fun roundsOf(w: WaitItem): List<Round> = w.rounds.orEmpty().mapIndexed { i, r ->
        val options = optsOf(r.options)
        val mine = options.flatMap { it.images + it.docs }
        Round(
            r.id, i + 1, r.at, r.text, r.description?.trim()?.takeIf { it.isNotEmpty() }, options,
            r.files.orEmpty().filter { f -> mine.none { it.path == f.path && it.session == f.session } },
            if (r.state == "redo" || r.state == "replaced") r.state else "closed", r.reason?.trim()?.takeIf { it.isNotEmpty() }, r.by, r.answeredAt,
        )
    }.reversed()

    /** Il motivo di «Nessuna, rifare»: obbligatorio, fino a 300 caratteri (contratto 1.137, `approval_redo`). */
    const val REDO_MAX = 300
    fun redoReady(text: String): Boolean = text.trim().length in 1..REDO_MAX

    /** Le miniature della scheda chiusa: la prima immagine di ogni opzione con lettera e nome; senza opzioni, le immagini della scheda. */
    data class Thumb(val letter: String?, val name: String, val file: WaitFile)

    fun thumbsOf(w: WaitItem, max: Int = 4): List<Thumb> =
        if (isChoice(w)) optsOf(w.options).filter { it.images.isNotEmpty() }.map { Thumb(it.letter, it.name, it.images[0]) }.take(max)
        else splitFiles(w.files).images.take(max).map { Thumb(null, "", it) }

    /**
     * La forma della cornice delle immagini di una scheda aperta: quella della prima immagine già arrivata, fra 9:16 e 16:9; finché
     * non ne arriva una, 4:3.
     */
    const val RATIO_DEFAULT = 4f / 3f
    fun frameRatio(known: List<Float?>): Float = known.firstOrNull { it != null && it.isFinite() && it > 0f }?.coerceIn(9f / 16f, 16f / 9f) ?: RATIO_DEFAULT

    /**
     * La forma di una miniatura della scheda chiusa (scelta A di Franz, 12/11 17:46): una tavola larga resta intera, di una schermata
     * alta si vede la parte in cima, mai più alta che larga.
     */
    fun stripRatio(r: Float?): Float = (r?.takeIf { it.isFinite() && it > 0f } ?: 1.1f).coerceIn(1.1f, 16f / 9f)

    /**
     * Il gesto di lato nel visore, da un'immagine intera: +1 la prossima (il dito va a sinistra), -1 la precedente, 0 niente. Vale un
     * tratto di almeno 56 px, o uno scatto svelto di almeno 20 px; un gesto più verticale che orizzontale non conta.
     */
    fun swipeTo(dx: Float, dy: Float, ms: Float): Int {
        val ax = kotlin.math.abs(dx)
        if (ax < kotlin.math.abs(dy) * 1.2f) return 0
        val fast = ms > 0f && ax >= 20f && ax / ms > 0.35f
        if (ax < 56f && !fast) return 0
        return if (dx < 0f) 1 else -1
    }
}
