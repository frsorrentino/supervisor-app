package it.pixelbox.cmwatch.rules

import kotlin.math.roundToInt

/**
 * app-barra-ibrida-28-11 (Franz 28/11, H1 di dalla-master/barra-ibrida-28-11.html; la barra C sottile di app-barra-stato-28-11 era «troppo
 * stretta e non buona esteticamente»): sotto il titolo delle sessioni e del foglio aperto della master la capsula della riga di icone di
 * prima (IconRow: alta 50, la pillola elastica del Rubber dock, il nome sulla scheda aperta, i numeri), con dentro, dopo un separatore
 * sottile, lo stato: modello · effort, la quota delle 5 ore e l'anellino del contesto, che aprono Dettagli. Qui le regole che non sono
 * disegno, uguali sul web (web/src/lib/statusBar.ts): i colori e quanto si riduce lo stato con poco spazio, finché la riga ci sta (come fit()
 * del mockup): prima via l'effort, poi il testo più piccolo, poi il nome del modello (resta in Dettagli), infine le percentuali. Le icone
 * coi numeri restano sempre; mai a capo, mai tagliato. Franz, dal telefono: «non mi piace la batteria, oltre che non capisco cosa
 * rappresenti»: la quota è un orologio e «5h 11%», quanto è usato, come la pillola di Utilizzo; ambra dal 70 %, rossa dal 90 %.
 * Le misure sono in dp, come i px CSS del web.
 */
object StatusBar {
    /** I passi, uno dopo l'altro: tutto; senza effort; il testo più piccolo; senza il modello; senza le percentuali. */
    enum class Step { FULL, NO_EFFORT, SMALL, NO_MODEL, NO_PCT }

    /** Geist Mono: ogni carattere è largo 0,6 della sua altezza (misurato nel browser, uguale a 400 e a 500). */
    const val ADV = 0.6f

    /** Lo stato: 11 sp, e 10 dal passo SMALL; fra modello, quota e anellino 9, poi 6. */
    const val TEXT = 11f
    const val TEXT_SMALL = 10f
    const val ITEM_GAP = 9f
    const val ITEM_GAP_SMALL = 6f

    /** Il separatore prima dello stato: 4 di margine, la riga da 1, 10 dopo; 6 in fondo, prima del bordo della capsula. */
    const val SEP = 15f
    const val END = 6f

    /** L'orologio della quota e l'anellino del contesto, da 12; fra il glifo e il suo testo 3. */
    const val GLYPH = 12f
    const val GLYPH_GAP = 3f

    /** La riga attorno alla capsula: 12 per lato; la capsula: 5 per lato (il bordo da 1 è disegnato dentro). */
    const val ROW_PAD = 12f
    const val CAPSULE_PAD = 5f

    /** Un margine per gli arrotondamenti del disegno: meglio un passo prima che un testo che tocca il bordo. */
    const val SAFE = 2f

    enum class Tone { NEUTRAL, WARN, ALERT }

    /** La quota delle 5 ore, quanto è usato: ambra dal 70 %, rossa dal 90 %. */
    const val QUOTA_WARN = 70
    const val QUOTA_ALERT = 90

    fun quotaTone(used: Int): Tone = when {
        used >= QUOTA_ALERT -> Tone.ALERT
        used >= QUOTA_WARN -> Tone.WARN
        else -> Tone.NEUTRAL
    }

    /** L'anellino del contesto è ambra dal 70 %, come il chip «Contesto N% · Handoff» (SessionBottom.CHIP_FROM). */
    fun ringTone(ctx: Int): Tone = if (ctx >= SessionBottom.CHIP_FROM) Tone.WARN else Tone.NEUTRAL

    /** Il testo della quota, «5h 11%», come la pillola di Utilizzo. */
    fun quotaText(used: Int): String = "5h $used%"

    /** Quello che lo stato scrive del modello: modello · effort; dal secondo passo il modello solo; dal quarto niente (resta in Dettagli). */
    fun modelText(model: String?, effort: String?, step: Step): String = when (step) {
        Step.NO_MODEL, Step.NO_PCT -> ""
        Step.FULL -> listOfNotNull(model, effort).filter { it.isNotEmpty() }.joinToString(" · ")
        else -> model.orEmpty()
    }

    /** Quello che mostra lo stato: modello ed effort; la quota delle 5 ore usata in % (null senza quota); il contesto in % (null senza). */
    data class Parts(val model: String?, val effort: String?, val used: Int?, val ctx: Int?)

    private fun mono(chars: Int, size: Float, scale: Float): Float = chars * ADV * size * scale

    /** La larghezza dello stato al passo [step]: il separatore, modello, quota e anellino coi loro spazi, il margine in fondo. */
    fun statusWidth(step: Step, p: Parts, scale: Float = 1f): Float {
        val small = step != Step.FULL && step != Step.NO_EFFORT
        val size = if (small) TEXT_SMALL else TEXT
        val pct = step != Step.NO_PCT
        val w = mutableListOf<Float>()
        val m = modelText(p.model, p.effort, step)
        if (m.isNotEmpty()) w += mono(m.length, size, scale)
        p.used?.let { u -> w += GLYPH + (if (pct) GLYPH_GAP + mono(quotaText(u).length, size, scale) else 0f) }
        p.ctx?.let { c -> w += GLYPH + (if (pct) GLYPH_GAP + mono("$c%".length, size, scale) else 0f) }
        if (w.isEmpty()) return 0f
        return SEP + END + w.sum() + (w.size - 1) * (if (small) ITEM_GAP_SMALL else ITEM_GAP)
    }

    /** Quanto deve essere larga la riga per il passo [step], con le schede larghe [tabs] (a riposo, col nome più lungo aperto). */
    fun needs(step: Step, tabs: Float, p: Parts, scale: Float = 1f): Float =
        ROW_PAD * 2 + CAPSULE_PAD * 2 + tabs + statusWidth(step, p, scale) + SAFE

    /**
     * Il passo dello stato nella riga larga [width] dp, con le schede larghe [tabs] a riposo col nome più lungo aperto (così il passo non
     * cambia aprendo e chiudendo le schede) e la scala del testo di sistema [scale]: il primo che ci sta per intero; se non ci sta niente,
     * l'ultimo. Con «Opus 5 · high», «5h 11%» e «34%» lo stato è largo 214 a tutto, 168 senza effort, 153 col testo piccolo, 111 senza
     * modello, 51 coi soli glifi. Le tre schede a 360 sono larghe 212 col nome più lungo aperto (Modifiche): la riga vuole 359, resta lo
     * stato senza modello; a 407 il modello col testo piccolo.
     */
    fun step(width: Float, tabs: Float, p: Parts, scale: Float = 1f): Step =
        Step.entries.firstOrNull { s -> needs(s, tabs, p, scale) <= width } ?: Step.NO_PCT

    /** La quota usata del contratto (in %), per [Parts.used]. */
    fun used(h5: Int?): Int? = h5?.let { (it.toFloat()).roundToInt().coerceIn(0, 100) }
}
