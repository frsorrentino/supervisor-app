// La home per categorie (Franz 02/11 16:44, compito app-home-categorie-02-11; HomeCats.kt nell'app): quattro categorie con le
// loro sezioni, le stesse su telefono e PWA. Si salva nel `home_order` di sempre: le chiavi di sezione nell'ordine voluto, poi
// `shut:` con le categorie chiuse, poi lo slot `pin:`. L'ordine delle categorie è quello della loro prima sezione.
import { dragTarget, move, pinned, withPin, type HomeSection } from './homeOrder'

export type CatId = 'you' | 'work' | 'out' | 'tools'
/** Le chiavi di sezione: quelle di oggi più le tre nuove, che si disegnano quando esistono (app-da-guardare-02-11, app-rendiconti-2-02-11). */
export type HomeKey = Exclude<HomeSection, 'plugins' | 'closed'> | 'da_guardare' | 'rendiconti' | 'pagella'
export type Cat = { id: CatId; open: boolean; sections: HomeKey[] }

export const DEFAULT_CATS: Cat[] = [
  // Tocca a te in quattro sezioni (app-tocca-sezioni-06-11): Decidi, Verifica (le prove e i lavori da guardare: `prove` porta
  // anche Da guardare, che non è più una voce sua), Riprendi (la vecchia Segui), Suggerimenti. Prima raccolta sta dentro Decidi.
  { id: 'you', open: true, sections: ['decisioni', 'prove', 'seguiti', 'suggestions'] },
  { id: 'work', open: true, sections: ['sessions', 'lavori'] },
  { id: 'out', open: false, sections: ['esiti', 'rendiconti'] },
  { id: 'tools', open: false, sections: ['usage', 'system', 'pagella'] },
]
/**
 * Il marcatore dell'ordine nuovo (Franz 02/11 21:28, app-home-nomi-02-11): un ordine salvato senza marcatore riprende una volta
 * l'ordine delle categorie dei riquadri e quello di partenza dentro Tocca a te e Risorse; aperte, chiuse e tabellone restano.
 */
export const REV = 'rev:4'
/**
 * Un ordine `rev:2` (prima delle voci accorpate) o `rev:3` (prima delle quattro sezioni) tiene categorie, chiuse e cima; dentro
 * Tocca a te riprende una volta l'ordine di partenza.
 */
const REV2 = 'rev:2'
const REV3 = 'rev:3'
const SHUT = 'shut:'
const TOP = 'top:'
/** Il bordo dello spazio in cima nella lista piatta del trascinamento: sopra c'è la cima, sotto le categorie. */
export const EDGE = 'edge:top'

export const catOf = (key: string): CatId | null => DEFAULT_CATS.find(c => (c.sections as string[]).includes(key))?.id ?? null

/** Le chiavi sconosciute si ignorano (anche `closed` e `plugins` di ieri, ora dentro Sessioni e Sistema), le mancanti si accodano nella loro categoria. */
export function decode(saved: string[] | null | undefined): Cat[] {
  const list = saved ?? []
  const up = top(saved) as string[]
  const fresh = list.length > 0 && !list.includes(REV) && !list.includes(REV2) && !list.includes(REV3)
  const youOld = !list.includes(REV) && (list.includes(REV2) || list.includes(REV3))
  const all = [...new Set(list.filter(k => catOf(k) && !up.includes(k)))] as HomeKey[]
  const keys = all.filter(k => (!fresh || (catOf(k) !== 'you' && catOf(k) !== 'tools')) && !(youOld && catOf(k) === 'you'))
  const order = fresh ? DEFAULT_CATS.map(c => c.id) : [...new Set([...all.map(k => catOf(k)!), ...DEFAULT_CATS.map(c => c.id)])]
  const shut = list.find(k => k.startsWith(SHUT))
  const closed = shut == null ? null : shut.slice(SHUT.length).split(',')
  return order.map(id => {
    const d = DEFAULT_CATS.find(c => c.id === id)!
    const mine = keys.filter(k => catOf(k) === id)
    return { id, open: closed ? !closed.includes(id) : d.open, sections: [...mine, ...d.sections.filter(k => !mine.includes(k) && !up.includes(k))] }
  })
}

/** L'arg dell'op `home_order`: null quando è tutto predefinito. */
export function encode(cats: Cat[], pin: HomeSection | null, up: HomeKey[] = []): string | null {
  if (!pin && !up.length && JSON.stringify(cats) === JSON.stringify(DEFAULT_CATS)) return null
  const shut = SHUT + cats.filter(c => !c.open).map(c => c.id).join(',')
  return JSON.stringify(withPin([...up.map(k => TOP + k), ...cats.flatMap(c => c.sections), shut, REV] as HomeSection[], pin))
}

export const moveCat = (cats: Cat[], from: number, to: number): Cat[] => move(cats, from, to)
/** Dentro la categoria e basta: fuori dai limiti resta com'è. */
export const moveSection = (cats: Cat[], id: CatId, from: number, to: number): Cat[] =>
  cats.map(c => (c.id === id ? (move(c.sections, from, to) === c.sections ? c : { ...c, sections: move(c.sections, from, to) }) : c))
export const toggle = (cats: Cat[], id: CatId): Cat[] => cats.map(c => (c.id === id ? { ...c, open: !c.open } : c))
/**
 * Dopo un trascinamento sulla home (ordine piatto): ogni categoria riprende le sue sezioni in quell'ordine, aperte e chiuse come
 * prima. Le chiavi che la lista non ha (non disegnate, come Pagella finché non esiste) tengono il loro posto.
 */
export const reorder = (cats: Cat[], flat: string[]): Cat[] =>
  cats.map(c => {
    const moved = flat.filter(k => (c.sections as string[]).includes(k)) as HomeKey[]
    let i = 0
    return { ...c, sections: c.sections.map(k => (moved.includes(k) ? moved[i++] : k)) }
  })

/** La categoria di una voce della home: Chiuse sta dentro Sessioni, Plugin e skill dentro Sistema. */
export const catFor = (k: string): CatId | null => (k === 'closed' ? 'work' : k === 'plugins' ? 'tools' : catOf(k))

/** Il trascinamento sul posto della home, dentro la categoria della voce presa: le vicine di un'altra categoria non contano. */
export function dragCats(order: HomeSection[], held: HomeSection, y: number, mids: Partial<Record<HomeSection, number>>, fromBoard = true): HomeSection[] {
  // Una voce del tabellone scavalca le altre voci del tabellone e il bordo, di qualunque categoria siano; una sezione presa in una
  // categoria resta lì: sul tabellone ci va solo rilasciandola sul riquadro (Franz 02/11 20:45, app-tabellone-quattro-02-11).
  const e = (order as string[]).indexOf(EDGE)
  const up = e < 0 || !fromBoard ? [] : (order as string[]).slice(0, e + 1)
  const mine = Object.fromEntries(Object.entries(mids).filter(([k]) => up.includes(k) || catFor(k) === catFor(held))) as Partial<Record<HomeSection, number>>
  return dragTarget(order, held, y, mine)
}

// Lo spazio in cima (Franz 02/11 19:18, app-home-in-cima-02-11): sezioni di qualsiasi categoria, nell'ordine scelto. Una voce in
// cima esce dalla sua categoria (Franz 19:26) e si salva come `top:<chiave>` al posto della sua chiave: il conto delle chiavi del
// relay non cresce. Un'app che non conosce `top:` la ignora e rimette la sezione nella sua categoria. HomeCats.kt nell'app.

/**
 * Le voci sul tabellone (Franz 02/11 20:45, app-tabellone-quattro-02-11): al massimo BOARD_MAX, nell'ordine salvato; doppie e
 * sconosciute si ignorano. Un ordine di prima con lo slot (`pin:`) e senza voci in cima porta la voce dello slot sul tabellone.
 */
export const BOARD_MAX = 4
export function top(saved: string[] | null | undefined): HomeKey[] {
  const list = [...new Set((saved ?? []).filter(k => k.startsWith(TOP)).map(k => k.slice(TOP.length)).filter(k => catOf(k)))] as HomeKey[]
  const pin = pinned(saved)
  return (list.length ? list : pin && catOf(pin) ? [pin as HomeKey] : []).slice(0, BOARD_MAX)
}

/** L'ordine mostrato e trascinato: la cima, il bordo, le sezioni delle categorie. */
export const flat = (up: HomeKey[], cats: Cat[]): string[] => [...up, EDGE, ...cats.flatMap(c => c.sections)]

/**
 * Dopo il rilascio: quello che sta sopra il bordo va in cima; il resto torna nella sua categoria nell'ordine del puntatore. Le chiavi
 * che la lista non ha (non disegnate) tengono il loro posto; chi scende dalla cima entra nella sua categoria dove l'ha lasciato.
 */
export function settle(cats: Cat[], list: string[]): { top: HomeKey[]; cats: Cat[] } {
  const e = list.indexOf(EDGE)
  if (e < 0) return { top: [], cats: reorder(cats, list) }
  const up = [...new Set(list.slice(0, e).filter(k => catOf(k)))] as HomeKey[]
  const below = list.slice(e + 1)
  const back = cats.map(c => {
    const returned = below.filter(k => catOf(k) === c.id && !(c.sections as string[]).includes(k)) as HomeKey[]
    return { ...c, sections: [...c.sections.filter(k => !up.includes(k)), ...returned] }
  })
  return { top: up, cats: reorder(back, below) }
}

/** Riordina e rilascio sul tabellone: la voce va in fondo al tabellone; pieno, niente cambia. */
export const toTop = (cats: Cat[], up: HomeKey[], key: HomeKey): { top: HomeKey[]; cats: Cat[] } =>
  up.includes(key) || !catOf(key) || up.length >= BOARD_MAX ? { top: up, cats } : { top: [...up, key], cats: cats.map(c => ({ ...c, sections: c.sections.filter(k => k !== key) })) }

/**
 * «Togli dalla cima» (Riordina, e il rilascio sotto la cima nella home): la voce torna al suo posto nella sua categoria, cioè prima
 * della prima sezione che nell'ordine iniziale la segue (app-cima-rilascio-reset-03-11); se non ce n'è, in fondo.
 */
export function fromTop(cats: Cat[], up: HomeKey[], key: HomeKey): { top: HomeKey[]; cats: Cat[] } {
  if (!up.includes(key)) return { top: up, cats }
  const def = (DEFAULT_CATS.find(c => c.id === catOf(key))?.sections ?? []) as string[]
  const after = def.slice(def.indexOf(key) + 1)
  return {
    top: up.filter(k => k !== key),
    cats: cats.map(c => {
      if (c.id !== catOf(key)) return c
      const s = c.sections.filter(k => k !== key)
      const i = s.findIndex(k => after.includes(k))
      return { ...c, sections: i < 0 ? [...s, key] : [...s.slice(0, i), key, ...s.slice(i)] }
    }),
  }
}

// Le categorie si trascinano anche nella home (Franz 02/11 19:00, app-categorie-trascina-02-11): nella lista del trascinamento
// una categoria è `cat:<id>`, così non si confonde con le chiavi di sezione.
export const catKeys = (cats: Cat[]): string[] => cats.map(c => `cat:${c.id}`)
/** Le categorie nell'ordine trascinato, con le loro sezioni e il loro interruttore; quelle che la lista non ha restano in fondo. */
export function catOrder(cats: Cat[], keys: string[]): Cat[] {
  const picked = keys.map(k => cats.find(c => `cat:${c.id}` === k)).filter((c): c is Cat => !!c)
  return [...picked, ...cats.filter(c => !picked.includes(c))]
}
