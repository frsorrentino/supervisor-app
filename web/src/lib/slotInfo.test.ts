import { describe, expect, it } from 'vitest'
import type { HostStatus, Session, Suggestion, SystemStatus } from './contract'
import type { InProgress, Line } from './lavori'
import { fitNames, lavoriInfo, pluginsInfo, suggestionNames, systemInfo } from './slotInfo'

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
  const sg = (text: string, kind = 'idea') => ({ text, kind }) as Suggestion
  const kinds = (k: string) => `tipo-${k}`
  const len = (x: string) => x.length
  it('slot_suggerimenti: il nome prima dei due punti o l\'etichetta del tipo', () => {
    expect(suggestionNames([sg('Atlas Shop: rinnova il dominio')], kinds)).toEqual(['Atlas Shop'])
    expect(suggestionNames([sg('Senza due punti', 'cleanup')], kinds)).toEqual(['tipo-cleanup'])
    expect(suggestionNames([sg(': vuoto prima')], kinds)).toEqual(['tipo-idea'])
  })
  it('slot_suggerimenti: prefisso di trenta sì, di trentuno no', () => {
    expect(suggestionNames([sg('a'.repeat(30) + ': x')], kinds)).toEqual(['a'.repeat(30)])
    expect(suggestionNames([sg('a'.repeat(31) + ': x')], kinds)).toEqual(['tipo-idea'])
  })
  it('slot_suggerimenti: ordine della card e nomi ripetuti una volta', () => {
    expect(suggestionNames([sg('B: 1'), sg('A: 2'), sg('B: 3'), sg('senza'), sg('altra')], kinds)).toEqual(['B', 'A', 'tipo-idea'])
  })
  it('slot_suggerimenti: nomi interi, poi +N', () => {
    const n = ['Alfa', 'Beta', 'Gamma']
    expect(fitNames(n, 19, len)).toBe('Alfa · Beta · Gamma')
    expect(fitNames(n, 18, len)).toBe('Alfa · Beta · +1')
    expect(fitNames(n, 9, len)).toBe('Alfa · +2')
  })
  it('slot_suggerimenti: senza posto neppure al primo, solo +N', () => {
    expect(fitNames(['Alfa', 'Beta', 'Gamma'], 8, len)).toBe('+3')
    expect(fitNames(['Alfa'], 3, len)).toBe('+1')
    expect(fitNames([], 100, len)).toBeNull()
  })
  it('slot_info: Plugin, nomi o ora del controllo', () => {
    expect(pluginsInfo({ checked_at: 1600, accounts: [{ account: 'a', updates: ['alfa@m', 'beta@m'] }, { account: 'b', updates: ['alfa@m'] }] })).toEqual({ names: ['alfa', 'beta'], checkedAt: null })
    expect(pluginsInfo({ checked_at: 1600, accounts: [{ account: 'a', updates: [] }] })).toEqual({ names: [], checkedAt: 1600 })
  })
})
