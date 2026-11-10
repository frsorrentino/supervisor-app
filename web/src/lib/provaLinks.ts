// Le prove citate nei messaggi (app-prova-link-10-11, Franz 10/11 20:59: «quando una sessione cita una prova possiamo linkarla?»):
// «prova #518», «prove #518 e #527», «prove 518, 527 e 543» diventano link `#/prova/<n>`, che aprono la scheda di quella prova.
// Come ProvaLinks in Kotlin.
import type { ProvaOne } from './contract'

/** Un riferimento nel testo: [inizio, fine) e il numero. Il primo di una frase comprende la parola «prova» o «prove». */
export type ProvaRef = { start: number; end: number; n: number }

// Il numero col cancelletto, o nudo di almeno tre cifre (le prove di oggi sono intorno a 500: «prove 10 minuti» non è una prova).
const NUM = String.raw`(?:#\d{1,9}|\d{3,9})`
const MENTION = new RegExp(String.raw`(?<![\p{L}\p{N}_])prov[ae]\s+(${NUM}(?:\s*(?:,|\be\b|\bed\b|\band\b)\s*${NUM})*)(?![\p{L}\p{N}_])`, 'giu')

export function provaRefs(text: string): ProvaRef[] {
  const out: ProvaRef[] = []
  for (const m of text.matchAll(MENTION)) {
    const g = m[1], gStart = m.index! + m[0].lastIndexOf(g)
    let i = 0
    for (const x of g.matchAll(/#?(\d+)/g)) {
      const end = gStart + x.index! + x[0].length
      out.push({ start: i === 0 ? m.index! : gStart + x.index!, end, n: Number(x[1]) })
      i++
    }
  }
  return out
}

/** Il link interno di una prova: `#/prova/<n>`. */
export const provaLink = (n: number) => `#/prova/${n}`
/** Il numero di una prova da un link `<web_app_url>/#/prova/<n>` o da `#/prova/<n>`; null per ogni altro indirizzo. */
export const provaOf = (link: string): number | null => { const m = /#\/prova\/(\d{1,9})$/.exec(link.trim()); return m ? Number(m[1]) : null }

/** Com'è andata la lettura di una prova per numero (contratto 1.134). */
export type ProvaRead = { kind: 'found'; prova: ProvaOne } | { kind: 'unsupported' } | { kind: 'refused'; message: string }
/** La risposta di `prove` con l'arg: la prova; l'elenco di un relay di prima (senza `id` in cima), non supportato; il rifiuto in una riga. */
export function readProva(ok: boolean, text: string): ProvaRead {
  if (!ok) return { kind: 'refused', message: (text.split('\n').find(l => l.trim()) ?? '').slice(0, 200) }
  try {
    const o = JSON.parse(text)
    if (!o || typeof o !== 'object' || !('id' in o) || 'groups' in o) return { kind: 'unsupported' }
    return { kind: 'found', prova: o as ProvaOne }
  } catch { return { kind: 'unsupported' } }
}
