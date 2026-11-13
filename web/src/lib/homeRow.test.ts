import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { GRIP_H, ROW_H, rowHeight, voices, type In, type Voice } from './homeRow'

// Gli stessi casi di HomeRowTest.kt.
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/home-row-cases.json', import.meta.url), 'utf8')) as {
  voices: { name: string; rows: In[]; shown: string[]; since: number | null; opened: string[]; out: Voice[] }[]
}

describe('Home in riga: le voci', () => {
  for (const c of file.voices) it(c.name, () => expect(voices(c.rows, c.shown, c.since, new Set(c.opened))).toEqual(c.out))
  it('vuota resta la maniglia, alta 28 px; con le voci 64', () => {
    expect([rowHeight([]), rowHeight(file.voices[0].out)]).toEqual([GRIP_H, ROW_H])
    expect([GRIP_H, ROW_H]).toEqual([28, 64])
  })
})
