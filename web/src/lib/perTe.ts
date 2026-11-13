// Il formato unico (app-formato-unico-12-11, variante A scelta da Franz il 12/11 alle 07:29; mockup in
// docs/mockup/2026-11-12-formato-unico): tutto ciò che aspetta la persona in una sessione è una voce della stessa forma, nell'ordine
// Ti aspetta, Decidi, Prova, Verifica. Ogni voce ha titolo, una riga di contesto, da quanto aspetta e il suo tasto principale; il
// tocco apre la scheda. Le domande, le decisioni e le prove vengono da `attese` (contratti 1.130-1.137), le verifiche dai risultati
// non visti (1.106). La stessa regola di PerTe.kt, coi casi di core/src/test/resources/per-te-cases.json.
import type { ResultGroup, WaitAction, WaitItem } from './contract'
import { kindOf, line, type Kind } from './guardare'
import { lookOf } from './tiAspetta'

export type Group = 'aspetta' | 'decidi' | 'prova' | 'verifica'
export const ORDER: readonly Group[] = ['aspetta', 'decidi', 'prova', 'verifica']
/** Il gruppo di una richiesta dalla sua etichetta: reply, verify, e ogni altra (decide, choose) è una decisione. */
export const groupOf = (label: string): Group => (label === 'reply' ? 'aspetta' : label === 'verify' ? 'prova' : 'decidi')

/** Il tasto sulla voce: `index` nei tasti della richiesta; `direct` false = chiede un testo, quindi apre la scheda. */
export type Main = { index: number; label: string; direct: boolean }
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
  return { index: i, label: short(num != null ? `${num} · ${a.label}` : a.label), direct: !a.text_field }
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
  wait: WaitItem | null; main: Main | null
  result: { id: string; kind: Kind; version: number | null } | null
}

const one = (s: string | null | undefined) => (s ?? '').replace(/\s+/g, ' ').trim()

/** Le voci della sessione: le richieste ancora aperte e i lavori con l'ultima versione non vista. */
export function voci(waits: readonly WaitItem[], results: readonly ResultGroup[], session: string): Voce[] {
  const out: Voce[] = []
  for (const w of waits) {
    if (w.session !== session || (w.state ?? 'open') !== 'open') continue
    const title = one(w.title) || one(w.text)
    const context = [one(w.text), one(w.description)].find(x => x && x !== title) ?? null
    out.push({ key: w.id, group: groupOf(w.label), title, context, since: w.at, wait: w, main: mainOf(w.actions), result: null })
  }
  for (const g of results) {
    const top = g.items[0]
    if (g.session !== session || !g.unseen || !top) continue
    const l = line(top)
    out.push({ key: top.id, group: 'verifica', title: l.title, context: null, since: top.at, wait: null, main: null, result: { id: top.id, kind: kindOf(top.kind), version: l.version } })
  }
  return out.sort((a, b) => ORDER.indexOf(a.group) - ORDER.indexOf(b.group) || a.since - b.since)
}

/**
 * I blocchi della chat (variante B): ognuno si riduce da sé a una riga col primo elemento, si apre sul posto e resta come lasciato.
 * Lo stato si ricorda per blocco, non per sessione; chi non ha mai scelto li trova ridotti.
 */
export type Block = 'perte' | 'azioni' | 'risultati'
const KEY = 'cm.blocks_open'
export const parseBlocks = (raw: string | null): Partial<Record<Block, boolean>> => {
  try { const o = JSON.parse(raw ?? '{}'); return o && typeof o === 'object' && !Array.isArray(o) ? o : {} } catch { return {} }
}
export const blockOpen = (b: Block): boolean => { try { return parseBlocks(localStorage.getItem(KEY))[b] === true } catch { return false } }
export const saveBlock = (b: Block, open: boolean) => { try { localStorage.setItem(KEY, JSON.stringify({ ...parseBlocks(localStorage.getItem(KEY)), [b]: open })) } catch { /* resta per questa visita */ } }
