// La scheda «Ti aspetta» (app-scheda-ti-aspetta-07-11, mockup approvato da Franz il 07/11 alle 21:04): la frase della domanda
// accesa nel pezzo di trascrizione e da quanto aspetta. La stessa regola di TiAspetta.kt, coi casi di
// core/src/test/resources/ti-aspetta-cases.json. Il modello della pagina segue quello di Kotlin; i dati arrivano col contratto.

export type Kind = 'decide' | 'choose' | 'verify' | 'answer'
export type Source = { session: string; at: number | null; text: string; ask: string | null; entry: string | null }
export type Other = { key: string; kind: Kind; title: string; session: string | null; since: number }
export type Page = { key: string; kind: Kind; session: string | null; since: number; source: Source | null; others: Other[] }
export type Seg = { text: string; on: boolean }

/** Contratto 1.131: una scelta numerata (option, o primary con recommended), il tasto pieno o un tasto tonale. Come TiAspetta.lookOf. */
export type Look = 'option' | 'main' | 'tonal'
export function lookOf(a: { style: string; recommended?: boolean }): Look {
  if (a.style === 'option' || (a.style === 'primary' && a.recommended)) return 'option'
  return a.style === 'primary' ? 'main' : 'tonal'
}

/** Il comando del tasto con il testo scritto dalla persona nel campo `text_field` (text, reason o arg). */
export function cmdOf<C extends Record<string, unknown>>(a: { cmd: C; text_field?: string | null }, written: string | null): C {
  return a.text_field ? { ...a.cmd, [a.text_field]: (written ?? '').trim() } : a.cmd
}

/**
 * La pagina a cui torna la scheda chiusa, con Indietro o dopo una risposta (Franz 08/11 21:38: «deve riportarmi alla schermata
 * precedente, da dove sono arrivato», app-ti-aspetta-lista-chiusa-08-11): quella aperta quando si è aperta la scheda; da un'altra
 * scheda resta la prima; aperta da un link, la home (null).
 */
export function backTo<P extends string>(kept: P | null, current: P | 'attesa' | null): P | null {
  return current === 'attesa' ? kept : current
}

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
