/// <reference lib="webworker" />
// Il service worker della web app: apre subito anche senza rete, e non tiene mai in cache i dati. Mostra le notifiche
// web push (fase 4) e al tocco apre la sessione. Vite lo costruisce a parte come /sw.js (vite.config.ts).
import { notificationOf, type Payload } from './lib/pushView'

declare const self: ServiceWorkerGlobalScope

// La pagina dalla rete se c'è (così un aggiornamento arriva al primo avvio), dalla cache se no; i file con l'impronta nel
// nome (/assets/…) dalla cache, perché non cambiano mai; l'API locale del relay e tutto il resto sempre dalla rete.
const CACHE = 'cm-shell-v1'
const SHELL = ['/', '/manifest.webmanifest', '/icon.svg', '/icon-192.png']

self.addEventListener('install', (e: ExtendableEvent) => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()))
})

self.addEventListener('activate', (e: ExtendableEvent) => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()))
})

self.addEventListener('fetch', (e: FetchEvent) => {
  const req = e.request
  const url = new URL(req.url)
  if (req.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return
  // Solo la pagina dell'app va nella cache come '/': le altre (es. /privacy) passano dalla rete.
  if (req.mode === 'navigate' && url.pathname === '/') {
    e.respondWith(fetch(req).then(res => { const copy = res.clone(); caches.open(CACHE).then(c => c.put('/', copy)); return res }).catch(async () => (await caches.match('/')) ?? Response.error()))
    return
  }
  if (url.pathname.startsWith('/assets/')) {
    e.respondWith(caches.match(req).then(hit => hit ?? fetch(req).then(res => { const copy = res.clone(); caches.open(CACHE).then(c => c.put(req, copy)); return res })))
    return
  }
  e.respondWith(fetch(req).catch(async () => (await caches.match(req)) ?? Response.error()))
})

// Su iPhone ogni push deve mostrare una notifica, altrimenti Safari toglie l'iscrizione.
self.addEventListener('push', (e: PushEvent) => {
  let p: Payload = null
  try { p = e.data?.json() ?? null } catch { p = { body: e.data?.text() ?? '' } }
  const n = notificationOf(p)
  // Contratto 1.68: un'interruzione resta finché non la tocchi e vibra come telefono e orologio (dove il browser lo fa).
  const strong = n.strong && !n.silent ? { requireInteraction: true, renotify: true, vibrate: [220, 120, 220] } : {}
  e.waitUntil(self.registration.showNotification(n.title, { body: n.body, tag: n.tag, silent: n.silent, icon: '/icon-192.png', badge: '/icon-192.png', data: { url: n.url }, ...strong } as NotificationOptions))
})

self.addEventListener('notificationclick', (e: NotificationEvent) => {
  e.notification.close()
  const url = new URL((e.notification.data as { url?: string } | null)?.url ?? '/', self.location.origin).href
  e.waitUntil((async () => {
    const wins = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    const open = wins[0]
    // L'app legge `#nome` solo all'avvio: una query nuova fa ricaricare la pagina invece di cambiare solo il frammento.
    if (open) {
      const fresh = new URL(url)
      fresh.searchParams.set('n', String(Date.now()))
      await open.focus()
      await open.navigate(fresh.href).catch(() => {})
      return
    }
    await self.clients.openWindow(url)
  })())
})
