import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// app-awaiting-etichetta-15-11 (scelta di Franz, 15/11 19:45: «Al lavoro»): una sessione awaiting conta fra le al lavoro e la parola
// lo dice, in italiano e in inglese, su web, telefono e orologio. Legge anche le stringhe di telefono e orologio.
describe('awaiting_parola', () => {
  const word = (file: string, key: string) => [...readFileSync(new URL(file, import.meta.url), 'utf8').matchAll(new RegExp(`${key}[^\n]*?awaiting: '([^']*)'`, 'g'))].map(m => m[1])
  it('web: la stessa parola di busy', () => {
    expect(word('./t.ts', "busy: 'al lavoro',")).toEqual(['al lavoro'])
    expect(word('./t.ts', "busy: 'working',")).toEqual(['working'])
  })
  const xml = (p: string, name: string) => readFileSync(new URL(`../../../${p}`, import.meta.url), 'utf8').match(new RegExp(`<string name="${name}">([^<]*)<`))?.[1]
  it('telefono e orologio', () => {
    expect(xml('mobile/src/main/res/values/strings.xml', 'state_awaiting')).toBe('al lavoro')
    expect(xml('mobile/src/main/res/values-en/strings.xml', 'state_awaiting')).toBe('working')
    expect(xml('wear/src/main/res/values/strings.xml', 'state_awaiting_prompt')).toBe('al lavoro')
    expect(xml('wear/src/main/res/values-en/strings.xml', 'state_awaiting_prompt')).toBe('working')
  })
})
