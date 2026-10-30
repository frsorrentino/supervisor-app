package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.layout.layout
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.AppLanguage
import it.pixelbox.cmwatch.rules.SettingsDevices
import it.pixelbox.cmwatch.rules.SpeechRate
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Impostazioni (mockup A, scelto da Franz il 03/10 alle 21:59): come le altre pagine, testata con ← e sezioni per
 * argomento, ognuna con la sua etichetta e il filo. Collegamento: lo schema dei dispositivi da toccare (`DeviceScheme`) e
 * «Rifai l'accoppiamento»; Lettura ad alta voce: voce e velocità in pillole; App: lingua, notifiche, Demo; Informazioni:
 * privacy e versione.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    host: String?, phoneName: String, watchName: String?, watchPending: Boolean, demo: Boolean, version: String,
    onRepair: () -> Unit, onDemo: (Boolean) -> Unit, onNotifications: () -> Unit, onPrivacy: () -> Unit,
    language: AppLanguage.Choice = AppLanguage.Choice.SYSTEM, onLanguage: (AppLanguage.Choice) -> Unit = {},
    /** La voce che legge (Franz, 02/10 00:01): le voci italiane del motore, quella scelta (null = predefinita), la prova. */
    voices: List<String> = emptyList(), voice: String? = null, onVoice: (String?) -> Unit = {}, onTryVoice: () -> Unit = {},
    /** La velocità della voce (Franz, 02/10 15:49: «un po' troppo rapida»), da 0,5× a 2× (slider del 07/10). */
    rate: Float = SpeechRate.NORMAL, onRate: (Float) -> Unit = {},
    /** Ogni quanti minuti la live fa il recap delle sessioni (Franz, 08/10 20:15), 0 = spento; il tocco passa al valore dopo. */
    liveRecap: Int = it.pixelbox.cmwatch.rules.LiveDesk.RECAP_DEFAULT_MIN, onLiveRecap: (Int) -> Unit = {},
    /** Lo stato dei dispositivi per lo schema; senza, quello che si sa dall'abbinamento. */
    devices: SettingsDevices.Model = SettingsDevices.build(host, null, null, 0, phoneName, version, true, watchName, watchPending, null),
    onBack: () -> Unit = {},
    /** Il dispositivo con la scheda aperta all'inizio (i provini); di solito il PC. */
    initialDevice: DeviceNode = DeviceNode.PC,
    /** Contratto 1.31: un dispositivo in più dal telefono, senza PC; null con un relay che non lo sa fare. */
    onAddDevice: (() -> Unit)? = null,
    /** Il tablet (Franz, 04/10 14:40): i dettagli della sessione a destra, spenti di default; null fuori dal tablet. */
    tabletDetails: Boolean? = null, onTabletDetails: (Boolean) -> Unit = {},
    /** Contratto 1.32: i dispositivi veri per lo schema B; null con un relay precedente (resta lo schema di prima). */
    linked: List<SettingsDevices.Linked>? = null,
    /** P6 (21/10): la riga «Aggiornamenti» con l'esito dell'ultimo controllo; il tocco controlla ora. Null = niente riga. */
    updateStatus: String? = null, onCheckUpdate: (() -> Unit)? = null,
    /** Contratto 1.89: Revoca accanto a ogni dispositivo dell'elenco (op `unpair`); null = relay senza l'op, niente tasto. */
    onRevoke: ((String) -> Unit)? = null,
    /** «Riordina la home» (app-riordino-sezioni-30-10): l'ordine salvato e il salvataggio; null = relay senza `home_order`, niente voce. */
    homeOrder: List<String>? = null, onHomeOrder: ((List<String>) -> Unit)? = null,
) {
    var reorder by rememberSaveable { mutableStateOf(false) }
    if (reorder && onHomeOrder != null) {
        androidx.activity.compose.BackHandler { reorder = false }
        HomeOrderScreen(homeOrder, onBack = { reorder = false }) { o -> onHomeOrder(o); reorder = false }
        return
    }
    var choosing by remember { mutableStateOf(false) }
    var choosingVoice by remember { mutableStateOf(false) }
    var device by rememberSaveable { mutableStateOf(initialDevice) }
    V3Theme { Column(Modifier.fillMaxSize().background(V3.Bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.settings_back), tint = CmColors.text) }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            V3Section(stringResource(R.string.sec_link), Icons.Outlined.Link)
                        if (linked != null && host != null) {
                LinkedList(linked, nowSec(), onRevoke)
            }
            else DeviceScheme(devices, device, onSelect = { device = it }) {
                // Non accoppiati (Demo): accoppiare è l'azione principale, sotto lo schema tratteggiato.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.connect_pc), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                    Text(stringResource(R.string.connect_pc_sub), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                    Button(
                        onClick = onRepair, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.pair_button)) }
                }
            }
            // Una voce della lista, non un bottone pieno: là sembrava «Salva» (Franz, 30/09 20:26).
            if (host != null) SectionCard {
                SettingsRow(Icons.Outlined.QrCodeScanner, stringResource(R.string.paired_repair), stringResource(R.string.repair_sub), onRepair)
                onAddDevice?.let { SettingsRow(Icons.Outlined.AddLink, stringResource(R.string.add_device), stringResource(R.string.add_device_sub), it) }
            }
            tabletDetails?.let { on ->
                SectionCard {
                    SettingsRow(Icons.Outlined.Info, stringResource(R.string.tablet_details), stringResource(R.string.tablet_details_sub), onClick = { onTabletDetails(!on) }) {
                        androidx.compose.material3.Switch(checked = on, onCheckedChange = onTabletDetails)
                    }
                }
            }

            onHomeOrder?.let {
                SectionCard { SettingsRow(Icons.Outlined.Reorder, stringResource(R.string.reorder_title), stringResource(R.string.reorder_row_sub), onClick = { reorder = true }) }
            }

            V3Section(stringResource(R.string.sec_reading), Icons.Outlined.GraphicEq)
            SectionCard {
                SettingsRow(Icons.Outlined.GraphicEq, stringResource(R.string.voice), voice ?: stringResource(R.string.voice_default), onClick = { choosingVoice = true })
                SettingsRow(Icons.Outlined.Speed, stringResource(R.string.speech_rate), stringResource(R.string.rate_hint), onClick = null) {
                    Text(rateLabel(rate), style = MaterialTheme.typography.titleSmall, color = CmColors.text)
                }
                // Lo stesso slider della barra di lettura (Franz, 07/10, approvato alle 22:06): da 0,5× a 2×, vale al rilascio.
                var draft by remember(rate) { mutableFloatStateOf(rate) }
                RateSlider(
                    draft, onChange = { draft = it }, onDone = { onRate(draft) },
                    modifier = Modifier.fillMaxWidth().padding(start = 60.dp, end = 16.dp, bottom = 14.dp),
                )
                SettingsRow(
                    Icons.Outlined.Headphones, stringResource(R.string.live_recap_setting), stringResource(R.string.live_recap_setting_sub),
                    onClick = { onLiveRecap(it.pixelbox.cmwatch.rules.LiveDesk.nextRecap(liveRecap)) },
                ) {
                    Text(if (liveRecap == 0) stringResource(R.string.live_recap_off) else stringResource(R.string.live_recap_every, liveRecap), style = MaterialTheme.typography.titleSmall, color = CmColors.text)
                }
            }

            V3Section(stringResource(R.string.sec_app), Icons.Outlined.Tune)
            SectionCard {
                SettingsRow(Icons.Outlined.Language, stringResource(R.string.language), languageName(language), onClick = { choosing = true })
                SettingsRow(Icons.Outlined.Notifications, stringResource(R.string.notifications), stringResource(R.string.notifications_sub), onNotifications)
                SettingsRow(Icons.Outlined.Science, stringResource(R.string.demo_mode), stringResource(R.string.demo_mode_sub), onClick = { onDemo(!demo) }) {
                    Switch(checked = demo, onCheckedChange = onDemo, colors = cmSwitchColors())
                }
            }

            V3Section(stringResource(R.string.sec_info), Icons.Outlined.Info)
            SectionCard {
                SettingsRow(Icons.Outlined.PrivacyTip, stringResource(R.string.privacy), stringResource(R.string.privacy_sub), onPrivacy)
                SettingsRow(Icons.Outlined.Info, stringResource(R.string.version_title), stringResource(R.string.version_value, version), onClick = null)
                updateStatus?.let { SettingsRow(Icons.Outlined.SystemUpdate, stringResource(R.string.updates_title), it, onCheckUpdate) }
            }
        }
    } }
    if (choosingVoice) {
        AlertDialog(
            onDismissRequest = { choosingVoice = false },
            confirmButton = { TextButton(onClick = onTryVoice) { Text(stringResource(R.string.voice_try), color = CmColors.actionIcon) } },
            dismissButton = { TextButton(onClick = { choosingVoice = false }) { Text(stringResource(R.string.close), color = CmColors.actionIcon) } },
            title = { Text(stringResource(R.string.voice)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    (listOf<String?>(null) + voices).forEach { v ->
                        Row(
                            Modifier.fillMaxWidth().handCursor().clickable { onVoice(v) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = v == voice, onClick = null)
                            Text(v ?: stringResource(R.string.voice_default), color = CmColors.text)
                        }
                    }
                }
            },
        )
    }
    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false }, confirmButton = {},
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    AppLanguage.Choice.entries.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().handCursor().clickable { choosing = false; onLanguage(c) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = c == language, onClick = null)
                            Text(languageName(c), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            containerColor = CmColors.surface,
        )
    }
}

/** Una lista della v3; il filo sopra la prima riga resta fuori. */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).layout { m, c ->
            val p = m.measure(c); val cut = 1.dp.roundToPx()
            layout(p.width, p.height - cut) { p.place(0, -cut) }
        },
        content = content,
    )
}

/** Una riga come nel menu ≡: icona grigia a contorno nel binario, titolo e valore attuale; la freccia se porta altrove, o un controllo a destra. */
@Composable
private fun SettingsRow(
    icon: ImageVector, title: String, sub: String, onClick: (() -> Unit)?, trailing: (@Composable () -> Unit)? = null,
) {
    V3Row(false, onClick, { Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }, {
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3)
        }
    }) {
        Text(title, style = V3.Name)
        Text(sub, style = V3.Small)
    }
}

@Composable
private fun languageName(c: AppLanguage.Choice): String = stringResource(when (c) {
    AppLanguage.Choice.SYSTEM -> R.string.language_system
    AppLanguage.Choice.ITALIAN -> R.string.language_it
    AppLanguage.Choice.ENGLISH -> R.string.language_en
})

/** Adesso in secondi, per le letture dello schema B; fermo nei provini come il resto delle schermate. */
@Composable
private fun nowSec(): Long = if (LocalStill.current) STILL_NOW else System.currentTimeMillis() / 1000

/** L'istante fisso dei provini dello schema B: dieci secondi dopo l'ultima lettura del telefono nelle fixture. */
internal const val STILL_NOW = 1789210800L

