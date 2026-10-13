// Notifiche web push (fase 4 del piano web): il relay pubblica la sua chiave VAPID nello stato, il browser si iscrive e
// scrive l'iscrizione cifrata in /push/<uid>; il relay manda la push accanto a quella FCM del telefono. Su iPhone e iPad
// le push arrivano solo all'app aggiunta alla schermata Home, e il permesso si chiede solo da un tocco.

import { fromB64 } from './crypto'

export type PushDoc = { endpoint: string; p256dh: string; auth: string }
export type PushStatus = 'hidden' | 'relay' | 'unsupported' | 'home' | 'denied' | 'off' | 'on'

export function pushStatus(o: {
  remote: boolean; vapid: string | null; supported: boolean; homeFirst: boolean; permission: NotificationPermission; subscribed: boolean
}): PushStatus {
  if (!o.remote) return 'hidden'
  if (!o.vapid) return 'relay'
  if (o.homeFirst) return 'home'
  if (!o.supported) return 'unsupported'
  if (o.permission === 'denied') return 'denied'
  return o.permission === 'granted' && o.subscribed ? 'on' : 'off'
}

/** La chiave pubblica VAPID in base64url (il punto P-256 non compresso, 65 byte) per `applicationServerKey`. */
export function vapidBytes(b64url: string): Uint8Array<ArrayBuffer> {
  const b = b64url.replace(/-/g, '+').replace(/_/g, '/')
  return fromB64(b + '='.repeat((4 - b.length % 4) % 4))
}

export function subscriptionDoc(s: PushSubscriptionJSON): PushDoc | null {
  const p256dh = s.keys?.p256dh, auth = s.keys?.auth
  if (!s.endpoint || !p256dh || !auth) return null
  return { endpoint: s.endpoint, p256dh, auth }
}

/** Chiede il permesso (da un tocco), si iscrive con la chiave del relay e restituisce l'iscrizione da salvare. */
export async function subscribe(vapid: string): Promise<PushDoc | null> {
  if (await Notification.requestPermission() !== 'granted') return null
  const reg = await navigator.serviceWorker.ready
  const old = await reg.pushManager.getSubscription()
  const key = vapidBytes(vapid)
  // Una chiave del relay cambiata vuol dire un'iscrizione nuova: quella vecchia non riceverebbe più niente.
  if (old) {
    const k = old.options.applicationServerKey
    const same = k && new Uint8Array(k).every((x, i) => x === key[i])
    if (same) return subscriptionDoc(old.toJSON())
    await old.unsubscribe()
  }
  const sub = await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: key })
  return subscriptionDoc(sub.toJSON())
}

export async function currentSubscription(): Promise<PushSubscription | null> {
  if (!('serviceWorker' in navigator) || !('PushManager' in globalThis)) return null
  const reg = await navigator.serviceWorker.getRegistration()
  return (await reg?.pushManager.getSubscription()) ?? null
}
