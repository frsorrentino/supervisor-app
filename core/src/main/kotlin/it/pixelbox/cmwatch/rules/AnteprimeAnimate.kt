package it.pixelbox.cmwatch.rules

/**
 * Le anteprime che si muovono (app-anteprime-webp-27-11, Franz 27/11 20:49: «ovunque ci siano delle anteprime che devono mostrare
 * un'animazione, queste siano animate in loop o a tocco»). Un WebP o una GIF animati, allegati a una carta o a un messaggio, girano in
 * loop finché sono a schermo e si fermano fuori; con le animazioni ridotte resta il primo fotogramma col ▶, e il tocco li apre a schermo
 * intero, dove girano. Accanto all'HTML di una bozza la master e le sessioni mandano il suo WebP animato con lo stesso nome
 * (entrata-home.webp ed entrata-home.html): nella carta o nel messaggio si vede il WebP, il tocco apre la bozza dal vivo. Il relay dà i
 * byte originali del file (`file`, contratto 1.34 e 1.35): niente miniature ferme. Le stesse regole in web/src/lib/anteprimeAnimate.ts,
 * coi casi di core/src/test/resources/anteprime-animate-cases.json.
 */
object AnteprimeAnimate {
    private val EXT = Regex("""\.(webp|gif)$""", RegexOption.IGNORE_CASE)
    private val DRAFT = Regex("""\.html?$""", RegexOption.IGNORE_CASE)

    /** Un'immagine che può muoversi: WebP o GIF per tipo, o per estensione quando il tipo manca o è generico. */
    fun capable(mime: String?, path: String): Boolean {
        val m = mime?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return when {
            m == "image/webp" || m == "image/gif" -> true
            m.isEmpty() || m == "application/octet-stream" -> EXT.containsMatchIn(path)
            else -> false
        }
    }

    /**
     * Si muove davvero, dai byte del file: un WebP col bit dell'animazione nell'intestazione estesa (VP8X) o una GIF con almeno due
     * fotogrammi. Un WebP o una GIF fermi restano immagini come le altre, senza ▶.
     */
    fun moves(b: ByteArray): Boolean = webpAnimated(b) || gifFrames(b) >= 2

    /** Gira solo a schermo e senza le animazioni ridotte; altrimenti resta il primo fotogramma. */
    fun plays(visible: Boolean, reduced: Boolean): Boolean = visible && !reduced

    /**
     * La bozza HTML che va con un'immagine che può muoversi: lo stesso nome senza l'ultima estensione (maiuscole a parte), .html o
     * .htm, fra i file dello stesso elenco; la prima se sono più d'una. Null se non c'è, o se l'immagine è ferma per tipo (un PNG
     * accanto al suo HTML resta un'immagine che si ingrandisce).
     */
    fun draftOf(image: String, mime: String?, files: List<String>): String? {
        if (!capable(mime, image)) return null
        val stem = stemOf(image) ?: return null
        return files.firstOrNull { p -> p != image && DRAFT.containsMatchIn(p) && stemOf(p).equals(stem, ignoreCase = true) }
    }

    /** L'immagine che può muoversi accanto a una bozza HTML (percorso e tipo dei file dello stesso elenco): la sua anteprima leggera. */
    fun previewOf(draft: String, files: List<Pair<String, String?>>): String? {
        if (!DRAFT.containsMatchIn(draft)) return null
        val stem = stemOf(draft) ?: return null
        return files.firstOrNull { (p, m) -> p != draft && capable(m, p) && stemOf(p).equals(stem, ignoreCase = true) }?.first
    }

    private fun stemOf(path: String): String? =
        path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.').takeIf { it.isNotEmpty() }

    private fun ByteArray.has(at: Int, s: String): Boolean = size >= at + s.length && s.indices.all { this[at + it] == s[it].code.toByte() }

    // RIFF....WEBP, poi il pezzo VP8X: il byte delle opzioni ha l'animazione nel secondo bit (0x02).
    private fun webpAnimated(b: ByteArray): Boolean =
        b.size > 20 && b.has(0, "RIFF") && b.has(8, "WEBP") && b.has(12, "VP8X") && (b[20].toInt() and 0x02) != 0

    /** I fotogrammi di una GIF, fermandosi al secondo: blocchi di estensione saltati, descrittori d'immagine contati. */
    private fun gifFrames(b: ByteArray): Int {
        if (b.size < 13 || !(b.has(0, "GIF87a") || b.has(0, "GIF89a"))) return 0
        val screen = b[10].toInt() and 0xFF
        var i = 13 + if (screen and 0x80 != 0) 3 * (1 shl ((screen and 0x07) + 1)) else 0
        var frames = 0
        // I sotto-blocchi di dati: un byte di lunghezza e i suoi byte, fino a quello lungo zero; false se il file finisce prima.
        fun skipBlocks(): Boolean {
            while (i < b.size) {
                val n = b[i].toInt() and 0xFF
                i += 1
                if (n == 0) return true
                i += n
            }
            return false
        }
        while (i < b.size && frames < 2) {
            when (b[i].toInt() and 0xFF) {
                0x21 -> { i += 2; if (!skipBlocks()) break }
                0x2C -> {
                    if (i + 10 > b.size) break
                    val local = b[i + 9].toInt() and 0xFF
                    i += 10 + if (local and 0x80 != 0) 3 * (1 shl ((local and 0x07) + 1)) else 0
                    frames++
                    // Il byte della misura del codice LZW, poi i dati del fotogramma.
                    i += 1
                    if (!skipBlocks()) break
                }
                else -> break
            }
        }
        return frames
    }
}
