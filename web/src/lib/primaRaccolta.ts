import type { PrimaRaccoltaPage, PrimaRaccoltaSection, PrimaRaccoltaSummary } from './contract'

// La prima raccolta (contratto 1.100), porta di PrimaRaccolta in Decide.kt.
/** All'abbinamento la pagina si apre da sola, una volta; a raccolta finita resta la riga della home. */
export const autoOpen = (s: PrimaRaccoltaSummary | null | undefined, seen: boolean) => !seen && (s?.status === 'running' || s?.status === 'ask')
export const askConsent = (p: PrimaRaccoltaPage) => (p.consent === null || p.consent === undefined) && !!p.ask_text?.trim()
export const canStart = (p: PrimaRaccoltaPage) => p.status === 'none' || p.status === 'interrupted'
/** Il primo giro consuma quota: prima dice quanto, parte con un tocco e una volta sola. */
export const firstRoundOffer = (p: PrimaRaccoltaPage): string | null => (p.status === 'done' && p.first_round && !p.first_round.at && p.first_round.cost?.trim()) || null
export const finished = (p: PrimaRaccoltaPage) => p.status === 'done'
export const emptySection = (s: PrimaRaccoltaSection) => s.n === 0 || !s.text?.trim()
