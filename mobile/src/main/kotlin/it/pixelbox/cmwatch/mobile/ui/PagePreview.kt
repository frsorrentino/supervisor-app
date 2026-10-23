package it.pixelbox.cmwatch.mobile.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MediaPreview
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import it.pixelbox.cmwatch.rules.FileActions
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Franz, 16/10 11:18: «le anteprime html dovrebbero apparire renderizzate e non come codice». HTML e SVG si disegnano in una
 * WebView separata con le regole di FileActions.PAGE_VIEW: JavaScript sì, nessuna interfaccia JS, niente file, content,
 * cookie e storage, niente navigazione e niente finestre. Fino a 1 MB (MediaPreview.TEXT_AUTO_MAX), letto dal file locale.
 * Negli snapshot (Paparazzi non ha la WebView) resta il riquadro vuoto.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PagePreview(path: String, mime: String?, fill: Boolean = false) {
    val doc = remember(path) { runCatching { FileActions.pageDoc(mime, java.io.File(path).readText()) }.getOrNull() } ?: return
    val shape = MaterialTheme.shapes.large
    // Franz 24/10 00:40 (anteprime in app): il riquadro è alto il 60% dello schermo (MediaPreview.pageHeight); a schermo intero riempie tutto.
    val box = if (fill) Modifier.fillMaxSize().background(Color.White)
    else Modifier.fillMaxWidth().height(MediaPreview.pageHeight(LocalConfiguration.current.screenHeightDp).dp).clip(shape).background(Color.White).border(1.dp, CmColors.line, shape)
    if (LocalInspectionMode.current) { Box(box); return }
    val rules = FileActions.PAGE_VIEW
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = rules.javaScript
                settings.allowFileAccess = rules.fileAccess
                settings.allowContentAccess = rules.contentAccess
                settings.domStorageEnabled = rules.domStorage
                settings.setSupportMultipleWindows(rules.windows)
                settings.javaScriptCanOpenWindowsAutomatically = rules.windows
                // L'app non usa altre WebView: i cookie spenti per tutte non tolgono niente.
                CookieManager.getInstance().setAcceptCookie(rules.cookies)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, rules.cookies)
                // Nessun link porta fuori dalla pagina: la WebView resta sull'anteprima.
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = !rules.navigation
                }
                loadDataWithBaseURL(null, doc, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
        modifier = box,
    )
}

/** Franz 24/10 00:40: la pagina a schermo intero, scorrevole e cliccabile, con il nome del file e «Chiudi». */
@Composable
internal fun PageFullDialog(path: String, mime: String?, name: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(CmColors.bg).safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = CmColors.text) }
            }
            PagePreview(path, mime, fill = true)
        }
    }
}
