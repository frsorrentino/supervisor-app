package it.pixelbox.cmwatch.mobile.ui

import kotlinx.coroutines.launch

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.BuildConfig
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Feedback

/**
 * Feedback dal menù (app-feedback-menu-04-11, scelta A; come web/src/lib/Feedback.svelte): il tipo, il testo, i dati tecnici
 * aggiunti da soli e la foto della pagina, spenta di base e con l'anteprima; poi la bozza anonima con «Invia dal mio GitHub»,
 * «Invia anonimo» e «Non ora». [shot] è la pagina fotografata prima di aprire il foglio (null: non c'è). [send] (contratto 1.121,
 * `feedback_app`) manda bozza e scelta; null: il relay non ha l'op, i due tasti d'invio sono spenti.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    screen: String, shot: Bitmap?,
    send: (suspend (Feedback.Draft, Feedback.Tech, ByteArray?, String) -> Feedback.Sent)?,
    onDismiss: () -> Unit,
) {
    val cfg = LocalConfiguration.current
    val tech = Feedback.Tech(
        BuildConfig.VERSION_NAME, BuildConfig.GIT_COMMIT,
        "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE} · ${cfg.screenWidthDp}×${cfg.screenHeightDp} dp",
        stringResource(R.string.feedback_screen, screen), stringResource(R.string.feedback_lang, cfg.locales[0].toLanguageTag()),
    )
    val labels = Feedback.Labels(stringResource(R.string.feedback_idea), stringResource(R.string.feedback_kind_bug), stringResource(R.string.feedback_tech), stringResource(R.string.feedback_photo_attached))
    var kind by rememberSaveable { mutableStateOf(Feedback.Kind.IDEA) }
    var text by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf(false) }
    var draft by remember { mutableStateOf<Feedback.Draft?>(null) }
    val image = remember(shot) { shot?.asImageBitmap() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    var sending by remember { mutableStateOf(false) }
    var sentAs by remember { mutableStateOf<String?>(null) }
    var github by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    val failText = stringResource(R.string.feedback_send_fail, "%s")
    val anonSent = stringResource(R.string.feedback_anon_sent); val githubHint = stringResource(R.string.feedback_github_hint); val kept = stringResource(R.string.feedback_kept)
    fun go(d: Feedback.Draft, choice: String) {
        val s = send ?: return
        sending = true
        scope.launch {
            // La foto in JPEG, solo se accesa.
            val jpg = if (d.photo) shot?.let { b -> java.io.ByteArrayOutputStream().also { o -> b.compress(Bitmap.CompressFormat.JPEG, 80, o) }.toByteArray() } else null
            val r = s(d, tech, jpg, choice)
            sending = false
            r.error?.let { e -> result = failText.replace("%s", e); return@launch }
            text = ""
            when (choice) {
                "github" -> { sentAs = choice; github = r.url; result = githubHint }
                "anonymous" -> { sentAs = choice; result = anonSent; kotlinx.coroutines.delay(it.pixelbox.cmwatch.rules.Leave.CHECK_MS + 300); onDismiss() }
                else -> { result = kept; kotlinx.coroutines.delay(900); onDismiss() }
            }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = V3.S1) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.feedback_menu), style = V3.Section.copy(color = V3.Tx, fontSize = V3.Section.fontSize * 1.2f))
            val d = draft
            if (d == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((k, label) in listOf(Feedback.Kind.IDEA to R.string.feedback_idea, Feedback.Kind.BUG to R.string.feedback_bug)) {
                        val on = kind == k
                        Text(
                            stringResource(label), style = V3.Name.copy(color = V3.Tx),
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp).background(if (on) V3.S3 else V3.S2, RoundedCornerShape(22.dp))
                                .then(if (on) Modifier.border(2.dp, V3.Tx2, RoundedCornerShape(22.dp)) else Modifier)
                                .clickable(role = Role.RadioButton) { kind = k }.padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                OutlinedTextField(
                    text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 140.dp), textStyle = V3.Body.copy(color = V3.Tx),
                    placeholder = { Text(stringResource(if (kind == Feedback.Kind.BUG) R.string.feedback_placeholder_bug else R.string.feedback_placeholder_idea), style = V3.Body.copy(color = V3.Tx3)) },
                )
                Column(Modifier.fillMaxWidth().background(V3.S2, V3.Shape).padding(14.dp)) {
                    Text(stringResource(R.string.feedback_tech), style = V3.Name.copy(color = V3.Tx))
                    Text(stringResource(R.string.feedback_tech_sub), style = V3.Small.copy(color = V3.Tx2))
                    Spacer(Modifier.heightIn(min = 6.dp))
                    for (l in listOf("app ${tech.version} (${tech.commit})", tech.device, tech.screen, tech.lang)) Text(l, style = V3.Meter.copy(color = V3.Tx2))
                }
                // Dove la pagina non si è potuta fotografare la riga non c'è.
                if (image != null) Column(Modifier.fillMaxWidth().background(V3.S2, V3.Shape)) {
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Switch) { photo = !photo }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.feedback_photo), style = V3.Name.copy(color = V3.Tx))
                            Text(stringResource(R.string.feedback_photo_sub), style = V3.Small.copy(color = V3.Tx2))
                        }
                        Switch(photo, { photo = it })
                    }
                    if (photo) Image(image, stringResource(R.string.feedback_photo_attached), Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp).heightIn(max = 360.dp), contentScale = ContentScale.Fit)
                }
                Button(
                    { draft = Feedback.draft(kind, text, tech, photo && image != null, labels) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = V3.Act, contentColor = V3.OnAct),
                ) { Text(stringResource(R.string.feedback_prepare), style = V3.Name) }
            } else {
                Column(Modifier.fillMaxWidth().background(V3.S2, V3.Shape).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.feedback_draft), style = V3.Name.copy(color = V3.Tx))
                    Text(stringResource(R.string.feedback_draft_sub), style = V3.Small.copy(color = V3.Tx2))
                    Text(d.title, style = V3.Name.copy(color = V3.Tx))
                    Text(d.body, style = V3.Meter.copy(color = V3.Tx2))
                    if (d.photo && image != null) Image(image, stringResource(R.string.feedback_photo_attached), Modifier.fillMaxWidth().heightIn(max = 360.dp), contentScale = ContentScale.Fit)
                }
                val note = result ?: if (send == null) stringResource(R.string.feedback_not_yet) else null
                note?.let { Text(it, style = V3.Small.copy(color = V3.Tx2)) }
                // Inviato (D3): il tasto usato prende la spunta disegnata.
                Button(
                    { go(d, "github") }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = send != null && !sending && sentAs == null,
                    colors = ButtonDefaults.buttonColors(containerColor = V3.Act, contentColor = V3.OnAct, disabledContainerColor = if (sentAs == "github") CheckFill else V3.S3),
                ) { CheckLabel(sentAs == "github") { Text(stringResource(R.string.feedback_send_github), style = V3.Name) } }
                github?.let { u ->
                    Button({ uri.openUri(u); onDismiss() }, Modifier.fillMaxWidth().heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = V3.Act, contentColor = V3.OnAct)) {
                        Text(stringResource(R.string.feedback_github_open), style = V3.Name)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton({ go(d, "anonymous") }, enabled = send != null && !sending && sentAs == null) { CheckLabel(sentAs == "anonymous") { Text(stringResource(R.string.feedback_send_anon)) } }
                    TextButton({ draft = null }, enabled = sentAs == null) { Text(stringResource(R.string.feedback_edit), color = V3.Tx) }
                    TextButton({ if (send != null && sentAs == null) go(d, "later") else onDismiss() }, enabled = !sending) { Text(stringResource(R.string.feedback_later), color = V3.Tx) }
                }
            }
        }
    }
}
