import type { ElicitField, Elicitation, State } from './contract'

// Contratto 1.83 (lavoro mcp-elicitation-lavoro-24-10, approvato da Franz): le domande dei server MCP, come Elicit.kt.
// I valori del modulo stanno come testo (choice = il value, boolean = "true"/"false", numeri come digitati) e diventano
// JSON solo all'invio. Il relay ricontrolla tutto: qui si ferma solo l'invio che sarebbe rifiutato di sicuro.
export type Problem =
  | { kind: 'required' } | { kind: 'length'; min?: number; max?: number } | { kind: 'not_number' } | { kind: 'not_whole' }
  | { kind: 'range'; min?: number; max?: number } | { kind: 'not_option' }
export type Values = Record<string, string | null | undefined>

/** Le domande ancora aperte: dopo `expires_at` la domanda è tornata al terminale e la scheda va via. */
export const openElicitations = (st: Pick<State, 'elicitations'> | null | undefined, now: number) => (st?.elicitations ?? []).filter(e => e.expires_at > now)

/** Il valore di partenza: il `default` del server, se c'è; mai inventato. */
export const initial = (f: ElicitField): string | null => (f.default == null ? null : String(f.default))

const num = (x: string) => { const n = Number(x.replace(',', '.')); return x.trim() === '' || Number.isNaN(n) ? null : n }

/** Cosa non va nel valore di un campo; null = va bene (vuoto e facoltativo va bene). */
export function problem(f: ElicitField, v: string | null | undefined): Problem | null {
  const x = (v ?? '').trim()
  if (!x) return f.required ? { kind: 'required' } : null
  if (f.kind === 'choice') return (f.options ?? []).some(o => o.value === x) ? null : { kind: 'not_option' }
  if (f.kind === 'boolean') return x === 'true' || x === 'false' ? null : { kind: 'not_option' }
  if (f.kind === 'text') return (f.min_length != null && x.length < f.min_length) || (f.max_length != null && x.length > f.max_length) ? { kind: 'length', min: f.min_length, max: f.max_length } : null
  if (f.kind === 'number' || f.kind === 'integer') {
    const n = num(x)
    if (n == null) return { kind: 'not_number' }
    if (f.kind === 'integer' && !Number.isInteger(n)) return { kind: 'not_whole' }
    if ((f.min != null && n < f.min) || (f.max != null && n > f.max)) return { kind: 'range', min: f.min, max: f.max }
  }
  return null
}

/** Si può mandare: ogni campo senza problemi. */
export const ready = (e: Elicitation, values: Values) => (e.fields ?? []).every(f => problem(f, values[f.key]) == null)

/** Il `content` dell'op `elicit`: i campi compilati, con il tipo giusto (numeri come numeri, sì/no come booleani). */
export function content(e: Elicitation, values: Values): Record<string, string | number | boolean> {
  const out: Record<string, string | number | boolean> = {}
  for (const f of e.fields ?? []) {
    const x = (values[f.key] ?? '').trim()
    if (!x) continue
    if (f.kind === 'boolean') out[f.key] = x === 'true'
    else if (f.kind === 'integer' || f.kind === 'number') { const n = num(x); if (n != null) out[f.key] = f.kind === 'integer' ? Math.trunc(n) : n }
    else out[f.key] = x
  }
  return out
}
