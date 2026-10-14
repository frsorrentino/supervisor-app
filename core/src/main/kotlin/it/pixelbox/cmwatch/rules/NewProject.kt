package it.pixelbox.cmwatch.rules

import java.time.LocalTime
import java.time.ZonedDateTime

/** Contratto 1.64, un progetto nuovo da telefono e web: il nome come lo accetta il relay, e la conferma al secondo tocco. */
object NewProject {
    private val NAME = Regex("[A-Za-z0-9._-]{1,64}")

    /** Lettere, cifre, . _ -, al massimo 64 caratteri, non comincia con il punto. */
    fun validName(name: String): Boolean = NAME.matches(name) && !name.startsWith(".")

    /** Il percorso del primo tocco, da «confirm: create <percorso>»; null per gli altri esiti. */
    fun confirmPath(text: String): String? = text.removePrefix(CONFIRM).takeIf { text.startsWith(CONFIRM) }?.trim()

    const val CONFIRM = "confirm: create "
}

/** Contratto 1.63, «Manda alle…»: l'epoch dell'ora scelta, oggi o domani se l'ora è già passata. */
object SendAt {
    fun epoch(time: LocalTime, now: ZonedDateTime): Long {
        val today = now.with(time).withSecond(0).withNano(0)
        return (if (today.isAfter(now)) today else today.plusDays(1)).toEpochSecond()
    }

    /** Il risultato riuscito di un prompt con `at`: «scheduled for HH:MM: …». */
    fun scheduled(text: String): Boolean = text.startsWith("scheduled for ")
}
