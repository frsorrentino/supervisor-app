// Le anteprime che si muovono (app-anteprime-webp-27-11, Franz 27/11 20:49: «ovunque ci siano delle anteprime che devono mostrare
// un'animazione, queste siano animate in loop o a tocco»), porta di AnteprimeAnimate.kt coi casi di
// core/src/test/resources/anteprime-animate-cases.json. Un WebP o una GIF animati girano in loop finché sono a schermo e si fermano
// fuori; con le animazioni ridotte resta il primo fotogramma col ▶, e il tocco li apre a schermo intero. Accanto all'HTML di una bozza
// la master e le sessioni mandano il suo WebP animato con lo stesso nome: si vede il WebP, il tocco apre la bozza dal vivo.

const EXT = /\.(webp|gif)$/i
const DRAFT = /\.html?$/i

/** Un'immagine che può muoversi: WebP o GIF per tipo, o per estensione quando il tipo manca o è generico. */
export function capable(mime: string | null | undefined, path: string): boolean {
  const m = (mime ?? '').split(';')[0].trim().toLowerCase()
  if (m === 'image/webp' || m === 'image/gif') return true
  return (!m || m === 'application/octet-stream') && EXT.test(path)
}

const has = (b: Uint8Array, at: number, s: string) => b.length >= at + s.length && [...s].every((c, i) => b[at + i] === c.charCodeAt(0))

// RIFF....WEBP, poi il pezzo VP8X: il byte delle opzioni ha l'animazione nel secondo bit (0x02).
const webpAnimated = (b: Uint8Array) => b.length > 20 && has(b, 0, 'RIFF') && has(b, 8, 'WEBP') && has(b, 12, 'VP8X') && (b[20] & 0x02) !== 0

/** I fotogrammi di una GIF, fermandosi al secondo: blocchi di estensione saltati, descrittori d'immagine contati. */
function gifFrames(b: Uint8Array): number {
  if (b.length < 13 || !(has(b, 0, 'GIF87a') || has(b, 0, 'GIF89a'))) return 0
  const screen = b[10]
  let i = 13 + (screen & 0x80 ? 3 * (1 << ((screen & 0x07) + 1)) : 0)
  let frames = 0
  // I sotto-blocchi di dati: un byte di lunghezza e i suoi byte, fino a quello lungo zero; false se il file finisce prima.
  const skipBlocks = (): boolean => {
    while (i < b.length) {
      const n = b[i]
      i += 1
      if (n === 0) return true
      i += n
    }
    return false
  }
  while (i < b.length && frames < 2) {
    const c = b[i]
    if (c === 0x21) { i += 2; if (!skipBlocks()) break }
    else if (c === 0x2c) {
      if (i + 10 > b.length) break
      const local = b[i + 9]
      i += 10 + (local & 0x80 ? 3 * (1 << ((local & 0x07) + 1)) : 0)
      frames++
      // Il byte della misura del codice LZW, poi i dati del fotogramma.
      i += 1
      if (!skipBlocks()) break
    } else break
  }
  return frames
}

/** Si muove davvero, dai byte del file: un WebP col bit dell'animazione (VP8X) o una GIF con almeno due fotogrammi. */
export const moves = (b: Uint8Array): boolean => webpAnimated(b) || gifFrames(b) >= 2

/** Gira solo a schermo e senza le animazioni ridotte; altrimenti resta il primo fotogramma. */
export const plays = (visible: boolean, reduced: boolean): boolean => visible && !reduced

const stemOf = (path: string): string | null => {
  const n = path.split('/').pop()!.split('\\').pop()!
  const dot = n.lastIndexOf('.')
  const s = dot < 0 ? n : n.slice(0, dot)
  return s || null
}
const same = (a: string | null, b: string | null) => a != null && b != null && a.toLowerCase() === b.toLowerCase()

/**
 * La bozza HTML che va con un'immagine che può muoversi: lo stesso nome senza l'ultima estensione (maiuscole a parte), .html o .htm,
 * fra i file dello stesso elenco; la prima se sono più d'una. Null se non c'è, o se l'immagine è ferma per tipo.
 */
export function draftOf(image: string, mime: string | null | undefined, files: string[]): string | null {
  if (!capable(mime, image)) return null
  const stem = stemOf(image)
  return files.find(p => p !== image && DRAFT.test(p) && same(stemOf(p), stem)) ?? null
}

/** La bozza di un'immagine fra i file dello stesso elenco (una carta, un'opzione, un messaggio), come file: quella di draftOf. */
export function draftIn<T extends { path: string; mime?: string | null }>(f: T, all: T[]): T | null {
  const p = draftOf(f.path, f.mime, all.map(x => x.path))
  return p == null ? null : all.find(x => x.path === p) ?? null
}

/** L'immagine che può muoversi accanto a una bozza HTML, fra i file dello stesso elenco: la sua anteprima leggera. */
export function previewOf(draft: string, files: { path: string; mime?: string | null }[]): string | null {
  if (!DRAFT.test(draft)) return null
  const stem = stemOf(draft)
  return files.find(f => f.path !== draft && capable(f.mime, f.path) && same(stemOf(f.path), stem))?.path ?? null
}
