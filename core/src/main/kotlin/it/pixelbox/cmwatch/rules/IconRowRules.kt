package it.pixelbox.cmwatch.rules

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * La riga di icone (app-posizioni-sistema-27-11, gruppo 1; proposta in dalla-master/posizioni-2026-11-27, «La riga di icone») con la
 * pillola elastica del Rubber dock (app-dock-elastico-27-11, scelta di Franz del 27/11 20:46; mockup riga-elastica.html e il codice
 * originale fonte-designbookmark/dock.js). Le regole che non sono disegno, uguali sul web (web/src/lib/iconRow.ts): il numero, le
 * molle, la meta e la forma della pillola, l'elastico oltre le estremità, la scheda sotto il dito, i tasti. Le misure sono in px CSS
 * sul web e in dp sul telefono, che le moltiplica per la densità.
 */
object IconRowRules {
    /** Il colore del numero, come nel resto dell'app: ambra se qualcosa aspetta Franz, verde un risultato nuovo, grigio un conteggio. */
    enum class Tone { WAIT, NEW, COUNT }

    /** Il numero accanto all'icona, col suo colore. */
    data class Badge(val text: String, val tone: Tone)

    /** A zero il numero sparisce e l'icona resta al suo posto. */
    fun badge(count: Int, tone: Tone): Badge? = if (count > 0) Badge(count.toString(), tone) else null

    /** Il nome della scheda per i lettori di schermo: il nome e, se c'è, il numero. */
    fun spoken(label: String, count: Int): String = if (count > 0) "$label $count" else label

    /** Una molla con massa 1, a = -k(x - t) - c·v, come shared/motion.js di designbookmark. */
    data class Spring(val k: Float, val c: Float) {
        /** Lo smorzamento relativo di Compose, c / (2·√k). Con «riduci movimento» critico: niente rimbalzo. */
        fun ratio(reduced: Boolean = false): Float = if (reduced) 1f else c / (2f * sqrt(k))

        /** Lo smorzamento per il motore del web (molla.ts); con «riduci movimento» critico, 2·√k, come `tune` di motion.js. */
        fun damping(reduced: Boolean = false): Float = if (reduced) 2f * sqrt(k) else c
    }

    /** Il nome della scheda scelta che si apre e quello della lasciata che si chiude: 300/26 di bar.js (proposta), 0,75. */
    val NAME = Spring(300f, 26f)

    /** Il bordo della pillola che guida il passo: --dock-lead di tokens.css, 640/38 (0,751). Prende il posto di X/W 340/28 del piano. */
    val LEAD = Spring(640f, 38f)

    /** Il bordo che resta indietro e tira la pillola: --dock-trail, 150/16 (0,653). */
    val TRAIL = Spring(150f, 16f)

    /** Col dito: --dock-drag-lead 1100/56 (0,844) e --dock-drag-trail 320/26 (0,727). */
    val DRAG_LEAD = Spring(1100f, 56f)
    val DRAG_TRAIL = Spring(320f, 26f)

    /** Senza direzione (al rilascio dentro la riga, o alla prima scheda): i due bordi 600/44 (0,898), moveTo di dock.js. */
    val SETTLE = Spring(600f, 44f)

    /** Il salto dell'icona scelta: --dock-pop 520/16 (0,351), con la spinta [PUSH] sulla velocità come setActive di dock.js. */
    val POP = Spring(520f, 16f)
    const val PUSH = -3.2f

    /** La riga che segue la pillola tirata oltre le estremità: nudge di dock.js, 300/18, al [FOLLOW] dell'allungamento. */
    val ROW = Spring(300f, 18f)
    const val FOLLOW = 0.35f

    /** L'altezza della pillola ferma e la larghezza minima (dock.js). */
    const val PILL_H = 40f
    const val PILL_MIN_W = 24f

    /** La pillola tirata si assottiglia ad area costante, fra questi due rapporti dell'altezza (dock.js). */
    const val PILL_THIN = 0.66f
    const val PILL_THICK = 1.1f

    /** L'elastico oltre la prima e l'ultima scheda: la funzione rubber di dock.js, d = 46 e 0,55. */
    const val BAND = 46f
    const val BAND_K = 0.55f

    /** Col dito la pillola resta sulla scheda di adesso finché il dito non ne esce di più di 6 (riga-elastica.html). */
    const val SLACK = 6f

    /** Il dito diventa un trascinamento oltre 4 di corsa (dock.js). */
    const val DRAG = 4f

    /** Lo spazio fra l'icona e il nome aperto. */
    const val NAME_GAP = 8f

    /**
     * La riga stretta (spazi di 7 e icone da 20 invece di 11 e 22) quando le arrivano al massimo 400 dp: la misura è quella che riceve la
     * riga, non lo schermo, così sta anche nelle colonne strette del tablet (app-posizioni-sistema-27-11 gruppo 6; la web la decide con la
     * container query di IconRow.svelte).
     */
    const val TIGHT_DP = 400f

    fun tight(widthDp: Float): Boolean = widthDp <= TIGHT_DP

    /** Alt+1…5 sceglie la scheda (proposta, «Tastiera fisica»). */
    const val MAX_KEYS = 5

    /** La direzione del passo da una scheda all'altra: 1 verso destra, -1 verso sinistra, 0 se una delle due manca o sono la stessa. */
    fun direction(from: Int, to: Int): Int = if (from < 0 || to < 0) 0 else to.compareTo(from).coerceIn(-1, 1)

    /**
     * Le molle dei due bordi, sinistro e destro, per un passo nella direzione [dir]: davanti quello verso cui si va, rigido; dietro
     * l'altro, pigro, e la differenza allunga la pillola. Senza direzione tutti e due [SETTLE]. [drag]: col dito.
     */
    fun edges(dir: Int, drag: Boolean): Pair<Spring, Spring> {
        val lead = if (drag) DRAG_LEAD else LEAD
        val trail = if (drag) DRAG_TRAIL else TRAIL
        return when {
            dir > 0 -> trail to lead
            dir < 0 -> lead to trail
            else -> SETTLE to SETTLE
        }
    }

    /** Un tratto della riga: il bordo sinistro e il destro di una scheda, misurati nel layout della riga. */
    data class Span(val left: Float, val right: Float)

    /** Dove vanno i due bordi della pillola: quelli della scheda scelta, spostati di [extra] dall'elastico oltre le estremità. */
    fun target(span: Span?, extra: Float = 0f): Span? = span?.let { Span(it.left + extra, it.right + extra) }

    /** La pillola disegnata: da [x] larga [width], alta [height], scesa di [top] dentro l'altezza ferma. */
    data class Pill(val x: Float, val width: Float, val height: Float, val top: Float)

    /**
     * La forma della pillola, il punto solo da cui la disegnano telefono e web: i due bordi come stanno adesso ([left], [right]), la
     * meta [goal]. Larga quanto i bordi (almeno [minWidth]); tirata più della meta si assottiglia ad area costante, stretta si
     * gonfia: altezza = [height] · clamp(1/√(larghezza / larghezza della meta), 0,66, 1,1), sempre centrata.
     */
    fun pill(left: Float, right: Float, goal: Span, height: Float = PILL_H, minWidth: Float = PILL_MIN_W): Pill {
        val a = min(left, right)
        val b = max(left, right)
        val w = max(b - a, minWidth)
        val rest = max(goal.right - goal.left, minWidth)
        val h = height * (1f / sqrt(w / rest)).coerceIn(PILL_THIN, PILL_THICK)
        return Pill(a, w, h, (height - h) / 2f)
    }

    /** L'elastico di dock.js: dentro [lo]..[hi] il punto resta dov'è, fuori resiste sempre di più e non supera mai [d]. */
    fun rubber(x: Float, lo: Float, hi: Float, d: Float = BAND): Float = when {
        x < lo -> lo - (1f - 1f / ((lo - x) * BAND_K / d + 1f)) * d
        x > hi -> hi + (1f - 1f / ((x - hi) * BAND_K / d + 1f)) * d
        else -> x
    }

    /**
     * Quanto la pillola va oltre le estremità col dito in [x]: l'elastico fra il centro della prima scheda e quello dell'ultima, meno
     * la parte dentro. 0 dentro la riga; la riga intera segue al [FOLLOW] di questo.
     */
    fun stretch(x: Float, spans: List<Span>, d: Float = BAND): Float {
        if (spans.isEmpty()) return 0f
        val a = (spans.first().left + spans.first().right) / 2f
        val b = (spans.last().left + spans.last().right) / 2f
        val lo = min(a, b)
        val hi = max(a, b)
        val c = rubber(x, lo, hi, d)
        return c - c.coerceIn(lo, hi)
    }

    /**
     * La scheda sotto il dito in [x] mentre trascina, partendo da [current]: resta quella finché il dito non ne esce di più di [slack];
     * fuori dalla riga conta la scheda all'estremità; fra due schede resta quella di adesso. -1 senza schede.
     */
    fun under(spans: List<Span>, x: Float, current: Int, slack: Float = SLACK): Int {
        if (spans.isEmpty()) return -1
        val lo = spans.first().left
        val xc = x.coerceIn(lo, max(lo, spans.last().right - 1f))
        val on = spans.getOrNull(current)
        if (on != null && xc >= on.left - slack && xc <= on.right + slack) return current
        val j = spans.indexOfFirst { xc >= it.left && xc < it.right }
        return if (j >= 0) j else if (on != null) current else -1
    }

    /** Al rilascio la pillola torna dentro: guida il bordo verso la riga (-1 se era oltre la fine, 1 se prima dell'inizio, 0 dentro). */
    fun settle(extra: Float): Int = when {
        extra > 0f -> -1
        extra < 0f -> 1
        else -> 0
    }

    /** Il nome che si apre, con [f] da 0 (chiuso) a 1 (aperto): compare solo dopo poco meno di un terzo della corsa (riga-elastica.html). */
    fun nameAlpha(f: Float): Float = (f * 1.4f - 0.4f).coerceIn(0f, 1f)

    /** Quanto dello spazio [NAME_GAP] fra icona e nome c'è già, da 0 a 1. */
    fun nameGap(f: Float): Float = f.coerceIn(0f, 1f)

    /** Alt+[digit] su [size] schede: l'indice della scheda, o null se quel tasto non ne sceglie una. */
    fun altDigit(digit: Int, size: Int): Int? = if (digit in 1..min(size, MAX_KEYS)) digit - 1 else null
}
