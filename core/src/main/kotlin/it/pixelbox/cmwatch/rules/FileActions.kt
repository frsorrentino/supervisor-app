package it.pixelbox.cmwatch.rules

/**
 * Le azioni su un file della chat (Franz, 07/10 15:32): apri, scarica, copia, condividi. «Copia» mette negli appunti il testo
 * dei file di testo e l'immagine delle immagini; sugli altri (PDF, archivi, video) non c'è (Franz, 15/10 22:19: «Nascondi
 * Copia dove non copia il file»): quasi tutte le app incollavano solo l'indirizzo. Restano Condividi e Scarica. Stessa regola
 * di fileActions.ts nella web.
 */
object FileActions {
    enum class CopyKind { TEXT, IMAGE, NONE }

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
