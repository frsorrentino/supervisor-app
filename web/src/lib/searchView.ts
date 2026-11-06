// La pagina Cerca (app-cerca-pagina-03-11, mockup del 03/11, scelta «Così» di Franz), come SearchView in Kotlin: prima di scrivere
// le ricerche recenti e i filtri; i risultati raggruppati per sessione, dal gruppo col risultato più recente. Il relay cerca sempre
// 7 giorni di tutte le sessioni (contratto 1.27): i filtri si applicano qui.
import { fold, type Hit } from './search'
import { dtf } from './dtf'

/** `session` null = tutte; `today` false = 7 giorni; `closed` = anche le sessioni chiuse. */
export type Filter = { session: string | null; today: boolean; closed: boolean }
export const FILTER: Filter = { session: null, today: false, closed: true }
/** `session` null = il registro; `live` null per il registro. */
export type Group = { session: string | null; live: boolean | null; hits: Hit[] }
export type Recent = { q: string; session?: string | null }
export const RECENT_MAX = 6

export function groups(hits: Hit[], f: Filter, dayStart: number): Group[] {
  const by = new Map<string, Group>()
  for (const h of hits) {
    if ((f.session != null && h.session !== f.session) || (f.today && h.at < dayStart) || (!f.closed && h.live === false)) continue
    const key = `${h.session ?? ''}\u0000${h.live === false}`
    const g = by.get(key) ?? { session: h.session, live: h.session == null ? null : h.live !== false, hits: [] }
    g.hits.push(h); by.set(key, g)
  }
  const out = [...by.values()]
  out.forEach(g => g.hits.sort((a, b) => b.at - a.at))
  return out.sort((a, b) => b.hits[0].at - a.hits[0].at)
}

/** In cima la più recente; la stessa ricerca (senza accenti né maiuscole) sale invece di ripetersi. */
export function remember(list: Recent[], r: Recent): Recent[] {
  const q = r.q.trim().replace(/\s+/g, ' ')
  if (!q) return list
  return [{ ...r, q }, ...list.filter(x => fold(x.q) !== fold(q))].slice(0, RECENT_MAX)
}

export type When = { kind: 'time'; hhmm: string } | { kind: 'yesterday' } | { kind: 'weekday'; day: number } | { kind: 'date'; ddmm: string }

const parts = (at: number, timeZone?: string) => {
  const p = Object.fromEntries(dtf('en-GB', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', weekday: 'short', hourCycle: 'h23' })
    .formatToParts(new Date(at * 1000)).map(x => [x.type, x.value]))
  return { y: +p.year, m: +p.month, d: +p.day, hh: p.hour, mm: p.minute, wd: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].indexOf(p.weekday) + 1 }
}
const dayNum = (x: { y: number; m: number; d: number }) => Date.UTC(x.y, x.m - 1, x.d) / 86_400_000

/** L'ora di un risultato come nel mockup: oggi l'ora, poi «ieri», poi il giorno della settimana (1 = lunedì), oltre la data. */
export function whenOf(at: number, now: number, timeZone?: string): When {
  const t = parts(at, timeZone), diff = dayNum(parts(now, timeZone)) - dayNum(t)
  if (diff <= 0) return { kind: 'time', hhmm: `${t.hh}:${t.mm}` }
  if (diff === 1) return { kind: 'yesterday' }
  if (diff < 7) return { kind: 'weekday', day: t.wd }
  return { kind: 'date', ddmm: `${String(t.d).padStart(2, '0')}/${String(t.m).padStart(2, '0')}` }
}

/** L'inizio del giorno di `now` nel fuso dato, in secondi. */
export function dayStart(now: number, timeZone?: string): number {
  const p = parts(now, timeZone)
  return now - (+p.hh * 3600 + +p.mm * 60) - (now % 60)
}
