// Le pagine dello scorrimento laterale sul telefono, porta di SwipePages.kt (Franz, 03/10 15:59: «switchare tra home e
// sessioni come tra sessioni, la home sempre come prima scheda»): `null` è la home, sempre per prima; poi le sessioni vive
// nell'ordine della home, senza la master, che sta nella home. La sessione aperta è sempre una pagina, anche una chiusa.
export type Pages = (string | null)[]

/** `live`: le sessioni della lista della home, nel loro ordine, senza la master e senza le chiuse. */
export const of = (live: string[], open: string | null): Pages => [null, ...live, ...(open != null && !live.includes(open) ? [open] : [])]

/**
 * Le pagine mentre si scorre fra le sessioni (Franz, 09/10 15:46: al rilascio «una disposizione a scatti»): quelle che
 * c'erano restano nel loro ordine, le nuove in fondo nell'ordine della home, quelle sparite escono. L'ordine si rifà solo
 * tornando alla home.
 */
export const stable = (prev: Pages, fresh: Pages): Pages => [...prev.filter(p => fresh.includes(p)), ...fresh.filter(p => !prev.includes(p))]

/** Che cosa fa la striscia a ogni pagina ferma: niente, aprire quella pagina (`null` = la home), o tornare a quella aperta. */
export type Step = { type: 'none' } | { type: 'open'; name: string | null } | { type: 'scrollTo'; page: number }

/**
 * Una decisione sola fra scorrimento e riallineamento (segnalazione del 07/10 16:09). Se la pagina ferma è cambiata per uno
 * scorrimento comanda la pagina; altrimenti la striscia torna alla sessione aperta, ma mai mentre scorre. `prev` null = la
 * striscia è appena comparsa: la pagina vista non annulla la scelta fatta altrove.
 */
export function step(prev: number | null, settled: number, scrolling: boolean, pages: Pages, open: string | null): Step {
  if (scrolling) return { type: 'none' }
  if (prev != null && prev !== settled && settled >= 0 && settled < pages.length && pages[settled] !== open) return { type: 'open', name: pages[settled] }
  const i = pages.indexOf(open)
  return i < 0 || i === settled ? { type: 'none' } : { type: 'scrollTo', page: i }
}
