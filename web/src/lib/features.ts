// La panoramica delle funzioni (Franz 01/11 20:39), porta di Features.kt: un elenco solo, letto da telefono e web. `version` è il primo
// commit con la prova nel codice, non la nascita della funzione: sulla pagina non si mostra.
import raw from '../../../mobile/src/main/assets/features.json?raw'
export type Text = { it: string; en: string }
export type Proof = { file: string; needle: string }
export type Feature = { key: string; group: string; name: Text; what: Text; where: Text; when: Text; how: Text; image?: string | null; state: 'ready' | 'coming'; version: string | null; route: string | null; section: string | null; phone: Proof | null; web: Proof | null }
export type Group = { key: string; it: string; en: string }
export type Catalog = { groups: Group[]; features: Feature[] }
export const catalog: Catalog = JSON.parse(raw)
/** I gruppi con le loro voci, in ordine; i vuoti non ci sono. */
export const grouped = (c: Catalog) => c.groups.map(g => ({ group: g, rows: c.features.filter(f => f.group === g.key) })).filter(x => x.rows.length)
/** Le sezioni della home senza una voce: devono essere zero. */
export const missingSections = (c: Catalog, sections: readonly string[]) => sections.filter(s => !c.features.some(f => f.section === s))
/** «Portami lì» solo se la funzione c'è e ha un posto suo: la home non conta, dice meno di quello che si vedeva già (Franz 01/11 22:05). */
export const canGo = (f: Feature) => f.state === 'ready' && f.route != null && f.route !== 'home'
