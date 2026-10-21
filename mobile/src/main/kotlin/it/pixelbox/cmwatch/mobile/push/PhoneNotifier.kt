package it.pixelbox.cmwatch.mobile.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.MainActivity
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.NotificationPlan
import it.pixelbox.cmwatch.rules.PhoneAlert
import it.pixelbox.cmwatch.rules.StrongAlert

/** Le notifiche del telefono (design 29/09): due canali, suono o silenzio secondo PhoneAlert. */
class PhoneNotifier(private val ctx: Context) {
    private val nm = NotificationManagerCompat.from(ctx)

    fun ensureChannels() {
        val m = ctx.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(NotificationChannel(CH_SOUND, ctx.getString(R.string.channel_questions), NotificationManager.IMPORTANCE_HIGH))
        m.createNotificationChannel(NotificationChannel(CH_QUIET, ctx.getString(R.string.channel_quiet), NotificationManager.IMPORTANCE_LOW))
        // Contratto 1.68: l'avviso forte, la stessa vibrazione dell'orologio (StrongAlert.VIBRATION) e il suono di sistema.
        m.createNotificationChannel(NotificationChannel(CH_STRONG, ctx.getString(R.string.channel_strong), NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true); vibrationPattern = StrongAlert.VIBRATION
        })
    }

    private fun channel(mode: PhoneAlert.Mode) = if (mode == PhoneAlert.Mode.SOUND) CH_SOUND else CH_QUIET
    /** 1.68: le interruzioni (domanda, ferma per te, turno fallito, quota finita, promemoria) sul canale forte. */
    private fun strong(mode: PhoneAlert.Mode) = if (mode == PhoneAlert.Mode.SOUND) CH_STRONG else CH_QUIET
    private fun id(session: String) = session.hashCode()

    private val labels get() = NotificationPlan.Labels(
        open = "", reply = ctx.getString(R.string.answer_free), retry = ctx.getString(R.string.retry), read = ctx.getString(R.string.read_aloud),
        stop = "", write = ctx.getString(R.string.write_prompt), resume = ctx.getString(R.string.reopen), sent = "", confirmed = "",
        notDelivered = ctx.getString(R.string.not_delivered), sessions = ctx.getString(R.string.tab_sessions),
    )

    /** `queue`: il tocco apre la coda «Ti aspettano» invece dell'app dov'era (piano 30/09, Task 1). */
    private fun open(code: Int, queue: Boolean = false) = PendingIntent.getActivity(
        ctx, code, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(MainActivity.EXTRA_QUEUE, queue),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun broadcast(action: String, session: String, n: Int, code: Int, mutable: Boolean = false) = PendingIntent.getBroadcast(
        ctx, code, Intent(ctx, ReplyReceiver::class.java).setAction(action).putExtra(ReplyRoute.SESSION, session).putExtra(ReplyRoute.N, n),
        (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun post(id: Int, b: NotificationCompat.Builder) = runCatching { nm.notify(id, b.build()) }

    fun question(s: Session, mode: PhoneAlert.Mode) {
        val q = s.question ?: return
        val plan = NotificationPlan.question(s, labels, emptyList())
        val b = NotificationCompat.Builder(ctx, strong(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(plan.title).setContentText(q.shareable).setStyle(NotificationCompat.BigTextStyle().bigText(q.shareable))
            .setSubText(plan.subText).setContentIntent(open(openCode(s.name, queue = true), queue = true)).setOnlyAlertOnce(true)
        q.options.take(3).forEach { o ->
            b.addAction(0, it.pixelbox.cmwatch.rules.QuestionRules.optionLabel(o), broadcast(ReplyRoute.OPTION, s.name, o.n, id(s.name) * 10 + o.n))
        }
        b.addAction(
            NotificationCompat.Action.Builder(0, labels.reply, broadcast(ReplyRoute.REPLY, s.name, 0, id(s.name) * 10 + 9, mutable = true))
                .addRemoteInput(RemoteInput.Builder(ReplyRoute.TEXT).setLabel(labels.reply).build()).build(),
        )
        post(id(s.name), b)
    }

    fun outcome(s: Session, mode: PhoneAlert.Mode) {
        val o = s.outcome ?: return
        post(id(s.name), NotificationCompat.Builder(ctx, channel(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.name).setContentText(o.shareShort).setStyle(NotificationCompat.BigTextStyle().bigText(o.shareFull))
            .setSubText(s.account).setContentIntent(open(openCode(s.name, queue = false))).setAutoCancel(true))
    }

    /** Contratto 1.57: il controllo di un task allo stop, una riga verde o rossa che apre la sessione. */
    fun taskCheck(e: it.pixelbox.cmwatch.contract.Event) {
        val green = it.pixelbox.cmwatch.rules.AlertEvents.green(e)
        post(("task-" + e.ref).hashCode(), NotificationCompat.Builder(ctx, if (green || e.quiet) CH_QUIET else CH_SOUND).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(e.shareTitle).setContentText(e.shareBody.lineSequence().firstOrNull { it.isNotBlank() })
            .setStyle(NotificationCompat.BigTextStyle().bigText(e.shareBody))
            .setColor(if (green) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()).setColorized(false)
            .setContentIntent(openSession(e.session)).setAutoCancel(true))
    }

    /**
     * Contratto 1.58: quota finita. «Continua sull'altro account» e «riprendi al reset» sono tasti della notifica; «passa a
     * un'altra sessione» apre la sessione, dove si sceglie a chi.
     */
    fun quotaLimit(e: it.pixelbox.cmwatch.contract.Event, mode: PhoneAlert.Mode) {
        val session = e.ref ?: e.session ?: return
        val words = it.pixelbox.cmwatch.rules.AlertEvents.Words(
            ctx.getString(R.string.quota_way_other), ctx.getString(R.string.quota_way_reset), ctx.getString(R.string.quota_way_pass),
        )
        val hhmm = { s: Long -> java.time.Instant.ofEpochSecond(s).atZone(java.time.ZoneId.systemDefault()).toLocalTime().toString().take(5) }
        val b = NotificationCompat.Builder(ctx, if (e.quiet) CH_QUIET else strong(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(e.shareTitle).setContentIntent(openSession(session)).setAutoCancel(true)
        it.pixelbox.cmwatch.rules.AlertEvents.ways(e, words, hhmm).forEachIndexed { i, w ->
            b.addAction(0, w.label, w.arg?.let { arg ->
                PendingIntent.getBroadcast(
                    ctx, ("quota-$session-$i").hashCode(),
                    Intent(ctx, ReplyReceiver::class.java).setAction(ReplyRoute.QUOTA).putExtra(ReplyRoute.SESSION, session).putExtra(ReplyRoute.ARG, arg),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            } ?: openSession(session))
        }
        post(("quota-limit-$session").hashCode(), b)
    }

    /** Contratto 1.61: turno finito in errore, una riga e il tasto «Continua» che manda `continue_text`. */
    fun turnFailed(e: it.pixelbox.cmwatch.contract.Event, mode: PhoneAlert.Mode) {
        val session = e.ref ?: e.session ?: return
        val b = NotificationCompat.Builder(ctx, if (e.quiet) CH_QUIET else strong(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(e.shareTitle).setContentIntent(openSession(session)).setAutoCancel(true)
        e.continueText?.takeIf { it.isNotBlank() }?.let { text ->
            b.addAction(0, ctx.getString(R.string.turn_continue), PendingIntent.getBroadcast(
                ctx, ("continue-$session").hashCode(),
                Intent(ctx, ReplyReceiver::class.java).setAction(ReplyRoute.CONTINUE).putExtra(ReplyRoute.SESSION, session).putExtra(ReplyRoute.ARG, text),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ))
        }
        post(("turn-failed-$session").hashCode(), b)
    }

    /** Contratto 1.67: la settimana di un account, una riga senza tasti e senza suono, che apre l'utilizzo. */
    fun week(e: it.pixelbox.cmwatch.contract.Event) {
        post(("week-" + e.account + "-" + e.kind).hashCode(), NotificationCompat.Builder(ctx, CH_QUIET).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(e.shareTitle).setContentText(e.shareBody.lineSequence().firstOrNull { it.isNotBlank() })
            .setContentIntent(open(("week-" + e.account).hashCode())).setAutoCancel(true))
    }

    /** Contratto 1.68: la sessione è ferma per Franz (needs_you o turno bloccante): il motivo in una riga, il tocco la apre. */
    fun stopped(s: Session, mode: PhoneAlert.Mode) {
        val stop = StrongAlert.stop(s) ?: return
        post(("stop-" + s.name).hashCode(), NotificationCompat.Builder(ctx, strong(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.name).setContentText(stop.text).setStyle(NotificationCompat.BigTextStyle().bigText(stop.text))
            .setSubText(s.account).setWhen(stop.at * 1000).setContentIntent(openSession(s.name)).setOnlyAlertOnce(false))
    }

    /** Contratto 1.68: il promemoria del relay a 10 e 30 minuti, lo stesso avviso forte in una riga. */
    fun reminder(e: it.pixelbox.cmwatch.contract.Event, mode: PhoneAlert.Mode) {
        post(("reminder-" + e.ref).hashCode(), NotificationCompat.Builder(ctx, if (e.quiet) CH_QUIET else strong(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(e.shareTitle).setContentIntent(openSession(e.session)).setAutoCancel(true))
    }

    /** Contratto 1.76: impegni fermi da più di due ore senza un passo; l'avviso forte con una riga per compito. */
    fun stalled(e: it.pixelbox.cmwatch.contract.Event, mode: PhoneAlert.Mode) {
        post(("stalled-" + (e.ref ?: e.key)).hashCode(), NotificationCompat.Builder(ctx, if (e.quiet) CH_QUIET else strong(mode)).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(e.shareTitle).setContentText(e.body).setStyle(NotificationCompat.BigTextStyle().bigText(e.body))
            .setContentIntent(openSession(e.session)).setAutoCancel(true))
    }

    /** Contratto 1.68: la voce è uscita da `state.attention`, la sua notifica e il suo promemoria si chiudono. */
    fun closeAttention(a: it.pixelbox.cmwatch.contract.Attention) {
        val ref = a.ref ?: return
        nm.cancel(("reminder-$ref").hashCode())
        when (a.kind) {
            "needs_you", "turn_blocking" -> nm.cancel(("stop-$ref").hashCode())
            "turn_failed" -> closeTurnFailed(ref)
            "quota_limit" -> closeQuotaLimit(ref)
        }
    }

    fun closeTurnFailed(session: String) = nm.cancel(("turn-failed-$session").hashCode())

    private fun openSession(session: String?) = PendingIntent.getActivity(
        ctx, ("open-" + session).hashCode(),
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(MainActivity.EXTRA_SESSION, session),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun closeQuotaLimit(session: String) = nm.cancel(("quota-limit-$session").hashCode())

    fun quota(account: String, title: String) {
        post(("quota-$account").hashCode(), NotificationCompat.Builder(ctx, CH_QUIET).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentIntent(open(account.hashCode())).setAutoCancel(true))
    }

    /** Diario o resoconto pronti: una notifica silenziosa che apre l'app (il testo sta nella scheda Diario). */
    fun diary(title: String) {
        if (!ctx.getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled()) return
        val prefs = kotlinx.coroutines.runBlocking { (ctx.applicationContext as it.pixelbox.cmwatch.mobile.PhoneApp).prefs.current() }
        if (prefs.demoMode || !prefs.paired) return
        post(title.hashCode(), NotificationCompat.Builder(ctx, CH_QUIET).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentIntent(open(title.hashCode())).setAutoCancel(true))
    }

    fun waitingNetwork(session: String) {
        post(id(session), NotificationCompat.Builder(ctx, CH_QUIET).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(session).setContentText(ctx.getString(R.string.waiting_network)).setOnlyAlertOnce(true))
    }

    fun closeQuestion(session: String) = nm.cancel(id(session))

    companion object {
        const val CH_SOUND = "phone_questions"
        const val CH_QUIET = "phone_quiet"
        const val CH_STRONG = "phone_strong"

        /** Il codice del tocco: la domanda (apre la coda) e l'esito della stessa sessione hanno PendingIntent distinti. */
        fun openCode(session: String, queue: Boolean): Int = session.hashCode() * 2 + if (queue) 1 else 0
    }
}
