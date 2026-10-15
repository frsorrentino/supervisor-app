import { describe, expect, it } from 'vitest'
import { chromeIntent, inApp } from './inApp'

describe('il browser interno di un\'app', () => {
  it('riconosce X, Instagram, Facebook, LinkedIn, Telegram e la WebView Android', () => {
    expect(inApp('Mozilla/5.0 (Linux; Android 16; Pixel 9; wv) AppleWebKit/537.36 Chrome/141 Mobile Safari/537.36 TwitterAndroid')).toEqual({ app: 'X', android: true, ios: false })
    expect(inApp('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Twitter for iPhone/10.0')?.app).toBe('X')
    expect(inApp('Mozilla/5.0 (iPhone) Instagram 300.0')?.app).toBe('Instagram')
    expect(inApp('Mozilla/5.0 (iPhone) [FBAN/FBIOS;FBAV/400.0]')?.app).toBe('Facebook')
    expect(inApp('Mozilla/5.0 (Linux; Android 15) LinkedInApp')?.app).toBe('LinkedIn')
    expect(inApp('Mozilla/5.0 (Linux; Android 15; K; wv) Telegram-Android/11.0')?.app).toBe('Telegram')
    expect(inApp('Mozilla/5.0 (Linux; Android 15; K; wv) AppleWebKit/537.36 Chrome/141 Mobile Safari/537.36')).toEqual({ app: '', android: true, ios: false })
  })
  it('Chrome e Safari veri no', () => {
    expect(inApp('Mozilla/5.0 (Linux; Android 16; Pixel 9) AppleWebKit/537.36 Chrome/141 Mobile Safari/537.36')).toBeNull()
    expect(inApp('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1')).toBeNull()
  })
  it('il link intent porta la stessa pagina a Chrome, senza la parte dopo #', () => {
    expect(chromeIntent(new URL('https://supervisor.francescosorrentino.com/?demo#storefront')))
      .toBe('intent://supervisor.francescosorrentino.com/?demo#Intent;scheme=https;package=com.android.chrome;S.browser_fallback_url=https%3A%2F%2Fsupervisor.francescosorrentino.com%2F%3Fdemo%23storefront;end')
  })
})
