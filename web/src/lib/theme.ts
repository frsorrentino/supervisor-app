// Il tema scelto nelle impostazioni (app-tema-giorno-notte-11-11, Franz 11/11 12:31; un collega vedeva la PWA di giorno senza modo di
// cambiarla): Automatico segue il sistema come prima, Giorno e Notte lo fissano. La scelta resta nel browser. Come AppTheme.kt.
// I colori del giorno stanno sotto `html.theme-day` (v3.css); la classe la mette all'avvio public/theme-boot.js, prima di disegnare,
// e poi `applyTheme` a ogni scelta e a ogni cambio del sistema. `/embed` resta di notte (night-only).

export type ThemeChoice = 'auto' | 'day' | 'night'
export const THEME_KEY = 'cm.theme'
/** La barra del browser e della PWA (theme-color): il fondo della pagina nei due temi. */
export const BAR = { day: '#F4F4F4', night: '#0B0B0B' } as const

/** Dalla chiave salvata; vuota o sconosciuta vale Automatico. */
export const themeOf = (v: string | null | undefined): ThemeChoice => (v === 'day' || v === 'night' ? v : 'auto')

/** Giorno o notte per una scelta, dato il sistema; `/embed` sempre di notte. */
export const isDay = (c: ThemeChoice, systemLight: boolean, nightOnly = false): boolean => !nightOnly && (c === 'day' || (c === 'auto' && systemLight))

export function loadTheme(): ThemeChoice { try { return themeOf(localStorage.getItem(THEME_KEY)) } catch { return 'auto' } }

const systemLight = () => typeof matchMedia === 'function' && matchMedia('(prefers-color-scheme: light)').matches

/** Mette il tema sulla pagina: la classe, color-scheme (moduli e barre di scorrimento) e le due theme-color della PWA. */
export function applyTheme(c: ThemeChoice, doc: Document = document, light = systemLight()): void {
  const html = doc.documentElement
  const nightOnly = html.classList.contains('night-only')
  const day = isDay(c, light, nightOnly)
  html.classList.toggle('theme-day', day)
  html.style.colorScheme = c === 'auto' && !nightOnly ? '' : day ? 'light' : 'dark'
  doc.querySelector('meta[name="color-scheme"]')?.setAttribute('content', c === 'auto' ? 'light dark' : day ? 'light' : 'dark')
  // Con una scelta fissa entrambe le theme-color (una per media) dicono il colore scelto: Safari su iPhone prende la sua.
  doc.querySelectorAll<HTMLMetaElement>('meta[name="theme-color"]').forEach(m => {
    const own = m.media.includes('light') ? BAR.day : m.media.includes('dark') ? BAR.night : m.content
    m.content = c === 'auto' ? own : day ? BAR.day : BAR.night
  })
}

/** Salva e applica subito. */
export function setTheme(c: ThemeChoice): void {
  try { if (c === 'auto') localStorage.removeItem(THEME_KEY); else localStorage.setItem(THEME_KEY, c) } catch { /* vale per questa visita */ }
  applyTheme(c)
}

/** All'avvio: applica la scelta e segue il sistema finché è Automatico. */
export function initTheme(): void {
  applyTheme(loadTheme())
  if (typeof matchMedia === 'function') matchMedia('(prefers-color-scheme: light)').addEventListener('change', () => applyTheme(loadTheme()))
}
