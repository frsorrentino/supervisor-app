import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { MemoryBus } from './bus'
import { privateFromScalar, toHex } from './crypto'
import { parseInvite } from './pairing'
import { canonicalUrl, deviceName, forget, loadRemote, needsHomeScreen, pairBrowser, wantsPairing } from './remoteAccess'

const fixture = (n: string) => JSON.parse(readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8'))
const range = (a: number, b: number) => Uint8Array.from({ length: b - a }, (_, i) => a + i)
const memory = () => { const m = new Map<string, string>(); return { m, get: (k: string) => m.get(k) ?? null, set: (k: string, v: string) => void m.set(k, v), remove: (k: string) => void m.delete(k) } }

describe('il nome del browser fra i dispositivi del PC', () => {
  it('browser e sistema dallo user agent', () => {
    expect(deviceName('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1')).toBe('Safari su iPhone')
    expect(deviceName('Mozilla/5.0 (iPad; CPU OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1')).toBe('Safari su iPad')
    expect(deviceName('Mozilla/5.0 (Linux; Android 16; Pixel 11 Pro XL) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36')).toBe('Chrome su Android')
    expect(deviceName('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15')).toBe('Safari su Mac')
    expect(deviceName('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0')).toBe('Edge su Windows')
    expect(deviceName('Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0')).toBe('Firefox su Linux')
    expect(deviceName('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/141.0 Mobile/15E148 Safari/604.1')).toBe('Chrome su iPhone')
    expect(deviceName('qualcosa')).toBe('Browser')
  })
})

describe('quando la web app chiede di accoppiarsi', () => {
  it('sulla pagina del relay o sul server di sviluppo no (strada locale o demo); altrove sì, finché non è accoppiata', () => {
    expect(wantsPairing(new URL('http://127.0.0.1:8765/'), null)).toBe(false)
    expect(wantsPairing(new URL('http://localhost:5173/'), null)).toBe(false)
    expect(wantsPairing(new URL('https://supervisor-app.web.app/'), null)).toBe(true)
    expect(wantsPairing(new URL('https://supervisor-app.web.app/?demo'), null)).toBe(false)
  })
})

describe('accoppiamento del browser, dall\'invito alla configurazione salvata', () => {
  const f = fixture('pair-add.json')
  const q = parseInvite(JSON.stringify(f.qr))!

  it('accede, si accoppia con il suo uid e salva database, chiave e sessione; forget toglie tutto', async () => {
    const store = memory()
    const bus = new MemoryBus({ [`pair/${f.qr.i}`]: { pc_pub: f.qr.c, host: f.qr.h, exp: f.qr.e, mode: 'add' } })
    bus.onPut = path => { if (path.endsWith('/watch')) bus.data[`pair/${f.qr.i}/ok`] = f.ok }
    const session = { uid: f.watch.uid, refresh: 'r', idToken: 't', expires: 9e15 }
    const r = await pairBrowser(q, store, {
      bus: () => bus, signIn: async () => session, keyPair: () => privateFromScalar(range(64, 96)),
      name: 'Safari su iPhone', now: () => f.qr.e - 60, pollMs: 1,
    })
    expect(r.host).toBe('penguin')
    expect((bus.data[`pair/${f.qr.i}/watch`] as { uid: string }).uid).toBe(f.watch.uid)
    const cfg = loadRemote(store)!
    expect(cfg).toMatchObject({ db: f.qr.f.d, apiKey: f.qr.f.k, host: 'penguin', uid: f.watch.uid })
    expect(toHex(cfg.key)).toBe(f.relay_key)
    expect(cfg.session?.refresh).toBe('r')
    forget(store)
    expect(loadRemote(store)).toBeNull()
  })

  it('un accoppiamento fallito non salva niente', async () => {
    const store = memory()
    await expect(pairBrowser(q, store, {
      bus: () => new MemoryBus({}), signIn: async () => ({ uid: 'u', refresh: 'r', idToken: 't', expires: 9e15 }),
      keyPair: () => privateFromScalar(range(64, 96)), name: 'n', now: () => f.qr.e - 60, pollMs: 1,
    })).rejects.toThrow('invalid')
    expect(loadRemote(store)).toBeNull()
  })

  it('una configurazione rotta si ignora', () => {
    const store = memory()
    store.set('cm.remote', '{"db":1}')
    expect(loadRemote(store)).toBeNull()
  })
})

describe('su iPhone e iPad prima la schermata Home', () => {
  const iphone = 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1'
  const ipadAsMac = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15'
  it('in Safari sì, nell\'app della Home no; un iPad che si presenta come Mac si riconosce dal tocco', () => {
    expect(needsHomeScreen(iphone, false, 5)).toBe(true)
    expect(needsHomeScreen(iphone, true, 5)).toBe(false)
    expect(needsHomeScreen(ipadAsMac, false, 5)).toBe(true)
    expect(needsHomeScreen(ipadAsMac, false, 0)).toBe(false)
    expect(needsHomeScreen('Mozilla/5.0 (Linux; Android 16) Chrome/141.0.0.0 Mobile Safari/537.36', false, 5)).toBe(false)
  })
})

describe('rinvio dagli indirizzi di Firebase al dominio', () => {
  const to = (u: string, paired = false) => canonicalUrl(new URL(u), paired)
  it('da .web.app e .firebaseapp.com al dominio, con percorso, query e invito', () => {
    expect(to('https://supervisor-francescosorrentino.web.app/')).toBe('https://supervisor.francescosorrentino.com/')
    expect(to('https://supervisor-francescosorrentino.firebaseapp.com/?demo#pair=abc')).toBe('https://supervisor.francescosorrentino.com/?demo#pair=abc')
  })
  it('un browser già collegato lì resta lì: la chiave è legata all\'indirizzo', () => {
    expect(to('https://supervisor-francescosorrentino.web.app/', true)).toBeNull()
  })
  it('sul dominio, sul relay e sul server di sviluppo nessun rinvio', () => {
    expect(to('https://supervisor.francescosorrentino.com/')).toBeNull()
    expect(to('http://127.0.0.1:8765/')).toBeNull()
    expect(to('http://localhost:5173/')).toBeNull()
  })
})
