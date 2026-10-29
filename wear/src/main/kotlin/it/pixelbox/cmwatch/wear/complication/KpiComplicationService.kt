package it.pixelbox.cmwatch.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.wear.CmApp

/**
 * Proposta 14 (25/10, compito app-kpi-piccole-25-10): una complicazione a parte da quella della quota (che resta com'era,
 * Franz 15-16/09). L'anello è la quota 5h dell'account scelto; al centro quante sessioni ti aspettano, «0» quando nessuna.
 * Il tocco apre l'app sulla prima domanda, o sulle sessioni.
 */
class KpiComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val preview = ContractJson.decodeState(assets.open("contract/state-1-question.json").bufferedReader().readText())
        return build(type, preview, "personale")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val app = application as CmApp
        return build(request.complicationType, app.repo.snapshot.value.state, app.prefs.current().complicationAccount)
    }

    private fun build(type: ComplicationType, state: State?, chosen: String): ComplicationData? {
        if (type != ComplicationType.RANGED_VALUE) return null
        val account = state?.let { st -> it.pixelbox.cmwatch.rules.Accounts.resolve(st, chosen) } ?: chosen
        val r = Kpi.ring(state, account)
        val desc = getString(R.string.kpi14_desc, r.waiting, r.quota?.let { "$it%" } ?: "—")
        return RangedValueComplicationData.Builder((r.quota ?: 0).toFloat(), 0f, 100f, PlainComplicationText.Builder(desc).build())
            .setText(PlainComplicationText.Builder(r.waiting.toString()).build())
            .setTitle(PlainComplicationText.Builder(getString(R.string.kpi14_title)).build())
            .setMonochromaticImage(MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_app_mono)).build())
            .setTapAction(open(it.pixelbox.cmwatch.rules.ComplicationTexts.tapTarget(state)))
            .build()
    }

    private fun open(uri: String): PendingIntent =
        PendingIntent.getActivity(this, 14, Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    companion object {
        /** L'identificatore stabile della proposta 14, per il controllo. */
        const val KPI = "kpi-14"
    }
}
