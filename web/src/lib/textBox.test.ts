import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'

// app-pillola-p-tagliata-09-11 (Franz 09/11 10:09: «la pillola taglia la p in basso»): `text-box: trim-both cap alphabetic`
// porta il riquadro del testo dalle maiuscole alla linea di base, e un `overflow` che taglia in verticale sullo stesso elemento
// nasconde le gambe di p, g, q e y. Per ogni regola con text-box nei componenti, le altre regole che finiscono sullo stesso
// elemento (stessa ultima classe) possono tagliare solo di lato.
const dir = new URL('./', import.meta.url)
const files = [...readdirSync(dir).filter(f => f.endsWith('.svelte')).map(f => new URL(f, dir)), new URL('../App.svelte', dir)]
type Rule = { file: string; selectors: string[]; body: string }
const rules: Rule[] = files.flatMap(u => {
  const css = [...readFileSync(u, 'utf8').matchAll(/<style[^>]*>([\s\S]*?)<\/style>/g)].map(m => m[1]).join('\n').replace(/\/\*[\s\S]*?\*\//g, '')
  return [...css.matchAll(/([^{}]+)\{([^{}]*)\}/g)].map(m => ({ file: u.pathname.split('/').pop()!, selectors: m[1].split(',').map(s => s.trim()).filter(Boolean), body: m[2] }))
})
const last = (sel: string) => sel.split(/[\s>+~]+/).pop()!.match(/\.[\w-]+/g)?.pop() ?? null
const clipsY = (body: string) => /(^|;|\s)overflow\s*:\s*(hidden|clip|auto|scroll)\b/.test(body) || /overflow-y\s*:\s*(hidden|clip|auto|scroll)/.test(body)

describe('text-box', () => {
  it('un testo portato alla linea di base non è tagliato in verticale', () => {
    const trimmed = rules.filter(r => /text-box\s*:\s*trim/.test(r.body))
    expect(trimmed.length).toBeGreaterThan(0)
    const bad = trimmed.flatMap(t => t.selectors.map(s => [t.file, last(s)] as const)).flatMap(([file, cls]) =>
      rules.filter(r => r.file === file && cls && r.selectors.some(s => last(s) === cls) && clipsY(r.body)).map(r => `${file}: ${r.selectors.join(', ')} { ${r.body.trim()} }`))
    expect(bad).toEqual([])
  })
})
