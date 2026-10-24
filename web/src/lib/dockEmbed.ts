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
