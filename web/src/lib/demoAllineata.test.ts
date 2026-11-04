import { parseSteps } from './nextSteps'
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { replaceNames, splitLabels, taskNames } from './chatLabels'
import { world } from './demo'
import { DemoTransport } from './demoTransport'
import { DEFAULT_ORDER, pinned, type HomeSection } from './homeOrder'
import { DEFAULT_CATS, decode as decodeCats, top as topOf } from './homeCats'
import { total } from './seguiti'
import { weekSlot } from './utilizzo'
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
  prima_raccolta: () => (st.prima_raccolta ? 1 : 0),
  seguiti: () => total(st.seguiti),
  prove: () => st.prove?.n ?? 0,
  decisioni: () => st.decisions?.n ?? 0,
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
  it('demo_allineata: ogni ok sta nella scheda della sua sessione', () => {
    expect(st.approvals.length).toBeGreaterThan(0)
    for (const a of st.approvals) {
      const c = st.commitments!.find(x => x.task === a.task)!
      expect(c, a.task).toBeDefined()
      expect(`${a.title} ${a.where}`, a.task).toContain(c.session!)
    }
  })
  it('demo_allineata: nessuna scheda chiede la stessa cosa due volte', () => {
    const words = (x: string) => new Set(x.toLowerCase().split(/\W+/).filter(w => w.length > 3))
    for (const s of st.sessions) {
      const stop = s.needs_you?.reason ?? s.turn_blocking?.text ?? s.question?.text
      const mine = st.commitments!.filter(c => c.session === s.name && c.waits?.on === 'person')
      expect(new Set(mine.map(c => c.paused?.reason ?? c.title)).size, s.name).toBe(mine.length)
      if (stop) for (const c of mine) for (const t of [c.title, c.paused?.reason ?? '']) {
        const shared = [...words(t)].filter(w => words(stop).has(w))
        expect(shared, `${s.name}: «${t}» ripete «${stop}»`).toEqual([])
      }
    }
  })
  it('demo_allineata: parte con Utilizzo sul tabellone, così il dato della settimana si vede (app-tabellone-quattro-02-11)', () => {
    expect(topOf(st.home_order)[0]).toBe('usage')
    expect(weekSlot(st.quota, NOW)).not.toBeNull()
  })
  it('demo_allineata: la home della demo è quella predefinita per categorie, con Utilizzo e Sessioni sul tabellone (app-tabellone-quattro-02-11)', () => {
    expect(topOf(st.home_order)).toEqual(['usage', 'sessions'])
    const want = DEFAULT_CATS.map(c => [c.id, c.open, c.sections.filter(k => k !== 'sessions' && k !== 'usage')])
    expect(decodeCats(st.home_order).map(c => [c.id, c.open, c.sections])).toEqual(want)
  })
  it('demo_allineata: ogni suggerimento ha un progetto della demo prima dei due punti', () => {
    const names = new Set(st.sessions.map(s => s.name))
    expect(st.suggestions!.length).toBeGreaterThan(0)
    for (const s of st.suggestions!) expect(names, s.text).toContain(s.text.split(':')[0])
  })
  it('demo_allineata: in chat si vedono il riquadro, Non provato e il nome di un compito', () => {
    const text = w.transcripts.master[0].text ?? ''
    expect(splitLabels(text).map(p => p.kind)).toEqual(['plain', 'you', 'untested', 'plain'])
    expect(replaceNames(text, taskNames(st.commitments))).toContain('](https://task.invalid/demo-cover)')
  })
  it("demo_allineata: ogni messaggio che chiude con «Esito:» ha il suo tipo, così la chat lo mostra con l'icona (app-bug-notte-03-11)", () => {
    // Come la chat: prima si toglie la riga «Prossimi:», poi conta l'ultima riga.
    const closing = Object.values(w.transcripts).flat().filter(e => e.role === 'assistant' && /^Esito:/.test(parseSteps(e.text ?? '').text.trim().split('\n').pop() ?? ''))
    expect(closing.length).toBeGreaterThan(1)
    for (const e of closing) expect(e.outcome_type, e.id).toBeTruthy()
  })
  it('demo_allineata: Da provare, Da decidere, ok, Esito e nota hanno i loro dati', async () => {
    const d = new DemoTransport(() => NOW)
    const read = async (op: string) => JSON.parse((await d.send(newCmd(op as never, null)))!.text)
    const p = await read('prove'), g = await read('decisioni')
    expect(st.prove?.n).toBe(p.n); expect(st.decisions?.n).toBe(g.n); expect(p.n).toBeGreaterThan(0); expect(g.groups.length).toBeGreaterThan(0)
    expect(st.approvals.every(a => a.untested && a.undo)).toBe(true)
    expect(w.transcripts.master[0].outcome_type).toBe('waiting')
    expect(st.sessions.some(s => s.heads_up?.text)).toBe(true)
    await d.send(newCmd('prova', null, '1,2', undefined, undefined, { result: 'ok' }))
    expect(d.state.prove).toBeNull()
    await d.send(newCmd('decisione', null, 'demo-a', undefined, undefined, { action: 'not_now', hours: 24 }))
    expect(d.state.decisions?.n).toBe(2)
    expect((await d.send(newCmd('decisione', null, 'demo-b', undefined, undefined, { action: 'drop' })))!.ok).toBe(false)
  })
  it('demo_rapporto_inglese: nessuna frase della master resta in italiano', () => {
    const NIGHT = readFileSync(new URL('./demo.ts', import.meta.url), 'utf8')
    const report = JSON.parse(readFileSync(new URL('../../../contract/night-report-sample.json', import.meta.url), 'utf8'))
    const texts = new Set<string>()
    const walk = (v: unknown): void => { if (typeof v === 'string') { if (/^(Dalla master|sposta il sito)/.test(v)) texts.add(v) } else if (Array.isArray(v)) v.forEach(walk); else if (v && typeof v === 'object') Object.values(v).forEach(walk) }
    walk(report)
    expect(texts.size).toBeGreaterThan(0)
    for (const x of texts) expect(NIGHT, x).toContain(`'${x}':`)
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

// app-demo-sempre-allineata-03-11: ogni campo che il relay manda nello stato (le fixture del contratto, livello alto e sessioni)
// ha dati anche nella demo. Un campo nuovo del contratto senza dati finti fa fallire il test, e con lui la build.
// Le eccezioni stanno qui sotto, ognuna col suo perché.
const NOT_IN_DEMO: Record<string, string> = {
  feedback: "le segnalazioni d'errore dei plugin: l'app non le legge ancora (non c'è nel tipo State)",
}
const filled = (v: unknown) => v != null && v !== '' && !(Array.isArray(v) && !v.length) && !(typeof v === 'object' && !Array.isArray(v) && !Object.keys(v as object).length)
const fixtures = ['state-1-question', 'state-2-idle', 'state-3-stale'].map(f => JSON.parse(readFileSync(new URL(`../../../contract/${f}.json`, import.meta.url), 'utf8')) as Record<string, unknown>)
const topKeys = [...new Set(fixtures.flatMap(f => Object.keys(f)))].sort()
const sessionKeys = [...new Set(fixtures.flatMap(f => ((f.sessions as Record<string, unknown>[]) ?? []).flatMap(s => Object.keys(s))))].sort()

describe('demo_allineata: i campi del contratto', () => {
  it('ogni campo dello stato ha dati nella demo', () => {
    const missing = topKeys.filter(k => !(k in NOT_IN_DEMO) && !filled((st as unknown as Record<string, unknown>)[k]))
    expect(missing, `campi dello stato senza dati nella demo: ${missing.join(', ')}`).toEqual([])
  })
  it('ogni campo di una sessione ha dati in almeno una sessione della demo', () => {
    const ss = st.sessions as unknown as Record<string, unknown>[]
    const missing = sessionKeys.filter(k => !(`sessions.${k}` in NOT_IN_DEMO) && !ss.some(s => filled(s[k])))
    expect(missing, `campi delle sessioni senza dati nella demo: ${missing.join(', ')}`).toEqual([])
  })
})

// Le letture che l'app fa quando apre una pagina o un riquadro: la demo risponde a ognuna (prima mancava `kpi`).
const READS = ['kpi', 'prove', 'decisioni', 'seguiti', 'results', 'prima_raccolta', 'plugins', 'projects', 'night', 'agenda', 'timeline'] as const
describe('demo_allineata: le letture', () => {
  it.each(READS)('demo_allineata: la demo risponde a %s', async (op) => {
    const d = new DemoTransport(() => NOW)
    const r = await d.send(newCmd(op as never, null))
    expect(r?.ok, `${op}: ${r?.text}`).toBe(true)
    expect(filled(JSON.parse(r!.text)), op).toBe(true)
  })
  it('demo_allineata: i riquadri dei KPI hanno i loro numeri', async () => {
    const d = new DemoTransport(() => NOW)
    const k = JSON.parse((await d.send(newCmd('kpi', null)))!.text)
    expect(k.kpi_health.local_disk.total).toBeGreaterThan(0)
    expect(k.kpi_health.commands.p95_s).toBeGreaterThan(0)
    expect(k.kpi_health.hosts.some((h: { working?: boolean }) => h.working)).toBe(true)
  })
  it('demo_allineata: i conti di Tocca a te per sessione sommano quelli della home', () => {
    const ss = st.sessions.filter(s => s.tocca)
    expect(ss.reduce((n, s) => n + s.tocca!.prove, 0)).toBe(st.tocca!.prove)
    expect(ss.reduce((n, s) => n + s.tocca!.decidi, 0)).toBe(st.tocca!.decidi)
  })
})
