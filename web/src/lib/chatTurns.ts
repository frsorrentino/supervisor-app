import type { TranscriptEntry } from './contract'
import type { Item } from './chatFeed'
import { kind, short } from './toolText'

// La chat a scheda unica (app-chat-scheda-unica-13-11, Franz 13/11 09:32: E1 a quattro zone, separate come in F3), porta di
// ChatTurns.kt. Un turno è la richiesta e tutto quello che la sessione scrive fino alla richiesta dopo: il lavoro (le frasi
// intermedie e i passi, nell'ordine della trascrizione) e la risposta, l'ultimo messaggio se dopo non ci sono altri passi.
export type Request = Extract<Item, { type: 'mine' | 'user' }>
export type Step = { kind: 'say' | 'tool'; entry: TranscriptEntry }
export type Turn = { type: 'turn'; key: string; request: Request | null; work: Step[]; answer: TranscriptEntry | null }
export type Row = Turn | Extract<Item, { type: 'separator' }>

/** Le voci della chat (anche già raccolte in gruppi di passi) divise in turni; i separatori restano fra un turno e l'altro. */
export function turns(items: Item[]): Row[] {
  const out: Row[] = []
  let cur: Turn | null = null
  const step = (kind: Step['kind'], entry: TranscriptEntry) => {
    if (!cur) out.push(cur = { type: 'turn', key: `h-${entry.id}`, request: null, work: [], answer: null })
    cur.work.push({ kind, entry })
  }
  for (const it of items) {
    if (it.type === 'mine') out.push(cur = { type: 'turn', key: `m-${it.sent.id}`, request: it, work: [], answer: null })
    else if (it.type === 'user') out.push(cur = { type: 'turn', key: `u-${it.entry.id}`, request: it, work: [], answer: null })
    else if (it.type === 'separator') { out.push(it); cur = null }
    else if (it.type === 'steps') for (const e of it.entries) step('tool', e)
    else step(it.type === 'tool' ? 'tool' : 'say', it.entry)
  }
  for (const r of out) {
    if (r.type !== 'turn') continue
    const last = r.work.at(-1)
    if (last?.kind === 'say') { r.answer = last.entry; r.work = r.work.slice(0, -1) }
  }
  return out
}

/** La riga di una voce della chat, per «Nuovi»: l'indice del turno che la contiene, -1 se non c'è. */
export function rowOf(rows: Row[], it: Item): number {
  if (it.type === 'separator') return rows.indexOf(it)
  if (it.type === 'mine' || it.type === 'user') return rows.findIndex(r => r.type === 'turn' && r.request === it)
  const e = it.type === 'steps' ? it.entries[0] : it.entry
  return rows.findIndex(r => r.type === 'turn' && (r.answer === e || r.work.some(w => w.entry === e)))
}

/** Tutte le voci della trascrizione di un turno, per trovare quella cercata. */
export const entryIds = (r: Turn): string[] =>
  [r.request?.entry?.id, ...r.work.map(w => w.entry.id), r.answer?.id].filter((x): x is string => x != null)

/** Quanti passi (gli strumenti, non le frasi) e quanti falliti. */
export const toolCount = (work: Step[]): number => work.filter(w => w.kind === 'tool').length
export const failedCount = (work: Step[]): number => work.filter(w => w.kind === 'tool' && w.entry.error).length

/** Oltre `FOLD_OVER` voci il lavoro chiuso mostra solo le ultime `FOLD_TAIL`; il tocco sulla sua testata lo apre tutto. */
export const FOLD_OVER = 6
export const FOLD_TAIL = 3
export const foldable = (work: Step[]): boolean => work.length > FOLD_OVER
export const shownWork = (work: Step[], open: boolean): Step[] => (open || !foldable(work) ? work : work.slice(-FOLD_TAIL))

/**
 * La riga di un passo: il nome dello strumento e il suo argomento su una riga. Per i file le ultime due parti del percorso;
 * per un comando la sua descrizione (app-chat-bash-descrizione-13-11, Franz 13/11 10:47: «Bash: descrizione», il comando
 * all'apertura), e senza descrizione il comando; per gli altri la prima riga del testo, e senza testo la nota. `prose` = l'argomento
 * è una frase, non codice.
 */
export function stepLine(e: TranscriptEntry): { name: string; arg: string; prose: boolean } {
  const raw = (e.tool ?? '').trim()
  const name = short(raw.split(/[ (]/)[0]) || '?'
  const text = (e.text ?? '').trim()
  const k = kind(e.tool)
  if ((k === 'read' || k === 'edit' || k === 'write') && text.includes('/') && !text.includes('\n')) {
    const parts = text.split('/').filter(Boolean)
    return { name, arg: parts.slice(-2).join('/'), prose: false }
  }
  const said = e.note?.trim().split('\n')[0]
  if (k === 'run' && said) return { name, arg: said, prose: true }
  const first = text.split('\n')[0]
  return { name, arg: first || e.note?.trim() || '', prose: false }
}

/** Il dettaglio che il tocco apre: la nota e il testo intero, solo se dicono più della riga; di un comando la descrizione intera e poi il comando. */
export function stepDetail(e: TranscriptEntry): { note: string | null; text: string | null } {
  const line = stepLine(e)
  const text = (e.text ?? '').trim()
  const note = e.note?.trim() || null
  return { note: note && (line.prose || note !== line.arg) ? note : null, text: text && text !== line.arg ? text : null }
}
