// Il formato unico (app-formato-unico-12-11, variante A scelta da Franz il 12/11 alle 07:29; mockup in
// docs/mockup/2026-11-12-formato-unico): tutto ciò che aspetta la persona in una sessione è una voce della stessa forma, nell'ordine
// Ti aspetta, Decidi, Prova, Verifica. Ogni voce ha titolo, una riga di contesto, da quanto aspetta e il suo tasto principale; il
// tocco apre la scheda. Le domande, le decisioni e le prove vengono da `attese` (contratti 1.130-1.137), le verifiche dai risultati
// non visti (1.106). La stessa regola di PerTe.kt, coi casi di core/src/test/resources/per-te-cases.json.
import type { ResultGroup, WaitAction, WaitItem } from './contract'
import { kindOf, line, type Kind } from './guardare'
import { lookOf } from './tiAspetta'
import { placeOf } from './tieniPremuto'

export type Group = 'aspetta' | 'decidi' | 'prova' | 'verifica'
export const ORDER: readonly Group[] = ['aspetta', 'decidi', 'prova', 'verifica']
/** Il gruppo di una richiesta dalla sua etichetta: reply, verify, e ogni altra (decide, choose) è una decisione. */
export const groupOf = (label: string): Group => (label === 'reply' ? 'aspetta' : label === 'verify' ? 'prova' : 'decidi')

/**
 * Il tasto sulla voce: `index` nei tasti della richiesta; `direct` false = chiede un testo, quindi apre la scheda; `recommended`: è la
 * scelta consigliata, e la riga lo mostra in ambra (app-riall-2-viste-16-11).
 */
export type Main = { index: number; label: string; direct: boolean; recommended: boolean; tap?: boolean }
type Act = Pick<WaitAction, 'label' | 'style' | 'recommended' | 'text_field'>
/**
 * Il tasto principale: la scelta consigliata, altrimenti il tasto pieno, altrimenti la prima scelta numerata; solo tasti secondari,
 * nessuno. Una scelta numerata porta il suo numero («1 · sì»), come nella scheda.
 */
export function mainOf(actions: readonly Act[] | null | undefined): Main | null {
  const acts = actions ?? []
  let i = acts.findIndex(a => a.recommended && a.style !== 'secondary')
  if (i < 0) i = acts.findIndex(a => a.style === 'primary')
  if (i < 0) i = acts.findIndex(a => a.style === 'option')
  if (i < 0) return null
  const a = acts[i]
  const num = lookOf(a) === 'option' ? acts.slice(0, i + 1).filter(x => lookOf(x) === 'option').length : null
  return { index: i, label: short(num != null ? `${num} · ${a.label}` : a.label), direct: !a.text_field, recommended: !!a.recommended }
}

/**
 * Il consiglio di chi chiede, sulla riga (app-riall-2-viste-16-11, direzione A: «consigliata: approvare»; contratto 1.149): approvare
 * o no su un ok; su una scelta l'opzione consigliata, col suo numero come sul tasto quando il tasto è lei. Senza consiglio, niente.
 */
export type Advice = { approve: boolean; option: null } | { approve: null; option: string }
export function adviceOf(w: Pick<WaitItem, 'advice'>, main: Main | null): Advice | null {
  const a = w.advice
  if (!a) return null
  if (a.approve === true || a.approve === false) return { approve: a.approve, option: null }
  if (main?.recommended) return { approve: null, option: main.label }
  const o = (a.option ?? '').replace(/\s+/g, ' ').trim()
  return o ? { approve: null, option: short(o) } : null
}

/** Il tasto sta sulla voce accanto al titolo: al massimo `MAIN_MAX` caratteri, tagliato a una parola intera; l'etichetta intera è nella scheda. */
export const MAIN_MAX = 16
function short(label: string): string {
  const l = label.replace(/\s+/g, ' ').trim()
  if (l.length <= MAIN_MAX) return l
  const cut = l.slice(0, MAIN_MAX + 1), at = cut.lastIndexOf(' ')
  return (at >= MAIN_MAX / 2 ? cut.slice(0, at) : l.slice(0, MAIN_MAX)).replace(/[\s·,;:]+$/, '')
}

/** `wait`: la richiesta (con `main` il suo tasto); `result`: il risultato da guardare, col tipo e la versione per la riga di contesto. */
export type Voce = {
  key: string; group: Group; title: string; context: string | null; since: number
  wait: WaitItem | null; main: Main | null; advice: Advice | null
  result: { id: string; kind: Kind; version: number | null } | null
}

const one = (s: string | null | undefined) => (s ?? '').replace(/\s+/g, ' ').trim()

/** La voce di una richiesta: titolo, contesto, gruppo e tasto principale. La stessa in «Per te» e in Dalla master (app-riall-1-dati-16-11). */
export function voceOf(w: WaitItem): Voce {
  const title = one(w.title) || one(w.text)
  const context = [one(w.text), one(w.description)].find(x => x && x !== title) ?? null
  // app-tieni-premuto-28-11: l'ok che pubblica si tiene premuto nella sua scheda, quindi la riga la apre; ogni altro ok e ogni Va
  // restano un tocco sulla riga (`tap`), col finale del cerchio verde (correzione della master del 28/11).
  const m = mainOf(w.actions)
  const place = m ? placeOf(w, m.index) : null
  const main = m && place === 'pubblica' ? { ...m, direct: false } : m && place ? { ...m, tap: true } : m
  return { key: w.id, group: groupOf(w.label), title, context, since: w.at, wait: w, main, advice: adviceOf(w, main), result: null }
}

/** Le voci della sessione: le richieste ancora aperte e i lavori con l'ultima versione non vista. */
export function voci(waits: readonly WaitItem[], results: readonly ResultGroup[], session: string): Voce[] {
  const out: Voce[] = []
  for (const w of waits) {
    if (w.session !== session || (w.state ?? 'open') !== 'open') continue
    out.push(voceOf(w))
  }
  for (const g of results) {
    const top = g.items[0]
    if (g.session !== session || !g.unseen || !top) continue
    const l = line(top)
    out.push({ key: top.id, group: 'verifica', title: l.title, context: null, since: top.at, wait: null, main: null, advice: null, result: { id: top.id, kind: kindOf(top.kind), version: l.version } })
  }
  return out.sort(byOrder)
}
const byOrder = (a: Voce, b: Voce) => ORDER.indexOf(a.group) - ORDER.indexOf(b.group) || a.since - b.since

/** Un gruppo di Dalla master: le voci di una sessione, o quelle senza sessione (`session` null). */
export type Gruppo = { session: string | null; voci: Voce[] }
/**
 * Dalla master (app-riall-2-viste-16-11, direzione A scelta da Franz il 16/11): le stesse voci di «Per te» di ogni sessione, una
 * sessione per gruppo; prima quella che aspetta da più tempo, in fondo le voci senza sessione (la prima raccolta). Le voci già
 * risposte (`answered`) restano fuori, e un gruppo senza voci non c'è. La stessa regola di PerTe.groupsOf.
 */
export function groupsOf(waits: readonly WaitItem[], results: readonly ResultGroup[], answered: ReadonlySet<string> = new Set()): Gruppo[] {
  const open = (w: WaitItem) => (w.state ?? 'open') === 'open'
  const names = new Set<string>()
  for (const w of waits) if (w.session && open(w)) names.add(w.session)
  for (const g of results) if (g.session && g.unseen && g.items.length) names.add(g.session)
  const oldest = (g: Gruppo) => Math.min(...g.voci.map(v => v.since))
  const out = [...names].map((s): Gruppo => ({ session: s, voci: voci(waits, results, s).filter(v => !answered.has(v.key)) }))
    .filter(g => g.voci.length).sort((a, b) => oldest(a) - oldest(b) || (a.session ?? '').localeCompare(b.session ?? ''))
  const loose = waits.filter(w => !w.session && open(w)).map(voceOf).filter(v => !answered.has(v.key)).sort(byOrder)
  return loose.length ? [...out, { session: null, voci: loose }] : out
}

/**
 * Per te in ordine (app-per-te-ordine-23-11, Franz 23/11 19:00 e 19:01; contratto 1.160): in alto, aperte, le carte che bloccano un
 * compito in corso (`blocking`), dalla più vecchia, ognuna con la sua sessione; sotto i gruppi per sessione con il resto (pause,
 * domande, verifiche), chiusi di base. Un gruppo che resta senza voci non c'è. La stessa regola di PerTe.splitBlocking.
 */
export type Bloccante = Voce & { session: string | null }
export function splitBlocking(groups: readonly Gruppo[]): { top: Bloccante[]; groups: Gruppo[] } {
  const top: Bloccante[] = []
  const rest: Gruppo[] = []
  for (const g of groups) {
    const mine = g.voci.filter(v => v.wait?.blocking === true)
    top.push(...mine.map(v => ({ ...v, session: g.session })))
    const left = g.voci.filter(v => v.wait?.blocking !== true)
    if (left.length) rest.push({ session: g.session, voci: left })
  }
  return { top: top.sort((a, b) => a.since - b.since), groups: rest }
}
