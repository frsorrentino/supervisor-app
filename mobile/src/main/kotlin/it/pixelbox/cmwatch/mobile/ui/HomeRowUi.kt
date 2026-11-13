package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.HomeRow
import it.pixelbox.cmwatch.rules.TiAspetta

/**
 * La Home in riga (app-home-riga-c1-13-11, variante C1 dei mockup, scelta di Franz del 13/11 alle 09:32): in basso, mentre la master
 * occupa la colonna. Solo le sessioni che ti aspettano fuori schermo e le novità, una voce per cosa: il tocco su una voce apre la sua
 * sessione; il resto della riga, la maniglia o la riga tirata su riaprono la Home. Vuota resta la maniglia, alta 28 dp.
 * `tight` (telefono): chi ti aspetta col nome e da quanto, le novità contate in una voce sola.
 */
@Composable
fun HomeRowBar(voices: List<HomeRow.Voice>, now: Long, tight: Boolean, onOpen: (HomeRow.Voice) -> Unit, onHome: () -> Unit) {
    val line = V3.Line
    val open = stringResource(R.string.home_row_open)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(HomeRow.rowHeight(voices).dp).background(V3.Bg)
            .drawBehind { drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .testTag("home-riga").semantics { contentDescription = open }.handCursor().clickable(onClick = onHome)
            // La riga tirata su: oltre 24 dp verso l'alto riapre la Home.
            .pointerInput(Unit) { var up = 0f; detectVerticalDragGestures(onDragStart = { up = 0f }) { _, dy -> up -= dy; if (up > 24.dp.toPx()) { up = Float.NEGATIVE_INFINITY; onHome() } } },
    ) {
        // La maniglia: in cima con le voci, al centro da sola.
        Box(Modifier.align(if (voices.isEmpty()) Alignment.Center else Alignment.TopCenter).padding(top = if (voices.isEmpty()) 0.dp else 5.dp).width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
        if (voices.isEmpty()) return@BoxWithConstraints
        // In una riga stretta (tablet) restano il nome e da quanto.
        val words = !tight && maxWidth >= 900.dp
        val waits = voices.filter { it.kind == HomeRow.Kind.WAIT }
        val news = voices.filter { it.kind == HomeRow.Kind.NEWS }
        Row(
            Modifier.fillMaxSize().padding(start = if (tight) 10.dp else 14.dp, end = if (tight) 10.dp else 14.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (tight) 8.dp else 10.dp),
        ) {
            Icon(Icons.Rounded.Home, null, tint = V3.Tx2, modifier = Modifier.size(22.dp))
            // Le voci su una riga; quelle che non ci stanno si raggiungono scorrendo.
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (tight) 8.dp else 10.dp)) {
                waits.forEach { v -> VoicePill(v, now, words, onOpen) }
                if (tight && news.isNotEmpty()) NewsPill(news.size.toString(), null) { onOpen(news.first()) }
                else news.forEach { v -> NewsPill(v.session, v.text.takeIf { words }) { onOpen(v) } }
            }
            if (!tight) Box(Modifier.size(40.dp).clip(CircleShape).background(V3.S3), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ExpandLess, null, tint = V3.Tx, modifier = Modifier.size(22.dp)) }
        }
    }
}

private val pillName get() = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, lineHeight = 20.sp)
private val pillText get() = TextStyle(fontFamily = V3.Sans, fontSize = 14.sp, lineHeight = 20.sp)

/** Una sessione che ti aspetta fuori schermo: il pallino, il nome, cosa in breve, da quanto. */
@Composable
private fun VoicePill(v: HomeRow.Voice, now: Long, words: Boolean, onOpen: (HomeRow.Voice) -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(V3.S1).border(1.dp, V3.Line, CircleShape).testTag("home-riga-voce").handCursor().clickable { onOpen(v) }.padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(V3.Wait))
        Text(v.session, style = pillName, color = V3.Tx, maxLines = 1)
        if (words) v.text?.let { Text(it, style = pillText, color = V3.Tx2, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.widthIn(max = 320.dp)) }
        v.at?.let { Text(TiAspetta.age((now - it).coerceAtLeast(0)), style = pillText.copy(fontFeatureSettings = "tnum"), color = V3.Wait, maxLines = 1) }
    }
}

/** Una novità: l'esito arrivato da quando la Home è in riga, col segno verde e «nuovo». */
@Composable
private fun NewsPill(name: String, text: String?, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(V3.S1).border(1.dp, V3.Good.copy(alpha = .45f).compositeOver(V3.Line), CircleShape).testTag("home-riga-novita").handCursor().clickable(onClick = onClick)
            .padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.CheckCircleOutline, null, tint = V3.Good, modifier = Modifier.size(18.dp))
        Text(name, style = pillName, color = V3.Tx, maxLines = 1)
        text?.let { Text(it, style = pillText, color = V3.Tx2, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.widthIn(max = 320.dp)) }
        Text(stringResource(R.string.home_row_new).uppercase(), style = TextStyle(fontFamily = V3.Mono, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp), color = V3.Good)
    }
}
