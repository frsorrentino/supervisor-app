// `/embed?dock=1`: la master dentro Studio (compito studio-dock-coerente-24-10, Franz 24/10 13:07). La pagina è trasparente e Studio
// la tiene fissa in basso; a ogni cambio la pagina gli dice stato e altezza, e Studio adatta il riquadro.
// Protocollo concordato con console-personale: {type:'cm-dock', state, height}, height in px CSS, solo verso Studio.
// app-posizioni-sistema-27-11 gruppo 6: dentro c'è il foglio della master della web app (MasterSheet), con le sue tre misure; l'altezza
// è quella del foglio, a ogni passo del dito e della molla ('dragging'), poi alla misura dove si ferma: 'closed' alla riga, 'open' a
// metà e intera. Il protocollo resta quello di prima.
import type { Stop } from './masterSheet'

export const STUDIO = 'https://studio.francescosorrentino.com'
export type DockState = 'closed' | 'open' | 'dragging' | 'unpaired'
export type DockMessage = { type: 'cm-dock'; state: DockState; height: number }

export const dockMessage = (state: DockState, height: number): DockMessage => ({ type: 'cm-dock', state, height: Math.max(0, Math.round(height)) })

export const dockWanted = (path: string, search: string) => /^\/embed(\.html)?$/.test(path) && new URLSearchParams(search).get('dock') === '1'

/**
 * Cosa dire a Studio: l'altezza del foglio e la misura dove sta ('move' mentre il dito o la molla lo portano). Con un menu o una finestra
 * aperti il riquadro prende tutto il posto (`space`), così il menu del campo o del ⋮ non resta chiuso nella riga; mai durante il gesto.
 */
export function dockFrame(at: Stop | 'move', height: number, space: number, popup: boolean): { state: DockState; height: number } {
  if (at === 'move') return { state: 'dragging', height: Math.round(height) }
  if (popup) return { state: 'open', height: Math.round(space) }
  return { state: at === 'row' ? 'closed' : 'open', height: Math.round(height) }
}

/**
 * Il posto del foglio dentro Studio: si parte da tutto lo schermo, e se Studio dà al riquadro meno di quanto chiesto (lo ferma sotto la
 * sua barra in alto) il posto diventa il riquadro più alto che Studio concede; così il foglio intero sta tutto nel riquadro, con lo stacco
 * sopra come nella web app, e la sua riga resta in vista. `sent`: l'altezza chiesta, `inner`: quella del riquadro, a riquadro fermo.
 */
export const learnSpace = (space: number, sent: number, inner: number) => (inner + 1 < sent ? Math.round(inner) : space)

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
