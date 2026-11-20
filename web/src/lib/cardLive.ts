import type { ProvaOne, WaitItem } from './contract'

// app-carte-stato-vivo-20-11 (Franz 20/11 20:38: «ci sono ancora incoerenze dopo approvazioni in un punto risultano da approvare in
// altri»): la carta di una prova nei messaggi della chat legge lo stesso stato di «Per te» e «Dalla master». L'aperto viene da `attese`
// (contratto 1.151); l'esito lo decide la prova letta con `prove N`, mai la sola assenza dalle aperte: fuori dalle aperte ma ancora
// `todo` (ritirata, compito chiuso, non ancora riletta) niente tasti e «non è più da fare», senza dirla confermata. Porta di CardLive.kt.

export type CardKind = 'loading' | 'open' | 'ok' | 'ko' | 'red' | 'not_open'
export type CardView = { kind: CardKind; at?: number | null; text?: string | null }

/** Le prove aperte secondo `attese`: le prove `todo` delle verifiche ancora aperte. null = non si sa. */
export function openProve(items: WaitItem[] | null | undefined): Set<number> | null {
  if (!items) return null
  return new Set(items.filter(w => (w.state ?? 'open') === 'open').flatMap(w => (w.tests ?? []).filter(t => t.state === 'todo').map(t => t.id)))
}

/** La carta della prova `p` (null = in arrivo) con le aperte `open`. */
export function provaView(p: ProvaOne | null | undefined, open: Set<number> | null): CardView {
  if (!p) return { kind: 'loading' }
  const at = p.at > 0 ? p.at : null
  const text = p.text?.trim() ? p.text : null
  if (p.state === 'ok' || p.state === 'ko' || p.state === 'red') return { kind: p.state, at, text }
  if (open && !open.has(p.id)) return { kind: 'not_open' }
  return { kind: 'open' }
}

/** Le carte in cache da rileggere: quelle che mostrano ancora da fare una prova che `attese` non ha più fra le aperte. */
export function staleProve(cached: Record<number, ProvaOne | null | undefined>, open: Set<number> | null): number[] {
  if (!open) return []
  return Object.entries(cached).filter(([n, p]) => p && p.state === 'todo' && !open.has(Number(n))).map(([n]) => Number(n)).sort((a, b) => a - b)
}

/** L'ora dell'esito: «20:31» oggi, «12/11 20:31» un altro giorno (ora locale). */
export function whenText(at: number, now: number): string {
  const t = new Date(at * 1000), n = new Date(now * 1000)
  const hm = `${String(t.getHours()).padStart(2, '0')}:${String(t.getMinutes()).padStart(2, '0')}`
  return t.toDateString() === n.toDateString() ? hm : `${t.getDate()}/${t.getMonth() + 1} ${hm}`
}

/**
 * La riga di una prova risolta, uguale al telefono (`provaDoneLine` in ProvaCards.kt): «Confermata: va · 20:31», «Non va · 20:31: <motivo>»,
 * «Confermata, controllo rosso · 20:31», «Non è più da fare». null per una prova aperta o in arrivo.
 */
export function provaDoneLine(v: CardView, now: number, t: { provaDoneOk: string; provaDoneKo: string; provaStateRed: string; provaNotOpen: string }): string | null {
  if (v.kind === 'not_open') return t.provaNotOpen
  const head = v.kind === 'ok' ? t.provaDoneOk : v.kind === 'ko' ? t.provaDoneKo : v.kind === 'red' ? t.provaStateRed : null
  if (!head) return null
  return [[head, v.at ? whenText(v.at, now) : null].filter(Boolean).join(' · '), v.text].filter(Boolean).join(': ')
}

/** Il no del relay che vuol dire «già fatto» per una prova: non è un errore. Come `ProveOk.alreadyDone`. */
export function provaAlreadyDone(text: string | null | undefined): boolean {
  const s = text ?? ''
  return /\bprova \d+/i.test(s) && /già confermata|non c'è più|already (confirmed|gone|done)|no longer/i.test(s)
}
