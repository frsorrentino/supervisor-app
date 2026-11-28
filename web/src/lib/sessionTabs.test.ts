import { describe, expect, it } from 'vitest'
import {
  closesOnDrag, countOf, dragOffset, fastOutSlowIn, MAX_STAGGER, newResults, notesCount, pick, ROW_MS, rowDelay, SHADE_MS, shadeOpen, TABS,
  tabCounts, toneOf,
} from './sessionTabs'

// La testata della sessione con la riga di icone e la tendina (app-posizioni-sistema-27-11, gruppo 2). Gli stessi casi di
// SessionTabsTest in Kotlin.
describe('schede della sessione', () => {
  it('le schede sono cinque e stanno sempre nello stesso ordine', () => {
    expect(TABS).toEqual(['chat', 'perte', 'risultati', 'modifiche', 'dettagli'])
  })

  it('Per te conta le voci che aspettano e gli avvisi, Risultati i nuovi, Modifiche i file', () => {
    expect(tabCounts(3, 1, 1, 4)).toEqual({ perte: 4, risultati: 1, modifiche: 4 })
    expect(tabCounts(0, 2, 0, null)).toEqual({ perte: 2, risultati: 0, modifiche: 0 })
    expect(tabCounts(0, 0, 0, undefined)).toEqual({ perte: 0, risultati: 0, modifiche: 0 })
  })

  it('i numeri non sono mai negativi', () => {
    expect(tabCounts(-1, -3, -2, -5)).toEqual({ perte: 0, risultati: 0, modifiche: 0 })
  })

  it('gli avvisi sono quelli della pillola Avvisi: la sessione --bg e la nota di Claude Code', () => {
    expect(notesCount(false, false)).toBe(0)
    expect(notesCount(true, false)).toBe(1)
    expect(notesCount(false, true)).toBe(1)
    expect(notesCount(true, true)).toBe(2)
  })

  it('un risultato è nuovo se la sua ultima versione non è vista', () => {
    expect(newResults([{ unseen: true }, { unseen: false }, { unseen: true }, {}])).toBe(2)
    expect(newResults([])).toBe(0)
  })

  it('il colore del numero dice lo stato: ambra Per te, verde Risultati, grigio Modifiche', () => {
    expect(toneOf('perte')).toBe('wait')
    expect(toneOf('risultati')).toBe('new')
    expect(toneOf('modifiche')).toBe('count')
  })

  it('Chat e Dettagli non hanno numero', () => {
    const c = tabCounts(3, 1, 2, 4)
    expect(countOf('chat', c)).toBe(0)
    expect(countOf('dettagli', c)).toBe(0)
    expect(countOf('perte', c)).toBe(4)
    expect(countOf('risultati', c)).toBe(2)
    expect(countOf('modifiche', c)).toBe(4)
  })

  it('un tocco su una scheda apre la tendina, sulla scheda aperta o su Chat la chiude', () => {
    expect(pick('chat', 'perte')).toBe('perte')
    expect(pick('perte', 'perte')).toBe('chat')
    expect(pick('perte', 'chat')).toBe('chat')
    expect(pick('chat', 'chat')).toBe('chat')
  })

  it('una scheda diversa prende il posto di quella aperta', () => {
    expect(pick('perte', 'dettagli')).toBe('dettagli')
    expect(pick('modifiche', 'risultati')).toBe('risultati')
  })

  it('una chiave sconosciuta lascia la scheda aperta', () => {
    expect(pick('dettagli', 'altro')).toBe('dettagli')
  })

  it('la tendina è aperta su ogni scheda tranne Chat', () => {
    expect(shadeOpen('chat')).toBe(false)
    for (const t of TABS.slice(1)) expect(shadeOpen(t)).toBe(true)
  })

  it('i tempi sono quelli del centro notifiche: 360 ms e 32 ms fra le righe', () => {
    expect(SHADE_MS).toBe(360)
    expect(ROW_MS).toBe(32)
    expect(rowDelay(0)).toBe(0)
    expect(rowDelay(1)).toBe(32)
    expect(rowDelay(3)).toBe(96)
  })

  it('dopo otto righe il ritardo non cresce più', () => {
    expect(rowDelay(MAX_STAGGER)).toBe(256)
    expect(rowDelay(MAX_STAGGER + 5)).toBe(256)
    expect(rowDelay(-2)).toBe(0)
  })

  it('col dito la tendina sale soltanto, mai oltre la sua altezza', () => {
    expect(dragOffset(40, 500)).toBe(0)
    expect(dragOffset(-120, 500)).toBe(-120)
    expect(dragOffset(-900, 500)).toBe(-500)
  })

  it('tirata su oltre un quarto della sua altezza, o con uno slancio, si chiude', () => {
    expect(closesOnDrag(-125, 0, 500)).toBe(true)
    expect(closesOnDrag(-100, 0, 500)).toBe(false)
    expect(closesOnDrag(-30, -900, 500)).toBe(true)
    expect(closesOnDrag(-30, -500, 500)).toBe(false)
  })

  it('tirata in giù non si chiude mai', () => {
    expect(closesOnDrag(0, -2000, 500)).toBe(false)
    expect(closesOnDrag(80, -2000, 500)).toBe(false)
  })

  it('la curva FAST_OUT_SLOW_IN parte veloce e arriva piano', () => {
    expect(fastOutSlowIn(0)).toBe(0)
    expect(fastOutSlowIn(1)).toBe(1)
    expect(fastOutSlowIn(-1)).toBe(0)
    expect(fastOutSlowIn(2)).toBe(1)
    // A metà del tempo è già oltre i tre quarti della corsa, come cubic-bezier(0.4, 0, 0.2, 1).
    expect(fastOutSlowIn(0.5)).toBeCloseTo(0.774, 2)
    let last = 0
    for (let k = 1; k <= 20; k++) { const v = fastOutSlowIn(k / 20); expect(v).toBeGreaterThanOrEqual(last); last = v }
  })
})
