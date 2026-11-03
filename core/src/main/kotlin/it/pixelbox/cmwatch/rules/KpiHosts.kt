package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.KpiDisk
import it.pixelbox.cmwatch.contract.KpiHealth
import it.pixelbox.cmwatch.contract.KpiHost

/**
 * I riquadri delle macchine in Sistema (app-kpi-win-disco-03-11, contratto 1.110): una macchina compare solo mentre lavora; a
 * riposo, al suo posto, lo spazio libero sul disco di questo computer. Con un relay precedente (`working` assente) restano tutte.
 */
object KpiHosts {
    fun shown(h: KpiHealth): List<KpiHost> = h.hosts.filter { it.working != false }

    /** Il disco al posto delle macchine a riposo; null se nessuna è a riposo o il disco non si legge. */
    fun disk(h: KpiHealth): KpiDisk? = h.localDisk?.takeIf { h.hosts.any { it.working == false } && it.total > 0 }
}
