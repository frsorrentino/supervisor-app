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
}
