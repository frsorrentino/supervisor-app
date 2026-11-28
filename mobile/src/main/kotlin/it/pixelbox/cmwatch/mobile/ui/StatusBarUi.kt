package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.IconRowRules
import it.pixelbox.cmwatch.rules.SessionTabs
import it.pixelbox.cmwatch.rules.StatusBar

/** Le schede della capsula, nell'ordine: Per te, Risultati, Modifiche. */
private val LEFT = listOf(SessionTabs.Key.PERTE, SessionTabs.Key.RISULTATI, SessionTabs.Key.MODIFICHE)

/**
 * La barra della sessione (app-barra-ibrida-28-11, H1 di dalla-master/barra-ibrida-28-11.html, al posto della barra C sottile; le regole in
 * [StatusBar], le stesse di web/src/lib/StatusBar.svelte): sotto il titolo della sessione, e nel foglio aperto della master al posto
 * dell'ultimo esito, la capsula della riga di icone di prima ([IconRow], alta 50 con la pillola elastica del Rubber dock e il nome sulla
 * scheda aperta) con Per te, Risultati e Modifiche e i loro numeri: un tocco fa scendere la loro tendina, un altro sull'aperta la richiude.
 * Nella stessa capsula, dopo un separatore, lo stato: modello · effort, la quota delle 5 ore (un orologio e «5h 11%», quanto è usato, al
 * posto della batteria: «non mi piace la batteria, oltre che non capisco cosa rappresenti», Franz) e l'anellino del contesto: un tocco
 * apre Dettagli. Con poco spazio lo stato si riduce a passi ([StatusBar.step], con le schede a riposo col nome più lungo aperto e la scala
 * del testo di sistema); le icone coi numeri restano. La riga: 6 dp sopra, la capsula da 50, 8 sotto e la riga sottile; la testata della
 * sessione a 360 dp è alta circa 108. Alt+1…5 ed Esc li tiene chi la ospita (SessionSheet). [more]: il ⋮ quando la pagina non ne ha uno.
 */
@Composable
internal fun StatusBarRow(
    counts: SessionTabs.Counts, active: SessionTabs.Key, onPick: (String) -> Unit,
    model: String?, effort: String?, quota: QuotaAccount?, ctx: Int?, bg: Color, more: (@Composable () -> Unit)? = null,
) {
    val hand = ImageVector.vectorResource(R.drawable.ic_hand)
    val scale = LocalDensity.current.fontScale
    val used = StatusBar.used(quota?.h5)
    val labels = mapOf(
        SessionTabs.Key.PERTE to stringResource(R.string.fy_title),
        SessionTabs.Key.RISULTATI to stringResource(R.string.watch_results),
        SessionTabs.Key.MODIFICHE to stringResource(R.string.changes_title),
    )
    val icons = mapOf(SessionTabs.Key.PERTE to hand, SessionTabs.Key.RISULTATI to Icons.Outlined.Visibility, SessionTabs.Key.MODIFICHE to Icons.Outlined.Difference)
    val tabs = LEFT.map { k -> IconTab(k.id, icons.getValue(k), labels.getValue(k), count = SessionTabs.count(k, counts), tone = SessionTabs.tone(k)) }
    val stateDesc = listOfNotNull(
        listOfNotNull(model, effort).joinToString(" · ").takeIf { x -> x.isNotEmpty() },
        used?.let { n -> stringResource(R.string.bar_quota_used, n) },
        ctx?.let { n -> stringResource(R.string.bar_ctx, n) },
    ).joinToString(", ") + ". " + stringResource(R.string.bar_open_details)
    Column(Modifier.fillMaxWidth().background(bg).testTag("barra-stato")) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoxWithConstraints(Modifier.weight(1f).padding(horizontal = StatusBar.ROW_PAD.dp), contentAlignment = Alignment.Center) {
                val tight = IconRowRules.tight(maxWidth.value)
                val rest = iconRowRest(tabs, tight)
                val parts = StatusBar.Parts(model, effort, used, ctx)
                val step = StatusBar.step(maxWidth.value + StatusBar.ROW_PAD * 2f, rest, parts, scale)
                IconRow(tabs, active.id, onPick, keys = false, trailing = {
                    BarState(step, model, effort, used, quota?.stale == true, ctx, on = active == SessionTabs.Key.DETTAGLI, desc = stateDesc) {
                        onPick(SessionTabs.Key.DETTAGLI.id)
                    }
                })
            }
            more?.invoke()
        }
        HorizontalDivider(color = V3.Line)
    }
}

private fun stateTone(t: StatusBar.Tone): Color = when (t) {
    StatusBar.Tone.ALERT -> V3.Red
    StatusBar.Tone.WARN -> V3.Wait
    StatusBar.Tone.NEUTRAL -> V3.Tx2
}

/**
 * Lo stato nella capsula al passo [step], dopo il separatore: modello · effort, l'orologio con la quota usata, l'anellino del contesto; un
 * tocco apre Dettagli. Le misure di [StatusBar]: 4 di margine, la riga da 1, 10 dopo, 6 in fondo; 28 dp d'altezza.
 */
@Composable
private fun BarState(step: StatusBar.Step, model: String?, effort: String?, used: Int?, stale: Boolean, ctx: Int?, on: Boolean, desc: String, onClick: () -> Unit) {
    val small = step != StatusBar.Step.FULL && step != StatusBar.Step.NO_EFFORT
    val fs = if (small) StatusBar.TEXT_SMALL else StatusBar.TEXT
    val style = TextStyle(fontFamily = V3.Mono, fontWeight = FontWeight.Medium, fontSize = fs.sp, lineHeight = (fs + 2f).sp)
    val text = StatusBar.modelText(model, effort, step)
    val pcts = step != StatusBar.Step.NO_PCT
    val line = V3.Line
    Row(
        Modifier.padding(start = 4.dp).height(28.dp)
            .drawBehind { drawLine(line, Offset(0.5.dp.toPx(), 0f), Offset(0.5.dp.toPx(), size.height), strokeWidth = 1.dp.toPx()) }
            .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
            .selectable(selected = on, role = Role.Tab, onClick = onClick).handCursor()
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .padding(start = 11.dp, end = StatusBar.END.dp).testTag("barra-dettagli"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((if (small) StatusBar.ITEM_GAP_SMALL else StatusBar.ITEM_GAP).dp),
    ) {
        if (text.isNotEmpty()) Text(text, style = style.copy(color = if (on) V3.Tx else V3.Tx2), maxLines = 1, softWrap = false)
        used?.let { n ->
            val c = stateTone(StatusBar.quotaTone(n))
            Row(Modifier.alpha(if (stale) 0.55f else 1f).testTag("barra-quota"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StatusBar.GLYPH_GAP.dp)) {
                Icon(Icons.Rounded.Schedule, null, tint = c, modifier = Modifier.size(StatusBar.GLYPH.dp))
                if (pcts) Text(StatusBar.quotaText(n), style = style.copy(color = c), maxLines = 1, softWrap = false)
            }
        }
        ctx?.let { n ->
            val c = stateTone(StatusBar.ringTone(n))
            Row(Modifier.testTag("barra-contesto"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StatusBar.GLYPH_GAP.dp)) {
                Ring(n, c)
                if (pcts) Text("$n%", style = style.copy(color = c), maxLines = 1, softWrap = false)
            }
        }
    }
}

/** L'anellino del contesto da 12 dp: il binario e l'arco. */
@Composable
private fun Ring(pct: Int, color: Color) {
    val track = V3.S3
    Canvas(Modifier.size(StatusBar.GLYPH.dp)) {
        val w = 2.2.dp.toPx()
        val inset = w / 2f + 0.3.dp.toPx()
        val corner = Offset(inset, inset)
        val box = Size(size.width - 2f * inset, size.height - 2f * inset)
        drawArc(track, -90f, 360f, false, topLeft = corner, size = box, style = Stroke(w))
        if (pct > 0) drawArc(color, -90f, 360f * pct.coerceIn(0, 100) / 100f, false, topLeft = corner, size = box, style = Stroke(w))
    }
}
