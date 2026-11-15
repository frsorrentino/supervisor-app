package it.pixelbox.cmwatch.wear.push

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.pixelbox.cmwatch.wear.CmApp

/** Sveglia FCM → lavoro expedited: un GET di /state, diff con Room, notifiche solo per il nuovo, tile e complication. */
class WakeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        android.util.Log.i("cmwatch", "wake worker")
        val app = applicationContext as CmApp
        val foreground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        // Contratto 1.148: l'etag e il published_at della push; al primo giro, se lo stato è già qui, niente GET. Un riprova rilegge.
        val push = if (runAttemptCount == 0) PUSH_KEYS.mapNotNull { k -> inputData.getString(k)?.let { k to it } }.toMap() else emptyMap()
        val ok = app.onWake(notify = !foreground, push = push)
        // Un GET perso in un cambio di rete si riprova, al massimo tre volte (piano prestazioni, Task 11): prima la sveglia
        // diceva sempre «fatto» e tile e complication restavano vecchie fino all'evento dopo.
        return if (ok || runAttemptCount >= 3) Result.success() else Result.retry()
    }

    companion object {
        private val PUSH_KEYS = listOf(it.pixelbox.cmwatch.rules.PushState.ETAG, it.pixelbox.cmwatch.rules.PushState.PUBLISHED_AT)

        fun enqueue(ctx: Context, push: Map<String, String> = emptyMap()) {
            val data = androidx.work.Data.Builder().apply { PUSH_KEYS.forEach { k -> push[k]?.let { putString(k, it) } } }.build()
            val req = OneTimeWorkRequestBuilder<WakeWorker>().setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).setInputData(data)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS).build()
            // Più sveglie ravvicinate fanno un GET solo, l'ultimo: quello in corso si ferma (le chiamate si possono interrompere
            // dal Task 4). Con APPEND_OR_REPLACE N messaggi diventavano N GET in fila; con KEEP si perderebbe uno stato nuovo
            // arrivato mentre il GET di prima era già partito.
            WorkManager.getInstance(ctx).enqueueUniqueWork("wake", ExistingWorkPolicy.REPLACE, req)
        }
    }
}
