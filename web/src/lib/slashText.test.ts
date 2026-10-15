import { describe, expect, it } from 'vitest'
import { known, readable, readableWaiting } from './slashText'

const P = { clear: 'svuotare il contesto', compact: 'compattare la conversazione', login: 'rifare il login', resume: 'riprendere la conversazione', exit: 'chiudere la sessione', context: 'vedere il contesto', cost: 'vedere il costo' }

// Gli stessi casi di SlashTextTest in Kotlin.
describe('comandi leggibili nello sblocco', () => {
  it('un comando noto diventa una frase col comando fra parentesi', () => {
    expect(readable('/clear', P)).toBe('svuotare il contesto (/clear)')
    expect(readable(' /compact ', P)).toBe('compattare la conversazione (/compact)')
    expect(readable('/resume abc', P)).toBe('riprendere la conversazione (/resume abc)')
  })
  it('il resto resta com\'è', () => {
    expect(readable('ok to publish the post', P)).toBe('ok to publish the post')
    expect(readable('/deploy', P)).toBe('/deploy')
    expect(known('run /clear later')).toBeNull()
  })
  it('le righe «Aspetta» del rapporto', () => {
    expect(readableWaiting('!: /clear', P)).toBe('!: svuotare il contesto (/clear)')
    expect(readableWaiting('domanda: Pubblico?', P)).toBe('domanda: Pubblico?')
  })
})
