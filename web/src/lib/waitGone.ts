// Una domanda che non aspetta più (app-domanda-chiusa-25-11, Franz 25/11 16:55): chiusa, già risposta altrove o sostituita da una carta
// nuova. Il relay lo dice con `ok: false` e «<id>: this question is no longer waiting (closed or already answered)» (contratto 1.130),
// in italiano «questa domanda non aspetta più». Non è un errore: la carta sparisce e il link apre «Già risolta», senza l'id; gli errori
// veri restano errori. Le stesse regole di core/.../rules/WaitGone.kt.
const GONE = /no longer waiting|non aspetta più/i

export const waitGone = (text: string | null | undefined): boolean => GONE.test(text ?? '')
