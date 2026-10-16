import type { ChangedFile, ChangesCount, ChangesView, CmdResult, Hunk, Session, StepDiff } from './contract'

// Il diff dal telefono (contratto 1.72), porta di DiffRules.kt con gli stessi test: la riga chiusa di un passaggio, la
// pillola della testata, le etichette e i motivi dei file della vista «Modifiche», le righe numerate del diff aperto,
// «Mostra tutto» e l'esito delle letture.

/** Il segno meno tipografico della specifica: «+12 −3». */
export const MINUS = '\u2212'
/** Oltre questa lunghezza una riga si accorcia a schermo: una riga minimizzata da 50.000 caratteri non blocca la pagina. */
export const MAX_LINE_CHARS = 2000
/** Il rifiuto del PC quando git non risponde: comincia così. */
export const GIT_TIMEOUT = 'git did not answer in 5 s'
/** Oltre questa misura il PC manda solo nome, stato e conteggi. */
export const BIG_BYTES = 1_000_000

export const counts = (added: number, removed: number) => `+${added} ${MINUS}${removed}`
export const name = (path: string) => path.replace(/\/+$/, '').split('/').pop() ?? path
/** La cartella del file, null per un file nella radice. */
export function folder(path: string): string | null {
  const p = path.replace(/\/+$/, '')
  const i = p.lastIndexOf('/')
  return i > 0 ? p.slice(0, i) : null
}
/** La riga chiusa sotto un passaggio di modifica. */
export const stepLine = (d: StepDiff) => `${name(d.path)} · ${counts(d.added, d.removed)}`
/** La pillola della testata: anche per una sessione chiusa, la cartella c'è ancora; null = niente pillola. */
export const pill = (s: Session): ChangesCount | null => s.changes ?? null

export type Label = 'new' | 'deleted' | 'renamed' | 'binary'
const LABELS: Record<string, Label> = { added: 'new', deleted: 'deleted', renamed: 'renamed', binary: 'binary' }
/** «nuovo», «eliminato», «rinominato» o «binario»; un file solo modificato, o uno stato sconosciuto, nessuna. */
export const label = (status: string): Label | null => LABELS[status] ?? null
/** Mai i numeri per un binario, mai «+0 −0» per un rinominato senza righe cambiate. */
export const showCounts = (f: ChangedFile) => f.status !== 'binary' && !(f.status === 'renamed' && f.added === 0 && f.removed === 0)

export type Hidden = 'binary' | 'secret' | 'big' | 'renamed' | 'empty'
const SECRET = [/^\.env/, /\.pem$/, /\.key$/, /^id_/, /\.p12$/, /^credentials/]
/** Il nome sensibile della specifica, sul nome del file: `.env*`, `*.pem`, `*.key`, `id_*`, `*.p12`, `credentials*`. */
export function secret(path: string): boolean { const n = name(path); return SECRET.some(r => r.test(n)) }
/** Perché un file aperto non ha righe; null quando ne ha, o quando è solo tagliato (si apre intero con `file=`). */
export function hidden(f: ChangedFile): Hidden | null {
  if (f.hunks.length || f.cut) return null
  if (f.status === 'binary') return 'binary'
  if (secret(f.path)) return 'secret'
  if ((f.size ?? 0) > BIG_BYTES) return 'big'
  if (f.status === 'renamed' && f.added === 0 && f.removed === 0) return 'renamed'
  return 'empty'
}

export type Row = { kind: 'same' | 'added' | 'removed' | 'gap' | 'note'; old: number | null; new: number | null; text: string; more: number }
/** Le righe numerate, come DiffRules.rows: tab in quattro spazi, niente «\r» in fondo, oltre MAX_LINE_CHARS accorciate. */
export function rows(hunks: Hunk[]): Row[] {
  const out: Row[] = []
  hunks.forEach((h, i) => {
    if (i > 0) out.push({ kind: 'gap', old: null, new: null, text: '', more: 0 })
    let o = h.old_start, n = h.new_start
    for (const raw of h.lines) {
      const sign = raw[0]
      if (sign === '\\') { out.push({ kind: 'note', old: null, new: null, text: raw.slice(1).trim(), more: 0 }); continue }
      const body = raw.slice(1).replace(/\r$/, '').replace(/\t/g, '    ')
      const text = body.slice(0, MAX_LINE_CHARS)
      const more = body.length - text.length
      if (sign === '+') out.push({ kind: 'added', old: null, new: n++, text, more })
      else if (sign === '-') out.push({ kind: 'removed', old: o++, new: null, text, more })
      else out.push({ kind: 'same', old: o++, new: n++, text, more })
    }
  })
  return out
}
/** Quante cifre servono ai numeri di riga. */
export const gutter = (rs: Row[]) => String(rs.reduce((m, r) => Math.max(m, r.old ?? 0, r.new ?? 0), 0)).length

/** I file che la vista tagliata ha lasciato fuori dall'elenco: `totals` li conta tutti. */
export const hiddenFiles = (v: ChangesView) => Math.max(0, v.totals.files - v.files.length)
export type Source = 'git' | 'no_commits' | 'transcript'
export const source = (v: ChangesView): Source => (v.source === 'transcript' ? 'transcript' : v.base == null ? 'no_commits' : 'git')
/** Revisione finale (16/10): un file aperto si chiede al PC con `file=` solo se l'elenco l'ha tagliato; altrimenti le righe ci sono già. */
export const askFile = (f: ChangedFile) => f.cut
/** L'argomento di `changes` per un file solo. */
export const fileArg = (path: string) => `file=${path}`
/** Il file nella risposta di `changes file=`: per percorso, o l'unico; null = non ha più modifiche. */
export const alone = (v: ChangesView, path: string): ChangedFile | null => v.files.find(f => f.path === path) ?? (v.files.length === 1 ? v.files[0] : null)

/** Che cosa mostra un passaggio aperto: come DiffRules.shown. */
export type Shown = { diff: StepDiff; showAll: boolean; ask: boolean; beyond: boolean }
export const shown = (entry: StepDiff, full: StepDiff | null, wantAll: boolean, busy: boolean): Shown => ({
  diff: full ?? entry, showAll: entry.cut && !full && !wantAll, ask: wantAll && !full && !busy, beyond: !!full?.cut,
})

export type Why = 'down' | 'git' | 'other'
export type Load<T> = { ok: true; value: T } | { ok: false; why: Why; text: string }
/** L'esito di una lettura; `r` null = nessuna risposta: «PC non raggiungibile». */
export function read<T>(r: CmdResult | null, decode: (raw: string) => T): Load<T> {
  if (!r) return { ok: false, why: 'down', text: '' }
  if (!r.ok) return { ok: false, why: r.text.startsWith(GIT_TIMEOUT) ? 'git' : 'other', text: r.text }
  try { return { ok: true, value: decode(r.text) } } catch { return { ok: false, why: 'other', text: r.text.slice(0, 200) } }
}
