// La fase A del programma di Franz del 01/11 (contratto 1.99), porta di Decide.kt: decidere meno e meglio, regole pure.
import type { Approval, DecisionGroup, DecisionItem, DecisionsPage, ProveGroup, Session } from './contract'

/** L'argomento di «Tutte ok»: gli id mostrati, così una prova scritta nel frattempo non si conferma senza vederla. */
export const allOk = (g: ProveGroup): string => g.prove.map(p => p.id).join(',')

const SCREENS = ['queue', 'box-aspetta', 'box-lavoro', 'box-esiti', 'lavori', 'launch', 'night', 'search', 'settings', 'homeorder', 'usage', 'home']
/** Dove porta `dove`: una chiave libera; solo quelle che l'app conosce aprono una schermata. */
export const screen = (dove: string | null | undefined): string | null => { const d = dove?.trim() ?? ''; return SCREENS.includes(d) ? d : null }

/** Le prossime decisioni: l'ordine è quello del relay, da qui prima e le più pesanti; quelle già toccate escono. */
export const nextDecisions = (page: DecisionsPage, touched: Set<string>, howMany: number): DecisionGroup[] => page.groups.filter(g => !touched.has(g.key)).slice(0, howMany)
export const canDrop = (reason: string) => reason.trim() !== ''
export type Act = 'approve' | 'done' | 'none'
/**
 * Contratto 1.104 (Franz 02/11 08:04: «non ricordo cosa era stato proposto»), porta di CinqueMinuti.buttons: ok = Procedi e Parliamone; choice = le opzioni;
 * pc = Fatto, solo per ciò che si fa a mano al PC; other = Parliamone. Senza `asks`, un relay di prima: il tasto di sempre.
 */
export type Btn = { kind: 'proceed' } | { kind: 'discuss' } | { kind: 'done' } | { kind: 'works' } | { kind: 'broken' } | { kind: 'choice'; label: string }
/**
 * app-decidere-tasti-03-11 (Franz 03/11 14:22: «In da decidere non ci sono tasti per decidere»), porta di CinqueMinuti.buttons: una pausa che aspetta
 * una prova di Franz si decide con Va bene e Non va; ogni altra pausa o richiesta con Fatto e Parliamone. Parole intere: «provato» e «approvare» non sono una prova.
 */
const TEST = /(?<!\p{L})(prova|prove|guarda|try|tries)(?!\p{L})/iu
const waitsTest = (i: DecisionItem) => i.kind !== 'ok' && i.asks !== 'pc' && !(i.options?.length) && TEST.test(i.reason ?? '')
export function buttons(i: DecisionItem): Btn[] {
  const opts = (i.options ?? []).map(label => ({ kind: 'choice', label }) as Btn)
  if (waitsTest(i)) return [{ kind: 'works' }, { kind: 'broken' }]
  switch (i.asks) {
    case 'ok': return [{ kind: 'proceed' }, { kind: 'discuss' }]
    case 'choice': return opts.length ? opts : [{ kind: 'discuss' }]
    case 'pc': return [{ kind: 'done' }]
    case 'other': return act(i) === 'done' ? [{ kind: 'done' }, { kind: 'discuss' }] : [{ kind: 'discuss' }]
  }
  const a = act(i)
  return a === 'approve' ? (opts.length ? opts : [{ kind: 'proceed' }]) : a === 'done' ? [{ kind: 'done' }] : []
}
/** Una scelta senza opzioni non si decide da qui: la sessione dove si sceglie, che la card dice. Null per tutto il resto. */
export const chooseAt = (i: DecisionItem): string | null => (i.asks === 'choice' && !(i.options?.length) ? (i.session ?? i.task ?? null) : null)
/** Procedi e una scelta passano dall'ok del registro solo per una card; per una pausa le dice alla master. */
export const viaApprove = (i: DecisionItem) => i.kind === 'ok' && !!i.task
/** Il testo della card: la frase della master, la causa, poi per voce titolo intero, inizio del piano e motivo; mai lo stesso testo due volte. */
export type ItemText = { title: string | null; plan: string | null; reason: string | null }
export function card(g: DecisionGroup): { head: string[]; items: ItemText[] } {
  const seen = new Set<string>()
  const once = (x?: string | null) => { const v = x?.trim(); if (!v || seen.has(v.toLowerCase())) return null; seen.add(v.toLowerCase()); return v }
  const head = [once(g.note), once(g.title)].filter((x): x is string => !!x)
  return { head, items: g.items.map(i => ({ title: once(i.title ?? i.short ?? i.session ?? i.task), plan: once(i.plan_start), reason: once(i.reason ?? i.what) })) }
}
/** Cosa fa il tasto principale di una voce: un ok si approva, una pausa o una richiesta si dicono fatte. */
export function act(i: DecisionItem): Act {
  if (i.kind === 'ok') return i.task ? 'approve' : 'none'
  if (i.kind === 'pause') return i.task ? 'done' : 'none'
  if (i.kind === 'needs_you') return i.session ? 'done' : 'none'
  return 'none'
}

export type Undo = 'si' | 'no' | 'in-parte' | null
/** Le righe della card: quello che manca resta null e la card dice «non dichiarato», non si inventa. */
export function okLines(a: Approval) {
  return { untested: a.untested?.trim() || null, undo: (['si', 'no', 'in-parte'].includes(a.undo ?? '') ? a.undo : null) as Undo, undoHow: a.undo_how?.trim() || null, precedent: a.precedent?.task ? a.precedent : null }
}
/** Gli id da mandare con l'ok: quelli mostrati. Undefined se la card non ne ha, così il comando resta com'era. */
export const okAlso = (a: Approval): string[] | undefined => (a.also?.length ? a.also.map(x => x.task) : undefined)

/** Il testo a schermo (forma A, Franz 02/11): il prefisso lo dice l'icona; resta nel copia, che prende il testo intero. */
export function outcomeText(line: string, label: string): string {
  const x = [label, 'Esito'].find(k => line.startsWith(`${k}:`))
  return x ? line.slice(x.length + 1).trim() : line
}

/** Il testo sopra e l'ultima riga, se è l'esito. Solo l'ultima riga piena conta, scritta con l'etichetta. */
export function splitOutcome(text: string, label: string): { body: string; outcome: string | null } {
  const lines = text.trimEnd().split('\n')
  const last = lines[lines.length - 1]?.trim() ?? ''
  // Le sessioni scrivono «Esito:» anche quando l'app è in inglese.
  if (![label, 'Esito'].some(x => last.startsWith(`${x}:`) || last.startsWith(`${x} `))) return { body: text, outcome: null }
  return { body: lines.slice(0, -1).join('\n').trimEnd(), outcome: last }
}

export const headsUpKey = (session: string, text: string) => `${session}|${text}`
/** La nota da mostrare: quelle già viste su questo dispositivo restano nascoste finché il testo non cambia. */
export const visibleHeadsUp = (s: Pick<Session, 'name' | 'heads_up'>, dismissed: Set<string>) => (s.heads_up?.text?.trim() && !dismissed.has(headsUpKey(s.name, s.heads_up.text)) ? s.heads_up : null)

/** Il titolo della notte (Franz 01/11): a cavallo di due mesi ogni giorno dice il suo, non «November 31-1». Le date sono AAAA-MM-GG. */
export function nightTitleParts(before: string, day: string, name: (d: Date) => string): { dayA: number; monthA: string | null; dayB: number; monthB: string } {
  const a = new Date(`${before}T12:00:00Z`), b = new Date(`${day}T12:00:00Z`)
  return { dayA: a.getUTCDate(), monthA: a.getUTCMonth() !== b.getUTCMonth() ? name(a) : null, dayB: b.getUTCDate(), monthB: name(b) }
}

// Contratto 1.107 (Franz 03/11 15:32 e 17:12, app-sezioni-accorpate-03-11, scelta A): una pausa con `prova` aspetta che Franz
// provi qualcosa. Escono da Decidi e si fondono per progetto in una scheda sola, quella della pre-release più nuova, che contiene
// le precedenti: una riga per cosa, «Tutto va» manda `provato` per ogni gruppo. Gli stessi casi in Decide.kt (ProveFuse).
export type TestRow = { key: string; task: string | null; text: string; since: number }
export type TestCard = { project: string; release: string | null; web: string | null; since: number; rows: TestRow[] }
/** Un gruppo di prova: solo pause con `prova`. Un gruppo misto resta in Decidi: `provato` toccherebbe anche le altre pause. */
const isTestGroup = (g: DecisionGroup) => g.items.length > 0 && g.items.every(i => i.kind === 'pause' && !!i.prova)
export function testCards(page: DecisionsPage): TestCard[] {
  const by = new Map<string, { g: DecisionGroup; i: DecisionItem }[]>()
  for (const g of page.groups.filter(isTestGroup)) for (const i of g.items) {
    const p = i.prova?.project ?? i.project ?? ''
    by.set(p, [...(by.get(p) ?? []), { g, i }])
  }
  return [...by.entries()].map(([project, l]) => {
    const s = [...l].sort((a, b) => b.i.since - a.i.since)
    const release = s.find(x => x.i.prova?.release)?.i.prova?.release ?? null
    const web = s.find(x => x.i.prova?.web)?.i.prova?.web ?? null
    const rows = s.map(x => ({ key: x.g.key, task: x.i.task ?? null, text: x.i.short ?? x.i.title ?? x.i.task ?? x.i.reason ?? '', since: x.i.since }))
    return { project, release, web, since: s[0].i.since, rows }
  }).sort((a, b) => b.since - a.since)
}
/** Le chiavi da mandare con `provato` o `non_va`: una decisione per gruppo. */
export const cardKeys = (c: TestCard): string[] => [...new Set(c.rows.map(r => r.key))]
/** Decidi senza i gruppi di prova; conti rifatti sui gruppi rimasti. */
export function withoutTests(page: DecisionsPage): DecisionsPage {
  const groups = page.groups.filter(g => !isTestGroup(g))
  const gone = page.groups.filter(isTestGroup).reduce((n, g) => n + g.unblocks.length, 0)
  return { ...page, groups, n: groups.length, here: groups.filter(g => g.where === 'here').length, tasks: Math.max(0, page.tasks - gone) }
}
