package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.ContiPage
import it.pixelbox.cmwatch.contract.Pagella
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ContiRules
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * app-rendiconti-2-02-11 (contratto 1.103): il mese coi clienti; il tocco su un cliente legge il suo dettaglio (lavori, commit, release
 * misurati) e sotto c'è l'unico tasto pieno, «Chiedi la bozza», che passa la richiesta alla master (`conti_azione`). Rendiconti.svelte
 * nella web.
 */
@Composable
fun RendicontiBody(
    page: ContiPage?, failed: Boolean,
    onLoad: (mese: String?, project: String?) -> Unit,
    onAsk: suspend (mese: String, project: String) -> Pair<Boolean, String>?,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val day = DateTimeFormatter.ofPattern("d/M")
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf<String?>(null) }
    var asking by remember { mutableStateOf(false) }
    val asked = stringResource(R.string.conti_asked)
    val askFailed = stringResource(R.string.conti_ask_failed)
    Column(Modifier.fillMaxWidth().testTag("rendiconti").padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (page == null) {
            Text(stringResource(if (failed) R.string.conti_failed else R.string.conti_loading), style = V3.Small.copy(fontSize = 14.sp), modifier = Modifier.padding(horizontal = 14.dp))
            return@Column
        }
        if (page.mesi.size > 1) FlowRow(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            page.mesi.forEach { m ->
                val on = m == page.mese
                Text(
                    ContiRules.monthName(m, locale), style = V3.Small.copy(fontSize = 14.sp, color = if (on) V3.Tx else V3.Tx2),
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) V3.S3 else V3.S2)
                        .then(if (on) Modifier.border(1.dp, V3.Tx2, RoundedCornerShape(999.dp)) else Modifier)
                        .handCursor().clickable { note = null; onLoad(m, null) }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        if (page.clienti.isEmpty()) Text(stringResource(R.string.conti_none, ContiRules.monthName(page.mese, locale)), style = V3.Small.copy(fontSize = 14.sp), modifier = Modifier.padding(horizontal = 14.dp))
        page.clienti.forEach { c ->
            val d = page.dettaglio?.takeIf { x -> x.project == c.project }
            Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).handCursor().clickable { note = null; onLoad(page.mese, if (d != null) null else c.project) }
                    .padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = 6.dp).testTag("rendiconti-${c.name}"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(c.name, style = V3.Name.copy(fontSize = 15.sp), maxLines = 1)
                val n = listOfNotNull(pluralStringResource(R.plurals.conti_tasks, c.n, c.n), c.draftAt?.let { at -> stringResource(R.string.conti_draft_on, day.format(Instant.ofEpochSecond(at).atZone(zone))) }).joinToString(" · ")
                Text(n, style = V3.Small.copy(fontSize = 14.sp), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Icon(if (d != null) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx3)
            }
            if (d != null) Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // app-play-lettura-ovunque-27-11: il misurato, i titoli dei compiti e la nota, ognuno col ▶ se va oltre una riga.
                LongText(
                    listOf(pluralStringResource(R.plurals.conti_tasks, d.measured.tasks, d.measured.tasks), stringResource(R.string.conti_commits, d.measured.commits), stringResource(R.string.conti_releases_n, d.measured.releases)).joinToString(" · "),
                    style = V3.Body.copy(fontSize = 15.sp),
                )
                d.tasks.forEach { x ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LongText(x.title, Modifier.weight(1f), style = V3.Small.copy(fontSize = 14.sp))
                        Text(day.format(Instant.ofEpochSecond(x.at).atZone(zone)), style = V3.Small.copy(fontSize = 13.sp, color = V3.Tx3))
                    }
                }
                if (d.releases.isNotEmpty()) Text(stringResource(R.string.conti_releases, d.releases.joinToString(", ") { r -> r.tag }), style = V3.Small.copy(fontSize = 14.sp))
                if (!d.git) Text(stringResource(R.string.conti_no_git), style = V3.Small.copy(fontSize = 14.sp))
                Text(
                    stringResource(if (c.draftAt != null) R.string.conti_ask_again else R.string.conti_ask),
                    style = V3.Name.copy(fontSize = 14.5.sp, color = V3.Bg),
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (asking) V3.Tx3 else V3.Tx).handCursor()
                        .clickable(enabled = !asking) {
                            asking = true
                            scope.launch { val r = onAsk(page.mese, c.project); asking = false; note = if (r == null) askFailed else if (r.first) asked else r.second }
                        }.padding(horizontal = 16.dp, vertical = 9.dp).testTag("rendiconti-bozza"),
                )
                note?.let { LongText(it, style = V3.Small.copy(fontSize = 14.sp)) }
            }
        }
    }
}

/** app-rendiconti-2-02-11 (contratto 1.103): la pagella per modello e tipo di lavoro, solo le righe da `min` casi (10) in su. Pagella.svelte nella web. */
@Composable
fun PagellaBody(pagella: Pagella, zone: ZoneId = ZoneId.systemDefault()) {
    val rows = ContiRules.pagellaRows(pagella)
    val kinds = mapOf("fix" to R.string.pagella_kind_fix, "feature" to R.string.pagella_kind_feature, "refactor" to R.string.pagella_kind_refactor, "docs" to R.string.pagella_kind_docs, "test" to R.string.pagella_kind_test)
    Column(Modifier.fillMaxWidth().testTag("pagella").padding(top = 4.dp, bottom = 8.dp)) {
        Text(stringResource(R.string.pagella_since, DateTimeFormatter.ofPattern("d/M").format(Instant.ofEpochSecond(pagella.since).atZone(zone)), pagella.min), style = V3.Small.copy(fontSize = 14.sp), modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp))
        rows.forEach { r ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
            val pct = ContiRules.ratePct(r)
            PagellaLine(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 8.dp).testTag("pagella-${r.model}-${r.kind}")) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(ContiRules.modelName(r.model), style = V3.Name.copy(fontSize = 15.sp), maxLines = 1)
                    Text(kinds[r.kind]?.let { stringResource(it) } ?: r.kind, style = V3.Small.copy(fontSize = 14.sp))
                }
                Text(stringResource(R.string.pagella_cases, r.n) + (pct?.let { p -> " · " + stringResource(R.string.pagella_defects, p) } ?: ""), style = V3.Meter.copy(fontSize = 14.sp, color = if ((pct ?: 0) >= 20) V3.Wait else V3.Tx), textAlign = TextAlign.End)
            }
        }
        // app-play-lettura-ovunque-27-11: ogni consiglio col ▶ accanto se va oltre una riga.
        // app-pagella-effort-28-11: il consiglio è un oggetto (contratto 1.103), detto come frase; una stringa vecchia resta com'è.
        pagella.advice.forEach { a ->
            val kind = (kinds[a.kind]?.let { stringResource(it) } ?: a.kind).replaceFirstChar { it.uppercase() }
            val rb = ContiRules.pct(a.rateBetter)
            val rw = ContiRules.pct(a.rateWorse)
            val text = a.text ?: if (rb != null && rw != null) stringResource(R.string.pagella_advice, kind, ContiRules.modelName(a.better), rb, ContiRules.modelName(a.worse), rw)
                else stringResource(R.string.pagella_advice_plain, kind, ContiRules.modelName(a.better), ContiRules.modelName(a.worse))
            LongText(text, Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp), style = V3.Small.copy(fontSize = 14.sp, color = V3.Wait))
        }
    }
}

/**
 * app-pagella-sovrapposta-19-11: una riga della Pagella. Il nome della prova a sinistra e i casi a destra sulla stessa riga quando ci
 * stanno; altrimenti i casi vanno sotto, allineati a destra. Mai uno sopra l'altro (prima, a 360 dp, «correzioni» finiva sotto «14 casi»).
 */
@Composable
internal fun PagellaLine(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val gap = 10.dp
    val rowGap = 2.dp
    Layout(content, modifier) { ms, c ->
        val free = c.copy(minWidth = 0, minHeight = 0)
        val lead = ms[0].measure(free)
        val oneLine = pagellaOneLine(lead.width, ms[1].maxIntrinsicWidth(Constraints.Infinity), gap.roundToPx(), c.maxWidth)
        val cases = ms[1].measure(if (oneLine) free.copy(maxWidth = (c.maxWidth - lead.width - gap.roundToPx()).coerceAtLeast(0)) else free)
        val w = c.maxWidth
        val h = if (oneLine) maxOf(lead.height, cases.height) else lead.height + rowGap.roundToPx() + cases.height
        layout(w, h.coerceAtLeast(c.minHeight)) {
            val top = ((h.coerceAtLeast(c.minHeight) - h) / 2)
            if (oneLine) {
                lead.placeRelative(0, top + (h - lead.height) / 2)
                cases.placeRelative(w - cases.width, top + (h - cases.height) / 2)
            } else {
                lead.placeRelative(0, top)
                cases.placeRelative(w - cases.width, top + lead.height + rowGap.roundToPx())
            }
        }
    }
}

/** Nome e casi stanno sulla stessa riga? Solo se ci stanno interi, col loro spazio in mezzo. */
internal fun pagellaOneLine(lead: Int, cases: Int, gap: Int, max: Int): Boolean = lead + gap + cases <= max
