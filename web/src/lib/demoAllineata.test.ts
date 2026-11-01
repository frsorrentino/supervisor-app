import { describe, expect, it } from 'vitest'
import { world } from './demo'
import { DemoTransport } from './demoTransport'
import { DEFAULT_ORDER, type HomeSection } from './homeOrder'
import { head, midnight, outcomes, waitingRows, workingRows } from './kpi'
import { forYou, inProgress, place } from './lavori'
import { pluginsInfo, systemInfo } from './slotInfo'
import { attachedTasks, build } from './summary'
import { visible } from './suggestions'
import { newCmd } from './transport'
import { demoFallback, demoLink, demoReason, NO_DATA_MS, realLink } from './remoteAccess'

const NOW = 1_793_560_000
const w = world(NOW)
const st = w.state
const out = outcomes(w.timeline, NOW)!
const today = new Date(NOW * 1000).toISOString().slice(0, 10)

// Una sezione nuova della home senza una riga qui fa fallire il test: è quello che evita un altro disallineamento.
const SECTIONS: Record<HomeSection, () => number> = {
  sessions: () => build(st, [], NOW, new Set()).rows.length,
  suggestions: () => visible(st, new Set(), NOW).shown.length,
  lavori: () => inProgress(st, w.agenda, NOW, 'Europe/Rome', today).total,
  esiti: () => out.settled,
  usage: () => Object.keys(st.quota).length,
  system: () => (st.system ? 1 : 0),
  plugins: () => st.plugins?.accounts.length ?? 0,
  closed: () => st.sessions.filter(s => s.state === 'gone').length,
}

describe('demo_allineata', () => {
  it.each(DEFAULT_ORDER)('demo_allineata: la sezione %s ha dati', (k) => {
    expect(SECTIONS[k], `manca ${k} nella demo`).toBeDefined()
    expect(SECTIONS[k]()).toBeGreaterThan(0)
  })
  it('demo_allineata: nessun numero del tabellone a zero, nemmeno appena dopo mezzanotte', () => {
    for (const at of [NOW, midnight(NOW) + 60]) {
      const x = world(at)
      const h = head(x.state, x.timeline, at)
      expect(h.waiting).toBeGreaterThan(0); expect(h.busy).toBeGreaterThan(0); expect(h.outcomes).toBeGreaterThan(0)
    }
  })
  it('demo_allineata: i numeri del tabellone sono quelli delle pagine dietro i riquadri', () => {
    const h = head(st, w.timeline, NOW)
    expect(h.waiting).toBe(waitingRows(st).length)
    expect(h.busy).toBe(workingRows(st, NOW).length)
    expect(h.outcomes).toBe(out.settledRows.length)
    expect(h.red).toBe(out.red.length)
    // «Ti aspettano» nella home: le schede in attesa più gli ok senza sessione.
    const m = build(st, [], NOW, new Set())
    const loose = place(forYou(st, w.agenda, NOW, today), new Set(m.rows.map(r => r.session.name)), attachedTasks(m.rows)).loose.ok
    expect(m.rows.filter(r => r.group === 'waiting').length + loose.length).toBe(h.waiting)
    expect(m.open).toBe(systemInfo(st.system!, st.hosts_status, st.sessions).sessions)
  })
  it('demo_allineata: esiti di ogni tipo, «in corso» fuori dal numero', () => {
    expect(new Set(out.done.map(d => d.type))).toEqual(new Set(['problem', 'waiting', 'shipped', 'done', 'answer', 'none', 'running']))
    expect(out.done.length - out.settled).toBe(2)
  })
  it('demo_allineata: il dato dello slot esce dai dati della demo', () => {
    expect(systemInfo(st.system!, st.hosts_status, st.sessions)).toMatchObject({ pct: 42, sessions: 4 })
    expect(pluginsInfo(st.plugins!).names).toEqual(['docs-helper', 'release-notes'])
  })
  it('demo_allineata: il PC finto risponde a plugin e ordine della home', async () => {
    const d = new DemoTransport(() => NOW)
    const send = (op: string, arg: string | null) => { const c = newCmd(op as never, null, arg); return d.send(c) }
    const page = JSON.parse((await send('plugins', null))!.text)
    expect(page.accounts[0].plugins.filter((p: { update: boolean }) => p.update).length).toBe(2)
    await send('plugin_update', 'personal:docs-helper@demo')
    expect(d.state.plugins!.accounts[0].updates).toEqual(['release-notes@demo'])
    expect((await send('home_order', JSON.stringify(['lavori', 'pin:system'])))!.ok).toBe(true)
    expect(d.state.home_order).toEqual(['lavori', 'pin:system'])
  })
})

describe('demo_senza_dati', () => {
  it('demo_senza_dati: senza stato si passa alla demo dopo la soglia o al rifiuto', () => {
    expect(demoFallback({ hasState: false, denied: false, elapsedMs: NO_DATA_MS - 1 })).toBe(false)
    expect(demoFallback({ hasState: false, denied: false, elapsedMs: NO_DATA_MS })).toBe(true)
    expect(demoFallback({ hasState: false, denied: true, elapsedMs: 0 })).toBe(true)
  })
  it('demo_senza_dati: con uno stato già arrivato no, nemmeno col rifiuto', () => {
    expect(demoFallback({ hasState: true, denied: false, elapsedMs: 10 * NO_DATA_MS })).toBe(false)
    expect(demoFallback({ hasState: true, denied: true, elapsedMs: 10 * NO_DATA_MS })).toBe(false)
  })
  it('demo_senza_dati: il motivo sta nel collegamento e il tasto torna all\'app', () => {
    const u = new URL('https://x.example/?demo=denied')
    expect(demoReason(u)).toBe('denied')
    expect(demoReason(new URL('https://x.example/?demo'))).toBeNull()
    expect(demoLink('offline', new URL('https://x.example/'))).toBe('/?demo=offline')
    expect(realLink(u)).toBe('/')
  })
})
