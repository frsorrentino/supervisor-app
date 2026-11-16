// La lista unica di ciò che aspetta la persona (app-riall-1-dati-16-11, contratto 1.151, Franz 16/11 20:39: passo 1 del
// riallineamento, docs/valutazioni/2026-11-16-chiarezza-sessione.md): Per te nella sessione, Decidi e Verifica in Dalla master, le
// chip delle sessioni e i conti leggono le stesse voci, così il numero sono le righe. Una voce risposta qui, o chiusa altrove
// (`state.attese.closed`), esce da ogni vista. Come Attese.kt, coi casi di contract/attese-rd-sample.json.
import type { WaitItem } from './contract'

export const isOpenItem = (w: Pick<WaitItem, 'state'>): boolean => (w.state ?? 'open') === 'open'
/** Decidi: le etichette decide e choose; Verifica: verify; Ti aspetta: reply. */
export const isDecide = (w: Pick<WaitItem, 'label'>): boolean => w.label === 'decide' || w.label === 'choose'
export const isVerify = (w: Pick<WaitItem, 'label'>): boolean => w.label === 'verify'
export const isReply = (w: Pick<WaitItem, 'label'>): boolean => w.label === 'reply'

/** Le voci da mostrare: aperte e non ancora risposte da qui né chiuse altrove. */
export const visible = (items: readonly WaitItem[], answered: ReadonlySet<string>): WaitItem[] =>
  items.filter(w => isOpenItem(w) && !answered.has(w.id))

export type Counts = { n: number; decidi: number; prove: number; rispondi: number }
/** I conti delle voci, di tutte o di una sessione: gli stessi di `state.tocca` e `sessions[].tocca`. */
export function counts(items: readonly WaitItem[], session?: string | null): Counts {
  const mine = session === undefined ? items : items.filter(w => (w.session ?? null) === session)
  return { n: mine.length, decidi: mine.filter(isDecide).length, prove: mine.filter(isVerify).length, rispondi: mine.filter(isReply).length }
}

/** Per le chip delle sessioni: per ognuna, le decisioni (`d`) e le verifiche (`v`). */
export function perSession(items: readonly WaitItem[]): Record<string, { d: number; v: number }> {
  const out: Record<string, { d: number; v: number }> = {}
  for (const w of items) {
    if (!w.session) continue
    const c = (out[w.session] ??= { d: 0, v: 0 })
    if (isDecide(w)) c.d++
    else if (isVerify(w)) c.v++
  }
  return out
}

/** Gli id delle voci chiuse da poco, da togliere subito da ogni vista. */
export const closedIds = (st: { attese?: { closed?: { id: string }[] | null } | null }): string[] => (st.attese?.closed ?? []).map(c => c.id)
