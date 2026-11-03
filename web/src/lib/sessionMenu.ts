// Il menu della sessione, uno solo per home, chat e plancia larga (Franz 03/11 15:58, app-menu-sessione-03-11: «allineiamo questi
// menu?»). Qui l'ordine e quali voci ci sono; testi e icone li mette chi disegna. Le voci vuote non compaiono. Gemello di SessionMenu.kt.
import type { Session, State } from './contract'
import { canClear, handoffPrompt } from './masterService'
import { MASTER } from './summary'

export type MenuKind = 'changes' | 'prove' | 'decidi' | 'guarda' | 'seguiti' | 'follow' | 'ask_master' | 'open_in_claude' | 'restart_clean' | 'exit'
/** Una voce; `n` è il numero accanto al titolo per le voci di Tocca a te. */
export type MenuItem = { kind: MenuKind; n?: number }
/** Le voci di Tocca a te che riguardano la sessione, nell'ordine della categoria (Prova, Decidi, Guarda, Segui). */
export type Mine = { prove?: number; decidi?: number; guarda?: number; seguiti?: number }

/** I risultati non visti della sessione, da `state.results` (al massimo 10 voci nello stato: un minimo, non il totale). */
export const unseenResults = (st: State, name: string): number => st.results?.items?.filter(r => r.session === name && r.seen_at == null).length ?? 0

/** `changes`: modifiche da mostrare che la riga delle pillole non tiene; `canExit`: il PC accetta /exit dall'app. */
export function sessionMenu(s: Session, st: State, mine: Mine = {}, opt: { changes?: boolean; canExit?: boolean } = {}): MenuItem[] {
  const live = s.state !== 'gone'
  const out: MenuItem[] = []
  if (opt.changes) out.push({ kind: 'changes' })
  for (const k of ['prove', 'decidi', 'guarda', 'seguiti'] as const) if ((mine[k] ?? 0) > 0) out.push({ kind: k, n: mine[k] })
  out.push({ kind: 'follow' })
  if (s.name !== MASTER && st.sessions.some(x => x.name === MASTER && x.state !== 'gone')) out.push({ kind: 'ask_master' })
  if (s.link?.trim()) out.push({ kind: 'open_in_claude' })
  // Riparti pulito: l'handoff col comando della sessione, poi /clear a turno finito (il «Handoff, poi /clear» del foglio del contesto).
  if (live && canClear(st) && handoffPrompt(st, s, c => c) != null) out.push({ kind: 'restart_clean' })
  if (live && opt.canExit) out.push({ kind: 'exit' })
  return out
}

/** Le icone delle voci nuove, nel tratto delle altre del menu (24×24, solo contorno). */
export const MENU_ICON: Partial<Record<MenuKind, string>> = {
  prove: 'M9 3h6M10 3v6l-5 9a2 2 0 0 0 1.7 3h10.6a2 2 0 0 0 1.7-3l-5-9V3M7 15h10',
  decidi: 'M10 2h4M12 14l3-3M12 22a8 8 0 1 0 0-16 8 8 0 0 0 0 16',
  guarda: 'M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6',
  seguiti: 'M21 12a9 9 0 1 1-3-6.7L21 8M21 3v5h-5M12 7v5l3 2',
  restart_clean: 'M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5',
}

/** I numeri di Tocca a te della sessione: nel web per ora solo i risultati non visti dallo stato. */
export const mineOf = (st: State, name: string): Mine => ({ guarda: unseenResults(st, name) })
