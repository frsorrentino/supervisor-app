package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.WaitItem

/**
 * Le regole delle otto animazioni scelte da Franz il 26/11 alle 11:50 (app-animazioni-otto-26-11, proposta in
 * docs/proposte/2026-11-26-animazioni/proposta.md): i tempi comuni e le decisioni che non sono disegno, uguali sul web
 * (web/src/lib/animazioni.ts). Il movimento vero lo fanno CmMotion sul telefono e le variabili --spring-* sul web.
 */
object Animazioni {
    /** 1, Copia che risponde: quanto resta la spunta «Copiato» prima di tornare Copia. */
    const val COPY_HOLD_MS = 1600L

    /** 6, Tieni premuto: quanto si tiene il dito sul tasto di un'approvazione di produzione. */
    const val HOLD_MS = 600L

    /** 3, Al lavoro: il ciclo del riflesso, lo stesso della linea del lavoro in corso (voce 1 del 31/10). */
    const val CYCLE_MS = 1600

    /** 6: solo le approvazioni che escono in produzione chiedono di tenere premuto; le altre restano a un tocco. */
    fun needsHold(a: Approval?): Boolean = a?.deploy == true

    fun needsHold(w: WaitItem?): Boolean = w?.card?.deploy == true

    /**
     * 4, Cifra che scorre: la cifra scorre solo quando cambia per un gesto di Franz (una risposta appena data), mai per un
     * aggiornamento dal PC (regola del 31/10: niente conteggi che scorrono sui dati dei push).
     */
    fun rolls(prev: Int?, next: Int, byGesture: Boolean): Boolean = byGesture && prev != null && prev != next

    /**
     * 2, Lettura che segue la voce: le parole di un paragrafo come intervalli di caratteri del testo mostrato, nell'ordine.
     * Una parola è una corsa di caratteri senza spazi.
     */
    fun words(text: String): List<IntRange> = Regex("\\S+").findAll(text).map { it.range }.toList()

    /**
     * 2: quale parola del paragrafo sta dicendo la voce. Il motore dice il carattere da cui parte dentro il pezzo che legge
     * (`onRangeStart` su Android, `boundary` sul web); il pezzo è il testo pulito di `SpeechText`, non quello mostrato, quindi
     * si contano le parole: quelle dei pezzi già letti dello stesso paragrafo (`before`) più quelle del pezzo prima del punto.
     * Torna l'indice della parola, limitato alle parole mostrate; null se il paragrafo non ne ha.
     */
    fun wordIndex(spokenChunk: String, offset: Int, before: Int, shownWords: Int): Int? {
        if (shownWords <= 0) return null
        val ws = words(spokenChunk)
        // La parola che contiene il punto, o la prima dopo uno spazio; oltre la fine, l'ultima.
        val at = ws.indexOfFirst { offset <= it.last }.let { if (it < 0) (ws.size - 1).coerceAtLeast(0) else it }
        return (before + at).coerceIn(0, shownWords - 1)
    }

    /** 2: quante parole ha un pezzo letto, per il conto dei pezzi successivi dello stesso paragrafo. */
    fun wordCount(text: String): Int = Regex("\\S+").findAll(text).count()
}
