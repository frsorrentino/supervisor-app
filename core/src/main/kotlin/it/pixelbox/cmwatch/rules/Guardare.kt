package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ResultGroup
import it.pixelbox.cmwatch.contract.ResultItem
import it.pixelbox.cmwatch.contract.ResultsPage
import it.pixelbox.cmwatch.contract.TranscriptFile

/**
 * «Da guardare» (app-da-guardare-02-11, mockup della master delle 08:16 approvato da Franz alle 16:44): i file che le sessioni
 * hanno consegnato con SendUserFile, dall'indice del relay (contratto 1.106). Qui il contenuto delle righe, uguale per telefono e
 * web (guardare.ts); le parole le mette l'app.
 */
object Guardare {
    enum class Kind { VIDEO, MOCKUP, IMAGE, DOCUMENT, OTHER }
    fun kind(k: String?): Kind = when (k) { "video" -> Kind.VIDEO; "mockup" -> Kind.MOCKUP; "image" -> Kind.IMAGE; "document" -> Kind.DOCUMENT; else -> Kind.OTHER }

    /**
     * Una riga: il titolo con la versione solo quando ce n'è più d'una («Film di Supervisor, versione 8»), sotto sessione, tipo,
     * durata in secondi interi per un video e ora; `unseen` accende il punto ambra.
     */
    data class Line(val title: String, val version: Int?, val session: String?, val kind: Kind, val seconds: Int?, val at: Long, val unseen: Boolean)

    fun line(i: ResultItem): Line = Line(
        i.title.trim().ifEmpty { i.path.substringAfterLast('/') }, i.version.takeIf { i.versions > 1 }, i.session?.trim()?.ifEmpty { null },
        kind(i.kind), i.durationS?.takeIf { kind(i.kind) == Kind.VIDEO && it > 0 }?.let { Math.round(it).toInt().coerceAtLeast(1) }, i.at, i.seenAt == null,
    )

    /** L'immagine da mostrare in piccolo: l'immagine stessa, o quella che accompagna un mockup; null per video e documenti. */
    fun thumb(i: ResultItem): TranscriptFile? = when (kind(i.kind)) {
        Kind.IMAGE -> TranscriptFile(i.path, i.mime, i.size)
        Kind.MOCKUP -> i.files.firstOrNull { it.mime?.startsWith("image/") == true } ?: TranscriptFile(i.path, i.mime, i.size).takeIf { i.mime?.startsWith("image/") == true }
        else -> null
    }

    /** Il file da aprire: per un mockup l'immagine se c'è (si guarda), altrimenti il file stesso. */
    fun main(i: ResultItem): TranscriptFile = TranscriptFile(i.path, i.mime, i.size)

    /** Nella sessione, la riga «Risultati»: i suoi lavori dalla pagina, il numero e l'ultimo («Film, versione 8»). */
    data class SessionLine(val groups: List<ResultGroup>, val latest: ResultItem?)
    fun ofSession(page: ResultsPage?, session: String): SessionLine {
        val gs = page?.groups.orEmpty().filter { it.session == session }
        return SessionLine(gs, gs.maxByOrNull { it.at }?.items?.firstOrNull())
    }

    /** Le versioni prima dell'ultima, che si aprono sotto «Versioni precedenti N». */
    fun older(g: ResultGroup): List<ResultItem> = g.items.drop(1)

    /**
     * app-risultati-visti-03-11 (Franz 03/11 15:26: «dovrebbero rimanere solo se effettivamente non li ho visti»): in elenco i lavori con
     * l'ultima versione non vista; quelli visti, dal relay o qui (`local`, il tocco su Visto prima della risposta), in fondo e chiusi.
     */
    data class Split(val fresh: List<ResultGroup>, val seen: List<ResultGroup>)
    fun splitSeen(groups: List<ResultGroup>, local: Set<String>): Split =
        groups.partition { g -> g.items.firstOrNull()?.let { it.seenAt == null && it.id !in local } == true }.let { (a, b) -> Split(a, b) }

    /** Un file aperto altrove nell'app (la chat): i risultati non visti che lo consegnano, da segnare visti. */
    fun seenByPath(items: List<ResultItem>, path: String): List<String> =
        items.filter { i -> i.seenAt == null && (i.path == path || i.files.orEmpty().any { f -> f.path == path }) }.map { it.id }

    /** La posizione di una nota: «36,7 s» col decimale solo se serve, «1:05» oltre il minuto. */
    fun pos(s: Double): String {
        val t = Math.round(s * 10) / 10.0
        if (t >= 60) { val n = t.toInt(); return "%d:%02d".format(n / 60, n % 60) }
        return if (t % 1.0 == 0.0) "${t.toInt()} s" else "${t.toInt()},${Math.round((t % 1.0) * 10)} s"
    }

    /** La nota parte vuota mai, e al massimo 2000 caratteri (contratto 1.106). */
    const val NOTE_MAX = 2000
    fun note(text: String): String? = text.trim().takeIf { it.isNotEmpty() }?.take(NOTE_MAX)
}
