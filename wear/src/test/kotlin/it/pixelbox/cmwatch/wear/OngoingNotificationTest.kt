package it.pixelbox.cmwatch.wear

import androidx.core.app.NotificationCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.wear.follow.FollowOngoing
import it.pixelbox.cmwatch.wear.push.SpeakService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * app-wear-play-rifiuto-09-11: Google Play ha rifiutato l'app il 03/11 anche per «attività in corso mancante». Ogni notifica
 * continua dell'orologio ha la sua `OngoingActivity` (icona sul quadrante, chip nelle app recenti, tocco che apre l'app): Segui
 * anche quando su Android 16 è un live update promosso, che prima partiva senza; la lettura vocale, che non l'aveva mai avuta.
 */
class OngoingNotificationTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND)
    private val session = ContractJson.decodeState(File("../contract/state-1-question.json").readText()).sessions.first()
    // Sotto layoutlib PendingIntent.getActivity non dà un intent: uno vuoto, costruito a mano, basta all'OngoingActivity.
    private val tap by lazy {
        val sender = Class.forName("android.content.IIntentSender")
        val target = java.lang.reflect.Proxy.newProxyInstance(sender.classLoader, arrayOf(sender)) { _, m, _ -> if (m.name == "asBinder") android.os.Binder() else null }
        android.app.PendingIntent::class.java.declaredConstructors.first { it.parameterTypes.contentEquals(arrayOf(sender)) }
            .apply { isAccessible = true }.newInstance(target) as android.app.PendingIntent
    }

    @Test fun followHasItsOngoingActivityWithTheLiveUpdateToo() {
        for (sdk in listOf(33, 35, 36)) {
            val n = FollowOngoing.notification(paparazzi.context, session, 1789210800L, sdk, tap)
            assertTrue("api $sdk: continua", n.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
            assertNotNull("api $sdk: OngoingActivity", androidx.wear.ongoing.SerializationHelper.create(n))
            assertEquals("api $sdk: live update promosso", sdk >= 36, NotificationCompat.isRequestPromotedOngoing(n))
        }
    }

    @Test fun readingAloudHasItsOngoingActivity() {
        val n = SpeakService.notification(paparazzi.context, tap)
        assertTrue(n.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        assertNotNull(androidx.wear.ongoing.SerializationHelper.create(n))
    }
}
