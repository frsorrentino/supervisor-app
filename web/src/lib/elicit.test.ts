import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { content, initial, openElicitations, problem, ready } from './elicit'
import type { ElicitField, State } from './contract'

const fixture = (n: string) => JSON.parse(readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8'))

describe('contratto 1.83: le domande dei server MCP', () => {
  const st = fixture('state-2-idle.json') as State
  const form = st.elicitations![0]
  const f = (k: string) => form.fields!.find(x => x.key === k) as ElicitField

  it('in /state, ops con elicit, aperte fino a expires_at', () => {
    expect(st.ops).toContain('elicit')
    expect(form.fields!.map(x => x.kind)).toEqual(['choice', 'boolean', 'text', 'integer'])
    expect(st.elicitations![1]).toMatchObject({ mode: 'url', url: 'https://www.figma.com/oauth?client_id=demo' })
    expect(openElicitations(st, form.asked_at)).toHaveLength(2)
    expect(openElicitations(st, form.expires_at).map(e => e.id)).toEqual(['e-9a0b1c2d3e4f'])
    expect(openElicitations(fixture('state-1-question.json'), 0)).toEqual([])
  })

  it('i problemi per tipo', () => {
    expect(problem(f('project'), null)).toEqual({ kind: 'required' })
    expect(problem(f('project'), 'BLOG')).toEqual({ kind: 'not_option' })
    expect(problem(f('project'), 'SHOP')).toBeNull()
    expect(problem(f('urgent'), null)).toBeNull()
    expect(problem(f('title'), 'ab')).toEqual({ kind: 'length', min: 3, max: 80 })
    expect(problem(f('estimate'), 'tre')).toEqual({ kind: 'not_number' })
    expect(problem(f('estimate'), '2,5')).toEqual({ kind: 'not_whole' })
    expect(problem(f('estimate'), '21')).toEqual({ kind: 'range', min: 1, max: 13 })
    expect(initial(f('urgent'))).toBe('false'); expect(initial(f('project'))).toBeNull()
  })

  it('il modulo compilato dà il content del comando di esempio …0830', () => {
    const values = { project: 'SHOP', urgent: 'true', title: 'Checkout total ignores the gift card', estimate: '3' }
    expect(ready(form, values)).toBe(true)
    expect(ready(form, { ...values, title: '' })).toBe(false)
    const sample = fixture('cmd-result-sample.json').cmd.find((c: { id: string }) => c.id.includes('0830'))
    expect(content(form, values)).toEqual(sample.content)
    expect(Object.keys(content(form, { project: 'SHOP', title: 'Abc', estimate: ' ' }))).toEqual(['project', 'title'])
  })
})
