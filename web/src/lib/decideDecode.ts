// Le pagine di `prove` e `decisioni` con gli elenchi sempre presenti: un relay che ne omette uno non rompe la pagina.
import type { CmdResult, DecisionsPage, PrimaRaccoltaPage, ProvePage, SeguitiPage } from './contract'
export const decodeSeguiti = (raw: string): SeguitiPage => { const p = JSON.parse(raw); return { at: 0, ...p, rimasto: p.rimasto ?? [], altrove: (p.altrove ?? []).map((r: SeguitiPage['altrove'][number]) => ({ ...r, positive: r.positive ?? [], errors: r.errors ?? [] })), promemoria: p.promemoria ?? [], promemoria_scattati: p.promemoria_scattati ?? [] } }
export const decodePrima = (raw: string): PrimaRaccoltaPage => { const p = JSON.parse(raw); return { at: 0, status: 'none', done: 0, total: 0, cost: 0, ...p, sections: p.sections ?? [] } }
export const decodeProve = (raw: string): ProvePage => { const p = JSON.parse(raw); return { at: 0, n: 0, builds: 0, ...p, groups: p.groups ?? [], ko: p.ko ?? [], red: p.red ?? [] } }
export const decodeDecisions = (raw: string): DecisionsPage => { const p = JSON.parse(raw); return { at: 0, n: 0, here: 0, tasks: 0, snoozed: 0, ...p, groups: p.groups ?? [] } }

/**
 * Una pagina letta dal PC (app-pwa-decisioni-03-11): la risposta decodificata, o `failed` se non è arrivata o non si legge. Una
 * pagina già in vista resta: un secondo tentativo fallito non la toglie.
 */
export function readPage<T>(r: CmdResult | null, decode: (raw: string) => T, shown: T | null): { page: T | null; failed: boolean } {
  if (!r?.ok) return { page: shown, failed: shown == null }
  try { return { page: decode(r.text), failed: false } } catch { return { page: shown, failed: shown == null } }
}
