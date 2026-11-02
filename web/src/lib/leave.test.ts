import { describe, expect, it } from 'vitest'
import { leave } from './leave'

// Franz 02/11 15:38: «quando seleziono ok si ferma tutto per un attimo e poi sparisce». Il segno arriva al tocco, il comando
// parte subito, la scheda esce dopo la conferma e non dopo il PC; se il PC dice no, torna.
const rec = () => {
  const log: string[] = []
  return { log, h: { pick: (ids: string[], v: string | null) => log.push(`pick ${ids.join(',')} ${v}`), hide: (ids: string[], v: boolean) => log.push(`hide ${ids.join(',')} ${v}`) } }
}
const deferred = () => { let res!: (v: boolean) => void; const p = new Promise<boolean>(r => (res = r)); return { p, res } }

describe('leave', () => {
  it('segno subito, comando subito, uscita dopo la conferma senza aspettare il PC', async () => {
    const { log, h } = rec(); const d = deferred(); let waited = false
    const done = leave(['7'], 'ok', () => { log.push('send'); return d.p }, h, async () => { waited = true })
    await new Promise(r => setTimeout(r, 0))
    expect(waited).toBe(true)
    expect(log).toEqual(['pick 7 ok', 'send', 'hide 7 true'])
    d.res(true)
    expect(await done).toBe(true)
    expect(log).toEqual(['pick 7 ok', 'send', 'hide 7 true'])
  })
  it('il PC dice no: la scheda torna e il tasto perde la conferma', async () => {
    const { log, h } = rec()
    expect(await leave(['7', '8'], 'ok', () => new Promise<boolean>(r => setTimeout(() => r(false), 5)), h, async () => {})).toBe(false)
    expect(log.slice(-2)).toEqual(['hide 7,8 false', 'pick 7,8 null'])
  })
  it('un no arrivato durante la conferma: la scheda non esce affatto', async () => {
    const { log, h } = rec(); const d = deferred()
    const done = leave(['7'], 'ok', () => d.p, h, async () => { d.res(false); await Promise.resolve() })
    expect(await done).toBe(false)
    expect(log).toEqual(['pick 7 ok', 'pick 7 null'])
  })
})
