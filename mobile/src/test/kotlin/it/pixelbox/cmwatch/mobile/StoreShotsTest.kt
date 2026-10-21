package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.ChatFeed
import it.pixelbox.cmwatch.rules.ChatRules
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.Sent
import it.pixelbox.cmwatch.rules.Summary
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * Le immagini intere. java.awt non è sul classpath di compilazione dei test Android, ma c'è a runtime: il gestore dei
 * fotogrammi è un proxy e ImageIO si chiama per riflessione.
 */
internal class FullSize(private val dir: File) : SnapshotHandler {
    override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int): SnapshotHandler.FrameHandler =
        java.lang.reflect.Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SnapshotHandler.FrameHandler::class.java)) { _, m, args ->
            if (m.name == "handle") {
                dir.mkdirs()
                val rendered = Class.forName("java.awt.image.RenderedImage")
                Class.forName("javax.imageio.ImageIO").getMethod("write", rendered, String::class.java, File::class.java)
                    .invoke(null, args[0], "PNG", File(dir, "${snapshot.testName.methodName}.png"))
            }
            null
        } as SnapshotHandler.FrameHandler
    override fun close() {}
}

/**
 * Gli screenshot del telefono per Google Play (Franz via master, 13/10 14:52): le schermate intere della demo, a 1080x2160
 * (Play vuole il lato lungo al massimo il doppio del corto). Solo dati inventati delle fixture, mai quelli di Franz.
 * Paparazzi riduce le immagini a 1000 px, quindi un gestore suo le scrive intere in build/store-shots/<lingua>/.
 * Si lanciano con `-Pstore.shots=true` (e `-Pstore.locale=en`); senza, i test non fanno niente e non toccano i golden.
 */
class StoreShotsTest {
    private val locale = System.getProperty("store.locale") ?: "it"
    private val on = System.getProperty("store.shots") == "true"


    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2160, locale = locale),
        theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = FullSize(File("build/store-shots/$locale")),
        useDeviceResolution = true,
    )

    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val menu = MenuActions(null, "", false, {}, {}, {}, {}, {})
    private val master = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null, suggestion = null)
    private val masterEntries = listOf(
        TranscriptEntry("u1.0", "user", text = "How is phase 2.2 of atlas-shop going?", at = st.ts - 600, origin = "phone"),
        TranscriptEntry("a1.0", "assistant", text = "Phase 2.2 is running on atlas-shop: tests green, 40 of 40. Local commits only: push and release with your ok.\n\nEsito: phase 2.2 running\nProssimi: push it · try it live", at = st.ts - 560),
    )

    private fun shot(content: @androidx.compose.runtime.Composable () -> Unit) {
        if (on) paparazzi.snapshot { CmPhoneTheme(still = true) { content() } }
    }

    /** La home: chi ti aspetta, chi lavora, chi ha finito; sotto, la barra della master. */
    @Test fun s1Home() = shot {
        val summary = Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet())
        SessionSheet(master, st.ts, emptyList(), 120, none, grid = true,
            home = { _, _ -> Column { SummaryList(summary, {}, { _, _ -> }, { _, _ -> }, {}, {}, startOpen = setOf("sessions")) } },
            homeOpen = true,
            appBar = { PageHeader(null, st.sessions, st.sessions.count { it.state != SessionState.GONE }, st.ts, {}, {}, menu) },
            bar = { MasterDock(master, MasterHome.hero(masterEntries, master), {}, {}) },
            feed = ChatFeed.merge(masterEntries, emptyList()))
    }

    /** Una domanda con le sue opzioni, a cui si risponde con un tocco. */
    @Test fun s2Question() = shot {
        val s = st.sessions.first { it.question != null }
        SessionSheet(s, st.ts, emptyList(), 120, none, choices = st.choices, ops = st.ops, canAttach = true,
            appBar = { PageHeader(s.name, st.sessions, 3, st.ts, {}, {}, menu) })
    }

    /** La conversazione di una sessione, con i file che manda e un messaggio partito dal telefono. */
    @Test fun s3Conversation() = shot {
        val t = st.ts - 600
        val entries = listOf(
            TranscriptEntry("u1.0", "user", text = "Add the Tuesday meeting notes to the draft", at = t, origin = "pc"),
            TranscriptEntry("a1.0", "assistant", text = "I'll read the draft first.", at = t + 5),
            TranscriptEntry("a1.1", "tool", text = "docs/draft.md", at = t + 5, tool = "Read"),
            TranscriptEntry("a4.0", "assistant", text = "The notes file was missing, so I added the Tuesday section to the draft by hand.", at = t + 40, turn = TranscriptTurn(t, t + 45, 4020, 130)),
            TranscriptEntry("a7.0", "tool", text = "Tuesday minutes", at = t + 155, tool = "SendUserFile", files = listOf(TranscriptFile("/w/field-notes/docs/minutes.pdf", "application/pdf", 212_000))),
            TranscriptEntry("p1.0", "user", text = "Also add the attendees list", at = t + 158, origin = "phone"),
        )
        val mine = Sent("c1", "field-notes", "Also add the attendees list", t + 156)
        val s = st.sessions.first { it.state == SessionState.BUSY }
        SessionSheet(s, st.ts, emptyList(), 120, none, choices = st.choices, ops = st.ops, canAttach = true,
            feed = ChatFeed.merge(entries, listOf(mine to ChatRules.Status.WORKING)), more = true,
            appBar = { PageHeader(s.name, st.sessions, 3, st.ts, {}, {}, menu) })
    }

    /** La master a tutto schermo: la sessione che guida le altre, con l'esito e i prossimi passi come tasti. */
    @Test fun s4Master() = shot {
        SessionSheet(master, st.ts, emptyList(), 120, none, grid = true,
            home = { _, _ -> androidx.compose.material3.Text("", Modifier.padding(16.dp)) },
            homeOpen = false,
            appBar = { PageHeader(null, st.sessions, 3, st.ts, {}, {}, menu) },
            bar = { MasterDock(master, MasterHome.hero(masterEntries, master), {}, {}, expanded = true, screen = true, trailing = { AppMenu(menu) }) },
            feed = ChatFeed.merge(masterEntries, emptyList()))
    }
}
