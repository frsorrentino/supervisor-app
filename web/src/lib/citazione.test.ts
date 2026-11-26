import { describe, expect, it } from 'vitest'
import { parts, plain } from './citazione'

// Le citazioni nei messaggi (app-citazioni-estetica-26-11): gli stessi casi di CitazioneTest in Kotlin.
describe('citazione', () => {
  it('citazione poi risposta', () => {
    const t = "> Per farla valere in tutte le sessioni conviene metterla nelle istruzioni globali\n> (CLAUDE.md): lo faccio se mi dai l'ok.\n\nok"
    expect(parts(t)).toEqual([
      { quote: true, text: "Per farla valere in tutte le sessioni conviene metterla nelle istruzioni globali\n(CLAUDE.md): lo faccio se mi dai l'ok." },
      { quote: false, text: 'ok' },
    ])
  })
  it('senza citazione', () => { expect(parts('solo testo')).toEqual([{ quote: false, text: 'solo testo' }]); expect(parts('')).toEqual([]) })
  it('testo, citazione, testo', () => {
    expect(parts('prima\n>citata\ndopo')).toEqual([{ quote: false, text: 'prima' }, { quote: true, text: 'citata' }, { quote: false, text: 'dopo' }])
    expect(plain('prima\n> citata\ndopo')).toBe('prima\n\ncitata\n\ndopo')
  })
})
