package it.pixelbox.cmwatch.mobile.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
internal fun PagePreview(path: String, mime: String?) {
    val doc = remember(path) { runCatching { FileActions.pageDoc(mime, java.io.File(path).readText()) }.getOrNull() } ?: return
    val shape = MaterialTheme.shapes.large
    val box = Modifier.fillMaxWidth().height(360.dp).clip(shape).background(Color.White).border(1.dp, CmColors.line, shape)
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
