import { describe, expect, it } from 'vitest'
import type { HostStatus, Session, Suggestion, SystemStatus } from './contract'
import type { InProgress, Line } from './lavori'
import { lavoriInfo, pluginsInfo, suggestionInfo, systemInfo } from './slotInfo'

const sys = (load = .42, level = 'green') => ({ at: 1, cores: 8, load1: load * 8, load_per_core: load, mem_total_mb: 8000, mem_available_mb: 5040, level, load_level: level, mem_level: 'green' }) as unknown as SystemStatus
const ses = (state: string, name = 'x') => ({ state, name }) as unknown as Session
const host = (name: string, up: boolean, used: number | null = null, total: number | null = null) => ({ name, up, ram_used: used, ram_total: total }) as unknown as HostStatus
const line = (short: string, since: number | null, n = 1) => ({ short, since, works: Array(n).fill({}), n }) as unknown as Line
const ip = (you: Line[], total: number) => ({ working: [], you, others: [], axis: {}, total }) as unknown as InProgress

describe('slot_info', () => {
  it('slot_info: Sistema, carico, RAM e macchine', () => {
    const i = systemInfo(sys(), [host('win', true, 16e9, 40e9)], [ses('idle', 'master'), ses('busy'), ses('idle'), ses('gone')])
    expect(i).toEqual({ pct: 42, barPct: 42, level: 'green', ramPct: 37, hosts: [{ name: 'win', ramPct: 40 }], sessions: 2 })
  })
  it('slot_info: oltre il cento la barra si ferma', () => {
    expect(systemInfo(sys(1.6, 'red'), [], [])).toMatchObject({ pct: 160, barPct: 100, level: 'red' })
  })
  it('slot_info: macchina spenta al posto della percentuale', () => {
    expect(systemInfo(sys(), [host('win', false, 16e9, 40e9), host('nuc', true)], []).hosts).toEqual([{ name: 'win', ramPct: null }])
  })
  it('slot_info: Lavori coi numeri della sezione e ultimo per te', () => {
    expect(lavoriInfo(ip([line('Vecchio', 100), line('Logo ambra', 900, 2)], 5))).toEqual({ you: 3, total: 5, youPct: 60, last: 'Logo ambra', lastSince: 900 })
  })
  it('slot_info: Lavori se nessuno aspetta Franz', () => {
    expect(lavoriInfo(ip([], 0))).toEqual({ you: 0, total: 0, youPct: 0, last: null, lastSince: null })
  })
  it('slot_info: Suggerimenti, la prima della card', () => {
    expect(suggestionInfo([{ text: 'prima' }, { text: 'seconda' }] as Suggestion[])).toBe('prima')
    expect(suggestionInfo([])).toBeNull()
  })
  it('slot_info: Plugin, nomi o ora del controllo', () => {
    expect(pluginsInfo({ checked_at: 1600, accounts: [{ account: 'a', updates: ['alfa@m', 'beta@m'] }, { account: 'b', updates: ['alfa@m'] }] })).toEqual({ names: ['alfa', 'beta'], checkedAt: null })
    expect(pluginsInfo({ checked_at: 1600, accounts: [{ account: 'a', updates: [] }] })).toEqual({ names: [], checkedAt: 1600 })
  })
})
