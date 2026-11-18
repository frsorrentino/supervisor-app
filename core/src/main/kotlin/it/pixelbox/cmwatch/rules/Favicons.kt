package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.FaviconImage
import it.pixelbox.cmwatch.contract.State

/**
 * Contratto 1.153 (app-favicon-siti-18-11, scelta B di Franz il 18/11 alle 17:11): la favicon del sito come avatar della sessione,
 * con l'anello dello stato intorno. Lo stato porta solo l'impronta; l'immagine si chiede una volta con l'op `favicon` e si tiene.
 * Senza impronta, senza l'op o con un'immagine che non torna resta l'icona di oggi.
 */
object Favicons {
    /** Le impronte da avere, una volta ciascuna, solo se il relay sa rispondere all'op. */
    fun wanted(st: State?): List<String> =
        if (st?.ops?.contains("favicon") != true) emptyList() else st.sessions.mapNotNull { s -> s.favicon?.takeIf(::valid) }.distinct()

    /** 12 esadecimali minuscoli, come le manda il relay; altro non si chiede e non diventa un nome di file. */
    fun valid(hash: String): Boolean = Regex("[0-9a-f]{12}").matches(hash)

    /**
     * I byte del PNG di una risposta, solo se è proprio quella chiesta: stessa impronta, PNG vero, entro 64 KB (il relay ne manda
     * meno di 16). Null altrimenti: un errore, un'impronta diversa o dati rotti lasciano l'icona di oggi.
     */
    fun png(hash: String, ok: Boolean, text: String): ByteArray? {
        if (!ok || !valid(hash)) return null
        val img: FaviconImage = runCatching { ContractJson.decodeFavicon(text) }.getOrNull() ?: return null
        if (img.hash != hash || img.mime != "image/png") return null
        val b = runCatching { java.util.Base64.getDecoder().decode(img.data) }.getOrNull() ?: return null
        val magic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        return b.takeIf { x -> x.size in 9..65_536 && x.copyOfRange(0, 8).contentEquals(magic) }
    }
}
