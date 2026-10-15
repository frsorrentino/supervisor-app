// Un comando nudo come testo di sblocco («/clear») diventa una frase col comando fra parentesi, come nell'app (Franz,
// 15/10 22:05: «Rendi leggibile /clear»). Solo i comandi noti; il resto resta com'è. Quello che parte resta il comando.
export const KNOWN = ['clear', 'compact', 'login', 'resume', 'exit', 'context', 'cost'] as const
export type Known = (typeof KNOWN)[number]

/** Il comando noto, se il testo è solo quello (con eventuali argomenti); altrimenti null. */
export function known(text: string): Known | null {
  const m = text.trim().match(/^\/([a-z-]+)(\s.*)?$/)
  return m && (KNOWN as readonly string[]).includes(m[1]) ? (m[1] as Known) : null
}

/** La frase leggibile: «svuotare il contesto (/clear)». `phrases` dalla lingua dell'app. */
export function readable(text: string, phrases: Record<Known, string>): string {
  const k = known(text)
  return k ? `${phrases[k]} (${text.trim()})` : text
}

/** Le righe «Aspetta» del rapporto («!: /clear»): la parte dopo il prefisso, leggibile. */
export function readableWaiting(line: string, phrases: Record<Known, string>): string {
  const m = line.match(/^(!|ok|domanda): (.*)$/)
  return m ? `${m[1]}: ${readable(m[2], phrases)}` : readable(line, phrases)
}
