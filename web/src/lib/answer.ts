// La risposta come documento (app-chat-scheda-unica-13-11), porta di Answer.kt: il testo diviso dove lo divide la sessione, cioè
// ai suoi titoli; dentro, paragrafi, voci d'elenco e blocchi di codice. Le parole restano quelle della sessione.
export type Section = { title: string; body: string }
export type Doc = { lead: string; secs: Section[] }
export type Rich = { type: 'p'; text: string } | { type: 'li'; mark: string; text: string; depth: number } | { type: 'code'; text: string }

const HEAD = /^#{1,6}\s+(.*)$/
const ITEM = /^(\s*)([-*•]|\d+[.)])\s+(.*)$/
const fence = (l: string) => l.trimStart().startsWith('```')
const clean = (lines: string[]) => lines.join('\n').replace(/^\n+|\s+$/g, '')

/** Quello che sta prima del primo titolo e una sezione per titolo; i `#` dentro un blocco di codice non sono titoli. */
export function sections(text: string): Doc {
  const lead: string[] = []
  const secs: { title: string; lines: string[] }[] = []
  let cur: string[] = lead
  let code = false
  for (const l of text.split('\n')) {
    if (fence(l)) code = !code
    const h = code ? null : HEAD.exec(l)
    if (h) { const s = { title: h[1].trim(), lines: [] as string[] }; secs.push(s); cur = s.lines } else cur.push(l)
  }
  return { lead: clean(lead), secs: secs.map(s => ({ title: s.title, body: clean(s.lines) })) }
}

/** Un testo senza titoli a pezzi: paragrafi (una riga vuota li separa), voci d'elenco col loro segno, blocchi di codice chiusi. */
export function rich(text: string): Rich[] {
  const out: Rich[] = []
  const lines = text.split('\n')
  let para: string[] = []
  const flush = () => { if (para.length) out.push({ type: 'p', text: para.join('\n') }); para = [] }
  let i = 0
  while (i < lines.length) {
    const l = lines[i]
    if (fence(l)) {
      let end = -1
      for (let k = i + 1; k < lines.length; k++) if (fence(lines[k])) { end = k; break }
      if (end >= 0) { flush(); const body = lines.slice(i + 1, end).join('\n'); if (body.trim()) out.push({ type: 'code', text: body }); i = end + 1; continue }
    }
    const m = ITEM.exec(l)
    const last = out.at(-1)
    if (m) { flush(); out.push({ type: 'li', mark: /\d/.test(m[2]) ? `${parseInt(m[2], 10)}.` : '•', text: m[3], depth: Math.min(3, Math.floor(m[1].length / 2)) }) }
    else if (!l.trim()) flush()
    // Una riga rientrata sotto una voce d'elenco continua la voce.
    else if (!para.length && last?.type === 'li' && /^\s+\S/.test(l) && lines[i - 1]?.trim()) last.text += `\n${l.trim()}`
    else para.push(l)
    i++
  }
  flush()
  return out
}

const CLOSING = (label: string) => new RegExp(`^\\s*(Esito|${label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}|Tipo|Prossimi|Watch)\\s*:`)
const TIPO = /^\s*Tipo\s*:\s*(.*)$/

/** La riga «Tipo: …» fra le righe di chiusura (Esito, Tipo, Prossimi) esce dal testo: è la pillola del piede. */
export function stripTipo(text: string, label = 'Esito'): { text: string; tipo: string | null } {
  const lines = text.split('\n')
  const closing = CLOSING(label)
  for (let i = lines.length - 1; i >= 0; i--) {
    if (!lines[i].trim()) continue
    if (!closing.test(lines[i])) break
    const m = TIPO.exec(lines[i])
    if (m) return { text: [...lines.slice(0, i), ...lines.slice(i + 1)].join('\n'), tipo: m[1].trim() || null }
  }
  return { text, tipo: null }
}
