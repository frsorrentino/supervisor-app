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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * La carta di una domanda nata in chat (contratto 1.130: Prossimi, AskUserQuestion, needs_you senza voce nel registro): l'etichetta,
 * la domanda e le scelte numerate, la consigliata in ambra; senza scelte un tasto solo per chiuderla. `onOption(null)` è quel tasto.
 * Dopo il tocco i tasti si spengono e resta acceso quello scelto. «Rispondi con parole tue» apre la sessione.
 */
@Composable
fun WaitCardBody(item: it.pixelbox.cmwatch.contract.WaitItem, onOption: (it.pixelbox.cmwatch.contract.WaitOption?) -> Unit, onOwnWords: (() -> Unit)?) {
    var sent by androidx.compose.runtime.saveable.rememberSaveable(item.id) { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(V3.S1).border(1.dp, V3.Wait.copy(alpha = 0.45f), shape).padding(14.dp).testTag("ti-aspetta-carta"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(item.labelText.uppercase(), style = V3.Small.copy(fontSize = 12.sp, letterSpacing = 1.sp, color = V3.Tx3))
        Text(item.text, style = V3.Body.copy(color = V3.Tx))
        val opts = item.options.orEmpty()
        if (opts.isNotEmpty()) opts.forEach { o ->
            val rec = o.recommended
            val oshape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            val on = sent == o.n
            Row(
                Modifier.fillMaxWidth().clip(oshape)
                    .background(if (rec) V3.Wait.copy(alpha = 0.14f) else V3.S2)
                    .border(1.dp, if (on) V3.Good else if (rec) V3.Wait.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Transparent, oshape)
                    .then(if (sent == null) Modifier.handCursor().clickable { sent = o.n; onOption(o) } else Modifier)
                    .graphicsLayer { alpha = if (sent != null && !on) 0.45f else 1f }
                    .padding(horizontal = 12.dp, vertical = 10.dp).testTag("ti-aspetta-scelta-${o.n}"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(if (rec) V3.Wait else androidx.compose.ui.graphics.Color.Transparent)
                        .border(1.5.dp, if (rec) V3.Wait else V3.Tx2, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(o.n.toString(), style = V3.Meter.copy(color = if (rec) V3.Bg else V3.Tx2)) }
                Column(Modifier.weight(1f)) {
                    Text(o.label, style = V3.Name.copy(fontSize = 15.sp, color = if (rec) V3.Wait else V3.Tx))
                    val sub = listOfNotNull(if (rec) stringResource(R.string.ti_aspetta_recommended) else null, o.description).joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, style = V3.Small)
                }
            }
        } else if (item.answer != null) V3Pill(stringResource(R.string.ti_aspetta_done), filled = sent == null) { if (sent == null) { sent = 0; onOption(null) } }
        else Text(stringResource(R.string.ti_aspetta_by_master), style = V3.Small)
        onOwnWords?.let { f ->
            Box(Modifier.clip(CircleShape).background(V3.S3).handCursor().clickable(onClick = f).padding(horizontal = 16.dp, vertical = 9.dp)) {
                Text(stringResource(R.string.ti_aspetta_own_words), style = V3.Small.copy(color = V3.Tx, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
            }
        }
    }
}

/** La scheda mentre si legge, o quando la domanda non aspetta più: la freccia indietro e una riga. */
@Composable
fun TiAspettaEmpty(text: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(LocalPageBg.current ?: V3.Bg).padding(6.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = V3.Tx) }
        Text(text, style = V3.Body.copy(color = V3.Tx2), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

/** Sul tablet la scheda sta al posto delle colonne, in un pannello come quello di una sessione: il grigio e il bordo delle schede. */
@Composable
fun TiAspettaColumn(content: @Composable () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxSize().padding(vertical = 10.dp).clip(shape).background(V3.S1).border(1.dp, V3.Line, shape)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalPageBg provides V3.S1) { content() }
    }
}
