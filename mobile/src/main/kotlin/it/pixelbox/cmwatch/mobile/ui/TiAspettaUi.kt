package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.TiAspetta
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * La scheda «Ti aspetta» (app-scheda-ti-aspetta-07-11, mockup approvato da Franz il 07/11 alle 21:04): la testata con la sessione e
 * da quanto aspetta; la carta coi tasti (`card`, quella di Decidi, Scegli, Verifica o Rispondi); «Da dove viene» col pezzo di
 * trascrizione, la frase della domanda accesa e «Apri nella trascrizione»; in fondo le altre che aspettano. `wide` (tablet, nella
 * colonna): la carta e «Da dove viene» affiancate e la × al posto della freccia indietro.
 */
@Composable
fun TiAspettaPage(
    page: TiAspetta.Page, now: Long, wide: Boolean, onBack: () -> Unit, onJump: (TiAspetta.Source) -> Unit, onOther: (String) -> Unit,
    card: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(LocalPageBg.current ?: V3.Bg).testTag("ti-aspetta-${page.key}")) {
        Row(Modifier.fillMaxWidth().padding(start = if (wide) 18.dp else 6.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = V3.Tx) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ti_aspetta_title), style = V3.Section.copy(fontSize = 22.sp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    page.session?.let { Text("$it ·", style = V3.Meter) }
                    Text(stringResource(R.string.ti_aspetta_since, TiAspetta.age(now - page.since)), style = V3.Meter.copy(color = V3.Wait))
                }
            }
            if (wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = V3.Tx2) }
        }
        if (wide) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { card() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { page.source?.let { SourceBlock(it, onJump) } }
            } else {
                card()
                page.source?.let { SourceBlock(it, onJump) }
            }
            if (page.others.isNotEmpty()) Others(page.others, now, onOther)
        }
    }
}

private val HM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun SecLabel(text: String, count: Int? = null) {
    Row(Modifier.padding(start = 2.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text.uppercase(), style = V3.Small.copy(fontSize = 12.sp, letterSpacing = 1.2.sp, color = V3.Tx3))
        count?.let { Text(it.toString(), style = V3.Small.copy(fontSize = 12.sp, color = V3.Wait)) }
    }
}

/** «Da dove viene»: la sessione e l'ora della risposta, il pezzo con la riga a sinistra e la frase della domanda accesa, il salto. */
@Composable
private fun SourceBlock(src: TiAspetta.Source, onJump: (TiAspetta.Source) -> Unit) {
    SecLabel(stringResource(R.string.ti_aspetta_source))
    Column(Modifier.fillMaxWidth().clip(SubShape).border(1.dp, V3.S3, SubShape).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            src.at?.let { t -> stringResource(R.string.ti_aspetta_reply_at, src.session, HM.format(Instant.ofEpochSecond(t).atZone(ZoneId.systemDefault()))) } ?: src.session,
            style = V3.Meter,
        )
        val hi = V3.Wait.copy(alpha = 0.18f)
        val text = buildAnnotatedString {
            TiAspetta.marks(src.text, src.ask).forEach { s -> if (s.on) withStyle(SpanStyle(background = hi, color = V3.Tx)) { append(s.text) } else append(s.text) }
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(V3.Tx3))
            Text(text, style = V3.Body.copy(color = V3.Tx2), modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp).testTag("ti-aspetta-pezzo"))
        }
        if (src.entry != null) Box(
            Modifier.clip(CircleShape).background(V3.S3).handCursor().clickable { onJump(src) }.padding(horizontal = 14.dp, vertical = 8.dp).testTag("ti-aspetta-salto"),
        ) { Text(stringResource(R.string.ti_aspetta_jump), style = V3.Small.copy(color = V3.Tx, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)) }
    }
}

/** Le altre domande che aspettano: pallino ambra, titolo, l'etichetta e la sessione in mono, la freccia. */
@Composable
private fun Others(others: List<TiAspetta.Other>, now: Long, onOther: (String) -> Unit) {
    SecLabel(stringResource(R.string.ti_aspetta_others), others.size)
    V3List {
        others.forEachIndexed { i, o ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).handCursor().clickable { onOther(o.key) }.padding(horizontal = 12.dp, vertical = 10.dp).testTag("ti-aspetta-altra-${o.key}"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(V3.Wait))
                Column(Modifier.weight(1f)) {
                    Text(o.title, style = V3.Body.copy(color = V3.Tx))
                    Text(listOfNotNull(kindLabel(o.kind), o.session, TiAspetta.age(now - o.since)).joinToString(" · "), style = V3.Meter)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3)
            }
        }
    }
}

/** L'etichetta all'imperativo della carta (Franz 06/11): Decidi, Scegli, Verifica, Rispondi. */
@Composable
internal fun kindLabel(k: TiAspetta.Kind): String = stringResource(
    when (k) { TiAspetta.Kind.DECIDE -> R.string.ti_aspetta_decide; TiAspetta.Kind.CHOOSE -> R.string.ti_aspetta_choose; TiAspetta.Kind.VERIFY -> R.string.ti_aspetta_verify; TiAspetta.Kind.ANSWER -> R.string.ti_aspetta_answer },
)
