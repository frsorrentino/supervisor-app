import { describe, expect, it } from 'vitest'
import { MemoryBus } from './bus'
import { fromHex, openBlob } from './crypto'
import { pushStatus, subscriptionDoc, vapidBytes } from './push'
import { notificationOf } from './pushView'
import { RemoteTransport } from './remote'

const KEY = fromHex('606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f')

describe('notifiche web push: quando si possono attivare', () => {
  const base = { remote: true, vapid: 'BAAA', supported: true, homeFirst: false, permission: 'default' as NotificationPermission, subscribed: false }
  it('serve la strada remota, un relay che le manda, il browser che le supporta e, su iPhone, l\'app della Home', () => {
    expect(pushStatus(base)).toBe('off')
    expect(pushStatus({ ...base, remote: false })).toBe('hidden')
    expect(pushStatus({ ...base, vapid: null })).toBe('relay')
    expect(pushStatus({ ...base, supported: false })).toBe('unsupported')
    expect(pushStatus({ ...base, homeFirst: true })).toBe('home')
    expect(pushStatus({ ...base, permission: 'denied' })).toBe('denied')
    expect(pushStatus({ ...base, permission: 'granted', subscribed: true })).toBe('on')
    expect(pushStatus({ ...base, permission: 'granted', subscribed: false })).toBe('off')
  })
})

describe('notifiche web push: i dati', () => {
  it('la chiave VAPID in base64url diventa i 65 byte del punto P-256', () => {
    const b64 = 'BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkrxZJjSgSnfckjBJuBkr3qBUYIHBQFLXYp5Nksh8U'
    const b = vapidBytes(b64)
    expect(b.length).toBe(65)
    expect(b[0]).toBe(4)
  })

  it('dell\'iscrizione restano endpoint e le due chiavi', () => {
    expect(subscriptionDoc({ endpoint: 'https://web.push.apple.com/abc', expirationTime: null, keys: { p256dh: 'P', auth: 'A' } }))
      .toEqual({ endpoint: 'https://web.push.apple.com/abc', p256dh: 'P', auth: 'A' })
    expect(subscriptionDoc({ endpoint: 'https://x' })).toBeNull()
  })

  it('il contenuto della push diventa titolo, testo e la sessione da aprire; uno sconosciuto ha un titolo comunque', () => {
    expect(notificationOf({ kind: 'question', title: '❓ ledger-api', body: 'Deploy now?', session: 'ledger-api', key: 'k1' }))
      .toEqual({ title: '❓ ledger-api', body: 'Deploy now?', tag: 'k1', url: '/#ledger-api', silent: false, strong: true })
    expect(notificationOf(null)).toEqual({ title: 'Supervisor', body: '', tag: 'supervisor', url: '/', silent: false, strong: false })
    // Contratto 1.68: ferma per te e promemoria con l'avviso forte, un esito no.
    expect(notificationOf({ kind: 'needs_you', title: '✋ kb' }).strong).toBe(true)
    expect(notificationOf({ kind: 'attention_reminder', title: '⏰' }).strong).toBe(true)
    expect(notificationOf({ kind: 'outcome', title: '✓ kb' }).strong).toBe(false)
  })

  it('il trasporto remoto scrive l\'iscrizione cifrata in /push/<uid> e la toglie', async () => {
    const bus = new MemoryBus({})
    const t = new RemoteTransport(bus, { key: KEY, uid: 'webUid' })
    await t.savePush({ endpoint: 'https://e', p256dh: 'P', auth: 'A' })
    expect(JSON.parse(await openBlob(JSON.stringify(bus.data['push/webUid']), KEY))).toEqual({ endpoint: 'https://e', p256dh: 'P', auth: 'A' })
    await t.savePush(null)
    expect(bus.deleted).toContain('push/webUid')
  })
})

describe('push senza segreti (contratto 1.56)', () => {
  it('la notifica usa preview quando c\'è', () => {
    const n = notificationOf({ title: '✓ x', body: 'token ghp_abc', preview: { title: '✓ x', body: 'token •••' }, key: 'k' })
    expect(n.body).toBe('token •••')
    expect(notificationOf({ title: 't', body: 'b' }).body).toBe('b')
    // Contratto 1.62: con Franz al PC la push arriva senza suono.
    expect(notificationOf({ title: 't', quiet: '1' }).silent).toBe(true)
    expect(notificationOf({ title: 't' }).silent).toBe(false)
  })
})
