import { describe, expect, it } from 'vitest'
import { CHECK_MS, decide, isNewer, watchVersion, type Published } from './newVersion'

// La versione nuova (app-web-versione-nuova-11-11): si legge /version.json al ritorno e ogni 30 minuti, anche nascosta.
describe('versione nuova', () => {
  it('solo un altro commit pubblicato è una versione nuova; un file che manca non dice niente', () => {
    expect(isNewer('3371b908', { version: '0.2.2101', commit: '3e63b63e' })).toBe(true)
    expect(isNewer('3371b908', { version: '0.2.2101', commit: '3371b908' })).toBe(false)
    expect(isNewer('3371b908', null)).toBe(false)
    expect(isNewer('', { version: 'x', commit: 'abc' })).toBe(false)
  })
  it('senza niente in corso si ricarica, con un testo o un invio in corso si offre il tocco', () => {
    expect(decide(true, false)).toBe('reload')
    expect(decide(true, true)).toBe('offer')
    expect(decide(false, true)).toBe('stay')
  })

  const setup = (pub: Published | null, busy = false) => {
    const did: string[] = []
    let visible = () => {}, tick = () => {}, every = 0
    const check = watchVersion({
      mine: 'aaaa1111', fetchPublished: async () => pub, busy: () => busy,
      reload: () => did.push('reload'), offer: () => did.push('offer'),
      onVisible: (f) => { visible = f }, every: (f, ms) => { tick = f; every = ms },
    })
    return { did, check, visible: () => visible(), tick: () => tick(), every: () => every }
  }
  it('al ritorno in primo piano controlla e ricarica', async () => {
    const s = setup({ version: '0.2.2102', commit: 'bbbb2222' })
    s.visible(); await new Promise(r => setTimeout(r, 0))
    expect(s.did).toEqual(['reload'])
  })
  it('ogni 30 minuti controlla anche se la scheda non torna mai in primo piano; con un testo a metà offre il tocco', async () => {
    const s = setup({ version: '0.2.2102', commit: 'bbbb2222' }, true)
    expect(s.every()).toBe(CHECK_MS)
    s.tick(); await new Promise(r => setTimeout(r, 0))
    expect(s.did).toEqual(['offer'])
  })
  it('la stessa versione, o un file che non si legge, non fanno niente', async () => {
    await setup({ version: '0.2.2101', commit: 'aaaa1111' }).check()
    const s = setup(null)
    await s.check()
    expect(s.did).toEqual([])
  })
})
