package it.pixelbox.cmwatch.rules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * L'aggiornamento dentro l'app (proposta P6 del 21/10, approvata da Franz il 21/10 alle 23:58): l'app guarda le release
 * firmate del suo repo su GitHub, pre-release comprese, e propone «Aggiornamento disponibile · Installa». Qui le regole
 * pure: quali release contano, quando una è un aggiornamento per quello che c'è installato, il digest da verificare sul
 * file scaricato. La rete, il download e l'installer stanno nell'app del telefono (`AppUpdater`).
 *
 * Una release è un aggiornamento se è stata pubblicata dopo l'installazione di adesso e il suo APK non è quello installato
 * (digest sha256 diverso): così lo stesso APK messo sul telefono via ADB non si ripropone, e una build di sviluppo più
 * nuova di ogni release non torna indietro. «Non ora» scarta quel tag finché non ne esce uno nuovo.
 */
object AppUpdate {
    const val REPO = "frsorrentino/supervisor-app"
    /** L'asset dell'APK del telefono nelle release (lo stesso nome della CI). */
    const val ASSET = "mobile-release.apk"
    const val RELEASES_URL = "https://api.github.com/repos/$REPO/releases?per_page=10"
    /** Da solo l'app controlla al più ogni mezz'ora; «Controlla ora» passa sempre. Anonimo: 60 richieste l'ora. */
    const val CHECK_EVERY_S = 5 * 60L

    data class Release(
        val tag: String, val name: String, val publishedAt: Long, val prerelease: Boolean, val url: String, val size: Long,
        /** Il digest sha256 dell'APK, esadecimale minuscolo; null se GitHub non lo dà: allora non si scarica. */
        val sha256: String?, val notes: String,
    )

    /** Quello che c'è sul telefono: il digest dell'APK installato (null finché non è calcolato) e quando è stato installato. */
    data class Installed(val sha256: String?, val installedAt: Long)

    @Serializable private data class GhAsset(val name: String = "", val digest: String? = null, val size: Long = 0, @SerialName("browser_download_url") val url: String = "")
    @Serializable private data class GhRelease(
        @SerialName("tag_name") val tag: String = "", val name: String? = null, val draft: Boolean = false, val prerelease: Boolean = false,
        @SerialName("published_at") val publishedAt: String? = null, val body: String? = null, val assets: List<GhAsset> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Le release pubblicate (non bozze) con l'APK del telefono, dalla più recente; un JSON che non torna = nessuna. */
    fun parse(body: String): List<Release> = runCatching { json.decodeFromString<List<GhRelease>>(body) }.getOrDefault(emptyList())
        .filter { !it.draft && it.tag.isNotBlank() }
        .mapNotNull { r ->
            val a = r.assets.firstOrNull { it.name == ASSET } ?: return@mapNotNull null
            val at = r.publishedAt?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() } ?: return@mapNotNull null
            Release(r.tag, r.name?.trim()?.takeIf { it.isNotEmpty() } ?: r.tag, at, r.prerelease, a.url, a.size, sha256Of(a.digest), r.body.orEmpty().trim())
        }
        .sortedByDescending { it.publishedAt }

    /** «sha256:ABC…» → «abc…»; un altro algoritmo, una lunghezza sbagliata o niente = null. */
    fun sha256Of(digest: String?): String? = digest?.trim()?.lowercase()?.takeIf { it.startsWith("sha256:") }
        ?.removePrefix("sha256:")?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }

    /**
     * La release da proporre: la più recente, se è uscita dopo l'installazione di adesso, non è l'APK già installato e non
     * è quella scartata con «Non ora». Senza il digest dell'installato si guarda solo la data.
     */
    fun offer(releases: List<Release>, installed: Installed, dismissed: String? = null): Release? {
        val r = releases.firstOrNull() ?: return null
        if (r.publishedAt <= installed.installedAt) return null
        if (r.sha256 != null && installed.sha256 != null && r.sha256 == installed.sha256) return null
        if (r.tag == dismissed) return null
        return r
    }

    /** Il file scaricato è buono solo se il suo sha256 è quello della release e la misura torna. */
    fun verified(release: Release, sha256: String, size: Long): Boolean =
        release.sha256 != null && release.sha256 == sha256.lowercase() && (release.size <= 0 || release.size == size)

    /** Un controllo da solo solo dopo [CHECK_EVERY_S] dall'ultimo (0 = mai controllato). */
    fun due(lastCheck: Long, now: Long): Boolean = lastCheck <= 0 || now - lastCheck >= CHECK_EVERY_S

    /** «68 MB», «1,5 MB», «950 kB». */
    fun sizeLabel(bytes: Long): String = when {
        bytes >= 10_000_000 -> "${bytes / 1_000_000} MB"
        bytes >= 1_000_000 -> "${bytes / 1_000_000},${(bytes % 1_000_000) / 100_000} MB"
        else -> "${bytes / 1000} kB"
    }

    /** Il nome del file in cache per un tag, senza caratteri che un tag può avere e un nome di file no. */
    fun fileName(tag: String): String = tag.map { if (it.isLetterOrDigit() || it == '-' || it == '.' || it == '_') it else '_' }.joinToString("") + ".apk"
}
