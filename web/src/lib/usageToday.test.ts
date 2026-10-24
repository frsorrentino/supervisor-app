import { describe, expect, it } from 'vitest'
import { usageRows } from './usageToday'

describe('Oggi · quota per sessione (contratto 1.84, B5)', () => {
  it('dal più grande; sotto il 3 % insieme in «altre N»; senza dati niente', () => {
    const r = usageRows([
      { session: 'master', account: 'personale', tokens: 140, share: 0.14 },
      { session: 'supervisor-app', account: 'personale', tokens: 340, share: 0.34 },
      { session: 'legnaia', account: 'personale', tokens: 20, share: 0.02 },
      { session: 'curriculum', account: 'personale', tokens: 10, share: 0.01 },
    ])
    expect(r).toEqual({ rows: [{ session: 'supervisor-app', pct: 34 }, { session: 'master', pct: 14 }], others: { n: 2, pct: 3 } })
    expect(usageRows([])).toBeNull()
    expect(usageRows(undefined)).toBeNull()
  })
  it('una sola voce piccola resta col suo nome', () => {
    expect(usageRows([{ session: 'master', account: 'p', tokens: 9, share: 0.9 }, { session: 'console', account: 'p', tokens: 1, share: 0.029 }]))
      .toEqual({ rows: [{ session: 'master', pct: 90 }, { session: 'console', pct: 3 }], others: null })
  })
})
