import { mount } from 'svelte'
import './app.css'
import './v3.css'
import App from './App.svelte'
import { canonicalUrl, loadRemote } from './lib/remoteAccess'
import { lang } from './lib/lang'
import { dockWanted } from './lib/dockEmbed'
import { installDialogGrow } from './lib/motion'
import { initTheme } from './lib/theme'
import { anchorOk, viewportVars } from './lib/viewport'

installDialogGrow()

// L'altezza dell'area visibile davvero (visualViewport): la usa #app in app.css. Nei browser interni delle app (X,
// Telegram, Instagram) la barra in basso non è tolta da 100vh né sempre da 100dvh.
// Su iPhone (app-ios-scatti-tastiera-12-11) anche lo scostamento dall'alto: con la tastiera Safari fa scorrere la pagina, e #app
// segue l'area visibile invece di restare a metà schermo; a tastiera aperta `html.kb` toglie il margine sicuro in basso.
const vv = window.visualViewport
const root = document.documentElement
const fit = () => {
  const { vvh, vvt, kb } = viewportVars(vv, innerHeight)
  root.style.setProperty('--vvh', `${vvh}px`); root.style.setProperty('--vvt', `${vvt}px`); root.classList.toggle('kb', kb)
}
fit()
vv?.addEventListener('resize', fit)
vv?.addEventListener('scroll', fit)
addEventListener('resize', fit)
// Le voci fuori schermo si saltano (content-visibility) solo dove il browser tiene fermo il punto di lettura: Safari prima della 27 no.
if (anchorOk(globalThis.CSS?.supports?.bind(CSS))) root.classList.add('anchor')

// La lingua scelta all'avvio (lang.ts) anche per il browser: lettori di schermo, traduzione, sillabazione.
document.documentElement.lang = lang
// Tutta l'app nello stile v3 (Franz 30/10 21:43); `/embed` resta di notte come Studio che lo ospita.
document.documentElement.classList.add('v3')
if (/^\/embed(\.html)?$/.test(location.pathname)) document.documentElement.classList.add('night-only')
// Il tema scelto, e Automatico che segue il sistema (app-tema-giorno-notte-11-11).
initTheme()

// Dai nomi di Firebase del sito al dominio, prima di disegnare la pagina (un browser già collegato lì resta lì).
const store = { get: (k: string) => { try { return localStorage.getItem(k) } catch { return null } }, set() {}, remove() {} }
// `/embed` resta dov'è: Studio riconosce i messaggi del riquadro dall'origine con cui l'ha aperto.
const embedded = /^\/embed(\.html)?$/.test(location.pathname)
const canonical = embedded ? null : canonicalUrl(new URL(location.href), loadRemote(store) != null)
// `/embed?dock=1` è trasparente sopra Studio: niente fondo su html e body (app.css).
if (dockWanted(location.pathname, location.search)) document.documentElement.classList.add('dock')
if (canonical) location.replace(canonical)

const app = canonical ? undefined : mount(App, {
  target: document.getElementById('app')!,
})

// Installabile come app (PWA): il service worker solo nella build, in sviluppo ricaricherebbe file vecchi.
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => { navigator.serviceWorker.register('/sw.js').catch(() => { /* senza, l'app va lo stesso */ }) })
}

export default app
