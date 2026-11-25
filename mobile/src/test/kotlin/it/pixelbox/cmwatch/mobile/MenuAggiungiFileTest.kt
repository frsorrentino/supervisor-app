package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.AttachMenuItems
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.V3
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * «Vai ai tuoi messaggi» in coda al menu del + (app-fumetto-menu-file-25-11, Franz 25/11 12:30: «spostiamo questa opzione nel menu di
 * aggiungi un file, in coda?»): il tasto tondo col fumetto non galleggia più sopra la chat; la sua azione è l'ultima voce del menu, con
 * lo stesso fumetto, come le altre voci. In una sessione dopo galleria, fotocamera e file; nella master dopo le azioni ricorrenti.
 * Il popup non si disegna nei provini: le stesse voci in una superficie come quella del menu. Golden registrati su Linux nella CI.
 */
class MenuAggiungiFileTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (560 * 2.75).toInt()), theme = "android:Theme.Material.NoActionBar")
    @After fun night() { V3.day = false }

    private fun shot(day: Boolean) = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = day) {
            Column(Modifier.background(V3.Bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf<(() -> Unit)?>(null, {}).forEach { recurring ->
                    Surface(color = V3.S1, shape = MaterialTheme.shapes.extraSmall, shadowElevation = 3.dp, modifier = Modifier.width(232.dp)) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            AttachMenuItems(attach = true, files = true, onRecurring = recurring, onGallery = {}, onCamera = {}, onFile = {}, onMine = {})
                        }
                    }
                }
            }
        }
    }

    @Test fun notte() = shot(day = false)
    @Test fun giorno() = shot(day = true)
}
