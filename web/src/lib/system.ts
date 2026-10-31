import type { SystemAdvice, SystemLevel, SystemStatus } from './contract'

type Lang = 'it' | 'en'

/** Il tocco sull'intestazione vale per il livello in cui è stato dato. */
export type Manual = { level: SystemLevel; open: boolean } | null

/** Franz 28/10 13:19: compatta quando è tutto verde, aperta da sola in giallo e in rosso. */
export const isOpen = (level: SystemLevel, manual: Manual): boolean => (manual && manual.level === level ? manual.open : level !== 'green')

/** Il tasto solo se il consiglio ne porta uno e il relay ne esegue l'op (`heavy_job` e `low_memory` sono solo parole). */
export const actionOf = (a: SystemAdvice, ops: string[] | null | undefined) => (a.action && (ops ?? []).includes(a.action.op) ? a.action : null)

export const num = (x: number, lang: Lang) => new Intl.NumberFormat(lang === 'it' ? 'it-IT' : 'en-US', { maximumFractionDigits: 1 }).format(x)
const size = (mb: number, lang: Lang) => (mb < 1024 ? `${Math.round(mb)} MB` : `${num(mb / 1024, lang)} GB`)

/** La riga dell'intestazione: carico per core e memoria libera; il totale sta nella scheda aperta (a 320 dp andava a capo). */
export const summaryLine = (s: SystemStatus, lang: Lang): string =>
  lang === 'it'
    ? `carico ${num(s.load_per_core, lang)} · ${num(s.mem_available_mb / 1024, lang)} GB`
    : `load ${num(s.load_per_core, lang)} · ${num(s.mem_available_mb / 1024, lang)} GB`

const GROUP: Record<Lang, Record<string, string>> = {
  it: { claude: 'Claude', chrome_bridge: 'chrome-bridge', relay: 'relay', other: 'altri' },
  en: { claude: 'Claude', chrome_bridge: 'chrome-bridge', relay: 'relay', other: 'other' },
}

/** Chi consuma, nell'ordine del relay (più CPU prima); vuota quando lo stato tagliato a 8 KB li ha tolti. */
export const consumersLine = (s: SystemStatus, lang: Lang): string =>
  s.consumers
    .map(c => `${GROUP[lang][c.group] ?? c.group}${c.count > 1 ? ` ×${c.count}` : ''} ${Math.round(c.cpu)}% ${size(c.rss_mb, lang)}`)
    .join(' · ')
