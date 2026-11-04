/** Più di due righe: l'altezza del testo supera due volte la sua interlinea (2 px di tolleranza per l'arrotondamento). */
export function overTwo(scrollHeight: number, lineHeight: number): boolean {
  const lh = Number.isFinite(lineHeight) && lineHeight > 0 ? lineHeight : 20
  return scrollHeight > lh * 2 + 2
}
