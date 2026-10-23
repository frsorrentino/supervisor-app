package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Elsewhere
import it.pixelbox.cmwatch.ui.tokens.CmColors
import kotlin.math.abs
import kotlin.math.roundToInt

/** Oltre questo trascinamento di lato la pastiglia è chiusa (come SWIPE_PX della web). */
private const val SWIPE_DP = 80

/**
 * L'avviso delle altre sessioni sotto la barra della chat (Franz, 02/10 20:47, variante A dei mockup): una riga sola,
 * la mano ambra per chi ti aspetta, la bandierina verde per un turno finito, con ✕. Il tocco apre la sessione, o la fila
 * «Ti aspettano» quando sono più d'una. Franz 23/10: anche chi ti aspetta si chiude, con la ✕ o trascinando di lato;
 * torna quando arriva una richiesta nuova (`Elsewhere.seenKeys`).
 */
@Composable
fun ElsewherePill(alert: Elsewhere.Alert, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val waiting = alert is Elsewhere.Waiting
    val tone = if (waiting) CmColors.briefWarn else CmColors.briefGood
    val text = when (alert) {
        is Elsewhere.Waiting -> if (alert.sessions.size == 1) stringResource(R.string.elsewhere_waiting, alert.sessions[0])
            else stringResource(R.string.elsewhere_waiting_many, alert.sessions.size)
        is Elsewhere.Finished -> stringResource(R.string.elsewhere_finished, alert.session)
    }
    val swipePx = with(LocalDensity.current) { SWIPE_DP.dp.toPx() }
    var dx by remember(alert.key) { mutableFloatStateOf(0f) }
    Surface(
        onClick = onOpen, color = tone.copy(alpha = 0.16f), shape = CircleShape,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 44.dp)
            .offset { IntOffset(dx.roundToInt(), 0) }
            .graphicsLayer { alpha = 1f - (abs(dx) / (swipePx * 2)).coerceIn(0f, 1f) }
            .draggable(rememberDraggableState { d -> dx += d }, Orientation.Horizontal, onDragStopped = { if (abs(dx) > swipePx) onDismiss() else dx = 0f }),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(if (waiting) R.drawable.ic_hand else R.drawable.ic_flag), null, tint = tone, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = CmColors.text,
                maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            Text(stringResource(if (waiting) R.string.elsewhere_reply else R.string.elsewhere_open), style = MaterialTheme.typography.labelLarge, color = tone)
            if (waiting) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = tone, modifier = Modifier.size(20.dp))
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = CmColors.text2, modifier = Modifier.size(18.dp)) }
        }
    }
}
