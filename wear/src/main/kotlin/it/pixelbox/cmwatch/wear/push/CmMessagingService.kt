package it.pixelbox.cmwatch.wear.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.pixelbox.cmwatch.wear.CmApp

/** FCM data message {kind, session, ts, key} → WakeWorker (expedited). */
class CmMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        android.util.Log.i("cmwatch", "fcm message: ${message.data}")
        WakeWorker.enqueue(this, message.data)
        // Contratti 1.58, 1.61 e 1.68: quota finita, turno fallito e promemoria arrivano solo come eventi, da leggere qui.
        val key = message.data["key"]
        if (key != null && message.data["kind"] in setOf("quota_limit", "turn_failed", "attention_reminder", "attention_closed")) {
            val app = application as CmApp
            kotlinx.coroutines.runBlocking { kotlinx.coroutines.withTimeoutOrNull(8_000) { app.repo.event(key) } }?.let { e ->
                // Contratto 1.160: una voce risolta altrove; via la sua notifica dal polso, senza suono.
                if (e.kind == it.pixelbox.cmwatch.contract.EventKind.ATTENTION_CLOSED) setOfNotNull(e.ref, e.wid).forEach { r ->
                    app.notifier.closeAttention(it.pixelbox.cmwatch.contract.Attention(e.attentionKind ?: "card", e.session, r))
                } else app.alert(if (message.data["quiet"] == "1") e.copy(quiet = true) else e)
            }
        }
    }
    override fun onNewToken(token: String) { (application as CmApp).subscribeTopic() }
}
