package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.rules.NotificationIcon.Icon
import org.junit.Assert.assertEquals
import org.junit.Test

/** Franz, 22/10 13:36: l'icona della notifica dice il tipo di avviso, al posto del simbolo nel titolo. */
class NotificationIconTest {
    @Test fun eachAlertKindHasItsIcon() {
        assertEquals(Icon.QUESTION, NotificationIcon.of(EventKind.QUESTION))
        assertEquals(Icon.DONE, NotificationIcon.of(EventKind.OUTCOME))
        assertEquals(Icon.CROSS, NotificationIcon.of(EventKind.GONE))
        assertEquals(Icon.BLOCK, NotificationIcon.of(EventKind.QUOTA_LIMIT))
        assertEquals(Icon.BLOCK, NotificationIcon.of(EventKind.QUOTA))
        assertEquals(Icon.WARN, NotificationIcon.of(EventKind.TURN_FAILED))
        assertEquals(Icon.WARN, NotificationIcon.of(EventKind.RESTART_FAILED))
        assertEquals(Icon.WARN, NotificationIcon.of(EventKind.RELAY_STALE))
        assertEquals(Icon.WEEK, NotificationIcon.of(EventKind.WEEK_UNUSED))
        assertEquals(Icon.WEEK, NotificationIcon.of(EventKind.WEEK_PACE))
        assertEquals(Icon.HAND, NotificationIcon.of(EventKind.NEEDS_YOU))
        assertEquals(Icon.HAND, NotificationIcon.of(EventKind.TURN_BLOCKING))
        assertEquals(Icon.BELL, NotificationIcon.of(EventKind.ATTENTION_REMINDER))
        assertEquals(Icon.CLOCK, NotificationIcon.of(EventKind.TASK_STALLED))
        assertEquals(Icon.NIGHT, NotificationIcon.of(EventKind.RECAP))
        assertEquals(Icon.NIGHT, NotificationIcon.of(EventKind.NIGHT_REPORT))
        assertEquals(Icon.APP, NotificationIcon.of(EventKind.LAUNCHED))
    }

    @Test fun aTaskCheckIsACheckOrACrossByItsOutcome() {
        assertEquals(Icon.CHECK, NotificationIcon.of(EventKind.TASK_CHECK, ok = true))
        assertEquals(Icon.CROSS, NotificationIcon.of(EventKind.TASK_CHECK, ok = false))
        assertEquals(Icon.CHECK, NotificationIcon.of(EventKind.TASK_CHECK))
    }
}
