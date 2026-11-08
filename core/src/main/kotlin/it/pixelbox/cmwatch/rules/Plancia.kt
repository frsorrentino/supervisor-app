package it.pixelbox.cmwatch.rules

/**
 * La plancia del tablet senza sessioni aperte (come `web/src/lib/Plancia.svelte`, pwa2 approvata da Franz il 30/10; sul
 * tablet app-tablet-come-pwa-03-11): le colonne per categoria nell'ordine della home, quante ne stanno a 360 dp l'una
 * tolto il pannello della master; quelle che non stanno scendono in fondo all'ultima che si vede. 320 fino alle sessioni a sé
 * (app-home-sessioni-a-se-08-11): nella colonna delle sessioni nome, tasto e scelta devono starci.
 */
object Plancia {
    const val COL = 360f
    const val MAX = 4

    fun count(width: Float, panel: Float?): Int = ((width - (panel ?: 0f)) / COL).toInt().coerceIn(1, MAX)

    fun <T> layout(order: List<T>, n: Int): List<List<T>> {
        val k = minOf(n, order.size)
        return (0 until k).map { i -> if (i == k - 1) order.drop(i) else listOf(order[i]) }
    }
}
