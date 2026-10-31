package it.pixelbox.cmwatch.mobile.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.ShareAttachment
import it.pixelbox.cmwatch.mobile.ui.ShareScreen
import it.pixelbox.cmwatch.rules.ShareLimits
import it.pixelbox.cmwatch.transport.TransportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Il «Condividi» di Android verso una sessione (contratto 1.19): testo, immagine o qualunque file, via `claude-master report`. */
class ShareActivity : ComponentActivity() {
    private val app get() = application as PhoneApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val stream: Uri? = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        val mime = stream?.let { contentResolver.getType(it) } ?: intent.type
        val (name, size) = stream?.let(::nameAndSize) ?: ("file" to -1L)
        setContent {
            CmPhoneTheme {
                val snap by app.repo.snapshot.collectAsStateWithLifecycle()
                var sending by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val state = snap.state ?: return@CmPhoneTheme
                val kind = stream?.let { ShareLimits.incoming(mime, size, state.share) }
                val limit = ShareLimits.maxFileBytes(state.share)
                val attached = when (kind) {
                    null -> null
                    ShareLimits.Incoming.IMAGE -> ShareAttachment(getString(R.string.share_with_image), true)
                    ShareLimits.Incoming.FILE -> ShareAttachment(getString(R.string.share_with_file, name, sizeText(size), sizeText(limit)), false)
                    ShareLimits.Incoming.TOO_BIG -> ShareAttachment(getString(R.string.share_file_too_big, name, sizeText(size), sizeText(limit)), false, blocked = true)
                    ShareLimits.Incoming.NOT_SUPPORTED -> ShareAttachment(getString(R.string.share_file_unsupported), false, blocked = true)
                }
                ShareScreen(state, text, attached, sending = sending) { session, message ->
                    sending = true
                    scope.launch {
                        val note = runCatching {
                            if (stream == null) {
                                app.repo.report(session, message, null, null, state.share?.maxBytes ?: 0)
                            } else if (kind == ShareLimits.Incoming.IMAGE) {
                                val bytes = withContext(Dispatchers.IO) { ImageShrink.jpeg(this@ShareActivity, stream) }
                                    ?: return@runCatching getString(R.string.share_unreadable)
                                app.repo.report(session, message, "image/jpeg", bytes, state.share?.maxBytes ?: 0)
                            } else {
                                // Lo stesso percorso degli allegati del tasto +: il file com'è, a pezzi se il relay li accetta.
                                val bytes = withContext(Dispatchers.IO) { contentResolver.openInputStream(stream)?.use { it.readBytes() } }
                                    ?: return@runCatching getString(R.string.share_file_unreadable)
                                if (bytes.size > limit) return@runCatching getString(R.string.file_too_big, sizeText(limit))
                                app.repo.report(session, message, mime ?: "application/octet-stream", bytes, state.share?.maxBytes ?: 0, name = name, partsMax = state.share?.takeIf { it.parts }?.maxPartsBytes)
                            }
                            getString(R.string.share_sent, session)
                        }.getOrElse { e ->
                            when (e) {
                                is TransportException.TooLarge -> getString(if (kind == ShareLimits.Incoming.IMAGE) R.string.share_too_large else R.string.file_too_big, sizeText(limit))
                                is TransportException.Network -> getString(R.string.share_offline)
                                else -> getString(R.string.share_failed)
                            }
                        }
                        Toast.makeText(this@ShareActivity, note, Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun nameAndSize(uri: Uri): Pair<String, Long>? = runCatching {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) (c.getString(0) ?: "file") to (if (c.isNull(1)) -1L else c.getLong(1)) else null
        }
    }.getOrNull()

    private fun sizeText(bytes: Long) = android.text.format.Formatter.formatShortFileSize(this, bytes.coerceAtLeast(0))
}
