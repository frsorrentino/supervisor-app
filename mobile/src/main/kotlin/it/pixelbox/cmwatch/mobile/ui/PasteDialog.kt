package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R

/** «Incolla il codice»: la riga di `relay pair --text`, quando il QR non si legge; la finestra dice quale (Franz, 04/10 15:46). */
@Composable
fun PasteDialog(onPair: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Cm.surfaceHigh,
        title = { Text(stringResource(R.string.paste_title)) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.paste_hint), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
                OutlinedTextField(text, { text = it }, minLines = 4, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("{\"v\":1,…", fontFamily = V3.Mono, color = Cm.stale) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = V3.Mono))
            }
        },
        confirmButton = { Button({ onPair(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.pair_button)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
