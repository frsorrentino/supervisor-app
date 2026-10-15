import type { Session, State } from './contract'

// La modalità dei permessi, porta di PermissionModes.kt (mockup del 14/10, ok di Franz il 15/10 alle 07:39; contratto 1.69):
// ⏸ piano, ⏵⏵ modifiche automatiche e automatica, ⏵⏵ ambra senza conferme, ? sconosciuta; niente per «chiede sempre».
export type Glyph = 'none' | 'plan' | 'forward' | 'bypass' | 'unknown'

const ORDER = ['plan', 'default', 'acceptEdits', 'auto', 'bypassPermissions']

export function glyph(mode: string | null | undefined): Glyph {
  if (!mode || mode === 'default') return 'none'
  if (mode === 'plan') return 'plan'
  if (mode === 'acceptEdits' || mode === 'auto') return 'forward'
  if (mode === 'bypassPermissions') return 'bypass'
  return 'unknown'
}

/** Nella home il segno solo se diverso dal solito: «senza conferme» in una sessione partita così non si segna. */
export const homeGlyph = (s: Session): Glyph => { const g = glyph(s.permission_mode); return g === 'bypass' && s.permission_bypass ? 'none' : g }

export const canChange = (st: State | null | undefined, s: Session): boolean => !!st?.ops?.includes('permission_mode') && s.state !== 'gone'

/** I tasti del gruppo Permessi: «senza conferme» solo se la sessione ci può arrivare. */
export const choices = (s: Session): string[] => ORDER.filter(m => m !== 'bypassPermissions' || s.permission_bypass || s.permission_mode === 'bypassPermissions')

const rank = (m: string) => { const i = ORDER.indexOf(m); return i < 0 ? 1 : i }

/** Più autonomia di adesso: il tasto si tocca due volte; toglierne non chiede niente. */
export const needsConfirm = (current: string | null | undefined, wanted: string): boolean => rank(wanted) > rank(current || 'default')
