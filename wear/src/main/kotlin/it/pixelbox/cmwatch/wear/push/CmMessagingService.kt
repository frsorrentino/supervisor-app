package it.pixelbox.cmwatch.wear.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.pixelbox.cmwatch.wear.CmApp

/** FCM data message {kind, session, ts, key} → WakeWorker (expedited). */
class CmMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        android.util.Log.i("cmwatch", "fcm message: ${message.data}")
        WakeWorker.enqueue(this)
        // Contratti 1.58, 1.61 e 1.68: quota finita, turno fallito e promemoria arrivano solo come eventi, da leggere qui.
        val key = message.data["key"]
        if (key != null && message.data["kind"] in setOf("quota_limit", "turn_failed", "attention_reminder")) {
            val app = application as CmApp
            kotlinx.coroutines.runBlocking { kotlinx.coroutines.withTimeoutOrNull(8_000) { app.repo.event(key) } }
                ?.let { e -> app.alert(if (message.data["quiet"] == "1") e.copy(quiet = true) else e) }
        }
    }
    override fun onNewToken(token: String) { (application as CmApp).subscribeTopic() }
}
