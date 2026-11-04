// La master nella PWA larga si sposta e si misura coi gesti (Franz 03/11 22:06-22:08, app-master-trascina-03-11): la testata
// trascinata cambia solo il lato; il bordo verso le sessioni allarga e stringe fra due posti fissi (colonna e metà schermo); il
// bordo superiore cambia l'altezza a scatti. Qui le regole, senza la pagina.
export type Side = 'left' | 'right'
export type Height = 'column' | 'half' | 'compact' | 'bar'
export type Rect = { x: number; y: number; w: number; h: number }
export type Zone<K> = { key: K; rect: Rect }

/** La colonna stretta della master. */
export const COLUMN = 420
/** Il posto di adesso della larghezza tiene finché il dito non passa il punto di mezzo di tanto. */
export const WIDTH_MARGIN = 28

/** Le due zone del lato: la metà sinistra e la metà destra della pagina. */
export const sideZones = (w: number, h: number): Zone<Side>[] => [
  { key: 'left', rect: { x: 0, y: 0, w: w / 2, h } },
  { key: 'right', rect: { x: w / 2, y: 0, w: w / 2, h } },
]
export const sideAt = (w: number, x: number): Side => (x < w / 2 ? 'left' : 'right')

/**
 * I posti fissi della larghezza (Franz 04/11 10:29-10:30, app-trascina-rifinitura-04-11: «meglio posizioni fisse», «così la
 * calamita ha più senso e può essere più forte»): la colonna e la metà schermo, se è più larga della colonna.
 */
export function widthStops(screen: number): number[] {
  const half = Math.round(screen / 2)
  return half > COLUMN ? [COLUMN, half] : [COLUMN]
}
/** La calamita della larghezza: sempre il posto fisso più vicino, mai una misura nel mezzo; `now` tiene entro WIDTH_MARGIN. */
export function widthAt(px: number, screen: number, now: number | null = null): number {
  const stops = widthStops(screen)
  const near = stops.reduce((a, b) => (Math.abs(px - b) < Math.abs(px - a) ? b : a))
  return now != null && stops.includes(now) && Math.abs(px - now) - Math.abs(px - near) < WIDTH_MARGIN ? now : near
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
