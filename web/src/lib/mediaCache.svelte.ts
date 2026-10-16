// Le anteprime già arrivate: chiave del file (`fileKey`, sessione, percorso e misura o voce) → indirizzo del blob. Restano
// finché la pagina è aperta, così la chat che si ridisegna non le richiede al PC.
export const media: Record<string, string> = $state({})
/** Le prime righe dei file di testo (Franz, 15/10 22:20), con la stessa chiave. */
export const texts: Record<string, { lines: string; more: boolean }> = $state({})
/** Le anteprime che non si possono fare (PDF rotto, testo illeggibile): resta la scheda. */
export const failed: Record<string, boolean> = $state({})
/** Franz, 16/10 11:18: il sorgente intero di HTML e SVG, da mostrare come pagina nel riquadro sandbox; con la stessa chiave. */
export const pages: Record<string, { mime: string; text: string }> = $state({})
