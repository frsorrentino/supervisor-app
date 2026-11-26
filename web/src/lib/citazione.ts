// Le citazioni nei messaggi (app-citazioni-estetica-26-11, Franz 26/11 15:46), porta di Citazione.kt: le righe di fila che cominciano
// con «>» diventano un blocco di citazione, senza il segno; il resto resta testo.
export type Part = { quote: boolean; text: string }
export const LINES = 3
const MARK = /^\s{0,3}>\s?/
export function parts(text: string): Part[] {
  const out: Part[] = []
  let buf: string[] = []
  let inQuote: boolean | null = null
  const flush = () => { const t = buf.join('\n').replace(/^\n+|\n+$/g, ''); if (inQuote != null && t.trim()) out.push({ quote: inQuote, text: t }); buf = [] }
  for (const line of text.split('\n')) {
    const q = MARK.test(line)
    if (inQuote !== q) { flush(); inQuote = q }
    buf.push(q ? line.replace(MARK, '') : line)
  }
  flush()
  return out
}
export const plain = (text: string): string => parts(text).map(p => p.text).join('\n\n')
