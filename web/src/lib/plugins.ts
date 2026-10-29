import type { Plugin, PluginsPage, State } from './contract'

// La sezione «Plugin e skill» della home (contratto 1.95, mockup approvato da Franz il 29/10 alle 10:39), porta di Plugins.kt.

/** Quanti aggiornamenti in tutto, dal riassunto dello stato; null con un relay precedente. */
export const pending = (st: State): number | null => (st.plugins ? st.plugins.accounts.reduce((n, a) => n + a.updates.length, 0) : null)

/** Gli account con almeno un aggiornamento, quelli a cui va «Aggiorna tutto». */
export const accountsWithUpdates = (st: State): string[] => (st.plugins?.accounts ?? []).filter(a => a.updates.length).map(a => a.account)

export type Account = { account: string; updates: Plugin[]; rest: Plugin[]; skills: number; skillNames: string[] }

const byName = (a: Plugin, b: Plugin) => a.name.localeCompare(b.name)

/** Un account dell'elenco: prima quelli da aggiornare, poi gli altri per nome. */
export function account(a: PluginsPage['accounts'][number]): Account {
  return {
    account: a.account,
    updates: a.plugins.filter(p => p.update === true).sort(byName),
    rest: a.plugins.filter(p => p.update !== true).sort(byName),
    skills: a.skills.length,
    skillNames: a.skills.map(s => (s.version ? `${s.name} ${s.version}` : s.name)),
  }
}

/** L'argomento di `plugin_update`. */
export const updateArg = (account: string, p: Plugin) => `${account}:${p.id}`
