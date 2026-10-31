package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.HomeOrder

/** Le voci della home sulla schermata: icona e nome, come le testate delle sezioni. */
@Composable
private fun sectionOf(key: String): Pair<ImageVector, String> = when (key) {
    "sessions" -> Icons.Outlined.PlayCircle to stringResource(R.string.home_sec_sessions)
    "suggestions" -> Icons.Outlined.AutoAwesome to stringResource(R.string.sug_title)
    "lavori" -> Icons.Outlined.Checklist to stringResource(R.string.lavori_title)
    "esiti" -> Icons.Outlined.SportsScore to stringResource(R.string.box_outcomes_title)
    else -> Icons.Outlined.DataUsage to stringResource(R.string.reorder_tools)
}

internal val HOME_DRAG_ROW = 56.dp

/** Durante il trascinamento nella home: i soli titoli delle sezioni, quello preso segue il dito. */
@Composable
internal fun HomeDragLayer(order: List<String>, held: String?, dy: Float, modifier: Modifier = Modifier) {
    Column(modifier.background(V3.Bg).padding(start = 12.dp, end = 12.dp, top = 14.dp).testTag("home-sezione-trascina")) {
        V3List {
            order.forEachIndexed { i, key ->
                androidx.compose.runtime.key(key) {
                    val (icon, name) = sectionOf(key)
                    val on = key == held
                    Box(Modifier.height(HOME_DRAG_ROW).zIndex(if (on) 1f else 0f).graphicsLayer { translationY = if (on) dy else 0f }.background(if (on) V3.S3 else V3.S1)) {
                        V3Row(i == 0, null, { Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }) { Text(name, style = V3.Name) }
                    }
                }
            }
        }
    }
}

/**
 * «Riordina la home» (Franz 30/10 17:22, compito app-riordino-sezioni-30-10), nello stile «Studio e Diretta»: le sezioni chiuse
 * su righe singole, con le frecce su e giù: il trascinamento sta nella home, con la pressione lunga sul titolo (Franz 31/10 01:01),
 * che non si vede e con TalkBack non si può. Tabellone e «Ti aspettano» restano fissi in cima. «Salva» è l'unico tasto pieno.
 */
@Composable
fun HomeOrderScreen(saved: List<String>?, onBack: () -> Unit, onSave: (List<String>) -> Unit) {
    var order by rememberSaveable(saved) { mutableStateOf(HomeOrder.normalize(saved)) }
    Column(Modifier.fillMaxSize().background(V3.Bg).systemBarsPadding().testTag("riordino-sezioni")) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.settings_back), tint = V3.Tx) }
            Text(stringResource(R.string.reorder_title), style = V3.Section.copy(fontSize = 22.sp, lineHeight = 26.sp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reorder_sub), style = V3.Small, modifier = Modifier.padding(horizontal = 4.dp))
            // Le due voci fisse, senza comandi.
            V3List {
                V3Row(true, null, { Icon(Icons.Outlined.Dashboard, null, tint = V3.Tx3, modifier = Modifier.size(22.dp)) }) { Text(stringResource(R.string.reorder_board), style = V3.Name.copy(color = V3.Tx2)) }
                V3Row(false, null, { Icon(Icons.Outlined.PanTool, null, tint = V3.Tx3, modifier = Modifier.size(22.dp)) }) { Text(stringResource(R.string.home_waiting), style = V3.Name.copy(color = V3.Tx2)) }
            }
            V3List {
                order.forEachIndexed { i, key ->
                    androidx.compose.runtime.key(key) {
                        val (icon, name) = sectionOf(key)
                        OrderRow(icon, name, i == 0, i, order.size, onMove = { to -> order = HomeOrder.move(order, order.indexOf(key), to) })
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = { onSave(order) }, shape = CircleShape, color = V3.Act, contentColor = V3.OnAct, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.reorder_save), style = V3.Name.copy(color = V3.OnAct)) }
            }
            Text(stringResource(R.string.reorder_reset), style = V3.Small.copy(color = V3.Tx), modifier = Modifier.handCursor().clickable { order = HomeOrder.DEFAULT }.padding(12.dp))
        }
    }
}

@Composable
private fun OrderRow(icon: ImageVector, name: String, first: Boolean, i: Int, size: Int, onMove: (Int) -> Unit) {
    val move by rememberUpdatedState(onMove)
    Box {
        V3Row(first, null, {
        }, {
            IconButton(onClick = { move(i - 1) }, enabled = i > 0, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.reorder_up, name), tint = if (i > 0) V3.Tx else V3.S3) }
            IconButton(onClick = { move(i + 1) }, enabled = i < size - 1, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.reorder_down, name), tint = if (i < size - 1) V3.Tx else V3.S3) }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(20.dp))
                // A capo, mai tagliato: con i caratteri grandi «Suggerimenti della master» non sta su una riga.
                Text(name, style = V3.Name)
            }
        }
    }
}
