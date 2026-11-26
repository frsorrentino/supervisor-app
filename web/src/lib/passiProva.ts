import type { ProofStep, ProofSteps } from './contract'

// Le tappe della prova su un filo (voce 5 della prima proposta di animazioni, app-animazioni-passi-26-11, contratto 1.162), porta di
// PassiProva.kt.
export type Look = 'done' | 'now' | 'failed' | 'todo'
export const look = (s: ProofStep): Look => (s.state === 'ok' ? 'done' : s.state === 'run' ? 'now' : s.state === 'ko' ? 'failed' : 'todo')
/** Fin dove arriva il filo: la prima tappa non passata (tutte passate = l'ultima). */
export function reach(items: ProofStep[]): number {
  const i = items.findIndex(s => s.state !== 'ok')
  return i < 0 ? Math.max(0, items.length - 1) : i
}
/** Il filo pieno da 0 a 1. */
export const fill = (items: ProofStep[]): number => (items.length <= 1 ? 1 : reach(items) / (items.length - 1))
export type Say = 'running' | 'failed' | 'todo' | 'all_done'
/** La riga sotto il filo; null senza tappe. */
export function line(steps: ProofSteps | null | undefined): { say: Say; label: string } | null {
  const items = steps?.items ?? []
  if (!items.length) return null
  const ko = items.find(s => s.state === 'ko'); if (ko) return { say: 'failed', label: ko.label }
  const run = items.find(s => s.state === 'run'); if (run) return { say: 'running', label: run.label }
  const todo = items.find(s => s.state !== 'ok'); if (todo) return { say: 'todo', label: todo.label }
  return { say: 'all_done', label: '' }
}
