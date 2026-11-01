package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*

/**
 * I dati del riquadro «Prova» di «Cosa sa fare» (Franz 01/11 22:13): lo stato della demo col di più che alla demo del telefono manca, gli stessi
 * dati di demo.ts sul web. Solo nomi finti. Niente transport: qui non passa nessun comando.
 */
object FeatureDemoState {
    private fun task(id: String, title: String, session: String, you: Boolean, reason: String? = null, at: Long = 0) = Commitment(
        task = id, title = title, short = title, session = session, state = if (you) "awaiting_ok" else "running", approved = !you, waits = CommitmentWaits(if (you) "you" else session, if (you) "person" else "session"),
        paused = reason?.let { CommitmentPause(it, at) },
    )

    fun state(base: State, now: Long, it: Boolean): State {
        val names = base.sessions.filter { s -> s.state != SessionState.GONE }.map { s -> s.name }
        val first = names.firstOrNull() ?: "storefront"
        val master = base.sessions.takeIf { l -> l.none { s -> s.name == "master" } }?.let { Session(id = "m", name = "master", account = "personal", project = "personal/control-room", state = SessionState.IDLE, since = now - 3600) }
        val busy = Session(id = "a", name = "admin-panel", account = "personal", project = "personal/admin-panel", state = SessionState.BUSY, since = now - 1500, turnStarted = now - 1500, tool = "Bash npm test")
        return base.copy(
            ts = now, sessions = listOfNotNull(master) + base.sessions + busy,
            commitments = listOf(
                task("demo-cover", if (it) "Immagine di copertina" else "Cover image", first, true, if (it) "Mandami l'immagine di copertina" else "Send me the cover image", now - 1200),
                task("demo-docs", if (it) "Pagine API" else "API pages", first, false),
                task("demo-users", if (it) "Tabella utenti" else "Users table", "admin-panel", false),
            ),
            system = SystemStatus(at = now - 30, cores = 8, load1 = 3.4, loadPerCore = 0.42, memTotalMb = 16000, memAvailableMb = 10100, level = "green", loadLevel = "green", memLevel = "green"),
            plugins = PluginsSummary(now - 2400, listOf(PluginsAccountSummary("personal", listOf("docs-helper@demo", "release-notes@demo")))),
            hostsStatus = listOf(HostStatus("win", true, 6_400_000_000, 16_000_000_000)),
            suggestions = listOf(
                Suggestion("sg-1", "recurring", text = if (it) "storefront: chiedi spesso com'è andata la CI, la aggiungo ai Ricorrenti?" else "storefront: you often ask how the CI went, add it to Recurring?", at = now - 900),
                Suggestion("sg-2", "cleanup", text = if (it) "admin-panel: due compiti in pausa da 2 giorni, li archivio?" else "admin-panel: two tasks paused for 2 days, archive them?", at = now - 900),
            ),
        )
    }

    /** Gli esiti di oggi, uno per tipo, sempre da mezzanotte in poi. */
    fun page(now: Long, zone: java.time.ZoneId, it: Boolean): TimelinePage {
        val day = Kpi.midnight(now, zone)
        fun e(ago: Long, text: String, en: String, type: String?) = TimelineEvent(maxOf(day + 1, now - ago), "outcome", if (it) text else en, type = type)
        return TimelinePage(day, listOf(
            TimelineSession("payments-api", true, events = listOf(e(5400, "I test dei rimborsi sono rossi sul caso parziale", "Refund tests are red on the partial case", "problem"), e(2700, "Rilascio 2.8.0 pronto: aspetta il tuo ok", "Deploy 2.8.0 ready: waiting for your ok", "waiting"))),
            TimelineSession("storefront", true, events = listOf(e(4800, "Totali del carrello corretti, test verdi", "Cart totals fixed, tests green", "done"), e(600, "Test in corso sul checkout", "Tests running on the checkout", "running"))),
            TimelineSession("blog", true, events = listOf(e(3600, "Post pubblicato sul sito di prova", "Post published on the staging site", "shipped"), e(7200, "Il feed si genera al momento della build", "The feed is generated at build time", "answer"))),
            TimelineSession("admin-panel", true, events = listOf(e(6300, "Filtri aggiunti alla tabella utenti", "Filters added to the users table", null))),
        ))
    }
}
