package it.pixelbox.cmwatch.rules

/**
 * Chiavi delle liste lazy: Compose chiude l'app se due righe hanno la stessa chiave. Il 15/10 alle 23:41 due
 * sessioni con lo stesso nome (un doppio launch --bg di claude-plugins) hanno mandato in crash l'app a ogni
 * apertura. La chiave parte dall'id della sessione; se anche quello si ripete, le copie prendono «#2», «#3»…
 */
fun <T> uniqueKeys(items: List<T>, base: (T) -> String): List<String> {
    val seen = HashMap<String, Int>()
    return items.map { item ->
        val b = base(item)
        val n = (seen[b] ?: 0) + 1
        seen[b] = n
        if (n == 1) b else "$b#$n"
    }
}
