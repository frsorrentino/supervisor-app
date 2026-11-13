import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { afterError, DELAYS_MS, gated, mayRead, MAX_SAME, open, retryIn, stopped, type Answer } from './retryGate'

// app-agenda-raffica-13-11: sul relay di un collega, con l'agenda non configurata, la web chiedeva `agenda` circa 175 volte al
// secondo, ogni volta con «ERR no agenda file».
/** Un trasporto finto che conta le richieste, e un orologio a mano coi tentativi programmati. */
function rig(answer: () => Answer | Promise<Answer>, off?: (t: string) => boolean) {
  let now = 0
  let sent = 0
  const timers: { at: number; fn: () => void }[] = []
  const seen: (string | null)[] = []
  const r = gated({
    send: async () => { sent++; return answer() }, ok: () => {}, fail: x => seen.push(x), off,
    now: () => now, timer: (fn, ms) => timers.push({ at: now + ms, fn }),
  })
  // Come l'effetto di App: riparte a ogni cambio di stato finché non c'è una pagina.
  const hammer = async (n: number) => { for (let i = 0; i < n; i++) await r.read('auto') }
  const advance = async (ms: number) => {
    now += ms
    for (const t of timers.splice(0).filter(t => (t.at <= now ? true : (timers.push(t), false)))) { t.fn(); await Promise.resolve(); await Promise.resolve(); await Promise.resolve() }
  }
  return { r, hammer, advance, sent: () => sent, seen, pending: () => timers.length }
}

describe('una lettura in errore non riparte subito', () => {
  it('mille giri dell\'effetto dopo un ERR fanno una richiesta sola', async () => {
    const x = rig(() => ({ ok: false, text: 'relay busy' }))
    await x.hammer(1000)
    expect(x.sent()).toBe(1)
  })
  it('poi riprova da sola con un\'attesa crescente e si ferma dopo tre errori uguali', async () => {
    const x = rig(() => ({ ok: false, text: 'relay busy' }))
    await x.hammer(50)
    await x.advance(DELAYS_MS[0] - 1); await x.hammer(50)
    expect(x.sent()).toBe(1)
    await x.advance(1)
    expect(x.sent()).toBe(2)
    await x.advance(DELAYS_MS[1] - 1); await x.hammer(50)
    expect(x.sent()).toBe(2)
    await x.advance(1)
    expect(x.sent()).toBe(3)
    // Ferma: nessun altro tentativo programmato, e l'effetto non la riaccende.
    expect([stopped(x.r.gate()), x.pending()]).toEqual([true, 0])
    await x.advance(3_600_000); await x.hammer(1000)
    expect(x.sent()).toBe(3)
    // La persona sì, una volta.
    await x.r.read('user')
    expect(x.sent()).toBe(4)
  })
  it('«no agenda file» spegne la funzione: nemmeno a mano, finché il relay non la offre di nuovo', async () => {
    const x = rig(() => ({ ok: false, text: 'no agenda file' }), t => t.startsWith('no agenda file'))
    await x.hammer(1000)
    await x.advance(3_600_000)
    await x.r.read('user'); await x.r.read('retry'); await x.hammer(100)
    expect([x.sent(), x.pending(), x.seen]).toEqual([1, 0, ['no agenda file']])
    x.r.reset()
    await x.hammer(10)
    expect(x.sent()).toBe(2)
  })
  it('una risposta buona riapre tutto; nessuna risposta conta come errore', async () => {
    let ok = false
    const x = rig(() => { if (ok) return { ok: true, text: '{}' }; throw new Error('down') })
    await x.hammer(20)
    expect([x.sent(), x.seen]).toEqual([1, [null]])
    ok = true
    await x.advance(DELAYS_MS[0])
    expect([x.sent(), x.r.gate()]).toEqual([2, open()])
    await x.hammer(3)
    expect(x.sent()).toBe(5)
  })
  it('una lettura per volta', async () => {
    let release: (a: Answer) => void = () => {}
    const x = rig(() => new Promise<Answer>(res => { release = res }))
    const first = x.r.read('auto')
    await x.r.read('auto'); await x.r.read('user')
    expect(x.sent()).toBe(1)
    release({ ok: true, text: '{}' }); await first
  })
  it('le regole: un errore diverso riparte da uno, l\'attesa non supera l\'ultima', () => {
    let g = afterError(open(), 'a', 0)
    expect([g.fails, g.until, mayRead(g, 0, 'user'), mayRead(g, DELAYS_MS[0], 'user'), mayRead(g, DELAYS_MS[0], 'auto')]).toEqual([1, DELAYS_MS[0], false, true, false])
    g = afterError(g, 'b', 100)
    expect([g.fails, retryIn(g, 100)]).toEqual([1, DELAYS_MS[0]])
    for (let i = 0; i < 5; i++) g = afterError(g, 'b', 100)
    expect([g.fails > MAX_SAME, g.until, retryIn(g, 100)]).toEqual([true, 100 + DELAYS_MS[2], null])
  })
  it('App: agenda e notte passano dal cancello, niente richiesta diretta', () => {
    const app = readFileSync(new URL('../App.svelte', import.meta.url), 'utf8')
    expect(app).toContain('const agendaReader = gated(')
    expect(app).toContain('const nightReader = gated(')
    expect(app.match(/newCmd\('agenda', null, null\)/g)).toHaveLength(1)
    expect(app.match(/newCmd\('night', null, null\)/g)).toHaveLength(1)
  })
})
