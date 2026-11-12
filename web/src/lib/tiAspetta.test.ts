import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { age, backTo, cmdOf, lookOf, marks } from './tiAspetta'
import { decodeWaits } from './decideDecode'
import { askedWait } from './sessionLinks'

// Gli stessi casi di TiAspettaTest in Kotlin, dallo stesso file (app-scheda-ti-aspetta-07-11).
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/ti-aspetta-cases.json', import.meta.url), 'utf8')) as {
  marks: { name: string; text: string; ask: string; out: [string, boolean][] }[]; age: { s: number; out: string }[]; links: { link: string; out: string | null }[]
}

describe('tiAspetta', () => {
  // app-ti-aspetta-lista-chiusa-08-11: la scheda chiusa torna alla pagina da cui si è aperta; da un'altra scheda, alla prima; da un link, alla home.
  it('back to where it was opened', () => {
    expect(backTo(null, 'box-aspetta')).toBe('box-aspetta')
    expect(backTo('box-aspetta', 'attesa')).toBe('box-aspetta')
    expect(backTo(null, null)).toBe(null)
    expect(backTo(null, 'attesa')).toBe(null)
  })
  for (const c of file.marks) it(c.name, () => expect(marks(c.text, c.ask)).toEqual(c.out.map(([text, on]) => ({ text, on }))))
  it('links', () => { for (const c of file.links) expect(askedWait(new URL(c.link).hash)).toBe(c.out) })
  it('actions of the fixture', () => {
    const s = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')) as { cmd: { id: string; op: string }[]; result: { id: string; text: string }[] }
    const id = s.cmd.find(c => c.op === 'attese')!.id
    const all = decodeWaits(s.result.find(r => r.id === id)!.text).items
    const choice = all.find(w => w.kind === 'choice')!
    expect(choice.title).toBe('Release 2.4'); expect(choice.card?.deploy).toBe(true)
    // Contratto 1.137: dopo le opzioni «None, redo» (`approval_redo`, col motivo in `text`), poi «Close without ok».
    expect(choice.actions!.map(lookOf)).toEqual(['option', 'option', 'tonal', 'tonal'])
    expect(choice.actions!.map(a => a.cmd.op)).toEqual(['approve', 'approve', 'approval_redo', 'approval_close'])
    expect(cmdOf(choice.actions![2], ' too big on the phone ').text).toBe('too big on the phone')
    expect(cmdOf(choice.actions![3], ' superseded by the hotfix ').text).toBe('superseded by the hotfix')
    const next = all.find(w => w.kind === 'prossimi')!.actions!
    expect(cmdOf(next.find(a => a.text_field === 'arg')!, 'Commit now').arg).toBe('Commit now')
  })
  it('age', () => { for (const c of file.age) expect(age(c.s)).toBe(c.out) })
})
