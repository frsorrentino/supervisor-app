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

/**
 * Strada B (Franz 23/10 23:08, mockup `docs/mockup/2026-10-23-sezioni-accordate`): una riga «Per te» che appartiene a una
 * sessione aperta sta nella scheda di quella sessione (`inCards`, per nome, nell'ordine di `forYou`); in «Lavori» restano le
 * altre (`loose`), in una lista compatta. L'impegno del registro dice la sessione; una riga dell'agenda non la dice (resta in
 * lista finché il contratto non la porta). Gli ok hanno già la loro riga nella scheda (`approvalsOf`): `attachedOk` = i loro
 * compiti, che qui non si ripetono.
 */
/**
 * Franz 24/10 00:44: «in lavori ogni scheda dovrebbe essere espandibile con maggiori dettagli». I dettagli di una riga «Per te»,
 * nell'ordine e senza i campi vuoti: impegno (chi aspetta chi, ultimo passo, prossimo, cartella, esito atteso, pausa), riga
 * dell'agenda (riferimento, dettaglio, chi blocca, fino a quando), ok (cosa, dove, chiesto alle). `asked_at` porta l'epoch.
 */
export type DetailKind = 'waits' | 'last_step' | 'next' | 'folder' | 'expected' | 'paused' | 'ref' | 'detail' | 'blocks' | 'until' | 'what' | 'where' | 'asked_at'
export type DetailRow = { kind: DetailKind; value: string }
export function details(r: ForYou): DetailRow[] {
  const out: DetailRow[] = []
  const add = (kind: DetailKind, value: string | number | null | undefined) => { const v = value == null ? '' : String(value).trim(); if (v) out.push({ kind, value: v }) }
  const c = r.commitment
  if (c) {
    add('waits', c.waits ? [c.waits.who, c.waits.on].filter(x => x && x.trim()).join(' ← ') : null)
    add('last_step', c.last_step?.text); add('next', c.next); add('folder', c.project); add('expected', c.expected); add('paused', c.paused?.reason)
  }
  const a = r.agenda
  if (a) { add('ref', a.ref); add('detail', a.detail); add('blocks', a.blocks); add('until', a.until) }
  if (!c && r.approval) { add('what', r.approval.what); add('where', r.approval.where); add('asked_at', r.approval.requested_at > 0 ? r.approval.requested_at : null) }
  return out
}

export type Placed = { inCards: Map<string, ForYou[]>; loose: ForYouGroups }
export function place(g: ForYouGroups, open: Set<string>, attachedOk: Set<string> = new Set()): Placed {
  const inCards = new Map<string, ForYou[]>()
  const loose = (r: ForYou): boolean => {
    const s = r.commitment?.session
    if (!s || !open.has(s)) return true
    inCards.set(s, [...(inCards.get(s) ?? []), r])
    return false
  }
  const ok = g.ok.filter(r => !r.approval || !attachedOk.has(r.approval.task))
  const outside = g.outside.filter(loose), claude = g.claude.filter(loose)
  return { inCards, loose: { ok, outside, claude, total: ok.length + outside.length + claude.length } }
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
/** Chi deve muoversi (mockup della master approvato da Franz il 22/10 alle 21:16, `docs/mockup/2026-10-22-in-corso/`). */
export type Who = 'working' | 'you' | 'others'
/**
 * Una riga di «In corso»: una o più voci (`works`) con lo stesso titolo breve in testa; `n` > 1 = la pastiglia «N compiti»
 * (stessa sessione e stesso motivo di pausa). `reason` = il motivo della pausa (in coda dietro un altro compito, o cosa serve
 * da Franz); `next` = il prossimo passo (l'ok da dare); `since` = da quando, epoch s, per la colonna di destra; `bars` = quelle
 * della prima voce.
 */
export type Line = {
  who: Who; works: Work[]; short: string; n: number; bars: Bar[]
  session: string | null; reason: string | null; next: string | null; approvedAt: number | null; since: number | null; agenda: AgendaRow | null
}
/** `working` = al lavoro (anche in coda dietro un altro compito); `you` = aspetta Franz (in pausa o per un ok); `others` = fermi su terzi. */
export type InProgress = { working: Line[]; you: Line[]; others: Line[]; axis: Axis; total: number }

/** Contratto 1.79: la lunghezza massima del titolo breve. */
export const SHORT_MAX = 28
const SLUG_PREFIX = /^[\w.-]+:\s+/
const TAIL_WORDS = new Set(['a', 'ad', 'al', 'alla', 'alle', 'ai', 'agli', 'con', 'da', 'dal', 'dalla', 'dalle', 'dai', 'dei', 'del', 'della', 'delle', 'degli', 'di', 'e', 'ed',
  'il', 'in', 'la', 'le', 'lo', 'gli', 'i', 'per', 'su', 'sul', 'sulla', 'sui', 'un', 'una', 'uno', 'o', 'che', 'the', 'of', 'and', 'to', 'on', 'for', 'with', 'an', 'at', 'by', 'or'])
const trimTail = (s: string) => s.replace(/[,;:\-· ]+$/, '')

/**
 * Il titolo breve di una riga: `short` del relay (contratto 1.79) quando c'è; altrimenti dal titolo: via il prefisso «nome: »
 * (un nome senza spazi), parole intere entro `max` caratteri, mai un articolo, una preposizione o una parentesi aperta in coda, mai «…».
 */
export function shortTitle(title: string, short?: string | null, max = SHORT_MAX): string {
  const s = short?.trim()
  if (s && s.length <= max && !s.includes('\n')) return s
  const t = title.replace(SLUG_PREFIX, '').replace(/\s+/g, ' ').trim()
  if (t.length <= max) return t || title.trim().slice(0, max)
  const head = t.slice(0, max + 1)
  const sp0 = head.lastIndexOf(' ')
  let cut = sp0 > 0 ? head.slice(0, sp0) : head.slice(0, max)
  for (;;) {
    const sp = cut.lastIndexOf(' ')
    if (sp <= 0) break
    const w = cut.slice(sp + 1)
    if (TAIL_WORDS.has(w.toLowerCase().replace(/[,;:]+$/, '')) || /^[(«[]/.test(w)) cut = cut.slice(0, sp); else break
  }
  return trimTail(cut) || t.slice(0, max).trim()
}

/** Il titolo di una riga raggruppata: la parte comune dei titoli brevi, a parola intera e di almeno 4 caratteri, altrimenti il primo. */
function commonShort(shorts: string[]): string {
  const first = shorts[0]
  if (shorts.length === 1) return first
  let n = first.length
  for (const s of shorts.slice(1)) { let i = 0; while (i < n && i < s.length && first[i] === s[i]) i++; n = i }
  let common = first.slice(0, n)
  if (n < first.length && first[n] !== ' ') common = common.includes(' ') ? common.slice(0, common.lastIndexOf(' ')) : ''
  common = trimTail(common)
  return common.length >= 4 ? common : first
}

const positive = (x: number | null | undefined) => (x && x > 0 ? x : null)

export function inProgress(st: State, agenda: AgendaPage | null | undefined, now: number, zone: string, today: string): InProgress {
  const ax = axis(now, zone)
  const rs = commitmentRows(st, now)
  const work = (r: Row): Work => ({ row: r, agenda: null, bars: bars(r.c, ax, now) })
  const line = (who: Who, works: Work[], short: string, c: Commitment | null, since: number | null, agenda: AgendaRow | null = null): Line =>
    ({ who, works, short, n: works.length, bars: works[0].bars, session: c?.session ?? null, reason: c?.paused?.reason ?? null, next: c?.next?.trim() || null, approvedAt: positive(c?.approved_at), since, agenda })
  // Chi aspetta una sessione è al lavoro anche in pausa: è in coda dietro un altro compito, e il motivo lo dice.
  const working = rs.filter(r => !r.yourTurn).map(r => line('working', [work(r)], shortTitle(r.c.title, r.c.short), r.c, positive(r.c.approved_at) ?? positive(r.c.last_step?.at)))
  // Chi aspetta Franz: gli ok da dare prima (relay: le pause in fondo); stessa sessione e stesso motivo di pausa = una riga.
  const grouped = new Map<string, Row[]>()
  for (const r of rs.filter(r => r.yourTurn)) { const k = r.c.paused ? `${r.c.session}|${r.c.paused.reason}` : `task|${r.c.task}`; grouped.set(k, [...(grouped.get(k) ?? []), r]) }
  const you = [...grouped.values()].map(g => {
    const c = g[0].c
    return line('you', g.map(work), commonShort(g.map(r => shortTitle(r.c.title, r.c.short))), c, positive(c.paused?.at) ?? positive(g[0].approval?.requested_at) ?? positive(c.last_step?.at))
  })
  const others = agendaModel(agenda, null, today).other.map(a => line('others', [{ row: null, agenda: a, bars: [] }], shortTitle(a.title, a.short), null, null, a))
  return { working, you, others, axis: ax, total: working.reduce((n, l) => n + l.n, 0) + you.reduce((n, l) => n + l.n, 0) + others.length }
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

/** La riga di home «fatto oggi N»: le righe della pagina di oggi dell'op `done` quando è caricata, altrimenti i `task_check` verdi di oggi dagli eventi. */
export function doneToday(pages: Record<string, DonePage>, events: Event[], zone: string, today: string): number {
  return pages[today]?.rows.length ?? (done(events, zone, today, 1).find(d => d.date === today)?.rows.length ?? 0)
}
