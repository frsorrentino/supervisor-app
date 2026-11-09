package it.pixelbox.cmwatch.wear.follow

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.FollowRules
import it.pixelbox.cmwatch.wear.push.Notifier

/**
 * Segui: «▶ ledger-api · 4 m» sul quadrante mentre la sessione seguita lavora; sparisce quando si ferma.
 * Su Android 16 (API 36+) la stessa notifica chiede anche di essere un «live update» promosso, con `ProgressStyle` indeterminata
 * (specifica di Franz, 12/09 15:26, punto 7).
 *
 * Google Play l'ha rifiutata il 03/11 (app-wear-play-rifiuto-09-11, «attività in corso mancante»): quando il live update riusciva
 * la notifica partiva senza `OngoingActivity`, e sui Wear OS 6 dei revisori mancavano l'indicatore sul quadrante e il chip nelle
 * app recenti. Ora c'è un solo builder: l'`OngoingActivity` sempre, il live update in più dove la piattaforma lo sa fare.
 */
class FollowOngoing(private val ctx: Context) {
    private var shownFor: String? = null

    fun update(state: State?, now: Long) {
        val s = state?.let { FollowRules.ongoing(it) }
        if (s == null) { if (shownFor != null) { NotificationManagerCompat.from(ctx).cancel(ID); shownFor = null }; return }
        runCatching { NotificationManagerCompat.from(ctx).notify(ID, notification(ctx, s, now)) }
        shownFor = s.name
    }

    companion object {
        const val ID = 7001

        /** La notifica di Segui con la sua `OngoingActivity`; da `sdk` 36 anche il live update promosso. `tap` apre la sessione. */
        fun notification(
            ctx: Context, s: Session, now: Long, sdk: Int = android.os.Build.VERSION.SDK_INT,
            tap: PendingIntent = PendingIntent.getActivity(ctx, ID, Intent(Intent.ACTION_VIEW, Uri.parse("cmwatch://session/${s.name}")).setPackage(ctx.packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        ): Notification {
            val status = FollowRules.status(s, now)
            val builder = NotificationCompat.Builder(ctx, Notifier.CHANNEL_FOLLOW)
                .setSmallIcon(R.drawable.ic_app_mono).setContentTitle(status).setOngoing(true).setContentIntent(tap)
                .setPriority(NotificationCompat.PRIORITY_LOW).setCategory(NotificationCompat.CATEGORY_STATUS)
            if (sdk >= 36) builder.setStyle(NotificationCompat.ProgressStyle().setProgressIndeterminate(true)).setRequestPromotedOngoing(true).setShortCriticalText(s.name)
            OngoingActivity.Builder(ctx, ID, builder)
                .setStaticIcon(R.drawable.ic_app_mono)
                .setStatus(Status.Builder().addTemplate(status).build())
                .setTouchIntent(tap)
                .build().apply(ctx)
            return builder.build()
        }
    }
}
