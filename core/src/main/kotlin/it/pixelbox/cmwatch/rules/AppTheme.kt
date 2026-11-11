package it.pixelbox.cmwatch.rules

/**
 * Il tema scelto nelle impostazioni (app-tema-giorno-notte-11-11, Franz 11/11 12:31): Automatico segue il sistema come prima, Giorno e
 * Notte lo fissano. La scelta resta sul dispositivo. Lo stesso in web/src/lib/theme.ts.
 */
object AppTheme {
    enum class Choice(val key: String) { AUTO("auto"), DAY("day"), NIGHT("night") }

    /** Dalla chiave salvata; una chiave vuota o sconosciuta vale Automatico. */
    fun fromKey(key: String?): Choice = Choice.entries.firstOrNull { it.key == key } ?: Choice.AUTO

    /** Giorno o notte per una scelta, dato il tema del sistema. */
    fun day(choice: Choice, systemDark: Boolean): Boolean = when (choice) {
        Choice.AUTO -> !systemDark
        Choice.DAY -> true
        Choice.NIGHT -> false
    }
}
