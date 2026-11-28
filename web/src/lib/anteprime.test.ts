import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { APP_ORIGINS, isReady, PREVIEW_SANDBOX } from './anteprime'

// app-visore-web-script-28-11: il sito delle anteprime e l'app devono dirsi le stesse cose. La configurazione dell'hosting, la pagina del
// sito e il codice dell'app non hanno un posto comune: questo test li tiene allineati.
const firebase = JSON.parse(readFileSync(new URL('../../firebase.json', import.meta.url), 'utf8'))
const page = readFileSync(new URL('../../anteprime/index.html', import.meta.url), 'utf8')
const source = readFileSync(new URL('./anteprime.ts', import.meta.url), 'utf8')
const PREVIEW = 'https://supervisor-anteprime.web.app'
type Header = { key: string; value: string }
const csps = (site: { headers: { headers: Header[] }[] }) => site.headers.flatMap((h) => h.headers).filter((h) => h.key === 'Content-Security-Policy').map((h) => h.value)
const directive = (csp: string, name: string) => csp.split(';').map((d) => d.trim()).find((d) => d.startsWith(name + ' '))?.split(/\s+/).slice(1) ?? []

describe('anteprime', () => {
  const [app, prev] = firebase.hosting
  it('due siti: l’app e le anteprime, servite da web/anteprime', () => {
    expect(app.site).toBe('supervisor-francescosorrentino')
    expect(prev.site).toBe('supervisor-anteprime')
    expect(prev.public).toBe('anteprime')
    expect(source).toContain(`'${PREVIEW}'`)
  })
  it('ogni CSP dell’app lascia incorniciare solo sé stessa e il sito delle anteprime', () => {
    expect(csps(app).length).toBeGreaterThan(0)
    for (const c of csps(app)) expect(directive(c, 'frame-src')).toEqual(["'self'", PREVIEW])
  })
  it('la CSP delle anteprime fa girare script e font, niente rete, e solo l’app la incornicia', () => {
    const [c] = csps(prev)
    expect(directive(c, 'script-src')).toEqual(expect.arrayContaining(["'unsafe-inline'", 'https:']))
    expect(directive(c, 'style-src')).toEqual(expect.arrayContaining(["'unsafe-inline'", 'https:']))
    expect(directive(c, 'font-src')).toContain('https:')
    expect(directive(c, 'connect-src')).toEqual(["'none'"])
    expect(directive(c, 'default-src')).toEqual(["'none'"])
    expect(new Set(directive(c, 'frame-ancestors'))).toEqual(new Set(APP_ORIGINS))
  })
  it('la pagina delle anteprime accetta le pagine solo dalle origini dell’app', () => {
    const list = /const APP = \[([^\]]*)\]/.exec(page)?.[1] ?? ''
    expect(new Set([...list.matchAll(/'([^']+)'/g)].map((m) => m[1]))).toEqual(new Set(APP_ORIGINS))
    expect(APP_ORIGINS).toEqual(['https://supervisor.francescosorrentino.com', 'https://supervisor-francescosorrentino.web.app', 'https://supervisor-francescosorrentino.firebaseapp.com'])
  })
  it('il riquadro: script sì, ma non porta via l’app, non apre finestre e non manda moduli', () => {
    const s = PREVIEW_SANDBOX.split(' ')
    expect(s).toContain('allow-scripts')
    for (const no of ['allow-top-navigation', 'allow-popups', 'allow-forms', 'allow-modals']) expect(s).not.toContain(no)
  })
  it('«pronta» vale solo dall’origine delle anteprime e col suo segnale', () => {
    expect(isReady({ origin: PREVIEW, data: { anteprima: 'pronta' } }, PREVIEW)).toBe(true)
    expect(isReady({ origin: 'https://altro.example', data: { anteprima: 'pronta' } }, PREVIEW)).toBe(false)
    expect(isReady({ origin: PREVIEW, data: { html: '<p>' } }, PREVIEW)).toBe(false)
    expect(isReady({ origin: PREVIEW, data: null }, PREVIEW)).toBe(false)
    expect(isReady({ origin: '', data: { anteprima: 'pronta' } }, '')).toBe(false)
  })
})
