import type { PrimaRaccoltaAsk, PrimaRaccoltaPage, PrimaRaccoltaSection, PrimaRaccoltaSummary } from './contract'

// La prima raccolta (contratto 1.100), porta di PrimaRaccolta in Decide.kt.
/** All'abbinamento la pagina si apre da sola, una volta; a raccolta finita resta la riga della home. */
export const autoOpen = (s: PrimaRaccoltaSummary | null | undefined, seen: boolean) => !seen && (s?.status === 'running' || s?.status === 'ask')
export const askConsent = (p: PrimaRaccoltaPage) => (p.consent === null || p.consent === undefined) && !!p.ask_text?.trim()
/**
 * Contratto 1.139 (app-avvio-scansione-12-11, Franz 12/11 15:23: «serve molto più sintetico»): la domanda breve con la sua riga piccola e i tasti
 * «Scansiona» e «Non ora»; senza testo o senza uno dei due tasti si torna alla domanda di prima.
 */
export const question = (p: PrimaRaccoltaPage): PrimaRaccoltaAsk | null => p.ask && p.ask.text?.trim() && p.ask.yes?.trim() && p.ask.later?.trim() ? p.ask : null
/** Mai partita: con la domanda breve è «Scansiona» ad avviarla, il tasto «Avvia la raccolta» non si aggiunge. */
export const canStart = (p: PrimaRaccoltaPage) => (p.status === 'none' && !question(p)) || p.status === 'interrupted'
/** Contratto 1.139: un'installazione che non ha mai raccolto ha `status: none` con `ask: true`, e aspetta te come `ask`. */
export const homeStatus = (s: PrimaRaccoltaSummary) => s.status === 'none' && s.ask ? 'ask' : s.status
export const asking = (s: PrimaRaccoltaSummary | null | undefined) => !!s && homeStatus(s) === 'ask'
/** Il primo giro consuma quota: prima dice quanto, parte con un tocco e una volta sola. */
export const firstRoundOffer = (p: PrimaRaccoltaPage): string | null => (p.status === 'done' && p.first_round && !p.first_round.at && p.first_round.cost?.trim()) || null
export const finished = (p: PrimaRaccoltaPage) => p.status === 'done'
export const emptySection = (s: PrimaRaccoltaSection) => s.n === 0 || !s.text?.trim()
