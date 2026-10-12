package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptFile

/**
 * La cache dei file della chat sul telefono (Franz, 12/10 09:31): un video già visto si riscaricava dal PC, 12 MB ogni
 * volta, a ogni riapertura della conversazione, perché solo una mappa in memoria ricordava il file scaricato. Ora la copia
 * si ritrova sul disco per nome, e il nome nasce da una chiave che cambia quando cambia il file sul PC.
 */
object FileCache {
    /** Il tetto della cartella, circa 16 video da 12 MB; oltre escono i file usati meno di recente. */
    const val MAX_BYTES = 200L * 1024 * 1024

    /**
     * Lo stesso file del PC. Il relay legge la misura quando prepara la pagina: un file riscritto con un'altra misura cambia
     * chiave anche nelle voci vecchie, e si riscarica. Senza misura (il file sul PC non c'è più, o sta su win) vale l'id
     * della voce: un nuovo invio dello stesso percorso è un'altra voce, e si riscarica.
     */
    fun key(file: TranscriptFile, entry: String): String =
        file.size?.let { "${file.path}\u0000size=$it" } ?: "${file.path}\u0000entry=$entry"

    /** Il nome sul disco: l'impronta della chiave, poi il nome del file, che le app di sistema mostrano quando lo aprono. */
    fun name(key: String, path: String): String {
        val sha = java.security.MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return sha.joinToString("") { "%02x".format(it) }.take(24) + "-" + path.substringAfterLast('/')
    }

    /** Un file della cartella: nome, byte, ultimo uso in ms. */
    data class Entry(val name: String, val bytes: Long, val used: Long)

    /**
     * I nomi da cancellare: dal meno usato di recente, finché il resto sta nel tetto. Restano sempre quelli in `keep`, il
     * file appena arrivato e quelli che la chat aperta mostra: tolti, l'anteprima li richiederebbe al PC.
     */
    fun evict(files: List<Entry>, max: Long = MAX_BYTES, keep: Set<String> = emptySet()): List<String> {
        var total = files.sumOf { it.bytes }
        val out = mutableListOf<String>()
        for (f in files.sortedBy { it.used }) {
            if (total <= max) break
            if (f.name in keep) continue
            out += f.name
            total -= f.bytes
        }
        return out
    }
}
