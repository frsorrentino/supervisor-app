package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.HostStatus
import it.pixelbox.cmwatch.contract.PluginsSummary
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.Suggestion
import it.pixelbox.cmwatch.contract.SystemStatus

/**
 * Cosa mostra una voce agganciata nello slot del tabellone (Franz 01/11 17:44, compito app-slot-info-01-11): titolo, barra e numero,
 * sotto una riga. Chiuse non è qui: il relay non dice quando una sessione chiusa dal PC si è chiusa (`since` e `closed.at` ripiegano
 * su `visto_ts`), quindi resta il numero e nessuna ora.
 */
object SlotInfo {
    /** `ramPct` null = la macchina è spenta e lo si dice al posto della percentuale. */
    data class HostRam(val name: String, val ramPct: Int?)

    /** `sessions` = le attive come «N aperte» in testa alla home: la master non conta. `pct` può passare 100 (più carico dei core); la barra si ferma a `barPct`. */
    data class SystemInfo(val pct: Int, val barPct: Int, val level: String, val ramPct: Int, val hosts: List<HostRam>, val sessions: Int)

    fun system(sys: SystemStatus, hosts: List<HostStatus>, sessions: List<Session>): SystemInfo {
        val pct = Math.round(sys.loadPerCore * 100).toInt().coerceAtLeast(0)
        val used = (1 - sys.memAvailableMb.toDouble() / maxOf(1, sys.memTotalMb)) * 100
        return SystemInfo(
            pct, pct.coerceAtMost(100), sys.loadLevel, Math.round(used).toInt().coerceIn(0, 100),
            hosts.mapNotNull { h ->
                val total = h.ramTotal ?: 0L
                when {
                    !h.up -> HostRam(h.name, null)
                    total > 0L && h.ramUsed != null -> HostRam(h.name, Math.round(h.ramUsed.toDouble() / total * 100).toInt().coerceIn(0, 100))
                    else -> null
                }
            },
            sessions.count { it.state != SessionState.GONE && it.name != ContextActions.MASTER },
        )
    }

    /** I numeri sono quelli della sezione Lavori ([Lavori.inProgress]); `last` è l'ultimo che aspetta Franz. */
    data class LavoriInfo(val you: Int, val total: Int, val last: String?, val lastSince: Long?) {
        val youPct: Int get() = if (total > 0) Math.round(you * 100f / total) else 0
    }

    fun lavori(ip: Lavori.InProgress): LavoriInfo {
        val last = ip.you.maxByOrNull { it.since ?: 0L }
        return LavoriInfo(ip.you.sumOf { it.n }, ip.total, last?.short, last?.since)
    }

    /** La prima proposta nello stesso ordine della card ([Suggestions.visible]). */
    fun suggestion(shown: List<Suggestion>): String? = shown.firstOrNull()?.text?.takeIf { it.isNotBlank() }

    /** Sotto Plugin e skill: i nomi da aggiornare, oppure quando è stato il controllo; `null, null` = non c'è niente da dire. */
    data class PluginsInfo(val names: List<String>, val checkedAt: Long?)

    fun plugins(s: PluginsSummary): PluginsInfo {
        val names = s.accounts.flatMap { it.updates }.map { it.substringBefore('@') }.distinct()
        return PluginsInfo(names, if (names.isEmpty()) s.checkedAt else null)
    }
}
