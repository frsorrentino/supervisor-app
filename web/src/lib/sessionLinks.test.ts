import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { askedSession, sessionOf, stripSessionLinks } from './sessionLinks'

// Gli stessi casi di SessionLinksTest in Kotlin: i link delle sessioni diventano il tasto «Apri in supervisor».
const st = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const sessions = st.sessions
const atlas = 'https://claude.ai/code/session_018CKZ1Pum1Qs7DX5hbRLQ6X'
const ledger = 'https://claude.ai/code/session_01CnGG8im7UG4KtjbPDx9fst'

describe('link di sessione', () => {
  it('riconosce il link della sessione', () => {
    expect(sessionOf(atlas, sessions)?.name).toBe('atlas-shop')
    expect(sessionOf(`${atlas}/`, sessions)?.name).toBe('atlas-shop')
    expect(sessionOf(`${atlas}?x=1#top`, sessions)?.name).toBe('atlas-shop')
    expect(sessionOf('https://claude.ai/code/session_unknown', sessions)).toBeNull()
    expect(sessionOf('', sessions)).toBeNull()
  })
  it('il messaggio della master nella segnalazione', () => {
    const text = '• **Prossimo passo:** rivedere il flusso dei rimborsi.\n• **Link:**\nhttps://claude.ai/code/session_018CKZ1Pum1Qs7DX5hbRLQ6X\n\nEsito: sessione atlas-shop aperta.'
    const r = stripSessionLinks(text, sessions)
    expect(r.text).toBe('• **Prossimo passo:** rivedere il flusso dei rimborsi.\n\nEsito: sessione atlas-shop aperta.')
    expect(r.buttons).toEqual([{ session: 'atlas-shop', url: atlas }])
  })
  it("l'etichetta sulla stessa riga sparisce", () => {
    expect(stripSessionLinks(`Sessione avviata.\n• Link: ${atlas}`, sessions).text).toBe('Sessione avviata.')
    expect(stripSessionLinks(`Sessione avviata.\nLink: ${atlas}.`, sessions).text).toBe('Sessione avviata.')
  })
  it('una frase tiene le parole e perde i due punti', () => {
    expect(stripSessionLinks(`La sessione atlas-shop è aperta e la trovi qui: ${atlas}`, sessions).text).toBe('La sessione atlas-shop è aperta e la trovi qui')
    expect(stripSessionLinks(`**La sessione atlas-shop è aperta e la trovi qui: ${atlas}**`, sessions).text).toBe('**La sessione atlas-shop è aperta e la trovi qui**')
  })
  it('la riga del recap tiene il resto e perde le parentesi vuote', () => {
    const r = stripSessionLinks(`🟧 ledger-api (${ledger}) · AskUserQuestion\n🟢 atlas-shop (${atlas}): fatto.`, sessions)
    expect(r.text).toBe('🟧 ledger-api · AskUserQuestion\n🟢 atlas-shop: fatto.')
    expect(r.buttons.map(b => b.session)).toEqual(['ledger-api', 'atlas-shop'])
  })
  it('un link con testo lascia il testo', () => {
    const r = stripSessionLinks(`Apri [atlas-shop](${atlas}) e [la doc](https://example.com/x).`, sessions)
    expect(r.text).toBe('Apri atlas-shop e [la doc](https://example.com/x).')
    expect(r.buttons).toEqual([{ session: 'atlas-shop', url: atlas }])
  })
  it("un tasto per sessione, nell'ordine in cui compaiono", () => {
    const r = stripSessionLinks(`${ledger} poi ${atlas} e ancora ${ledger}`, sessions)
    expect(r.buttons.map(b => b.session)).toEqual(['ledger-api', 'atlas-shop'])
    expect(r.text).toBe('poi e ancora')
    expect(stripSessionLinks(`- a\n  - b qui ${atlas}`, sessions).text).toBe('- a\n  - b qui')
  })
  it('gli altri link restano', () => {
    const text = 'Mockup: https://claude.ai/artifact/X e https://claude.ai/code/session_unknown'
    expect(stripSessionLinks(text, sessions)).toEqual({ text, buttons: [] })
    expect(stripSessionLinks('niente', sessions).text).toBe('niente')
    expect(stripSessionLinks(atlas, []).text).toBe(atlas)
  })
  it("l'indirizzo chiede una sessione con ?s= o #nome", () => {
    expect(askedSession('?s=legnaia', '')).toBe('legnaia')
    expect(askedSession('?demo&s=field%20notes', '#altro')).toBe('field notes')
    expect(askedSession('?demo', '#master')).toBe('master')
    expect(askedSession('', '')).toBe('')
  })
})
