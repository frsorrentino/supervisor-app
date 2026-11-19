import type { TranscriptEntry, TranscriptFile } from './contract'

// Porta di BozzeLinks.kt (app-bozze-link-19-11): «le tre bozze», «la bozza», «bozza A», «bozze A e B» e le voci d'elenco «A, …:»
// diventano link alle immagini o agli HTML mandati nello stesso turno; una lettera apre il file che la porta nel nome.
export type BozzaRef = { start: number; end: number; file: number }

const WORD = /(?<!\p{L})(?:[Ll]e\s+(?:(?:due|tre|quattro|cinque|sei|sette|nuove|altre|prime)\s+)?|[Ll]a\s+|[Uu]na\s+)?[Bb]ozz[ae](?!\p{L})(?:\s+([A-H](?:\s*(?:,|\/|\be\b|\bed\b)\s*[A-H])*)(?![\p{L}\d]))?/gud
const HEAD = /^[ \t]*(?:[-*•][ \t]+)?([A-H])(?=[,:][ \t])([^:\n]{0,60}:)?/gmd
const VISUAL_EXT = new Set(['png', 'jpg', 'jpeg', 'webp', 'gif', 'svg', 'html', 'htm'])

export const visual = (f: TranscriptFile) => {
  if (f.mime && (f.mime.startsWith('image/') || f.mime === 'text/html')) return true
  const dot = f.path.lastIndexOf('.')
  return dot >= 0 && VISUAL_EXT.has(f.path.slice(dot + 1).toLowerCase())
}

/** La lettera di un file dal nome: `scheda-a.png`, `bozza_B.html`, `c.png`; null se non ne porta una. */
export function letterOf(path: string): string | null {
  const base = path.split(/[\\/]/).pop() ?? path
  const stem = base.includes('.') ? base.slice(0, base.lastIndexOf('.')) : base
  const m = /(?:^|[-_ .])([A-Ha-h])$/.exec(stem)
  return m ? m[1].toUpperCase() : null
}

/** I file visivi del turno di `index`: dei passi dall'ultimo messaggio della persona fino a `index` compreso. */
export function turnFiles(entries: TranscriptEntry[], index: number): { file: TranscriptFile; entry: string }[] {
  if (index < 0 || index >= entries.length || entries[index].role === 'user') return []
  let start = index
  while (start > 0 && entries[start - 1].role !== 'user') start--
  return entries.slice(start, index + 1).flatMap(e => (e.files ?? []).filter(visual).map(file => ({ file, entry: e.id })))
}

export function bozzeRefs(text: string, files: string[]): BozzaRef[] {
  if (!files.length) return []
  const letters = files.map(letterOf)
  const fileOf = (c: string | null) => (c == null ? 0 : letters.indexOf(c) >= 0 ? letters.indexOf(c) : null)
  const out: BozzaRef[] = []
  for (const m of text.matchAll(WORD)) {
    const g = m.indices![1]
    if (!g) { out.push({ start: m.index!, end: m.index! + m[0].length, file: 0 }); continue }
    let i = 0
    for (const x of m[1].matchAll(/[A-H]/g)) {
      const at = g[0] + x.index!
      const f = fileOf(x[0])
      if (f != null) out.push({ start: i === 0 ? m.index! : at, end: at + 1, file: f })
      i++
    }
  }
  for (const m of text.matchAll(HEAD)) {
    const f = fileOf(m[1])
    if (f == null) continue
    const [a] = m.indices![1]
    const end = m.indices![2] ? m.indices![2][1] - 1 : a + 1
    if (!out.some(o => o.start < end && a < o.end)) out.push({ start: a, end, file: f })
  }
  return out.sort((x, y) => x.start - y.start)
}

/** I file visivi del turno di una risposta e come aprirne uno, per il testo (Md). */
export type Bozze = { files: string[]; open: (i: number) => void }
