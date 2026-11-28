package it.pixelbox.cmwatch.rules

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Il foglio della master (app-posizioni-sistema-27-11 gruppo 4; proposta B «Foglio» in dalla-master/posizioni-2026-11-27, mockup
 * m/b-foglio.html): sale dal basso con tre misure, riga (la riga della master con il suo stato e ▶, e il campo: circa 170 dp con la
 * maniglia), metà e intera. Trascinato dalla maniglia o dalla sua riga segue il dito, oltre la riga e oltre l'intera con l'elastico;
 * al rilascio la velocità si proietta come nei fogli di iOS (decelerazione 0,998) e il foglio scatta alla misura più vicina al punto
 * proiettato, con la molla senza rimbalzo della master ([MasterGestures.land]). Il tocco sulla riga passa alla misura dopo
 * (riga → metà → intera → riga, come il mockup); Indietro, Esc e ↓ a campo vuoto riportano alla riga. Le misure sono altezze dal fondo
 * del posto che lo contiene, in dp (sul web in px CSS); le velocità in dp al secondo, positive verso l'alto. Le stesse regole sul web
 * (web/src/lib/masterSheet.ts).
 */
object MasterSheet {
    enum class Stop(val id: String) { ROW("row"), HALF("half"), FULL("full") }

    /** La riga prima di misurarla: maniglia, la riga della master e il campo. */
    const val ROW_DP = 170f
    /** A tutta altezza resta uno stacco sopra il foglio: si vede che è un foglio, e sotto c'è la pagina. */
    const val GAP_DP = 8f
    /**
     * Metà come nel mockup approvato (m/b-foglio.html, `meta: H * .42`): il bordo del foglio al 42 % del posto, cioè il foglio alto il
     * 58 %. Così alla conversazione della master restano almeno 200 dp a 360 × 780, fra la sua riga, i chip delle Azioni e il campo.
     */
    const val HALF_SHARE = 0.58f
    /** La decelerazione di iOS per millisecondo (UIScrollView.DecelerationRate.normal). */
    const val RATE = 0.998f
    /** L'elastico oltre la riga e oltre l'intera: la costante di UIScrollView. */
    const val RUBBER = 0.55f
    /** La preferenza unica, nelle preferenze «ui»: l'ultima misura del foglio. */
    const val PREF = "master_sheet"

    /** Le altezze delle tre misure. */
    data class Anchors(val row: Float, val half: Float, val full: Float) {
        fun of(s: Stop): Float = when (s) { Stop.ROW -> row; Stop.HALF -> half; Stop.FULL -> full }
    }

    /**
     * Le tre altezze dentro `avail` (l'altezza del posto, sotto la barra in alto): la riga è `row`, misurata; l'intera tutto meno lo
     * stacco; la metà il 58 % del posto ([HALF_SHARE]), mai sotto la riga né sopra l'intera. In un posto troppo basso coincidono con la riga.
     */
    fun anchors(avail: Float, row: Float, gap: Float = GAP_DP): Anchors {
        val r = max(0f, row)
        val full = max(r, avail - gap)
        val half = min(full, max(r, avail * HALF_SHARE))
        return Anchors(r, half, full)
    }

    /** Dove arriverebbe il foglio rallentando come iOS: posizione + (v / 1000) · 0,998 / (1 − 0,998), cioè v · 0,499 s. */
    fun project(pos: Float, v: Float, rate: Float = RATE): Float = pos + v / 1000f * rate / (1f - rate)

    /** La misura più vicina a un'altezza; a pari distanza la più bassa. */
    fun nearest(a: Anchors, h: Float): Stop {
        var best = Stop.ROW
        for (s in Stop.entries) if (abs(a.of(s) - h) < abs(a.of(best) - h)) best = s
        return best
    }

    /** Al rilascio: la misura più vicina al punto proiettato. */
    fun snap(a: Anchors, h: Float, v: Float): Stop = nearest(a, project(h, v))

    /** L'elastico di iOS: `over` dp oltre il bordo diventano meno, mai più di `dim`. */
    fun band(over: Float, dim: Float, k: Float = RUBBER): Float = if (over <= 0f || dim <= 0f) 0f else (1f - 1f / (over * k / dim + 1f)) * dim

    /** L'altezza col dito: libera fra `lo` e `hi`, oltre con l'elastico (`dim`: l'altezza del posto). */
    fun rubber(h: Float, lo: Float, hi: Float, dim: Float, k: Float = RUBBER): Float = when {
        h < lo -> lo - band(lo - h, dim, k)
        h > hi -> hi + band(h - hi, dim, k)
        else -> h
    }

    /** Il tocco sulla riga: alla misura dopo, e dall'intera di nuovo alla riga. */
    fun tap(s: Stop): Stop = when (s) { Stop.ROW -> Stop.HALF; Stop.HALF -> Stop.FULL; Stop.FULL -> Stop.ROW }

    /** ↑ sulla riga del foglio: una misura più su; dall'intera resta lì. */
    fun up(s: Stop): Stop = if (s == Stop.ROW) Stop.HALF else Stop.FULL

    /** La preferenza letta: una misura nota, altrimenti la riga. */
    fun read(raw: String?): Stop = Stop.entries.firstOrNull { it.id == raw } ?: Stop.ROW

    // app-posizioni-sistema-27-11, prova 1447 (Franz: «sembra quasi che si sta alzando solo l'header»; «una sfumatura verso il nero tra il
    // bordo della master superiore e la home e qualche animazione di movimento che fa comprendere bene l'azione di sovrapposizione»): il
    // foglio è un blocco solo, testata e conversazione insieme; la home dietro si vela e arretra mentre lui sale, e sopra il bordo c'è un'ombra.

    /** Quanto è salito il foglio: 0 alla riga, 1 all'intera. Guida il velo, l'arretramento della home e l'ombra sopra il bordo. */
    fun progress(a: Anchors, h: Float): Float = if (a.full <= a.row) 0f else ((h - a.row) / (a.full - a.row)).coerceIn(0f, 1f)

    /** Il velo nero sulla home all'intera: 40 %. */
    const val DIM_MAX = 0.4f
    /** La home arretra fino a 0,96 all'intera, dal centro in alto. */
    const val RECEDE = 0.04f
    /** L'ombra sopra il bordo del foglio: nero che sfuma verso l'alto, dal 30 % alla riga al 55 % all'intera, alta SHADE_DP. */
    const val SHADE_MIN = 0.3f
    const val SHADE_MAX = 0.55f
    const val SHADE_DP = 40f

    data class Cues(val dim: Float, val scale: Float, val shade: Float)

    /**
     * Il velo sulla home, la sua scala e l'ombra sopra il bordo per l'avanzamento `p`. Con il movimento ridotto la home non arretra: restano
     * il velo e l'ombra, che non si muovono.
     */
    fun cues(p: Float, reduced: Boolean = false): Cues {
        val q = p.coerceIn(0f, 1f)
        return Cues(DIM_MAX * q, if (reduced) 1f else 1f - RECEDE * q, SHADE_MIN + (SHADE_MAX - SHADE_MIN) * q)
    }

    /**
     * La conversazione rigida (prova 1447): quanta tenerne ferma (px) all'inizio di un movimento, col foglio alto [h], partito da [from],
     * verso [to] (null quando lo tiene il dito). Dalla riga, dove non se ne vede niente, quanta se ne vedrà all'arrivo (col dito, alla misura
     * sopra): sale tutta insieme alla testata e il campo la copre finché non arriva. Altrimenti quanta se ne vede adesso: scendendo resta
     * rigida e passa sotto il campo; salendo si allunga col foglio e l'ultimo messaggio resta sopra il campo, perché sotto l'ultimo non c'è
     * altro da mostrare (alzarla tutta subito farebbe saltare quello che si vede). Da fermo torna alta quanto si vede.
     */
    fun freezeAt(a: Anchors, h: Float, from: Stop, to: Stop?): Float {
        val vis = { x: Float -> max(0f, x - a.row) }
        return if (h <= a.row + 0.5f) vis(a.of(to ?: up(from))) else vis(h)
    }

    /** Esc riporta sempre alla riga; ↓ solo a campo vuoto o col fuoco fuori dal campo. */
    fun toRow(key: String, inField: Boolean, fieldEmpty: Boolean): Boolean = key == "Escape" || (key == "ArrowDown" && (!inField || fieldEmpty))

    /** Distanza dall'arrivo e velocità della molla d'arrivo al tempo `t` (s): x(t) = (x0 + (v0 + ω·x0)·t)·e^(−ωt). */
    data class At(val x: Float, val v: Float)

    fun settleAt(from: Float, v: Float, w: Float, t: Float): At {
        val e = exp(-w * t)
        val b = v + w * from
        return At((from + b * t) * e, (b - w * (from + b * t)) * e)
    }
}
