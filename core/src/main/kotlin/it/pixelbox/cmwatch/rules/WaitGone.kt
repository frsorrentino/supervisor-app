package it.pixelbox.cmwatch.rules

/**
 * Una domanda che non aspetta più (app-domanda-chiusa-25-11, Franz 25/11 16:55: «se una domanda non è più disponibile perché ha avuto
 * risposte, non richiede alcun messaggio di errore»): chiusa, già risposta altrove o sostituita da una carta nuova. Il relay lo dice con
 * `ok: false` e «<id>: this question is no longer waiting (closed or already answered)» (contratto 1.130, fixture …1302), in italiano
 * «questa domanda non aspetta più». Non è un errore: la carta sparisce e il link apre «Già risolta», senza l'id. Gli errori veri (rete,
 * permessi, un'op sconosciuta) restano errori. Le stesse regole di web/src/lib/waitGone.ts.
 */
object WaitGone {
    private val GONE = Regex("""no longer waiting|non aspetta più""", RegexOption.IGNORE_CASE)

    fun gone(text: String?): Boolean = GONE.containsMatchIn(text.orEmpty())
}
