import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { TranscriptEntry } from './contract'
import { group, merge } from './chatFeed'
import { entryIds, failedCount, foldable, FOLD_TAIL, rowOf, shownWork, stepDetail, stepLine, toolCount, turns, type Step } from './chatTurns'
import { copyText, rich, sections, stripTipo } from './answer'

// Gli stessi casi di ChatTurnsTest.kt.
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/chat-turns-cases.json', import.meta.url), 'utf8')) as {
  turns: { name: string; entries: TranscriptEntry[]; out: unknown[] }[]
  sections: { name: string; text: string; lead: string; secs: { title: string; body: string }[] }[]
  rich: { name: string; text: string; out: unknown[] }[]
  tipo: { name: string; text: string; out: { text: string; tipo: string | null } }[]
  copy: { name: string; text: string; out: string }[]
  steps: { name: string; tool: string; text: string | null; note: string | null; out: { name: string; arg: string; note: string | null; text: string | null } }[]
}
const shape = (entries: TranscriptEntry[], grouped: boolean) => {
  const items = merge(entries, [])
  return turns(grouped ? group(items) : items).map(r => r.type === 'separator' ? { sep: r.entry.id }
    : { key: r.key, request: r.request?.entry?.id ?? null, work: r.work.map(w => `${w.kind}:${w.entry.id}`), answer: r.answer?.id ?? null, ...(r.auto ? { auto: true } : {}) })
}

describe('scheda unica: i turni', () => {
  for (const c of file.turns) it(c.name, () => {
    expect(shape(c.entries, false)).toEqual(c.out)
    // Le voci già raccolte in gruppi di passi danno gli stessi turni.
    expect(shape(c.entries, true)).toEqual(c.out)
  })
  it('un messaggio mandato da qui e non ancora trascritto apre il suo turno', () => {
    const rows = turns(merge([], [[{ id: 'c1', session: 'a', text: 'Vai', sentAt: 10 }, 'sending']]))
    expect(rows.map(r => r.type === 'turn' && [r.key, r.request?.type, r.work.length, r.answer])).toEqual([['m-c1', 'mine', 0, null]])
  })
  it('la riga di una voce, per «Nuovi», e le voci di un turno, per Cerca', () => {
    const items = group(merge(file.turns[0].entries, []))
    const rows = turns(items)
    expect(items.map(it => rowOf(rows, it))).toEqual(items.map(() => 0))
    expect(rows[0].type === 'turn' && entryIds(rows[0])).toEqual(['u1', 'a1', 't1', 'a2', 't2', 't3', 'a3'])
  })
  it('un lavoro lungo chiuso mostra le ultime voci; aperto, tutte', () => {
    const work: Step[] = Array.from({ length: 9 }, (_, i) => ({ kind: i % 4 ? 'tool' : 'say', entry: { id: `e${i}`, role: 'tool', error: i === 5 } }))
    expect(foldable(work)).toBe(true)
    expect(shownWork(work, false).map(w => w.entry.id)).toEqual(work.slice(-FOLD_TAIL).map(w => w.entry.id))
    expect(shownWork(work, true)).toHaveLength(9)
    expect(foldable(work.slice(0, 6))).toBe(false)
    expect(shownWork(work.slice(0, 6), false)).toHaveLength(6)
    expect([toolCount(work), failedCount(work)]).toEqual([6, 1])
  })
})

describe('scheda unica: la riga di un passo', () => {
  for (const c of file.steps) it(c.name, () => {
    const e: TranscriptEntry = { id: 'x', role: 'tool', tool: c.tool, text: c.text, note: c.note }
    expect({ ...stepLine(e), ...stepDetail(e) }).toEqual(c.out)
  })
})

describe('scheda unica: la risposta come documento', () => {
  for (const c of file.sections) it(c.name, () => expect(sections(c.text)).toEqual({ lead: c.lead, secs: c.secs }))
  for (const c of file.rich) it(c.name, () => expect(rich(c.text)).toEqual(c.out))
  for (const c of file.tipo) it(c.name, () => expect(stripTipo(c.text)).toEqual(c.out))
  // app-chat-testo-da-inviare-13-11: il tasto «Copia» del box di una citazione.
  for (const c of file.copy) it(c.name, () => expect(copyText(c.text)).toBe(c.out))
  it('in inglese la riga dell\'esito con la sua etichetta chiude come «Esito»', () => {
    expect(stripTipo('Done.\nOutcome: fixed\nTipo: pronto', 'Outcome')).toEqual({ text: 'Done.\nOutcome: fixed', tipo: 'pronto' })
  })
})
