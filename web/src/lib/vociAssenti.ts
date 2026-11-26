import type { Question, Session, State } from './contract'
import { allowAllVisible } from './questionRules'

// Le voci che l'app non offre più quando il relay dice che non ci sono (app-issue4-cause-app-26-11, contratto 1.161, issue #4), porta
// di VociAssenti.kt: «Chat about this», «Scrivi», «Consenti sempre», la notte, Riapri, gli slash e Chiudi.

/** «Chat about this»: solo se c'è in fondo al dialogo; senza `foot` come prima. */
export const chat = (q: Question): boolean => q.foot == null || q.foot.includes('chat')
/** «Scrivi»: solo se il dialogo ha «Type something.»; senza `foot` come prima. */
export const type = (q: Question): boolean => q.foot == null || q.foot.includes('type')
/** «Consenti sempre»: le regole di sempre e `allow_all` che non dice false. */
export const allowAll = (q: Question): boolean => allowAllVisible(q)
/** La notte: `ops` ha `night` solo con un rapporto su disco; senza `ops` come prima. */
export const night = (ops: string[] | null | undefined): boolean => ops == null || ops.includes('night')
/** Riapri: mai su una sessione che c'è ed è aperta; una chiusa, o che lo stato non porta più, la decide il PC. */
export const reopen = (s: Session | null | undefined): boolean => s == null || s.state === 'gone'
/** Uno slash va solo a una sessione che c'è ed è aperta. */
export const slash = (st: State | null | undefined, name: string): boolean => !!st?.sessions?.some(s => s.name === name && s.state !== 'gone')
/** Chiudi: non su una sessione attaccata al PC né con un lavoro in sottofondo (`close_preview` → `blocked_by: background`). */
export const close = (s: Session, blockedBy: string | null = null): boolean => !s.attached && blockedBy !== 'background'
/** Il rifiuto del PC per del testo scritto a mano nella casella della sessione: il nome della sessione, o null. */
export function typedIn(text: string | null | undefined): string | null {
  const m = /casella di «([^»]+)» c'è del testo digitato/.exec(text ?? '')
  return m ? m[1] : null
}
