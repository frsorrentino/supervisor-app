package it.pixelbox.cmwatch.rules

/**
 * Le azioni su un file della chat (Franz, 07/10 15:32): apri, scarica, copia, condividi. «Copia» mette negli appunti il testo
 * dei file di testo e l'immagine delle immagini; sugli altri (PDF, archivi, video) non c'è (Franz, 15/10 22:19: «Nascondi
 * Copia dove non copia il file»): quasi tutte le app incollavano solo l'indirizzo. Restano Condividi e Scarica. Stessa regola
 * di fileActions.ts nella web.
 */
object FileActions {
    enum class CopyKind { TEXT, IMAGE, NONE }

    /**
     * Franz, 16/10 11:18: le anteprime HTML e SVG come pagina, in una WebView separata. JavaScript sì; nessuna interfaccia JS,
     * niente file e content, niente cookie, niente storage, niente navigazione fuori dalla pagina e niente finestre: la pagina
     * non raggiunge l'app né il token. Lo stesso riquadro della web (PAGE_SANDBOX, iframe senza allow-same-origin).
     */
    data class PageView(
        val javaScript: Boolean, val jsInterfaces: Boolean, val fileAccess: Boolean, val contentAccess: Boolean,
        val cookies: Boolean, val domStorage: Boolean, val navigation: Boolean, val windows: Boolean,
    )
    val PAGE_VIEW = PageView(javaScript = true, jsInterfaces = false, fileAccess = false, contentAccess = false, cookies = false, domStorage = false, navigation = false, windows = false)

    /** Il documento della WebView: l'HTML com'è, un SVG dentro una pagina senza margini. */
    fun pageDoc(mime: String?, text: String): String =
        if (mime?.substringBefore(';')?.trim()?.lowercase() == "image/svg+xml") "<!doctype html><body style=\"margin:0\">$text" else text

    private val TEXTUAL = Regex("""/(json|xml|javascript|x-sh|x-yaml|yaml|toml)$|\+(xml|json)$""")

    fun copyKind(mime: String?): CopyKind {
        val m = mime?.substringBefore(';')?.trim()?.lowercase() ?: return CopyKind.NONE
        return when {
            m.startsWith("text/") || TEXTUAL.containsMatchIn(m) -> CopyKind.TEXT
            m.startsWith("image/") -> CopyKind.IMAGE
            else -> CopyKind.NONE
        }
    }

    /** Il tasto «Copia» c'è solo dove copia davvero il contenuto. */
    fun canCopy(mime: String?): Boolean = copyKind(mime) != CopyKind.NONE
}
