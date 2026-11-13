package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.ChatFeed
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * La chat a scheda unica (app-chat-scheda-unica-13-11, Franz 13/11 09:32: E1 a quattro zone, separate come in F3) col turno dei
 * mockup: la richiesta a fumetto a destra, poi una scheda sola con il lavoro (le frasi al loro posto, un passo per riga), la risposta
 * coi suoi titoli e l'indice, il piede con Esito, Tipo e Prossimi. Telefono e colonna del tablet, giorno e notte, schermo stretto coi
 * caratteri grandi; un turno ancora al lavoro con un passo fallito. Golden da CI.
 */
class ChatSchedaScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 5200), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val t = st.ts - 600
    private val diff = ContractJson.decodeChanges(File("../contract/changes-sample.json").readText()).files[0].let { StepDiff("/home/dev/payments-api/src/refunds/partial.ts", "edit", 14, 3, it.hunks) }
    private val answer = """Il rimborso parziale ora passa su staging: ho corretto l'arrotondamento dei centesimi e aggiunto i test che mancavano. Il deploy della 2.8.1 parte solo con il tuo ok.

## Cosa è cambiato
- `src/refunds/partial.ts`: l'importo si calcola in centesimi interi, così un rimborso in tre rate non perde più 0,01 €.
- `src/webhooks/stripe.ts`: l'evento `charge.refunded` aggiorna l'ordine una volta sola, anche quando Stripe lo manda due volte.
- 18 test nuovi in `tests/refunds.test.ts`, tutti verdi.

## Come l'ho provato
1. `npm run test` in locale: 232 test verdi.
2. `npm run test:staging`: rimborsi, webhook e checkout passano.
3. Un rimborso vero di 12,34 € sulla carta di prova: arriva in 4 secondi e l'ordine passa a «rimborsato».

Per il deploy, quando dici:
```
npm run deploy -- --env production --tag 2.8.1
```

## Cosa resta
**Non provato:** il rimborso su una carta americana, serve la sandbox US.

Esito: rimborso parziale corretto, 232 test verdi, la 2.8.1 è pronta.
Tipo: pronto
Prossimi: ok al deploy della 2.8.1 · prova il rimborso dal vivo · apri la PR"""
    private val turn = listOf(
        TranscriptEntry("u1", "user", text = "Il rimborso parziale perde un centesimo: sistemalo e prova su staging", at = t, origin = "phone"),
        TranscriptEntry("a1", "assistant", text = "Guardo come si calcola l'importo del rimborso parziale.", at = t + 5),
        TranscriptEntry("a2", "tool", text = "/home/dev/payments-api/src/refunds/partial.ts", at = t + 10, tool = "Read"),
        TranscriptEntry("a3", "assistant", text = "L'importo si divide in euro con i decimali: 12,34 € in tre rate fa 4,1133 € a rata, e l'arrotondamento di ogni rata perde un centesimo. Passo ai centesimi interi e do il resto all'ultima rata.", at = t + 20),
        TranscriptEntry("a4", "tool", text = "/home/dev/payments-api/src/refunds/partial.ts", at = t + 30, tool = "Edit", diff = diff),
        TranscriptEntry("a5", "tool", text = "npm run test:staging", at = t + 50, tool = "Bash", note = "Prova su staging"),
        TranscriptEntry("a6", "assistant", text = answer, at = t + 82, turn = TranscriptTurn(started = t, ended = t + 82, out = 640), outcomeType = "done"),
    )
    // Un turno ancora al lavoro: nessuna risposta, un passo fallito, il lavoro lungo chiuso sulle ultime voci.
    private val working = listOf(TranscriptEntry("w0", "user", text = "Prepara il deploy della 2.8.1 e aspetta il mio ok", at = t + 100, origin = "pc")) +
        List(8) { i -> TranscriptEntry("w${i + 1}", "tool", text = if (i == 6) "grep -rn constructEvent src/" else "src/webhooks/step$i.ts", at = t + 110 + i * 5L, tool = if (i == 6) "Bash" else "Read", error = i == 6) } +
        TranscriptEntry("w9", "assistant", text = "La verifica della firma stava in due posti: la sposto nel gestore.", at = t + 160) +
        TranscriptEntry("w10", "tool", text = "/home/dev/payments-api/src/webhooks/stripe.ts", at = t + 170, tool = "Edit", diff = diff.copy(path = "/home/dev/payments-api/src/webhooks/stripe.ts", added = 6, removed = 9))

    @After fun night() { V3.day = false }

    @androidx.compose.runtime.Composable
    private fun shot(entries: List<TranscriptEntry> = turn, day: Boolean = false) {
        val s = st.sessions.first { it.question == null }
        CmPhoneTheme(still = true, day = day) { SessionSheet(s, st.ts, emptyList(), 120, none, feed = ChatFeed.merge(entries, emptyList())) }
    }

    @Test fun turn() = paparazzi.snapshot { shot() }
    @Test fun turnDay() = paparazzi.snapshot { shot(day = true) }
    @Test fun working() = paparazzi.snapshot { shot(turn + working) }
    @Test fun turn320Font13() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), fontScale = 1.3f, screenHeight = 7000))
        paparazzi.snapshot { shot() }
    }
    // Nella colonna larga del tablet.
    @Test fun tablet() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 1640, screenHeight = 3400, density = com.android.resources.Density.XHIGH))
        paparazzi.snapshot { shot() }
    }
    @Test fun tabletDay() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 1640, screenHeight = 3400, density = com.android.resources.Density.XHIGH))
        paparazzi.snapshot { shot(day = true) }
    }
}
