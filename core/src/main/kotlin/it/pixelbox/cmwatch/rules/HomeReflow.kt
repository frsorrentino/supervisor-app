package it.pixelbox.cmwatch.rules

/**
 * La home che si riposiziona (app-home-riposiziona-animata-04-11): aprendo la prima sessione la sua scheda si espande nella
 * colonna mentre le altre schede scivolano verso la colonna unica; chiudendo l'ultima l'inverso. Una sola coreografia nei due
 * versi, con la durata e la curva della web (`--reflow-ms`, `--reflow-ease` in web/src/v3.css).
 */
object HomeReflow {
    const val DURATION_MS = 700
    /** cubic-bezier(0.23, 1, 0.32, 1): parte svelta e rallenta in fondo. */
    val EASE = floatArrayOf(0.23f, 1f, 0.32f, 1f)
    /** Col movimento ridotto: solo un'assolvenza breve. */
    const val REDUCED_FADE_MS = 150

    /**
     * A onda (app-effetti-movimenti-04-11, punto 6, come wave() della web): una scheda parte 18 ms dopo per ogni gradino di 48 dp
     * verso il basso (un terzo verso destra), al massimo 160 ms; la durata si accorcia dello stesso tanto, così l'insieme resta
     * nei DURATION_MS.
     */
    const val WAVE_STEP_MS = 18
    const val WAVE_MAX_MS = 160
    const val WAVE_STEP_DP = 48f
    fun waveDelay(topDp: Float, leftDp: Float): Int =
        (((topDp + leftDp / 3f).coerceAtLeast(0f) / WAVE_STEP_DP).toInt() * WAVE_STEP_MS).coerceAtMost(WAVE_MAX_MS)
}
