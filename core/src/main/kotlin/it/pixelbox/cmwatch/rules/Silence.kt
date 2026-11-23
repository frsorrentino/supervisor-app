package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.WaitItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * app-silenzio-assenso-23-11 (Franz 23/11 13:20 e 13:23): una carta di ok o di prova, di un cliente o di Franz, si può dare per risolta
 * se nessuno risponde entro una scadenza (3 giorni). Contratto 1.159: `silence_until` sulle voci di attese, l'op `silence` col tasto
 * «Dai per risolto se tace», `silence` nei chiusi. Chiuso così non è mai un ok di Franz. Come silence.ts.
 */
object Silence {
    /** La durata del tasto: 3 giorni, quella che il relay mette col testo vuoto. */
    const val DEFAULT = "3d"

    /**
     * Il tasto c'è su una carta di ok o di prova aperta, con un compito, se il relay ha l'op. Mai su una scelta fra opzioni né su una
     * carta che nomina la produzione (release, push, deploy, pagamenti e invii il relay li rifiuta comunque, e lo dice).
     */
    fun offer(w: WaitItem, ops: List<String>?): Boolean =
        ops?.contains("silence") == true && w.task != null && Attese.isOpen(w) &&
            (w.kind == "ok" || w.kind == "prova") && w.label != "choose" && w.options.isNullOrEmpty() &&
            w.actions.orEmpty().none { it.style == "option" } && w.card?.deploy != true

    /** La scadenza c'è e non è passata: la carta dice quando si chiude da sola. Dopo la scadenza esce alla prossima passata del relay. */
    fun pending(until: Long?, now: Long): Boolean = until != null && until > now

    /** «giovedì 26/11 alle 13:30» (in inglese «Thursday 26/11 at 13:30»): la data intera della carta. */
    fun whenLong(until: Long, zone: ZoneId, locale: Locale): String {
        val t = Instant.ofEpochSecond(until).atZone(zone)
        val day = t.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val at = if (locale.language == "en") "at" else "alle"
        return "$day ${t.dayOfMonth}/${t.monthValue} $at ${"%02d:%02d".format(t.hour, t.minute)}"
    }

    /** «gio 26/11»: la data corta della riga di Per te. */
    fun whenShort(until: Long, zone: ZoneId, locale: Locale): String {
        val t = Instant.ofEpochSecond(until).atZone(zone)
        return "${t.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")} ${t.dayOfMonth}/${t.monthValue}"
    }
}
