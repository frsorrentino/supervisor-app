import { describe, expect, it } from 'vitest'
import { ask } from './changes'
import { decodeChanges, decodeDiff, type Cmd, type CmdResult } from './contract'
import { newCmd } from './transport'

// Le letture della vista «Modifiche» e di «Mostra tutto» (contratto 1.72): sempre un esito per la vista, mai un'eccezione.
describe('ask', () => {
  const c = newCmd('changes', 'storefront', null)
  const reply = (r: Pick<CmdResult, 'ok' | 'text'>) => ({ send: async (x: Cmd): Promise<CmdResult> => ({ id: x.id, at: 1, ...r }) })
  it('senza trasporto, o col trasporto che cade: PC non raggiungibile', async () => {
    expect(await ask(null, c, decodeChanges)).toEqual({ ok: false, why: 'down', text: '' })
    expect(await ask({ send: async () => { throw new Error('timeout') } }, c, decodeChanges)).toEqual({ ok: false, why: 'down', text: '' })
  })
  it('git fermo e gli altri rifiuti del PC', async () => {
    expect(await ask(reply({ ok: false, text: 'git did not answer in 5 s' }), c, decodeChanges)).toMatchObject({ ok: false, why: 'git' })
    expect(await ask(reply({ ok: false, text: 'no entry a9 in the transcript' }), newCmd('diff', 'storefront', 'a9'), decodeDiff))
      .toEqual({ ok: false, why: 'other', text: 'no entry a9 in the transcript' })
  })
  it("i due comandi: changes con file= e diff con l'id della voce", () => {
    expect([newCmd('changes', 'storefront', 'file=src/checkout/totals.ts').arg, newCmd('diff', 'blog', 'w1').op]).toEqual(['file=src/checkout/totals.ts', 'diff'])
  })
})
