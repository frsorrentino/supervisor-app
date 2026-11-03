import type { CmdResult, Option, Question, Session } from './contract'

// Le regole della domanda, porta di QuestionRules.kt: pressione lunga per il rischio alto, «Consenti tutto» mai per high,
// le opzioni brevi su una riga in parti uguali, l'etichetta senza il riquadro disegnato a caratteri.
export const needsLongPress = (tier: Question['tier']) => tier === 'high'
/** Senza opzioni lette non c'è «non chiedere più»: il relay rifiuterebbe allow_all. */
export const allowAllVisible = (q: Question) => q.kind === 'permission' && q.tier !== 'high' && q.options.length > 0

/** L'etichetta senza il riquadro (U+2500–U+259F): la prima riga che dice qualcosa. */
export function optionText(label: string): string {
  return label.split('\n').map(l => l.replace(/[─-▟]/g, ' ').replace(/\s+/g, ' ').trim()).find(l => l) ?? label.trim()
}
export const optionLabel = (o: Option) => `${o.n} · ${optionText(o.label)}`

export const INLINE_CHARS = 14
/** Due o tre opzioni brevi su una riga, in parti uguali; altrimenti una sotto l'altra. */
/** Con una spiegazione o una consigliata (1.53) mai in riga. */
export const inline = (options: Option[]) => options.length >= 2 && options.length <= 3 &&
  options.every(o => optionText(o.label).length <= INLINE_CHARS && !o.recommended && !o.description?.trim())
/** Contratto 1.53: la riga sotto il tasto, «consigliata · spiegazione», o null. */
export function optionNote(o: Option, recommendedWord: string): string | null {
  const parts = [o.recommended ? recommendedWord : null, o.description ? optionText(o.description) || null : null].filter(Boolean)
  return parts.length ? parts.join(' · ') : null
}
/** Il tasto pieno: l'opzione consigliata, altrimenti la prima. */
export const primaryIndex = (options: Option[]) => Math.max(0, options.findIndex(o => o.recommended))

/** Contratto 1.101: cosa si approva, se è un permesso e il relay lo sa. */
export const approves = (q: Question): string | null => (q.kind === 'permission' && q.what?.trim()) || null
/** Il testo per una notifica o la schermata di blocco: cosa si approva, altrimenti il testo senza segreti (1.56). */
export const shareable = (q: Question): string => approves(q) ?? q.preview ?? q.text

/** Oltre questa lunghezza un permesso si piega a FOLD_LINES righe, con il tasto per aprirlo. */
export const FOLD_CHARS = 120
export const FOLD_LINES = 4
const words = (s: string) => [...s].filter(c => /[\p{L}\p{N}]/u.test(c)).join('')
/**
 * Contratto 1.101, la card di un permesso (Franz, 02/11 01:22): `main` il testo grande, cosa si approva o la domanda con un relay
 * precedente; `question` la domanda generica sotto; `more` il testo del dialogo se dice di più; `folds` c'è da aprire.
 */
export function permissionCard(q: Question): { main: string; question: string | null; more: string | null; folds: boolean } | null {
  if (q.kind !== 'permission') return null
  const what = approves(q)
  const main = what ?? q.text
  const body = q.body?.trim()
  const more = body && what && words(body) !== words(main) ? body : null
  const question = what && words(q.text) && words(q.text) !== words(main) ? q.text : null
  return { main, question, more, folds: main.length > FOLD_CHARS || more !== null }
}

/** Contratto 1.10: la risposta a parole, gli a capo diventano spazi. */
export const textArg = (text: string) => `text:${text.replace(/\s*\n\s*/g, ' ').trim()}`
/** Contratto 1.10: «Parliamone», la domanda rifiutata e la sessione in attesa di un messaggio. */
export const CHAT_ARG = 'chat'

/** Contratto 1.52: il rifiuto di una risposta arrivata dopo una domanda nuova. */
export const QUESTION_CHANGED = ': the question changed after your answer, nothing was pressed'
/** Contratto 1.55: se la domanda a cui si rispondeva è quella risolta al terminale, il rifiuto lo dice; altrimenti il testo del relay. */
export function refusalText(r: CmdResult, answered: string | null, sessions: Session[], alreadyResolved: (name: string) => string): string {
  if (r.ok || !answered || !r.text.endsWith(QUESTION_CHANGED)) return r.text
  const name = r.text.slice(0, -QUESTION_CHANGED.length)
  return sessions.some(s => s.name === name && s.resolved?.id === answered) ? alreadyResolved(name) : r.text
}

/** I testi di `relay.cmd_superseded` dei relay precedenti a 1.112, che non mandano il campo. */
const SUPERSEDED_TEXTS = new Set(['superseded by a newer read of the same session', 'sostituita da una lettura più recente della stessa sessione'])

/** Contratto 1.112: una lettura rimpiazzata da una più recente: la richiesta si chiude in silenzio, mai un avviso. */
export const superseded = (r: CmdResult | null | undefined): boolean => !!r && !r.ok && (r.superseded === true || SUPERSEDED_TEXTS.has(r.text.trim()))
