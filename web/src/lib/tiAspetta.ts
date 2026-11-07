// La scheda «Ti aspetta» (app-scheda-ti-aspetta-07-11, mockup approvato da Franz il 07/11 alle 21:04): la frase della domanda
// accesa nel pezzo di trascrizione e da quanto aspetta. La stessa regola di TiAspetta.kt, coi casi di
// core/src/test/resources/ti-aspetta-cases.json. Il modello della pagina segue quello di Kotlin; i dati arrivano col contratto.

export type Kind = 'decide' | 'choose' | 'verify' | 'answer'
export type Source = { session: string; at: number | null; text: string; ask: string | null; entry: string | null }
export type Other = { key: string; kind: Kind; title: string; session: string | null; since: number }
export type Page = { key: string; kind: Kind; session: string | null; since: number; source: Source | null; others: Other[] }
export type Seg = { text: string; on: boolean }

const esc = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/** Il testo con la frase della domanda accesa; maiuscole e spazi non contano. Senza frase o se non c'è, tutto spento. */
export function marks(text: string, ask: string | null): Seg[] {
  const words = (ask ?? '').trim().split(/\s+/).filter(w => w.length > 0)
  if (!words.length) return [{ text, on: false }]
  const m = new RegExp(words.map(esc).join('\\s+'), 'i').exec(text)
  if (!m) return [{ text, on: false }]
  const out: Seg[] = []
  if (m.index > 0) out.push({ text: text.slice(0, m.index), on: false })
  out.push({ text: m[0], on: true })
  const end = m.index + m[0].length
  if (end < text.length) out.push({ text: text.slice(end), on: false })
  return out
}

/** Da quanto aspetta: minuti (almeno 1), poi ore, poi giorni. */
export function age(seconds: number): string {
  if (seconds < 3600) return `${Math.max(1, Math.floor(seconds / 60))} min`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} h`
  return `${Math.floor(seconds / 86400)} g`
}
