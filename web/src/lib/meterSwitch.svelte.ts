// Il contatore dei fotogrammi nascosto (app-fluidita-06-11): spento di default; si accende e si spegne con tre tocchi in fila sul
// numero di versione nella barra in alto, o con `?meter=1` (`?meter=0` lo spegne). Resta acceso su questo browser.
const KEY = 'cm.meter'
const load = (k: string) => { try { return localStorage.getItem(k) } catch { return null } }
const store = (k: string, v: string | null) => { try { if (v == null) localStorage.removeItem(k); else localStorage.setItem(k, v) } catch { /* resta per la visita */ } }

const asked = typeof location === 'undefined' ? null : new URLSearchParams(location.search).get('meter')
if (asked === '1') store(KEY, '1')
if (asked === '0') store(KEY, null)

export const meter = $state<{ on: boolean }>({ on: load(KEY) === '1' })

export function setMeter(on: boolean) { meter.on = on; store(KEY, on ? '1' : null) }

/** Tre tocchi sul numero di versione entro 700 ms l'uno dall'altro. */
let taps: number[] = []
export function tapVersion(now = performance.now()) {
  taps = [...taps.filter(x => now - x < 700 * 2), now].slice(-3)
  if (taps.length === 3 && now - taps[0] < 1400) { taps = []; setMeter(!meter.on) }
}
