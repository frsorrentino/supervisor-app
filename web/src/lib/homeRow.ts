import type { Row } from './summary'
import { MASTER } from './summary'
import { headline } from './outcome'

// La Home in riga (app-home-riga-c1-13-11, giro C dei mockup, variante C1 scelta da Franz il 13/11 alle 09:32), porta di
// HomeRow.kt. Mentre la master occupa la colonna, la Home è una riga in basso che mostra solo quello che non si vede già: le
// sessioni che ti aspettano e non sono a schermo, e le novità da quando la Home è in riga. Una voce per cosa, che apre la sua
// sessione; una novità va via quando la apri o quando riapri la Home. Vuota, resta la maniglia.
/** Una riga della Home, come la vede la regola: `group` waiting | finished | working | still. */
export type In = { session: string; group: string; text: string | null; at: number | null; key?: string | null }
/** `wait` = ti aspetta fuori schermo; `news` = un esito arrivato da quando la Home è in riga. */
export type Voice = { kind: 'wait' | 'news'; session: string; text: string | null; at: number | null; key: string }

/** Le righe della Home (`build`) per la regola: di chi ha finito il titolo dell'esito, degli altri la prima riga del testo. */
export const fromSummary = (rows: Row[]): In[] => rows.map(r => ({
  session: r.session.name, group: r.group, at: r.at, key: r.key ?? null,
  text: r.group === 'finished' && r.session.outcome ? headline(r.session.outcome) : r.text,
}))

/**
 * Le voci della riga: `shown` = le sessioni aperte a schermo, `since` = da quando la Home è in riga (epoch s; null = nessuna
 * novità), `opened` = le novità già aperte. Prima chi ti aspetta, nell'ordine della Home; poi le novità. La master non c'è mai:
 * è lei a schermo.
 */
export function voices(rows: In[], shown: string[], since: number | null, opened: Set<string>): Voice[] {
  const off = rows.filter(r => r.session !== MASTER && !shown.includes(r.session))
  const line = (x: string | null) => x?.split('\n')[0].trim() || null
  const waits: Voice[] = off.filter(r => r.group === 'waiting').map(r => ({ kind: 'wait', session: r.session, text: line(r.text), at: r.at, key: `wait:${r.session}` }))
  const news: Voice[] = since == null ? [] : off.filter(r => r.group === 'finished' && (r.at ?? 0) > since)
    .map((r): Voice => ({ kind: 'news', session: r.session, text: line(r.text), at: r.at, key: r.key ?? `news:${r.session}@${r.at}` }))
    .filter(v => !opened.has(v.key))
  return [...waits, ...news]
}

/** L'altezza della riga: piena con le voci, solo la maniglia senza. */
export const ROW_H = 64
export const GRIP_H = 28
export const rowHeight = (vs: Voice[]): number => (vs.length ? ROW_H : GRIP_H)
