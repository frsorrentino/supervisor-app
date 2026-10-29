import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type PluginsPage } from './contract'
import { account, accountsWithUpdates, pending, updateArg } from './plugins'

const read = (f: string) => readFileSync(new URL(`../../../contract/${f}`, import.meta.url), 'utf8')
const sample = JSON.parse(read('cmd-result-sample.json'))
const pid = sample.cmd.find((c: { op: string }) => c.op === 'plugins').id
const page: PluginsPage = JSON.parse(sample.result.find((r: { id: string }) => r.id === pid).text)

describe('Plugin e skill (contratto 1.95)', () => {
  it('plugins_state: il riassunto conta gli aggiornamenti; un relay precedente non lo ha', () => {
    const st = decodeState(read('state-3-stale.json'))
    expect(pending(st)).toBe(1)
    expect(accountsWithUpdates(st)).toEqual(['personal'])
    expect(pending(decodeState(read('state-2-idle.json')))).toBeNull()
  })

  it('l\'elenco mette prima chi ha un aggiornamento; senza ultima versione nota resta fra gli altri', () => {
    const a = account(page.accounts.find(x => x.account === 'personal')!)
    expect(a.updates.map(p => p.name)).toEqual(['chrisbanes-skills'])
    expect(a.rest.some(p => p.name === 'frontend-design' && p.update == null)).toBe(true)
    expect(updateArg('personal', a.updates[0])).toBe('personal:chrisbanes-skills@chrisbanes-skills')
    expect(a.skills).toBeGreaterThan(0)
  })
})
