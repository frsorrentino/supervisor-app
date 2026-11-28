import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { allegatiTarget, MAX_ALLEGATI } from './allegatiCarta'
import type { Commitment, Session } from './contract'
import { proveDemo, waitsDemo, world } from './demo'
import { decodeWaits } from './decideDecode'

// app-allegati-carta-27-11: a chi vanno gli allegati della risposta di una carta (Ti aspetta, Banco prova). Gli stessi casi di
// AllegatiCartaTest in Kotlin, dallo stesso file.
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/allegati-carta-cases.json', import.meta.url), 'utf8')) as {
  sessions: Session[]; commitments: Commitment[]; cases: { name: string; session: string | null; task: string | null; out: string }[]
}
const st = { sessions: file.sessions, commitments: file.commitments }

describe('allegati della risposta di una carta', () => {
  for (const c of file.cases) it(c.name, () => expect(allegatiTarget(c.session, c.task, st)).toBe(c.out))

  it('senza stato vanno alla master', () => expect(allegatiTarget('alfa', 'app-x', null)).toBe('master'))

  it('la carta di prova della fixture del relay va alla sua sessione, se è viva', () => {
    const raw = JSON.parse(readFileSync(new URL('../../../contract/attese-rd-sample.json', import.meta.url), 'utf8')) as { attese: unknown }
    const items = decodeWaits(JSON.stringify(raw.attese)).items
    const prova = items.find(w => w.kind === 'prova')!
    expect(prova.session).toBe('win:alfa')
    const live = { ...st, sessions: [...st.sessions, { ...st.sessions[0], id: 's9', name: 'win:alfa' }] }
    expect(allegatiTarget(prova.session, prova.task, live)).toBe('win:alfa')
    // La raccolta non ha né sessione né compito: la master.
    const raccolta = items.find(w => w.kind === 'raccolta')!
    expect(allegatiTarget(raccolta.session, raccolta.task, live)).toBe('master')
  })

  it('nella demo la carta di prova e la prova 1 vanno a storefront, la prova 2 ad admin-panel', () => {
    const now = 1_800_000_000, w = world(now).state
    const carta = waitsDemo(now).find(x => x.kind === 'prova')!
    expect(allegatiTarget(carta.session, carta.task, w)).toBe('storefront')
    const [p1, p2] = proveDemo(now).groups[0].prove
    // Le prove della demo non portano la sessione: la dà il registro (demo-cart è di storefront, demo-users di admin-panel).
    expect(allegatiTarget(p1.session, p1.task, w)).toBe('storefront')
    expect(allegatiTarget(p2.session, p2.task, w)).toBe('admin-panel')
    // La copertina di blog, se blog si chiudesse: la master.
    const cover = waitsDemo(now).find(x => x.task === 'demo-cover')!
    const closed = { ...w, sessions: w.sessions.map(s => (s.name === 'blog' ? { ...s, state: 'gone' as const } : s)) }
    expect(allegatiTarget(cover.session, cover.task, closed)).toBe('master')
  })

  it('lo stesso tetto del campo delle sessioni', () => expect(MAX_ALLEGATI).toBe(5))
})
