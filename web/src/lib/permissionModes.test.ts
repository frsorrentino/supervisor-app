import { describe, expect, it } from 'vitest'
import type { Session, State } from './contract'
import { canChange, choices, glyph, homeGlyph, needsConfirm } from './permissionModes'

// Gli stessi casi di PermissionModesTest.
const s = (mode: string | null, bypass = false, state: Session['state'] = 'idle'): Session =>
  ({ id: '1', name: 'kb', account: 'personale', project: 'p', state, since: 0, permission_mode: mode, permission_bypass: bypass })

describe('modalità dei permessi', () => {
  it('i segni del terminale', () => {
    expect([null, 'default', 'plan', 'acceptEdits', 'auto', 'bypassPermissions', 'dontAsk'].map(glyph))
      .toEqual(['none', 'none', 'plan', 'forward', 'forward', 'bypass', 'unknown'])
  })
  it('nella home solo se diverso dal solito', () => {
    expect(homeGlyph(s('bypassPermissions', true))).toBe('none')
    expect(homeGlyph(s('plan', true))).toBe('plan')
    expect(homeGlyph(s('bypassPermissions'))).toBe('bypass')
  })
  it('«senza conferme» solo per chi è partita così', () => {
    expect(choices(s('default'))).toEqual(['plan', 'default', 'acceptEdits', 'auto'])
    expect(choices(s('plan', true))).toEqual(['plan', 'default', 'acceptEdits', 'auto', 'bypassPermissions'])
  })
  it('più autonomia chiede conferma, meno no', () => {
    expect(needsConfirm('plan', 'acceptEdits')).toBe(true)
    expect(needsConfirm(null, 'auto')).toBe(true)
    expect(needsConfirm('bypassPermissions', 'default')).toBe(false)
  })
  it('solo con un relay che conosce l\'op', () => {
    const st = { ops: ['prompt', 'permission_mode'] } as unknown as State
    expect(canChange(st, s('default'))).toBe(true)
    expect(canChange({ ops: ['prompt'] } as unknown as State, s('default'))).toBe(false)
    expect(canChange(st, s('default', false, 'gone'))).toBe(false)
  })
})
