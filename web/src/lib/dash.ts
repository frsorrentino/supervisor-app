import type { Attention, HostStatus, Project, Published, Session, State } from './contract'
import { locale } from './lang'
import { MASTER } from './summary'
import { t } from './t'

// Il cruscotto della plancia senza sessioni aperte, a tre colonne (proposta approvata da Franz il 21/10 alle 13:02,
// docs/proposte/2026-10-21-home-tre-colonne.md): cosa serve da te, il lavoro, numeri e strumenti. Qui solo i conti; i
// riquadri li disegna App.svelte riusando quelli che esistono.

/** Le colonne secondo la larghezza della finestra: 3 sopra i 1400 px, 2 da 1000, una sotto. */
export function dashColumns(width: number): 1 | 2 | 3 {
  return width >= 1400 ? 3 : width >= 1000 ? 2 : 1
}

/** «Ti aspettano»: le voci di `attention` (contratto 1.68), la più vecchia prima, con da quanto aspettano. */
export type Waiting = { session: string | null; title: string; kind: string; waitedS: number }
export function waitingFor(attention: Attention[] | null | undefined, now: number): Waiting[] {
  return [...(attention ?? [])].sort((a, b) => a.since - b.since)
    .map(a => ({ session: a.session ?? null, title: a.title, kind: a.kind, waitedS: Math.max(0, now - a.since) }))
}

/** Le sessioni per stato, la master esclusa (sta nella home); le chiuse di recente sono le ultime 5 chiuse, la più nuova prima. */
export type ByState = { busy: number; waiting: number; idle: number; closed: Session[] }
export function sessionsByState(sessions: Session[]): ByState {
  const live = sessions.filter(s => s.name !== MASTER)
  return {
    busy: live.filter(s => s.state === 'busy').length,
    waiting: live.filter(s => s.state === 'waiting' || s.state === 'awaiting').length,
    idle: live.filter(s => s.state === 'idle').length,
    closed: live.filter(s => s.state === 'gone').sort((a, b) => b.since - a.since).slice(0, 5),
  }
}

/** Il lancio rapido: i progetti usati di recente, al massimo 6, senza quelli che hanno già una sessione viva. */
export function recentProjects(st: Pick<State, 'projects' | 'sessions'>, max = 6): Project[] {
  // `sessions[].project` è relativo alla cartella delle sessioni, `projects[].path` assoluto.
  const live = st.sessions.filter(s => s.state !== 'gone').map(s => String(s.project))
  const taken = (p: Project) => live.some(x => p.path === x || p.path.endsWith('/' + x))
  return [...(st.projects ?? [])].filter(p => p.last_used != null && !taken(p))
    .sort((a, b) => (b.last_used ?? 0) - (a.last_used ?? 0)).slice(0, max)
}

/** Contratto 1.75: la riga dei numeri di un host acceso, «CPU 42 % · RAM 9,1 di 15,9 GB · disco 88,3 GB liberi di 271»;
 *  i pezzi che il relay manda null (un helper di prima della 1.75 non dà il disco) restano fuori. */
const gb = (b: number) => (b / 2 ** 30).toLocaleString(locale, { maximumFractionDigits: b >= 100 * 2 ** 30 ? 0 : 1 })
export function hostNumbers(h: HostStatus): string {
  const parts: string[] = []
  if (h.cpu != null) parts.push(t.dashCpu(Math.round(h.cpu * 100)))
  if (h.ram_used != null && h.ram_total != null) parts.push(t.dashRam(gb(h.ram_used), gb(h.ram_total)))
  if (h.disk_free != null && h.disk_total != null) parts.push(t.dashDisk(gb(h.disk_free), gb(h.disk_total)))
  return parts.join(' · ')
}

/** Le pubblicazioni di oggi come arrivano (la più nuova prima), col nome corto del progetto: l'ultimo pezzo della cartella. */
export type PublishedRow = Published & { name: string }
export function publishedRows(list: Published[] | null | undefined): PublishedRow[] {
  return (list ?? []).map(p => ({ ...p, name: p.project.split('/').filter(Boolean).pop() ?? p.project }))
}
