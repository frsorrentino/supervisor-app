// La lingua della web app (Franz, 13/10 21:48): inglese di base, italiano se il browser mette l'italiano per primo;
// `?lang=en` o `?lang=it` la forza. Si sceglie una volta all'avvio; nei test (niente `location`) resta l'italiano.

export type Lang = 'it' | 'en'

export function pickLang(search: string, languages: readonly string[]): Lang {
  const q = new URLSearchParams(search).get('lang')
  if (q === 'it' || q === 'en') return q
  return languages[0]?.toLowerCase().startsWith('it') ? 'it' : 'en'
}

export const lang: Lang = typeof location === 'undefined' || typeof navigator === 'undefined'
  ? 'it'
  : pickLang(location.search, navigator.languages?.length ? navigator.languages : [navigator.language ?? ''])

/** Le date e i numeri: 24 ore anche in inglese, come l'app. */
export const locale = lang === 'it' ? 'it-IT' : 'en-GB'
