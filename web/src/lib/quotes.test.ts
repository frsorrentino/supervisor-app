import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { MAX, markdown } from './quotes'

// Gli stessi casi di QuotesTest in Kotlin, dallo stesso file (app-citazione-trascrizione-07-11).
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/quote-cases.json', import.meta.url), 'utf8')) as {
  max: number; cases: { name: string; quotes: string[]; reply: string; out: string }[]
}

describe('quotes', () => {
  it('same max as the app', () => expect(MAX).toBe(file.max))
  for (const c of file.cases) it(c.name, () => expect(markdown(c.quotes, c.reply)).toBe(c.out))
})
