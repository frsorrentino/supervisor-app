import { reducedMotion } from './motion'

// Un'azione che toglie un elemento (Franz 02/11 15:38: «non voglio elementi che scompaiono o che non danno alcun feedback»).
// Voce 11 e voce 7 delle animazioni del 31/10, in un solo ordine per tutta l'app: al tocco il tasto scelto diventa la conferma
// (`pick`), il comando parte subito, dopo la conferma l'elemento esce (`hide`, con la sua transizione) e chi sta sotto sale.
// Non si aspetta il PC per uscire: il blocco di un attimo era quello. Se il PC dice no, l'elemento torna e il tasto si spegne.

/** Quanto resta in vista la conferma sul tasto prima che l'elemento esca. */
export const confirmMs = (): number => (reducedMotion() ? 0 : 350)

export type LeaveHooks = { pick: (ids: string[], choice: string | null) => void; hide: (ids: string[], hidden: boolean) => void }

export async function leave(
  ids: string[], choice: string, send: () => Promise<boolean>, h: LeaveHooks,
  wait: (ms: number) => Promise<void> = (ms) => new Promise(r => setTimeout(r, ms)),
): Promise<boolean> {
  h.pick(ids, choice)
  let settled: boolean | null = null
  const sent = send().then(ok => (settled = ok), () => (settled = false))
  await wait(confirmMs())
  if (settled === false) { h.pick(ids, null); return false }
  h.hide(ids, true)
  const ok = await sent
  if (!ok) { h.hide(ids, false); h.pick(ids, null) }
  return ok
}
