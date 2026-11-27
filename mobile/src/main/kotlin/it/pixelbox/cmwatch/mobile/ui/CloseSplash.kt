package it.pixelbox.cmwatch.mobile.ui

import it.pixelbox.cmwatch.contract.ProofStep
import it.pixelbox.cmwatch.contract.ProofSteps
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.CloseSplash
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SPLASH_HM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Il pannello di chiusura della pagina della sessione (Franz, 08/10 18:29, variante A; dal 20/11 la carta C di
 * app-chiudi-scheda-20-11, in fondo e senza velo): dice che cosa succede. «Si sta chiudendo» con i passi veri; «è chiusa» con l'ora, «Riapri» e «Torna alla
 * home», e dopo 3 s il ritorno da solo, con la barra che si consuma.
 */
@Composable
fun CloseSplashOverlay(
    phase: CloseSplash.Phase, onHome: () -> Unit, onReopen: (() -> Unit)?,
    /** In una colonna del tablet il pannello toglie la colonna invece di tornare alla home. */
    column: Boolean = false,
    /** Rifiutata dal relay (app-chiudi-esito-04-11): «Va bene» toglie il motivo e lascia la pagina com'era. */
    onDismiss: () -> Unit = onHome,
    /** Contratto 1.120: «Chiudi quando finisce» ("after") o «Chiudi comunque» ("force", confermato qui). */
    onArm: ((String) -> Unit)? = null,
) {
    val home by rememberUpdatedState(onHome)
    val hm = { at: Long -> SPLASH_HM.format(Instant.ofEpochSecond(at).atZone(ZoneId.systemDefault())) }
    // app-chiudi-scheda-20-11 (bozza C): niente velo; la carta sta in fondo, al posto del campo di scrittura, sopra la barra dei gesti.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        CloseCardFrame(
            Modifier.navigationBarsPadding().imePadding().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) {
            val closed = phase is CloseSplash.Phase.Closed
            Icon(Icons.Rounded.PowerSettingsNew, null, tint = if (closed) Cm.text2 else if (phase is CloseSplash.Phase.Refused) Cm.waiting else V3.Busy, modifier = Modifier.size(32.dp).padding(bottom = 2.dp))
            when (phase) {
                is CloseSplash.Phase.Closing -> {
                    CloseTitle(stringResource(R.string.splash_closing, phase.name))
                    // Contratto 1.155 (app-chiudi-salva-19-11): «Salva e chiudi» segue closing.mode, save e poi exit.
                    CloseLine(stringResource(if (phase.save) R.string.splash_save_sent_at else R.string.splash_sent_at, hm(phase.sentAt)))
                    // Voce 3 del terzo giro (app-animazioni-terzo-c-26-11): i passi della chiusura sulla barra a segmenti dei passi della
                    // prova, col suo moto e coi nomi dentro; la riga sotto ripeteva il passo in corso e non c'è più (master, 27/11: senza
                    // nomi i segmenti sembravano caselle per un codice).
                    val end = stringResource(R.string.splash_step_closed)
                    val bar = if (phase.save) {
                        val exiting = phase.step == "exit"
                        listOf(
                            ProofStep("save", stringResource(R.string.splash_step_save), if (exiting) "ok" else "run"),
                            ProofStep("exit", stringResource(R.string.splash_step_exit), if (exiting) "run" else "todo"),
                            ProofStep("done", end, "todo"),
                        )
                    } else listOf(
                        ProofStep("sent", stringResource(R.string.splash_step_sent), if (phase.delivered) "ok" else "run"),
                        ProofStep("done", end, if (phase.delivered) "run" else "todo"),
                    )
                    KStepBar(ProofSteps(items = bar), why = false)
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp), color = V3.Busy, trackColor = Cm.surfaceHigh)
                    if (phase.slow) {
                        Text(stringResource(R.string.splash_slow), style = MaterialTheme.typography.labelMedium, color = Cm.waiting)
                        CloseKey(stringResource(if (column) R.string.splash_drop_column else R.string.splash_home), CloseKeyKind.TONAL, onHome, Modifier.fillMaxWidth().padding(top = 8.dp))
                    } else Text(stringResource(if (phase.save) R.string.splash_save_usually else if (phase.force) R.string.splash_force_usually else R.string.splash_usually), style = MaterialTheme.typography.labelMedium, color = Cm.text2)
                }
                is CloseSplash.Phase.Refused -> CloseRefusedBody(phase.name, phase.why, onArm.takeIf { phase.armable }, onDismiss)
                is CloseSplash.Phase.Closed -> {
                    CloseTitle(stringResource(R.string.splash_closed, phase.name))
                    CloseLine(stringResource(if (phase.byMe) R.string.splash_closed_line else R.string.splash_closed_line_other, hm(phase.at)))
                    // Contratto 1.155: col passaggio di consegne (e il suo file, la fine visibile), salvataggio scaduto, o niente da salvare.
                    when {
                        phase.handoff -> {
                            Text(stringResource(R.string.splash_handoff), style = MaterialTheme.typography.bodyMedium, color = V3.Good)
                            phase.file?.let { p -> Text(p, style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Cm.text2, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Clip) }
                        }
                        phase.reason != null -> Text(stringResource(R.string.splash_expired, phase.reason!!), style = MaterialTheme.typography.bodyMedium, color = Cm.waiting)
                        phase.save -> Text(stringResource(R.string.splash_no_handoff), style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                    }
                    // La barra si consuma nei 3 s prima del ritorno alla home.
                    val left = remember(phase) { androidx.compose.animation.core.Animatable(1f) }
                    LaunchedEffect(phase) {
                        left.animateTo(0f, androidx.compose.animation.core.tween(CloseSplash.CLOSED_SHOW_MS.toInt(), easing = androidx.compose.animation.core.LinearEasing))
                        home()
                    }
                    LinearProgressIndicator({ left.value }, Modifier.fillMaxWidth().padding(top = 10.dp), color = Cm.text2, trackColor = Cm.surfaceHigh, drawStopIndicator = {})
                    Text(stringResource(if (column) R.string.splash_drop_soon else R.string.splash_back_soon), style = MaterialTheme.typography.labelMedium, color = Cm.text2)
                    val homeKey = @Composable { CloseKey(stringResource(if (column) R.string.splash_drop_column else R.string.splash_home), CloseKeyKind.MAIN, onHome) }
                    if (onReopen != null) CloseKeyPair({ CloseKey(stringResource(R.string.splash_reopen), CloseKeyKind.TONAL, onReopen) }, homeKey, Modifier.padding(top = 10.dp))
                    else CloseKey(stringResource(if (column) R.string.splash_drop_column else R.string.splash_home), CloseKeyKind.MAIN, onHome, Modifier.fillMaxWidth().padding(top = 10.dp))
                }
            }
        }
    }
}
