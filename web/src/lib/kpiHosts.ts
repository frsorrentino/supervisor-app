// I riquadri delle macchine in Sistema (app-kpi-win-disco-03-11, contratto 1.110), come KpiHosts in Kotlin: una macchina compare
// solo mentre lavora; a riposo, al suo posto, lo spazio libero sul disco di questo computer. Con un relay precedente restano tutte.
import type { KpiHealth, KpiHost } from './contract'

export const shownHosts = (h: KpiHealth): KpiHost[] => h.hosts.filter(x => x.working !== false)

/** Il disco al posto delle macchine a riposo; null se nessuna è a riposo o il disco non si legge. */
export const restingDisk = (h: KpiHealth): { free: number; total: number } | null =>
  h.local_disk && h.local_disk.total > 0 && h.hosts.some(x => x.working === false) ? h.local_disk : null
