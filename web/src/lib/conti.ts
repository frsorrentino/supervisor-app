import { locale } from './lang'

// app-rendiconti-2-02-11 (programma delle otto proposte, fase C, approvato da Franz il 01/11 alle 20:35), contratto 1.103: `conti`
// legge il mese (arg «AAAA-MM», null = il corrente) coi clienti e, con `project`, il dettaglio di uno; `conti_azione` chiede la bozza
// del rendiconto alla master. La pagella sta nella stessa pagina: per modello e tipo di lavoro, quanti casi e quanti difetti.
// Porta: ContiRules.kt nell'app.
export type ContiCliente = { project: string; name: string; n: number; draft_at?: number | null }
export type ContiDettaglio = {
  project: string; name: string; mese: string; measured: { tasks: number; commits: number; releases: number }
  tasks: { task: string; title: string; at: number; outcome_kind?: string | null }[]
  commits: { hash: string; at: number; subject: string }[]; commits_more?: boolean
  releases: { tag: string; at: number }[]; git?: boolean
}
export type PagellaRow = { model: string; kind: string; n: number; enough: boolean; defects?: number | null; rate?: number | null; efforts?: Record<string, number> }
/**
 * app-pagella-effort-28-11 (nota di win-supervisor, 28/11): dal contratto 1.103 un consiglio della pagella è un oggetto, cioè per un tipo
 * di lavoro il modello che sbaglia meno e quello che sbaglia di più, coi tassi di difetti da 0 a 1. Una stringa resta testo.
 */
export type PagellaAdvice = { kind: string; better: string; worse: string; rate_better?: number | null; rate_worse?: number | null }
export type Pagella = { since: number; min: number; rows: PagellaRow[]; advice?: (PagellaAdvice | string)[]; unknown_model?: number }
export type ContiPage = { at: number; mese: string; mesi: string[]; clienti: ContiCliente[]; dettaglio?: ContiDettaglio | null; pagella?: Pagella | null }

export function decodeConti(raw: string): ContiPage {
  const p = JSON.parse(raw) as ContiPage
  if (typeof p.mese !== 'string' || !Array.isArray(p.clienti)) throw new Error('conti')
  return { ...p, mesi: p.mesi ?? [p.mese] }
}

/** «settembre 2026» da «2026-09», nella lingua dell'app. */
export function monthName(mese: string): string {
  const [y, m] = mese.split('-').map(Number)
  return new Date(y, m - 1, 15).toLocaleDateString(locale, { month: 'long', year: 'numeric' })
}

/** La pagella mostra solo le righe con abbastanza casi (da `min`, 10): una percentuale su 6 casi non dice niente. Dalla più usata. */
export function pagellaRows(p: Pagella | null | undefined): PagellaRow[] {
  if (!p) return []
  return p.rows.filter(r => r.enough && r.n >= p.min).sort((a, b) => b.n - a.n || a.model.localeCompare(b.model))
}

/** «Opus 5.5» da «claude-opus-5-5»: il nome come lo dice Franz. */
export function modelName(id: string): string {
  const m = /^claude-([a-z]+)-(\d+)(?:-(\d+))?/.exec(id)
  if (!m) return id
  return `${m[1][0].toUpperCase()}${m[1].slice(1)} ${m[2]}${m[3] ? `.${m[3]}` : ''}`
}

/** «14 %» dei casi con un difetto, arrotondato. */
export const ratePct = (r: PagellaRow) => (r.rate == null ? null : Math.round(r.rate * 100))
const pct = (r: number | null | undefined) => (r == null ? null : Math.round(r * 100))
type AdviceWords = { pagellaKind: Record<string, string>; pagellaAdvice: (k: string, b: string, rb: number | null, w: string, rw: number | null) => string }
/** Un consiglio della pagella detto come frase, nella lingua di [w]; una stringa com'è. */
export function adviceText(a: PagellaAdvice | string, w: AdviceWords): string {
  if (typeof a === 'string') return a
  const k = w.pagellaKind[a.kind] ?? a.kind
  return w.pagellaAdvice(k.charAt(0).toUpperCase() + k.slice(1), modelName(a.better), pct(a.rate_better), modelName(a.worse), pct(a.rate_worse))
}
