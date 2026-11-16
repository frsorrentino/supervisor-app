import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { isOpen, parseBoxes, toggle, type BoxState, type Kind } from './riquadri'

// app-riquadri-chiusi-16-11: gli stessi casi di RiquadriTest.kt.
type Cases = {
  cases: { name: string; st: BoxState; waiting: string[]; open: Partial<Record<Kind, boolean>> }[]
  toggles: { name: string; st: BoxState; kind: Kind; waiting: string[]; out: BoxState }[]
}
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/riquadri-cases.json', import.meta.url), 'utf8')) as Cases

describe('riquadri_chiusi', () => {
  for (const c of file.cases) it(c.name, () => {
    for (const [k, v] of Object.entries(c.open)) expect(isOpen(c.st, k as Kind, c.waiting), k).toBe(v)
  })
  for (const c of file.toggles) it(c.name, () => expect(toggle(c.st, c.kind, c.waiting)).toEqual(c.out))
  it('uno stato illeggibile vale nessuno stato', () => {
    expect([parseBoxes(null), parseBoxes('x'), parseBoxes('[1]')]).toEqual([{}, {}, {}])
    expect(parseBoxes('{"blog":{"open":{"azioni":true}}}')).toEqual({ blog: { open: { azioni: true } } })
  })
})
