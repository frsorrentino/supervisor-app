// I link `https://claude.ai/code/session_<id>` nei testi della chat (Franz, 21/10 19:04: «piuttosto che mandare il link
// all'app claude possiamo mandare link o tasto che apra sessioni in app supervisor?»), porta di SessionLinks.kt. Un link
// uguale al `link` di una sessione dello stato esce dal testo e diventa il tasto «Apri in supervisor» col nome della
// sessione; il link com'era resta nel tasto (tasto destro o pressione lunga). Un link di sessione che lo stato non conosce
// resta un link normale. Un link con testo `[officina](url)` lascia il suo testo. La riga che senza il link è solo
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
/** Contratto 1.130: `#/attesa/<id>` (id «w» e 10 cifre esadecimali) apre la scheda «Ti aspetta»; null per ogni altro indirizzo. */
export function askedWait(hash: string): string | null {
  return /^#\/attesa\/(w[0-9a-f]{10})$/.exec(hash.trim())?.[1] ?? null
}

export function askedSession(search: string, hash: string): string | null {
  const q = new URLSearchParams(search).get('s')?.trim()
  if (q) return q
  // Un indirizzo della web (`#/attesa/…`) non è il nome di una sessione.
  if (hash.startsWith('#/')) return null
  let h: string
  try { h = decodeURIComponent(hash.slice(1)) } catch { h = hash.slice(1) }
  // Niente chiesto è null, mai '' (23/10): con '' la web locale apriva la pagina di una sessione senza nome e la striscia
  // aveva due pagine con la stessa chiave.
  return h.trim() || null
}

/**
 * I link delle richieste nei testi della chat (app-scheda-scelta-12-11, Franz 12/11 15:10: «in chat, al posto del link, una scheda
 * compatta»): `…/#/attesa/<id>`, col sito o senza, e i link con testo che ci portano escono dal testo; gli id restano in `ids`,
 * nell'ordine e una volta sola, e al loro posto la chat mostra la scheda. Un id nudo citato in una frase resta dov'è e ha la sua
 * scheda lo stesso. La riga rimasta vuota o con la sola etichetta sparisce, come per i link delle sessioni. Come SessionLinks.kt,
 * coi casi di core/src/test/resources/scelta-cases.json.
 */
const WAIT_ID = 'w[0-9a-f]{10}'
const WAIT_MD = new RegExp(String.raw`\[[^\]\n]+]\((?:https?://[^)\s]*)?#/attesa/(${WAIT_ID})\)`, 'g')
const WAIT_URL = new RegExp(String.raw`(?:https?://[^\s<>()\[\]«»]*)?#/attesa/(${WAIT_ID})(?![0-9A-Za-z])`, 'g')
const WAIT_BARE = new RegExp(String.raw`(?<![\p{L}\p{N}_/#-])(${WAIT_ID})(?![\p{L}\p{N}_-])`, 'gu')
const TAKEN = '\u0000'
const TAKEN_AND_DOTS = /\u0000[.,;:!?]*(?=\s|$)|\u0000/g
export function stripWaitLinks(text: string): { text: string; ids: string[] } {
  if (!/w[0-9a-f]{10}/.test(text)) return { text, ids: [] }
  const ids: string[] = []
  const note = (id: string) => { if (!ids.includes(id)) ids.push(id); return TAKEN }
  const t = text.replace(WAIT_MD, (_m: string, id: string) => note(id)).replace(WAIT_URL, (_m: string, id: string) => note(id))
  let out = text
  if (ids.length) {
    const before = text.split('\n')
    const lines: (string | null)[] = t.split('\n').map((l, i) => (l.includes(TAKEN) ? tidy(l.replace(TAKEN_AND_DOTS, ''), before[i].match(/^[ \t]*/)![0]) : l))
    for (let i = 1; i < lines.length; i++) { const p = lines[i - 1]; if (lines[i] == null && p != null && isLabel(p)) lines[i - 1] = null }
    out = lines.filter((l): l is string => l != null).join('\n').replace(/\n{3,}/g, '\n\n').replace(/^\n+|\n+$/g, '')
  }
  for (const m of out.matchAll(WAIT_BARE)) if (!ids.includes(m[1])) ids.push(m[1])
  return ids.length ? { text: out, ids } : { text, ids }
}
