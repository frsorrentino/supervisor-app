import { describe, expect, it } from 'vitest'
import { BAR, applyTheme, isDay, themeOf } from './theme'

// Automatico, Giorno, Notte (app-tema-giorno-notte-11-11): come AppThemeTest.kt, più quello che la pagina mette nel documento.
describe('tema', () => {
  it('Automatico segue il sistema, Giorno e Notte no', () => {
    expect(isDay('auto', true)).toBe(true)
    expect(isDay('auto', false)).toBe(false)
    for (const light of [true, false]) { expect(isDay('day', light)).toBe(true); expect(isDay('night', light)).toBe(false) }
  })
  it('/embed resta di notte', () => { expect(isDay('day', true, true)).toBe(false) })
  it('una chiave vuota o sconosciuta vale Automatico', () => {
    expect(themeOf(null)).toBe('auto'); expect(themeOf('sepia')).toBe('auto'); expect(themeOf('day')).toBe('day'); expect(themeOf('night')).toBe('night')
  })

  // Senza DOM nei test: il documento finto ha solo quello che applyTheme tocca.
  const page = () => {
    const cls = new Set<string>()
    const html = { classList: { contains: (c: string) => cls.has(c), toggle: (c: string, on: boolean) => { if (on) cls.add(c); else cls.delete(c) } }, style: { colorScheme: '' } }
    const metas = [{ media: '(prefers-color-scheme: dark)', content: BAR.night }, { media: '(prefers-color-scheme: light)', content: BAR.day }]
    const scheme = { content: 'light dark', setAttribute(_: string, v: string) { this.content = v }, getAttribute() { return this.content } }
    return { documentElement: html, querySelector: () => scheme, querySelectorAll: () => metas } as unknown as Document
  }
  const bars = (d: Document) => [...(d.querySelectorAll('meta') as unknown as HTMLMetaElement[])].map(m => m.content)

  it('Giorno col sistema scuro: classe, color-scheme e barre del giorno', () => {
    const d = page(); applyTheme('day', d, false)
    expect(d.documentElement.classList.contains('theme-day')).toBe(true)
    expect(d.documentElement.style.colorScheme).toBe('light')
    expect(d.querySelector('meta[name="color-scheme"]')?.getAttribute('content')).toBe('light')
    expect(bars(d)).toEqual([BAR.day, BAR.day])
  })
  it('Notte col sistema chiaro, poi di nuovo Automatico', () => {
    const d = page(); applyTheme('night', d, true)
    expect(d.documentElement.classList.contains('theme-day')).toBe(false)
    expect(d.documentElement.style.colorScheme).toBe('dark')
    expect(bars(d)).toEqual([BAR.night, BAR.night])
    applyTheme('auto', d, true)
    expect(d.documentElement.classList.contains('theme-day')).toBe(true)
    expect(d.documentElement.style.colorScheme).toBe('')
    expect(d.querySelector('meta[name="color-scheme"]')?.getAttribute('content')).toBe('light dark')
    expect(bars(d)).toEqual([BAR.night, BAR.day])
  })
})
