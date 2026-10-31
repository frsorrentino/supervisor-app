package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.PluginsPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * I comandi di «Plugin e skill», fuori dal disegno (Franz 31/10 15:31; pluginsRun.ts nella web). Un aggiornamento dura decine di
 * secondi: dopo la conferma lo stato «in corso» si vede subito e la seconda pressione non manda un altro comando. All'esito resta
 * il testo del relay e la lista si rilegge, perché il riassunto di /state.plugins arriva solo con la push dopo. Vive fuori dalla
 * lista: se la riga esce dallo schermo o si cambia pagina, il comando e il suo esito non si perdono.
 */
class PluginsRun(
    private val scope: CoroutineScope,
    private val load: suspend () -> PluginsPage?,
    private val run: suspend (CmdOp, String?) -> CmdResult?,
    initial: PluginsPage? = null,
) {
    /** `text` null: nessuna risposta entro il limite. */
    data class Note(val ok: Boolean, val text: String?)
    data class State(val page: PluginsPage?, val loading: Boolean = false, val busy: Set<String> = emptySet(), val notes: Map<String, Note> = emptyMap(), val all: Boolean = false)

    private val _state = MutableStateFlow(State(initial))
    val state: StateFlow<State> = _state

    fun refresh() { scope.launch { reload() } }

    /** La sezione si apre: un esito di un giro vecchio non resta, e l'elenco si rilegge. */
    fun opened() {
        if (_state.value.busy.isEmpty()) _state.update { it.copy(notes = emptyMap()) }
        refresh()
    }

    fun act(key: String, op: CmdOp, arg: String?, note: String) {
        if (key in _state.value.busy) return
        scope.launch { perform(key, op, arg, note) }
    }

    fun updateAll(accounts: List<String>) {
        if (_state.value.all) return
        _state.update { it.copy(all = true) }
        scope.launch {
            try { accounts.forEach { a -> perform("all:$a", CmdOp.PLUGIN_UPDATE_ALL, a, a) } } finally { _state.update { it.copy(all = false) } }
        }
    }

    private suspend fun reload() {
        _state.update { it.copy(loading = true) }
        val p = load()
        _state.update { it.copy(page = p ?: it.page, loading = false) }
    }

    private suspend fun perform(key: String, op: CmdOp, arg: String?, note: String) {
        _state.update { it.copy(busy = it.busy + key) }
        val r = try { run(op, arg) } finally { _state.update { it.copy(busy = it.busy - key) } }
        _state.update { it.copy(notes = it.notes + (note to if (r != null) Note(r.ok, r.text) else Note(false, null))) }
        if (op != CmdOp.PLUGIN_CHECK) reload()
    }
}
