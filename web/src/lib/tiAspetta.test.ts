import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { age, marks } from './tiAspetta'
import { askedWait } from './sessionLinks'

// Gli stessi casi di TiAspettaTest in Kotlin, dallo stesso file (app-scheda-ti-aspetta-07-11).
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/ti-aspetta-cases.json', import.meta.url), 'utf8')) as {
  marks: { name: string; text: string; ask: string; out: [string, boolean][] }[]; age: { s: number; out: string }[]; links: { link: string; out: string | null }[]
}

describe('tiAspetta', () => {
  for (const c of file.marks) it(c.name, () => expect(marks(c.text, c.ask)).toEqual(c.out.map(([text, on]) => ({ text, on }))))
  it('links', () => { for (const c of file.links) expect(askedWait(new URL(c.link).hash)).toBe(c.out) })
  it('age', () => { for (const c of file.age) expect(age(c.s)).toBe(c.out) })
})
