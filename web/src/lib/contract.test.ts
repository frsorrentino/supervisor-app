import { describe, expect, it } from 'vitest'
import { readPreview } from './closeFinished'
import { readFileSync } from 'node:fs'
import { blankState, decodeChanges, decodeDiff, decodeState, withAfterPage, withFirstPage, tailKey, transcriptArg, withTail, type Cmd, type CmdResult, type TranscriptEntry } from './contract'
import { newCmd } from './transport'

const fixture = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')

describe('contratto', () => {
  it('legge i tre stati delle fixture', () => {
    for (const n of ['state-1-question.json', 'state-2-idle.json', 'state-3-stale.json']) {
      const s = decodeState(fixture(n))
      expect(s.v).toBe(1)
      expect(Array.isArray(s.sessions)).toBe(true)
    }
  })

  it('porta host e pubblicazioni di oggi della 1.75, vuoti con un relay precedente', () => {
    const idle = decodeState(fixture('state-2-idle.json'))
    expect(idle.hosts_status).toEqual([expect.objectContaining({ name: 'win', up: true, cpu: 0.42, ram_total: 17063350272, sessions: [] })])
    expect(idle.published_today.map(p => p.kind)).toEqual(['deploy', 'release', 'push'])
    const q = decodeState(fixture('state-1-question.json'))
    expect([q.hosts_status, q.published_today]).toEqual([[], []])
    const stale = decodeState(fixture('state-3-stale.json'))
    expect(stale.hosts_status[0]).toMatchObject({ up: false, error: 'ssh: timeout', cpu: null, disk_free: null })
    expect(blankState(1).hosts_status).toEqual([]); expect(blankState(1).published_today).toEqual([])
  })

  // Contratto 1.86: i campioni orari della settimana e da dove viene; assenti con un relay precedente o senza campioni.
  it('porta w7_points e w7_origins della 1.86', () => {
    const q = decodeState(fixture('state-2-idle.json')).quota
    expect(q.personal.w7_points).toEqual({ start: 1789005600, steps: [[0, 2], [2, 4], [3, 5], [24, 30], [25, 38]] })
    expect(q.personal.w7_origins).toEqual({ sessions: 20.1, automations: 6.5, requests: 11.4 })
    expect(q.work.w7_points ?? null).toBeNull()
    expect(q.work.w7_origins ?? null).toBeNull()
  })

  it('porta i ricorrenti della 1.33 e i dispositivi della 1.32', () => {
    const s = decodeState(fixture('state-2-idle.json'))
    expect(s.recurring?.map(r => r.id)).toEqual(['x-posts', 'release-changelog', 'plugin-rivals'])
    expect(s.devices?.map(d => d.kind)).toEqual(['phone', 'watch', 'tablet', 'chromebook', null, 'web'])
  })

  it('porta consiglio, approvazioni e sessioni finite della 1.37, con i default quando mancano', () => {
    const s = decodeState(fixture('state-1-question.json'))
    const atlas = s.sessions.find(x => x.name === 'atlas-shop')!
    expect(atlas.advice).toMatchObject({ model: 'claude-fable-5-1', effort: 'high', switch_cost_tokens: 36000, when: 'next_task', differs: true })
    expect(s.sessions.find(x => x.name === 'field-notes')!.finished).toBe(true)
    expect(atlas.finished).toBe(false)
    expect(atlas.duplicate_of).toBeNull()
    expect(s.sessions.find(x => x.name === 'field-notes')!.advice).toBeNull()
    expect(s.approvals.map(a => [a.task, a.deploy])).toEqual([['atlas-release-2-4', true]])
    expect(decodeState(fixture('state-3-stale.json')).approvals).toEqual([])
    expect(s.ops).toEqual(expect.arrayContaining(['approve', 'decision']))
  })

  it('porta i Prossimi della 1.38, con le voci che sbloccano', () => {
    const s = decodeState(fixture('state-1-question.json'))
    expect(s.sessions.find(x => x.name === 'atlas-shop')!.next_steps).toEqual([
      { text: 'ok to deploy on staging', blocking: true }, { text: 'review the test seeds', blocking: false },
    ])
    expect(s.sessions.find(x => x.name === 'field-notes')!.next_steps).toBeUndefined()
  })

  it('rifiuta quello che non è uno stato', () => {
    expect(() => decodeState('{"x":1}')).toThrow()
  })
})

describe('lo stato prima del primo /state del relay (supervisor, 13/10 20:48)', () => {
  it('vuoto, mai i dati di prova: nessuna sessione, quota, recap o dispositivo', () => {
    const s = blankState(1789210000)
    expect(s.ts).toBe(1789210000)
    expect(s.sessions).toEqual([])
    expect(s.quota).toEqual({})
    expect(s.recap.items).toEqual([])
    expect(s.devices ?? []).toEqual([])
  })
})

describe('chat da /tail (contratto 1.59)', () => {
  const e = (id: string) => ({ id, role: 'assistant', text: id, at: 1 })
  const tail = { session: 'atlas-shop', id: 's1', at: 100, entries: [e('b'), e('c'), e('d')], more: true }
  it('si aggancia alla copia o la sostituisce', () => {
    expect(withTail([e('a'), e('b')], tail).map(x => x.id)).toEqual(['a', 'b', 'c', 'd'])
    expect(withTail([e('x')], tail).map(x => x.id)).toEqual(['b', 'c', 'd'])
    expect(withTail([e('x')], { ...tail, entries: [] }).map(x => x.id)).toEqual(['x'])
    expect(tailKey('abc.def#g$h[i]j/k')).toBe('abc_def_g_h_i_j_k')
  })

  // Franz 28/10 11:02: al riavvio della PWA la chat mostrava solo i prompt. Il /tail di una sessione con passaggi pesanti
  // tiene una o due voci (32 KB), e la lettura dopo chiedeva solo `after=` l'ultima: le risposte prima non arrivavano mai.
  it('transcript_reset: con reset la pagina after prende il posto della lista, senza reset va in coda', () => {
    const have: TranscriptEntry[] = [{ id: 'a1.0', role: 'user', text: 'vecchia' }]
    const fresh: TranscriptEntry[] = [{ id: 'n1.0', role: 'user', text: 'nuova' }]
    expect(withAfterPage(have, { entries: fresh, more: true, reset: true }).map(e => e.id)).toEqual(['n1.0'])
    expect(withAfterPage(have, { entries: fresh }).map(e => e.id)).toEqual(['a1.0', 'n1.0'])
  })

  it('transcript_after_restart: la prima lettura chiede la pagina intera anche con le voci del tail', () => {
    expect(transcriptArg([e('y'), e('z')], false)).toBe('50')
    expect(transcriptArg([], false)).toBe('50')
    expect(transcriptArg([e('y'), e('z')], true)).toBe('200:after=z')
    const page = { entries: [e('v'), e('w'), e('x'), e('y')], more: true }
    expect(withFirstPage([e('y'), e('z')], page).map(x => x.id)).toEqual(['v', 'w', 'x', 'y', 'z'])
    expect(withFirstPage([], page).map(x => x.id)).toEqual(['v', 'w', 'x', 'y'])
    expect(withFirstPage([e('z')], { entries: [] }).map(x => x.id)).toEqual(['z'])
  })
})

describe('contratto 1.72, il diff dal telefono (gli stessi casi di DiffContractTest)', () => {
  const hunk = { old_start: 12, old_lines: 3, new_start: 12, new_lines: 4, lines: [
    ' export function total(cart: Cart) {', '-  return cart.items.reduce((s, i) => s + i.price, 0)',
    '+  const sub = cart.items.reduce((s, i) => s + i.price * i.qty, 0)', '+  return sub - (cart.giftCard?.amount ?? 0)', ' }'] }
  it('legge la vista «Modifiche» della fixture', () => {
    const v = decodeChanges(fixture('changes-sample.json'))
    expect([v.source, v.repo, v.base, v.cut, v.at]).toEqual(['git', '/home/dev/storefront', 'a1b2c3d', false, 1792100000])
    expect(v.files.map(f => [f.path, f.status, f.added, f.removed, f.size, f.cut])).toEqual([
      ['src/checkout/totals.ts', 'modified', 2, 1, 2140, false], ['tests/checkout/totals.test.ts', 'added', 5, 0, 212, false],
      ['.env.local', 'modified', 1, 1, 88, false], ['assets/logo.png', 'binary', 0, 0, 48213, false]])
    expect(v.files[0].hunks).toEqual([hunk])
    expect(v.files[1].hunks[0].lines).toHaveLength(5)
    expect([v.files[2].hunks, v.files[3].hunks]).toEqual([[], []])
    expect(v.totals).toEqual({ files: 4, added: 8, removed: 2 })
  })
  it('in /state i conteggi solo dove ci sono modifiche, e le due op', () => {
    const s = decodeState(fixture('state-1-question.json'))
    expect(s.sessions.filter(x => x.changes).map(x => [x.name, x.changes])).toEqual([['atlas-shop', { files: 4, added: 8, removed: 2, at: 1792100000 }]])
    for (const n of ['state-1-question.json', 'state-2-idle.json']) expect(decodeState(fixture(n)).ops).toEqual(expect.arrayContaining(['changes', 'diff']))
  })
  // Contratto 1.80 (23/10): le richieste in «Serve te» si chiudono da sole; op `attention_done` = il tasto «Fatto».
  it('1.80: attention porta closes, needs_you until ed expires_at, e attention_done parte con kind e since', () => {
    const s = decodeState(fixture('state-1-question.json'))
    expect(s.ops).toEqual(expect.arrayContaining(['attention_done']))
    expect(s.attention![0].closes).toBe('answer')
    const root = JSON.parse(fixture('cmd-result-sample.json'))
    const done = root.cmd.filter((c: Cmd) => c.op === 'attention_done')
    expect(done.map((c: Cmd) => [c.arg, c.kind])).toEqual([['atlas-shop', 'needs_you'], ['nobody', 'needs_you']])
    expect(root.result.find((r: CmdResult) => r.id === done[0].id).text).toBe('closed: needs_you atlas-shop')
    expect(root.result.find((r: CmdResult) => r.id === done[1].id).ok).toBe(false)
    const c = newCmd('attention_done', null, 'pix-docs', undefined, undefined, { kind: 'turn_blocking', since: 1789210000 })
    expect([c.kind, c.since, c.session]).toEqual(['turn_blocking', 1789210000, null])
    expect('kind' in newCmd('screen', 'x')).toBe(false)
    // Dalla 1.84 la fixture ha già un needs_you con expires_at (atlas-shop, legato a un compito).
    const st = decodeState(fixture('state-2-idle.json'))
    expect(st.sessions.find(x => x.needs_you)!.needs_you!.expires_at).toBe(1789221400)
  })
  // Contratto 1.84 (24/10): suggerimenti, quota di oggi, needs_you legato a un compito, chiusura con lo stato salvato.
  it('1.84: suggestions, usage_today, needs_you.task/from, closed e le op suggestion, close_preview, close', () => {
    const s1 = decodeState(fixture('state-1-question.json')), s2 = decodeState(fixture('state-2-idle.json')), s3 = decodeState(fixture('state-3-stale.json'))
    expect(s1.suggestions).toEqual([])
    expect(s2.suggestions!.map(x => x.kind).sort()).toEqual([...new Set(s2.suggestions!.map(x => x.kind))].sort())
    expect(s2.suggestions!.length).toBe(3)
    expect(s2.usage_today!.length).toBe(4)
    expect(new Set(s2.usage_today!.map(u => u.account)).size).toBe(2)
    const asked = s2.sessions.find(x => x.needs_you?.task)!
    expect([asked.name, asked.needs_you!.task, asked.needs_you!.from]).toEqual(['atlas-shop', 'atlas-tv-pairing', 'master'])
    expect(s1.sessions.find(x => x.closed)!.closed).toEqual({ at: 1789200000, session_id: '3d1b2c4e-0000-4000-8000-000000000004' })
    expect(s3.sessions.find(x => x.closed)!.closed!.dirty_n).toBe(2)
    const root = JSON.parse(fixture('cmd-result-sample.json'))
    const ops = (op: string) => root.cmd.filter((c: Cmd) => c.op === op).map((c: Cmd) => root.result.find((r: CmdResult) => r.id === c.id).ok)
    expect(ops('suggestion')).toEqual([true, true, false, true, true])   // 1.87: un id che non c'è più è «already gone», ok; 1.88: choose fuori dalle opzioni, discuss, choose
    expect(ops('close_preview')).toEqual([true, false])
    expect(ops('close')).toEqual([true, false])
    const prev = root.cmd.find((c: Cmd) => c.op === 'close_preview')
    expect(readPreview(root.result.find((r: CmdResult) => r.id === prev.id).text)!.sessions.length).toBeGreaterThan(0)
  })
  // Contratto 1.88 (27/10): la coda dei suggerimenti; state-3 ha una voce per tipo nuovo, state-2 le tre con rank e source.
  it('1.88: rank, source, options sulle choice, i sei tipi nuovi, niente prompt nello stato', () => {
    const s2 = decodeState(fixture('state-2-idle.json')), s3 = decodeState(fixture('state-3-stale.json'))
    expect(s2.suggestions!.map(x => x.rank)).toEqual([2, 5, 6])
    expect(s2.suggestions!.every(x => x.source === 'master' && x.prompt === undefined)).toBe(true)
    expect(s3.suggestions!.map(x => x.kind)).toEqual(['choice', 'followup', 'unsaved', 'waiting', 'idea', 'proposal'])
    expect(s3.suggestions!.find(x => x.kind === 'choice')!.options).toEqual(['Short', 'Full'])
    expect(s3.suggestions!.filter(x => x.source === 'script').length).toBe(3)
  })
  // Contratto 1.81 (23/10): un compito che chiede una scelta porta `options`; op `approve` con `choice`.
  it('1.81: un ok con opzioni porta options e approve parte con choice', () => {
    const s = decodeState(fixture('state-2-idle.json'))
    expect(s.approvals!.find(a => a.task === 'atlas-checkout-layout')!.options).toEqual(['Strada A', 'Strada B'])
    expect(decodeState(fixture('state-1-question.json')).approvals?.some(a => a.options)).toBeFalsy()
    const root = JSON.parse(fixture('cmd-result-sample.json'))
    const ch = root.cmd.filter((c: Cmd) => c.op === 'approve' && c.choice)
    expect(ch.map((c: Cmd) => c.choice)).toEqual(['Strada B', 'Strada C'])
    expect(root.result.find((r: CmdResult) => r.id === ch[0].id).text).toBe('approved atlas-checkout-layout (Strada B)')
    expect(root.result.find((r: CmdResult) => r.id === ch[1].id).ok).toBe(false)
    const c = newCmd('approve', null, 'atlas-checkout-layout', 'ok', undefined, { choice: 'Strada B' })
    expect(c.choice).toBe('Strada B')
    expect('choice' in newCmd('approve', null, 't', 'ok')).toBe(false)
  })
  // Contratto 1.82 (24/10): op transcript con sid per una conversazione chiusa.
  it('1.82: transcript per sid, riuscito e rifiutato; il comando porta sid', () => {
    const root = JSON.parse(fixture('cmd-result-sample.json'))
    const by = root.cmd.filter((c: Cmd) => c.op === 'transcript' && c.sid)
    expect(by.length).toBe(2); expect(by[0].session).toBe(null)
    expect(root.result.find((r: CmdResult) => r.id === by[0].id).ok).toBe(true)
    expect(root.result.find((r: CmdResult) => r.id === by[1].id).text).toBe('no transcript for ffffffff-0000-4000-8000-000000000000')
    expect(newCmd('transcript', null, '50', undefined, undefined, { sid: 'abc' }).sid).toBe('abc')
    expect('sid' in newCmd('transcript', 'x', '50')).toBe(false)
  })
  it('ogni voce della trascrizione ha la chiave diff, e la voce Edit porta il suo', () => {
    const root = JSON.parse(fixture('cmd-result-sample.json'))
    const ids = new Set(root.cmd.filter((c: { op: string }) => c.op === 'transcript').map((c: { id: string }) => c.id))
    const pages = root.result.filter((r: { id: string; ok: boolean }) => ids.has(r.id) && r.ok).map((r: { text: string }) => JSON.parse(r.text))
    const tail = JSON.parse(fixture('local-api.json')).exchanges.filter((x: { name: string }) => x.name === 'tail').map((x: { response: { body: unknown } }) => x.response.body)
    const all: TranscriptEntry[] = [...pages, ...tail].flatMap((p: { entries: TranscriptEntry[] }) => p.entries)
    expect(all.length).toBeGreaterThan(0)
    expect(all.every(e => 'diff' in e)).toBe(true)
    const withDiff = [...new Map(all.filter(e => e.diff).map(e => [e.id, e])).values()]
    expect(withDiff.map(e => e.tool)).toEqual(['Edit'])
    expect(withDiff[0].diff).toEqual({ path: '/home/dev/storefront/src/checkout/totals.ts', kind: 'edit', added: 2, removed: 1, hunks: [hunk], cut: false })
  })
  it('il passaggio intero di `diff`; quello che non è un diff o una vista si rifiuta', () => {
    expect(decodeDiff(JSON.stringify({ path: '/w/a.ts', kind: 'create', added: 1, removed: 0, hunks: [], cut: false })).kind).toBe('create')
    expect(() => decodeDiff('{"x":1}')).toThrow()
    expect(() => decodeChanges('{"x":1}')).toThrow()
  })
})
