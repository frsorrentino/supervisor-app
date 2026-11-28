package it.pixelbox.cmwatch.rules

import kotlin.math.max

/**
 * app-barra-stato-28-11 (Franz 28/11 14:07, variante C di dalla-master/barra-stato-28-11.html): una sola barra di stato, alta 28 dp con
 * la riga sottile sotto, subito sotto il titolo delle sessioni e del foglio aperto della master, al posto della riga di icone. A sinistra
 * Per te, Risultati e Modifiche col loro numero (a zero solo l'icona), che fanno scendere la loro tendina; a destra modello · effort, la
 * quota delle 5 ore come batteria e il contesto come anellino, che aprono Dettagli. Qui le regole che non sono disegno, uguali sul web
 * (web/src/lib/statusBar.ts): quanto resta della quota, i colori, e quanto si stringe la barra con poco spazio («ok c ma quando c'è poco
 * spazio deve ridursi anche il testo», «o comunque deve disporsi meglio»): il testo non va mai a capo, non si taglia e non esce dai glifi.
 * Le misure sono in dp, come i px CSS del web.
 */
object StatusBar {
    /**
     * I passi, dal più largo: tutto («Opus 5 · high», batteria e anellino con la loro percentuale accanto); il testo più piccolo; il
     * modello senza effort; le percentuali dentro i glifi; solo i glifi. I numeri delle icone di sinistra restano a ogni passo.
     */
    enum class Step { FULL, SMALL, NO_EFFORT, INSIDE, GLYPHS }

    /** Geist Mono: ogni carattere è largo 0,6 della sua altezza (misurato nel browser, uguale a 400 e a 500). */
    const val ADV = 0.6f

    /** L'altezza della barra; sotto, la riga sottile da 1. */
    const val BAR_DP = 28

    /**
     * Il margine a sinistra e a destra, e lo spazio minimo fra le icone e lo stato. Nel foglio della master i margini sono 16, come le sue
     * schede (app-foglio-master-testata-28-11: «il padding esiguo tra testo e bordo»).
     */
    const val PAD = 12f
    const val PAD_SHEET = 16f
    const val SIDES_GAP = 12f

    /** Le icone di sinistra (17), lo spazio fra un'icona e il suo numero (3) e fra due icone (10); il numero, sempre a 11,5 sp. */
    const val ICON = 17f
    const val NUM_GAP = 3f
    const val ICON_GAP = 10f
    const val NUM = 11.5f

    /** Il testo di destra: 10,5 sp, e 9 dal passo SMALL. */
    const val TEXT = 10.5f
    const val TEXT_SMALL = 9f

    /** Fra modello, batteria e anellino (8); fra un glifo e la sua percentuale (3). */
    const val ITEM_GAP = 8f
    const val GLYPH_GAP = 3f

    /** La batteria, 19 più la punta da 3,5, e l'anellino da 12; col numero dentro la batteria è larga 24 più la punta, l'anellino 18. */
    const val BAT = 22.5f
    const val RING = 12f
    const val BAT_IN = 27.5f
    const val RING_IN = 18f

    /** Il numero dentro i glifi: 7,5 dp, fisso come i glifi, così non ne esce mai, anche col testo grande. */
    const val IN_TEXT = 7.5f

    /** Un margine per gli arrotondamenti del disegno: meglio un passo prima che un testo che tocca il bordo. */
    const val SAFE = 6f

    /** Quanto resta della quota delle 5 ore, come la batteria del telefono: 100 meno l'usato, fra 0 e 100. */
    fun remaining(used: Int): Int = (100 - used).coerceIn(0, 100)

    enum class Tone { NEUTRAL, WARN, ALERT }

    /** La batteria diventa ambra quando resta il 30 % o meno, rossa col 10 % o meno. */
    const val BAT_WARN = 30
    const val BAT_ALERT = 10

    fun batteryTone(left: Int): Tone = when {
        left <= BAT_ALERT -> Tone.ALERT
        left <= BAT_WARN -> Tone.WARN
        else -> Tone.NEUTRAL
    }

    /** L'anellino del contesto è ambra dal 70 %, come il chip «Contesto N% · Handoff» (SessionBottom.CHIP_FROM). */
    fun ringTone(ctx: Int): Tone = if (ctx >= SessionBottom.CHIP_FROM) Tone.WARN else Tone.NEUTRAL

    /** Quello che la barra scrive a destra: modello · effort ai primi due passi, poi il modello solo; coi soli glifi niente. */
    fun modelText(model: String?, effort: String?, step: Step): String = when (step) {
        Step.GLYPHS -> ""
        Step.FULL, Step.SMALL -> listOfNotNull(model, effort).filter { it.isNotEmpty() }.joinToString(" · ")
        else -> model.orEmpty()
    }

    /**
     * Quello che la barra mostra: i numeri di Per te, Risultati e Modifiche nell'ordine (a zero solo l'icona); modello ed effort; quanto
     * resta della quota delle 5 ore ([remaining], null senza quota); il contesto in % (null senza).
     */
    data class Parts(val counts: List<Int>, val model: String?, val effort: String?, val left: Int?, val ctx: Int?)

    private fun mono(chars: Int, size: Float, scale: Float): Float = chars * ADV * size * scale

    private fun pct(n: Int): String = "$n%"

    /** La larghezza delle icone di sinistra coi loro numeri. */
    fun leftWidth(counts: List<Int>, scale: Float = 1f): Float = counts.foldIndexed(0f) { i, w, n ->
        w + (if (i > 0) ICON_GAP else 0f) + ICON + (if (n > 0) NUM_GAP + mono(n.toString().length, NUM, scale) else 0f)
    }

    /** La larghezza dello stato di destra al passo [step]. */
    fun rightWidth(step: Step, p: Parts, scale: Float = 1f): Float {
        val size = if (step == Step.FULL) TEXT else TEXT_SMALL
        val w = mutableListOf<Float>()
        val m = modelText(p.model, p.effort, step)
        if (m.isNotEmpty()) w += mono(m.length, size, scale)
        val glyph = { base: Float, inside: Float, n: Int ->
            when (step) {
                Step.INSIDE -> inside
                Step.GLYPHS -> base
                else -> base + GLYPH_GAP + mono(pct(n).length, size, scale)
            }
        }
        p.left?.let { n -> w += glyph(BAT, BAT_IN, n) }
        p.ctx?.let { n -> w += glyph(RING, RING_IN, n) }
        return w.sum() + max(0, w.size - 1) * ITEM_GAP
    }

    /** Quanto deve essere larga la barra per il passo [step]: margini, icone coi numeri, lo spazio fra i due lati, lo stato. */
    fun needs(step: Step, p: Parts, scale: Float = 1f, pad: Float = PAD): Float = pad * 2 + leftWidth(p.counts, scale) + SIDES_GAP + SAFE + rightWidth(step, p, scale)

    /**
     * Il passo della barra larga [width] dp con la scala del testo di sistema [scale]: il primo che ci sta per intero; se non ci sta
     * niente, solo i glifi. Con «Opus 5 · high», quota e contesto a due cifre e tre numeri a una cifra, a scala 1: tutto da 319 dp, il
     * testo piccolo da 302, il modello senza effort da 264, le percentuali dentro da 237, sotto solo i glifi. A scala 1,3 tutto da 361:
     * a 360 il testo piccolo.
     */
    fun step(width: Float, p: Parts, scale: Float = 1f, pad: Float = PAD): Step = Step.entries.firstOrNull { s -> needs(s, p, scale, pad) <= width } ?: Step.GLYPHS
}
