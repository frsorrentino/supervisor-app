// Le note di Claude Code segnate «Già lo so» su questo dispositivo, condivise fra la nota e il conto dei riquadri della sessione
// (app-riquadri-chiusi-16-11: la pillola «Avvisi» conta la nota solo finché si vede).
const KEY = 'cm.heads_up_seen'
const read = (): string[] => { try { return JSON.parse(localStorage.getItem(KEY) ?? '[]') as string[] } catch { return [] } }
export const huSeen = $state<{ set: Set<string> }>({ set: new Set(read()) })

export function markKnown(key: string) {
  huSeen.set = new Set([...huSeen.set, key])
  try { localStorage.setItem(KEY, JSON.stringify([...huSeen.set])) } catch { /* resta per questa visita */ }
}
