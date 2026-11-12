// La scheda di scelta (app-scheda-scelta-12-11, contratto 1.137; descrizione approvata da Franz il 12/11 alle 15:10): le opzioni
// con la loro lettera, la riga di pro e contro, le immagini e il tasto che le sceglie; «Nessuna, rifare» col motivo; com'è finita
// (dall'archivio); i giri precedenti col motivo per cui sono stati scartati. Le stesse regole di Scelta.kt, coi casi di
// core/src/test/resources/scelta-cases.json.
import type { WaitFile, WaitItem, WaitOption } from './contract'
import { lookOf } from './tiAspetta'

/** La lettera di un posto: 1 → A. */
const letterAt = (n: number): string => (n >= 1 && n <= 26 ? String.fromCharCode(64 + n) : String(n))
// Una maiuscola da sola, o seguita da un separatore e dal nome: «A», «A frecce», «A: frecce», «B) pannello», «C - a scomparsa».
const OWN = /^([A-Z])(?:[\s.:)·–—-]+([\s\S]*))?$/

export type Lettered = { letter: string; name: string }
/**
 * La lettera e il nome di ogni opzione, in ordine. Se ogni etichetta porta la sua lettera e sono tutte diverse valgono quelle
 * («B pannello» resta B anche al primo posto), e il nome è il resto; altrimenti la lettera viene dal posto e il nome è l'etichetta.
 */
export function lettersOf(labels: string[]): Lettered[] {
  const own = labels.map(l => { const m = OWN.exec(l.trim()); return m ? { letter: m[1], name: (m[2] ?? '').trim() } : null })
  const all = own.length > 0 && own.every(x => x != null) && new Set(own.map(x => x!.letter)).size === own.length
  return labels.map((l, i) => (all ? own[i]! : { letter: letterAt(i + 1), name: l.trim() }))
}

/** Immagini e documenti di un elenco di file: decide `kind` del relay (un svg è un documento); senza, il tipo. */
export function splitFiles(files: WaitFile[] | null | undefined): { images: WaitFile[]; docs: WaitFile[] } {
  const image = (f: WaitFile) => (f.kind ? f.kind === 'image' : !!f.mime?.startsWith('image/') && f.mime !== 'image/svg+xml')
  const all = files ?? []
  return { images: all.filter(image), docs: all.filter(f => !image(f)) }
}

/** La corsia dei file dichiarati in una richiesta (`session` null): il relay tiene una lettura `file` alla volta per sessione. */
export const WAIT_LANE = '\u0001attesa'
/**
 * La chiave di un file di una richiesta nelle anteprime: con la sessione è la stessa della sua scheda in chat (`fileKey`), così
 * l'immagine già arrivata non si richiede; un file dichiarato sta nella corsia delle richieste.
 */
export const waitFileKey = (f: WaitFile): string => `${f.session ?? WAIT_LANE}\n${f.path}\u0000${f.size != null ? `size=${f.size}` : 'entry=attesa'}`

export type Opt = Lettered & { n: number; label: string; value: string; line: string | null; recommended: boolean; images: WaitFile[]; docs: WaitFile[] }
const optsOf = (options: WaitOption[] | null | undefined): Opt[] => {
  const list = options ?? []
  const names = lettersOf(list.map(o => o.label))
  return list.map((o, i) => ({ ...names[i], n: o.n, label: o.label, value: o.value, line: o.description?.trim() || null, recommended: !!o.recommended, ...splitFiles(o.files) }))
}

/** Una scheda di scelta: almeno due opzioni di un ok con scelta, o di qualunque richiesta le cui opzioni portano file. */
export const isChoice = (w: WaitItem): boolean => (w.options?.length ?? 0) >= 2 && (w.kind === 'choice' || w.options!.some(o => !!o.files?.length))

/**
 * `action`: il posto in `actions` del tasto che sceglie l'opzione (null dall'archivio, dove non ci sono tasti); `redo`: quello di
 * «Nessuna, rifare»; `rest`: gli altri tasti, in ordine; `extra`: i file della scheda che nessuna opzione ha.
 */
export type Choice = { options: (Opt & { action: number | null })[]; extra: WaitFile[]; redo: number | null; rest: number[] }
export function choiceOf(w: WaitItem): Choice {
  const acts = w.actions ?? []
  const taken = new Set<number>()
  const free = (pick: (a: (typeof acts)[number]) => boolean) => acts.findIndex((a, j) => !taken.has(j) && a.cmd.op !== 'approval_redo' && pick(a))
  const options = optsOf(w.options).map(o => {
    // Il tasto dell'opzione: quello che manda il suo valore; se no quello con la sua etichetta; se no il primo tasto di scelta libero.
    let i = free(a => a.cmd.choice != null && a.cmd.choice === o.value)
    if (i < 0) i = free(a => lookOf(a) === 'option' && a.label === o.label)
    if (i < 0) i = free(a => lookOf(a) === 'option')
    if (i >= 0) taken.add(i)
    return { ...o, action: i >= 0 ? i : null }
  })
  const redo = acts.findIndex(a => a.cmd.op === 'approval_redo')
  const mine = options.flatMap(o => [...o.images, ...o.docs])
  const same = (a: WaitFile, b: WaitFile) => a.path === b.path && (a.session ?? null) === (b.session ?? null)
  return {
    options, redo: redo >= 0 ? redo : null,
    rest: acts.map((_, i) => i).filter(i => !taken.has(i) && i !== redo),
    extra: (w.files ?? []).filter(f => !mine.some(x => same(x, f))),
  }
}

export type OutcomeState = 'open' | 'chosen' | 'approved' | 'redo' | 'closed'
const STATES: readonly string[] = ['open', 'chosen', 'approved', 'redo', 'closed']
/** Com'è finita una scheda: lo stato, l'opzione scelta con la sua lettera (o le parole, se non è un'opzione), il motivo, chi e quando. */
export type Outcome = { state: OutcomeState; letter: string | null; name: string | null; reason: string | null; by: string | null; at: number | null }
export function outcomeOf(w: Pick<WaitItem, 'state' | 'answered' | 'options'>): Outcome {
  const a = w.answered ?? null
  const state = (w.state == null ? 'open' : STATES.includes(w.state) ? w.state : a ? 'closed' : 'open') as OutcomeState
  const none: Outcome = { state, letter: null, name: null, reason: a?.reason?.trim() || null, by: a?.by ?? null, at: a?.at ?? null }
  if (state !== 'chosen' || !a?.choice) return none
  const list = w.options ?? []
  const i = [list.findIndex(o => o.value === a.choice), list.findIndex(o => o.label === a.choice)].find(x => x >= 0)
  if (i == null) return { ...none, name: a.choice }
  return { ...none, ...lettersOf(list.map(o => o.label))[i] }
}

export type RoundState = 'redo' | 'closed' | 'replaced'
/** Un giro precedente: `n` il suo numero dal primo; `chosen` non c'è mai per un giro scartato, resta per un relay che lo dica. */
export type Round = { id: string; n: number; at: number; text: string; description: string | null; options: Opt[]; extra: WaitFile[]; state: RoundState; reason: string | null; by: string | null; answeredAt: number | null }
/** I giri precedenti della scheda, dal più recente. */
export function roundsOf(w: Pick<WaitItem, 'rounds'>): Round[] {
  return (w.rounds ?? []).map((r, i): Round => {
    const options = optsOf(r.options), mine = options.flatMap(o => [...o.images, ...o.docs])
    return {
      id: r.id, n: i + 1, at: r.at, text: r.text, description: r.description?.trim() || null, options,
      extra: (r.files ?? []).filter(f => !mine.some(x => x.path === f.path && (x.session ?? null) === (f.session ?? null))),
      state: (r.state === 'redo' || r.state === 'replaced' ? r.state : 'closed') as RoundState,
      reason: r.reason?.trim() || null, by: r.by ?? null, answeredAt: r.answered_at ?? null,
    }
  }).reverse()
}

/** Il motivo di «Nessuna, rifare»: obbligatorio, fino a 300 caratteri (contratto 1.137, `approval_redo`). */
export const REDO_MAX = 300
export const redoReady = (text: string): boolean => { const n = text.trim().length; return n > 0 && n <= REDO_MAX }

/** Le miniature della scheda compatta: la prima immagine di ogni opzione con la sua lettera; senza opzioni, le immagini della scheda. */
export type Thumb = { letter: string | null; file: WaitFile }
export function thumbsOf(w: WaitItem, max = 4): Thumb[] {
  if (isChoice(w)) return optsOf(w.options).filter(o => o.images.length).map(o => ({ letter: o.letter, file: o.images[0] })).slice(0, max)
  return splitFiles(w.files).images.slice(0, max).map(file => ({ letter: null, file }))
}

/**
 * La forma della cornice delle immagini di una scheda: quella della prima immagine già arrivata, fra 9:16 e 16:9, così due
 * schermate di telefono stanno affiancate alte e due tavole larghe non lasciano fasce vuote; finché non ne arriva una, 4:3.
 */
export const RATIO_DEFAULT = 4 / 3
export function frameRatio(known: (number | null | undefined)[]): number {
  const r = known.find(x => x != null && Number.isFinite(x) && x > 0)
  return r == null ? RATIO_DEFAULT : Math.min(16 / 9, Math.max(9 / 16, r))
}

/**
 * Il gesto di lato nel visore, da un'immagine intera: +1 la prossima (il dito va a sinistra), -1 la precedente, 0 niente. Vale un
 * tratto di almeno 56 px, o uno scatto svelto di almeno 20 px; un gesto più verticale che orizzontale non conta.
 */
export function swipeTo(dx: number, dy: number, ms: number): -1 | 0 | 1 {
  const ax = Math.abs(dx)
  if (ax < Math.abs(dy) * 1.2) return 0
  const fast = ms > 0 && ax >= 20 && ax / ms > 0.35
  if (ax < 56 && !fast) return 0
  return dx < 0 ? 1 : -1
}

/** La misura di un file a parole, come nelle schede dei file della chat: «48 KB», «1,2 MB». */
export const sizeText = (b: number): string => (b >= 1_000_000 ? `${(b / 1_000_000).toFixed(1).replace('.', ',')} MB` : b >= 1000 ? `${Math.floor(b / 1000)} KB` : `${b} B`)
