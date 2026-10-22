// La pagina unica «Lavori» (mockup approvato da Franz il 22/10/2026), porta di Lavori.kt: quattro blocchi ordinati per
// domanda, non per fonte. «Per te» (solo quello che aspetta l'utente, ognuno col suo tasto), «In corso» (un compito per
// riga con la barra dei sette giorni), «Stanotte» (la coda, dallo stato) e «Fatto» (per giorno, con la prova). Fonti:
// `state.commitments` e `state.approvals` (1.76), l'agenda (op `agenda`), gli eventi `task_check` (1.57); la prova e i
// giorni prima arrivano con l'op `done` del contratto 1.77.
import type { AgendaPage, AgendaRow, Approval, Commitment, DonePage, Event, State } from './contract'
import { PERSON, rows as commitmentRows, type Row } from './commitments'
import { agendaModel } from './recapAgenda'

export type Kind = 'ok' | 'outside' | 'claude'
/** Una riga di «Per te»: `who` = chi chiede o chi si sblocca; `at` = quando (epoch s) o null. */
export type ForYou = { kind: Kind; title: string; who: string | null; at: number | null; approval: Approval | null; commitment: Commitment | null; agenda: AgendaRow | null }
export type ForYouGroups = { ok: ForYou[]; outside: ForYou[]; claude: ForYou[]; total: number }

const blank = (s: string | null | undefined): string | null => (s && s.trim() ? s : null)
const pos = (n: number | null | undefined): number | null => (n && n > 0 ? n : null)
const row = (p: Partial<ForYou> & { kind: Kind; title: string }): ForYou => ({ who: null, at: null, approval: null, commitment: null, agenda: null, ...p })

export function forYou(st: State, agenda: AgendaPage | null | undefined, now: number, today: string): ForYouGroups {
  const rs = commitmentRows(st, now)
  // Gli impegni che aspettano l'utente (non in pausa), con l'ok del registro collegato quando c'è: un ok solo, mai due volte.
  const linked = rs.filter(r => r.yourTurn && !r.c.paused)
    .map(r => row({ kind: 'ok', title: r.c.title, who: blank(r.c.session), at: pos(r.approval?.requested_at) ?? r.c.last_step?.at ?? null, approval: r.approval, commitment: r.c }))
  const taken = new Set(linked.map(r => r.approval?.task).filter(Boolean))
  const loose = (st.approvals ?? []).filter(a => !taken.has(a.task)).map(a => row({ kind: 'ok', title: a.title, who: blank(a.where), at: pos(a.requested_at), approval: a }))
  const ok = [...linked, ...loose].sort((a, b) => (a.at ?? Infinity) - (b.at ?? Infinity))
  const m = agendaModel(agenda, null, today)
  const outside = [
    ...m.you.map(r => row({ kind: 'outside', title: r.title, who: blank(r.ref), agenda: r })),
    ...(st.commitments ?? []).filter(c => c.paused && c.waits?.on === PERSON).map(c => row({ kind: 'outside', title: c.paused!.reason, who: blank(c.session), at: pos(c.paused!.at), commitment: c })),
  ]
  const claude = m.claude.map(r => row({ kind: 'claude', title: r.title, who: blank(r.ref), agenda: r }))
  return { ok, outside, claude, total: ok.length + outside.length + claude.length }
}

/** Un tratto della barra, frazioni 0..1 dell'asse; `dashed` = in pausa. */
export type Bar = { from: number; to: number; dashed: boolean }
/** Sette giorni: `start` = mezzanotte locale del primo, `end` = mezzanotte di domani; `days` = [etichetta, frazione di inizio]. */
export type Axis = { start: number; end: number; days: [string, number][]; now: number }
export const MIN_BAR = 0.01

const parts = (t: number, zone: string) => {
  const o = Object.fromEntries(new Intl.DateTimeFormat('en-CA', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' })
    .formatToParts(new Date(t * 1000)).filter(p => p.type !== 'literal').map(p => [p.type, Number(p.value)]))
  return o as { year: number; month: number; day: number; hour: number; minute: number; second: number }
}
/** Il giorno locale, `AAAA-MM-GG`. */
export const ymd = (t: number, zone: string): string => { const p = parts(t, zone); return `${p.year}-${String(p.month).padStart(2, '0')}-${String(p.day).padStart(2, '0')}` }
const utcOf = (d: string): number => { const [y, m, dd] = d.split('-').map(Number); return Date.UTC(y, m - 1, dd) / 1000 }
export const addDays = (d: string, n: number): string => new Date((utcOf(d) + n * 86400) * 1000).toISOString().slice(0, 10)
/** La mezzanotte locale di un giorno `AAAA-MM-GG` nel fuso, epoch s (due passi per l'ora legale). */
export function midnight(d: string, zone: string): number {
  const want = utcOf(d)
  let t = want
  for (let i = 0; i < 2; i++) { const p = parts(t, zone); t -= Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second) / 1000 - want }
  return t
}
const frac = (t: number, start: number, end: number) => Math.min(1, Math.max(0, (t - start) / (end - start)))

export function axis(now: number, zone: string, days = 7): Axis {
  const today = ymd(now, zone)
  const start = midnight(addDays(today, -(days - 1)), zone)
  const end = midnight(addDays(today, 1), zone)
  const labels: [string, number][] = Array.from({ length: days }, (_, i) => { const d = addDays(today, -(days - 1 - i)); return [String(Number(d.slice(8, 10))), frac(midnight(d, zone), start, end)] })
  return { start, end, days: labels, now: frac(now, start, end) }
}

/** Dall'ok (o da `now − age_s`) a adesso, tagliata all'asse; in pausa piena fino alla pausa e tratteggiata da lì; tutta tratteggiata se la pausa è più vecchia dell'inizio visibile. */
export function bars(c: Commitment, ax: Axis, now: number): Bar[] {
  const f = (t: number) => frac(t, ax.start, ax.end)
  // Un ok nel futuro (orologi diversi) non rompe niente: l'inizio non supera mai adesso.
  const begin = Math.min(Math.max(c.approved_at ?? now - c.age_s, ax.start), Math.max(now, ax.start))
  const end = Math.min(Math.max(now, begin), ax.end)
  const p = c.paused
  const raw: Bar[] = !p ? [{ from: f(begin), to: f(end), dashed: false }]
    : p.at <= begin ? [{ from: f(begin), to: f(end), dashed: true }]
    : [{ from: f(begin), to: f(Math.min(p.at, end)), dashed: false }, { from: f(Math.min(p.at, end)), to: f(end), dashed: true }]
  return raw.map(b => { if (b.to - b.from >= MIN_BAR) return b; const from = Math.min(b.from, 1 - MIN_BAR); return { from, to: from + MIN_BAR, dashed: b.dashed } })
}

/** Una riga di «In corso»: un impegno (`row`) con le barre, o una riga dell'agenda ferma su terzi (`agenda`, senza barre). */
export type Work = { row: Row | null; agenda: AgendaRow | null; bars: Bar[] }
export type InProgress = { active: Work[]; paused: Work[]; others: Work[]; axis: Axis }

export function inProgress(st: State, agenda: AgendaPage | null | undefined, now: number, zone: string, today: string): InProgress {
  const ax = axis(now, zone)
  const rs = commitmentRows(st, now)
  const active = rs.filter(r => !r.c.paused && !r.yourTurn).map(r => ({ row: r, agenda: null, bars: bars(r.c, ax, now) }))
  const paused = rs.filter(r => r.c.paused).map(r => ({ row: r, agenda: null, bars: bars(r.c, ax, now) }))
  const others = agendaModel(agenda, null, today).other.map(a => ({ row: null, agenda: a, bars: [] }))
  return { active, paused, others, axis: ax }
}

/** Un compito chiuso; `proof` = la prova in una riga («release v2.3», «commit 9f8e7d6», il file, il comando), null dagli eventi; `url` col link; `outcome` ∈ green|cancelled|failed. */
export type Done = { task: string; title: string; session: string | null; at: number; proof: string | null; url?: string | null; outcome?: string }
/** `n` = quanti chiusi quel giorno; `rows` vuote con `n` > 0 = un giorno prima richiuso, da caricare con `done` di quel giorno. */
export type DoneDay = { date: string; rows: Done[]; n: number }

/** Contratto 1.77: i giorni dalle pagine dell'op `done` già caricate (una per data), più i giorni prima elencati col solo conteggio; dal più recente. */
export function doneFromPages(pages: Record<string, DonePage>, _zone: string): DoneDay[] {
  const loaded = new Map<string, DoneDay>()
  for (const pg of Object.values(pages)) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(pg.date)) continue
    const rows: Done[] = pg.rows.map(r => {
      const pr = r.proof
      const label = pr ? (pr.kind === 'file' || pr.kind === 'check' || !pr.kind ? pr.value : `${pr.kind} ${pr.value}`).trim() || null : null
      return { task: r.task, title: r.title || r.task, session: r.session ?? null, at: r.at, proof: label, url: pr?.url ?? null, outcome: r.outcome || 'green' }
    })
    loaded.set(pg.date, { date: pg.date, rows, n: rows.length })
  }
  const refs = new Map<string, number>()
  for (const pg of Object.values(pages)) for (const ref of pg.days ?? []) if (!loaded.has(ref.date)) refs.set(ref.date, Math.max(refs.get(ref.date) ?? 0, ref.n))
  return [...loaded.values(), ...[...refs].map(([date, n]) => ({ date, rows: [], n }))].sort((a, b) => (a.date < b.date ? 1 : -1))
}

/** I `task_check` verdi degli ultimi `days` giorni, uno per compito (l'ultimo), per giorno dal più recente; `title` = la prima riga del corpo. */
export function done(events: Event[], zone: string, today: string, days = 7): DoneDay[] {
  const first = addDays(today, -(days - 1))
  const last = new Map<string, Event>()
  for (const e of events) {
    // Contratto 1.78: l'esito sta in `ok`; con un relay precedente lo dice il ✓ in testa al titolo.
    if (e.kind !== 'task_check' || !(e.ok ?? e.title.startsWith('✓')) || !e.ref?.trim()) continue
    const prev = last.get(e.ref)
    if (!prev || e.ts > prev.ts) last.set(e.ref, e)
  }
  const byDay = new Map<string, Done[]>()
  for (const e of last.values()) {
    const d = ymd(e.ts, zone)
    if (d < first || d > today) continue
    const title = (e.body ?? '').split('\n')[0].trim() || e.ref!
    byDay.set(d, [...(byDay.get(d) ?? []), { task: e.ref!, title, session: e.session ?? null, at: e.ts, proof: null }])
  }
  return [...byDay.entries()].sort((a, b) => (a[0] < b[0] ? 1 : -1)).map(([date, rs]) => ({ date, rows: rs.sort((a, b) => b.at - a.at), n: rs.length }))
}
