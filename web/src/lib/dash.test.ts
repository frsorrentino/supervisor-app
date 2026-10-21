import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import type { State } from './contract'
import { dashColumns, hostNumbers, publishedRows, recentProjects, sessionsByState, waitingFor } from './dash'

const fixture = (n: string): State => JSON.parse(readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8'))
const st = fixture('state-1-question.json')

describe('il cruscotto a tre colonne (Franz, 21/10 13:02)', () => {
  it('3 colonne sopra i 1400 px, 2 da 1000, una sotto', () => {
    expect([1440, 1400, 1399, 1200, 1000, 999, 900].map(dashColumns)).toEqual([3, 3, 2, 2, 2, 1, 1])
  })
  it('«Ti aspettano»: la più vecchia prima, con da quanto', () => {
    const w = waitingFor([...(st.attention ?? []), { kind: 'needs_you', session: 'field-notes', title: 'login', since: st.ts - 3600 }], st.ts)
    expect(w.map(x => x.session)).toEqual(['field-notes', 'ledger-api'])
    expect(w[0].waitedS).toBe(3600); expect(w[1].waitedS).toBe(300)
    expect(waitingFor(null, st.ts)).toEqual([])
  })
  it('le sessioni per stato, senza la master, con le chiuse di recente', () => {
    const b = sessionsByState([...st.sessions, { ...st.sessions[1], name: 'master' }])
    expect([b.busy, b.waiting, b.idle]).toEqual([1, 1, 1])
    expect(b.closed.map(s => s.name)).toEqual(['orbit-docs'])
  })
  it('il lancio rapido: progetti recenti senza una sessione viva', () => {
    // atlas-shop e ledger-api hanno una sessione viva; orbit-docs è chiusa.
    expect(recentProjects(st).map(p => p.name)).toEqual(['orbit-docs'])
    expect(recentProjects({ ...st, sessions: [] }).map(p => p.name)).toEqual(['atlas-shop', 'ledger-api', 'orbit-docs'])
  })
  it('PC Windows (1.75): i numeri di un host acceso, niente pezzi null', () => {
    const win = fixture('state-2-idle.json').hosts_status[0]
    expect(hostNumbers(win)).toBe('CPU 42 % · RAM 9,1 di 15,9 GB · disco 88,3 GB liberi di 271')
    expect(hostNumbers({ ...win, disk_free: null, disk_total: null })).toBe('CPU 42 % · RAM 9,1 di 15,9 GB')
    expect(hostNumbers(fixture('state-3-stale.json').hosts_status[0])).toBe('')
  })
  it('pubblicazioni di oggi (1.75): l\'ordine del relay, col nome corto del progetto', () => {
    const rows = publishedRows(fixture('state-2-idle.json').published_today)
    expect(rows.map(r => [r.kind, r.name, r.text])).toEqual([
      ['deploy', 'orbit-docs', 'hosting orbit-docs'], ['release', 'atlas-shop', '2.3.1'], ['push', 'atlas-shop', 'origin/feature/checkout'],
    ])
    expect(publishedRows(undefined)).toEqual([])
  })
})
