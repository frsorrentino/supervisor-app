package it.pixelbox.cmwatch.rules

/**
 * Quali file della chat si scaricano da soli per l'anteprima (stessa regola di media.ts nella web). Le immagini fino a
 * 10 MB (Franz, 10/10 09:54: «vorrei che nascessero già in anteprima»), i video fino a 25 MB solo su una rete senza
 * contatore, come il Wi-Fi di casa (10/10 15:24). Dal 15/10 22:20 («ogni file allegato mostri già l'anteprima con width
 * 100%») anche i PDF fino a 10 MB, con la prima pagina, e i testi fino a 1 MB, con le prime righe, ma non su una rete a
 * contatore: lì aspettano «Mostra anteprima». Gli altri file hanno una scheda larga, senza scaricare niente.
 */
object MediaPreview {
    const val IMAGE_AUTO_MAX = 10_000_000L
    /** Il tetto dei file a pezzi dal PC (contratto 1.34): oltre, il PC li rifiuta comunque. */
    const val VIDEO_AUTO_MAX = 26_214_400L
    const val PDF_AUTO_MAX = 10_000_000L
    const val TEXT_AUTO_MAX = 1_000_000L
    /** Le righe di un testo nell'anteprima. */
    const val TEXT_LINES = 12

    enum class Kind { IMAGE, VIDEO, PDF, TEXT, OTHER }

    private val TEXTUAL = Regex("""/(json|xml|javascript|x-sh|x-yaml|yaml|toml)$|\+(xml|json)$""")
    private val TEXT_EXT = Regex("""\.(md|markdown|txt|log|csv|tsv|json|ya?ml|toml|ini|sh|py|ts|js|mjs|kt|kts|java|go|rs|rb|svelte|css|scss|sql|xml|env)$""", RegexOption.IGNORE_CASE)

    fun kind(mime: String?, path: String = ""): Kind {
        val m = mime?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return when {
            m.startsWith("image/") && m != "image/svg+xml" -> Kind.IMAGE
            m.startsWith("video/") -> Kind.VIDEO
            m == "application/pdf" || (m.isEmpty() && path.endsWith(".pdf", ignoreCase = true)) -> Kind.PDF
            m.startsWith("text/") || TEXTUAL.containsMatchIn(m) -> Kind.TEXT
            (m.isEmpty() || m == "application/octet-stream") && TEXT_EXT.containsMatchIn(path) -> Kind.TEXT
            else -> Kind.OTHER
        }
    }

    fun auto(mime: String?, size: Long?, unmetered: Boolean, path: String = ""): Boolean = when (kind(mime, path)) {
        Kind.IMAGE -> (size ?: 0L) <= IMAGE_AUTO_MAX
        Kind.VIDEO -> unmetered && size != null && size <= VIDEO_AUTO_MAX
        Kind.PDF -> unmetered && (size ?: 0L) <= PDF_AUTO_MAX
        Kind.TEXT -> unmetered && (size ?: 0L) <= TEXT_AUTO_MAX
        Kind.OTHER -> false
    }

    /** Il tasto «Mostra anteprima»: dove un'anteprima c'è ma non è arrivata da sola. */
    fun canPreview(mime: String?, path: String = ""): Boolean = kind(mime, path) != Kind.OTHER

    fun isVideo(mime: String?): Boolean = mime?.startsWith("video/") == true

    /** Le prime righe di un testo per l'anteprima, senza righe vuote in fondo; `more` = ce n'erano altre. */
    fun firstLines(text: String, n: Int = TEXT_LINES): Pair<String, Boolean> {
        val all = text.replace("\r\n", "\n").replace('\r', '\n').split('\n').toMutableList()
        while (all.isNotEmpty() && all.last().isBlank()) all.removeAt(all.lastIndex)
        return all.take(n).joinToString("\n") to (all.size > n)
    }
}
