package it.pixelbox.cmwatch.mobile.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.rules.PhonePrimary
import kotlinx.coroutines.launch

/** Dall'azione della notifica al comando: pura, per il test. */
object ReplyRoute {
    const val OPTION = "it.pixelbox.cmwatch.mobile.OPTION"
    const val REPLY = "it.pixelbox.cmwatch.mobile.REPLY"
    /** Contratto 1.58: una strada dalla notifica di quota finita; `arg` = other_account | at_reset. */
    const val QUOTA = "it.pixelbox.cmwatch.mobile.QUOTA"
    const val ARG = "arg"
    /** Contratto 1.61: «Continua» dopo un turno fallito; `arg` = il testo da mandare come prompt. */
    const val CONTINUE = "it.pixelbox.cmwatch.mobile.CONTINUE"
    const val SESSION = "session"
    const val N = "n"
    const val TEXT = "text"

    sealed class Cmd {
        data class Option(val session: String, val n: Int) : Cmd()
        data class Text(val session: String, val text: String) : Cmd()
        data class Quota(val session: String, val arg: String) : Cmd()
        data class Continue(val session: String, val text: String) : Cmd()
    }

    /** Il testo della notifica risponde solo a una domanda ancora aperta; altrimenti diventa un prompt (revisione 29/09). */
    fun textTarget(session: Session?, text: String): PhonePrimary.Target =
        session?.let { PhonePrimary.target(it, text) } ?: PhonePrimary.Target.PROMPT

    fun from(action: String?, session: String?, n: Int, text: String?, arg: String? = null): Cmd? {
        if (session == null) return null
        return when (action) {
            OPTION -> if (n > 0) Cmd.Option(session, n) else null
            QUOTA -> arg?.takeIf { it.isNotEmpty() }?.let { Cmd.Quota(session, it) }
            CONTINUE -> arg?.takeIf { it.isNotBlank() }?.let { Cmd.Continue(session, it) }
            REPLY -> text?.trim()?.takeIf { it.isNotEmpty() }?.let { Cmd.Text(session, it) }
            else -> null
        }
    }
}

/** Risposta dalla notifica: opzione o testo scritto; senza rete resta in coda nel Repo e la notifica lo dice. */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val app = ctx.applicationContext as PhoneApp
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(ReplyRoute.TEXT)?.toString()
        val cmd = ReplyRoute.from(intent.action, intent.getStringExtra(ReplyRoute.SESSION), intent.getIntExtra(ReplyRoute.N, 0), text, intent.getStringExtra(ReplyRoute.ARG)) ?: return
        val done = goAsync()
        app.scope.launch {
            try {
                when (cmd) {
                    is ReplyRoute.Cmd.Option -> app.repo.answer(cmd.session, cmd.n)
                    is ReplyRoute.Cmd.Continue -> { app.notifier.closeTurnFailed(cmd.session); app.repo.prompt(cmd.session, cmd.text) }
                    is ReplyRoute.Cmd.Quota -> { app.notifier.closeQuotaLimit(cmd.session); app.repo.command(it.pixelbox.cmwatch.contract.CmdOp.QUOTA_ROUTE, cmd.session, cmd.arg) }
                    is ReplyRoute.Cmd.Text -> {
                        val live = app.repo.snapshot.value.state?.sessions?.firstOrNull { it.name == cmd.session }
                        if (ReplyRoute.textTarget(live, cmd.text) == PhonePrimary.Target.ANSWER_TEXT) app.repo.answerText(cmd.session, cmd.text)
                        else app.repo.prompt(cmd.session, cmd.text)
                    }
                }
                val session = when (cmd) { is ReplyRoute.Cmd.Option -> cmd.session; is ReplyRoute.Cmd.Text -> cmd.session; is ReplyRoute.Cmd.Quota -> cmd.session; is ReplyRoute.Cmd.Continue -> cmd.session }
                if (!app.isOnline()) app.notifier.waitingNetwork(session)
            } finally { done.finish() }
        }
    }
}
