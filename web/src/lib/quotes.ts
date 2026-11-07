// Citare la trascrizione (app-citazione-trascrizione-07-11, Franz 07/11 20:06): i pezzi scelti con «Cita» o «Cita tutto» vanno
// alla sessione in Markdown, ogni riga con «> », una riga vuota fra un pezzo e l'altro e sotto la risposta. Un pezzo più lungo di
// MAX caratteri si taglia a fine parola con «…». La stessa regola di Quotes.kt; i casi in comune in core/src/test/resources/quote-cases.json.

export const MAX = 1000

export type Quote = { id: number; text: string; at: number | null }

const ws = /\s/

/** Il pezzo pulito e, se serve, tagliato: a fine parola se l'ultima parola sta negli ultimi 200 caratteri, se no secco. */
export function cut(text: string): string {
  const t = text.replace(/\r\n/g, '\n').trim()
  if (t.length <= MAX) return t
  const head = t.slice(0, MAX)
  let kept = head
  if (ws.test(t[MAX])) kept = head.trimEnd()
  else {
    let i = head.length - 1
    while (i >= 0 && !ws.test(head[i])) i--
    if (i > (MAX * 4) / 5) kept = head.slice(0, i).trimEnd()
  }
  return kept + '…'
}

/** Il testo da mandare: i pezzi citati, poi la risposta; senza pezzi, la risposta com'è scritta. */
export function markdown(quotes: string[], reply: string): string {
  const body = quotes.map(cut).filter(q => q.length > 0)
    .map(q => q.split('\n').map(l => (l.trim() === '' ? '>' : '> ' + l.trimEnd())).join('\n')).join('\n\n')
  const r = reply.replace(/\r\n/g, '\n').trim()
  return [body, r].filter(x => x.length > 0).join('\n\n')
}
