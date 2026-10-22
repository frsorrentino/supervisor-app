// Niente emoji nell'app e nelle notifiche (Franz, 22/10 13:36: «solo svg o fontawesome»), porta di Emoji.kt: i testi che
// arrivano dal relay (titoli e corpi degli eventi, le push) passano da qui prima di essere mostrati. Vanno via i pittogrammi,
// i simboli e i dingbat (✓ ✗ ❓ ⚠ ⛔), gli orologi e i comandi (⏸ ⏺), le forme con presentazione emoji (▶ ◀ ◾), i selettori di
// variante, lo ZWJ, i keycap e i tag delle bandiere; restano lettere, numeri, frecce («→» «↳»), «·» e la punteggiatura.
// Gli spazi intorno a un simbolo tolto si chiudono, i «·» rimasti ai bordi cadono.

const DROP = /^[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{2B00}-\u{2BFF}\u{231A}\u{231B}\u{23E9}-\u{23F3}\u{23F8}-\u{23FA}\u{25AA}\u{25AB}\u{25B6}\u{25C0}\u{25FB}-\u{25FE}\u{203C}\u{2049}\u{2139}\u{24C2}\u{2934}\u{2935}\u{3030}\u{303D}\u{3297}\u{3299}\u{FE0E}\u{FE0F}\u{200D}\u{20E3}\u{E0020}-\u{E007F}]$/u
const CLOSERS = ',.;:!?)'

export function stripEmoji(text: string): string {
  if (!/[ -￿]/.test(text)) return text
  let out = ''
  let dropped = false
  for (const ch of text) {
    if (DROP.test(ch)) { dropped = true; continue }
    if (dropped) {
      dropped = false
      if (ch === ' ' && (out === '' || out.endsWith(' '))) continue
      if (CLOSERS.includes(ch) && out.endsWith(' ')) out = out.slice(0, -1)
    }
    out += ch
  }
  return out.trim().replace(/^[· ]+|[· ]+$/g, '')
}
