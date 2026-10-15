// Quello che il service worker mostra di una push: sta da solo perché sw.ts non divida codice con l'app (un service
// worker classico non può importare moduli).

export type Payload = {
  kind?: string; title?: string; body?: string; session?: string | null; key?: string
  /** Contratto 1.56: titolo e corpo senza segreti, quando il relay ha nascosto qualcosa. */
  preview?: { title?: string; body?: string } | null
  /** Contratto 1.62: Franz è al PC su quella sessione, senza suono né vibrazione. */
  quiet?: string | boolean
} | null

/** Quello che il service worker mostra: il tocco apre la sessione (`/#<nome>`, come i link della web app). */
/** Contratto 1.68: le interruzioni con l'avviso forte (copia di STRONG in strongAlert.ts, il service worker sta da solo). */
const STRONG = ['question', 'needs_you', 'turn_blocking', 'turn_failed', 'quota_limit', 'attention_reminder']

export function notificationOf(p: Payload): { title: string; body: string; tag: string; url: string; silent: boolean; strong: boolean } {
  return {
    silent: p?.quiet === true || p?.quiet === '1',
    strong: STRONG.includes(p?.kind ?? ''),
    title: p?.preview?.title || p?.title || 'Supervisor',
    body: p?.preview?.body ?? p?.body ?? '',
    tag: p?.key || 'supervisor',
    url: p?.session ? `/#${encodeURIComponent(p.session)}` : '/',
  }
}
