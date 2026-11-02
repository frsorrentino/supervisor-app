import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { decodePrima } from './decideDecode'
import { DemoTransport } from './demoTransport'
import { askConsent, autoOpen, canStart, emptySection, finished, firstRoundOffer } from './primaRaccolta'
import { newCmd } from './transport'

// Contratto 1.100, sulle fixture del relay: gli stessi casi di PrimaRaccoltaTest in Kotlin.
const sample = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')) as { cmd: { id: string; op: string }[]; result: { id: string; text: string }[] }
const id = sample.cmd.find(c => c.op === 'prima_raccolta')!.id
const page = decodePrima(sample.result.find(r => r.id === id)!.text)
const summary = decodeState(readFileSync(new URL('../../../contract/state-3-stale.json', import.meta.url), 'utf8')).prima_raccolta!

describe('prima_raccolta', () => {
  it('riassunto e pagina', () => {
    expect(summary).toMatchObject({ status: 'ask', done: 7, total: 8, ask: true })
    expect(page.sections.map(s => s.key)).toEqual(['plugins', 'sessions'])
    expect(page.cost).toBe(0)
  })
  it('si apre da sola una volta', () => {
    expect(autoOpen(summary, false)).toBe(true); expect(autoOpen({ ...summary, status: 'running' }, false)).toBe(true)
    expect(autoOpen(summary, true)).toBe(false); expect(autoOpen({ ...summary, status: 'done' }, false)).toBe(false); expect(autoOpen(null, false)).toBe(false)
  })
  it('una sezione vuota dice perché', () => {
    expect(emptySection(page.sections[0])).toBe(false); expect(emptySection(page.sections[1])).toBe(true)
    expect(page.sections[1].why).toBe('Past sessions are read only with your yes.')
  })
  it('il consenso si chiede una volta', () => {
    expect(askConsent(page)).toBe(true); expect(askConsent({ ...page, consent: true })).toBe(false); expect(askConsent({ ...page, consent: false })).toBe(false); expect(askConsent({ ...page, ask_text: null })).toBe(false)
  })
  it('si avvia solo se non sta girando', () => {
    expect(canStart(page)).toBe(false); expect(canStart({ ...page, status: 'none' })).toBe(true); expect(canStart({ ...page, status: 'interrupted' })).toBe(true); expect(canStart({ ...page, status: 'running' })).toBe(false)
  })
  it('il primo giro dice il costo e parte una volta', () => {
    expect(firstRoundOffer(page)).toBeNull()
    const done = { ...page, status: 'done' }
    expect(firstRoundOffer(done)).toContain('It starts only if you touch it')
    expect(firstRoundOffer({ ...done, first_round: { ...done.first_round, at: 5 } })).toBeNull()
    expect(finished(done)).toBe(true); expect(finished(page)).toBe(false)
  })
  it('il tocco porta azione, valore e dispositivo', () => {
    expect(newCmd('prima_raccolta_azione', null, null, undefined, undefined, { action: 'consent', value: 'si' })).toMatchObject({ action: 'consent', value: 'si', device: 'web' })
  })
  it('demo: dal consenso al primo giro, la riga della home sparisce', async () => {
    const d = new DemoTransport(() => 1_800_000_000)
    expect(d.state.prima_raccolta?.status).toBe('ask')
    const read = async () => decodePrima((await d.send(newCmd('prima_raccolta', null)))!.text)
    expect((await read()).sections.find(s => s.key === 'sessions')!.n).toBe(0)
    await d.send(newCmd('prima_raccolta_azione', null, null, undefined, undefined, { action: 'consent', value: 'si' }))
    const p = await read()
    expect(p.status).toBe('done'); expect(p.sections.find(s => s.key === 'sessions')!.n).toBe(12); expect(firstRoundOffer(p)).toBeTruthy()
    expect(d.state.prima_raccolta?.status).toBe('done')
    await d.send(newCmd('prima_raccolta_azione', null, null, undefined, undefined, { action: 'first_round' }))
    expect(firstRoundOffer(await read())).toBeNull(); expect(d.state.prima_raccolta).toBeNull()
  })
})
