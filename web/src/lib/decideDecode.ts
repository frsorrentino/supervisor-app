// Le pagine di `prove` e `decisioni` con gli elenchi sempre presenti: un relay che ne omette uno non rompe la pagina.
import type { DecisionsPage, ProvePage } from './contract'
export const decodeProve = (raw: string): ProvePage => { const p = JSON.parse(raw); return { at: 0, n: 0, builds: 0, ...p, groups: p.groups ?? [], ko: p.ko ?? [], red: p.red ?? [] } }
export const decodeDecisions = (raw: string): DecisionsPage => { const p = JSON.parse(raw); return { at: 0, n: 0, here: 0, tasks: 0, snoozed: 0, ...p, groups: p.groups ?? [] } }
