package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.SystemStatus
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SystemRules
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Contratto 1.90: il colore di un livello (green, yellow, red), come le barre del contesto. */
internal fun systemTone(level: String): Color = when (level) {
    "red" -> CmColors.briefAlertRing
    "yellow" -> CmColors.briefWarn
    else -> CmColors.briefGood
}

@Composable
private fun italian(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "it"

/** La riga compatta: carico per core e memoria libera sul totale. */
@Composable
internal fun systemSummary(s: SystemStatus): String {
    val c = italian()
    return stringResource(R.string.system_summary, SystemRules.num(s.loadPerCore, c), SystemRules.num(s.memAvailableMb / 1024.0, c), SystemRules.num(s.memTotalMb / 1024.0, c))
}

/** Il titolo della sezione «Sistema»: come gli altri, col pallino del livello; compatto, a destra la riga del carico. */
@Composable
internal fun SystemHeader(s: SystemStatus, open: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().handCursor().clickable(onClick = onToggle).padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(if (open) Icons.Rounded.ExpandMore else Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CmColors.actionIcon, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.home_sec_system).uppercase(), style = MonoSmall.copy(color = CmColors.text2, letterSpacing = 1.8.sp))
        Box(Modifier.size(8.dp).clip(CircleShape).background(systemTone(s.level)))
        Box(Modifier.weight(1f).height(1.dp).background(CmColors.line))
        if (!open) Text(systemSummary(s), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1)
    }
}

/**
 * La scheda aperta (Franz 28/10 13:19): carico e memoria con le barre del loro livello, chi consuma, poi i consigli; un
 * consiglio con l'azione ha il suo tasto e la conferma nella scheda, come «Chiudi» di una sessione. Lo stato tagliato a
 * 8 KB arriva spesso senza consumatori: allora niente riga, il resto sì.
 */
@Composable
internal fun SystemCard(s: SystemStatus, ops: List<String>?, onAction: ((CmdOp, String) -> Unit)?) {
    val c = italian()
    var ask by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(setOf<String>()) }
    Column(
        Modifier.fillMaxWidth().testTag("system_advice").clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Meter(stringResource(R.string.system_load), (s.loadPerCore / 3).toFloat(), systemTone(s.loadLevel), stringResource(R.string.system_load_v, SystemRules.num(s.loadPerCore, c)))
        Meter(
            stringResource(R.string.system_mem), 1f - s.memAvailableMb.toFloat() / s.memTotalMb.coerceAtLeast(1), systemTone(s.memLevel),
            stringResource(R.string.system_mem_v, SystemRules.num(s.memAvailableMb / 1024.0, c), SystemRules.num(s.memTotalMb / 1024.0, c)),
        )
        if (s.consumers.isNotEmpty()) {
            val groups = mapOf("claude" to R.string.system_group_claude, "chrome_bridge" to R.string.system_group_chrome_bridge, "relay" to R.string.system_group_relay, "other" to R.string.system_group_other)
            val line = s.consumers.map { g ->
                val name = groups[g.group]?.let { stringResource(it) } ?: g.group
                "$name${if (g.count > 1) " ×${g.count}" else ""} ${SystemRules.cpu(g.cpu)}% ${SystemRules.size(g.rssMb, c)}"
            }.joinToString(" · ")
            Text(line, style = MonoSmall, color = CmColors.text2)
        }
        if (s.advice.isEmpty()) Text(stringResource(R.string.system_calm), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        s.advice.forEach { a ->
            val op = onAction?.let { SystemRules.opOf(a, ops) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(CmColors.line))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(systemTone(a.level)))
                Text(a.text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.weight(1f))
                if (op != null && ask != a.id) Surface(
                    onClick = { if (a.id !in sent) ask = a.id }, shape = CircleShape, color = CmColors.surfaceHigh,
                    contentColor = if (a.id in sent) CmColors.text2 else CmColors.text,
                ) { Text(a.action!!.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
            }
            if (op != null && ask == a.id) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(if (op == CmdOp.SYS_UNIT_STOP) R.string.system_confirm_stop else R.string.system_confirm), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, modifier = Modifier.padding(end = 4.dp))
                TextButton(onClick = { ask = null }) { Text(stringResource(R.string.system_keep), color = CmColors.actionIcon) }
                Button(
                    onClick = { ask = null; sent = sent + a.id; onAction?.invoke(op, a.action!!.arg) },
                    colors = ButtonDefaults.buttonColors(containerColor = CmColors.gone, contentColor = Color.White),
                ) { Text(a.action!!.label) }
            }
        }
    }
}

@Composable
private fun Meter(label: String, fraction: Float, tone: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.width(76.dp))
        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(CmColors.briefTrack)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(tone))
        }
        Text(value, style = MonoSmall, color = CmColors.text2, maxLines = 1)
    }
}
