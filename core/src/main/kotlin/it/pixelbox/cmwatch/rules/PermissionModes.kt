package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State

/**
 * La modalità dei permessi (mockup del 14/10, ok di Franz il 15/10 alle 07:39): gli stessi segni del terminale di Claude
 * Code, ⏸ piano, ⏵⏵ modifiche automatiche e automatica, ⏵⏵ ambra senza conferme, ? per un nome che l'app non conosce; niente
 * per «chiede sempre». Contratto 1.69: il cambio dal pannello modello ed effort con l'op `permission_mode`.
 */
object PermissionModes {
    enum class Glyph { NONE, PLAN, FORWARD, BYPASS, UNKNOWN }

    const val DEFAULT = "default"
    const val PLAN = "plan"
    const val ACCEPT_EDITS = "acceptEdits"
    const val AUTO = "auto"
    const val BYPASS = "bypassPermissions"

    /** Dal meno al più autonomo: l'ordine dei tasti nel pannello. */
    private val ORDER = listOf(PLAN, DEFAULT, ACCEPT_EDITS, AUTO, BYPASS)

    fun glyph(mode: String?): Glyph = when (mode) {
        null, "", DEFAULT -> Glyph.NONE
        PLAN -> Glyph.PLAN
        ACCEPT_EDITS, AUTO -> Glyph.FORWARD
        BYPASS -> Glyph.BYPASS
        else -> Glyph.UNKNOWN
    }

    /** Nella home il segno solo se diverso dal solito: «senza conferme» in una sessione partita così non si segna. */
    fun homeGlyph(s: Session): Glyph = glyph(s.permissionMode).takeUnless { it == Glyph.BYPASS && s.permissionBypass } ?: Glyph.NONE

    /** Il relay sa cambiarla (contratto 1.69): l'op è in `state.ops`. */
    fun canChange(state: State?, s: Session): Boolean = state?.ops?.contains("permission_mode") == true && s.state != it.pixelbox.cmwatch.contract.SessionState.GONE

    /** I tasti del gruppo Permessi: «senza conferme» solo se la sessione ci può arrivare. */
    fun choices(s: Session): List<String> = ORDER.filter { it != BYPASS || s.permissionBypass || s.permissionMode == BYPASS }

    /** Più autonomia di adesso: il tasto si tocca due volte (la prima chiede conferma); toglierne non chiede niente. */
    fun needsConfirm(current: String?, wanted: String): Boolean = rank(wanted) > rank(current ?: DEFAULT)

    private fun rank(mode: String): Int = ORDER.indexOf(mode).let { if (it < 0) ORDER.indexOf(DEFAULT) else it }
}
