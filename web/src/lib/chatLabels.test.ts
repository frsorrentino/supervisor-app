import { describe, expect, it } from 'vitest'
import { replaceNames, splitLabels as s, TASK_URL as u, taskNames } from './chatLabels'

const plain = (text: string) => [{ kind: 'plain', text }]
describe('chat_etichette', () => {
  it('chat_etichette: Da te in tutte le forme', () => {
    const body = '1. Installa\n2. Prova'
    for (const h of ['Da te', 'Da te:', '**Da te**', '**Da te:**', '**Da te**:', '__Da te__'])
      expect(s(`Prima\n\n${h}\n${body}\n\nDopo`), h).toEqual([{ kind: 'plain', text: 'Prima' }, { kind: 'you', title: 'Da te', body }, { kind: 'plain', text: 'Dopo' }])
    expect(s('**For you:**\n\n- a')).toEqual([{ kind: 'you', title: 'For you', body: '- a' }])
  })
  it('chat_etichette: Da te senza elenco o a metà frase resta', () => {
    for (const t of ['Da te\nNiente elenco', 'Aspetta da te\n- a', 'Da te ci vuole poco\n- a', 'da te\n- a', 'Da te']) expect(s(t), t).toEqual(plain(t))
  })
  it('chat_etichette: Non provato', () => {
    for (const l of ['**Non provato:** il gesto', '**Non provato**: il gesto', '**Non provato** il gesto', 'Non provato: il gesto', '**Not tested:** x', 'Not tested: x', 'Non provato']) expect(s(l), l).toEqual([{ kind: 'untested', text: l }])
    expect(s('A\n\n**Non provato:**\n- uno\n- due\n\nB')).toEqual([{ kind: 'plain', text: 'A' }, { kind: 'untested', text: '**Non provato:**\n- uno\n- due' }, { kind: 'plain', text: 'B' }])
  })
  it('chat_etichette: Non provato a metà frase resta', () => {
    for (const t of ['Non provato il gesto', 'Il gesto è Non provato: dopo', 'non provato: x', '**Non provato il gesto**']) expect(s(t), t).toEqual(plain(t))
  })
  it('chat_etichette: senza etichette identico, nel codice no', () => {
    const t = '\n  Testo **vero**\n\n\n- a\n'
    expect(s(t)).toEqual(plain(t))
    const c = '```\nDa te\n- a\nNon provato: x\n```'
    expect(s(c)).toEqual(plain(c))
  })
})
const names = new Map([['app-a-01', 'Logo'], ['app-a-01-11', 'Slot']])
describe('chat_nomi', () => {
  it('chat_nomi: codice esatto diventa il nome', () => {
    expect(replaceNames('fatto app-a-01 e app-a-01-11.', names)).toBe(`fatto [Logo](${u}app-a-01) e [Slot](${u}app-a-01-11).`)
    expect(replaceNames('`app-a-01`', names)).toBe(`[Logo](${u}app-a-01)`)
  })
  it('chat_nomi: codice sconosciuto o parziale resta', () => {
    for (const t of ['app-a-01-12', 'xapp-a-01', 'app-a', 'app-a-01_b']) expect(replaceNames(t, names), t).toBe(t)
    expect(replaceNames('app-a-01', new Map())).toBe('app-a-01')
  })
  it('chat_nomi: codice, link e blocchi non si toccano', () => {
    for (const t of ['```\napp-a-01\n```', '`git log app-a-01`', '[app-a-01](https://x.example/a)', 'https://x.example/app-a-01']) expect(replaceNames(t, names), t).toBe(t)
  })
  it('chat_nomi: dallo stato solo con nome breve', () => {
    expect([...taskNames([{ task: 'a', short: 'Logo' }, { task: 'b' }, { task: 'd', short: 'd' }, { task: 'e', short: ' ' }])]).toEqual([['a', 'Logo']])
  })
})
