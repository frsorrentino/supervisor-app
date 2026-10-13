import { mount } from 'svelte'
import './app.css'
import App from './App.svelte'
import { canonicalUrl, loadRemote } from './lib/remoteAccess'

// Dai nomi di Firebase del sito al dominio, prima di disegnare la pagina (un browser già collegato lì resta lì).
const store = { get: (k: string) => { try { return localStorage.getItem(k) } catch { return null } }, set() {}, remove() {} }
const canonical = canonicalUrl(new URL(location.href), loadRemote(store) != null)
if (canonical) location.replace(canonical)

const app = canonical ? undefined : mount(App, {
  target: document.getElementById('app')!,
})

// Installabile come app (PWA): il service worker solo nella build, in sviluppo ricaricherebbe file vecchi.
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => { navigator.serviceWorker.register('/sw.js').catch(() => { /* senza, l'app va lo stesso */ }) })
}

export default app
