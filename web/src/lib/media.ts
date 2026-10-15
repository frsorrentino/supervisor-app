import type { TranscriptFile } from './contract'

// Quali file della chat si scaricano da soli per l'anteprima, come MediaPreview sul telefono: le immagini fino a 10 MB,
// i video fino a 25 MB (Franz, 10/10 15:24: «Ok anteprime video»). Nel browser non c'è il contatore della rete del
// telefono: vale `saveData`, il risparmio dati chiesto dall'utente.
export const IMAGE_AUTO_MAX = 10_000_000
/** Il tetto dei file a pezzi dal PC (contratto 1.34): oltre, il PC li rifiuta comunque. */
export const VIDEO_AUTO_MAX = 26_214_400

/** La chiave di un file della chat nelle anteprime e nei file appena arrivati, come FileCache.key nell'app: con la misura
 *  della voce lo stesso file tiene la sua chiave a ogni riapertura, e lo stesso percorso mandato di nuovo con un'altra
 *  misura è un altro file; senza misura decide la voce (Franz, 12/10 15:34). */
export const fileKey = (session: string, f: TranscriptFile, entry: string) =>
  `${session}\n${f.path}\u0000${f.size != null ? `size=${f.size}` : `entry=${entry}`}`

export const isVideo = (mime?: string | null) => !!mime?.startsWith('video/')
export const isImage = (mime?: string | null) => !!mime?.startsWith('image/')

// Ogni allegato nasce con l'anteprima a tutta larghezza (Franz, 15/10 22:20): immagini e video come prima, i PDF con la
// prima pagina, testo e codice con le prime righe, gli altri con una scheda larga (icona, nome, misura) senza scaricare
// niente. PDF fino a 10 MB e testi fino a 1 MB arrivano da soli, ma non col risparmio dati: lì aspettano «Mostra anteprima».
export const PDF_AUTO_MAX = 10_000_000
export const TEXT_AUTO_MAX = 1_000_000
/** Le righe di un testo nell'anteprima. */
export const TEXT_LINES = 12
export type PreviewKind = 'image' | 'video' | 'pdf' | 'text' | 'other'

const TEXT_EXT = /\.(md|markdown|txt|log|csv|tsv|json|ya?ml|toml|ini|sh|py|ts|js|mjs|kt|kts|java|go|rs|rb|svelte|css|scss|sql|xml|env)$/i
export function previewKind(mime: string | null | undefined, path = ''): PreviewKind {
  const m = (mime ?? '').split(';')[0].trim().toLowerCase()
  if (m.startsWith('image/') && m !== 'image/svg+xml') return 'image'
  if (m.startsWith('video/')) return 'video'
  if (m === 'application/pdf' || (!m && /\.pdf$/i.test(path))) return 'pdf'
  if (m.startsWith('text/') || /\/(json|xml|javascript|x-sh|x-yaml|yaml|toml)$|\+(xml|json)$/.test(m)) return 'text'
  if ((!m || m === 'application/octet-stream') && TEXT_EXT.test(path)) return 'text'
  return 'other'
}

export function autoPreview(mime: string | null | undefined, size: number | null | undefined, unmetered = true, path = ''): boolean {
  switch (previewKind(mime, path)) {
    case 'image': return (size ?? 0) <= IMAGE_AUTO_MAX
    case 'video': return unmetered && size != null && size <= VIDEO_AUTO_MAX
    case 'pdf': return unmetered && (size ?? 0) <= PDF_AUTO_MAX
    case 'text': return unmetered && (size ?? 0) <= TEXT_AUTO_MAX
    default: return false
  }
}

/** Il tasto «Mostra anteprima»: dove un'anteprima c'è ma non è arrivata da sola. */
export const canPreview = (mime: string | null | undefined, path = '') => previewKind(mime, path) !== 'other'

/** Le prime righe di un testo per l'anteprima, senza a capo in fondo. */
export function firstLines(text: string, n = TEXT_LINES): { lines: string; more: boolean } {
  const all = text.replace(/\r\n?/g, '\n').split('\n')
  while (all.length && !all.at(-1)!.trim()) all.pop()
  return { lines: all.slice(0, n).join('\n'), more: all.length > n }
}
