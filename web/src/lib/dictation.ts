// La dettatura nel campo di scrittura (Franz, 16/10 23:09): il riconoscimento vocale del browser nella lingua dell'app,
// il testo intermedio nel campo mentre Franz parla, aggiunto a quello già scritto. Non invia mai: Franz corregge e preme
// invio. Si ferma con un secondo tocco o da sola quando finisce il parlato. Dove l'API manca (Firefox) il tasto non c'è.
import type { Lang } from './lang'

export interface RecResult { readonly isFinal: boolean; readonly 0: { readonly transcript: string } }
export interface RecEvent { readonly results: ArrayLike<RecResult> }
export interface Rec {
  lang: string; interimResults: boolean; continuous: boolean
  onresult: ((e: RecEvent) => void) | null
  onerror: ((e: { error: string }) => void) | null
  onend: (() => void) | null
  start(): void; stop(): void; abort(): void
}
export type RecCtor = new () => Rec

/** SpeechRecognition o webkitSpeechRecognition (Chrome, Safari); null dove manca. */
export function speechCtor(w: object | undefined = typeof window === 'undefined' ? undefined : window): RecCtor | null {
  const g = w as { SpeechRecognition?: RecCtor; webkitSpeechRecognition?: RecCtor } | undefined
  return g?.SpeechRecognition ?? g?.webkitSpeechRecognition ?? null
}

export const dictLang = (l: Lang) => (l === 'it' ? 'it-IT' : 'en-US')

/** Il dettato dopo quello già scritto, separato da uno spazio se serve. */
export function joinDictation(base: string, spoken: string): string {
  const s = spoken.trim()
  if (!s) return base
  return !base || /\s$/.test(base) ? base + s : `${base} ${s}`
}

/** Gli errori che Franz vede: permesso negato, nessun parlato, il resto; `aborted` è il nostro stop. */
export type DictError = 'denied' | 'no_speech' | 'failed'
export function dictError(code: string): DictError | null {
  if (code === 'aborted') return null
  if (code === 'not-allowed' || code === 'service-not-allowed') return 'denied'
  if (code === 'no-speech') return 'no_speech'
  return 'failed'
}

export interface Dictation { stop(): void; cancel(): void }

/**
 * Avvia la dettatura: `text` riceve ogni volta il campo intero (quello scritto prima più il dettato, intermedio compreso),
 * `end` arriva una volta sola, con l'errore se c'è stato. `stop` chiude e tiene l'ultimo testo; `cancel` stacca tutto
 * (all'invio, perché un risultato in ritardo non riempia il campo appena svuotato).
 */
export function dictate(Ctor: RecCtor, lang: string, base: string, on: { text: (s: string) => void; end: (err: DictError | null) => void }): Dictation {
  const rec = new Ctor()
  rec.lang = lang
  rec.interimResults = true
  rec.continuous = false
  let err: DictError | null = null
  let done = false
  const finish = () => { if (done) return; done = true; rec.onresult = rec.onerror = rec.onend = null; on.end(err) }
  rec.onresult = (e) => {
    let spoken = ''
    for (let i = 0; i < e.results.length; i++) spoken += e.results[i][0].transcript
    on.text(joinDictation(base, spoken))
  }
  rec.onerror = (e) => { err = dictError(e.error) ?? err }
  rec.onend = finish
  try { rec.start() } catch { err = 'failed'; finish() }
  return {
    stop: () => { if (!done) rec.stop() },
    cancel: () => { if (done) return; rec.onresult = null; finish(); rec.abort() },
  }
}
