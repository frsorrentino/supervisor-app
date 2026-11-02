// La home per categorie (Franz 02/11 16:44, compito app-home-categorie-02-11; HomeCats.kt nell'app): quattro categorie con le
// loro sezioni, le stesse su telefono e PWA. Si salva nel `home_order` di sempre: le chiavi di sezione nell'ordine voluto, poi
// `shut:` con le categorie chiuse, poi lo slot `pin:`. L'ordine delle categorie è quello della loro prima sezione.
import { move, withPin, type HomeSection } from './homeOrder'

export type CatId = 'you' | 'work' | 'out' | 'tools'
/** Le chiavi di sezione: quelle di oggi più le tre nuove, che si disegnano quando esistono (app-da-guardare-02-11, app-rendiconti-2-02-11). */
export type HomeKey = Exclude<HomeSection, 'plugins' | 'closed'> | 'da_guardare' | 'rendiconti' | 'pagella'
export type Cat = { id: CatId; open: boolean; sections: HomeKey[] }

export const DEFAULT_CATS: Cat[] = [
  { id: 'you', open: true, sections: ['prima_raccolta', 'decisioni', 'prove', 'suggestions'] },
  { id: 'work', open: true, sections: ['sessions', 'seguiti', 'lavori'] },
  { id: 'out', open: false, sections: ['esiti', 'da_guardare', 'rendiconti'] },
  { id: 'tools', open: false, sections: ['usage', 'pagella', 'system'] },
]
const SHUT = 'shut:'

export const catOf = (key: string): CatId | null => DEFAULT_CATS.find(c => (c.sections as string[]).includes(key))?.id ?? null

/** Le chiavi sconosciute si ignorano (anche `closed` e `plugins` di ieri, ora dentro Sessioni e Sistema), le mancanti si accodano nella loro categoria. */
export function decode(saved: string[] | null | undefined): Cat[] {
  const list = saved ?? []
  const keys = [...new Set(list.filter(k => catOf(k)))] as HomeKey[]
  const order = [...new Set([...keys.map(k => catOf(k)!), ...DEFAULT_CATS.map(c => c.id)])]
  const shut = list.find(k => k.startsWith(SHUT))
  const closed = shut == null ? null : shut.slice(SHUT.length).split(',')
  return order.map(id => {
    const d = DEFAULT_CATS.find(c => c.id === id)!
    const mine = keys.filter(k => catOf(k) === id)
    return { id, open: closed ? !closed.includes(id) : d.open, sections: [...mine, ...d.sections.filter(k => !mine.includes(k))] }
  })
}

/** L'arg dell'op `home_order`: null quando è tutto predefinito. */
export function encode(cats: Cat[], pin: HomeSection | null): string | null {
  if (!pin && JSON.stringify(cats) === JSON.stringify(DEFAULT_CATS)) return null
  const shut = SHUT + cats.filter(c => !c.open).map(c => c.id).join(',')
  return JSON.stringify(withPin([...cats.flatMap(c => c.sections), shut] as HomeSection[], pin))
}

export const moveCat = (cats: Cat[], from: number, to: number): Cat[] => move(cats, from, to)
/** Dentro la categoria e basta: fuori dai limiti resta com'è. */
export const moveSection = (cats: Cat[], id: CatId, from: number, to: number): Cat[] =>
  cats.map(c => (c.id === id ? (move(c.sections, from, to) === c.sections ? c : { ...c, sections: move(c.sections, from, to) }) : c))
export const toggle = (cats: Cat[], id: CatId): Cat[] => cats.map(c => (c.id === id ? { ...c, open: !c.open } : c))
/** Dopo un trascinamento sulla home (ordine piatto): ogni categoria riprende le sue sezioni in quell'ordine, aperte e chiuse come prima. */
export const reorder = (cats: Cat[], flat: string[]): Cat[] =>
  cats.map(c => { const mine = flat.filter(k => catOf(k) === c.id) as HomeKey[]; return { ...c, sections: [...new Set([...mine, ...c.sections])] } })
