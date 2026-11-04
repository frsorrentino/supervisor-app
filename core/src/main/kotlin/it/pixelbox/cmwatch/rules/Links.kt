package it.pixelbox.cmwatch.rules

/**
 * I link nei testi della chat, da rendere toccabili (Franz, 01/10 20:05: il link del mockup nella risposta non si
 * apriva). Solo `http(s)://`: un dominio senza schema resta testo. La punteggiatura in coda (punto, virgola, «»,
 * parentesi chiusa senza la sua aperta, gli asterischi del grassetto) non fa parte del link.
 */
object Links {
    private val URL = Regex("""https?://[^\s<>"«»\[\]{}]+""")
    private const val TRAILING = ".,;:!?'’\"»)*"

    fun find(text: String): List<IntRange> = URL.findAll(text).mapNotNull { m ->
        var end = m.range.last
        while (end > m.range.first && text[end] in TRAILING) {
            val url = text.substring(m.range.first, end + 1)
            // Una ) chiusa da una ( dentro il link ne fa parte: Roma_(città).
            if (text[end] == ')' && url.count { it == '(' } >= url.count { it == ')' }) break
            end--
        }
        (m.range.first..end).takeIf { end - m.range.first >= "http://x".length - 1 }
    }.toList()

    /** Un link a un file da scaricare: il nome che avrà nei download e se è un APK da aprire nell'installatore. */
    data class Download(val name: String, val apk: Boolean)

    /**
     * app-download-fuori-scheda-04-11 (Franz, 04/11 07:15: «i download da GitHub si fermano al 100%»): un link a un file (APK,
     * video, audio, archivio) o a un allegato di una release GitHub non va al browser, che lo lasciava fermo al 100%, ma al
     * gestore dei download di Android. Le pagine, PDF e immagini compresi, restano al browser. Solo `http(s)`.
     */
    fun download(url: String): Download? {
        val m = Regex("""^https?://[^/?#]+(/[^?#]*)?""", RegexOption.IGNORE_CASE).find(url.trim()) ?: return null
        val path = m.groupValues[1]
        val name = path.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: return null
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext !in FILE_EXT && !path.contains("/releases/download/")) return null
        return Download(name, ext == "apk")
    }

    private val FILE_EXT = setOf("apk", "aab", "mp4", "mov", "webm", "mkv", "m4v", "mp3", "m4a", "wav", "ogg", "zip", "tar", "gz", "tgz", "7z", "rar", "xz", "dmg", "exe", "msi")
}
