import type { Session } from './contract'
import { MASTER, type Group } from './summary'
import { stop } from './strongAlert'

// Il menu delle sessioni in alto, porta di SessionsMenu.kt: le sessioni vive nei gruppi del riepilogo, nell'ordine
// ricevuto; la master no; le chiuse solo contate.
export type SessionsMenuModel = { groups: [Group, Session[]][]; closed: number }

export function of(sessions: Session[]): SessionsMenuModel {
  const live = sessions.filter(s => s.state !== 'gone' && s.name !== MASTER)
  // Contratto 1.68: chi è ferma per Franz sta con chi ti aspetta, come nella home e nel banner (15/10: il menu la metteva al lavoro).
  const group = (s: Session): Group => (s.question != null || stop(s) != null ? 'waiting' : s.state === 'busy' || s.state === 'awaiting' ? 'working' : 'still')
  return {
    groups: (['waiting', 'working', 'still'] as const).flatMap(g => { const l = live.filter(s => group(s) === g); return l.length ? [[g, l] as [Group, Session[]]] : [] }),
    closed: sessions.filter(s => s.state === 'gone').length,
  }
}
