// app-visore-web-script-28-11 (Franz 28/11 16:35, «Ok a tutte e due»): nel visore HTML della web gli script delle pagine non giravano,
// perché la CSP dell'app (script-src 'self') vale anche nell'iframe srcdoc. Le pagine ora girano sul sito delle anteprime, un'origine
// a parte con una CSP sua (web/firebase.json, site supervisor-anteprime; la pagina è web/anteprime/index.html): l'app incornicia quel
// sito, aspetta «pronta» e gli manda l'HTML, solo a quell'origine. Se il sito non risponde, il riquadro torna allo srcdoc di prima.
import { CANONICAL, FIREBASE_HOSTS } from './remoteAccess'

/** Il sito delle anteprime. In sviluppo vuoto (srcdoc come prima), salvo VITE_ANTEPRIME. */
export const PREVIEW_ORIGIN: string = (import.meta.env.VITE_ANTEPRIME as string | undefined) ?? (import.meta.env.DEV ? '' : 'https://supervisor-anteprime.web.app')

/** Le origini dell'app che possono incorniciare il sito delle anteprime e mandargli pagine: le stesse di frame-ancestors. */
export const APP_ORIGINS = [CANONICAL, ...FIREBASE_HOSTS.map((h) => `https://${h}`)]

/**
 * Il riquadro sull'altra origine. Gli script girano; niente navigazione della pagina dell'app, finestre o moduli. allow-same-origin dà al
 * riquadro l'origine delle anteprime, diversa da quella dell'app: non legge token, cookie o storage dell'app, e l'app gli manda l'HTML
 * solo a quell'origine.
 */
export const PREVIEW_SANDBOX = 'allow-scripts allow-same-origin'

/** Quanto si aspetta «pronta» prima di tornare allo srcdoc (sito giù, bloccato o non ancora pubblicato). */
export const PREVIEW_WAIT_MS = 4000

/** Il messaggio del sito delle anteprime pronto a ricevere la pagina: dalla sua origine, col suo segnale. */
export const isReady = (e: { origin: string; data: unknown }, origin: string = PREVIEW_ORIGIN): boolean =>
  !!origin && e.origin === origin && (e.data as { anteprima?: unknown } | null)?.anteprima === 'pronta'
