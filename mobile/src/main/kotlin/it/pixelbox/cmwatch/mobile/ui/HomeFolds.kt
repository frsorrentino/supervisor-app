package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cosa è aperto nella home e nella plancia (app-frecce-plancia-tablet-07-11, Franz 07/11 17:00: «verifica il funzionamento delle
 * frecce riapertura e chiusura schede da tablet plancia, non mi pare funzionino»). Lo tiene l'Activity, sopra la lista: la scheda
 * aperta e le sezioni restano come le hai lasciate quando la lista si rifà, quando una sessione cambia gruppo (al lavoro, ferma,
 * finita) e fra plancia e home con le sessioni aperte. La scheda si riconosce dal nome della sessione, non dal gruppo: col gruppo
 * nella chiave una sessione che cambiava stato si richiudeva da sola.
 */
class HomeFolds(card: String? = null) {
    /** La scheda di sessione aperta, una alla volta. */
    var card by mutableStateOf(card)
    /** Il filtro acceso dal tabellone (app-tabellone-filtri-14-11): resta finché l'app è aperta, non si ricorda fra un avvio e l'altro. */
    var boardFilter by mutableStateOf<it.pixelbox.cmwatch.rules.Kpi.BoardFilter?>(null)
    private val states = HashMap<String, MutableState<Boolean>>()

    fun isOpen(session: String): Boolean = card == session
    fun toggle(session: String) { card = if (card == session) null else session }

    /** Una sezione richiudibile: la prima volta col suo valore di partenza, poi come l'ha lasciata il tocco. */
    fun state(key: String, start: Boolean): MutableState<Boolean> = states.getOrPut(key) { mutableStateOf(start) }

    companion object {
        /** `initiallyOpen` dei test e dei link, «GRUPPO:nome» o il nome solo; il nome può avere i due punti («win:supervisor-app»). */
        fun cardOf(initiallyOpen: String?): String? = initiallyOpen?.let { k ->
            val head = k.substringBefore(':')
            if (k.contains(':') && head.isNotEmpty() && head.all { c -> c.isUpperCase() || c == '_' }) k.substringAfter(':') else k
        }
    }
}
