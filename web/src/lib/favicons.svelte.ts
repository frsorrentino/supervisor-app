// app-favicon-siti-18-11 (contratto 1.153, scelta B di Franz il 18/11): la favicon del sito come avatar della sessione, con l'anello
// dello stato. Lo stato porta l'impronta; l'immagine arriva una volta con l'op `favicon` e si tiene, anche fra un'apertura e l'altra.
// Senza impronta, senza l'op o con dati che non tornano resta l'icona di oggi. Porta di Favicons.kt.
import { SvelteMap } from 'svelte/reactivity'
import type { State } from './contract'

export const validHash = (h: unknown): h is string => typeof h === 'string' && /^[0-9a-f]{12}$/.test(h)

/** Le impronte da avere, una volta ciascuna, solo se il relay sa rispondere all'op. */
export function wanted(st: State | null | undefined): string[] {
  if (!st?.ops?.includes('favicon')) return []
  return [...new Set(st.sessions.map(s => s.favicon).filter(validHash))]
}

/** L'immagine di una risposta come data URL, solo se è proprio quella chiesta: stessa impronta, PNG vero, entro 64 KB. */
export function faviconUrl(hash: string, ok: boolean, text: string): string | null {
  if (!ok || !validHash(hash)) return null
  let img: { hash?: unknown; mime?: unknown; data?: unknown }
  try { img = JSON.parse(text) } catch { return null }
  if (img?.hash !== hash || img.mime !== 'image/png' || typeof img.data !== 'string' || img.data.length > 90_000) return null
  let head: string
  try { head = atob(img.data.slice(0, 12)) } catch { return null }
  if (!head.startsWith('\x89PNG\r\n\x1a\n')) return null
  return `data:image/png;base64,${img.data}`
}

/** Le favicon arrivate, per impronta: le leggono badge e righe. */
export const favicons = new SvelteMap<string, string>()

const KEY = (h: string) => `favicon:${h}`
export function remembered(hash: string): string | null {
  try { return localStorage.getItem(KEY(hash)) } catch { return null }
}
export function keep(hash: string, url: string) {
  favicons.set(hash, url)
  try { localStorage.setItem(KEY(hash), url) } catch { /* privata o piena: resta in memoria */ }
}

/** Il colore dell'anello per il glifo della riga, come StateGlyph. */
export const ringOf = (kind: string) => ({ waiting: 'var(--wait)', busy: 'var(--busy)', done: 'var(--good)', red: 'var(--red)', still: 'var(--tx3)' } as Record<string, string>)[kind] ?? 'var(--tx3)'
/** Il colore dell'anello per lo stato della sessione, nei badge. */
export const ringOfState = (state: string) => state === 'waiting' || state === 'awaiting' ? 'var(--wait)' : state === 'busy' ? 'var(--busy)' : 'var(--tx3)'
