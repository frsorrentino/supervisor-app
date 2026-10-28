import { describe, expect, it } from 'vitest'
import type { Session } from './contract'
import type { Row } from './summary'
import { candidates, closable, readPreview } from './closeFinished'

const now = 100_000
const row = (name: string, group: Row['group'], p: Partial<Session> = {}): Row =>
  ({ group, session: { id: name, name, account: 'p', project: name, state: 'idle', since: now - 7200, ...p } as Session, text: null, at: null, approvals: [] })

describe('Chiudi le finite (contratto 1.84, B4)', () => {
  it('candidate: le ferme senza finestra al PC da almeno un\'ora', () => {
    const rows = [row('curriculum', 'still'), row('officina', 'still', { attached: true }), row('fresca', 'still', { since: now - 600 }), row('blog', 'waiting'), row('app', 'working', { state: 'busy' })]
    expect(candidates(rows, now).map(s => s.name)).toEqual(['curriculum'])
  })
  it("l'anteprima: cosa si salva, chi non si chiude e perché", () => {
    const p = readPreview(JSON.stringify({ sessions: [
      { name: 'curriculum', closable: true, blocked_by: null, save: { outcome: 'CV inviato', next: 'Aspettare', session_id: '3f2a', dirty_n: 2, dirty: ['cv.docx', 'note.md'] } },
      { name: 'app', closable: false, blocked_by: 'busy' },
    ] }))
    expect(p?.sessions[0].save?.dirty_n).toBe(2)
    expect(closable(p!)).toEqual(['curriculum'])
    expect(readPreview('rotto')).toBeNull()
  })
})
