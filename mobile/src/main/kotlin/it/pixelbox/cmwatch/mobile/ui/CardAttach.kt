package it.pixelbox.cmwatch.mobile.ui

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.AllegatiCarta

/**
 * app-allegati-carta-27-11 (Franz 27/11 20:47, su una carta del Banco prova: «questo campo di testo dovrebbe avere la possibilità di
 * caricare allegati come nelle sessioni»): gli allegati del campo di risposta di una carta (Ti aspetta, Banco prova), col + del campo
 * delle sessioni, lo stesso tetto e la stessa anteprima. [files]: il relay accetta file di ogni formato (`share.any`), oltre alle
 * immagini. [send] carica gli allegati e manda il `report` alla sessione del compito (AllegatiCarta.target): null se sono arrivati,
 * altrimenti il motivo, e allora la risposta non parte e il campo tiene testo e allegati.
 */
class CardAttach(val files: Boolean, val send: suspend (List<Uri>) -> String?)

/** I nuovi allegati in coda a quelli che ci sono, senza doppioni, fino al tetto del campo delle sessioni. */
internal fun cardAttachAdd(have: List<Uri>, picked: List<Uri>): List<Uri> = (have + picked).distinct().take(AllegatiCarta.MAX)

/** Il + del campo delle sessioni, con le stesse voci: foto dalla galleria, foto scattata ora e, con `share.any`, un file. */
@Composable
internal fun CardAttachPlus(attach: CardAttach, onPicked: (List<Uri>) -> Unit) {
    Box(Modifier.testTag("carta-allega")) { AttachButton(files = attach.files, attach = true, onPicked = onPicked) }
}

/** Le anteprime sopra il campo, come nel campo delle sessioni: l'immagine ridotta (o l'icona e il nome di un file) con la × per toglierla. */
@Composable
internal fun CardAttachThumbs(files: List<Uri>, enabled: Boolean, onRemove: (Uri) -> Unit) {
    if (files.isEmpty()) return
    Row(Modifier.horizontalScroll(rememberScrollState()).testTag("carta-allegati"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        files.forEach { uri ->
            Box {
                UriThumb(uri, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)))
                IconButton(onClick = { onRemove(uri) }, enabled = enabled, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.remove_image), tint = Cm.text2, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Sotto il campo, le parole della chat: «carico l'allegato» mentre partono, «non consegnato» col motivo se non sono arrivati. */
@Composable
internal fun CardAttachState(busy: Boolean, failed: String?) {
    if (busy) Row(Modifier.testTag("carta-allegati-carico"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Rounded.CloudUpload, null, tint = Cm.text2, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.chat_uploading), style = MaterialTheme.typography.labelMedium, color = Cm.text2)
    } else if (failed != null) Row(Modifier.testTag("carta-allegati-errore"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = Cm.gone, modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Text(listOf(stringResource(R.string.chat_failed), failed).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = Cm.goneDim)
    }
}
