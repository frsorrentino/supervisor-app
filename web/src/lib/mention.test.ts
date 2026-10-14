import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { parseMention, suggestMention } from './mention'

const st = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))

describe('@nome nella chat della master', () => {
  it('«@» propone le sessioni vive, filtrate da quel che segue', () => {
    expect(suggestMention('@atl', st.sessions, 'master').map(s => s.name)).toEqual(['atlas-shop'])
    expect(suggestMention('@atlas-shop run', st.sessions, 'master')).toEqual([])
    expect(suggestMention('hello @atl', st.sessions, 'master')).toEqual([])
  })
  it('il messaggio va alla sessione nominata', () => {
    expect(parseMention('@atlas-shop run the tests', st.sessions, 'master')).toEqual(['atlas-shop', 'run the tests'])
    expect(parseMention('@atlas-shop', st.sessions, 'master')).toBeNull()
    expect(parseMention('@nobody hi', st.sessions, 'master')).toBeNull()
  })
})
