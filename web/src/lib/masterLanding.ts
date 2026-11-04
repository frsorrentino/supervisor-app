// La master nella PWA larga si sposta e si misura coi gesti (Franz 03/11 22:06-22:08, app-master-trascina-03-11): la testata
// trascinata cambia solo il lato; il bordo verso le sessioni allarga e stringe, con due scatti (colonna e metà schermo) e la
// misura libera fra i due; il bordo superiore cambia l'altezza a scatti. Qui le regole, senza la pagina.
export type Side = 'left' | 'right'
export type Height = 'column' | 'half' | 'compact' | 'bar'
export type Rect = { x: number; y: number; w: number; h: number }
export type Zone<K> = { key: K; rect: Rect }

/** La colonna stretta della master e i limiti della misura libera. */
export const COLUMN = 420
export const MIN_W = 360
/** Entro questa distanza da uno scatto la larghezza ci si ferma. */
export const SNAP = 32

/** Le due zone del lato: la metà sinistra e la metà destra della pagina. */
export const sideZones = (w: number, h: number): Zone<Side>[] => [
  { key: 'left', rect: { x: 0, y: 0, w: w / 2, h } },
  { key: 'right', rect: { x: w / 2, y: 0, w: w / 2, h } },
]
export const sideAt = (w: number, x: number): Side => (x < w / 2 ? 'left' : 'right')

/** La larghezza mentre si trascina il bordo: fra MIN_W e il 60 % dello schermo. */
export const clampWidth = (px: number, screen: number) => Math.round(Math.max(MIN_W, Math.min(screen * 0.6, px)))
/** Al rilascio: vicina a uno scatto (colonna, metà schermo) ci si ferma, altrimenti resta dove la si lascia. */
export function snapWidth(px: number, screen: number): number {
  const w = clampWidth(px, screen)
  for (const s of [COLUMN, Math.round(screen / 2)]) if (Math.abs(w - s) <= SNAP) return s
  return w
}

/**
 * Le bande del bordo superiore sulla colonna della master, dall'alto: colonna intera, sotto la home a metà, compatta, e in
 * fondo la riga. `under` false (nessuna sessione aperta, la master non sta sotto la home): solo colonna e riga.
 */
export function heightZones(col: Rect, under: boolean): Zone<Height>[] {
  const cuts: [Height, number][] = under ? [['column', 0.15], ['half', 0.6], ['compact', 0.88], ['bar', 1]] : [['column', 0.85], ['bar', 1]]
  let y = col.y
  return cuts.map(([key, f]) => { const end = col.y + col.h * f; const z = { key, rect: { x: col.x, y, w: col.w, h: end - y } }; y = end; return z })
}
export function heightAt(zs: Zone<Height>[], y: number): Height {
  for (const z of zs) if (y < z.rect.y + z.rect.h) return z.key
  return zs[zs.length - 1].key
}

/** Quanto deve restare sotto il dito un bersaglio nuovo prima di prenderlo (app-trascina-rifinitura-04-11). */
export const DWELL_MS = 140

/**
 * La sosta (app-trascina-rifinitura-04-11, Franz 04/11 08:20: «dalla riga in basso c'è sfarfallio»): il bersaglio di adesso
 * tiene; uno nuovo vince solo se resta sotto il dito per `dwell` ms di fila. `feed` dice il bersaglio da mostrare e, se uno nuovo
 * aspetta, fra quanti ms riguardare anche senza altri movimenti del dito.
 */
export function steady<K>(start: K, dwell = DWELL_MS) {
  let cur = start
  let cand: { key: K; since: number } | null = null
  return {
    get current() { return cur },
    feed(key: K, now: number): { on: K; wait: number | null } {
      if (key === cur) { cand = null; return { on: cur, wait: null } }
      if (!cand || cand.key !== key) cand = { key, since: now }
      const left = dwell - (now - cand.since)
      if (left <= 0) { cur = key; cand = null; return { on: cur, wait: null } }
      return { on: cur, wait: left }
    },
  }
}
