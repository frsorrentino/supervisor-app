package it.pixelbox.cmwatch.mobile

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler
import androidx.core.content.ContextCompat
import it.pixelbox.cmwatch.rules.Links

/**
 * app-download-fuori-scheda-04-11 (Franz, 04/11 07:15: «i download da GitHub si fermano al 100% e non consentono lo
 * scaricamento»; il 03/11 lo stesso con un video). Un link a un file ([Links.download]) non va più al browser, dove restava
 * fermo al 100%, ma al gestore dei download di Android: il file finisce in Download con la sua notifica. Un APK, finito di
 * scaricare con l'app in primo piano, si apre nell'installatore; senza il permesso di installare si apre la sua schermata.
 * Ogni altro link va a [fallback], come prima.
 */
class DownloadLinks(private val activity: Activity, private val fallback: UriHandler) : UriHandler {
    private val apks = mutableSetOf<Long>()
    private var listening = false

    /** Contratto 1.130: un link `#/attesa/<id>` apre la scheda «Ti aspetta» dentro l'app (app-scheda-ti-aspetta-07-11). */
    var onWait: ((String) -> Unit)? = null
    /** Contratto 1.132: un link `#/monitor/<id>` apre la scheda di quel monitoraggio (app-monitoraggi-09-11). */
    var onMonitor: ((String) -> Unit)? = null

    override fun openUri(uri: String) {
        it.pixelbox.cmwatch.rules.TiAspetta.waitOf(uri)?.let { w -> onWait?.let { f -> f(w); return } }
        it.pixelbox.cmwatch.rules.Monitors.idOf(uri)?.let { m -> onMonitor?.let { f -> f(m); return } }
        val d = Links.download(uri) ?: return fallback.openUri(uri)
        val dm = activity.getSystemService(DownloadManager::class.java) ?: return fallback.openUri(uri)
        val req = DownloadManager.Request(Uri.parse(uri))
            .setTitle(d.name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, d.name)
        if (d.apk) req.setMimeType(APK_MIME)
        val id = runCatching { dm.enqueue(req) }.getOrElse { return fallback.openUri(uri) }
        Toast.makeText(activity, activity.getString(R.string.download_started, d.name), Toast.LENGTH_SHORT).show()
        if (d.apk && BuildConfig.SELF_UPDATE) { apks += id; listen() }
    }

    private fun listen() {
        if (listening) return
        listening = true
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (!apks.remove(id)) return
                install(id)
            }
        }
        // Il fine download arriva dal sistema: il ricevitore va esportato.
        ContextCompat.registerReceiver(activity.applicationContext, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
    }

    private fun install(id: Long) {
        val dm = activity.getSystemService(DownloadManager::class.java) ?: return
        val ok = runCatching {
            dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
                c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
            }
        }.getOrDefault(false)
        if (!ok) return
        val file = dm.getUriForDownloadedFile(id) ?: return
        // Senza il permesso l'installatore rifiuterebbe: prima la schermata del permesso; l'APK resta in Download e nella notifica.
        if (!activity.packageManager.canRequestPackageInstalls()) {
            runCatching { activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))) }
            return
        }
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(file, APK_MIME).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { activity.startActivity(view) }
    }

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
