// Dove si può lasciare la master trascinandola nella PWA larga (Franz 03/11 18:50, app-master-trascina-03-11): mentre la si
// trascina compaiono i punti di atterraggio, e al rilascio la master va in quello sotto il dito. Le stesse posizioni del tasto
// Posizione: una riga in basso, colonna intera, metà schermo, metà colonna sotto la home; ognuna a destra o a sinistra.
export type Side = 'left' | 'right'
export type Place = 'bar' | 'column' | 'half' | 'under'
export type Landing = { side: Side; place: Place }
export type Rect = { x: number; y: number; w: number; h: number }
export type Zone = Landing & { rect: Rect }

/** La larghezza della colonna della master (`.dpanel`, stretta) e l'altezza della barra in fondo. */
export const COLUMN = 420
export const BAR = 96

/**
 * Le zone sulla pagina larga `w`×`h`. `under`: la master può stare sotto la home (solo con le sessioni aperte). Sui due lati:
 * la colonna esterna (alta: colonna intera; bassa, se `under`: metà colonna con la home) e la fascia verso il centro fino a
 * metà schermo (metà schermo). In fondo, a tutta larghezza, la riga.
 */
export function zones(w: number, h: number, under: boolean): Zone[] {
  const top = h - BAR
  const col = Math.min(COLUMN, w / 4)
  const out: Zone[] = [{ side: 'right', place: 'bar', rect: { x: 0, y: top, w, h: BAR } }]
  for (const side of ['left', 'right'] as const) {
    const cx = side === 'left' ? 0 : w - col
    const hx = side === 'left' ? col : w / 2
    out.push({ side, place: 'column', rect: { x: cx, y: 0, w: col, h: under ? top / 2 : top } })
    if (under) out.push({ side, place: 'under', rect: { x: cx, y: top / 2, w: col, h: top / 2 } })
    out.push({ side, place: 'half', rect: { x: hx, y: 0, w: w / 2 - col, h: top } })
  }
  return out
}

const inside = (r: Rect, x: number, y: number) => x >= r.x && x < r.x + r.w && y >= r.y && y < r.y + r.h

/** La zona sotto il dito; null al centro della pagina, dove lasciarla non cambia niente. */
export const landingAt = (zs: Zone[], x: number, y: number): Zone | null => zs.find(z => inside(z.rect, x, y)) ?? null

/** La posizione di adesso, per evidenziare la zona dove la master sta già. */
export function current(s: { open: boolean; left: boolean; under: boolean; half: boolean }): Landing {
  const side: Side = s.left ? 'left' : 'right'
  if (!s.open) return { side: 'right', place: 'bar' }
  if (s.under) return { side, place: 'under' }
  return { side, place: s.half ? 'column' : 'half' }
}
