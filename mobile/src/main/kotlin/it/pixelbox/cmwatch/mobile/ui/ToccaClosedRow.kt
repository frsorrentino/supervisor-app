package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.ToccaToday
import it.pixelbox.cmwatch.mobile.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Le voci uscite da sole da Tocca a te oggi (app-pulizia-tocca-03-11, contratto 1.113): una riga grigia in fondo alla categoria,
 * un tocco apre l'elenco, solo da guardare. Niente da decidere: le chiude il registro con le sue regole.
 */
@Composable
fun ToccaClosedRow(today: ToccaToday, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    val off = animationsOff()
    Column(modifier.fillMaxWidth().then(if (off) Modifier else Modifier.animateContentSize()).testTag("tocca-chiuse-oggi")) {
        Row(
            // Icona e testo sulle colonne delle testate di sezione (V3Section): binario V3.Rail, poi 8 dp.
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { open = !open }.padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(V3.Rail), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CleaningServices, null, tint = V3.Tx2, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(8.dp))
            Text(pluralStringResource(R.plurals.tocca_closed_today, today.n, today.n), style = V3.Small, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.padding(start = 6.dp).size(24.dp).rotate(if (open) 180f else 0f))
        }
        if (open) {
            val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            Column(Modifier.fillMaxWidth().padding(start = V3.Rail + 8.dp, end = 12.dp, bottom = 8.dp)) {
                today.items.forEachIndexed { i, x ->
                    if (i > 0) HorizontalDivider(color = V3.Line)
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text("${x.short?.takeIf { it.isNotBlank() } ?: x.task} · ${kindText(x.kind)}", style = V3.Small.copy(color = V3.Tx, fontWeight = FontWeight.Medium))
                        Text(listOfNotNull(ruleText(x.rule), fmt.format(Date(x.at * 1000))).joinToString(" · "), style = V3.Small)
                    }
                }
                val more = today.n - today.items.size
                if (more > 0) Text(stringResource(R.string.closed_more, more), style = V3.Small, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable private fun kindText(kind: String): String = when (kind) {
    "pause" -> stringResource(R.string.tocca_kind_pause)
    "prova" -> stringResource(R.string.tocca_kind_prova)
    "needs_you" -> stringResource(R.string.tocca_kind_needs_you)
    else -> kind
}

@Composable private fun ruleText(rule: Int): String? = when (rule) {
    1 -> stringResource(R.string.tocca_rule_1)
    2 -> stringResource(R.string.tocca_rule_2)
    3 -> stringResource(R.string.tocca_rule_3)
    4 -> stringResource(R.string.tocca_rule_4)
    else -> null
}
