package it.pixelbox.cmwatch.mobile.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import it.pixelbox.cmwatch.mobile.BuildConfig
import it.pixelbox.cmwatch.rules.AppUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * L'aggiornamento dentro l'app (P6 del 21/10, approvata da Franz alle 23:58): legge le release del repo su GitHub
 * (anonimo, pre-release comprese), scarica l'APK del telefono nella cache, lo verifica col digest sha256 che GitHub
 * pubblica per l'asset e apre l'installer di Android, che accetta solo un APK con la stessa firma di quello installato.
 * Le regole stanno in [AppUpdate]; qui rete, file e intent. Niente ADB, niente link, niente limiti della chat.
 * Solo nel flavor github: nella build di Play (`BuildConfig.SELF_UPDATE` falso) non controlla e non installa niente.
 */
class AppUpdater(private val ctx: Context, private val scope: CoroutineScope) {
    sealed class Phase {
        /** La release di cui si parla; nessuna in [None]. */
        abstract val release: AppUpdate.Release?
        data object None : Phase() { override val release: AppUpdate.Release? get() = null }
        data class Available(override val release: AppUpdate.Release) : Phase()
        data class Downloading(override val release: AppUpdate.Release, val percent: Int) : Phase()
        data class Ready(override val release: AppUpdate.Release, val file: File) : Phase()
        data class Failed(override val release: AppUpdate.Release, val reason: Reason) : Phase()
    }
    enum class Reason { NO_DIGEST, BAD_DIGEST, NETWORK, HTTP }

    /**
     * `checkedAt`: l'ultimo controllo riuscito (epoch s); `error`: perché l'ultimo è fallito, finché uno non riesce;
     * `needsPermission`: Android vuole prima il permesso di installare app, chiesto nella sua schermata.
     */
    data class Ui(
        val phase: Phase = Phase.None, val checking: Boolean = false, val checkedAt: Long? = null, val error: String? = null,
        val needsPermission: Boolean = false,
    )

    val ui = MutableStateFlow(Ui())
    private val prefs = ctx.getSharedPreferences("update", Context.MODE_PRIVATE)
    private var releases: List<AppUpdate.Release> = emptyList()

    /** Da solo, all'apertura dell'app: non più di una volta ogni mezz'ora. */
    fun checkIfDue(now: Long = System.currentTimeMillis() / 1000) {
        if (!BuildConfig.SELF_UPDATE) return
        if (AppUpdate.due(prefs.getLong(CHECKED, 0), now)) check()
    }

    /** Legge le release e decide. `manual` («Controlla ora») rifà il controllo anche se recente e dimentica «Non ora». */
    fun check(manual: Boolean = false) {
        if (!BuildConfig.SELF_UPDATE) return
        if (ui.value.checking) return
        if (manual) prefs.edit().remove(DISMISSED).apply()
        ui.update { it.copy(checking = true) }
        scope.launch(Dispatchers.IO) {
            val got = runCatching { get(AppUpdate.RELEASES_URL) }
            val body = got.getOrNull()
            if (body == null) {
                val e = got.exceptionOrNull()
                ui.update { it.copy(checking = false, error = e?.message?.takeIf { m -> m.isNotBlank() } ?: e?.javaClass?.simpleName ?: "?") }
                return@launch
            }
            releases = AppUpdate.parse(body)
            val now = System.currentTimeMillis() / 1000
            prefs.edit().putLong(CHECKED, now).apply()
            val offer = AppUpdate.offer(releases, installed(), prefs.getString(DISMISSED, null))
            tidyCache(offer?.tag)
            ui.update { u ->
                val same = u.phase.release?.tag == offer?.tag
                val phase = when {
                    offer == null -> Phase.None
                    // Un download in corso o un file già pronto per questo stesso tag restano quello che sono.
                    same && (u.phase is Phase.Downloading || (u.phase is Phase.Ready && (u.phase as Phase.Ready).file.exists())) -> u.phase
                    else -> cached(offer)?.let { Phase.Ready(offer, it) } ?: Phase.Available(offer)
                }
                u.copy(phase = phase, checking = false, checkedAt = now, error = null)
            }
        }
    }

    /** «Installa»: scarica se serve (verificando il digest mentre scarica), poi apre l'installer. */
    fun install(release: AppUpdate.Release) {
        if (!BuildConfig.SELF_UPDATE) return
        val p = ui.value.phase
        if (p is Phase.Downloading) return
        if (p is Phase.Ready && p.release.tag == release.tag && p.file.exists()) { scope.launch { open(p.file) }; return }
        if (release.sha256 == null) { ui.update { it.copy(phase = Phase.Failed(release, Reason.NO_DIGEST)) }; return }
        ui.update { it.copy(phase = Phase.Downloading(release, 0)) }
        scope.launch(Dispatchers.IO) {
            val dir = File(ctx.cacheDir, DIR).apply { mkdirs() }
            val out = File(dir, AppUpdate.fileName(release.tag))
            val tmp = File(dir, out.name + PART)
            val downloaded = runCatching {
                val c = connect(release.url)
                val code = c.responseCode
                if (code !in 200..299) throw HttpFailure(code)
                val total = c.contentLengthLong.takeIf { it > 0 } ?: release.size
                val md = MessageDigest.getInstance("SHA-256")
                var read = 0L
                var last = -1
                c.inputStream.use { ins ->
                    tmp.outputStream().use { os ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            os.write(buf, 0, n); md.update(buf, 0, n); read += n
                            val pct = if (total > 0) ((read * 100) / total).toInt().coerceIn(0, 99) else 0
                            if (pct != last) { last = pct; ui.update { it.copy(phase = Phase.Downloading(release, pct)) } }
                        }
                    }
                }
                md.digest().joinToString("") { "%02x".format(it) } to read
            }
            val (sha, size) = downloaded.getOrElse { e ->
                tmp.delete()
                ui.update { it.copy(phase = Phase.Failed(release, if (e is HttpFailure) Reason.HTTP else Reason.NETWORK)) }
                return@launch
            }
            if (!AppUpdate.verified(release, sha, size)) {
                tmp.delete()
                ui.update { it.copy(phase = Phase.Failed(release, Reason.BAD_DIGEST)) }
                return@launch
            }
            out.delete()
            if (!tmp.renameTo(out)) { ui.update { it.copy(phase = Phase.Failed(release, Reason.NETWORK)) }; return@launch }
            ui.update { it.copy(phase = Phase.Ready(release, out)) }
            open(out)
        }
    }

    /** «Non ora»: quel tag non si ripropone finché non ne esce uno nuovo o non si controlla a mano. */
    fun dismiss(tag: String) {
        prefs.edit().putString(DISMISSED, tag).apply()
        ui.update { if (it.phase.release?.tag == tag) it.copy(phase = Phase.None, needsPermission = false) else it }
    }

    /** Di ritorno dalla schermata del permesso con il file pronto: l'installer si apre da solo. */
    fun resumed() {
        if (!BuildConfig.SELF_UPDATE) return
        val u = ui.value
        val p = u.phase
        if (u.needsPermission && p is Phase.Ready && p.file.exists() && ctx.packageManager.canRequestPackageInstalls()) scope.launch { open(p.file) }
    }

    private suspend fun open(file: File) = withContext(Dispatchers.Main) {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            ui.update { it.copy(needsPermission = true) }
            runCatching {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return@withContext
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ui.update { it.copy(needsPermission = false) }
        runCatching { ctx.startActivity(intent) }
    }

    /** Il digest dell'APK installato, calcolato una volta per installazione e ricordato; e quando è stato installato. */
    private fun installed(): AppUpdate.Installed {
        val info = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
        val at = (info?.lastUpdateTime ?: 0L) / 1000
        val key = "$SHA_PREFIX$at"
        val sha = prefs.getString(key, null) ?: runCatching {
            val md = MessageDigest.getInstance("SHA-256")
            File(ctx.applicationInfo.sourceDir).inputStream().use { ins ->
                val buf = ByteArray(64 * 1024)
                while (true) { val n = ins.read(buf); if (n < 0) break; md.update(buf, 0, n) }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()?.also { prefs.edit().putString(key, it).apply() }
        return AppUpdate.Installed(sha, at)
    }

    /** Il file già scaricato e verificato per questa release, se c'è ancora ed è intero. */
    private fun cached(r: AppUpdate.Release): File? =
        File(File(ctx.cacheDir, DIR), AppUpdate.fileName(r.tag)).takeIf { it.exists() && (r.size <= 0 || it.length() == r.size) }

    /** Nella cache resta solo il file del tag proposto: gli altri APK e i download a metà se ne vanno. */
    private fun tidyCache(keep: String?) {
        val name = keep?.let(AppUpdate::fileName)
        File(ctx.cacheDir, DIR).listFiles()?.forEach { f -> if (f.name != name) f.delete() }
    }

    private fun connect(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = true
        setRequestProperty("User-Agent", USER_AGENT)
        setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream")
    }

    private fun get(url: String): String {
        val c = connect(url)
        val code = c.responseCode
        if (code !in 200..299) throw HttpFailure(code)
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private class HttpFailure(code: Int) : Exception("HTTP $code")

    private companion object {
        const val DIR = "updates"
        const val PART = ".part"
        const val CHECKED = "checked_at"
        const val DISMISSED = "dismissed"
        const val SHA_PREFIX = "installed_sha@"
        const val USER_AGENT = "cmwatch-mobile"
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
