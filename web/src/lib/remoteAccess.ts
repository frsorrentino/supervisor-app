// Il browser accoppiato (fase 3): database, chiave Firebase, chiave del relay e accesso anonimo, salvati in localStorage
// dell'origine della web app. Su iPhone e iPad l'app aggiunta alla schermata Home ha un suo spazio, separato da Safari:
// per questo si accoppia dall'app installata.

import { AnonAuth, RestBus, type Bus, type Session } from './bus'
import { fromHex, generatePair, toHex } from './crypto'
import { pairAdd, type Invite } from './pairing'
import { RemoteTransport } from './remote'

type Store = { get(k: string): string | null | undefined; set(k: string, v: string): void; remove(k: string): void }
type Bytes = Uint8Array<ArrayBuffer>
export type RemoteConfig = { db: string; apiKey: string; host: string; uid: string; key: Bytes; session: Session | null }

const KEY = 'cm.remote'

export function loadRemote(store: Store): RemoteConfig | null {
  try {
    const o = JSON.parse(store.get(KEY) ?? '')
    if (typeof o.db !== 'string' || typeof o.apiKey !== 'string' || typeof o.uid !== 'string' || !/^[0-9a-f]{64}$/i.test(o.key)) return null
    return { db: o.db, apiKey: o.apiKey, host: String(o.host ?? ''), uid: o.uid, key: fromHex(o.key) as Bytes, session: o.session ?? null }
  } catch { return null }
}

function saveRemote(store: Store, c: RemoteConfig) {
  store.set(KEY, JSON.stringify({ ...c, key: toHex(c.key) }))
}

export const forget = (store: Store) => store.remove(KEY)

/** Il collegamento da usare: nessuno con `?demo`, che mostra solo i dati di prova anche su un browser accoppiato. */
export function remoteFor(url: URL, store: Store): RemoteConfig | null {
  return url.searchParams.has('demo') ? null : loadRemote(store)
}

/** La pagina del relay (strada locale) e il server di sviluppo non si accoppiano; `?demo` mostra i dati di prova. */
export function wantsPairing(url: URL, remote: RemoteConfig | null): boolean {
  if (remote) return false
  if (url.hostname === '127.0.0.1' || url.hostname === 'localhost') return false
  return !url.searchParams.has('demo')
}

/** Il nome del browser nella lista dei dispositivi del PC, come «Safari su iPhone». */
export function deviceName(ua: string): string {
  const os = /iPhone/.test(ua) ? 'iPhone' : /iPad/.test(ua) ? 'iPad' : /Android/.test(ua) ? 'Android'
    : /Macintosh/.test(ua) ? 'Mac' : /Windows/.test(ua) ? 'Windows' : /CrOS/.test(ua) ? 'Chromebook' : /Linux/.test(ua) ? 'Linux' : null
  const browser = /Edg\//.test(ua) ? 'Edge' : /Firefox\/|FxiOS/.test(ua) ? 'Firefox' : /Chrome\/|CriOS/.test(ua) ? 'Chrome'
    : /Safari\//.test(ua) ? 'Safari' : null
  if (!os || !browser) return 'Browser'
  return `${browser} su ${os}`
}

type Deps = {
  bus: (db: string, auth: () => Promise<string>) => Bus
  signIn: (apiKey: string) => Promise<Session>
  keyPair: () => Promise<CryptoKeyPair>
  name: string
  now?: () => number
  pollMs?: number
}

/** Accede a Firebase come utente anonimo, si accoppia con il suo uid e salva tutto solo se il PC ha confermato. */
export async function pairBrowser(q: Invite, store: Store, d: Deps): Promise<{ host: string }> {
  const session = await d.signIn(q.f.k)
  const bus = d.bus(q.f.d, async () => session.idToken)
  const r = await pairAdd(bus, q, { uid: session.uid, name: d.name, keyPair: await d.keyPair(), now: d.now, pollMs: d.pollMs })
  saveRemote(store, { db: q.f.d, apiKey: q.f.k, host: r.host, uid: session.uid, key: r.key, session })
  return { host: r.host }
}

/** Le dipendenze vere, per la pagina di accoppiamento. */
export const browserDeps = (name: string): Deps => ({
  bus: (db, auth) => new RestBus(db, auth),
  signIn: async apiKey => {
    let s: Session | null = null
    const auth = new AnonAuth(apiKey, null, x => (s = x))
    await auth.token()
    return s!
  },
  keyPair: generatePair,
  name,
})

/** Il trasporto remoto del browser accoppiato; il rinnovo del token aggiorna la sessione salvata. */
export function remoteTransport(c: RemoteConfig, store: Store): RemoteTransport {
  const auth = new AnonAuth(c.apiKey, c.session, s => saveRemote(store, { ...c, session: s }))
  return new RemoteTransport(new RestBus(c.db, () => auth.token()), { key: c.key, uid: c.uid })
}

/** Safari su iPhone o iPad (anche l'iPad che si presenta come Mac, riconosciuto dal tocco) fuori dall'app della Home. */
export function needsHomeScreen(ua: string, standalone: boolean, touchPoints: number): boolean {
  if (standalone) return false
  return /iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua) && touchPoints > 1)
}

/** L'indirizzo pubblico della web app (Franz, 13/10 12:33); i nomi di Firebase del sito rinviano qui. */
export const CANONICAL = 'https://supervisor.francescosorrentino.com'
const FIREBASE_HOSTS = ['supervisor-francescosorrentino.web.app', 'supervisor-francescosorrentino.firebaseapp.com']

/**
 * Dove rinviare un browser arrivato su un nome di Firebase del sito, o null. Chi è già collegato lì resta: su iPhone
 * l'app della Home, la chiave e le notifiche sono legate all'indirizzo, e un rinvio le perderebbe.
 */
export function canonicalUrl(url: URL, paired: boolean): string | null {
  if (paired || !FIREBASE_HOSTS.includes(url.hostname)) return null
  return CANONICAL + url.pathname + url.search + url.hash
}
