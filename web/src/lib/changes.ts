import type { Cmd, CmdResult } from './contract'
import { read, type Load } from './diff'
import type { Transport } from './transport'

// Le letture della vista «Modifiche» e di «Mostra tutto» (contratto 1.72): il comando al PC e il suo esito per la vista,
// senza la riga dei rifiuti in basso (lo dice la vista). Senza trasporto, senza risposta o con la rete giù: «PC non raggiungibile».
export async function ask<T>(tr: Pick<Transport, 'send'> | null, c: Cmd, decode: (raw: string) => T): Promise<Load<T>> {
  let r: CmdResult | null = null
  if (tr) { try { r = await tr.send(c) } catch { r = null } }
  return read(r, decode)
}
