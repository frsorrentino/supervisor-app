import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import type { TranscriptEntry } from './contract'
import { bozzeRefs, letterOf, turnFiles } from './bozzeLinks'

// Gli stessi casi di BozzeLinksTest in Kotlin, dallo stesso file (app-bozze-link-19-11).
const cases = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/bozze-links-cases.json', import.meta.url), 'utf8')) as
  { name: string; text: string; files: string[]; links: { text: string; file: number }[] }[]

describe('le bozze citate', () => {
  for (const c of cases) it(c.name, () => {
    expect(bozzeRefs(c.text, c.files).map(r => ({ text: c.text.slice(r.start, r.end), file: r.file }))).toEqual(c.links)
  })
  it('la lettera viene dal nome del file', () => {
    expect(letterOf('docs/mockups/scheda-a.png')).toBe('A')
    expect(letterOf('C:\\x\\bozza_B.html')).toBe('B')
    expect(letterOf('schermo.png')).toBeNull()
  })
  it('i file del turno si fermano al messaggio della persona', () => {
    const e = (id: string, role: string, ...p: string[]) => ({ id, role, files: p.length ? p.map(path => ({ path })) : null }) as TranscriptEntry
    const l = [e('1', 'assistant', 'old-a.png'), e('2', 'user'), e('3', 'tool', 'scheda-a.png', 'note.md'), e('4', 'tool', 'scheda-b.html'), e('5', 'assistant')]
    expect(turnFiles(l, 4).map(x => [x.file.path, x.entry])).toEqual([['scheda-a.png', '3'], ['scheda-b.html', '4']])
    expect(turnFiles(l, 1)).toEqual([])
  })
})
