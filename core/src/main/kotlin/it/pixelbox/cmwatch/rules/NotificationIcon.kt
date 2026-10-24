package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.EventKind

/**
 * L'icona piccola della notifica dice il tipo di avviso (Franz, 22/10 13:36), al posto del simbolo che stava in testa al
 * titolo (❓ ✓ ✗ ⚠ ⛔): la stessa per telefono e orologio; il drawable lo sceglie l'app.
 */
object NotificationIcon {
    enum class Icon { QUESTION, DONE, CHECK, CROSS, BLOCK, WARN, WEEK, HAND, BELL, CLOCK, NIGHT, APP }

    /** `ok` conta solo per un `task_check` (contratto 1.78, o il «✓» del titolo con un relay precedente). */
    fun of(kind: EventKind, ok: Boolean? = null): Icon = when (kind) {
        EventKind.QUESTION, EventKind.ELICITATION -> Icon.QUESTION
        EventKind.OUTCOME -> Icon.DONE
        EventKind.TASK_CHECK -> if (ok == false) Icon.CROSS else Icon.CHECK
        EventKind.GONE -> Icon.CROSS
        EventKind.QUOTA_LIMIT, EventKind.QUOTA -> Icon.BLOCK
        EventKind.TURN_FAILED, EventKind.RESTART_FAILED, EventKind.RELAY_STALE -> Icon.WARN
        EventKind.WEEK_UNUSED, EventKind.WEEK_PACE -> Icon.WEEK
        EventKind.NEEDS_YOU, EventKind.TURN_BLOCKING -> Icon.HAND
        EventKind.ATTENTION_REMINDER -> Icon.BELL
        EventKind.TASK_STALLED -> Icon.CLOCK
        EventKind.RECAP, EventKind.NIGHT_REPORT -> Icon.NIGHT
        EventKind.ANSWERED, EventKind.LAUNCHED, EventKind.RESUMED -> Icon.APP
    }
}
