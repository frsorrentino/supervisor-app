package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Features

/**
 * «Cosa sa fare» (Franz 01/11 20:39): tutte le funzioni, per a cosa servono. Ogni voce apre la sua pagina (22:05: tornare alla home non spiegava
 * niente): cosa fa e quando serve, come si usa, e «Portami lì» se ha un posto suo.
 */
@Composable
fun FeaturesScreen(catalog: Features.Catalog, onBack: () -> Unit, startOn: String? = null, demo: FeatureDemoData? = null, onRoute: (String) -> Unit) {
    val italian = LocalConfiguration.current.locales[0].language == "it"
    var openKey by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(startOn) }
    val opened = catalog.features.firstOrNull { it.key == openKey }
    if (opened != null) {
        androidx.activity.compose.BackHandler { openKey = null }
        FeaturePage(opened, italian, demo, onBack = { openKey = null }, onRoute = onRoute)
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().testTag("funzioni"), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 28.dp)) {
        item("bar") {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Cm.text) }
                Text(stringResource(R.string.features_title), style = MaterialTheme.typography.titleLarge, color = Cm.text)
            }
        }
        Features.grouped(catalog).forEach { (g, rows) ->
            item("g-${g.key}") {
                Text(if (italian) g.it else g.en, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Cm.text2, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp))
            }
            item("c-${g.key}") {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow)) {
                    rows.forEachIndexed { i, f ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Cm.line))
                        Row(
                            Modifier.fillMaxWidth().handCursor().clickable { openKey = f.key }.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(f.name.of(italian), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text, modifier = Modifier.weight(1f, fill = false))
                                    if (f.state == Features.COMING) Text(stringResource(R.string.features_coming), style = MonoSmall, color = Cm.waiting, maxLines = 1)
                                }
                                Text(f.what.of(italian), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
                                f.where.of(italian).takeIf { it.isNotEmpty() }?.let { Text(it, style = MonoSmall) }
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = Cm.text2, modifier = Modifier.padding(start = 8.dp).size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/** La pagina di una voce. */
@Composable
private fun FeaturePage(f: Features.Feature, italian: Boolean, demo: FeatureDemoData?, onBack: () -> Unit, onRoute: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().verticalScroll(androidx.compose.foundation.rememberScrollState()).testTag("funzione-${f.key}").padding(start = 12.dp, end = 12.dp, bottom = 28.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Cm.text) }
            Text(f.name.of(italian), style = MaterialTheme.typography.titleLarge, color = Cm.text, modifier = Modifier.weight(1f))
            if (f.state == Features.COMING) Text(stringResource(R.string.features_coming), style = MonoSmall, color = Cm.waiting, maxLines = 1, modifier = Modifier.padding(end = 8.dp))
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(f.what.of(italian) + " " + f.`when`.of(italian), style = MaterialTheme.typography.bodyLarge, color = Cm.text)
            if (demo != null && f.key in Features.DEMO_KEYS) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.features_try).uppercase(), style = MonoSmall.copy(fontWeight = FontWeight.Medium), color = Cm.text2)
                FeatureDemoBox(f.key, demo)
            }
            f.how.of(italian).takeIf { it.isNotEmpty() }?.let { how ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.features_how).uppercase(), style = MonoSmall.copy(fontWeight = FontWeight.Medium), color = Cm.text2)
                    Text(how, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                }
            }
            f.where.of(italian).takeIf { it.isNotEmpty() }?.let { Text(it, style = MonoSmall) }
            if (Features.canGo(f)) androidx.compose.material3.FilledTonalButton(onClick = { onRoute(f.route!!) }, modifier = Modifier.handCursor()) { Text(stringResource(R.string.features_go)) }
        }
    }
}
