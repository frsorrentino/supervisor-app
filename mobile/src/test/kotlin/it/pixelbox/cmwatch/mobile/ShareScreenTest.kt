package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

class ShareScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())

    @Test fun shareText() = paparazzi.snapshot { CmPhoneTheme(still = true) { ShareScreen(st, "il pulsante del checkout è grigio", attached = null, sending = false, onSend = { _, _ -> }) } }
    @Test fun shareImage() = paparazzi.snapshot { CmPhoneTheme(still = true) { ShareScreen(st, "", attached = it.pixelbox.cmwatch.mobile.ui.ShareAttachment("Con l'immagine condivisa", true), sending = false, onSend = { _, _ -> }) } }
    @Test fun shareVideo() = paparazzi.snapshot { CmPhoneTheme(still = true) { ShareScreen(st, "", attached = it.pixelbox.cmwatch.mobile.ui.ShareAttachment("video.mp4, 12 MB (massimo 50 MB)", false), sending = false, onSend = { _, _ -> }) } }
    @Test fun shareFileTooBig() = paparazzi.snapshot { CmPhoneTheme(still = true) { ShareScreen(st, "", attached = it.pixelbox.cmwatch.mobile.ui.ShareAttachment("video.mp4 pesa 80 MB: il limite è 50 MB, non si può mandare", false, blocked = true), sending = false, onSend = { _, _ -> }) } }
    @Test fun shareOldRelay() = paparazzi.snapshot { CmPhoneTheme(still = true) { ShareScreen(st.copy(share = null), "", attached = it.pixelbox.cmwatch.mobile.ui.ShareAttachment("Con l'immagine condivisa", true), sending = false, onSend = { _, _ -> }) } }
}
