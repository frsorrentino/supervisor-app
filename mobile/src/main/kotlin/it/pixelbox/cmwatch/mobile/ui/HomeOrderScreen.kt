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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MilitaryTech
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.HomeCats
import it.pixelbox.cmwatch.rules.HomeOrder
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex

/** Le voci della home sulla schermata: icona e nome, come le testate delle sezioni. */
@Composable
private fun sectionOf(key: String): Pair<ImageVector, String> = when (key) {
    // La categoria presa nella home (app-categorie-trascina-02-11).
    "cat:you", "cat:work", "cat:out", "cat:tools" -> catIcon(key.removePrefix("cat:")) to catName(key.removePrefix("cat:"))
    "seguiti" -> Icons.Outlined.Update to stringResource(R.string.home_sec_follow)
    "prove" -> Icons.Outlined.Science to stringResource(R.string.home_sec_verify)
    "decisioni" -> Icons.Outlined.Timer to stringResource(R.string.home_sec_decide)
    "sessions" -> Icons.Outlined.PlayCircle to stringResource(R.string.home_sec_sessions)
    "suggestions" -> Icons.Outlined.AutoAwesome to stringResource(R.string.sug_title)
    "notte" -> Icons.Rounded.Bedtime to stringResource(R.string.home_sec_night)
    "lavori" -> Icons.Outlined.Checklist to stringResource(R.string.lavori_progress)
    "esiti" -> Icons.Outlined.SportsScore to stringResource(R.string.box_outcomes_title)
    // Le tre sezioni nuove (app-da-guardare-02-11, app-rendiconti-2-02-11): in Riordina hanno già il loro posto.
    "da_guardare" -> Icons.Outlined.Visibility to stringResource(R.string.home_sec_look)
    "rendiconti" -> Icons.Outlined.Description to stringResource(R.string.home_sec_reports)
    "pagella" -> Icons.Outlined.MilitaryTech to stringResource(R.string.home_sec_scorecard)
    "system" -> Icons.Outlined.Memory to stringResource(R.string.home_sec_system)
    "plugins" -> Icons.Outlined.Extension to stringResource(R.string.home_sec_plugins)
    "closed" -> Icons.Outlined.Inventory2 to stringResource(R.string.cat_closed)
    else -> Icons.Outlined.DataUsage to stringResource(R.string.home_sec_usage)
}

/** La voce presa nella home: la testata che segue il dito, sopra la lista che resta com'è (Franz 31/10 14:27). */
@Composable
internal fun HomeDragGhost(key: String, modifier: Modifier = Modifier) {
    val (icon, name) = sectionOf(key)
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp).graphicsLayer { shadowElevation = 16f; shape = V3.Shape; clip = true }.testTag("home-sezione-trascina")) {
        V3Row(true, null, { Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }, fill = V3.S3) { Text(name, style = V3.Name) }
    }
}

/**
 * «Riordina la home» per categorie (Franz 02/11 16:44, app-home-categorie-02-11; mockup home-categorie-telefono.png, riquadro 4):
 * si trascinano le categorie e, dentro ognuna, le sue sezioni, con la pressione lunga sulla maniglia; l'interruttore decide se in
 * home la categoria è in vista o chiusa. Una sezione resta sempre nella sua categoria. Con TalkBack le frecce sono azioni della
 * riga. «Salva» è l'unico tasto pieno; ordine e categorie valgono anche per le colonne della PWA.
 */
@Composable
fun HomeOrderScreen(saved: List<String>?, onBack: () -> Unit, onSave: (List<String>) -> Unit) {
    var cats by remember(saved) { mutableStateOf(HomeCats.decode(saved)) }
    // Lo spazio in cima (app-home-in-cima-02-11): la prima card, con «Togli dalla cima»; ogni sezione ha «Metti in cima».
    var top by remember(saved) { mutableStateOf(HomeCats.top(saved)) }
    fun putTop(key: String) { val (t, c) = HomeCats.toTop(cats, top, key); top = t; cats = c }
    fun dropTop(key: String) { val (t, c) = HomeCats.fromTop(cats, top, key); top = t; cats = c }
    val drag = remember { OrderDrag() }
    Column(Modifier.fillMaxSize().background(V3.Bg).systemBarsPadding().testTag("riordino-sezioni")) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.settings_back), tint = V3.Tx) }
            Text(stringResource(R.string.reorder_title), style = V3.Section.copy(fontSize = 22.sp, lineHeight = 26.sp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val ids = cats.map { it.id }
            Column(Modifier.clip(V3.Shape).background(V3.S1).testTag("riordino-cima")) {
                Text(stringResource(R.string.reorder_top), style = V3.Section, modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 10.dp))
                if (top.isEmpty()) Text(stringResource(R.string.reorder_top_empty), style = V3.Small, modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 14.dp))
                top.forEachIndexed { ti, key ->
                    androidx.compose.runtime.key("top-$key") {
                        val (icon, label) = sectionOf(key)
                        Column(drag.item("top-$key").background(V3.S1)) {
                            androidx.compose.material3.HorizontalDivider(Modifier.padding(horizontal = 12.dp), thickness = 1.dp, color = V3.Line)
                            Row(Modifier.fillMaxWidth().moveActions(label, ti, top.size) { to -> top = HomeOrder.move(top, ti, to) }.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Handle(drag.handle("top-$key", top.map { k -> "top-$k" }) { from, to -> top = HomeOrder.move(top, from, to) })
                                Icon(icon, null, tint = V3.Tx2, modifier = Modifier.padding(start = 4.dp, end = 12.dp).size(22.dp))
                                Text(label, style = V3.Name, modifier = Modifier.weight(1f))
                                IconButton(onClick = { dropTop(key) }, modifier = Modifier.testTag("riordino-togli-$key")) { Icon(Icons.Outlined.VerticalAlignBottom, stringResource(R.string.reorder_top_out, label), tint = V3.Tx2) }
                            }
                        }
                    }
                }
            }
            cats.forEachIndexed { ci, c ->
                androidx.compose.runtime.key(c.id) {
                    Column(drag.item(c.id).clip(V3.Shape).background(V3.S1).testTag("riordino-cat-${c.id}")) {
                        val name = catName(c.id)
                        val inView = stringResource(R.string.reorder_in_view)
                        val shut = stringResource(R.string.reorder_cat_closed)
                        Row(Modifier.fillMaxWidth().moveActions(name, ci, cats.size) { to -> cats = HomeCats.moveCat(cats, ci, to) }.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Handle(drag.handle(c.id, ids) { from, to -> cats = HomeCats.moveCat(cats, from, to) })
                            Text(name, style = V3.Section, modifier = Modifier.weight(1f).padding(start = 4.dp))
                            Text(if (c.open) inView else shut, style = V3.Small, modifier = Modifier.padding(end = 10.dp))
                            androidx.compose.material3.Switch(checked = c.open, onCheckedChange = { cats = HomeCats.toggle(cats, c.id) }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = V3.Busy, checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedBorderColor = V3.Busy, uncheckedTrackColor = V3.S3, uncheckedThumbColor = V3.Tx, uncheckedBorderColor = V3.S3), modifier = Modifier.testTag("riordino-interruttore-${c.id}"))
                        }
                        c.sections.forEachIndexed { si, key ->
                            androidx.compose.runtime.key(key) {
                                val (icon, label) = sectionOf(key)
                                Column(drag.item(key).background(V3.S1)) {
                                    androidx.compose.material3.HorizontalDivider(Modifier.padding(horizontal = 12.dp), thickness = 1.dp, color = V3.Line)
                                    Row(Modifier.fillMaxWidth().moveActions(label, si, c.sections.size) { to -> cats = HomeCats.moveSection(cats, c.id, si, to) }.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Handle(drag.handle(key, c.sections) { from, to -> cats = HomeCats.moveSection(cats, c.id, from, to) })
                                        Icon(icon, null, tint = V3.Tx2, modifier = Modifier.padding(start = 4.dp, end = 12.dp).size(22.dp))
                                        // A capo, mai tagliato: con i caratteri grandi i nomi lunghi non stanno su una riga.
                                        Text(label, style = V3.Name, modifier = Modifier.weight(1f))
                                        // Sul tabellone stanno quattro voci (app-tabellone-quattro-02-11): pieno, la freccia non c'è.
                                        if (top.size < HomeCats.BOARD_MAX) IconButton(onClick = { putTop(key) }, modifier = Modifier.testTag("riordino-in-cima-$key")) { Icon(Icons.Outlined.VerticalAlignTop, stringResource(R.string.reorder_top_in, label), tint = V3.Tx2) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(stringResource(R.string.reorder_foot), style = V3.Small, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = { onSave(HomeCats.encode(cats, null, top) ?: emptyList()) }, shape = CircleShape, color = V3.Act, contentColor = V3.OnAct, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.reorder_save), style = V3.Name.copy(color = V3.OnAct)) }
            }
            Text(stringResource(R.string.reorder_reset), style = V3.Small.copy(color = V3.Tx), modifier = Modifier.handCursor().clickable { cats = HomeCats.DEFAULT; top = emptyList() }.padding(12.dp))
        }
    }
}

@Composable
private fun Handle(modifier: Modifier) = Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
    Icon(Icons.Outlined.DragIndicator, null, tint = V3.Tx3, modifier = Modifier.size(22.dp))
}

/** Le frecce per TalkBack: «Sposta su» e «Sposta giù» come azioni della riga, al posto dei tasti che non si vedono più. */
@Composable
private fun Modifier.moveActions(name: String, i: Int, size: Int, onMove: (Int) -> Unit): Modifier {
    val up = stringResource(R.string.reorder_up, name)
    val down = stringResource(R.string.reorder_down, name)
    val move by rememberUpdatedState(onMove)
    return semantics(mergeDescendants = true) {
        customActions = listOfNotNull(
            if (i > 0) CustomAccessibilityAction(up) { move(i - 1); true } else null,
            if (i < size - 1) CustomAccessibilityAction(down) { move(i + 1); true } else null,
        )
    }
}

/**
 * Il trascinamento sulla maniglia: la voce presa segue il dito e scavalca la vicina quando il dito ne passa la metà, come nella
 * home (HomeOrder.dragTarget); dopo lo scambio lo spostamento si accorcia dell'altezza della vicina, così la voce non salta.
 */
private class OrderDrag {
    var held by mutableStateOf<String?>(null)
    var dy by mutableFloatStateOf(0f)
    val heights = mutableMapOf<String, Int>()

    fun item(key: String): Modifier = Modifier
        .onSizeChanged { heights[key] = it.height }
        .zIndex(if (held == key) 1f else 0f)
        .graphicsLayer { translationY = if (held == key) dy else 0f; shadowElevation = if (held == key) 16f else 0f; shape = V3.Shape }

    @Composable
    fun handle(key: String, keys: List<String>, onMove: (Int, Int) -> Unit): Modifier {
        val now by rememberUpdatedState(keys)
        val move by rememberUpdatedState(onMove)
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        return Modifier.pointerInput(key) {
            detectDragGesturesAfterLongPress(
                onDragStart = { held = key; dy = 0f; haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) },
                onDragEnd = { held = null; dy = 0f },
                onDragCancel = { held = null; dy = 0f },
            ) { change, d ->
                change.consume()
                dy += d.y
                val i = now.indexOf(key)
                val next = now.getOrNull(i + 1)?.let { heights[it] }
                val prev = now.getOrNull(i - 1)?.let { heights[it] }
                if (next != null && dy > next / 2f) { move(i, i + 1); dy -= next }
                else if (prev != null && dy < -prev / 2f) { move(i, i - 1); dy += prev }
            }
        }
    }
}
