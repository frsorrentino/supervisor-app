import type { CmdResult, State } from './contract'

// app-web-risposta-toglie-attesa-12-11 (Franz, 12/11 12:07 dalla PWA: «ho fatto ok in chat ma in home rimane la domanda,
// serve rimozione istantanea»). Come l'app Android (Repo.optimistic): appena parte una risposta a una sessione che aspettava,
// la voce sparisce da «Ti aspettano» e la sessione passa al lavoro. Torna solo se uno stato fatto dal PC dopo aver preso la
// risposta la dice ancora aperta; uno stato partito prima non conta. Una domanda o una ferma nuova si vede subito.

/** Quello che aspettava quando è partita la risposta; `done` = l'ora del PC dell'esito, null finché non arriva. */
export type Answered = { q: string | null; ny: number | null; tb: number | null; done: number | null }

/** La marca per una risposta a `name`; null se la sessione non aspettava niente. */
export function markAnswer(st: State, name: string): Answered | null {
  const s = st.sessions.find(x => x.name === name)
  if (!s) return null
  const q = s.question?.id ?? null, ny = s.needs_you?.reason?.trim() ? s.needs_you.at : null, tb = s.turn_blocking?.text?.trim() ? s.turn_blocking.at : null
  return q == null && ny == null && tb == null ? null : { q, ny, tb, done: null }
}

/** L'esito del PC: riuscito fissa l'ora oltre la quale lo stato comanda; rifiutato o perso toglie la marca (la voce torna). */
export function settle(marks: Record<string, Answered>, name: string, r: CmdResult | null): Record<string, Answered> {
  const m = marks[name]
  if (!m) return marks
  const { [name]: _, ...rest } = marks
  return r?.ok ? { ...rest, [name]: { ...m, done: r.at } } : rest
}

/** Le marche ancora valide per questo stato: cade quella di una sessione per cui lo stato è già dopo l'esito. */
export function live(marks: Record<string, Answered>, st: State): Record<string, Answered> {
  const out: Record<string, Answered> = {}
  for (const [n, m] of Object.entries(marks)) if (m.done == null || st.ts <= m.done) out[n] = m
  return out
}

/** Lo stato come si vede: per ogni marca la voce risposta non c'è e la sessione è al lavoro. */
export function applyAnswered(st: State, marks: Record<string, Answered>): State {
  const names = Object.keys(marks)
  if (!names.length) return st
  let hid = false
  const sessions = st.sessions.map(s => {
    const m = marks[s.name]
    if (!m) return s
    const q = m.q != null && s.question?.id === m.q, ny = m.ny != null && s.needs_you?.at === m.ny, tb = m.tb != null && s.turn_blocking?.at === m.tb
    if (!q && !ny && !tb) return s
    hid = true
    const left = (q ? null : s.question) ?? (ny ? null : s.needs_you) ?? (tb ? null : s.turn_blocking)
    return {
      ...s,
      ...(q ? { question: null } : {}), ...(ny ? { needs_you: null } : {}), ...(tb ? { turn_blocking: null } : {}),
      ...(!left && (s.state === 'waiting' || s.state === 'awaiting') ? { state: 'busy' as const } : {}),
    }
  })
  if (!hid) return st
  // Le voci di `attention` della stessa sessione nate fino alla risposta: una nata dopo resta.
  const upTo = (n: string) => { const m = marks[n]; const s = st.sessions.find(x => x.name === n); return Math.max(s?.question?.id === m.q ? s?.question?.asked_at ?? 0 : 0, m.ny ?? 0, m.tb ?? 0) }
  const attention = st.attention?.filter(a => !(a.session && marks[a.session] && a.since <= upTo(a.session)))
  return { ...st, sessions, ...(st.attention ? { attention } : {}) }
}
