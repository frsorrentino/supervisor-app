// Il riquadro «Rilascia per allegare» di una chat (app-web-trascina-riquadro-12-11). Franz, 12/11 dal Mac: trascinando un file
// su una sessione e passando sopra un'altra, quella di mezzo restava col riquadro anche dopo il rilascio nella sessione giusta.
// `dragleave` arriva dall'elemento interno sotto il puntatore, non dalla colonna, e la colonna non si spegneva. Qui il riquadro
// resta acceso solo finché arrivano i `dragover` (il browser li manda di continuo mentre il file è sopra, al più ogni 550 ms
// da fermo) e si spegne subito quando il puntatore esce davvero dalla colonna o il file viene rilasciato in qualunque punto.

/** Senza dragover per tanto il file non è più sopra la colonna. */
export const DRAG_IDLE_MS = 700

export function dragHold(set: (on: boolean) => void, idleMs = DRAG_IDLE_MS) {
  let timer: ReturnType<typeof setTimeout> | undefined
  const off = () => { clearTimeout(timer); timer = undefined; set(false) }
  return {
    /** Un dragover col file sopra la colonna. */
    over() { set(true); clearTimeout(timer); timer = setTimeout(off, idleMs) },
    /** Un dragleave; `outside` = il puntatore è uscito dalla colonna (non è solo passato a un elemento interno). */
    leave(outside: boolean) { if (outside) off() },
    /** Rilascio o fine del trascinamento, qui o altrove. */
    off,
  }
}
