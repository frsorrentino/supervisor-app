import { describe, expect, it, vi } from 'vitest'
import type { Fetched } from './fileActions'
import { FileStore } from './fileStore'

const file: Fetched = { blob: new Blob(['ciao']), name: 'nota.txt', mime: 'text/plain' }
const key = (session: string) => `${session}\nnota.txt\u0000size=4`

// Franz, 16/10 15:59: «il condividi da pwa non funziona, dice sempre file pronto tocca di nuovo». Il foglio di condivisione
// vuole il tocco ancora vivo: col file già pronto navigator.share parte nello stesso giro del tocco, senza await prima.
describe('file della chat', () => {
  it('Condividi al primo tocco: col file preparato dall\'anteprima la condivisione parte senza await', async () => {
    const store = new FileStore()
    await store.get(key('master'), async () => file)
    const share = vi.fn()
    store.tap(key('master'), async () => { throw new Error('non deve riscaricare') }, f => share(f))
    expect(share).toHaveBeenCalledWith(file)
  })
  it('senza file pronto lo chiede una volta sola, e il secondo tocco parte subito', async () => {
    const store = new FileStore()
    const load = vi.fn(async () => file)
    const share = vi.fn()
    store.tap(key('master'), load, share)
    expect(share).not.toHaveBeenCalled()
    await store.tap(key('master'), load, share)
    expect(load).toHaveBeenCalledTimes(1)
    share.mockClear()
    store.tap(key('master'), load, share)
    expect(share).toHaveBeenCalledTimes(1)
  })
  it('resta pronto finché la chat è a schermo, anche dopo minuti', async () => {
    vi.useFakeTimers()
    const store = new FileStore()
    await store.get(key('pippo'), async () => file)
    await store.get(key('master'), async () => file)
    vi.advanceTimersByTime(30 * 60_000)
    store.keep(['master', 'pippo'])
    expect(store.peek(key('pippo'))).toBe(file)
    store.keep(['master'])
    expect(store.peek(key('pippo'))).toBeUndefined()
    expect(store.peek(key('master'))).toBe(file)
    vi.useRealTimers()
  })
  it('un file che non arriva non resta: il tocco dopo lo richiede', async () => {
    const store = new FileStore()
    expect(await store.get(key('master'), async () => null)).toBeNull()
    expect(await store.get(key('master'), async () => { throw new Error('rete') })).toBeNull()
    expect(await store.get(key('master'), async () => file)).toBe(file)
  })
})
