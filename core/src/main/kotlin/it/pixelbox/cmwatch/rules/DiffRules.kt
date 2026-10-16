package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ChangedFile
import it.pixelbox.cmwatch.contract.ChangesCount
import it.pixelbox.cmwatch.contract.ChangesView
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Hunk
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.StepDiff

/**
 * Il diff dal telefono (contratto 1.72, specifica del 15/10 approvata da Franz dalle 22:43 alle 23:11): le regole comuni a
 * telefono, tablet e web (diff.ts, con gli stessi test). La riga chiusa di un passaggio, «totals.ts · +2 −1»; le etichette e
 * i motivi dei file della vista «Modifiche»; le righe numerate del diff aperto; «Mostra tutto»; l'esito delle letture.
 */
object DiffRules {
    /** Il segno meno tipografico della specifica: «+12 −3». */
    const val MINUS = '\u2212'
    /** Oltre questa lunghezza una riga si accorcia a schermo: una riga minimizzata da 50.000 caratteri non blocca la vista. */
    const val MAX_LINE_CHARS = 2_000
    /** Il rifiuto del PC quando git non risponde (contratto 1.72): comincia così. */
    const val GIT_TIMEOUT = "git did not answer in 5 s"
    /** Oltre questa misura il PC manda solo nome, stato e conteggi. */
    const val BIG_BYTES = 1_000_000L

    fun counts(added: Int, removed: Int): String = "+$added $MINUS$removed"

    fun name(path: String): String = path.trimEnd('/').substringAfterLast('/')

    /** La cartella del file, null per un file nella radice. */
    fun folder(path: String): String? = path.trimEnd('/').substringBeforeLast('/', "").ifEmpty { null }

    /** La riga chiusa sotto un passaggio di modifica. */
    fun stepLine(d: StepDiff): String = "${name(d.path)} · ${counts(d.added, d.removed)}"

    /** La pillola della testata: i conteggi, anche per una sessione chiusa (la cartella c'è ancora); null = niente pillola. */
    fun pill(s: Session): ChangesCount? = s.changes

    enum class Label { NEW, DELETED, RENAMED, BINARY }

    /** «nuovo», «eliminato», «rinominato» o «binario»; un file solo modificato, o uno stato che l'app non conosce, nessuna. */
    fun label(status: String): Label? = when (status) {
        "added" -> Label.NEW
        "deleted" -> Label.DELETED
        "renamed" -> Label.RENAMED
        "binary" -> Label.BINARY
        else -> null
    }

    /** I numeri accanto al file: mai per un binario (git non conta le righe), mai «+0 −0» per un rinominato senza righe cambiate. */
    fun showCounts(f: ChangedFile): Boolean = f.status != "binary" && !(f.status == "renamed" && f.added == 0 && f.removed == 0)

    /** Perché un file aperto non ha righe: la vista lo dice invece di restare vuota. */
    enum class Hidden { BINARY, SECRET, BIG, RENAMED, EMPTY }

    private val SECRET = listOf("""^\.env""", """\.pem$""", """\.key$""", """^id_""", """\.p12$""", """^credentials""").map { Regex(it) }

    /** Il nome sensibile della specifica, sul nome del file: `.env*`, `*.pem`, `*.key`, `id_*`, `*.p12`, `credentials*`. */
    fun secret(path: String): Boolean = name(path).let { n -> SECRET.any { it.containsMatchIn(n) } }

    /** Null quando ci sono righe da mostrare, o quando il file è solo tagliato (si apre intero con `file=`). */
    fun hidden(f: ChangedFile): Hidden? = when {
        f.hunks.isNotEmpty() || f.cut -> null
        f.status == "binary" -> Hidden.BINARY
        secret(f.path) -> Hidden.SECRET
        (f.size ?: 0L) > BIG_BYTES -> Hidden.BIG
        f.status == "renamed" && f.added == 0 && f.removed == 0 -> Hidden.RENAMED
        else -> Hidden.EMPTY
    }

    enum class Kind { SAME, ADDED, REMOVED, GAP, NOTE }

    /** Una riga del diff aperto: `old` e `new` i numeri nelle due versioni, null dove la riga non c'è; `more` = caratteri lasciati fuori. */
    data class Row(val kind: Kind, val old: Int?, val new: Int?, val text: String, val more: Int = 0)

    /**
     * Le righe numerate: il contesto coi due numeri, le tolte col vecchio, le aggiunte col nuovo; fra due pezzi una riga GAP;
     * «\ No newline at end of file» come nota senza numeri. I tab diventano quattro spazi e il «\r» di Windows in fondo
     * sparisce; oltre MAX_LINE_CHARS la riga si accorcia e `more` dice quanto resta.
     */
    fun rows(hunks: List<Hunk>): List<Row> {
        val out = ArrayList<Row>()
        hunks.forEachIndexed { i, h ->
            if (i > 0) out += Row(Kind.GAP, null, null, "")
            var o = h.oldStart
            var n = h.newStart
            for (raw in h.lines) {
                val sign = raw.firstOrNull()
                if (sign == '\\') { out += Row(Kind.NOTE, null, null, raw.drop(1).trim()); continue }
                val body = raw.drop(1).removeSuffix("\r").replace("\t", "    ")
                val text = body.take(MAX_LINE_CHARS)
                val more = body.length - text.length
                out += when (sign) {
                    '+' -> Row(Kind.ADDED, null, n++, text, more)
                    '-' -> Row(Kind.REMOVED, o++, null, text, more)
                    else -> Row(Kind.SAME, o++, n++, text, more)
                }
            }
        }
        return out
    }

    /** Quante cifre servono ai numeri di riga. */
    fun gutter(rows: List<Row>): Int = (rows.maxOfOrNull { maxOf(it.old ?: 0, it.new ?: 0) } ?: 0).toString().length

    /** I file che la vista tagliata (oltre 50 file o 2000 righe) ha lasciato fuori dall'elenco: `totals` li conta tutti. */
    fun hiddenFiles(v: ChangesView): Int = (v.totals.files - v.files.size).coerceAtLeast(0)

    enum class Source { GIT, NO_COMMITS, TRANSCRIPT }

    /** La riga in testa alla vista: rispetto all'ultimo commit, repo senza commit, o cartella fuori da git. */
    fun source(v: ChangesView): Source = when {
        v.source == "transcript" -> Source.TRANSCRIPT
        v.base == null -> Source.NO_COMMITS
        else -> Source.GIT
    }

    /** L'argomento di `changes` per un file solo. */
    fun fileArg(path: String): String = "file=$path"

    /** Il file nella risposta di `changes file=`: per percorso, o l'unico; null = non ha più modifiche. */
    fun alone(v: ChangesView, path: String): ChangedFile? = v.files.firstOrNull { it.path == path } ?: v.files.singleOrNull()

    /**
     * Che cosa mostra un passaggio aperto. `full` = il passaggio intero arrivato con l'op `diff`; `wantAll` = «Mostra tutto»
     * toccato (si salva: dopo una rotazione la copia in memoria non c'è più e `ask` lo richiede); `busy` = una richiesta in
     * volo o finita male (Riprova la libera). `showAll` = il tasto; `beyond` = anche intero il passaggio supera le 5000 righe.
     */
    data class Shown(val diff: StepDiff, val showAll: Boolean, val ask: Boolean, val beyond: Boolean)

    fun shown(entry: StepDiff, full: StepDiff?, wantAll: Boolean, busy: Boolean): Shown = Shown(
        diff = full ?: entry,
        showAll = entry.cut && full == null && !wantAll,
        ask = wantAll && full == null && !busy,
        beyond = full?.cut == true,
    )

    enum class Why { DOWN, GIT, OTHER }

    /** L'esito di una lettura `changes` o `diff`. */
    sealed interface Load<out T> {
        data class Ok<T>(val value: T) : Load<T>
        /** `text` = il rifiuto del PC così com'è: per OTHER la vista lo mostra. */
        data class Failed(val why: Why, val text: String = "") : Load<Nothing>
    }

    /** `r` null = nessuna risposta (PC spento o irraggiungibile, nessuna rete): «PC non raggiungibile». */
    fun <T> read(r: CmdResult?, decode: (String) -> T): Load<T> = when {
        r == null -> Load.Failed(Why.DOWN)
        !r.ok -> Load.Failed(if (r.text.startsWith(GIT_TIMEOUT)) Why.GIT else Why.OTHER, r.text)
        else -> try { Load.Ok(decode(r.text)) } catch (e: Exception) { Load.Failed(Why.OTHER, r.text.take(200)) }
    }
}
