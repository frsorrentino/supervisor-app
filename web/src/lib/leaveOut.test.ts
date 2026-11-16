import { readFileSync } from 'node:fs'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { LEAVE_FADE_MS, LEAVE_MS, leaveOut } from './motion'

// app-prove-chiusura-anim-16-11 (Franz 16/11 09:58): in «Da provare» la scheda approvata esce come le altre che si chiudono, in altezza
// e senza rotazione; con la riduzione del movimento solo la dissolvenza.
describe('uscita_semplice', () => {
  const node = { offsetHeight: 80 } as unknown as Element
  afterEach(() => vi.unstubAllGlobals())
  it('si chiude in altezza e sfuma, senza trasformazioni', () => {
    const o = leaveOut(node) as { duration: number; css: (t: number) => string }
    expect(o.duration).toBe(LEAVE_MS)
    expect(o.css(0.5)).toBe('overflow: hidden; height: 40.0px; opacity: 0.5')
    expect(o.css(0.5)).not.toMatch(/rotate|transform/)
  })
  it('con meno movimento solo la dissolvenza', () => {
    vi.stubGlobal('matchMedia', () => ({ matches: true }))
    const o = leaveOut(node) as { duration: number; css: (t: number) => string }
    expect([o.duration, o.css(0.25)]).toEqual([LEAVE_FADE_MS, 'opacity: 0.25'])
  })
  it('spenta non anima', () => expect(leaveOut(node, { on: false })).toEqual({ duration: 0 }))
  it('Da provare non usa più il giro verso il contatore', () => {
    const page = readFileSync(new URL('./DaProvare.svelte', import.meta.url), 'utf8')
    expect(page).not.toMatch(/cardFx|toCounter/)
    expect(page.match(/out:leaveOut/g)).toHaveLength(2)
    expect(page).toContain('{focus} leave onOpen')
    expect(page).toContain('testid="prova-ko" leave>')
  })
})
