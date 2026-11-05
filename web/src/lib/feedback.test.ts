import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { anonymize, bytesOf, deviceOf, draftOf, parseChoice, parseDraft, relayKind, relayTech, titleOf } from './feedback'

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

// Contratti 1.121 e 1.123, `feedback_app`: le voci 1210-1214 di cmd-result-sample.json.
describe('feedback_app', () => {
  const sample = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8'))
  const res = (n: string) => sample.result.find((r: { id: string }) => r.id.endsWith(n))
  it('la bozza del primo passo ha hash, titolo e i tre tasti', () => {
    const d = parseDraft(res('00001210').text)!
    expect(d.hash).toBe('7c41e09a2b')
    expect(d.screenshot).toBe(true)
    expect(d.options.map(o => o.id)).toEqual(['github', 'anonymous', 'later'])
    // 1.123: con la schermata la riga da mostrare prima dei tasti; github_ok assente, quindi niente GitHub.
    expect(d.screenshot_notice).toMatch(/maintainer/)
    expect(d.github_ok).toBeUndefined()
    expect(parseDraft(res('00001214').text)).toBeNull()
  })
  it("l'esito del secondo: github col link, anonimo col link, più tardi senza", () => {
    expect(parseChoice(res('00001212').text)!.url).toMatch(/^https:\/\/github\.com\/frsorrentino\/supervisor-feedback\/issues\/new\?/)
    expect(parseChoice(res('00001212').text)!.screenshot_url).toMatch(/^https:\/\/github\.com\/frsorrentino\/supervisor-feedback\/raw\//)
    expect(parseChoice(res('00001211').text)!.choice).toBe('anonymous')
    expect(parseChoice(res('00001213').text)).toEqual({ choice: 'later', url: null, text: expect.any(String), screenshot_url: null })
    expect(parseChoice(res('00001211').text)!.text).toMatch(/with the screenshot/)
  })
  it('tipo e dati tecnici come li vuole il relay', () => {
    expect(relayKind('idea')).toBe('suggerimento')
    expect(relayKind('bug')).toBe('bug')
    const t = relayTech({ version: '0.2.1', commit: 'abc', device: 'x'.repeat(100), screen: 'Home', lang: 'it' })
    expect(Object.keys(t).every(k => /^[a-z_]+$/.test(k))).toBe(true)
    expect(t.device.length).toBe(80)
    // Le richieste del primo passo nella fixture usano le stesse chiavi.
    const cmd = sample.cmds?.find?.((c: { id: string }) => c.id.endsWith('00001210')) ?? sample.cmd?.find?.((c: { id: string }) => c.id.endsWith('00001210'))
    if (cmd) expect(Object.keys(cmd.tech).every((k: string) => /^[a-z_]+$/.test(k))).toBe(true)
  })
  it('la foto in byte per /share', () => {
    expect(bytesOf('data:image/png;base64,AAEC')).toEqual({ mime: 'image/png', data: new Uint8Array([0, 1, 2]) })
    expect(bytesOf('nope')).toBeNull()
  })
})
