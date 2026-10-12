// Le anteprime già arrivate: chiave del file (`fileKey`, sessione, percorso e misura o voce) → indirizzo del blob. Restano
// finché la pagina è aperta, così la chat che si ridisegna non le richiede al PC.
export const media: Record<string, string> = $state({})
