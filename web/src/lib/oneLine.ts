// Il riassunto a destra delle righe della home e delle testate sta su una riga (Franz 03/11 08:07, variante 1,
// app-riassunti-una-riga-03-11). Le parti vanno in ordine di importanza; quelle che non entrano cadono intere, dall'ultima; se
// non entra nemmeno la prima resta il suo numero. Mai parole spezzate, mai puntini. Le stesse regole in OneLine.kt.
export const SEP = ' · '
const NUMBER = /[-+]?\d+(?:[.,]\d+)?\s?%?/

/** Le parti di un riassunto già scritto coi separatori. */
export const parts = (s: string): string[] => s.split(SEP).map(x => x.trim()).filter(Boolean)

/** Quante parti entrano in `max`: `width(k)` è la larghezza delle prime k unite. 0 = nemmeno la prima: si mostra `short`. */
export function count(n: number, max: number, width: (k: number) => number): number {
  let k = n
  while (k > 0 && width(k) > max) k--
  return k
}

/** La prima parte ridotta al suo numero; senza un numero resta intera. */
export const short = (part: string): string => NUMBER.exec(part)?.[0].trim() ?? part
