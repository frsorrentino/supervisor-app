import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { decodeWait, decodeWaits } from './decideDecode'

// Contratto 1.130 (app-scheda-ti-aspetta-07-11): le attese della fixture, come ContractTest.waits in Kotlin.
const fixture = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')
const sample = JSON.parse(fixture('cmd-result-sample.json')) as { cmd: { id: string; op: string; arg: string | null }[]; result: { id: string; ok: boolean; text: string }[] }
const result = (op: string, arg: string | null = null) => {
  const c = sample.cmd.filter(x => x.op === op).find(x => arg == null || x.arg === arg)!
  return sample.result.find(r => r.id === c.id)!
}

describe('waits', () => {
  it('attese', () => {
    const p = decodeWaits(result('attese').text)
    expect(p.items.map(i => i.id)).toEqual(['wd2f87e85e2', 'wb973ef3ed0'])
    expect(p.items[0].answer).toEqual({ op: 'approve', arg: 'atlas-release-2-4' })
    expect(p.items[0].options?.[0].recommended).toBe(true)
    expect(p.items[1].answer?.done?.op).toBe('attention_done')
  })
  it('attesa con il pezzo di trascrizione', () => {
    const w = decodeWait(result('attesa', 'wd2f87e85e2').text)
    expect(w.msg?.id).toBe('5d2e8f1a-3c47-4b9e-8a61-7e0d9c4b2f18.0')
    expect(w.msg?.key).toBe('Should I release now or tomorrow morning?')
    expect(result('attesa', 'w0000000000').ok).toBe(false)
  })
  it('wid nello stato', () => {
    const st = decodeState(fixture('state-1-question.json'))
    expect(st.ops).toEqual(expect.arrayContaining(['attese', 'attesa']))
    expect(st.approvals.every(a => !!a.wid)).toBe(true)
  })
})
