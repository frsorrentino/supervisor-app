package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.ui.graphics.Color

/**
 * I nomi vecchi dei colori (CmColors, che resta per orologio e widget) sui token v3, come web/src/v3.css: ogni schermata del
 * telefono prende così notte e giorno dallo stesso punto. Il colore appartiene allo stato; struttura e azioni neutre.
 */
internal object Cm {
    val bg get() = V3.Bg; val surfaceLow get() = V3.S1; val surface get() = V3.S2; val surfaceHigh get() = V3.S3; val line get() = V3.Line
    val text get() = V3.Tx; val text2 get() = V3.Tx2; val stale get() = V3.Tx3; val stepsDot get() = V3.Tx3
    val primary get() = V3.Act; val onPrimary get() = V3.OnAct; val accent get() = V3.Busy; val actionIcon get() = V3.Tx2
    val waiting get() = V3.Wait; val busy get() = V3.Busy; val idle get() = V3.Good; val gone get() = V3.Red; val goneDim get() = V3.Red
    val advice get() = V3.Tx2; val prod get() = V3.Red; val modelOpus get() = V3.Tx2
    val briefCard get() = V3.S1; val briefLabel get() = V3.Tx2; val briefBig get() = V3.Tx; val briefSecondary get() = V3.Tx2; val briefRing get() = V3.Busy; val briefTrack get() = V3.S3
    val briefGood get() = V3.Good; val briefGoodInk get() = V3.OnAct; val briefWarn get() = V3.Wait; val briefWarnInk get() = V3.BannerInk; val briefAlert get() = V3.Red; val briefAlertRing get() = V3.Red; val briefWeek get() = V3.Tx2
}
