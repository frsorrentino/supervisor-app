package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.ContractJson

/**
 * Letture leggere (app-letture-leggere-11-11, contratto 1.135; Franz 11/11 14:38: «correggi tutto il possibile purché non pesi sulla
 * qualità dell'app»): ogni lettura riparte con l'etag dell'ultima risposta allo stesso comando; un relay 1.135 risponde `unchanged`
 * col testo vuoto e qui torna la risposta che si aveva, così le schermate restano identiche. Un relay di prima ignora l'etag e manda
 * tutto, come prima. Lo stesso in web/src/lib/lightReads.ts.
 */
class EtagTransport(private val inner: Transport) : Transport by inner {
    private val last = object : LinkedHashMap<String, CmdResult>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CmdResult>?) = size > KEEP
    }

    override suspend fun send(cmd: Cmd, onWritten: () -> Unit): CmdResult {
        val key = key(cmd)
        val had = synchronized(last) { last[key] }
        val r = inner.send(if (had?.etag != null) cmd.copy(etag = had.etag) else cmd, onWritten)
        synchronized(last) {
            if (r.unchanged && had != null) return had.copy(id = r.id, at = if (r.at != 0L) r.at else had.at)
            if (r.ok && r.etag != null) last[key] = r else last.remove(key)
        }
        return r
    }

    companion object {
        /** Quante risposte tenere: le letture dell'app sono qualche decina. */
        const val KEEP = 80
        /** Il comando senza ciò che cambia a ogni invio: due comandi con la stessa chiave chiedono la stessa cosa. */
        fun key(cmd: Cmd): String = ContractJson.encode(cmd.copy(id = "", issued = 0, uid = null, etag = null))
    }
}
