import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { adviceLine, adviceOf, age, backTo, cmdOf, lookOf, marks } from './tiAspetta'
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
// Contratto 1.149 (app-consiglio-carte-16-11): gli stessi casi di TiAspettaTest.kt.
  it('il consiglio di chi chiede, con la motivazione', () => {
    const s = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')) as { cmd: { id: string; op: string }[]; result: { id: string; text: string }[] }
    const all = decodeWaits(s.result.find(r => r.id === s.cmd.find(c => c.op === 'attese')!.id)!.text).items
    const choice = all.find(w => w.kind === 'choice')!
    expect(choice.advice).toEqual({ text: 'Tested on staging; a tag undoes it', option: 'Release now', approve: null })
    // I segni li porta già recommended: l'opzione consigliata e il suo tasto, e nessun altro. Nella fixture il loro value finisce in
    // « (recommended)» mentre advice.option no: l'app non abbina su advice.option, mostra solo il testo.
    expect(choice.options!.filter(o => o.recommended).map(o => o.value)).toEqual(['Release now (recommended)'])
    expect(choice.actions!.filter(a => a.recommended).map(a => a.cmd.choice)).toEqual(['Release now (recommended)'])
    const say = (a: Parameters<typeof adviceOf>[0]) => { const x = adviceOf(a); return x ? adviceLine(x, 'Consiglio', 'approvare', 'non approvare') : null }
    expect(say(choice.advice)).toBe('Consiglio: Tested on staging; a tag undoes it')
    // Le voci che non sono carte, e le carte nate prima, non hanno consiglio: niente riga.
    expect(all.filter(w => w.kind !== 'choice' && w.kind !== 'ok').every(w => say(w.advice) === null)).toBe(true)
    expect([say(null), say(undefined), say({ text: '  ' })]).toEqual([null, null, null])
    const ap = (JSON.parse(readFileSync(new URL('../../../contract/approvals-advice-sample.json', import.meta.url), 'utf8')) as { approvals: { advice?: Parameters<typeof adviceOf>[0] }[] }).approvals
    expect(ap.map(a => say(a.advice))).toEqual(['Consiglio: approvare · Tested on staging; a tag undoes it', 'Consiglio: Fewer steps to pay'])
    expect(say({ text: 'Not tested on the phone yet', approve: false })).toBe('Consiglio: non approvare · Not tested on the phone yet')
  })
  it('actions of the fixture', () => {
    const s = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')) as { cmd: { id: string; op: string }[]; result: { id: string; text: string }[] }
    const id = s.cmd.find(c => c.op === 'attese')!.id
    const all = decodeWaits(s.result.find(r => r.id === id)!.text).items
    const choice = all.find(w => w.kind === 'choice')!
    expect(choice.title).toBe('Release 2.4'); expect(choice.card?.deploy).toBe(true)
    // Contratto 1.137: dopo le opzioni «None, redo» (`approval_redo`, col motivo in `text`), poi «Close without ok».
    // Contratto 1.151: dopo «Close without ok» anche «Non ora» e «Lascia perdere» della decisione.
    expect(choice.actions!.map(lookOf)).toEqual(['option', 'option', 'tonal', 'tonal', 'tonal', 'tonal'])
    expect(choice.actions!.map(a => a.cmd.op)).toEqual(['approve', 'approve', 'approval_redo', 'approval_close', 'decisione', 'decisione'])
    expect(cmdOf(choice.actions![2], ' too big on the phone ').text).toBe('too big on the phone')
    expect(cmdOf(choice.actions![3], ' superseded by the hotfix ').text).toBe('superseded by the hotfix')
    const next = all.find(w => w.kind === 'prossimi')!.actions!
    expect(cmdOf(next.find(a => a.text_field === 'arg')!, 'Commit now').arg).toBe('Commit now')
  })
  it('age', () => { for (const c of file.age) expect(age(c.s)).toBe(c.out) })
})
