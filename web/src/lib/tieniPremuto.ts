import type { Approval, Question, WaitItem } from './contract'

// Il tasto «tieni premuto» (app-tieni-premuto-28-11, Franz 28/11 16:22, «Tutti e tre»; mockup tieni-premuto-28-11.html, dal Hold to
// publish di designbookmark con le sue molle): le regole che non sono disegno, porta di TieniPremuto.kt, con gli stessi casi nei test.
// Tenendo premuto la pillola si riempie da sinistra, lineare nel tempo del suo posto; lasciata prima torna indietro, fa «no» con una
// scossa e non parte niente; arrivata in fondo diventa il cerchio verde con la spunta, poi parte l'azione.
// Correzione della master del 28/11 (dopo la nota di Franz delle 16:33): si tiene premuto solo nei tre posti scelti da Franz; ogni altro
// «Approva» e ogni «Va» restano a un tocco, col finale dello stesso componente (cerchio verde e spunta), poi la carta o la riga esce.

/** I posti del componente: i tre da tenere premuto e i due a un tocco col finale del cerchio verde. */
export type Place = 'chiudi' | 'riparti' | 'pubblica' | 'approva' | 'va'
/** Tenere premuto (con il pieno) o toccare (solo il finale). */
export type Mode = 'hold' | 'tap'

export const modeOf = (p: Place): Mode => (p === 'chiudi' || p === 'riparti' || p === 'pubblica' ? 'hold' : 'tap')

/** Quanto si tiene premuto, in ms: 1,2 s per chiudere, 1 s per ripartire, 1,5 s per pubblicare; 0 per i posti a un tocco. */
export const holdMs = (p: Place): number => ({ chiudi: 1200, riparti: 1000, pubblica: 1500, approva: 0, va: 0 })[p]

/** Il pieno dopo `elapsedMs` di pressione: lineare da 0 a 1, è una barra di avanzamento; un posto a un tocco è subito pieno. */
export const progress = (elapsedMs: number, hold: number): number => (hold <= 0 ? 1 : Math.min(1, Math.max(0, elapsedMs / hold)))

export type Release = 'early' | 'complete'
/** Il dito alzato dopo `elapsedMs`: prima del tempo è «lasciato prima» e non parte niente; dal tempo in poi è fatto. */
export const release = (elapsedMs: number, hold: number): Release => (elapsedMs >= hold ? 'complete' : 'early')

/** La spinta della scossa «no» lasciando prima, in px/s: 260·min(1, 2p + 0,3); niente con il moto ridotto. */
export const kick = (p: number, reduced = false): number => (reduced ? 0 : 260 * Math.min(1, Math.max(0, p) * 2 + 0.3))

// Le molle dell'originale (motion.js e tokens.css: massa 1, a = -k(x - t) - c·v), rigidità e smorzamento.
export const SQUASH = 0.96
export const PRESS: [number, number] = [520, 20]
export const RELEASE: [number, number] = [260, 14]
export const SHAKE: [number, number] = [700, 9]
export const MORPH: [number, number] = [320, 20]
/** La spunta parte dopo 160 ms e si disegna in 450 ms con (.65, 0, .35, 1); l'azione parte a spunta disegnata. */
export const CHECK_DELAY_MS = 160
export const CHECK_DRAW_MS = 450
export const FIRE_MS = CHECK_DELAY_MS + CHECK_DRAW_MS
/** Dopo un ok a un tocco la carta esce a spunta disegnata e vista intera un attimo, non subito. */
export const LEAVE_MS = FIRE_MS + 190
/** Quanto resta il cerchio verde quando nessuno toglie il tasto, prima di tornare pillola. */
export const DONE_MS = 1700
/** Con il moto ridotto lo smorzamento è critico (2√k): la molla arriva senza rimbalzo. */
export const damping = (k: number, c: number, reduced: boolean): number => (reduced ? 2 * Math.sqrt(k) : c)

// ---- Quale ok si tiene premuto: una carta che pubblica (il posto «pubblica») ----

// Le parole di chi fa uscire qualcosa: pubblicare, rilasciare, release, publish, deploy, git push, un push su origin, main o in
// produzione, mandare o portare in produzione, andare online. Gli indirizzi e i nomi di ramo (release/2.9) non contano, né le note di
// rilascio. La stessa espressione di TieniPremuto.kt.
const PUBLISH = /\b(?:pubblic(?!it)\w*|rilasc\w*|releas\w*|publish\w*|deploy\w*|git\s+push|push\w*\s+(?:-{1,2}\w+\s+)*(?:to|in|su|on|into|verso)\s+(?:origin|main|master|prod\w*|produzione)\b|go[\s-]live|(?:mett\w*|mand\w*|port\w*|andare|andr\w*|andiamo|va|vanno|vado)\s+(?:\S+\s+){0,3}(?:online|in\s+produzione)\b|(?:promot|mov|send|ship|roll)\w*\s+(?:\S+\s+){0,3}(?:to|into)\s+prod(?:uction)?\b)/i
const NOT_PUBLISH = /https?:\/\/\S+|\breleases?\/\S*|\brelease[\s-]notes?\b|\bnote\s+di\s+rilascio\b/gi

/** I testi nominano un'uscita (vedi le parole sopra). */
export const publishes = (texts: (string | null | undefined)[]): boolean => texts.some(t => t != null && PUBLISH.test(t.replace(NOT_PUBLISH, ' ')))

// Un'opzione che dice di no o rimanda non diventa mai «Tieni premuto per pubblicare».
const REFUSES = /^\s*(?:\d+\s*·\s*)?(?:no\b|non\b|niente\b|nessun\w*|aspett\w*|rimand\w*|annull\w*|lascia\s+perdere|più\s+tardi|dopo\b|domani\b|domattina\b|stasera\b|wait\b|later\b|not\b|don'?t\b|cancel\w*|skip\w*|stop\b|tomorrow\b)/i
/** L'etichetta di un tasto che rifiuta o rimanda. */
export const refuses = (label: string): boolean => REFUSES.test(label)

/**
 * Il tasto della carta che si tiene premuto per pubblicare, o null. È il tasto dell'ok (il pieno) o, senza, la scelta consigliata; mai
 * uno che chiede un testo, mai un «no» o un «rimanda», mai su una verifica. La carta pubblica se il contratto la segna per la produzione
 * (`card.deploy`, contratto 1.131) o se titolo, testo, descrizione, cosa, dove o l'etichetta del tasto nominano un'uscita.
 */
export function holdIndex(w: WaitItem): number | null {
  if (w.label === 'verify') return null
  const acts = w.actions ?? []
  let i = acts.findIndex(a => a.style === 'primary')
  if (i < 0) i = acts.findIndex(a => !!a.recommended)
  if (i < 0) return null
  const a = acts[i]
  if (a.text_field || refuses(a.label)) return null
  const out = w.card?.deploy === true || publishes([w.title, w.text, w.description, w.card?.what, w.card?.where, a.label])
  return out ? i : null
}

/** L'ok di `state.approvals`: si tiene premuto se esce in produzione (`deploy`) o se titolo, cosa o dove nominano un'uscita. */
export const holds = (a: Approval): boolean => a.deploy === true || publishes([a.title, a.what, a.where])

/**
 * L'opzione di una domanda che si tiene premuta per pubblicare (il suo numero), o null: la consigliata o, su un permesso senza consigliata,
 * la prima che non dice di no; mai un «no» o un «rimanda». La domanda pubblica se cosa si approva, il dialogo, il testo o l'opzione
 * nominano un'uscita (il «Pubblica il deploy 2.8.0: ./deploy.sh --env production» del mockup); le altre opzioni restano.
 */
export function holdOption(q: Question): number | null {
  const o = q.options.find(x => x.recommended) ?? q.options.find(x => q.kind === 'permission' && !refuses(x.label))
  if (!o || refuses(o.label)) return null
  return publishes([q.what, q.body, q.text, o.label, o.description]) ? o.n : null
}

/**
 * Il posto del tasto `i` della carta: «pubblica» per l'ok che pubblica (da tenere premuto); «va» per il Va di una prova (`prova` con
 * `result` ok, anche «Ora va»); «approva» per ogni altro ok (`approve`, anche la scelta di un'opzione); null per gli altri tasti.
 */
export function placeOf(w: WaitItem, i: number): Place | null {
  const a = w.actions?.[i]
  if (!a) return null
  if (holdIndex(w) === i) return 'pubblica'
  if (a.cmd.op === 'prova' && a.cmd.result === 'ok') return 'va'
  if (a.cmd.op === 'approve') return 'approva'
  return null
}
