import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type Commitment } from './contract'
import { rows, tone, turnOf, yours } from './commitments'
import { STRONG } from './strongAlert'

// Gli stessi casi di CommitmentsTest in Kotlin (contratto 1.76, P2 e P4).
const fixture = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')
const idle = decodeState(fixture('state-2-idle.json'))
const stale = decodeState(fixture('state-3-stale.json'))
const night = JSON.parse(fixture('night-report-sample.json')) as { commitments: Commitment[] }

describe('impegni', () => {
  it('le fixture portano gli impegni della 1.76', () => {
    expect(idle.commitments!.map(c => c.task)).toEqual(['ledger-migrations-011', 'atlas-seo-pages'])
    const led = idle.commitments![0]
    expect(led).toMatchObject({ state: 'running', stalled: true, approved: true, idle_s: 13800, approved_at: 1789200600, approved_by: 'maintainer', outcome_kind: 'deploy', paused: null })
    expect(led.last_step).toEqual({ at: 1789200600, kind: 'approval', text: 'migrations on staging' })
    expect(led.waits).toEqual({ who: 'person', on: 'ledger-api' })
    expect(led.check).toEqual({ outcome: 'red', at: 1789199760 })
    expect(decodeState(fixture('state-1-question.json')).commitments).toEqual([])
    // Contratto 1.79: il titolo breve `short` (1-28 caratteri, una riga, mai «…»).
    expect(idle.commitments!.map(c => c.short)).toEqual(['Migrations 008-011', 'SEO pages of atlas-shop'])
    expect(night.commitments.map(c => c.short)).toEqual(['Release 1.4 di ledger-api', 'APK del Gantt sul telefono'])
    const paused = stale.commitments![0]
    expect(paused.paused).toEqual({ reason: "after the client's review", at: 1789100000, by: 'master' })
    expect(paused.expected).toBeNull()
    // 1.77: la pausa aspetta la persona e `paused.at` è l'inizio vero, prima del passo di pausa.
    expect([paused.waits!.on, paused.paused!.at]).toEqual(['person', 1789100000])
    const events = JSON.parse(fixture('events-sample.json')) as { kind: string; ref?: string; session?: string }[]
    const stalled = events.find(e => e.kind === 'task_stalled')!
    expect([stalled.ref, stalled.session]).toEqual(['ledger-migrations-011', 'ledger-api'])
    expect(STRONG.has('task_stalled')).toBe(true)
    expect(night.commitments.map(c => c.task)).toEqual(['ledger-release-14', 'notes-gantt-apk'])
  })

  it("le righe tengono l'ordine del relay con l'età di adesso", () => {
    const r = rows(idle, idle.ts)
    expect(r.map(x => x.c.task)).toEqual(['ledger-migrations-011', 'atlas-seo-pages'])
    expect(r.map(x => x.idle)).toEqual([13800, 600])
    expect(r.map(x => x.tone)).toEqual(['warn', 'normal'])
    expect(r.map(x => x.turn)).toEqual(['ledger-api', 'atlas-shop'])
    expect(r.some(x => x.yourTurn)).toBe(false)
    expect(r.every(x => x.approval === null)).toBe(true)
    // Cinque ore dopo la pubblicazione (published_at, non ts).
    const later = rows(idle, (idle.published_at ?? idle.ts) + 5 * 3600)
    expect(later.map(x => x.idle)).toEqual([13800 + 18000, 600 + 18000])
    expect(later.map(x => x.tone)).toEqual(['alert', 'warn'])
    expect(later[0].age).toBe(13800 + 18000)
  })

  it('un impegno in pausa non ha colore e va in fondo', () => {
    const row = rows(stale, stale.ts)[0]
    expect([row.tone, row.turn, row.yourTurn]).toEqual(['normal', null, true]) // 1.77: la pausa aspetta la persona
    expect(yours(stale)).toBe(0)
    const mixed = { ...idle, commitments: [...stale.commitments!, ...idle.commitments!] }
    expect(rows(mixed, idle.ts).map(x => x.c.task)).toEqual(['ledger-migrations-011', 'atlas-seo-pages', 'orbit-docs-site'])
    expect(tone(stale.commitments![0], 10 * 3600)).toBe('normal')
  })

  it("un ok del registro diventa il tasto Approva quando tocca all'utente", () => {
    const ok = { task: 'ledger-release-14', title: 'Release 1.4 di ledger-api', what: 'tag v1.4 e push', where: 'github.com/example/ledger-api', deploy: false, requested_at: 1791332460 }
    const st = { ...idle, commitments: night.commitments, approvals: [ok] }
    const r = rows(st, idle.ts)
    const release = r.find(x => x.c.task === 'ledger-release-14')!
    expect([release.yourTurn, release.turn, release.approval]).toEqual([true, null, ok])
    const gantt = r.find(x => x.c.task === 'notes-gantt-apk')!
    expect([gantt.yourTurn, gantt.turn, gantt.approval]).toEqual([false, 'field-notes', null])
    expect(yours(st)).toBe(1)
    expect(yours(idle)).toBe(0)
    expect(turnOf(night.commitments[0])).toBeNull()
  })

  it('con un relay vecchio niente righe', () => {
    expect(rows({ ...idle, commitments: null }, idle.ts)).toEqual([])
    expect(rows({ ...idle, commitments: undefined }, idle.ts)).toEqual([])
    expect(yours({ ...idle, commitments: null })).toBe(0)
  })
})
