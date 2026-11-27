/**
 * Più di una riga: l'altezza del testo supera la sua interlinea (2 px di tolleranza per l'arrotondamento). app-play-lettura-ovunque-27-11
 * (Franz 27/11 13:29, «il play per la lettura ovunque ci siano testi più lunghi di una riga»): prima erano due righe (app-tasto-play-03-11).
 */
export function overOne(scrollHeight: number, lineHeight: number): boolean {
  const lh = Number.isFinite(lineHeight) && lineHeight > 0 ? lineHeight : 20
  return scrollHeight > lh + 2
}

/** Le parole dei paragrafi prima di `block`: la parola detta nel paragrafo letto diventa la parola del testo intero. */
export const wordsBeforeBlock = (counts: number[], block: number): number => counts.slice(0, Math.max(0, block)).reduce((n, c) => n + c, 0)
