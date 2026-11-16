// La risposta da una voce (app-riall-1-dati-16-11): la voce esce subito da ogni vista (archivio unico), il comando del suo tasto
// parte; se il PC lo rifiuta, la voce torna. Come in «Per te» e nella scheda Ti aspetta.
import type { CmdResult, WaitItem } from './contract'
import { newCmd } from './transport'
import { cmdOf } from './tiAspetta'
import { markAnswered } from './attese.svelte'

export async function answerWait(run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>, w: WaitItem, index: number, written: string | null = null): Promise<boolean> {
  const a = w.actions?.[index]
  if (!a) return false
  const c = cmdOf(a, a.text_field ? written : null)
  markAnswered(w.id)
  const r = await run({ ...newCmd(c.op as Parameters<typeof newCmd>[0], c.session ?? null, c.arg ?? null), ...c } as ReturnType<typeof newCmd>)
  if (r?.ok) return true
  markAnswered(w.id, false)
  return false
}
