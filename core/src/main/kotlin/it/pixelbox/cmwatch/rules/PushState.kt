package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest

/**
 * Contratto 1.148 (app-push-etag-15-11, taglio 2 della misura Firebase del 15/11: il 99% dei download era /state riletto intero dopo
 * ogni push). Il messaggio FCM di un evento porta, facoltativi, `etag` e `published_at` dello /state che c'è sul bus quando parte.
 * `etag` = i primi 16 caratteri esadecimali minuscoli dello SHA-1 del testo UTF-8 del campo `enc` del documento {v, enc}: chi ha già
 * scaricato /state, con una GET o dallo stream, lo ricalcola senza decifrare. `published_at` è lo stesso numero dello stato decifrato.
 * Se il dispositivo ha già quello stato non lo rilegge; senza i due campi (relay di prima) legge come sempre.
 */
object PushState {
    const val ETAG = "etag"
    const val PUBLISHED_AT = "published_at"

    fun etagOf(enc: String): String =
        MessageDigest.getInstance("SHA-1").digest(enc.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(16)

    /** L'etag del documento di /state così com'è sul bus, o null se non è una busta cifrata. */
    fun etagOfDoc(doc: JsonElement): String? =
        ((doc as? JsonObject)?.get("enc") as? JsonPrimitive)?.takeIf { it.isString }?.content?.let(::etagOf)

    /**
     * Vero se lo stato di cui parla la push è già qui: stesso [haveEtag] dell'ultimo /state scaricato, o `published_at` non più nuovo
     * di [havePublishedAt], quello dello stato in memoria. Un campo assente o illeggibile non dice niente.
     */
    fun known(data: Map<String, String>, haveEtag: String?, havePublishedAt: Double?): Boolean {
        val etag = data[ETAG]?.takeIf { it.isNotBlank() }
        if (etag != null && etag == haveEtag) return true
        val at = data[PUBLISHED_AT]?.toDoubleOrNull() ?: return false
        return havePublishedAt != null && at <= havePublishedAt
    }
}
