import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeChanges, decodeState, type ChangedFile, type StepDiff } from './contract'
import { alone, counts, fileArg, folder, gutter, hidden, hiddenFiles, label, MAX_LINE_CHARS, name, pill, read, rows, secret, showCounts, shown, source, stepLine } from './diff'

// Gli stessi casi di DiffRulesTest in Kotlin (contratto 1.72, il diff dal telefono).
const fixture = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')
const view = decodeChanges(fixture('changes-sample.json'))
const totals = view.files[0]
const csv = (n: number, cut: boolean): StepDiff => ({ path: '/w/a.csv', kind: 'create', added: 261, removed: 0, hunks: [{ old_start: 0, old_lines: 0, new_start: 1, new_lines: 261, lines: Array.from({ length: n }, (_, i) => `+r${i}`) }], cut })

describe('la riga chiusa', () => {
  it('nome, numeri col meno tipografico e cartella', () => {
    expect(counts(2, 1)).toBe('+2 \u22121')
    expect(stepLine({ path: '/home/dev/storefront/src/checkout/totals.ts', kind: 'edit', added: 2, removed: 1, hunks: totals.hunks, cut: false })).toBe('totals.ts · +2 \u22121')
    expect([name('src/checkout/totals.ts'), folder('src/checkout/totals.ts'), folder('.env.local')]).toEqual(['totals.ts', 'src/checkout', null])
  })
})

describe('i file della vista «Modifiche»', () => {
  it('le etichette e i numeri', () => {
    expect(view.files.map(f => label(f.status))).toEqual([null, 'new', null, 'binary'])
    expect(['deleted', 'renamed', 'copied'].map(label)).toEqual(['deleted', 'renamed', null])
    expect(view.files.map(showCounts)).toEqual([true, true, true, false])
  })
  it('perché un file arriva senza righe', () => {
    expect(view.files.map(hidden)).toEqual([null, null, 'secret', 'binary'])
    expect(hidden({ path: 'data/dump.sql', status: 'modified', added: 10, removed: 2, size: 3_200_000, hunks: [], cut: false })).toBe('big')
    for (const p of ['.env', 'certs/server.pem', 'deploy.key', 'id_ed25519', 'store.p12', 'credentials.json']) expect(secret(p), p).toBe(true)
    for (const p of ['environment.ts', 'keys.md', 'src/identity.ts']) expect(secret(p), p).toBe(false)
    expect(hidden({ path: 'notes/empty.md', status: 'added', added: 0, removed: 0, size: 0, hunks: [], cut: false })).toBe('empty')
  })
  // Caso 2 dei punti delicati.
  it('rinominato senza righe cambiate: niente «+0 −0», e aperto lo dice', () => {
    const f: ChangedFile = { path: 'src/checkout/sum.ts', status: 'renamed', added: 0, removed: 0, size: 2140, hunks: [], cut: false }
    expect([label(f.status), showCounts(f), hidden(f)]).toEqual(['renamed', false, 'renamed'])
    const g: ChangedFile = { ...f, added: 1, removed: 1, hunks: totals.hunks }
    expect([showCounts(g), hidden(g)]).toEqual([true, null])
  })
})

describe('le righe del diff aperto', () => {
  it('quelle della fixture, numerate', () => {
    const r = rows(totals.hunks)
    expect(r).toEqual([
      { kind: 'same', old: 12, new: 12, text: 'export function total(cart: Cart) {', more: 0 },
      { kind: 'removed', old: 13, new: null, text: '  return cart.items.reduce((s, i) => s + i.price, 0)', more: 0 },
      { kind: 'added', old: null, new: 13, text: '  const sub = cart.items.reduce((s, i) => s + i.price * i.qty, 0)', more: 0 },
      { kind: 'added', old: null, new: 14, text: '  return sub - (cart.giftCard?.amount ?? 0)', more: 0 },
      { kind: 'same', old: 14, new: 15, text: '}', more: 0 },
    ])
    expect(gutter(r)).toBe(2)
    const created = rows(view.files[1].hunks)
    expect(created.map(x => x.new)).toEqual([1, 2, 3, 4, 5])
    expect(created.every(x => x.kind === 'added' && x.old === null)).toBe(true)
    expect(created[1].text).toBe('')
  })
  it('pezzi separati, note, tab e fine riga di Windows', () => {
    const r = rows([
      { old_start: 3, old_lines: 1, new_start: 3, new_lines: 1, lines: ['-\told\r', '+\tnew\r'] },
      { old_start: 40, old_lines: 1, new_start: 40, new_lines: 2, lines: [' end', '+more', '\\ No newline at end of file'] },
    ])
    expect(r.map(x => x.kind)).toEqual(['removed', 'added', 'gap', 'same', 'added', 'note'])
    expect([r[0].text, r[1].text]).toEqual(['    old', '    new'])
    expect(r[5]).toEqual({ kind: 'note', old: null, new: null, text: 'No newline at end of file', more: 0 })
    expect(r.map(x => x.old)).toEqual([3, null, null, 40, null, null])
    expect(r.map(x => x.new)).toEqual([null, 3, null, 40, 41, null])
  })
  // Caso 1 dei punti delicati.
  it('una riga lunghissima senza spazi: a schermo le prime 2000 lettere e quante ne restano', () => {
    const r = rows([{ old_start: 1, old_lines: 0, new_start: 1, new_lines: 1, lines: ['+' + 'x'.repeat(50_000)] }])[0]
    expect([r.text.length, r.more, r.text.includes('\n')]).toEqual([MAX_LINE_CHARS, 48_000, false])
  })
})

describe('tagli, pillola e letture', () => {
  // Caso 3 dei punti delicati.
  it('cut sul passaggio e sulla vista', () => {
    const step = csv(200, true)
    expect(shown(step, null, false, false).showAll).toBe(true)
    expect(shown(step, null, true, false)).toEqual({ diff: step, showAll: false, ask: true, beyond: false })
    const whole = csv(261, false)
    expect(shown(step, whole, true, false)).toEqual({ diff: whole, showAll: false, ask: false, beyond: false })
    expect(shown(step, { ...whole, cut: true }, true, false).beyond).toBe(true)
    const cutView = { ...view, files: Array.from({ length: 50 }, (_, i) => ({ ...totals, path: `src/f${i}.ts` })), totals: { files: 60, added: 900, removed: 120 }, cut: true }
    expect([hiddenFiles(cutView), hiddenFiles(view)]).toEqual([10, 0])
    expect(hidden({ ...totals, hunks: [], cut: true })).toBeNull()
    expect(fileArg('src/checkout/totals.ts')).toBe('file=src/checkout/totals.ts')
  })
  // Caso 4 dei punti delicati.
  it("una sessione chiusa tiene la pillola: la cartella c'è ancora", () => {
    const s = decodeState(fixture('state-1-question.json')).sessions.find(x => x.changes)!
    expect(pill({ ...s, state: 'gone' })).toEqual({ files: 4, added: 8, removed: 2, at: 1792100000 })
    expect(pill({ ...s, changes: null })).toBeNull()
  })
  // Caso 5 dei punti delicati: la scelta «Mostra tutto» resta, la copia in memoria no.
  it('rimontato il passaggio, quello intero si richiede da solo', () => {
    const after = shown(csv(200, true), null, true, false)
    expect([after.ask, after.diff.hunks[0].lines.length]).toEqual([true, 200])
    expect(shown(csv(200, true), null, true, true).ask).toBe(false)
  })
  it("l'esito di una lettura", () => {
    expect(read(null, decodeChanges)).toEqual({ ok: false, why: 'down', text: '' })
    expect(read({ id: '1', ok: false, text: 'git did not answer in 5 s', at: 0 }, decodeChanges)).toMatchObject({ ok: false, why: 'git' })
    expect(read({ id: '2', ok: false, text: 'no session x', at: 0 }, decodeChanges)).toEqual({ ok: false, why: 'other', text: 'no session x' })
    expect(read({ id: '3', ok: true, text: fixture('changes-sample.json'), at: 0 }, decodeChanges)).toEqual({ ok: true, value: view })
    expect(read({ id: '4', ok: true, text: 'not json', at: 0 }, decodeChanges)).toMatchObject({ ok: false, why: 'other' })
    expect(alone(view, 'src/checkout/totals.ts')).toEqual(totals)
    expect(alone({ ...view, files: [] }, 'src/checkout/totals.ts')).toBeNull()
  })
  it('da dove vengono le modifiche', () => {
    expect([source(view), source({ ...view, base: null }), source({ ...view, source: 'transcript', repo: null, base: null })]).toEqual(['git', 'no_commits', 'transcript'])
  })
})
