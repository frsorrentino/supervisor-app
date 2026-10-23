// I link `https://claude.ai/code/session_<id>` nei testi della chat (Franz, 21/10 19:04: «piuttosto che mandare il link
// all'app claude possiamo mandare link o tasto che apra sessioni in app supervisor?»), porta di SessionLinks.kt. Un link
// uguale al `link` di una sessione dello stato esce dal testo e diventa il tasto «Apri in supervisor» col nome della
// sessione; il link com'era resta nel tasto (tasto destro o pressione lunga). Un link di sessione che lo stato non conosce
// resta un link normale. Un link con testo `[legnaia](url)` lascia il suo testo. La riga che senza il link è solo
// un'etichetta («• **Link:**») sparisce, come le parentesi rimaste vuote; i due punti rimasti in coda a una frase se ne vanno.
import type { Session } from './contract'
import { findLinks } from './links'

export type SessionButton = { session: string; url: string }
export type Stripped = { text: string; buttons: SessionButton[] }

const MD_LINK = /\[([^\]\n]+)]\((https?:\/\/[^)\s]+)\)/g
const EMPTY_PARENS = /\(\s*\)|«\s*»/g
const SPACES = / {2,}/g
const ORPHAN = /\s+([.,;:!?»)])/g
const EATEN = '.,;:!?'
const BULLET = /^(\s*)([-*•]|\d+[.)])\s+/
const MARKS = /[*_`#]+/g
const TRAILING_COLON = /\s*:\s*([*_`]*)\s*$/
const LABEL_WORDS = 3

const norm = (u: string) => u.split('#')[0].split('?')[0].replace(/\/+$/, '').trim()

/** La sessione il cui `link` è `url`, null se nessuna; la coda `?`, `#` e `/` non conta. */
export function sessionOf(url: string, sessions: Session[]): Session | null {
  const key = norm(url)
  return sessions.find(s => (s.link ?? '').trim() !== '' && norm(s.link ?? '') === key) ?? null
}

/** Il testo senza i link delle sessioni note, e un tasto per sessione nell'ordine in cui compaiono. */
export function stripSessionLinks(text: string, sessions: Session[]): Stripped {
  if (!text.includes('://') || !sessions.some(s => (s.link ?? '').trim() !== '')) return { text, buttons: [] }
  const buttons: SessionButton[] = []
  const note = (s: Session, url: string) => { if (!buttons.some(b => b.session === s.name)) buttons.push({ session: s.name, url }) }
  let t = text.replace(MD_LINK, (m: string, label: string, url: string) => { const s = sessionOf(url, sessions); if (!s) return m; note(s, url); return label })
  const bare = findLinks(t).filter(([a, b]) => { const s = sessionOf(t.slice(a, b), sessions); if (s) note(s, t.slice(a, b)); return !!s })
  if (!buttons.length) return { text, buttons: [] }
  for (const [a, b] of [...bare].reverse()) {
    // La punteggiatura attaccata al link («url.», «url,») va via con lui, se dopo c'è uno spazio o la fine della riga.
    let end = b
    while (end < t.length && EATEN.includes(t[end])) end++
    if (end < t.length && !/\s/.test(t[end])) end = b
    t = t.slice(0, a) + t.slice(end)
  }
  const before = text.split('\n')
  const after = t.split('\n')
  const out: (string | null)[] = after.map((l, i) => (l === before[i] ? l : tidy(l, before[i].match(/^[ \t]*/)![0])))
  // L'etichetta sulla riga sopra un link che stava da solo («• **Link:**» e a capo l'indirizzo) se ne va con lui.
  for (let i = 1; i < after.length; i++) { const p = out[i - 1]; if (out[i] == null && p != null && isLabel(p)) out[i - 1] = null }
  return { text: out.filter((l): l is string => l != null).join('\n').replace(/\n{3,}/g, '\n\n').replace(/^\n+|\n+$/g, ''), buttons }
}

/** Una riga da cui è uscito un link, col rientro che aveva: null se non resta niente da leggere. */
function tidy(line: string, indent: string): string | null {
  let l = line.replace(EMPTY_PARENS, '')
  l = indent + l.trim().replace(SPACES, ' ').replace(ORPHAN, '$1')
  if (core(l) === '' || isLabel(l)) return null
  return l.replace(TRAILING_COLON, '$1').trimEnd()
}

/** La riga senza il pallino dell'elenco e i segni del markdown. */
const core = (line: string) => line.replace(BULLET, '').replace(MARKS, '').trim()

/** «Link:», «• **Apri qui:**»: fino a tre parole e i due punti, niente altro. */
function isLabel(line: string): boolean {
  const c = core(line)
  return c.endsWith(':') && c.replace(/:+$/, '').trim().split(/\s+/).length <= LABEL_WORDS
}

/** La sessione chiesta dall'indirizzo: `?s=<nome>` (notifiche e Telegram), altrimenti `#nome` di prima. */
export function askedSession(search: string, hash: string): string | null {
  const q = new URLSearchParams(search).get('s')?.trim()
  if (q) return q
  let h: string
  try { h = decodeURIComponent(hash.slice(1)) } catch { h = hash.slice(1) }
  // Niente chiesto è null, mai '' (23/10): con '' la web locale apriva la pagina di una sessione senza nome e la striscia
  // aveva due pagine con la stessa chiave.
  return h.trim() || null
}
