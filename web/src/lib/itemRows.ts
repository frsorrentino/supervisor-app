import type { AltroveRun, DecisionGroup, PrimaRaccoltaSection, Promemoria, ProveItem, ProvePage, RimastoItem, Suggestion } from './contract'
import { age, whenText } from './seguiti'
import { t } from './t'
import { emptySection } from './primaRaccolta'

// Le voci dentro le sezioni come righe di sessione (app-richiede-te-righe-02-11, mockup richiede-te.png punto 4), porta di
// ItemRows.kt: pallino di stato, nome, dato breve a destra, sotto cosa c'è; il tocco apre la scheda coi tasti di sempre.
export type Tone = 'wait' | 'busy' | 'good' | 'red' | 'still'
export type Kind = 'here' | 'at_pc' | 'unblocks' | 'build' | 'to_install' | 'reported' | 'red' | 'product' | 'checked' | 'at_time' | 'until' | 'filled' | 'empty' | 'kind'
export type Right = { kind: Kind; text?: string | null; n?: number }
// `icon`: il tipo di voce che sceglie l'icona al posto del pallino (Suggerimenti, app-suggerimenti-icone-03-11); assente = pallino.
export type Row = { key: string; name: string; sub: string | null; tone: Tone; right: Right[]; icon?: string }

/** Quante righe si vedono prima di «altre N»: con una sola in più si vedono tutte. */
export const SHOWN = 3
export const shown = (n: number, all: boolean) => (all || n <= SHOWN + 1 ? n : SHOWN)

const clean = (s: string | null | undefined) => { const t = s?.trim(); return t ? t : null }

/** Da decidere: il titolo e, sotto, la frase della master, la causa o il primo motivo; da qui in ambra, al PC grigio. */
export function decision(g: DecisionGroup): Row {
  const name = clean(g.title) ?? clean(g.note) ?? g.items.map(i => clean(i.title) ?? clean(i.short)).find(Boolean) ?? g.key
  const sub = [g.note, g.cause, ...g.items.flatMap(i => [i.reason ?? i.what, i.plan_start])].map(clean).find(t => t && t !== name) ?? null
  const pc = g.where === 'pc'
  return { key: g.key, name, sub, tone: pc ? 'still' : 'wait', right: [{ kind: pc ? 'at_pc' : 'here' }, ...(g.weight > 1 ? [{ kind: 'unblocks' as const, n: g.weight }] : [])] }
}

/** Da provare: in ambra se la versione c'è già, grigia se va prima installata; a destra la sessione (la versione è la testata del gruppo). */
export function prove(p: ProveItem, installed: boolean): Row {
  const who = clean(p.session) ?? clean(p.project)
  return { key: String(p.id), name: clean(p.short) ?? clean(p.title) ?? p.task, sub: clean(p.fai), tone: installed ? 'wait' : 'still', right: [...(installed ? [] : [{ kind: 'to_install' as const }]), ...(who ? [{ kind: 'product' as const, text: who }] : [])] }
}
export const proveKo = (k: ProvePage['ko'][number]): Row => ({ key: `ko-${k.id}`, name: clean(k.short) ?? k.task, sub: clean(k.text), tone: 'red', right: [{ kind: 'reported' }] })
export const proveRed = (r: ProvePage['red'][number]): Row => ({ key: `red-${r.task}`, name: clean(r.short) ?? r.task, sub: clean(r.reason), tone: 'red', right: [{ kind: 'red' }] })

/** Da seguire: ambra se è indietro, verde allineato, grigio se non si sa. */
export const rimasto = (i: RimastoItem): Row => ({
  key: `r-${i.product}-${i.key}`, name: clean(i.title) ?? i.key, sub: clean(i.where),
  tone: ({ behind: 'wait', aligned: 'good', unknown: 'still' } as const)[age(i)], right: [{ kind: 'product', text: i.product }],
})
export const altrove = (r: AltroveRun): Row => ({
  key: `a-${r.id}`, name: clean(r.text) ?? r.id, sub: clean(r.positive[0]) ?? clean(r.errors[0]),
  tone: r.running ? 'busy' : r.positive.length ? 'wait' : r.errors.length ? 'red' : 'good', right: [{ kind: 'checked', text: `${r.done}/${r.total}` }],
})
export const promemoria = (p: Promemoria): Row =>
  ({ key: `p-${p.id}`, name: clean(p.text) ?? p.id, sub: null, tone: 'still', right: [p.at_time != null ? { kind: 'at_time', text: String(p.at_time) } : { kind: 'until' }] })

/** Prima raccolta: verde quel che è raccolto, grigio quel che è vuoto (sotto, perché). */
export function prima(s: PrimaRaccoltaSection): Row {
  const empty = emptySection(s)
  return { key: s.key, name: s.key, sub: empty ? clean(s.why) ?? clean(s.when) : clean(s.text), tone: empty ? 'still' : 'good', right: [empty ? { kind: 'empty' } : { kind: 'filled', n: s.n }] }
}

/** Un testo in nome e resto: la prima riga o la prima frase (fino a «. », «: » o «? »); il resto, se c'è, va sotto. */
export function split(text: string): [string | null, string | null] {
  const t = text.trim().replace(/\s*\n\s*/g, '\n')
  if (!t) return [null, null]
  const nl = t.indexOf('\n')
  const m = /[.:?!] /.exec(t)
  const cuts = [nl >= 0 ? nl : null, m ? m.index + 1 : null].filter((x): x is number => x != null)
  if (!cuts.length) return [t, null]
  const cut = Math.min(...cuts)
  return [t.slice(0, cut).trim().replace(/:+$/, ''), clean(t.slice(cut).replace(/\n/g, ' '))]
}

/** Suggerimenti: tutti in ambra (chiedono una risposta); a destra il tipo. */
export function suggestion(s: Suggestion): Row {
  const [name, rest] = split(s.text)
  return { key: s.id, name: name ?? s.kind, sub: rest, tone: 'wait', right: [{ kind: 'kind', text: s.kind }], icon: s.kind }
}

/** Il dato a destra in parole, le parti separate da « · »; il tipo di un suggerimento col suo nome. */
export function rightText(r: Row): string {
  return r.right.map(p => {
    switch (p.kind) {
      case 'here': return t.decisionsHere
      case 'at_pc': return t.decisionsAtPc
      case 'unblocks': return t.decisionsUnblocks(p.n ?? 0)
      case 'build': case 'product': case 'checked': return p.text ?? ''
      case 'to_install': return t.itemToInstall
      case 'reported': return t.itemReported
      case 'red': return t.itemRed
      case 'at_time': return p.text ? whenText(Number(p.text)) : ''
      case 'until': return t.itemUntil
      case 'filled': return p.n ? String(p.n) : ''
      case 'empty': return t.itemEmpty
      case 'kind': return p.text ? t.sugKind[p.text] ?? t.sugKindOther : ''
    }
  }).filter(Boolean).join(' · ')
}
