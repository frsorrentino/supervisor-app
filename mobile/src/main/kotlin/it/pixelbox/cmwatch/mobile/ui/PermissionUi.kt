package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Pause
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.PermissionModes
import it.pixelbox.cmwatch.rules.PermissionModes.Glyph
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * La modalità dei permessi coi segni del terminale (mockup del 14/10, ok di Franz il 15/10 alle 07:39): ⏸ piano, ⏵⏵
 * modifiche automatiche e automatica, ⏵⏵ ambra senza conferme, ? sconosciuta; niente per «chiede sempre».
 */
@Composable
fun PermissionGlyph(glyph: Glyph, size: Dp = 16.dp, tint: Color = CmColors.text) {
    val icon = when (glyph) {
        Glyph.NONE -> return
        Glyph.PLAN -> Icons.Rounded.Pause
        Glyph.FORWARD, Glyph.BYPASS -> Icons.Rounded.FastForward
        Glyph.UNKNOWN -> Icons.AutoMirrored.Rounded.HelpOutline
    }
    Icon(icon, null, tint = if (glyph == Glyph.BYPASS) CmColors.briefWarn else tint, modifier = Modifier.size(size))
}

@Composable
fun permissionName(mode: String?): String = when (mode) {
    null, "", PermissionModes.DEFAULT -> stringResource(R.string.perm_default)
    PermissionModes.PLAN -> stringResource(R.string.perm_plan)
    PermissionModes.ACCEPT_EDITS -> stringResource(R.string.perm_accept)
    PermissionModes.AUTO -> stringResource(R.string.perm_auto)
    PermissionModes.BYPASS -> stringResource(R.string.perm_bypass)
    else -> mode
}

@Composable
fun permissionSub(mode: String?): String = when (mode) {
    null, "", PermissionModes.DEFAULT -> stringResource(R.string.perm_default_sub)
    PermissionModes.PLAN -> stringResource(R.string.perm_plan_sub)
    PermissionModes.ACCEPT_EDITS -> stringResource(R.string.perm_accept_sub)
    PermissionModes.AUTO -> stringResource(R.string.perm_auto_sub)
    PermissionModes.BYPASS -> stringResource(R.string.perm_bypass_sub)
    else -> stringResource(R.string.perm_unknown_sub)
}

/** Il segno con nome e spiegazione, la scheda che si apre tenendo premuto il segno (mockup, schermata 1). */
@Composable
fun PermissionCard(mode: String?) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceHigh).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PermissionGlyph(PermissionModes.glyph(mode))
            Text(permissionName(mode), style = MaterialTheme.typography.titleSmall, color = CmColors.text, fontWeight = FontWeight.SemiBold)
        }
        Text(permissionSub(mode), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
    }
}

/**
 * Contratto 1.69: il gruppo Permessi nel pannello modello ed effort. Più autonomia chiede un secondo tocco (bordo e riga
 * ambra); toglierne parte subito. «Senza conferme» c'è solo se la sessione ci può arrivare.
 */
@Composable
fun PermissionGroup(s: Session, onPick: (String) -> Unit) {
    var armed by remember(s.permissionMode) { mutableStateOf<String?>(null) }
    val current = s.permissionMode ?: PermissionModes.DEFAULT
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PermissionModes.choices(s).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { m ->
                    val on = m == current
                    val isArmed = armed == m
                    Surface(
                        onClick = {
                            when {
                                on -> Unit
                                PermissionModes.needsConfirm(current, m) && !isArmed -> armed = m
                                else -> { armed = null; onPick(m) }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (on) CmColors.primary else CmColors.surfaceHigh,
                        contentColor = if (on) CmColors.onPrimary else CmColors.text,
                        border = if (isArmed) androidx.compose.foundation.BorderStroke(2.dp, CmColors.briefWarn) else null,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp).handCursor(),
                    ) {
                        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                            PermissionGlyph(PermissionModes.glyph(m), 14.dp, tint = if (on) CmColors.onPrimary else CmColors.text)
                            Text(permissionName(m), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
                repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
        armed?.let { Text(stringResource(R.string.perm_confirm, permissionName(it)), style = MaterialTheme.typography.bodyMedium, color = CmColors.briefWarn) }
        Text(stringResource(R.string.perm_note), style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
    }
}
