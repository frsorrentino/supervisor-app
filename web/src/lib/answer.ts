import { parse } from './markdown'

// La risposta come documento (app-chat-scheda-unica-13-11), porta di Answer.kt: il testo diviso dove lo divide la sessione, cioè
// ai suoi titoli; dentro, paragrafi, voci d'elenco e blocchi di codice. Le parole restano quelle della sessione.
export type Section = { title: string; body: string }
export type Doc = { lead: string; secs: Section[] }
/** `quote` = righe di fila che cominciano con «>» (un testo da mandare a qualcuno): `text` è il markdown di dentro, senza i «>». */
export type Rich = { type: 'p'; text: string } | { type: 'li'; mark: string; text: string; depth: number } | { type: 'code'; text: string } | { type: 'quote'; text: string }

const HEAD = /^#{1,6}\s+(.*)$/
const ITEM = /^(\s*)([-*•]|\d+[.)])\s+(.*)$/
const QUOTE = /^\s{0,3}> ?/
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

/** Un testo senza titoli a pezzi: paragrafi (una riga vuota li separa), voci d'elenco col loro segno, blocchi di codice chiusi, citazioni. */
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
    // Una citazione (app-chat-testo-da-inviare-13-11): le righe con «>» di fila, senza il segno.
    if (QUOTE.test(l)) {
      flush()
      const q: string[] = []
      while (i < lines.length && QUOTE.test(lines[i])) q.push(lines[i++].replace(QUOTE, ''))
      out.push({ type: 'quote', text: q.join('\n') })
      continue
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

/** Una riga senza i segni del markdown: grassetto, corsivo e codice senza apici; un link col testo diverso dall'indirizzo lo porta fra parentesi. */
function plainInline(src: string): string {
  const md = parse(src)
  let out = ''
  let at = 0
  for (const l of md.spans.filter(x => x.kind === 'link').sort((a, b) => a.start - b.start)) {
    if (l.start < at) continue
    const label = md.text.slice(l.start, l.end)
    out += md.text.slice(at, l.end) + (l.url && label !== l.url ? ` (${l.url})` : '')
    at = l.end
  }
  return out + md.text.slice(at)
}

/**
 * Il testo di una citazione pronto da incollare in una mail (il tasto «Copia» del suo box): senza «>» né segni del markdown, i
 * numeri dell'elenco e gli a capo al loro posto, una riga vuota fra un pezzo e l'altro (le voci di un elenco restano di fila).
 */
export function copyText(text: string): string {
  const parts = rich(text)
  let out = ''
  parts.forEach((p, i) => {
    const lines = (x: string) => x.split('\n').map(plainInline).join('\n')
    const s = p.type === 'p' ? lines(p.text) : p.type === 'li' ? `${'  '.repeat(p.depth)}${p.mark === '•' ? '-' : p.mark} ${lines(p.text)}` : p.type === 'code' ? p.text : copyText(p.text)
    if (i > 0) out += p.type === 'li' && parts[i - 1].type === 'li' ? '\n' : '\n\n'
    out += s
  })
  return out
}
