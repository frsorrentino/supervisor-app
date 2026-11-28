package it.pixelbox.cmwatch.rules

import kotlin.math.max

/**
 * app-entrata-home-27-11 (Franz 27/11 20:46, «l'effetto di entrata elastica su tutti gli elementi della home quando si avvia l'app»;
 * variante B del mockup home-entrata.html della master): alla prima apertura i blocchi della home entrano dall'alto uno dopo l'altro.
 * Le regole che non sono disegno, uguali sul web (web/src/lib/entrataHome.ts): i tempi, la molla, la barra della settimana, la volta
 * sola. Le misure sono in px CSS sul web e in dp sul telefono.
 */
object EntrataHome {
    /** Il primo blocco parte dopo 70 ms, ognuno dei seguenti 45 ms dopo quello sopra, come le righe del Rubber dock. */
    const val DELAY_MS = 70
    const val STAGGER_MS = 45

    /** Opacità da 0 e sfocatura da 5 in 0,4 s ease. */
    const val FADE_MS = 400
    const val BLUR = 5f

    /** Sale di 24 e scala da 0,94 con la molla con cui il dock compare (dock.js, intro). */
    const val RISE = 24f
    const val SCALE_FROM = 0.94f

    /** La molla 220/15, massa 1: smorzamento relativo 15 / (2·√220) ≈ 0,506. */
    val SPRING = IconRowRules.Spring(220f, 15f)

    /** La barra della settimana cresce in 0,8 s con cubic-bezier(.34, 1.56, .64, 1), 220 ms dopo il suo riquadro. */
    const val BAR_MS = 800
    const val BAR_AFTER_MS = 220

    /** Quando parte il blocco in posizione [index], contata dall'alto da 0. */
    fun delayMs(index: Int): Int = DELAY_MS + STAGGER_MS * max(0, index)

    /** Quando parte la barra della settimana, il cui riquadro è in posizione [boxIndex]. */
    fun barDelayMs(boxIndex: Int): Int = delayMs(boxIndex) + BAR_AFTER_MS

    /** Entra solo alla prima apertura e mai con «riduci movimento»: allora la home compare ferma. */
    fun plays(first: Boolean, reduced: Boolean): Boolean = first && !reduced

    /**
     * La volta sola: [take] è vero alla prima chiamata e poi sempre falso, anche se quella volta la home è comparsa ferma. Una per
     * processo sul telefono, una per caricamento della pagina sul web: tornando alla home da una sessione non si ripete.
     */
    class Once {
        var taken = false
            private set

        fun take(): Boolean {
            if (taken) return false
            taken = true
            return true
        }
    }

    /**
     * Le posizioni dall'alto: la lista dà il numero dopo ai blocchi che compone finché la home è al primo disegno ([close] lo chiude);
     * a chi arriva dopo (scorrendo, o con un dato nuovo) nessun numero, e il blocco resta fermo.
     */
    class Order {
        private var count = 0
        private var open = true

        fun next(): Int? = if (open) count++ else null

        fun close() {
            open = false
        }
    }
}
