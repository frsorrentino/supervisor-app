// Gli stessi casi di KpiHostsTest in Kotlin, sul `kpi` della fixture: win al lavoro, disco locale 20 GiB liberi di 58.
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { KpiHealth } from './contract'
import { restingDisk, shownHosts } from './kpiHosts'

const root = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8'))
const id = root.cmd.find((c: { op: string }) => c.op === 'kpi').id
const h: KpiHealth = JSON.parse(root.result.find((r: { id: string }) => r.id === id).text).kpi_health

describe('macchine in Sistema (1.110)', () => {
  it('una macchina al lavoro resta e il disco aspetta', () => {
    expect(h.hosts[0].working_why).toEqual(['session'])
    expect(shownHosts(h).map(x => x.name)).toEqual(['win'])
    expect(restingDisk(h)).toBeNull()
  })
  it('a riposo lascia il posto al disco', () => {
    const rest = { ...h, hosts: h.hosts.map(x => ({ ...x, working: false, working_why: [] })) }
    expect(shownHosts(rest)).toEqual([])
    expect(restingDisk(rest)?.free).toBe(21474836480)
    expect(restingDisk({ ...rest, local_disk: null })).toBeNull()
  })
  it('con un relay precedente restano tutte', () => {
    const old = { ...h, hosts: h.hosts.map(x => ({ ...x, working: undefined })), local_disk: undefined }
    expect(shownHosts(old).map(x => x.name)).toEqual(['win'])
    expect(restingDisk(old)).toBeNull()
  })
})
