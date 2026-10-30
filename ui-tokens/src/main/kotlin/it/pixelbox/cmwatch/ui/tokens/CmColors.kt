package it.pixelbox.cmwatch.ui.tokens

import androidx.compose.ui.graphics.Color

/** Token del design (sezione 3): tema scuro unico, cobalto solo su bottone pieno, scroll bar, chip «seguita», anello quota. */
// Dal 30/10 (Tutti e quattro, Franz 23:31) i valori dello stile v3 per orologio, tile, widget e notifiche; il telefono usa mobile/ui/Cm.kt. bg resta nero puro: sull'OLED del polso i pixel spenti non consumano.
object CmColors {
    val bg = Color(0xFF000000)
    /** Tre gradini di superficie, non uno: sfondo, riga, card. Senza il gradino la profondità non si vede. */
    val surfaceLow = Color(0xFF121316)
    val surface = Color(0xFF1B1D22)   // più chiaro (Franz, 12/09 20:03): come i tasti del selettore app di Wear OS
    val surfaceHigh = Color(0xFF272A31)   // card in stile brief, misurata sui suoi fotogrammi
    val line = Color(0xFF25272D)
    val text = Color(0xFFEDEEF0)
    // Più chiaro (proposta A7, Franz 15/09 23:28: «un testo che contrasti di più»): da #9AA3B2 a 10:1 sul nero, sempre
    // sotto il bianco del testo principale e sotto `briefSecondary`, così la gerarchia resta.
    val text2 = Color(0xFF969BA4)
    val accent = Color(0xFF5AA2FF)
    // Bottone principale come nelle app Google M3 Expressive e come il tasto di bordo della tile (Franz, 16/09 00:41:
    // «colore appropriato» al posto del cobalto): azzurro pastello con testo blu notte, 11:1. Il cobalto resta allo stato
    // «lavora» e ai fili, non ai bottoni.
    val primary = Color(0xFFFFFFFF)
    val onPrimary = Color(0xFF0B0C0E)
    val accentPressed = Color(0xFF5AA2FF)
    // Azzurro pastello delle icone delle azioni (proposta del 14/09, confermata da Franz il 15/09 10:42): il cobalto
    // resta ai bottoni pieni, sulle icone era un blu acceso.
    val actionIcon = Color(0xFF969BA4)
    val waiting = Color(0xFFFFB23F)
    /** Contratto 1.37: il consiglio di modello ed effort (puntino e «consigliato») e il bollino della produzione. */
    val advice = Color(0xFF969BA4)
    val prod = Color(0xFFFF5A5F)
    // Giallino della sessione seguita, «come se fosse accesa» (Franz, 14/09 16:27): più chiaro dell'ambra di attesa.
    val followed = Color(0xFFFFB23F)
    val busy = Color(0xFF5AA2FF)
    val idle = Color(0xFF34D399)
    val gone = Color(0xFFFF5A5F)
    val goneDim = Color(0xFFFF5A5F)   // rosso desaturato: una sessione chiusa non deve urlare
    // Il gruppo di passaggi (Franz, 02/10 07:38, variante c): fondo appena tinto, rossastro se un passaggio è fallito.
    val stepsBg = Color(0xFF121316)
    val stepsFailBg = Color(0xFF2A1618)
    val stepsDot = Color(0xFF858A93)
    // 5:1 sulla superficie: il grigio di prima stava a 3:1 e al sole spariva (review UX, 13/09).
    val stale = Color(0xFF858A93)
    val accountAgenzia = Color(0xFFE53935)
    val accountPersonale = Color(0xFF43A047)

    // Stile del «brief mattutino» di Wear OS, misurato sui fotogrammi della sua schermata (13/09 16:25).
    val briefCard = surfaceHigh
    val briefLabel = Color(0xFF969BA4)
    val briefBig = Color(0xFFEDEEF0)
    val briefSecondary = Color(0xFF969BA4)
    val briefRing = Color(0xFF5AA2FF)
    val briefTrack = Color(0xFF272A31)
    val briefChip = Color(0xFF272A31)
    val briefChipInk = Color(0xFFEDEEF0)
    val briefGood = Color(0xFF34D399)
    val briefGoodInk = Color(0xFF0B0C0E)
    val briefWarn = Color(0xFFFFB23F)
    val briefWarnInk = Color(0xFF0B0C0E)
    val briefAlert = Color(0xFFFF5A5F)
    val briefAlertInk = Color(0xFF0B0C0E)
    val briefAlertRing = Color(0xFFFF5A5F)

    // L'anello interno della quota, la settimana (Franz, 16/09 11:52): lavanda, vicino all'azzurro delle 5 ore ma
    // distinguibile a colpo d'occhio. Stesso colore sulla pillola «settimana», che così fa da legenda senza scritte.
    val briefWeek = Color(0xFF969BA4)
    val briefWeekInk = Color(0xFF0B0C0E)

    // Il pallino della pillola del modello (Franz, 16/09 08:29): la famiglia si riconosce dal colore prima del nome.
    // Opus prende il corallo di Claude, che è il colore del marchio; le altre due restano dentro la tavolozza del brief.
    val modelOpus = Color(0xFF969BA4)
    val modelSonnet = Color(0xFF5AA2FF)
    val modelHaiku = Color(0xFF34D399)
    val modelOther = Color(0xFF969BA4)

    // Widget della home, accordato al widget di ads-widget (Franz, 01/10 13:22): stessa tavolozza scura del suo tema.
    val widgetBg = Color(0xFF121316)
    val widgetText = Color(0xFFEDEEF0)
    val widgetText2 = Color(0xFF969BA4)
    val widgetAccent = Color(0xFF5AA2FF)
    val widgetChip = Color(0x0FFFFFFF)    // bianco al 6 %: la pastiglia di ↻
    val widgetTrack = Color(0x1AFFFFFF)   // bianco al 10 %: il binario degli archi e delle barre
}
