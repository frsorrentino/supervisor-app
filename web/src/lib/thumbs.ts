// Le anteprime dei documenti in Verifica (app-anteprime-verifica-11-11, contratto 1.136), porta di Thumbs.kt: le consegne senza un'immagine
// loro chiedono al relay la loro anteprima con l'op `thumb`, una volta per versione e poi in cache (qui, per tutta la pagina); con l'etag
// (LightTransport) la stessa lettura torna «unchanged». Un relay senza l'op tiene l'icona di oggi.
import type { CmdResult, ResultItem } from './contract'
import { kindOf, thumb } from './guardare'
import { newCmd } from './transport'

export type ThumbPreview = { id: string; path: string; at: number; kind: 'image' | 'text' | 'none' | string; mime?: string; data?: string; width?: number; height?: number; text?: string; why?: string }
export type Shown = { kind: 'image'; src: string } | { kind: 'lines'; text: string } | null

export const MAX_LINES = 20
export const MAX_CHARS = 1200

/** Ha senso chiederla: non un'immagine o un mockup con l'immagine (si vedono già), non un video. */
export const wantsPreview = (i: ResultItem): boolean => !thumb(i) && kindOf(i.kind) !== 'video'

/** La chiave della cache: una consegna rifatta (versione nuova, file sostituito) ha un'anteprima nuova. */
export const previewKey = (i: ResultItem): string => `${i.id}:${i.version ?? 1}:${i.at}:${!!i.replaced}`

/** La risposta dell'op: l'anteprima, o null per un rifiuto (id sconosciuto) o una risposta che non si legge. */
export function readThumb(ok: boolean, text: string): ThumbPreview | null {
  if (!ok) return null
  try { const o = JSON.parse(text); return o && typeof o === 'object' && typeof o.kind === 'string' ? o as ThumbPreview : null } catch { return null }
}

/** Cosa mostrare nella casella: l'immagine, le prime righe, o niente (resta l'icona). */
export function shownOf(p: ThumbPreview | null): Shown {
  if (p?.kind === 'image' && p.data) return { kind: 'image', src: `data:${p.mime ?? 'image/jpeg'};base64,${p.data}` }
  if (p?.kind === 'text' && p.text?.trim()) return { kind: 'lines', text: p.text.split('\n').slice(0, MAX_LINES).join('\n').slice(0, MAX_CHARS) }
  return null
}

const cache = new Map<string, ThumbPreview | null>()
const flying = new Map<string, Promise<ThumbPreview | null>>()

/** Una lettura per versione, anche se più caselle la chiedono insieme; null senza risposta (si potrà richiedere più tardi). */
export function loadPreview(i: ResultItem, run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>): Promise<ThumbPreview | null> {
  const k = previewKey(i)
  if (cache.has(k)) return Promise.resolve(cache.get(k) ?? null)
  const f = flying.get(k)
  if (f) return f
  const p = run(newCmd('thumb', null, i.id)).then(r => {
    if (!r) return null
    const x = readThumb(r.ok, r.text)
    cache.set(k, x)
    return x
  }).finally(() => flying.delete(k))
  flying.set(k, p)
  return p
}

/** Solo per i test. */
export const clearPreviews = () => { cache.clear(); flying.clear() }
