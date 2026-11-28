package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.IconRowRules
import it.pixelbox.cmwatch.rules.SessionTabs
import it.pixelbox.cmwatch.rules.StatusBar

/** Le icone di sinistra, nell'ordine: Per te, Risultati, Modifiche. */
private val LEFT = listOf(SessionTabs.Key.PERTE, SessionTabs.Key.RISULTATI, SessionTabs.Key.MODIFICHE)

/**
 * La barra di stato (app-barra-stato-28-11, variante C di dalla-master/barra-stato-28-11.html; le regole in [StatusBar], le stesse di
 * web/src/lib/StatusBar.svelte): alta 28 dp con la riga sottile sotto, subito sotto il titolo della sessione, e nel foglio aperto della
 * master, al posto della riga di icone. A sinistra Per te, Risultati e Modifiche col loro numero (a zero solo l'icona): un tocco fa
 * scendere la loro tendina, un altro sull'icona aperta la richiude. A destra modello · effort, la quota delle 5 ore come batteria
 * (quanto resta) e il contesto come anellino: un tocco apre Dettagli. Con poco spazio il passo lo sceglie [StatusBar.step] dalla
 * larghezza della barra e dalla scala del testo di sistema: prima il testo più piccolo, poi il modello senza effort, poi le percentuali
 * dentro i glifi, infine solo i glifi; i numeri delle icone restano. [more]: il ⋮ della sessione quando la pagina non ne ha uno.
 */
@Composable
internal fun StatusBarRow(
    counts: SessionTabs.Counts, active: SessionTabs.Key, onPick: (String) -> Unit,
    model: String?, effort: String?, quota: QuotaAccount?, ctx: Int?, bg: Color, more: (@Composable () -> Unit)? = null,
    /** I margini ai lati: 12, e 16 nel foglio della master, come le sue schede (app-foglio-master-testata-28-11). */
    pad: Float = StatusBar.PAD,
) {
    val hand = ImageVector.vectorResource(R.drawable.ic_hand)
    val scale = LocalDensity.current.fontScale
    val left = quota?.h5?.let { used -> StatusBar.remaining(used) }
    val labels = mapOf(
        SessionTabs.Key.PERTE to stringResource(R.string.fy_title),
        SessionTabs.Key.RISULTATI to stringResource(R.string.watch_results),
        SessionTabs.Key.MODIFICHE to stringResource(R.string.changes_title),
    )
    val icons = mapOf(SessionTabs.Key.PERTE to hand, SessionTabs.Key.RISULTATI to Icons.Outlined.Visibility, SessionTabs.Key.MODIFICHE to Icons.Outlined.Difference)
    val stateDesc = listOfNotNull(
        listOfNotNull(model, effort).joinToString(" · ").takeIf { x -> x.isNotEmpty() },
        left?.let { n -> stringResource(R.string.bar_quota_left, n) },
        ctx?.let { n -> stringResource(R.string.bar_ctx, n) },
    ).joinToString(", ") + ". " + stringResource(R.string.bar_open_details)
    Column(Modifier.fillMaxWidth().background(bg).testTag("barra-stato")) {
        // Il ⋮, quando sta qui (una pagina senza testata), tiene i suoi 48 dp: la riga cresce con lui invece di schiacciarlo.
        Row(Modifier.fillMaxWidth().heightIn(min = StatusBar.BAR_DP.dp), verticalAlignment = Alignment.CenterVertically) {
            BoxWithConstraints(Modifier.weight(1f).height(StatusBar.BAR_DP.dp)) {
                val parts = StatusBar.Parts(LEFT.map { k -> SessionTabs.count(k, counts) }, model, effort, left, ctx)
                val step = StatusBar.step(maxWidth.value, parts, scale, pad)
                // Il tocco delle icone prende 5 dp per lato e quello dello stato 6: i margini qui li tolgono, così le distanze restano
                // quelle di StatusBar (12 ai lati, 10 fra le icone).
                Row(
                    Modifier.fillMaxSize().padding(start = (pad - 5f).dp, end = (pad - 6f).dp).selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        LEFT.forEach { k ->
                            BarIcon(icons.getValue(k), labels.getValue(k), SessionTabs.count(k, counts), SessionTabs.tone(k), on = active == k, tag = "barra-${k.id}") { onPick(k.id) }
                        }
                    }
                    BarState(step, model, effort, left, quota?.stale == true, ctx, on = active == SessionTabs.Key.DETTAGLI, desc = stateDesc) { onPick(SessionTabs.Key.DETTAGLI.id) }
                }
            }
            more?.invoke()
        }
        HorizontalDivider(color = V3.Line)
    }
}

private val NumStyle get() = TextStyle(fontFamily = V3.Mono, fontWeight = FontWeight.Medium, fontSize = StatusBar.NUM.sp, lineHeight = (StatusBar.NUM + 2f).sp, fontFeatureSettings = "tnum")

private fun barTone(t: IconRowRules.Tone): Color = when (t) {
    IconRowRules.Tone.WAIT -> V3.Wait
    IconRowRules.Tone.NEW -> V3.Good
    IconRowRules.Tone.COUNT -> V3.Tx2
}

private fun stateTone(t: StatusBar.Tone): Color = when (t) {
    StatusBar.Tone.ALERT -> V3.Red
    StatusBar.Tone.WARN -> V3.Wait
    StatusBar.Tone.NEUTRAL -> V3.Tx2
}

/** Un'icona di sinistra col suo numero (a zero solo l'icona); l'aperta più chiara. */
@Composable
private fun BarIcon(icon: ImageVector, label: String, count: Int, tone: IconRowRules.Tone, on: Boolean, tag: String, onClick: () -> Unit) {
    val desc = if (count > 0) "$label $count" else label
    Row(
        Modifier.fillMaxHeight().clip(RoundedCornerShape(6.dp)).selectable(selected = on, role = Role.Tab, onClick = onClick).handCursor()
            .semantics(mergeDescendants = true) { contentDescription = desc }.padding(horizontal = 5.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StatusBar.NUM_GAP.dp),
    ) {
        Icon(icon, null, tint = if (on) V3.Tx else V3.Tx2, modifier = Modifier.size(StatusBar.ICON.dp))
        if (count > 0) Text(count.toString(), style = NumStyle.copy(color = barTone(tone)), maxLines = 1, softWrap = false)
    }
}

/** Lo stato di destra al passo [step]: modello · effort, la batteria con quanto resta, l'anellino del contesto; un tocco apre Dettagli. */
@Composable
private fun BarState(step: StatusBar.Step, model: String?, effort: String?, left: Int?, stale: Boolean, ctx: Int?, on: Boolean, desc: String, onClick: () -> Unit) {
    val size = if (step == StatusBar.Step.FULL) StatusBar.TEXT else StatusBar.TEXT_SMALL
    val style = TextStyle(fontFamily = V3.Mono, fontWeight = FontWeight.Medium, fontSize = size.sp, lineHeight = (size + 2f).sp)
    val text = StatusBar.modelText(model, effort, step)
    val pcts = step == StatusBar.Step.FULL || step == StatusBar.Step.SMALL || step == StatusBar.Step.NO_EFFORT
    val inside = step == StatusBar.Step.INSIDE
    Row(
        Modifier.fillMaxHeight().clip(RoundedCornerShape(6.dp)).selectable(selected = on, role = Role.Tab, onClick = onClick).handCursor()
            .semantics(mergeDescendants = true) { contentDescription = desc }.padding(horizontal = 6.dp).testTag("barra-dettagli"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StatusBar.ITEM_GAP.dp),
    ) {
        if (text.isNotEmpty()) Text(text, style = style.copy(color = if (on) V3.Tx else V3.Tx2), maxLines = 1, softWrap = false)
        left?.let { n ->
            val c = stateTone(StatusBar.batteryTone(n))
            Row(Modifier.alpha(if (stale) 0.55f else 1f).testTag("barra-batteria"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StatusBar.GLYPH_GAP.dp)) {
                Battery(n, c, inside)
                if (pcts) Text("$n%", style = style.copy(color = c), maxLines = 1, softWrap = false)
            }
        }
        ctx?.let { n ->
            val c = stateTone(StatusBar.ringTone(n))
            Row(Modifier.testTag("barra-contesto"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StatusBar.GLYPH_GAP.dp)) {
                Ring(n, c, inside)
                if (pcts) Text("$n%", style = style.copy(color = c), maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Il numero dentro i glifi: [StatusBar.IN_TEXT] dp fissi, come i glifi, così non ne esce mai, anche col testo di sistema grande. */
@Composable
private fun inStyle(color: Color): TextStyle {
    val fs = with(LocalDensity.current) { StatusBar.IN_TEXT.dp.toSp() }
    return TextStyle(fontFamily = V3.Mono, fontWeight = FontWeight.SemiBold, fontSize = fs, lineHeight = fs, color = color, textAlign = TextAlign.Center)
}

/** La batteria: il corpo col bordo, la punta fuori, il pieno quanto resta; col numero dentro il pieno si fa leggero. */
@Composable
private fun Battery(left: Int, color: Color, inside: Boolean) {
    val body = (if (inside) StatusBar.BAT_IN else StatusBar.BAT) - 3.5f
    val h = if (inside) 12f else 10f
    Box(Modifier.size(width = (body + 3.5f).dp, height = h.dp), contentAlignment = Alignment.CenterStart) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.3.dp.toPx()
            val bodyW = body.dp.toPx()
            drawRoundRect(color, topLeft = Offset(stroke / 2f, stroke / 2f), size = Size(bodyW - stroke, size.height - stroke), cornerRadius = CornerRadius(2.5.dp.toPx()), style = Stroke(stroke))
            val capH = 3.6.dp.toPx()
            drawRoundRect(color, topLeft = Offset(bodyW + 1.5.dp.toPx(), (size.height - capH) / 2f), size = Size(2.dp.toPx(), capH), cornerRadius = CornerRadius(1.dp.toPx()))
            val pad = stroke
            val inner = (bodyW - 2f * pad).coerceAtLeast(0f)
            drawRoundRect(
                color.copy(alpha = if (inside) 0.3f else 1f), topLeft = Offset(pad, pad), size = Size(inner * left.coerceIn(0, 100) / 100f, size.height - 2f * pad),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
        if (inside) Text(left.toString(), style = inStyle(color), maxLines = 1, softWrap = false, modifier = Modifier.width(body.dp))
    }
}

/** L'anellino del contesto: il binario e l'arco; col numero dentro è più grande. */
@Composable
private fun Ring(pct: Int, color: Color, inside: Boolean) {
    val d = if (inside) StatusBar.RING_IN else StatusBar.RING
    val track = V3.S3
    Box(Modifier.size(d.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = (if (inside) 1.6f else 2.2f).dp.toPx()
            val inset = w / 2f + (if (inside) 0.2f else 0.3f).dp.toPx()
            val corner = Offset(inset, inset)
            val box = Size(size.width - 2f * inset, size.height - 2f * inset)
            drawArc(track, -90f, 360f, false, topLeft = corner, size = box, style = Stroke(w))
            if (pct > 0) drawArc(color, -90f, 360f * pct.coerceIn(0, 100) / 100f, false, topLeft = corner, size = box, style = Stroke(w))
        }
        if (inside) Text(pct.toString(), style = inStyle(color), maxLines = 1, softWrap = false)
    }
}
