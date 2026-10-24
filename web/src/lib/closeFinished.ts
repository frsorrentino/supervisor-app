import type { Session } from './contract'
import type { Row } from './summary'

// «Chiudi le finite (N)» (Franz 24/10 19:46, B4 del mockup; contratto 1.84). Franz: «bisogna accertarsi che quando si
// chiude una sessione l'ultimo stato sia stato salvato». Candidate: le ferme senza finestra al PC, ferme da almeno un'ora;
// il relay rifà i controlli (al lavoro, domanda, lavori in sottofondo) e salva lo stato prima di chiudere.
export const IDLE_FOR_S = 3600
export const candidates = (rows: Row[], now: number): Session[] =>
  rows.filter(r => r.group === 'still' && r.session.state === 'idle' && !r.session.attached && now - r.session.since >= IDLE_FOR_S).map(r => r.session)

export type Saved = { outcome?: string | null; next?: string | null; session_id?: string | null; dirty_n?: number | null; dirty?: string[] | null }
export type PreviewRow = { name: string; closable: boolean; blocked_by?: 'busy' | 'question' | 'background' | 'attached' | string | null; save?: Saved | null }
export type Preview = { sessions: PreviewRow[] }

/** Il testo dell'op `close_preview`; null se non si legge. */
export function readPreview(text: string | null | undefined): Preview | null {
  try {
    const p = JSON.parse(text ?? '')
    return p && Array.isArray(p.sessions) ? { sessions: p.sessions.filter((x: PreviewRow) => typeof x?.name === 'string') } : null
  } catch { return null }
}

/** Le chiudibili, nell'ordine dell'anteprima. */
export const closable = (p: Preview): string[] => p.sessions.filter(x => x.closable).map(x => x.name)
