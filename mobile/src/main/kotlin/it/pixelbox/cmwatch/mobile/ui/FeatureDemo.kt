package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Features
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.NightPage
import it.pixelbox.cmwatch.rules.SlotInfo
import it.pixelbox.cmwatch.rules.Suggestions
import java.time.ZoneId

/** Quello che serve al riquadro «Prova»: lo stato finto della demo, senza transport e quindi senza relay. */
class FeatureDemoData(val state: State, val page: TimelinePage, val night: NightReport?, val now: Long)

/**
 * Il riquadro «Prova» di una funzione (Franz 01/11 22:13): il componente vero coi dati della demo. I tasti restano qui dentro: nessuna
 * chiamata al relay. Le pagine intere stanno in un'altezza fissa, così scorrono da sole dentro la pagina della voce.
 */
@Composable
internal fun FeatureDemoBox(key: String, d: FeatureDemoData) {
    val frame = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Cm.line, RoundedCornerShape(16.dp)).background(Cm.bg).testTag("prova-$key")
    val tall = frame.height(460.dp)
    val st = d.state
    val head = Kpi.head(st, d.page, d.now, ZoneId.systemDefault())
    when (key) {
        "tabellone" -> Box(frame.padding(8.dp)) { Kpi7(head, st, d.now, onBox = {}) }
        "ti_aspettano" -> Box(tall) { HomeBoxScreen(HomeBox.WAITING, st, d.page, d.now, onBack = {}, onSession = {}, embedded = true) }
        "esiti" -> Box(tall) { HomeBoxScreen(HomeBox.OUTCOMES, st, d.page, d.now, onBack = {}, onSession = {}, startByType = true, embedded = true) }
        "slot" -> {
            var second by remember { mutableStateOf(false) }
            Column(frame.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kpi7(head, st, d.now, onBox = {}, slot = {
                    if (second) st.plugins?.let { PluginsSlotRow(SlotInfo.plugins(it), ZoneId.systemDefault(), Modifier, false) {} }
                    else st.system?.let { SystemSlotRow(SlotInfo.system(it, st.hostsStatus, st.sessions), Modifier, false) {} }
                })
                TwoWaySwitch(second, stringResource(R.string.home_sec_system), stringResource(R.string.home_sec_plugins)) { second = it }
            }
        }
        "lavori" -> Box(tall) { LavoriScreen(st, null, emptyList(), d.now, onBack = {}, onApprove = { _, _, _ -> }, onAgendaEdit = {}, onStart = {}, onDoneForPerson = {}, onAddNight = {}, onRemoveNight = {}, onNight = {}, onSession = {}, header = LavoriHeader.NONE) }
        "suggestions" -> {
            var done by remember { mutableStateOf(emptySet<String>()) }
            val v = Suggestions.visible(st, done, d.now)
            Box(frame.padding(8.dp)) { SuggestionsCard(v) { s, _, _ -> done = done + s.id } }
        }
        "master" -> {
            val master = st.sessions.firstOrNull { it.name == "master" }
            var open by remember { mutableStateOf(false) }
            Column(frame) {
                if (master != null) MasterDock(master, MasterHome.Hero("Fase 2.2 avviata su payments-api", "", emptyList(), d.now - 600), {}, { open = !open }, expanded = open)
                if (open) Box(Modifier.padding(12.dp)) { ChatBody(FeatureDemoTexts.MASTER_ANSWER) }
            }
        }
        "system" -> Box(frame.padding(8.dp)) { st.system?.let { SystemCard(it, null, null) } }
        "notte_report" -> Box(tall) { d.night?.let { NightScreen(NightPage.of(it, ZoneId.systemDefault()), null, false, onBack = {}, onRefresh = {}, onChat = {}, onAnswer = { _, _ -> }, onApprove = {}, onSend = { _, _ -> }) } }
        "web_app" -> {
            val uri = LocalUriHandler.current
            Box(frame.padding(12.dp)) { androidx.compose.material3.TextButton(onClick = { uri.openUri(FeatureDemoTexts.WEB_DEMO) }) { Text(FeatureDemoTexts.WEB_DEMO.removePrefix("https://")) } }
        }
    }
}

internal object FeatureDemoTexts {
    const val WEB_DEMO = "https://supervisor.francescosorrentino.com/?demo"
    const val MASTER_ANSWER = "**Da te**\n1. Manda l'immagine di copertina al blog.\n2. Fai il login nella sandbox.\n\n**Non provato:** il checkout su un telefono vero.\n\nEsito: due cose aspettano te."
}
