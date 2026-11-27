// La risposta divisa in blocchi da leggere e da ascoltare uno per volta, porta di AnswerText.kt (app-web-parola-lettura-27-11; Franz,
// 15/09 17:19: «non solo sentire tutto ma anche sentire per blocchi/paragrafi»). Paragrafi separati da una riga vuota, ogni voce d'elenco
// un blocco, titoli markdown o righe tutte in grassetto come titoli, blocchi ``` come codice; un blocco ```text è testo da copiare
// (`text`), che la voce legge. Il grassetto si toglie, i `backtick` restano.

export type AnswerKind = 'para' | 'bullet' | 'heading' | 'code' | 'text'
export type AnswerBlock = { kind: AnswerKind; text: string }

const TESTO = ['text', 'txt', 'plain', 'plaintext']
const isCopy = (fence: string) => TESTO.includes(fence.trim().replace(/^```/, '').trim().toLowerCase())
const TITOLO = /^#{1,6}\s+(.+)$/
const GRASSETTO = /^\*\*(.+?)\*\*:?$/
const VOCE = /^\s*(?:[-*•]|\d+[.)])\s+(.+)$/
const pulito = (t: string) => t.replace(/\*\*|__/g, '').trim()

export function answerBlocks(text: string): AnswerBlock[] {
  const out: AnswerBlock[] = []
  let para = ''
  let code: string | null = null
  let codeKind: AnswerKind = 'code'
  let afterBullet = false
  const flush = () => { if (para.trim()) out.push({ kind: 'para', text: pulito(para) }); para = '' }
  for (const raw of text.split(/\r\n|\r|\n/)) {
    const line = raw.replace(/\s+$/, '')
    if (code != null) {
      if (line.trimStart().startsWith('```')) { out.push({ kind: codeKind, text: code.replace(/\n+$/, '') }); code = null }
      else code += line + '\n'
      continue
    }
    const t = line.trim()
    const voce = VOCE.exec(line)
    const titolo = TITOLO.exec(t)
    const grassetto = GRASSETTO.exec(t)
    if (t.startsWith('```')) { flush(); code = ''; codeKind = isCopy(t) ? 'text' : 'code'; afterBullet = false }
    else if (!t) { flush(); afterBullet = false }
    else if (titolo) { flush(); out.push({ kind: 'heading', text: pulito(titolo[1]) }); afterBullet = false }
    else if (grassetto) { flush(); out.push({ kind: 'heading', text: pulito(grassetto[1]) }); afterBullet = false }
    else if (voce) { flush(); out.push({ kind: 'bullet', text: pulito(voce[1]) }); afterBullet = true }
    // Una riga rientrata sotto una voce d'elenco la continua.
    else if (afterBullet && raw.startsWith(' ')) { const b = out.pop()!; out.push({ ...b, text: `${b.text} ${pulito(t)}` }) }
    else { if (para) para += ' '; para += t; afterBullet = false }
  }
  if (code != null && code.trim()) out.push({ kind: codeKind, text: code.replace(/\n+$/, '') })
  flush()
  return out.filter(b => b.text.trim() !== '')
}
