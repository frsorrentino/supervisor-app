package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R

/*
 * La chiusura di una sessione, una struttura sola dalla domanda all'esito (app-chiudi-scheda-20-11, Franz 20/11 19:08: bozza C,
 * «va bene c ma non voglio il tasto bianco»). Nella pagina la carta prende il posto del campo di scrittura, senza velo; nella card
 * della home gli stessi testi e gli stessi tasti stanno dentro la card. I tasti: l'azione principale col velo e il bordo ambra
 * (i «Tasti B» di V3Pill e MiniButton, app-sessioni-compatte-06-11), gli altri tonali; una riga intera ciascuno, mai a capo.
 */

/** Le due metà stanno affiancate solo se ognuna entra intera nella sua metà; altrimenti vanno in colonna, a ogni misura del testo. */
internal fun closePairFits(a: Int, b: Int, gap: Int, max: Int): Boolean = maxOf(a, b) * 2 + gap <= max

internal enum class CloseKeyKind { MAIN, TONAL, DANGER }

@Composable
internal fun CloseKey(text: String, kind: CloseKeyKind, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val press = remember { MutableInteractionSource() }
    val main = kind == CloseKeyKind.MAIN
    val fg = when (kind) { CloseKeyKind.MAIN -> V3.Wait; CloseKeyKind.DANGER -> V3.Red; CloseKeyKind.TONAL -> V3.Tx }
    Surface(
        onClick = onClick, interactionSource = press, modifier = modifier.heightIn(min = 48.dp).pressScale(press), shape = pressShape(press),
        color = if (main) V3.Wait.copy(alpha = 0.14f) else V3.S3, contentColor = fg,
        border = if (main) BorderStroke(1.dp, V3.Wait.copy(alpha = 0.6f)) else null,
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(text, style = V3.Name.copy(fontSize = 16.sp, lineHeight = 20.sp, color = fg), maxLines = 1, softWrap = false)
        }
    }
}

/** Due tasti a metà, o uno sopra l'altro quando uno dei due non entra nella sua metà ([closePairFits]). */
@Composable
internal fun CloseKeyPair(first: @Composable () -> Unit, second: @Composable () -> Unit, modifier: Modifier = Modifier) =
    Layout({ first(); second() }, modifier.fillMaxWidth().testTag("close-pair")) { ms, c ->
        val gap = 8.dp.roundToPx()
        val max = c.maxWidth
        val half = (max - gap) / 2
        if (closePairFits(ms[0].maxIntrinsicWidth(Constraints.Infinity), ms[1].maxIntrinsicWidth(Constraints.Infinity), gap, max)) {
            val a = ms[0].measure(Constraints.fixedWidth(half)); val b = ms[1].measure(Constraints.fixedWidth(half))
            layout(max, maxOf(a.height, b.height)) { a.place(0, 0); b.place(half + gap, 0) }
        } else {
            val a = ms[0].measure(Constraints.fixedWidth(max)); val b = ms[1].measure(Constraints.fixedWidth(max))
            layout(max, a.height + gap + b.height) { a.place(0, 0); b.place(0, a.height + gap) }
        }
    }

/** La carta della pagina: il campo di scrittura diventa questa carta, poi torna campo o porta alla home. */
@Composable
internal fun CloseCardFrame(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Surface(
    modifier.widthIn(max = 560.dp).fillMaxWidth().testTag("close-card"), shape = RoundedCornerShape(20.dp), color = V3.S2,
    border = BorderStroke(1.dp, V3.Line), shadowElevation = 8.dp,
) {
    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
internal fun CloseTitle(text: String) = Text(text, style = V3.Name.copy(fontSize = 21.sp, lineHeight = 25.sp, color = V3.Tx))

@Composable
internal fun CloseLine(text: String, color: androidx.compose.ui.graphics.Color = V3.Tx2) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)

/** La sessione è aperta anche in una finestra sul PC: si dice prima, ed è un'informazione, non un divieto (supervisor-chiudi-attaccate-20-11). */
@Composable
internal fun CloseAttachedNote() = Surface(Modifier.fillMaxWidth().padding(top = 6.dp).testTag("close-attached"), shape = RoundedCornerShape(14.dp), color = V3.S3) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.Computer, null, tint = V3.Busy, modifier = Modifier.size(20.dp))
        Column {
            Text(stringResource(R.string.close_attached), style = MaterialTheme.typography.bodyMedium, color = V3.Tx)
            Text(stringResource(R.string.close_attached_line), style = MaterialTheme.typography.bodySmall, color = V3.Tx2)
        }
    }
}

/** I tasti della domanda: con [save] «Salva e chiudi» (contratto 1.155) e sotto «Lascia aperta» e «Chiudi senza salvare». */
@Composable
internal fun CloseAskKeys(save: Boolean, closeLabel: String, onKeep: () -> Unit, onClose: (save: Boolean) -> Unit) =
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (save) {
            CloseKey(stringResource(R.string.close_save), CloseKeyKind.MAIN, { onClose(true) }, Modifier.fillMaxWidth())
            CloseKeyPair(
                { CloseKey(stringResource(R.string.close_keep), CloseKeyKind.TONAL, onKeep) },
                { CloseKey(stringResource(R.string.close_no_save), CloseKeyKind.DANGER, { onClose(false) }) },
            )
        } else CloseKeyPair(
            { CloseKey(stringResource(R.string.close_keep), CloseKeyKind.TONAL, onKeep) },
            { CloseKey(closeLabel, CloseKeyKind.DANGER, { onClose(false) }) },
        )
    }

/** La domanda prima di chiudere [s]: nella pagina col titolo e cosa succede; nella card ([inCard]) la riga breve. */
@Composable
internal fun CloseAskBody(s: Session, save: Boolean, onKeep: () -> Unit, onClose: (save: Boolean) -> Unit, inCard: Boolean = false) {
    if (inCard) CloseLine(stringResource(if (save) R.string.close_inline_save else R.string.close_inline))
    else {
        CloseTitle(stringResource(R.string.close_title, s.name))
        CloseLine(closeStateLine(s))
    }
    // Contratto 1.161 (issue #4): su una sessione in uso al PC (dal 20/11 `attached` vale solo per quelle) o con un lavoro in
    // sottofondo (`close_preview` → `blocked_by: background`) Chiudi non si offre: il motivo e «Lascia aperta».
    val preview = LocalClosePreview.current
    var blocked by androidx.compose.runtime.remember(s.name) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(s.name, preview) { blocked = preview?.invoke(listOf(s.name))?.sessions?.firstOrNull { it.name == s.name }?.blockedBy }
    if (!it.pixelbox.cmwatch.rules.VociAssenti.close(s, blocked)) {
        CloseBlockedNote(background = !s.attached)
        Column(Modifier.fillMaxWidth().padding(top = 12.dp)) { CloseKey(stringResource(R.string.close_keep), CloseKeyKind.TONAL, onKeep, Modifier.fillMaxWidth()) }
        return
    }
    if (!inCard) Text(stringResource(if (save) R.string.close_what_save else R.string.close_what), style = MaterialTheme.typography.bodyMedium, color = V3.Tx2, modifier = Modifier.padding(top = 8.dp))
    CloseAskKeys(save, if (inCard) stringResource(R.string.cleanup_close_short) else stringResource(R.string.cleanup_close, s.name), onKeep, onClose)
}

/** La lettura `close_preview` del Chiudi (contratto 1.161); null senza l'op. */
val LocalClosePreview = androidx.compose.runtime.staticCompositionLocalOf<(suspend (List<String>) -> it.pixelbox.cmwatch.rules.CloseFinished.Preview?)?> { null }

/** Perché Chiudi non c'è: in uso al PC, o un lavoro in sottofondo. */
@Composable
internal fun CloseBlockedNote(background: Boolean) = Surface(Modifier.fillMaxWidth().padding(top = 6.dp).testTag("close-blocked"), shape = RoundedCornerShape(14.dp), color = V3.S3) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.Computer, null, tint = V3.Busy, modifier = Modifier.size(20.dp))
        Column {
            Text(stringResource(if (background) R.string.close_stop_background else R.string.close_stop_attached), style = MaterialTheme.typography.bodyMedium, color = V3.Tx)
            Text(stringResource(R.string.close_stop_line), style = MaterialTheme.typography.bodySmall, color = V3.Tx2)
        }
    }
}

/**
 * Il Chiudi rifiutato dal PC, col motivo in italiano, una volta sola (app-chiudi-esito-04-11; senza più lo snackbar sotto). Si
 * può armare (contratto 1.120): «Chiudi quando finisce» è l'azione principale, «Chiudi comunque» chiede conferma qui.
 */
@Composable
internal fun CloseRefusedBody(name: String, why: String, onArm: ((String) -> Unit)?, onDismiss: () -> Unit, inCard: Boolean = false) {
    var forceAsk by remember(name, why) { mutableStateOf(false) }
    if (forceAsk && onArm != null) {
        if (inCard) CloseLine(stringResource(R.string.close_force_ask, name), V3.Tx) else CloseTitle(stringResource(R.string.close_force_ask, name))
        Text(stringResource(R.string.close_force_what), style = MaterialTheme.typography.bodyMedium, color = V3.Tx2)
        CloseKeyPair(
            { CloseKey(stringResource(R.string.close_force_cancel), CloseKeyKind.TONAL, { forceAsk = false }) },
            { CloseKey(stringResource(R.string.close_force), CloseKeyKind.DANGER, { forceAsk = false; onArm("force") }) },
            Modifier.padding(top = 12.dp),
        )
    } else {
        if (inCard) CloseLine(stringResource(R.string.splash_refused, name), V3.Wait) else CloseTitle(stringResource(R.string.splash_refused, name))
        Text(why, style = MaterialTheme.typography.bodyMedium, color = if (inCard) V3.Tx2 else V3.Tx)
        if (onArm != null) Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CloseKey(stringResource(R.string.close_after), CloseKeyKind.MAIN, { onArm("after") }, Modifier.fillMaxWidth())
            CloseKeyPair(
                { CloseKey(stringResource(R.string.close_keep), CloseKeyKind.TONAL, onDismiss) },
                { CloseKey(stringResource(R.string.close_force), CloseKeyKind.DANGER, { forceAsk = true }) },
            )
        } else CloseKey(stringResource(R.string.splash_refused_ok), CloseKeyKind.MAIN, onDismiss, Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}
