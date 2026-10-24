import { describe, expect, it } from 'vitest'
import { STUDIO, dockMessage, dockWanted, dragHeight, settle } from './dockEmbed'

describe('/embed?dock=1, la master dentro Studio (studio-dock-coerente-24-10)', () => {
  it('il messaggio per Studio: tipo cm-dock, stato, altezza in px interi', () => {
    expect(dockMessage('closed', 63.6)).toEqual({ type: 'cm-dock', state: 'closed', height: 64 })
    expect(dockMessage('dragging', -5)).toEqual({ type: 'cm-dock', state: 'dragging', height: 0 })
    expect(STUDIO).toBe('https://studio.francescosorrentino.com')
  })

  it('la barra solo con /embed e dock=1', () => {
    expect(dockWanted('/embed', '?dock=1')).toBe(true)
    expect(dockWanted('/embed.html', '?x=2&dock=1')).toBe(true)
    expect(dockWanted('/embed', '')).toBe(false)
    expect(dockWanted('/', '?dock=1')).toBe(false)
  })

  it("durante il gesto l'altezza segue il dito, fra barra e foglio aperto", () => {
    expect(dragHeight(64, -100, 64, 800)).toBe(164)
    expect(dragHeight(64, 50, 64, 800)).toBe(64)
    expect(dragHeight(800, -50, 64, 800)).toBe(800)
    expect(dragHeight(800, 300, 64, 800)).toBe(500)
  })

  it('a fine gesto: oltre 40 px in su apre, oltre 40 px in giù chiude, altrimenti torna com era', () => {
    expect(settle(false, -41)).toBe(true)
    expect(settle(false, -30)).toBe(false)
    expect(settle(true, 41)).toBe(false)
    expect(settle(true, 30)).toBe(true)
    expect(settle(true, -200)).toBe(true)
  })
})
