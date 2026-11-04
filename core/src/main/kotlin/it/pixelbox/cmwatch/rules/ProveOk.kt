package it.pixelbox.cmwatch.rules

/**
 * L'esito dell'Ok su una prova (app-prova-ok-sparisce-04-11, Franz 04/11 18:18: «ho dato più volte l'ok alle voci del prova, ma
 * continuo a vederle sempre lì»): la scheda sparisce appena l'Ok è mandato e torna solo se il relay rifiuta per un motivo vero.
 * «Già confermata» o «non c'è più» vuol dire che la prova è fatta (un altro tocco, un altro dispositivo); senza risposta in tempo
 * l'Ok è comunque partito, e la lista che si rilegge dice la verità. Le stesse regole di web/src/lib/decide.ts (`proveDone`).
 */
object ProveOk {
    private val DONE = Regex("""già confermata|non c'è più|already (confirmed|gone|done)|no longer""", RegexOption.IGNORE_CASE)

    /** [ok] null: nessun risultato in tempo. */
    fun done(ok: Boolean?, text: String?): Boolean = ok != false || DONE.containsMatchIn(text.orEmpty())
}
