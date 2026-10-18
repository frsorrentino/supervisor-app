// La pagina Studio (spec del 18/10, approvata da Franz): le pratiche dello scadenziario come lente sulla master, con lo
// stesso dock della home. Il relay dà pratiche, scadenze ed esiti con l'op `studio`; qui le quattro sezioni.
import type { Session, StudioDeadline, StudioOutcome, StudioPage } from './contract'
import { MAX } from './nextSteps'

export type CaseView = { id: string; name: string; project: string; next: StudioDeadline | null; session: Session | null; last: StudioOutcome | null; open: boolean }
export type StudioStep = { label: string; send: string }
export type StudioModel = { deadlines: (StudioDeadline & { caseName: string | null })[]; open: CaseView[]; rest: CaseView[]; steps: StudioStep[]; recap: (StudioOutcome & { names: string[] })[] }

/** Un esito più vecchio di una settimana non tiene la pratica in evidenza. */
export const RECENT_S = 7 * 86400

/** «Prepara Ricorso al tribunale»: entro `max` caratteri, tagliato a fine parola, mai coi puntini. */
export function chip(prefix: string, title: string, max = 40): string {
  const full = `${prefix} ${title}`.trim()
  if (full.length <= max) return full
  const cut = full.slice(0, max + 1)
  const space = cut.lastIndexOf(' ')
  return (space > prefix.length ? cut.slice(0, space) : full.slice(0, max)).trimEnd()
}

/** Il blocco `[studio]…[/studio]` che il relay antepone al prompt non si mostra in chat. */
export function stripScope(text: string): string {
  const m = /^\[studio\]\n[\s\S]*?\n\[\/studio\]\n+/.exec(text)
  return m ? text.slice(m[0].length) : text
}

/**
 * Le quattro sezioni. Più pratiche possono stare nella stessa cartella: la sessione accesa lì vale per tutte, l'esito
 * entra nel recap una volta sola con i nomi di tutte. In evidenza chi ha una scadenza in avviso, una sessione o un esito
 * dell'ultima settimana, prima la scadenza più vicina; le altre restano chiuse sotto.
 */
export function studioModel(page: StudioPage, sessions: Session[], now: number, step: (d: StudioDeadline, caseName: string | null) => StudioStep): StudioModel {
  const nameOf = new Map(page.cases.map(c => [c.id, c.name]))
  const deadlines = [...page.deadlines].sort((a, b) => a.days_left - b.days_left).map(d => ({ ...d, caseName: nameOf.get(d.case) ?? null }))
  const views: CaseView[] = page.cases.map(c => {
    const next = deadlines.find(d => d.case === c.id) ?? null
    const session = sessions.filter(s => s.project === c.project && s.state !== 'gone').sort((a, b) => b.since - a.since)[0] ?? null
    const last = page.outcomes.filter(o => o.project === c.project).sort((a, b) => b.at - a.at)[0] ?? null
    return { ...c, next, session, last, open: !!next?.due || !!session || (!!last && now - last.at < RECENT_S) }
  })
  const left = (v: CaseView) => (v.next?.due ? v.next.days_left : Number.MAX_SAFE_INTEGER)
  const open = views.filter(v => v.open).map((v, i) => [v, i] as const).sort((a, b) => left(a[0]) - left(b[0]) || a[1] - b[1]).map(x => x[0])
  const steps = deadlines.filter(d => d.due).slice(0, MAX).map(d => step(d, d.caseName))
  const seen = new Set<string>()
  const recap = [...page.outcomes].sort((a, b) => b.at - a.at).filter(o => {
    const k = `${o.project}\n${o.at}`
    if (seen.has(k)) return false
    seen.add(k)
    return true
  }).map(o => ({ ...o, names: page.cases.filter(c => c.project === o.project).map(c => c.name) }))
  return { deadlines, open, rest: views.filter(v => !v.open), steps, recap }
}
