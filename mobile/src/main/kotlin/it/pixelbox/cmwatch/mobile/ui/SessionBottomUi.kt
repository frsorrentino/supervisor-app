package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SessionBottom

// In basso nella sessione (app-posizioni-sistema-27-11 gruppo 3, app-posizioni-basso-27-11: in alto si consulta, in basso si scrive), come
// RigaAzioni.svelte e l'anellino di Composer.svelte sul web; misure e colori del mockup dalla-master/posizioni-2026-11-27/pagina/m/a-regia.html.
// Le regole in SessionBottom.

/** Un'azione della riga sopra il campo: il testo, se sblocca un lavoro fermo (contratto 1.38), se è vecchia (ripetuta in tre risposte). */
/** `terminal`: il suggerito del terminale (app-posizioni-sistema-27-11 gruppo 4, nel foglio della master), col segno del prompt. */
internal data class AzioneChip(val text: String, val blocking: Boolean = false, val old: Boolean = false, val terminal: Boolean = false)

/**
 * La riga delle Azioni sopra il campo: i Prossimi della sessione in chip da 32 dp su una riga che scorre di lato, 6 dp fra l'uno e l'altro.
 * C'è solo a campo vuoto ([SessionBottom.rowShown]). Il tocco porta l'azione nel campo, come la riga dell'elenco di prima; tenuta premuta
 * la toglie, e le tolte non tornano. Quelle che sbloccano un lavoro fermo hanno il lucchetto al posto del fulmine; le vecchie stanno in
 * fondo, attenuate. Dal 70 % di contesto il primo chip, ambra, è «Contesto 72% · Handoff» e fa l'handoff di prima.
 */
@Composable
internal fun AzioniRow(
    actions: List<AzioneChip>, ctxPct: Int?, onPick: (String) -> Unit, onHandoff: () -> Unit, onDismiss: ((String) -> Unit)?, modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().testTag("azioni").horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ctxPct != null) AzioneChipView(stringResource(R.string.ctx_chip, ctxPct), Icons.Outlined.Layers, onClick = onHandoff, amber = true, tag = "azioni-contesto")
        actions.forEach { a ->
            AzioneChipView(
                a.text, if (a.terminal) Icons.Outlined.Terminal else if (a.blocking) Icons.Outlined.Lock else Icons.Outlined.Bolt, onClick = { onPick(a.text) }, faded = a.old,
                desc = if (a.blocking) stringResource(R.string.steps_unblock) else null,
                onLongClick = onDismiss?.takeIf { !a.terminal }?.let { d -> { d(a.text) } },
            )
        }
    }
}

/** Un chip della riga: icona da 16 e testo su una riga sola; ambra il chip del contesto, attenuate le azioni vecchie. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AzioneChipView(
    text: String, icon: ImageVector, onClick: () -> Unit, amber: Boolean = false, faded: Boolean = false, desc: String? = null, tag: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val fill = if (amber) V3.Wait.copy(alpha = 0.12f).compositeOver(V3.S2) else V3.S2
    val line = if (amber) V3.Wait.copy(alpha = 0.5f) else V3.Line
    val ink = when {
        amber -> V3.Wait
        faded -> V3.Tx2
        else -> V3.Tx
    }
    Row(
        Modifier.height(32.dp).clip(CircleShape).background(fill).border(1.dp, line, CircleShape).handCursor()
            .combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, desc, tint = if (amber) V3.Wait else V3.Tx2, modifier = Modifier.size(16.dp))
        Text(text, style = ChipText.copy(color = ink), maxLines = 1, softWrap = false)
    }
}

private val ChipText get() = TextStyle(fontFamily = V3.Sans, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, lineHeight = 16.sp)
