// `/embed?dock=1`: la barra della master dentro Studio (compito studio-dock-coerente-24-10, Franz 24/10 13:07). La pagina è
// trasparente e Studio la tiene fissa in basso; a ogni cambio la pagina gli dice stato e altezza, e Studio adatta il riquadro.
// Protocollo concordato con console-personale: {type:'cm-dock', state, height}, height in px CSS, solo verso Studio.
export const STUDIO = 'https://studio.francescosorrentino.com'
export type DockState = 'closed' | 'open' | 'dragging' | 'unpaired'
export type DockMessage = { type: 'cm-dock'; state: DockState; height: number }

export const dockMessage = (state: DockState, height: number): DockMessage => ({ type: 'cm-dock', state, height: Math.max(0, Math.round(height)) })

export const dockWanted = (path: string, search: string) => /^\/embed(\.html)?$/.test(path) && new URLSearchParams(search).get('dock') === '1'

/** L'altezza del riquadro mentre il dito trascina la barra: `dy` negativo è verso l'alto. */
export const dragHeight = (start: number, dy: number, closed: number, open: number) => Math.min(open, Math.max(closed, start - dy))

/** Lasciata la barra: come MasterDock nella web app, oltre 40 px in su apre e oltre 40 px in giù chiude. */
export const settle = (expanded: boolean, dy: number) => (expanded ? dy <= 40 : dy < -40)

// L'ultima barra vista, per disegnarla appena la pagina parte (Franz dal telefono, 24/10 13:47): a ogni cambio di pagina
// Studio ricarica il riquadro, e il relay risponde dopo qualche secondo. Basta la sessione e l'ultima risposta della master,
// che dà la riga dell'esito (masterHome.hero); il relay la sostituisce appena arriva.
export const DOCK_CACHE = 'cm.dock_bar'
export type DockCache<S, E> = { master: S; entries: E[] }

export function dockCacheOf<S, E extends { role: string; text?: string | null }>(master: S, entries: E[]): string {
  const last = [...entries].reverse().find(e => e.role !== 'user' && e.role !== 'tool' && e.text?.trim())
  return JSON.stringify({ master, entries: last ? [last] : [] })
}

export function readDockCache<S, E>(raw: string | null): DockCache<S, E> | null {
  if (!raw) return null
  try {
    const c = JSON.parse(raw)
    return c && typeof c === 'object' && c.master && Array.isArray(c.entries) ? c : null
  } catch { return null }
}
