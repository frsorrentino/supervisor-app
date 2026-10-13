import { describe, expect, it } from 'vitest'
import { demoAgenda, demoEvents, demoNight, demoState, demoTranscripts } from './demo'
import { showHomeNight } from './night'

describe('i dati della demo (Franz, 13/10 22:30)', () => {
  it('lo stato porta il riferimento al rapporto della notte di prova: la home mostra il riquadro Notte', () => {
    expect(demoState.night.report?.date).toBe(demoNight.date)
    expect(showHomeNight(demoState.night.report, null)).toBe(true)
  })

  it('nessun nome vero: né i vecchi nomi del plugin né Franz', () => {
    const all = JSON.stringify([demoState, demoTranscripts, demoAgenda, demoEvents])
    for (const name of ['claude-master', 'team-supervisor', 'franz', 'Franz', 'personali/']) expect(all).not.toContain(name)
  })
})
