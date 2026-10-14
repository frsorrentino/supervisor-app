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
