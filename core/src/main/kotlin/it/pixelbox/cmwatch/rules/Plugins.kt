package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Plugin
import it.pixelbox.cmwatch.contract.PluginsAccount
import it.pixelbox.cmwatch.contract.State

/**
 * La sezione «Plugin e skill» della home (contratto 1.95, mockup approvato da Franz il 29/10 alle 10:39): per account i
 * plugin con un aggiornamento in alto, ciascuno col suo «Aggiorna», gli altri dietro «Mostra»; «Aggiorna tutto» per gli
 * account che ne hanno, con conferma.
 */
object Plugins {
    /** Quanti aggiornamenti in tutto, dal riassunto dello stato; null con un relay precedente. */
    fun pending(state: State?): Int? = state?.plugins?.accounts?.sumOf { it.updates.size }

    /** Gli account con almeno un aggiornamento, quelli a cui va «Aggiorna tutto». */
    fun accountsWithUpdates(state: State?): List<String> = state?.plugins?.accounts.orEmpty().filter { it.updates.isNotEmpty() }.map { it.account }

    data class Account(val account: String, val updates: List<Plugin>, val rest: List<Plugin>, val skills: Int, val skillNames: List<String>)

    /** Un account dell'elenco: prima quelli da aggiornare, poi gli altri per nome. */
    fun account(a: PluginsAccount): Account {
        val (up, rest) = a.plugins.partition { it.update == true }
        return Account(a.account, up.sortedBy { it.name }, rest.sortedBy { it.name }, a.skills.size, a.skills.map { s -> s.version?.let { "${s.name} $it" } ?: s.name })
    }

    /** L'argomento di `plugin_update`. */
    fun updateArg(account: String, p: Plugin): String = "$account:${p.id}"
}
