import { mount } from 'svelte'
import './app.css'
import './v3.css'
import App from './App.svelte'
import { canonicalUrl, loadRemote } from './lib/remoteAccess'
import { lang } from './lib/lang'
import { dockWanted } from './lib/dockEmbed'

// L'altezza dell'area visibile davvero (visualViewport): la usa #app in app.css. Nei browser interni delle app (X,
// Telegram, Instagram) la barra in basso non è tolta da 100vh né sempre da 100dvh.
const vv = window.visualViewport
const fit = () => document.documentElement.style.setProperty('--vvh', `${Math.round(vv ? vv.height * vv.scale : innerHeight)}px`)
fit()
vv?.addEventListener('resize', fit)
addEventListener('resize', fit)

// La lingua scelta all'avvio (lang.ts) anche per il browser: lettori di schermo, traduzione, sillabazione.
document.documentElement.lang = lang

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
