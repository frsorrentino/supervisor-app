import { describe, expect, it } from 'vitest'
import { answerBlocks } from './answerText'

// Gli stessi casi di AnswerTextTest in Kotlin (Franz, 15/09 17:19: la risposta leggibile a paragrafi e ascoltabile per blocchi).
describe('answerBlocks', () => {
  it('paragrafi, elenchi, titoli e codice', () => {
    const t = 'Il preflight di release controlla.\nSeconda riga.\n\n- RL4 rosso\n- RL4b **verde**\n\n**Suite intera**\n' +
      'Non rilanciata `release.sh`.\n\n```\nbash release.sh\n```'
    expect(answerBlocks(t)).toEqual([
      { kind: 'para', text: 'Il preflight di release controlla. Seconda riga.' },
      { kind: 'bullet', text: 'RL4 rosso' },
      { kind: 'bullet', text: 'RL4b verde' },
      { kind: 'heading', text: 'Suite intera' },
      { kind: 'para', text: 'Non rilanciata `release.sh`.' },
      { kind: 'code', text: 'bash release.sh' },
    ])
  })
  it('titolo markdown', () => { expect(answerBlocks('## Esito')).toEqual([{ kind: 'heading', text: 'Esito' }]) })
  it('una riga rientrata continua la voce', () => { expect(answerBlocks('1. prima parte\n   seconda parte')).toEqual([{ kind: 'bullet', text: 'prima parte seconda parte' }]) })
  it('un testo vuoto non ha blocchi', () => { expect(answerBlocks('\n\n')).toEqual([]) })
  it('un blocco ```text è testo da leggere', () => { expect(answerBlocks('```text\nCiao Marco,\na domani\n```')).toEqual([{ kind: 'text', text: 'Ciao Marco,\na domani' }]) })
})
