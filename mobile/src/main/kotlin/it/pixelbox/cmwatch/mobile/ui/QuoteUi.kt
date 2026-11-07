package it.pixelbox.cmwatch.mobile.ui

import android.content.ClipboardManager
import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Quotes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Citare la trascrizione (app-citazione-trascrizione-07-11, Franz 07/11 20:06): «Cita» accanto a «Copia» sulla selezione,
 * «Cita tutto» per il messaggio intero; i pezzi stanno sopra il campo con l'ora della risposta e una ×, anche più d'uno, e
 * partono in Markdown con la risposta (Quotes.markdown).
 */
class QuoteDraft {
    data class Item(val id: Long, val text: String, val at: Long?)
    val items = mutableStateListOf<Item>()
    private var next = 0L
    fun add(text: String, at: Long?) { Quotes.cut(text).takeIf { it.isNotEmpty() }?.let { t -> items += Item(next++, t, at) } }
    fun remove(id: Long) { items.removeAll { it.id == id } }
    /** Il testo da mandare con la risposta; svuota le citazioni. */
    fun take(reply: String): String = Quotes.markdown(items.map { it.text }, reply).also { items.clear() }
}

/** Le citazioni della sessione aperta: le scrive la trascrizione, le legge il campo. */
internal val LocalQuoteDraft = staticCompositionLocalOf<QuoteDraft?> { null }

/**
 * Il menu della selezione con «Cita» e «Cita tutto» accanto a «Copia» e «Seleziona tutto». Compose non dà il testo selezionato:
 * «Cita» lo fa copiare negli appunti, lo legge e rimette negli appunti quello che c'era.
 */
internal class QuoteToolbar(
    private val view: View, private val labels: List<String>, private val onQuote: (String) -> Unit, private val onQuoteAll: () -> Unit,
) : TextToolbar {
    private var mode: ActionMode? = null
    private var rect = Rect.Zero
    private var copy: (() -> Unit)? = null
    private var all: (() -> Unit)? = null
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    private val callback = object : ActionMode.Callback2() {
        override fun onCreateActionMode(m: ActionMode, menu: Menu): Boolean { fill(menu); return true }
        override fun onPrepareActionMode(m: ActionMode, menu: Menu): Boolean { menu.clear(); fill(menu); return true }
        override fun onActionItemClicked(m: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                COPY -> copy?.invoke()
                QUOTE -> quoteSelection()
                QUOTE_ALL -> onQuoteAll()
                ALL -> { all?.invoke(); return true }
            }
            hide()
            return true
        }
        override fun onDestroyActionMode(m: ActionMode) { mode = null; status = TextToolbarStatus.Hidden }
        override fun onGetContentRect(m: ActionMode, v: View, out: android.graphics.Rect) {
            out.set(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
        }
    }

    private fun fill(menu: Menu) {
        if (copy != null) menu.add(0, COPY, 0, labels[0]).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        if (copy != null) menu.add(0, QUOTE, 1, labels[1]).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, QUOTE_ALL, 2, labels[2]).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        if (all != null) menu.add(0, ALL, 3, labels[3]).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    private fun quoteSelection() {
        val cm = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val before = runCatching { cm?.primaryClip }.getOrNull()
        copy?.invoke()
        // La copia di Compose può arrivare un attimo dopo: si legge al giro dopo e si rimette quello che c'era.
        view.postDelayed({
            val text = runCatching { cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(view.context)?.toString() }.getOrNull()
            runCatching { if (before != null) cm?.setPrimaryClip(before) else if (android.os.Build.VERSION.SDK_INT >= 28) cm?.clearPrimaryClip() }
            text?.takeIf { it.isNotBlank() }?.let(onQuote)
        }, 80)
    }

    override fun showMenu(rect: Rect, onCopyRequested: (() -> Unit)?, onPasteRequested: (() -> Unit)?, onCutRequested: (() -> Unit)?, onSelectAllRequested: (() -> Unit)?) {
        this.rect = rect; copy = onCopyRequested; all = onSelectAllRequested
        val m = mode
        if (m == null) { mode = view.startActionMode(callback, ActionMode.TYPE_FLOATING); status = TextToolbarStatus.Shown } else m.invalidate()
    }

    override fun hide() { mode?.finish(); mode = null; status = TextToolbarStatus.Hidden }

    private companion object { const val COPY = 1; const val QUOTE = 2; const val QUOTE_ALL = 3; const val ALL = 4 }
}

/** Un testo della trascrizione che si può citare: il menu della selezione ha «Cita» e «Cita tutto» (`text`, con l'ora `at`). */
@Composable
internal fun Quotable(text: String, at: Long?, content: @Composable () -> Unit) {
    val draft = LocalQuoteDraft.current
    if (draft == null) { content(); return }
    val view = LocalView.current
    val labels = listOf(stringResource(android.R.string.copy), stringResource(R.string.quote), stringResource(R.string.quote_all), stringResource(android.R.string.selectAll))
    val bar = remember(view, text, at, labels) { QuoteToolbar(view, labels, { t -> draft.add(t, at) }, { draft.add(text, at) }) }
    CompositionLocalProvider(LocalTextToolbar provides bar) { content() }
}

private val HM = DateTimeFormatter.ofPattern("HH:mm")

/** I pezzi citati sopra il campo: la riga a sinistra, l'ora della risposta, il testo in tre righe al più e la × per toglierlo. */
@Composable
internal fun QuoteChips(draft: QuoteDraft) {
    if (draft.items.isEmpty()) return
    Column(Modifier.fillMaxWidth().testTag("citazioni"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        draft.items.forEach { q ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(SubShape).border(1.dp, V3.S3, SubShape).testTag("citazione-${q.id}"),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(V3.Tx3))
                Column(Modifier.weight(1f).padding(start = 10.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    q.at?.let { t -> Text(stringResource(R.string.quote_from, HM.format(Instant.ofEpochSecond(t).atZone(ZoneId.systemDefault()))), style = V3.Meter) }
                    Text(q.text, style = V3.Small.copy(color = V3.Tx), maxLines = 3, overflow = TextOverflow.Clip)
                }
                IconButton(onClick = { draft.remove(q.id) }, modifier = Modifier.size(40.dp).handCursor().testTag("citazione-togli-${q.id}")) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.quote_remove), tint = V3.Tx2, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
