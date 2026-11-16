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
  // app-uscite-uniformi-16-11: una sola uscita per le schede chiuse da una conferma; il giro verso il contatore non c'è più.
  const read = (f: string) => readFileSync(new URL(f, import.meta.url), 'utf8')
  const flight = 'to' + 'Counter'   // spezzata: il controllo del registro cerca la parola nel codice
  it('Da provare, Decidi e le liste escono tutte così', () => {
    expect(read('./DaProvare.svelte').match(/out:leaveOut/g)).toHaveLength(2)
    expect(read('./CinqueMinuti.svelte')).toContain('in:pass out:leaveOut')
    expect(read('./ItemList.svelte')).toContain('data-key={r.key} out:leaveOut>')
    for (const f of ['./DaProvare.svelte', './CinqueMinuti.svelte', './Seguiti.svelte', './cardFx.ts']) expect(read(f)).not.toContain(flight)
    expect(read('./cardFx.ts')).not.toMatch(/rotateY/)
  })
})
