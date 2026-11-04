import { describe, expect, it } from 'vitest'
import { anonymize, deviceOf, draftOf, titleOf } from './feedback'

// Feedback dal menù (app-feedback-menu-04-11).
const labels = { kind: { idea: 'Suggerimento', bug: 'Bug' }, tech: 'Dati tecnici', photo: 'Foto della pagina allegata' }
const tech = { version: '0.2.1890', commit: 'abc1234', device: 'Chrome 156 · Windows · 1600×900', screen: 'schermata: Lavori', lang: 'lingua: it' }

describe('feedback', () => {
  it('il dispositivo dal browser', () => {
    expect(deviceOf('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/156.0.0.0 Safari/537.36', 1600, 900)).toBe('Chrome 156 · Windows · 1600×900')
    expect(deviceOf('Mozilla/5.0 (Linux; Android 16) Chrome/156.0 Mobile Safari/537.36 Edg/156.0', 412, 915)).toBe('Edge 156 · Android · 412×915')
  })
  it('la bozza è anonima: email, cartelle personali, chiavi, IP', () => {
    const s = anonymize('scrivi a fr.x@gmail.com, file in C:\\Users\\Utente\\x e /home/franz/y, token ghp_abcdefghijklmnopqrstuvwxyz0123 da 192.168.1.20')
    expect(s).toBe('scrivi a <email>, file in C:\\Users\\<utente>\\x e /home/<utente>/y, token <chiave> da <ip>')
  })
  it('il titolo è la prima riga, tagliata a parola intera', () => {
    expect(titleOf('La home sbiadisce\naltro')).toBe('La home sbiadisce')
    const t = titleOf('parola '.repeat(20))
    expect(t.length).toBeLessThanOrEqual(72)
    expect(t.endsWith('parola')).toBe(true)
  })
  it('la bozza porta tipo, testo, dati tecnici e la foto solo se accesa', () => {
    const d = draftOf('bug', 'Il tasto non risponde\nsu Lavori', tech, false, labels)
    expect(d.title).toBe('[Bug] Il tasto non risponde')
    expect(d.body).toContain('- app: 0.2.1890 (abc1234)')
    expect(d.body).toContain('schermata: Lavori')
    expect(d.body).not.toContain('Foto')
    expect(draftOf('idea', 'x', tech, true, labels).body).toContain('_Foto della pagina allegata_')
  })
})
