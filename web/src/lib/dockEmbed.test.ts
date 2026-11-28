import { describe, expect, it } from 'vitest'
import { STUDIO, dockCacheOf, dockFrame, dockMessage, dockWanted, learnSpace, readDockCache } from './dockEmbed'

describe('/embed?dock=1, la master dentro Studio (studio-dock-coerente-24-10)', () => {
  it('il messaggio per Studio: tipo cm-dock, stato, altezza in px interi', () => {
    expect(dockMessage('closed', 63.6)).toEqual({ type: 'cm-dock', state: 'closed', height: 64 })
    expect(dockMessage('dragging', -5)).toEqual({ type: 'cm-dock', state: 'dragging', height: 0 })
    expect(STUDIO).toBe('https://studio.francescosorrentino.com')
  })

  it('la master solo con /embed e dock=1', () => {
    expect(dockWanted('/embed', '?dock=1')).toBe(true)
    expect(dockWanted('/embed.html', '?x=2&dock=1')).toBe(true)
    expect(dockWanted('/embed', '')).toBe(false)
    expect(dockWanted('/', '?dock=1')).toBe(false)
  })

  // app-posizioni-sistema-27-11 gruppo 6: il foglio della master al posto della barra; Studio riceve sempre l'altezza del foglio.
  it("lo stato per Studio dalla misura del foglio: riga chiusa, metà e intera aperte, in movimento l'altezza che segue", () => {
    expect(dockFrame('row', 150.4, 900, false)).toEqual({ state: 'closed', height: 150 })
    expect(dockFrame('half', 522, 900, false)).toEqual({ state: 'open', height: 522 })
    expect(dockFrame('full', 892, 900, false)).toEqual({ state: 'open', height: 892 })
    expect(dockFrame('move', 333.6, 900, false)).toEqual({ state: 'dragging', height: 334 })
  })

  it('con un menu o una finestra aperti il riquadro prende tutto il posto, mai durante il gesto', () => {
    expect(dockFrame('row', 150, 900, true)).toEqual({ state: 'open', height: 900 })
    expect(dockFrame('half', 522, 900, true)).toEqual({ state: 'open', height: 900 })
    expect(dockFrame('move', 300, 900, true)).toEqual({ state: 'dragging', height: 300 })
  })

  it('se Studio dà al riquadro meno del chiesto, il posto diventa quel riquadro e il foglio intero sta tutto dentro', () => {
    // Chiesta l'intera (900 - 8) e avuti 700: il posto è 700, l'intera 692 con lo stacco sopra.
    expect(learnSpace(900, 892, 700)).toBe(700)
    // Chiesto tutto il posto per un menu e avuti 700: lo stesso.
    expect(learnSpace(900, 900, 700)).toBe(700)
    // Avuto quanto chiesto, o un pixel di arrotondamento: il posto resta.
    expect(learnSpace(900, 522, 522)).toBe(900)
    expect(learnSpace(900, 522, 521)).toBe(900)
  })

  it("la cache della barra tiene la sessione e solo l'ultima risposta della master", () => {
    const m = { name: 'master', context: 34 }
    const raw = dockCacheOf(m, [{ role: 'assistant', text: 'Esito: vecchio' }, { role: 'assistant', text: 'Esito: nuovo' }, { role: 'user', text: 'ok' }, { role: 'tool', text: 'x' }])
    expect(readDockCache(raw)).toEqual({ master: m, entries: [{ role: 'assistant', text: 'Esito: nuovo' }] })
    expect(readDockCache(dockCacheOf(m, []))).toEqual({ master: m, entries: [] })
    expect(readDockCache(null)).toBeNull()
    expect(readDockCache('{rotto')).toBeNull()
    expect(readDockCache('{"entries":[]}')).toBeNull()
  })
})
