// L'archivio unico delle voci che aspettano la persona (app-riall-1-dati-16-11, contratto 1.151): letto con l'op `attese` quando
// `state.attese.rev` cambia, e condiviso da tutte le viste. `answered`: le voci risposte da qui o chiuse altrove, fuori da ogni vista
// subito; una risposta rifiutata dal PC rimette la voce. La regola in attese.ts.
import type { CmdResult, ResultGroup, ResultsPage, WaitItem } from './contract'
import { decodeWaits, readPage } from './decideDecode'
import { newCmd } from './transport'
import { visible } from './attese'
import { groupsOf, type Gruppo } from './perTe'

export const waits = $state<{ items: WaitItem[]; rev: string | null; answered: Set<string> }>({ items: [], rev: null, answered: new Set() })

export const openWaits = (): WaitItem[] => visible(waits.items, waits.answered)

/** Voce 4 di app-animazioni-otto-26-11: l'ora dell'ultima risposta data da qui (ms). La cifra del tabellone scorre solo subito dopo. */
let gestureAt = 0
export const lastGestureAt = (): number => gestureAt

export function markAnswered(id: string, on = true) {
  if (on) gestureAt = Date.now()
  if (on === waits.answered.has(id)) return
  const s = new Set(waits.answered)
  if (on) s.add(id); else s.delete(id)
  waits.answered = s
}

/**
 * app-tieni-premuto-28-11: dopo un ok a un tocco la voce esce a spunta disegnata (`wait` ms), non subito, così il cerchio verde del tasto
 * si vede; senza attesa esce subito come prima. `undo` la rimette (il PC ha rifiutato), anche prima che sia uscita.
 */
export function answeredLater(id: string, wait: number): { undo: () => void } {
  let off = false
  const timer = wait > 0 ? setTimeout(() => { if (!off) markAnswered(id) }, wait) : null
  if (!timer) markAnswered(id)
  return { undo: () => { off = true; if (timer) clearTimeout(timer); markAnswered(id, false) } }
}

export function applyClosed(ids: readonly string[]) {
  if (ids.every(i => waits.answered.has(i))) return
  waits.answered = new Set([...waits.answered, ...ids])
}

/**
 * I risultati da guardare (contratto 1.106), letti una volta per tutte le viste quando lo stato dice che sono cambiati
 * (app-riall-2-viste-16-11): «Per te» nella sessione e Dalla master mostrano e contano le stesse righe.
 */
export const results = $state<{ groups: ResultGroup[] }>({ groups: [] })
export async function readResults(run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>) {
  const r = await run(newCmd('results', null))
  if (!r?.ok) return
  try { results.groups = (JSON.parse(r.text) as ResultsPage).groups ?? [] } catch { /* resta com'era */ }
}
/** Dalla master per sessione, senza le voci già risposte (perTe.ts). */
export const perTeGroups = (): Gruppo[] => groupsOf(waits.items, results.groups, waits.answered)

/** Rilegge la lista; le risposte già uscite dalla lista non servono più. */
export async function readWaits(run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>) {
  const { page } = readPage(await run(newCmd('attese', null)), decodeWaits, null)
  if (!page) return
  waits.items = page.items
  waits.rev = page.rev ?? null
  const ids = new Set(page.items.map(w => w.id))
  if ([...waits.answered].some(i => !ids.has(i))) waits.answered = new Set([...waits.answered].filter(i => ids.has(i)))
}
