package it.pixelbox.cmwatch.mobile.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * La sveglia FCM: un GET dello stato; le notifiche partono dal diff degli stati in PhoneApp. Il GET si attende qui
 * (onMessageReceived gira su un thread suo, con circa 10 s): tornando subito il processo poteva morire prima (revisione 29/09).
 */
class PhoneMessagingService : FirebaseMessagingService() {
    /** Token nuovo: l'iscrizione al topic si rifà, come sull'orologio. */
    override fun onNewToken(token: String) { (application as PhoneApp).subscribeTopic() }

    override fun onMessageReceived(message: RemoteMessage) {
        val app = application as PhoneApp
        val ok = runBlocking { withTimeoutOrNull(8_000) { app.repo.refresh() } } == true
        // Un GET non riuscito si riprova dopo, fuori dal tempo concesso a onMessageReceived (piano prestazioni, Task 11).
        if (!ok) PhoneWakeWorker.enqueue(this)
        // Contratto 1.18: diario e resoconto della notte arrivano con il push; si avvisa in silenzio, il testo è nel Diario.
        when (message.data["kind"]) {
            "recap" -> app.notifier.diary(getString(R.string.notif_diary))
            "night_report" -> app.notifier.diary(getString(R.string.notif_night))
            // Contratti 1.57 e 1.58: la push porta solo la chiave, l'evento si legge da /events.
            // needs_you e turn_blocking (1.68) li avvisa il diff degli stati; il promemoria arriva solo come evento.
            "task_check", "quota_limit", "turn_failed", "week_unused", "week_pace", "attention_reminder", "elicitation" -> message.data["key"]?.let { key ->
                runBlocking { withTimeoutOrNull(8_000) { app.repo.event(key) } }?.let { e ->
                    // Contratto 1.62: con Franz al PC la push dice data.quiet = "1"; l'evento lo dice anche lui.
                    val ev = if (message.data["quiet"] == "1") e.copy(quiet = true) else e
                    val mode = runBlocking { app.alertMode(ev.ref ?: ev.session) }
                    when (ev.kind) {
                        it.pixelbox.cmwatch.contract.EventKind.TASK_CHECK -> app.notifier.taskCheck(ev)
                        it.pixelbox.cmwatch.contract.EventKind.TURN_FAILED -> app.notifier.turnFailed(ev, mode)
                        it.pixelbox.cmwatch.contract.EventKind.WEEK_UNUSED, it.pixelbox.cmwatch.contract.EventKind.WEEK_PACE -> app.notifier.week(ev)
                        // Contratto 1.68: il promemoria suona solo se la voce aspetta ancora.
                        it.pixelbox.cmwatch.contract.EventKind.ATTENTION_REMINDER ->
                            if (it.pixelbox.cmwatch.rules.StrongAlert.open(app.lastState(), ev)) app.notifier.reminder(ev, mode)
                        // Contratto 1.76: impegni fermi da ore, un avviso forte col riepilogo del registro.
                        it.pixelbox.cmwatch.contract.EventKind.TASK_STALLED -> app.notifier.stalled(ev, mode)
                        // Contratto 1.83: un server MCP chiede qualcosa; la risposta si dà dalla scheda in cima alla home.
                        it.pixelbox.cmwatch.contract.EventKind.ELICITATION -> app.notifier.elicitation(ev, mode)
                        else -> app.notifier.quotaLimit(ev, mode)
                    }
                }
            }
        }
    }
}
