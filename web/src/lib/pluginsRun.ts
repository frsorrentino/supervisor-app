import type { PluginsPage } from './contract'

// I comandi di «Plugin e skill», fuori dal disegno (Franz 31/10 15:31; PluginsRun.kt nell'app): un aggiornamento dura decine di
// secondi, quindi dopo la conferma lo stato «in corso» si vede subito e la seconda pressione non manda un altro comando. All'esito
// resta il testo del relay e la lista si rilegge: il riassunto di /state.plugins arriva solo con la push dopo.
export type PluginOp = 'plugin_check' | 'plugin_update' | 'plugin_update_all'
export type RunNote = { ok: boolean; text: string | null }
export type RunState = { page: PluginsPage | null; loading: boolean; busy: Record<string, boolean>; notes: Record<string, RunNote>; all: boolean }

export function pluginsRun(load: () => Promise<PluginsPage | null>, run: (op: PluginOp, arg: string | null) => Promise<{ ok: boolean; text: string } | null>) {
  let s: RunState = { page: null, loading: false, busy: {}, notes: {}, all: false }
  let listener: ((v: RunState) => void) | null = null
  const set = (p: Partial<RunState>) => { s = { ...s, ...p }; listener?.(s) }
  async function refresh() { set({ loading: true }); const p = await load(); set({ page: p ?? s.page, loading: false }) }
  async function act(key: string, op: PluginOp, arg: string | null, note: string) {
    if (s.busy[key]) return
    set({ busy: { ...s.busy, [key]: true } })
    const r = await run(op, arg)
    set({ busy: { ...s.busy, [key]: false }, notes: { ...s.notes, [note]: r ? { ok: r.ok, text: r.text } : { ok: false, text: null } } })
    if (op !== 'plugin_check') await refresh()
  }
  async function updateAll(accounts: string[]) {
    if (s.all) return
    set({ all: true })
    for (const a of accounts) await act(`all:${a}`, 'plugin_update_all', a, a)
    set({ all: false })
  }
  return { get: () => s, sub: (f: (v: RunState) => void) => { listener = f }, refresh, act, updateAll }
}
