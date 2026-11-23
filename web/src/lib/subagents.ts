import type { Subagent, Subagents } from './contract'
import { modelName } from './conti'

// app-sottoagenti-23-11 (Franz 23/11 13:06: «sul terminale si possono vedere, su supervisor non ce n'è traccia»; 13:26 «procedi con
// la consigliata», la variante A): i sottoagenti di una sessione come righe sotto la sessione in «Al lavoro adesso», in una sezione
// che nasce chiusa e si apre col toggle delle altre. Contratto 1.158. Porta di SubagentRules.kt, coi casi di
// contract/subagents-sample.json.

/** Il riassunto della sezione: «2 al lavoro, 5 finiti». `done` sono tutti gli altri lanciati, anche quelli di giorni fa. */
export function summary(s: Subagents): { running: number; done: number } {
  return { running: Math.max(0, s.running), done: Math.max(0, s.n - s.running) }
}

/** Una riga: titolo (la descrizione, o il tipo), al lavoro o finito, tipo, modello, background, fase, token da finito, secondi. */
export type SubRow = { id: string; title: string; running: boolean; type: string; model: string | null; bg: boolean; phase: string | null; tokens: [number, number] | null; seconds: number | null }
export function row(a: Subagent, now: number): SubRow {
  const running = !a.finished
  const seconds = running ? (a.started != null ? Math.max(0, now - a.started) : null)
    : a.started != null && a.ended != null ? Math.max(0, a.ended - a.started) : null
  return {
    id: a.id, title: a.desc.trim() || a.type, running, type: a.type, model: a.model ? modelName(a.model) : null, bg: a.bg, phase: a.phase ?? null,
    tokens: a.tokens && !running ? [a.tokens.in, a.tokens.out] : null, seconds,
  }
}
export const rows = (s: Subagents, now: number) => s.items.map(a => row(a, now))

/** «Tutti e 7»: quando la sessione ne ha lanciati più di quelli nelle righe. */
export const more = (s: Subagents): number | null => (s.n > s.items.length ? s.n : null)

/** «69k», «211k», «0,6k», «1,2M»: i token corti, con la virgola in italiano. */
export function tokens(n: number, en = false): string {
  const one = (x: number) => { const t = x.toFixed(1).replace(/\.0$/, ''); return en ? t : t.replace('.', ',') }
  if (n >= 1_000_000) return `${one(n / 1_000_000)}M`
  if (n >= 10_000) return `${Math.round(n / 1000)}k`
  return `${one(n / 1000)}k`
}

const NB = ' '
/** «18 s», «4 m», «1 h 05»: quanto ha lavorato o da quanto lavora. */
export function duration(s: number): string {
  if (s < 60) return `${s}${NB}s`
  if (s < 3600) return `${Math.floor(s / 60)}${NB}m`
  return `${Math.floor(s / 3600)}${NB}h${NB}${String(Math.floor((s % 3600) / 60)).padStart(2, '0')}`
}

/** La risposta dell'op `subagents`: null se la sessione non ne ha lanciati o il testo non torna. */
export function parseAnswer(text: string): Subagents | null {
  try { const j = JSON.parse(text) as { subagents?: Subagents | null }; return j?.subagents && Array.isArray(j.subagents.items) ? j.subagents : null } catch { return null }
}
