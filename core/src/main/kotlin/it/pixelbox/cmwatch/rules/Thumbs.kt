package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.ResultItem
import it.pixelbox.cmwatch.contract.ThumbPreview

/**
 * Le anteprime dei documenti in Verifica (app-anteprime-verifica-11-11, Franz 11/11 19:49: «Qui l'anteprima si vede, invece vorrei che si
 * vedesse»; contratto 1.136): le consegne senza un'immagine loro (documenti, altri file) chiedono al relay la loro anteprima con l'op
 * `thumb`, una volta per versione e poi in cache; con l'etag la stessa lettura torna «unchanged». Un relay senza l'op tiene l'icona.
 * Lo stesso in web/src/lib/guardare.ts.
 */
object Thumbs {
    /** Ha senso chiederla: non è un'immagine o un mockup con l'immagine (si vedono già), non è un video. */
    fun wants(i: ResultItem): Boolean = Guardare.thumb(i) == null && Guardare.kind(i.kind) != Guardare.Kind.VIDEO

    /** La chiave della cache: una consegna rifatta (versione nuova, file sostituito) ha un'anteprima nuova. */
    fun key(i: ResultItem): String = "${i.id}:${i.version}:${i.at}:${i.replaced == true}"

    /** La risposta dell'op: l'anteprima, o null per un rifiuto (id sconosciuto) o una risposta che non si legge. */
    fun read(ok: Boolean, text: String): ThumbPreview? =
        if (!ok) null else runCatching { ContractJson.json.decodeFromString(ThumbPreview.serializer(), text) }.getOrNull()

    /** Cosa mostrare nella casella. */
    sealed interface Shown {
        data class Image(val bytes: ByteArray) : Shown
        data class Lines(val text: String) : Shown
        data object Icon : Shown
    }

    fun shown(p: ThumbPreview?): Shown = when {
        p == null -> Shown.Icon
        p.kind == "image" && !p.data.isNullOrBlank() -> runCatching { Shown.Image(java.util.Base64.getDecoder().decode(p.data)) }.getOrElse { Shown.Icon }
        p.kind == "text" && !p.text.isNullOrBlank() -> Shown.Lines(p.text.lines().take(MAX_LINES).joinToString("\n").take(MAX_CHARS))
        else -> Shown.Icon
    }

    const val MAX_LINES = 20
    const val MAX_CHARS = 1200
}
