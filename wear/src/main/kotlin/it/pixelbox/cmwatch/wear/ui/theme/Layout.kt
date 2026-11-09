package it.pixelbox.cmwatch.wear.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.TransformationVariableSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * Testi, intestazioni e righe senza superficie si deformano come le card (S07): vicino al bordo tondo la libreria li
 * stringe invece di lasciarli tagliare dal cerchio. I margini delle liste sono quelli di default di `ScreenScaffold`
 * (5,2 % ai lati, 10 % sopra e sotto, più lo spazio del bottone curvo), non più percentuali calcolate da noi.
 * Dentro, il margine interno delle card della libreria: il testo nudo parte dove parte il testo delle card, non dal
 * loro bordo (negli snapshot del 15/09 00:50 le ultime righe di Esito e Timeline perdevano ancora la prima lettera).
 * In più il 6 % della larghezza per lato (app-wear-play-rifiuto-09-11): il testo nudo non si stringe come le superfici, e vicino
 * al bordo in alto o in basso le righe lunghe di un paragrafo uscivano dal cerchio («Jn your phone» nelle prove di Google).
 */
@Composable
fun Modifier.morph(scope: TransformingLazyColumnItemScope, spec: TransformationSpec): Modifier = with(scope) {
    val dir = LocalLayoutDirection.current
    val side = (LocalConfiguration.current.screenWidthDp * 0.06f).dp
    this@morph.transformedHeight(scope, spec)
        .graphicsLayer { with(spec) { applyContainerTransformation(scrollProgress) } }
        .padding(start = CardDefaults.ContentPadding.calculateStartPadding(dir) + side, end = CardDefaults.ContentPadding.calculateEndPadding(dir) + side)
}

/**
 * Le deformazioni delle liste con le voci che spariscono del tutto ai bordi (app-wear-play-rifiuto-09-11): Google Play ha rifiutato
 * l'app il 03/11 per testi e tasti tagliati dal bordo tondo (Abbinamento e Impostazioni ferme in fondo, «Jn your phone», il titolo
 * sotto l'ora). Con le deformazioni di serie una voce al bordo resta a metà opaca e il cerchio la taglia; qui fondo e contenuto
 * vanno a zero mentre la voce esce, così al bordo non resta niente di tagliato. Il resto (scala, zone, curva) è quello di serie.
 */
// A zero finché il bordo della voce non ha passato metà della zona di passaggio: una voce che esce dall'alto è già sparita
// quando il cerchio comincerebbe a tagliarla.
private val Fade = TransformationVariableSpec(0f)

@Composable
fun rememberRoundSpec(): TransformationSpec = rememberTransformationSpec(
    ResponsiveTransformationSpec.smallScreen(containerAlpha = Fade, contentAlpha = Fade),
    ResponsiveTransformationSpec.largeScreen(containerAlpha = Fade, contentAlpha = Fade),
)

/**
 * I margini delle liste: ai lati e in cima quelli di `ScreenScaffold` (5,2 % e 10 %), in fondo il 25 % dell'altezza invece del 10 %
 * (app-wear-play-rifiuto-09-11). Col 10 % l'ultima voce, a lista ferma in fondo, restava nella zona in cui si stringe e il cerchio
 * ne tagliava gli angoli in basso («Try the demo» e «Demo mode» nelle prove di Google). Una lista che finisce con una riga di
 * testo corta (Quota) ne vuole meno: con tanto spazio sotto, la card alta che la precede saliva fin sotto il bordo in alto.
 */
@Composable
fun roundListPadding(bottom: Float = 0.25f): PaddingValues {
    val c = LocalConfiguration.current
    return PaddingValues(start = (c.screenWidthDp * 0.052f).dp, end = (c.screenWidthDp * 0.052f).dp, top = (c.screenHeightDp * 0.10f).dp, bottom = (c.screenHeightDp * bottom).dp)
}
