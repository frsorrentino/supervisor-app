package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.PerTe
import it.pixelbox.cmwatch.rules.ProvaLinks
import kotlinx.coroutines.launch

/**
 * Le prove citate nelle chat come schede compatte (app-prova-scheda-in-chat-11-11, Franz 11/11 19:49: «Riguardo i link all'interno delle
 * sessioni al tasto prova, vorrei che fossero proprio delle schede come queste, ma compatte»). La stessa famiglia di Verifica: casella,
 * titolo, cosa fare in una riga, i tasti Non va e Ok; con l'esito già dato, l'esito al posto dei tasti. Il tocco sulla scheda apre la
 * prova come il link. I dati dall'op `prove` col numero (contratto 1.134), una lettura per scheda con l'etag e poi in cache, riletta
 * solo dopo un tocco: niente letture periodiche. Fuori dall'app (i provini) nessun [LocalProvaCards]: restano i link.
 */
class ProvaCardsHost(
    /** La prova letta, null finché non arriva. */
    val read: (Long) -> ProvaLinks.Read?,
    /** Chiede la prova se non c'è ancora: una volta sola, poi la cache. */
    val ask: (Long) -> Unit,
    val open: (Long) -> Unit,
    val ok: suspend (Long) -> Boolean,
    val ko: suspend (ProvaOne, String) -> Boolean,
    /** app-carte-stato-vivo-20-11: le prove aperte secondo `attese` (CardLive.openProve); null = non si sa. */
    val openProve: () -> Set<Long>? = { null },
)

val LocalProvaCards = staticCompositionLocalOf<ProvaCardsHost?> { null }

/** Una scheda per ogni prova citata nel testo, nell'ordine in cui compare. */
@Composable
fun ProvaCards(text: String) {
    val host = LocalProvaCards.current ?: return
    val ns = remember(text) { ProvaLinks.find(text).map { it.n }.distinct() }
    ns.forEach { n ->
        LaunchedEffect(n) { host.ask(n) }
        ProvaCard(n, host.read(n), onOpen = { host.open(n) }, onOk = { host.ok(n) }, onKo = { p, why -> host.ko(p, why) }, open = host.openProve())
    }
}

/** La scheda compatta di una prova. Un numero che non esiste, o un relay che non sa leggerla, non dà scheda: resta il link. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun ProvaCard(n: Long, read: ProvaLinks.Read?, onOpen: () -> Unit, onOk: suspend () -> Boolean, onKo: suspend (ProvaOne, String) -> Boolean, open: Set<Long>? = null) {
    if (read is ProvaLinks.Read.Refused || read is ProvaLinks.Read.Unsupported) return
    val p = (read as? ProvaLinks.Read.Found)?.prova
    val scope = rememberCoroutineScope()
    var sent by remember(n) { mutableStateOf(false) }
    // Dal formato unico (app-formato-unico-12-11, variante A di Franz del 12/11 alle 07:29) la scheda è una voce: icona, «Prova #N» col
    // titolo, cosa fare su una riga e il tasto principale, Ok; «Non va» e il resto stanno nella prova aperta, a un tocco.
    // app-carte-stato-vivo-20-11: lo stato dalla stessa fonte di «Per te» (CardLive): l'esito letto decide; fuori dalle aperte ma ancora
    // da fare, nessun tasto e «non è più da fare». Un Ok appena partito da qui vale come esito finché la prova non si rilegge.
    val v = if (p != null && p.state == "todo" && sent) it.pixelbox.cmwatch.rules.CardLive.View(it.pixelbox.cmwatch.rules.CardLive.Kind.OK) else it.pixelbox.cmwatch.rules.CardLive.prova(p, open)
    val title = stringResource(R.string.prova_title, n.toString()).let { t -> p?.let { "$t · ${it.title ?: it.short ?: it.task}" } ?: t }
    val sub = provaDoneLine(v) ?: when (v.kind) {
        it.pixelbox.cmwatch.rules.CardLive.Kind.LOADING -> stringResource(R.string.prova_loading)
        else -> listOfNotNull(stringResource(R.string.per_te_prova), p?.fai?.takeIf { it.isNotBlank() }).joinToString(" · ")
    }
    VoceRow(
        PerTe.Group.PROVA, title, sub, null, if (v.actionable) stringResource(R.string.prove_ok) else null, onOpen,
        onMain = { sent = true; scope.launch { if (!onOk()) sent = false } },
        tone = when (v.kind) { it.pixelbox.cmwatch.rules.CardLive.Kind.OK -> V3.Good; it.pixelbox.cmwatch.rules.CardLive.Kind.KO, it.pixelbox.cmwatch.rules.CardLive.Kind.RED -> V3.Red; else -> null }, card = true, tag = "prova-scheda-$n",
    )
}

/**
 * La riga di una prova risolta, uguale nella web (`provaDoneLine` in cardLive.ts): «Confermata: va · 20:31», «Non va · 20:31: <motivo>»,
 * «Confermata, controllo rosso · 20:31», «Non è più da fare». null per una prova aperta o in arrivo.
 */
@Composable
internal fun provaDoneLine(v: it.pixelbox.cmwatch.rules.CardLive.View): String? {
    val hm = v.at?.let { at -> it.pixelbox.cmwatch.rules.CardLive.whenText(at, System.currentTimeMillis() / 1000, java.time.ZoneId.systemDefault()) }
    val head = when (v.kind) {
        it.pixelbox.cmwatch.rules.CardLive.Kind.OK -> stringResource(R.string.prova_done_ok)
        it.pixelbox.cmwatch.rules.CardLive.Kind.KO -> stringResource(R.string.prova_done_ko)
        it.pixelbox.cmwatch.rules.CardLive.Kind.RED -> stringResource(R.string.prova_state_red)
        it.pixelbox.cmwatch.rules.CardLive.Kind.NOT_OPEN -> return stringResource(R.string.prova_not_open)
        else -> return null
    }
    return listOfNotNull(listOfNotNull(head, hm).joinToString(" · "), v.text).joinToString(": ")
}
