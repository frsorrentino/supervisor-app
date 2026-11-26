package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** app-apertura-master-scatti-26-11: un dito sulla chat mentre la risposta esce ferma l'inseguimento del fondo al primo gesto. */
class ChatFollowTest {
    @Test fun followsWhenFreeAndThereIsMoreBelow() = assertTrue(ChatFollow.chase(follow = true, below = true, items = 3, scrolling = false, touching = false))

    @Test fun aFingerOnTheListStopsTheChaseAtOnce() =
        assertFalse("il dito appoggiato, prima ancora del trascinamento", ChatFollow.chase(follow = true, below = true, items = 3, scrolling = false, touching = true))

    @Test fun neverDuringAScroll() = assertFalse(ChatFollow.chase(follow = true, below = true, items = 3, scrolling = true, touching = false))

    @Test fun releasedAboveTheBottomItStaysWhereTheFingerLeftIt() {
        assertFalse(ChatFollow.afterRelease(atEnd = false))
        assertTrue(ChatFollow.afterRelease(atEnd = true))
    }
}
