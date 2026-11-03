package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.AltroveRun
import it.pixelbox.cmwatch.contract.DecisionGroup
import it.pixelbox.cmwatch.contract.PrimaRaccoltaSection
import it.pixelbox.cmwatch.contract.Promemoria
import it.pixelbox.cmwatch.contract.ProveItem
import it.pixelbox.cmwatch.contract.ProveKo
import it.pixelbox.cmwatch.contract.ProveRed
import it.pixelbox.cmwatch.contract.RimastoItem
import it.pixelbox.cmwatch.contract.Suggestion

/**
 * Le voci dentro le sezioni come righe di sessione (app-richiede-te-righe-02-11, mockup richiede-te.png punto 4): pallino di stato,
 * nome, un dato breve a destra, sotto una riga che dice cosa c'è; il tocco apre la scheda coi tasti di sempre. Qui il contenuto
 * della riga, uguale per telefono e web (itemRows.ts); le parole del dato a destra le mette l'app da [Right].
 */
object ItemRows {
    /** Il colore del pallino: ambra chiede Franz, blu lavora, verde a posto, rosso non va, grigio fermo o senza niente da fare. */
    enum class Tone { WAIT, BUSY, GOOD, RED, STILL }

    /** Il dato breve a destra: `kind` lo mette in parole l'app, `text` è già un testo del relay (versione, prodotto), `n` un numero. */
    enum class Kind { HERE, AT_PC, UNBLOCKS, BUILD, TO_INSTALL, REPORTED, RED, PRODUCT, CHECKED, AT_TIME, UNTIL, FILLED, EMPTY, KIND }
    data class Right(val kind: Kind, val text: String? = null, val n: Int = 0)

    /** `icon`: il tipo di voce che sceglie l'icona al posto del pallino (Suggerimenti, app-suggerimenti-icone-03-11); null = pallino. */
    data class Row(val key: String, val name: String, val sub: String?, val tone: Tone, val right: List<Right>, val icon: String? = null)

    /** Quante righe si vedono prima di «altre N»: il resto si apre col tocco. */
    const val SHOWN = 3
    fun shown(n: Int, all: Boolean): Int = if (all || n <= SHOWN + 1) n else SHOWN

    private fun String?.clean() = this?.trim()?.takeIf { it.isNotEmpty() }

    /** Da decidere: il titolo e, sotto, la frase della master, la causa o il primo motivo; da qui in ambra, al PC grigio. */
    fun decision(g: DecisionGroup): Row {
        val name = g.title.clean() ?: g.note.clean() ?: g.items.firstNotNullOfOrNull { it.title.clean() ?: it.short.clean() } ?: g.key
        val sub = listOf(g.note, g.cause).plus(g.items.flatMap { listOf(it.reason ?: it.what, it.planStart) }).firstNotNullOfOrNull { it.clean()?.takeIf { t -> t != name } }
        val pc = g.where == "pc"
        return Row(g.key, name, sub, if (pc) Tone.STILL else Tone.WAIT, listOfNotNull(Right(if (pc) Kind.AT_PC else Kind.HERE), if (g.weight > 1) Right(Kind.UNBLOCKS, n = g.weight) else null))
    }

    /**
     * Da provare: la prova e cosa fare; in ambra se la versione c'è già, grigia se va prima installata. A destra la sessione: la
     * versione è già la testata del gruppo.
     */
    fun prove(p: ProveItem, installed: Boolean): Row =
        Row(p.id.toString(), p.short.clean() ?: p.title.clean() ?: p.task, p.fai.clean(), if (installed) Tone.WAIT else Tone.STILL, listOfNotNull(if (installed) null else Right(Kind.TO_INSTALL), (p.session.clean() ?: p.project.clean())?.let { Right(Kind.PRODUCT, it) }))
    fun proveKo(k: ProveKo): Row = Row("ko-${k.id}", k.short.clean() ?: k.task, k.text.clean(), Tone.RED, listOf(Right(Kind.REPORTED)))
    fun proveRed(r: ProveRed): Row = Row("red-${r.task}", r.short.clean() ?: r.task, r.reason.clean(), Tone.RED, listOf(Right(Kind.RED)))

    /** Da seguire, rimasto indietro: ambra se è indietro (Accetta o Allinea), verde allineato, grigio se non si sa. */
    fun rimasto(i: RimastoItem): Row = Row(
        "r-${i.product}-${i.key}", i.title.clean() ?: i.key, i.where.clean(),
        when (Seguiti.age(i)) { Seguiti.Age.BEHIND -> Tone.WAIT; Seguiti.Age.ALIGNED -> Tone.GOOD; Seguiti.Age.UNKNOWN -> Tone.STILL },
        listOf(Right(Kind.PRODUCT, i.product)),
    )
    /** Da seguire, altrove: blu mentre gira, ambra se ha trovato qualcosa, verde pulito. */
    fun altrove(r: AltroveRun): Row = Row(
        "a-${r.id}", r.text.clean() ?: r.id, r.positive.firstOrNull().clean() ?: r.errors.firstOrNull().clean(),
        when { r.running -> Tone.BUSY; r.positive.isNotEmpty() -> Tone.WAIT; r.errors.isNotEmpty() -> Tone.RED; else -> Tone.GOOD },
        listOf(Right(Kind.CHECKED, "${r.done}/${r.total}")),
    )
    fun promemoria(p: Promemoria): Row = Row("p-${p.id}", p.text.clean() ?: p.id, null, Tone.STILL, listOf(if (p.atTime != null) Right(Kind.AT_TIME, n = 0, text = p.atTime.toString()) else Right(Kind.UNTIL)))

    /** Prima raccolta: verde quel che è raccolto, grigio quel che è vuoto (sotto, perché). */
    fun prima(s: PrimaRaccoltaSection): Row {
        val empty = PrimaRaccolta.empty(s)
        return Row(s.key, s.key, if (empty) s.why.clean() ?: s.whenFills.clean() else s.text.clean(), if (empty) Tone.STILL else Tone.GOOD, listOf(if (empty) Right(Kind.EMPTY) else Right(Kind.FILLED, n = s.n)))
    }

    /** Suggerimenti: tutti in ambra (chiedono una risposta); a destra il tipo. */
    fun suggestion(s: Suggestion): Row {
        val (name, rest) = split(s.text)
        return Row(s.id, name ?: s.kind, rest, Tone.WAIT, listOf(Right(Kind.KIND, s.kind)), icon = s.kind)
    }

    /** Un testo in nome e resto: la prima riga o la prima frase (fino a «. », «: » o «? »); il resto, se c'è, va sotto. */
    fun split(text: String): Pair<String?, String?> {
        val t = text.trim().replace(Regex("\\s*\\n\\s*"), "\n")
        if (t.isEmpty()) return null to null
        val nl = t.indexOf('\n')
        val stop = Regex("[.:?!] ").find(t)?.range?.first
        val cut = listOfNotNull(nl.takeIf { it >= 0 }, stop?.plus(1)).minOrNull() ?: return t to null
        return t.substring(0, cut).trim().trimEnd(':') to t.substring(cut).replace('\n', ' ').trim().clean()
    }
}
