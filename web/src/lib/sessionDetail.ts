import type { Session } from './contract'
import { summary } from './outcome'
import { okOf } from './queue'
import type { Group } from './summary'

// Porta di SessionDetail.kt (Franz 02/11 21:28, app-attesa-dettagli-02-11: «deve darmi maggiori dettagli sulla scelta da
// compiere, ora ci sono solo dati statistici»). Tutto dallo stato: l'ultimo messaggio è `outcome.full`, le scelte
// `next_steps`, il lavoro in corso `tool` e `tool_note`. Le statistiche restano, sotto.
export type Detail = { said: string | null; saidAt: number | null; steps: string[]; doing: string | null }

const TIPO = /^[\s*_>-]*Tipo\s*:/i

/** `shown` = il testo già in testa alla scheda (la domanda, il motivo o l'esito). */
export function detailOf(s: Session, group: Group, shown: string | null): Detail {
  const asks = group === 'waiting' && !s.question
  const o = asks ? s.outcome ?? null : null
  const raw = o ? summary(o).split('\n').filter(l => !TIPO.test(l)).join('\n').trim() : ''
  const said = raw && raw.toLowerCase() !== (shown ?? '').trim().toLowerCase() ? raw : null
  // La voce che ha già il suo «Ok» non si ripete; con «Fatto» (needs-you, «Da te») le voci di Prossimi sono tutte nuove.
  const ok = okOf(s)?.toLowerCase() ?? null
  const steps = asks ? (s.next_steps ?? []).map(n => n.text.trim()).filter(x => x && x.toLowerCase() !== ok) : []
  const inTurn = !s.outcome || (s.turn_started ?? 0) > s.outcome.at
  const doing = group === 'working' && inTurn && s.tool?.trim() ? [s.tool.trim(), s.tool_note?.trim()].filter(Boolean).join(' · ') : null
  return { said, saidAt: said ? o!.at : null, steps, doing }
}
