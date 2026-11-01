// Le etichette di una risposta in chat (Franz 01/11 21:24), porta di ChatLabels.kt. Solo se l'etichetta è esatta e a inizio riga, in grassetto
// o no, con o senza i due punti; «Non provato» senza grassetto e senza due punti vale solo da solo sulla riga. Dentro un blocco di codice non
// si guarda. Un messaggio senza etichette resta un pezzo solo, identico.
export type Part = { kind: 'plain'; text: string } | { kind: 'you'; title: string; body: string } | { kind: 'untested'; text: string }

const YOU = 'Da te|For you', NOT = 'Non provato|Not tested'
const HEAD = new RegExp(`^\\s*(?:\\*\\*|__)?(${YOU}):?(?:\\*\\*|__)?:?\\s*$`)
const NOT_BOLD = new RegExp(`^\\s*(?:\\*\\*|__)(?:${NOT}):?(?:\\*\\*|__)(?:\\s|:|$).*$`)
const NOT_PLAIN = new RegExp(`^\\s*(?:${NOT})(?::.*|\\s*)$`)
const ITEM = /^\s*(?:[-*•]|\d+[.)])\s+.*/
const FENCE = /^\s*```.*/
const blank = (s: string) => s.trim() === ''

export function splitLabels(text: string): Part[] {
  const lines = text.split('\n')
  const out: Part[] = []
  let plain: string[] = []
  const flush = () => {
    const t = plain.join('\n').replace(/^\n+|\n+$/g, '')
    if (!blank(t)) out.push({ kind: 'plain', text: t })
    plain = []
  }
  let fenced = false, i = 0
  while (i < lines.length) {
    const l = lines[i]
    if (FENCE.test(l)) { fenced = !fenced; plain.push(l); i++; continue }
    const head = fenced ? null : HEAD.exec(l)
    let next = i + 1
    while (next < lines.length && blank(lines[next])) next++
    if (head && next < lines.length && ITEM.test(lines[next])) {
      const body: string[] = []
      let j = next
      while (j < lines.length) {
        const b = lines[j]
        if (blank(b)) {
          let k = j + 1
          while (k < lines.length && blank(lines[k])) k++
          if (k < lines.length && ITEM.test(lines[k])) { j = k; continue }
          break
        }
        if (!ITEM.test(b) && !b.startsWith(' ') && !b.startsWith('\t')) break
        body.push(b); j++
      }
      flush(); out.push({ kind: 'you', title: head[1], body: body.join('\n') }); i = j; continue
    }
    if (!fenced && (NOT_BOLD.test(l) || NOT_PLAIN.test(l))) {
      const body = [l]
      let j = i + 1
      while (j < lines.length && !blank(lines[j]) && !FENCE.test(lines[j])) body.push(lines[j++])
      flush(); out.push({ kind: 'untested', text: body.join('\n') }); i = j; continue
    }
    plain.push(l); i++
  }
  flush()
  return out.every(p => p.kind === 'plain') ? [{ kind: 'plain', text }] : out
}

// I nomi dei compiti in chat (Franz 01/11 21:31), porta di ChatNames.kt.
export const TASK_URL = 'https://task.invalid/'
const NAME_FENCE = FENCE
const SKIP = /`[^`\n]+`|\[[^\]\n]+\]\([^)\s]+\)|https?:\/\/\S+/g
const escape = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/** Codice → nome breve, dai compiti dello stato; senza nome breve il codice resta. */
export function taskNames(commitments: { task: string; short?: string | null }[] | null | undefined): Map<string, string> {
  const m = new Map<string, string>()
  for (const c of commitments ?? []) { const s = c.short?.trim() ?? ''; if (s && s !== c.task && !/[[\]]/.test(s)) m.set(c.task, s) }
  return m
}

export function replaceNames(text: string, names: Map<string, string>): string {
  if (!names.size) return text
  const codes = new RegExp(`(?<![\\w-])(${[...names.keys()].sort((a, b) => b.length - a.length).map(escape).join('|')})(?![\\w-])`, 'g')
  const link = (c: string) => `[${names.get(c)}](${TASK_URL}${c})`
  const plain = (s: string) => s.replace(codes, c => link(c))
  let fenced = false
  return text.split('\n').map(line => {
    if (NAME_FENCE.test(line)) { fenced = !fenced; return line }
    if (fenced) return line
    let b = '', at = 0
    for (const m of line.matchAll(SKIP)) {
      b += plain(line.slice(at, m.index))
      const inner = m[0].startsWith('`') ? m[0].replace(/`/g, '') : null
      b += inner != null && names.has(inner) ? link(inner) : m[0]
      at = m.index + m[0].length
    }
    return b + plain(line.slice(at))
  }).join('\n')
}
