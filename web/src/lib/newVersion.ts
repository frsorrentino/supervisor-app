// La versione nuova (app-web-versione-nuova-11-11, Franz 11/11 18:10): una scheda aperta da prima di un rilascio restava col codice
// vecchio finché non la si ricaricava (l'11/11 due ore a 368 comandi l'ora dopo l'uscita delle letture leggere). Al ritorno in primo
// piano e ogni 30 minuti, anche con la scheda sempre nascosta, si legge /version.json (un file statico accanto alla pagina, non un
// comando al PC). Se il commit pubblicato è un altro: senza niente in corso la pagina si ricarica da sola, altrimenti la riga
// «Versione nuova, tocca per aggiornare». Il service worker prende la pagina dalla rete, quindi la ricarica porta la versione nuova.

export type Published = { version: string; commit: string }
export const CHECK_MS = 30 * 60_000

/** Un altro commit pubblicato; un file che non c'è o non si legge non dice niente. */
export const isNewer = (mine: string, pub: Published | null): boolean => !!mine && !!pub?.commit && pub.commit !== mine

/** Cosa fare con una versione nuova: ricaricare se non c'è niente in corso, altrimenti offrire il tocco. */
export const decide = (newer: boolean, busy: boolean): 'stay' | 'reload' | 'offer' => (!newer ? 'stay' : busy ? 'offer' : 'reload')

/** Testo scritto a metà in un campo della pagina: una ricarica lo perderebbe. */
export function draftOpen(doc: Document = document): boolean {
  return [...doc.querySelectorAll<HTMLTextAreaElement | HTMLInputElement>('textarea, input[type="text"], input[type="search"], input:not([type])')]
    .some(f => f.value.trim().length > 0)
}

type Opts = {
  mine: string
  fetchPublished: () => Promise<Published | null>
  busy: () => boolean
  reload: () => void
  offer: () => void
  onVisible?: (f: () => void) => void
  every?: (f: () => void, ms: number) => void
}

const whenVisible = (f: () => void) => document.addEventListener('visibilitychange', () => { if (!document.hidden) f() })
const everyMs = (f: () => void, ms: number) => { setInterval(f, ms) }

/** Il controllo: al ritorno in primo piano e ogni CHECK_MS, anche nascosta. Torna la funzione che controlla subito (per i test). */
export function watchVersion(o: Opts): () => Promise<void> {
  let checking = false
  const check = async () => {
    if (checking) return
    checking = true
    try {
      const pub = await o.fetchPublished().catch(() => null)
      const what = decide(isNewer(o.mine, pub), o.busy())
      if (what === 'reload') o.reload()
      else if (what === 'offer') o.offer()
    } finally { checking = false }
  }
  ;(o.onVisible ?? whenVisible)(() => void check())
  ;(o.every ?? everyMs)(() => void check(), CHECK_MS)
  return check
}

/** Il file pubblicato, mai dalla cache. */
export async function fetchPublished(): Promise<Published | null> {
  const r = await fetch('/version.json', { cache: 'no-store' })
  if (!r.ok) return null
  const j = await r.json() as Partial<Published>
  return typeof j.commit === 'string' ? { version: String(j.version ?? ''), commit: j.commit } : null
}

/** Prima di ricaricare si chiede anche il service worker nuovo, poi la pagina dalla rete. */
export async function reloadFresh(): Promise<void> {
  try { await (await navigator.serviceWorker?.getRegistration())?.update() } catch { /* la pagina arriva lo stesso dalla rete */ }
  location.reload()
}
