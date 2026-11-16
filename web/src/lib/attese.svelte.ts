// L'archivio unico delle voci che aspettano la persona (app-riall-1-dati-16-11, contratto 1.151): letto con l'op `attese` quando
// `state.attese.rev` cambia, e condiviso da tutte le viste. `answered`: le voci risposte da qui o chiuse altrove, fuori da ogni vista
// subito; una risposta rifiutata dal PC rimette la voce. La regola in attese.ts.
import type { CmdResult, WaitItem } from './contract'
import { decodeWaits, readPage } from './decideDecode'
import { newCmd } from './transport'
import { visible } from './attese'

export const waits = $state<{ items: WaitItem[]; rev: string | null; answered: Set<string> }>({ items: [], rev: null, answered: new Set() })

export const openWaits = (): WaitItem[] => visible(waits.items, waits.answered)

export function markAnswered(id: string, on = true) {
  if (on === waits.answered.has(id)) return
  const s = new Set(waits.answered)
  if (on) s.add(id); else s.delete(id)
  waits.answered = s
}

export function applyClosed(ids: readonly string[]) {
  if (ids.every(i => waits.answered.has(i))) return
  waits.answered = new Set([...waits.answered, ...ids])
}

/** Rilegge la lista; le risposte già uscite dalla lista non servono più. */
export async function readWaits(run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>) {
  const { page } = readPage(await run(newCmd('attese', null)), decodeWaits, null)
  if (!page) return
  waits.items = page.items
  waits.rev = page.rev ?? null
  const ids = new Set(page.items.map(w => w.id))
  if ([...waits.answered].some(i => !ids.has(i))) waits.answered = new Set([...waits.answered].filter(i => ids.has(i)))
}
