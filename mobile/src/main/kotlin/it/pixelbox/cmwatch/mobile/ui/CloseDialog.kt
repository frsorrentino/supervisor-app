package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** «sta lavorando», «ha finito il compito alle 10:48», «stessa conversazione di rino»: la sessione è aperta. */
@Composable
internal fun closeStateLine(s: Session): String {
    val c = MasterService.closeState(s)
    return when (c.kind) {
        MasterService.CloseState.Kind.WORKING -> stringResource(R.string.close_working, s.name)
        MasterService.CloseState.Kind.DUPLICATE -> stringResource(R.string.close_duplicate, s.name, c.of.orEmpty())
        MasterService.CloseState.Kind.FINISHED -> c.at?.let { stringResource(R.string.close_finished_at, s.name, clockHm(it)) } ?: stringResource(R.string.close_finished, s.name)
        MasterService.CloseState.Kind.STILL -> stringResource(R.string.close_still, s.name)
    }
}

internal fun clockHm(at: Long): String = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(at))

/**
 * La domanda prima di chiudere una sessione dal menu o con /exit scritto nel campo (Franz, 06/10 10:57; nome nel titolo, lo stato,
 * cosa succede). app-chiudi-scheda-20-11 (Franz 20/11 19:08, bozza C): non più una finestra al centro ma la carta della chiusura in
 * fondo alla pagina, dove sta il campo di scrittura, senza velo; un tocco fuori la lascia aperta. Contratto 1.155: con [onSave]
 * «Salva e chiudi» è l'azione principale e [onConfirm] diventa «Chiudi senza salvare».
 */
@Composable
internal fun CloseDialog(s: Session, onDismiss: () -> Unit, onConfirm: () -> Unit, onSave: (() -> Unit)? = null) = androidx.compose.ui.window.Dialog(
    onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
) {
    // Niente velo: la pagina resta leggibile sopra la carta.
    (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setDimAmount(0f)
    Box(
        Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        CloseCardFrame(
            Modifier.navigationBarsPadding().imePadding().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) {
            CloseAskBody(s, save = onSave != null, onKeep = onDismiss, onClose = { save -> if (save) onSave?.invoke() else onConfirm() })
        }
    }
}
