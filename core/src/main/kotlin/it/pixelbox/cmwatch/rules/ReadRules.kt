package it.pixelbox.cmwatch.rules

/**
 * app-play-lettura-ovunque-27-11 (Franz 27/11 13:29): quando un testo ha accanto il ▶ della lettura. Solo se il testo misurato va oltre
 * una riga, o se è tagliato ed è più lungo di quello che mostra; su una riga sola niente tasto. Come `overOne` in longText.ts sulla web.
 */
object ReadRules {
    fun isLong(lineCount: Int, clipped: Boolean): Boolean = lineCount > 1 || clipped
}
