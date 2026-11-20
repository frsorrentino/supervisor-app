package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
 * La domanda prima di chiudere una sessione dal menu o con /exit scritto nel campo (Franz, 06/10 10:57, mockup approvato
 * 12:25): il nome nel titolo e nel tasto, lo stato, cosa succede. Dalla card della home la conferma sta nella card.
 * Contratto 1.155 (app-chiudi-salva-19-11, Franz 19/11 15:16): con [onSave] «Salva e chiudi» è il tasto pieno, e
 * [onConfirm] diventa «Chiudi senza salvare» accanto a «Lascia aperta».
 */
@Composable
internal fun CloseDialog(s: Session, onDismiss: () -> Unit, onConfirm: () -> Unit, onSave: (() -> Unit)? = null) = if (onSave != null) AlertDialog(
    onDismissRequest = onDismiss, containerColor = Cm.surface,
    title = { Text(stringResource(R.string.close_title, s.name)) },
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(closeStateLine(s), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
            Text(stringResource(R.string.close_what_save), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
        }
    },
    confirmButton = {
        Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary)) {
            Text(stringResource(R.string.close_save))
        }
    },
    dismissButton = {
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_keep), color = Cm.actionIcon) }
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.close_no_save), color = Cm.gone) }
        }
    },
) else AlertDialog(
    onDismissRequest = onDismiss, containerColor = Cm.surface,
    title = { Text(stringResource(R.string.close_title, s.name)) },
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(closeStateLine(s), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
            Text(stringResource(R.string.close_what), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
        }
    },
    confirmButton = {
        Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = V3.S3, contentColor = Cm.gone)) {
            Text(stringResource(R.string.cleanup_close, s.name))
        }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_keep), color = Cm.actionIcon) } },
)
