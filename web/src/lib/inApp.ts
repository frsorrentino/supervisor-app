// Il browser interno di un'app (Franz, 15/10 21:09: il link della demo su X si apre dentro X, con una barra in basso che
// ostacola l'uso): quale app, se si capisce dallo user agent, e come uscirne.
export type InApp = { app: string; android: boolean; ios: boolean }

const APPS: [RegExp, string][] = [
  [/Twitter|TwitterAndroid/i, 'X'],
  [/Instagram/i, 'Instagram'],
  [/FBAN|FBAV|FB_IAB|FBIOS/i, 'Facebook'],
  [/LinkedInApp/i, 'LinkedIn'],
  [/Telegram/i, 'Telegram'],
]

export function inApp(ua: string): InApp | null {
  const android = /Android/i.test(ua), ios = /iPhone|iPad|iPod/i.test(ua)
  const named = APPS.find(([re]) => re.test(ua))?.[1]
  // Una WebView Android senza nome di app (`; wv)`) è comunque un browser interno.
  const app = named ?? (android && /; wv\)/.test(ua) ? '' : null)
  return app == null ? null : { app, android, ios }
}

/** Su Android la stessa pagina in Chrome (link intent://); fuori da Chrome il sistema chiede con cosa aprirla. */
export function chromeIntent(url: URL): string {
  return `intent://${url.host}${url.pathname}${url.search}#Intent;scheme=${url.protocol.replace(':', '')};package=com.android.chrome;S.browser_fallback_url=${encodeURIComponent(url.href)};end`
}
