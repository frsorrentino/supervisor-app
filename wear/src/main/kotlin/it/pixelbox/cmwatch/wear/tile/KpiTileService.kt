package it.pixelbox.cmwatch.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.PrimaryLayoutMargins
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.TimelinePage
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.MainActivity
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Proposta 15 (25/10, compito app-kpi-medie-25-10): la tile «Ultime 6 ore», a parte dalla tile delle sessioni. Tre righe,
 * le sessioni più attive; sei caselle, una per ora: blu al lavoro, ambra ti aspetta (solo l'ora in corso, dallo stato),
 * grigio ferma. La cronologia la chiede l'orologio stesso, al più ogni 15 minuti, e resta in memoria fra un disegno e
 * l'altro: la tile si disegna subito con quella che c'è.
 */
open class KpiTileService : TileService() {
    private val scheme = ColorScheme(
        primary = 0xFFFFFFFF.toInt().argb, onPrimary = 0xFF0B0C0E.toInt().argb,
        surfaceContainer = 0xFF1B1D22.toInt().argb, onSurface = 0xFFEDEEF0.toInt().argb, onSurfaceVariant = 0xFFACB1B9.toInt().argb,
        background = 0xFF000000.toInt().argb, onBackground = 0xFFFFFFFF.toInt().argb,
    )
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            val app = application as CmApp
            scope.launch {
                try {
                    if (app.repo.snapshot.value.state == null) kotlinx.coroutines.withTimeoutOrNull(800) { app.repo.snapshot.first { it.state != null } }
                    val state = app.repo.snapshot.value.state
                    val now = System.currentTimeMillis() / 1000
                    completer.set(
                        TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES)
                            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root(requestParams.deviceConfiguration, state, cache, now)))
                            .setFreshnessIntervalMillis(FETCH_EVERY_S * 1000)
                            .build()
                    )
                    if (state?.ops?.contains("timeline") == true && now - cachedAt >= FETCH_EVERY_S && fetching.compareAndSet(false, true)) {
                        try {
                            val id = app.repo.command(CmdOp.TIMELINE, null, "${Kpi.HOURS}h")
                            val r = kotlinx.coroutines.withTimeoutOrNull(20_000) { app.repo.resultsById.first { it.containsKey(id) }[id] }
                            app.repo.forget(id)
                            if (r?.ok == true) runCatching { ContractJson.decodeTimeline(r.text) }.onSuccess { p -> cache = p; cachedAt = now; requestUpdate(app) }
                        } finally { fetching.set(false) }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    completer.setCancelled(); throw e
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
            "kpi-tile"
        }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Il layout dai dati, senza repository: tre righe, il titolo e, sotto, chi ti aspetta adesso. */
    internal fun root(device: DeviceParametersBuilders.DeviceParameters, state: State?, page: TimelinePage?, now: Long): LayoutElement {
        val rows = Kpi.tileRows(page, state, now)
        val waiting = state?.sessions.orEmpty().firstOrNull { Kpi.waitingSince(it) != null }?.name
        return materialScope(this, device, allowDynamicTheme = false, defaultColorScheme = scheme) {
            primaryLayout(
                margins = PrimaryLayoutMargins.MIN_PRIMARY_LAYOUT_MARGIN,
                titleSlot = { text(getString(R.string.kpi15_title).layoutString, typography = Typography.LABEL_MEDIUM, color = colorScheme.onBackground, maxLines = 1) },
                mainSlot = { grid(rows, page == null) },
                bottomSlot = {
                    text((waiting?.let { getString(R.string.kpi15_waiting, it) } ?: getString(R.string.kpi15_none_waiting)).layoutString,
                        typography = Typography.BODY_SMALL, color = colorScheme.onSurfaceVariant, maxLines = 1)
                },
            )
        }
    }

    private fun MaterialScope.grid(rows: List<Kpi.TileRow>, loading: Boolean): LayoutElement {
        val col = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickable(launch("cmwatch://sessions"), id = "kpi-15")).build())
        if (rows.isEmpty()) {
            col.addContent(text(getString(if (loading) R.string.kpi15_loading else R.string.kpi15_quiet).layoutString, typography = Typography.BODY_MEDIUM, color = colorScheme.onSurface, maxLines = 2))
            return col.build()
        }
        rows.forEachIndexed { i, r ->
            if (i > 0) col.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())
            val row = LayoutElementBuilders.Row.Builder().setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            row.addContent(
                LayoutElementBuilders.Box.Builder().setWidth(dp(58f)).setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                    .addContent(text(r.name.substringBefore('-').take(8).layoutString, typography = Typography.BODY_SMALL, color = colorScheme.onSurface, maxLines = 1)).build()
            )
            r.cells.forEachIndexed { k, b ->
                if (k > 0) row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(4f)).build())
                row.addContent(cell(b))
            }
            col.addContent(row.build())
        }
        return col.build()
    }

    private fun cell(b: Kpi.Band): LayoutElement = LayoutElementBuilders.Box.Builder().setWidth(dp(14f)).setHeight(dp(14f))
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder().setBackground(
                ModifiersBuilders.Background.Builder().setColor(ColorBuilders.argb(when (b) { Kpi.Band.WORK -> WORK; Kpi.Band.WAIT -> WAIT; Kpi.Band.STILL -> STILL }))
                    .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(4f)).build()).build()
            ).build()
        ).build()

    private fun launch(uri: String): ActionBuilders.LaunchAction = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name)
                .addKeyToExtraMapping(MainActivity.EXTRA_URI, ActionBuilders.stringExtra(uri)).build()
        ).build()

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { c -> c.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES).build()); "kpi-res" }

    companion object {
        private const val RESOURCES = "kpi15-1"
        const val FETCH_EVERY_S = 15 * 60L
        private const val WORK = 0xFF5AA2FF.toInt()   // CmColors.busy
        private const val WAIT = 0xFFFFB23F.toInt()   // CmColors.waiting
        private const val STILL = 0xFF272A31.toInt()  // CmColors.briefTrack
        @Volatile private var cache: TimelinePage? = null
        @Volatile private var cachedAt = 0L
        private val fetching = java.util.concurrent.atomic.AtomicBoolean(false)
        fun requestUpdate(app: CmApp) = getUpdater(app).requestUpdate(KpiTileService::class.java)
    }
}
