import { describe, expect, it } from 'vitest'
import type { Session } from './contract'
import { isMaster, launchCard, masterOf, masterTarget, STARTING_S } from './masterLaunch'

const s = (name: string, state = 'idle', master?: boolean) => ({ name, state, ...(master == null ? {} : { master }) }) as unknown as Session
const now = 10_000

describe('la master (D9)', () => {
  it('senza il flag è quella che si chiama master', () => {
    expect(masterOf([s('kb'), s('master')])?.name).toBe('master')
    expect(masterOf([s('kb'), s('master', 'gone')])).toBeNull()
  })
  it('col flag vince il flag, anche con un altro nome', () => {
    const all = [s('master', 'idle', false), s('regia', 'idle', true)]
    expect(masterOf(all)?.name).toBe('regia')
    expect(isMaster(all[0], all)).toBe(false)
  })
  it('il tocco va alla master nota anche chiusa, altrimenti a master', () => {
    expect(masterTarget([s('kb'), s('regia', 'gone', true)])).toBe('regia')
    expect(masterTarget([s('kb')])).toBe('master')
  })
})

describe('la card «Avvia la master»', () => {
  it('con lo stato fresco un tocco la avvia', () => {
    expect(launchCard({ stale: false }, null, null, now)).toEqual({ kind: 'launch', error: null, button: true })
    expect(launchCard({ stale: false, slowS: 40 }, null, null, now).kind).toBe('launch')
  })
  it('con lo stato vecchio il PC è spento, senza tasto', () => {
    expect(launchCard({ stale: true, minutes: 4 }, now - 5, null, now)).toEqual({ kind: 'pc_off', error: null, button: false })
    expect(launchCard(null, null, null, now).kind).toBe('pc_off')
  })
  it('dopo il tocco si sta avviando, finché non scade la finestra', () => {
    expect(launchCard({ stale: false }, now - 10, null, now)).toEqual({ kind: 'starting', error: null, button: false })
    expect(launchCard({ stale: false }, now - STARTING_S, null, now).kind).toBe('launch')
  })
  it('un rifiuto mostra il testo del relay e si può riprovare', () => {
    expect(launchCard({ stale: false }, now - 10, 'master is not a closed session', now)).toEqual({ kind: 'failed', error: 'master is not a closed session', button: true })
  })
})
