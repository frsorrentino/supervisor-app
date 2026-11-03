import { describe, expect, it } from 'vitest'
import { decisions, prove, seguiti } from './homeRight'

describe('voci_nuove_testo', () => {
  it('voci_nuove_testo: solo quello che il numero non dice', () => {
    expect(prove({ n: 5, builds: 2, red: 0, ko: 1 })).toEqual([{ part: 'builds', n: 2 }, { part: 'ko', n: 1 }])
    expect(prove({ n: 3, builds: 1, red: 0, ko: 0 })).toEqual([{ part: 'builds', n: 1 }])
    expect(decisions({ n: 2, here: 1, tasks: 3, snoozed: 0 })).toEqual([{ part: 'here', n: 1 }, { part: 'unblocks', n: 3 }])
    expect(decisions({ n: 2, here: 0, tasks: 3, snoozed: 0 })).toEqual([{ part: 'unblocks', n: 3 }])
    expect(seguiti({ rimasto: 2, altrove: 0, promemoria: 1 })).toEqual([{ part: 'behind', n: 2 }, { part: 'reminders', n: 1 }])
  })
  it('voci_nuove_testo: la home dà a ogni voce nuova il suo testo, e il numero di Esiti sta accanto al nome', async () => {
    const { readFileSync } = await import('node:fs')
    const home = readFileSync(new URL('./Home.svelte', import.meta.url), 'utf8')
    // Prima raccolta sta dentro Decidi e il suo stato apre il testo di Decidi (app-sezioni-accorpate-03-11).
    for (const k of ['seguiti', 'prove', 'decisioni']) expect(home, k).toContain(`rightOf('${k}')`)
    expect(home).toContain("sec('esiti', part && out.red.length ? t.deskRedToday : t.homeOutcomes, 'sports_score', esitiCount,")
  })
})
