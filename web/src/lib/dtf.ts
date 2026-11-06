// I formattatori di date una volta sola per lingua e opzioni (app-fluidita-06-11): crearne uno nuovo a ogni chiamata costava
// 40-50 ms a CPU 4× nel cambio fra cruscotto e plancia (lavori.ts, masterHome.ts, dayBars.ts nel profilo).
const cache = new Map<string, Intl.DateTimeFormat>()
export function dtf(locale?: string | string[], options: Intl.DateTimeFormatOptions = {}): Intl.DateTimeFormat {
  const k = JSON.stringify([locale ?? null, options])
  let f = cache.get(k)
  if (!f) { f = new Intl.DateTimeFormat(locale, options); cache.set(k, f) }
  return f
}
