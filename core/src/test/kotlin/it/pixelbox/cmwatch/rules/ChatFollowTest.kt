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

    // app-scroll-master-lavoro-27-11: la chat riletta non si sposta per portare in vista qualcosa.
    @Test fun readingHoldsEveryBringIntoView() {
        assertTrue("non segue il fondo", ChatFollow.holdBringIntoView(follow = false, atEnd = false, touching = false, flinging = false, sinceTapMs = 5_000))
        assertTrue("segue ma non è in fondo", ChatFollow.holdBringIntoView(follow = true, atEnd = false, touching = false, flinging = false, sinceTapMs = 5_000))
        assertTrue("dito giù anche in fondo", ChatFollow.holdBringIntoView(follow = true, atEnd = true, touching = true, flinging = false, sinceTapMs = 5_000))
        assertTrue("slancio in corso", ChatFollow.holdBringIntoView(follow = true, atEnd = true, touching = false, flinging = true, sinceTapMs = 5_000))
    }

    @Test fun atTheEndOrRightAfterATapItPasses() {
        assertFalse("in fondo, ferma", ChatFollow.holdBringIntoView(follow = true, atEnd = true, touching = false, flinging = false, sinceTapMs = 5_000))
        assertFalse("un tasto toccato nella risposta", ChatFollow.holdBringIntoView(follow = false, atEnd = false, touching = false, flinging = false, sinceTapMs = 200))
    }

    @Test fun releasedAboveTheBottomItStaysWhereTheFingerLeftIt() {
        assertFalse(ChatFollow.afterRelease(atEnd = false))
        assertTrue(ChatFollow.afterRelease(atEnd = true))
    }
}
