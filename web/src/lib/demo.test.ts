import { describe, expect, it } from 'vitest'
import { world } from './demo'
import { DemoTransport } from './demoTransport'
import { build } from './summary'
import { alert } from './elsewhere'
import { items as queueOf } from './queue'
import { forMaster } from './strongAlert'
import { page as nightPage, showHomeNight } from './night'
import { newCmd } from './transport'
import type { State } from './contract'

// La demo rispecchia l'app (Franz, 15/10 20:36: «deve essere affidabile e rispecchiare al 100% l'app»): i nomi e i testi
// della demo Android, i conti che tornano fra home, banner, avviso e coda, e un PC finto che risponde come FakeTransport.
const NOW = 1_792_090_000
const w = world(NOW)
const live = (st: State) => st.sessions.filter(s => s.state !== 'gone')

describe('i dati della demo', () => {
  it('i nomi della demo Android (DemoText), nessun nome vero', () => {
    expect(w.state.sessions.map(s => s.name)).toEqual(['master', 'payments-api', 'storefront', 'blog', 'pricing-page'])
    const all = JSON.stringify(w)
    for (const name of ['claude-master', 'team-supervisor', 'franz', 'Franz', 'personali/', 'ledger-api', 'atlas-shop', 'field-notes', 'orbit-docs', 'DEPLOY_TOKEN']) expect(all).not.toContain(name)
  })
  it('tutto è di adesso: niente nel futuro, la notte appena passata, il recap di oggi', () => {
    expect(w.state.ts).toBe(NOW)
    expect(w.events.every(e => e.ts <= NOW)).toBe(true)
    for (const s of w.state.sessions) { expect(s.since).toBeLessThanOrEqual(NOW); if (s.question) expect(s.question.asked_at).toBeLessThanOrEqual(NOW) }
    expect(NOW - w.night.generated_at).toBeLessThan(86400)
    expect(w.state.night.report?.date).toBe(w.night.date)
    expect(showHomeNight(w.state.night.report, null)).toBe(true)
  })
  it('nessuna risposta a una domanda ancora aperta, nessun limite di quota su un account sotto il limite', () => {
    const open = new Set(w.state.sessions.filter(s => s.question).map(s => s.name))
    expect(w.events.some(e => e.kind === 'answered' && open.has(e.session ?? ''))).toBe(false)
    expect(w.events.some(e => e.kind === 'quota_limit')).toBe(false)
  })
  it('i conti tornano: «ti aspettano» nella home, il banner della master, l\'avviso in una chat e la coda', () => {
    const waiting = build(w.state, [], NOW, new Set()).rows.filter(r => r.group === 'waiting').map(r => r.session.name)
    expect(waiting.sort()).toEqual(['blog', 'payments-api', 'storefront'])
    expect(forMaster(w.state).map(a => a.session).sort()).toEqual(waiting)
    expect(queueOf(w.state).map(i => i.session).sort()).toEqual(waiting)
    const a = alert(w.state, 'storefront', NOW, new Set(), new Set())
    expect(a?.type === 'waiting' && a.sessions.sort()).toEqual(['blog', 'payments-api'])
  })
  it('la notte chiede quello che è aperto adesso', () => {
    const p = nightPage(w.night, 'Europe/Rome', { live: new Set(live(w.state).map(s => s.name)), pending: new Set(w.state.approvals.map(a => a.task)) })
    const q = w.state.sessions.find(s => s.name === 'payments-api')!.question!
    expect(p.needs.find(n => n.session === 'payments-api')?.text).toBe(q.text)
  })
})

describe('il PC finto della demo (FakeTransport)', () => {
  const tr = () => new DemoTransport(() => NOW + 60)
  it('una risposta toglie la domanda e mette la sessione al lavoro', async () => {
    const d = tr()
    const q = d.state.sessions.find(s => s.name === 'payments-api')!.question!
    const r = await d.send(newCmd('answer', 'payments-api', '1', undefined, q.id))
    expect(r).toMatchObject({ ok: true, text: 'answered 1. yes' })
    const s = d.state.sessions.find(x => x.name === 'payments-api')!
    expect([s.state, s.question, s.tool_note]).toEqual(['busy', null, 'Deploying 2.8.0 to production'])
    expect(d.state.attention?.some(a => a.session === 'payments-api')).toBe(false)
  })
  it('un messaggio la mette al lavoro e toglie la ferma; programmato aspetta', async () => {
    const d = tr()
    expect((await d.send(newCmd('prompt', 'storefront', 'here is the code: 4411'))).text).toBe('delivered')
    const s = d.state.sessions.find(x => x.name === 'storefront')!
    expect([s.state, s.needs_you]).toEqual(['busy', null])
    expect((await d.send({ ...newCmd('prompt', 'blog', 'later'), at: NOW + 3600 })).text).toMatch(/^scheduled for \d\d:\d\d: blog gets it then/)
  })
  it('i rifiuti del relay', async () => {
    const d = tr()
    expect(await d.send(newCmd('answer', 'blog', '1'))).toMatchObject({ ok: false, text: 'blog has no question' })
    expect(await d.send(newCmd('model', 'blog', 'x'))).toMatchObject({ ok: false, text: 'blog: not available in demo' })
    expect(await d.send(newCmd('interrupt', 'blog'))).toMatchObject({ ok: false, text: 'blog: nothing to stop' })
    expect(await d.send(newCmd('pair_add', null))).toMatchObject({ ok: false })
  })
  it('stop, riapri, approva, segui, notte e chiusura', async () => {
    const d = tr()
    expect((await d.send(newCmd('interrupt', 'storefront'))).text).toBe('storefront: stopped')
    expect((await d.send(newCmd('reopen', 'pricing-page'))).ok).toBe(true)
    expect(d.state.sessions.find(s => s.name === 'pricing-page')!.state).toBe('idle')
    const task = d.state.approvals[0].task
    expect((await d.send(newCmd('approve', null, task))).text).toBe(`approved ${task}`)
    expect(d.state.approvals).toEqual([])
    await d.send(newCmd('follow', 'payments-api'))
    expect(d.state.sessions.find(s => s.name === 'payments-api')!.followed).toBe(true)
    const p = d.state.projects[0].path
    const r = await d.send(newCmd('night_add', null, p, 'tidy the docs'))
    expect(r.ok && r.job).toBeTruthy()
    expect((await d.send(newCmd('night_remove', null, r.job!))).ok).toBe(true)
    await d.send(newCmd('slash', 'blog', 'exit'))
    expect(d.state.sessions.find(s => s.name === 'blog')!.state).toBe('gone')
  })
  it('la conversazione di prova, poi solo le voci nuove', async () => {
    const d = tr()
    const all = JSON.parse((await d.send(newCmd('transcript', 'blog', '50'))).text).entries
    expect(all.length).toBeGreaterThan(0)
    expect(JSON.parse((await d.send(newCmd('transcript', 'blog', `200:after=${all.at(-1).id}`))).text).entries).toEqual([])
  })
  it('lo stesso comando due volte ha la stessa risposta', async () => {
    const d = tr()
    const c = newCmd('interrupt', 'storefront')
    const a = await d.send(c), b = await d.send(c)
    expect(b).toBe(a)
  })
})
