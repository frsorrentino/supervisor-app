// I riquadri della sessione chiusi come pillole (app-riquadri-chiusi-16-11, Franz 16/11 20:02, primo pezzo del passo 3 del
// riallineamento, docs/valutazioni/2026-11-16-chiarezza-sessione.md): Per te, Risultati, Azioni, Contesto, Altre sessioni e Avvisi
// nascono chiusi, una pillola col numero, e si aprono al tocco. Da solo si apre soltanto «Per te» quando ha una voce che aspetta la
// persona; chiuso a mano resta chiuso finché non arriva una voce nuova. Lo stato resta per sessione. Come Riquadri.kt, coi casi di
// core/src/test/resources/riquadri-cases.json.
export type Kind = 'perte' | 'risultati' | 'azioni' | 'contesto' | 'altre' | 'avvisi'
export const KINDS: readonly Kind[] = ['perte', 'risultati', 'azioni', 'contesto', 'altre', 'avvisi']

/** Lo stato di una sessione: i riquadri aperti o chiusi a mano; `keys` le voci di «Per te» che aspettavano quando è stato chiuso. */
export type BoxState = { open?: Partial<Record<Kind, boolean>>; keys?: string[] }

/** Aperto adesso. `waiting`: le chiavi delle voci di Per te che aspettano la persona (non i risultati da guardare). */
export function isOpen(st: BoxState, kind: Kind, waiting: readonly string[] = []): boolean {
  const o = st.open?.[kind]
  if (kind !== 'perte') return o === true
  if (o === true) return true
  if (o === false) return waiting.some(k => !(st.keys ?? []).includes(k))
  return waiting.length > 0
}

/** Il tocco: apre o chiude. Chiudendo «Per te» si ricordano le voci che aspettavano, così una nuova lo riapre. */
export function toggle(st: BoxState, kind: Kind, waiting: readonly string[] = []): BoxState {
  const was = isOpen(st, kind, waiting)
  const open = { ...st.open, [kind]: !was }
  return kind === 'perte' && was ? { ...st, open, keys: [...waiting] } : { ...st, open }
}

/** Lo stato salvato di tutte le sessioni; qualunque cosa illeggibile vale nessuno stato. */
export function parseBoxes(raw: string | null): Record<string, BoxState> {
  try { const o = JSON.parse(raw ?? '{}'); return o && typeof o === 'object' && !Array.isArray(o) ? o : {} } catch { return {} }
}
