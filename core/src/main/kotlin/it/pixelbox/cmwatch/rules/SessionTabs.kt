package it.pixelbox.cmwatch.rules

import kotlin.math.max
import kotlin.math.min

/**
 * La testata della sessione con la riga di icone e la tendina (app-posizioni-sistema-27-11, gruppo 2; proposta in
 * dalla-master/posizioni-2026-11-27, «La riga di icone» e la tabella di «Cosa sta in basso»). Le schede sono sempre cinque, nello
 * stesso ordine: Chat · Per te · Risultati · Modifiche · Dettagli. Qui le regole che non sono disegno, uguali sul web
 * (web/src/lib/sessionTabs.ts): i numeri delle schede dagli stessi dati delle pillole e della riga dei dati di prima, il tocco che
 * apre o chiude la tendina, i tempi del centro notifiche (360 ms FAST_OUT_SLOW_IN, 32 ms fra una riga e l'altra), la tendina tirata
 * su. La curva in Compose è FastOutSlowInEasing.
 */
object SessionTabs {
    /** Le schede, con la chiave della riga di icone. */
    enum class Key(val id: String) { CHAT("chat"), PERTE("perte"), RISULTATI("risultati"), MODIFICHE("modifiche"), DETTAGLI("dettagli") }

    /** L'ordine delle schede, che non cambia mai: Alt+1…5 le sceglie in quest'ordine. */
    val ORDER: List<Key> = listOf(Key.CHAT, Key.PERTE, Key.RISULTATI, Key.MODIFICHE, Key.DETTAGLI)

    /** La scheda di una chiave della riga di icone; null se non è una scheda della sessione. */
    fun keyOf(id: String): Key? = ORDER.firstOrNull { it.id == id }

    /** I numeri delle schede con un numero; Chat e Dettagli non ne hanno. */
    data class Counts(val perTe: Int, val risultati: Int, val modifiche: Int)

    /**
     * I numeri delle schede, dagli stessi dati che usavano le pillole e la riga dei dati: Per te = le voci che in questa sessione
     * aspettano Franz più i suoi avvisi (la pillola «Per te» e la pillola «Avvisi»); Risultati = i risultati nuovi, cioè i lavori con
     * l'ultima versione non vista; Modifiche = i file cambiati (la pillola «4 file · +8 −2»). Mai negativi.
     */
    fun counts(voci: Int, notes: Int, newResults: Int, files: Int?): Counts =
        Counts(perTe = max(0, voci) + max(0, notes), risultati = max(0, newResults), modifiche = max(0, files ?: 0))

    /** Gli avvisi della sessione, come la pillola «Avvisi»: l'avviso delle sessioni --bg (contratto 1.146) e la nota di Claude Code. */
    fun notes(bg: Boolean, headsUp: Boolean): Int = (if (bg) 1 else 0) + (if (headsUp) 1 else 0)

    /** I risultati nuovi di una sessione: i lavori con l'ultima versione non vista (il puntino della riga «Risultati»). */
    fun newResults(unseen: List<Boolean>): Int = unseen.count { it }

    /** Il colore del numero di una scheda: ambra chi aspetta Franz, verde un risultato nuovo, grigio il conteggio dei file. */
    fun tone(key: Key): IconRowRules.Tone = when (key) {
        Key.PERTE -> IconRowRules.Tone.WAIT
        Key.RISULTATI -> IconRowRules.Tone.NEW
        else -> IconRowRules.Tone.COUNT
    }

    /** Il numero di una scheda; 0 per Chat e Dettagli, che non ne hanno (a zero la riga non lo mostra). */
    fun count(key: Key, c: Counts): Int = when (key) {
        Key.PERTE -> c.perTe
        Key.RISULTATI -> c.risultati
        Key.MODIFICHE -> c.modifiche
        else -> 0
    }

    /**
     * Il tocco su una scheda (o Alt+1…5): la scheda aperta toccata di nuovo, o Chat, chiude la tendina e torna alla conversazione; un'altra
     * la apre, o prende il posto di quella aperta. Una chiave sconosciuta lascia le cose come stanno.
     */
    fun pick(open: Key, tapped: String): Key {
        val t = keyOf(tapped) ?: return open
        return if (t == open || t == Key.CHAT) Key.CHAT else t
    }

    /** La tendina è aperta su ogni scheda tranne Chat. */
    fun shadeOpen(key: Key): Boolean = key != Key.CHAT

    /** Il centro notifiche (docs/proposte/2026-11-26-centro-notifiche/fonti.md): ANIMATION_DURATION_STANDARD 360 ms, FAST_OUT_SLOW_IN. */
    const val SHADE_MS = 360

    /** Fra una riga e l'altra della tendina, come le righe del centro notifiche: 32 ms. */
    const val ROW_MS = 32

    /** Dopo tante righe il ritardo non cresce più: le ultime di una lista lunga non aspettano. */
    const val MAX_STAGGER = 8

    /** Il ritardo della riga [i] (da 0) mentre la tendina scende. */
    fun rowDelay(i: Int): Int = min(max(0, i), MAX_STAGGER) * ROW_MS

    /**
     * app-barra-stato-28-11 (Franz 28/11 14:11, «quando selezioni per te non è necessario che la tendina arrivi fino in fondo, serve che
     * si apra fino dove c'è contenuto»): la tendina è alta quanto il suo contenuto più la maniglia, mai oltre il posto fino al campo
     * ([room]); solo allora arriva al campo e il contenuto scorre dentro. Sotto una tendina corta la chat resta in vista.
     */
    fun shadeHeight(content: Int, grip: Int, room: Int): Int = max(0, min(max(0, content) + max(0, grip), room))

    /** Tirata su oltre questa parte della sua altezza, la tendina si chiude. */
    const val DRAG_CLOSE = 0.25f

    /** O con uno slancio verso l'alto oltre questa velocità (px CSS al secondo sul web, dp al secondo sul telefono). */
    const val FLING = 800f

    /** Lo spostamento della tendina col dito: solo verso l'alto, mai oltre la sua altezza. */
    fun dragOffset(dy: Float, height: Float): Float = dy.coerceIn(-max(0f, height), 0f)

    /** Al rilascio: la tendina tirata su ([dy] negativo) si chiude oltre un quarto della sua altezza o con uno slancio verso l'alto. */
    fun closesOnDrag(dy: Float, velocity: Float, height: Float): Boolean {
        if (dy >= 0f) return false
        return -dy >= max(0f, height) * DRAG_CLOSE || velocity <= -FLING
    }
}
