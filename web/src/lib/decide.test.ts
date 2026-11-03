import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { nightTitleParts, act, allOk, canDrop, headsUpKey, nextDecisions, okAlso, okLines, screen, splitOutcome, outcomeText, visibleHeadsUp, buttons, card, viaApprove, chooseAt } from './decide'
import { decodeState, type Approval, type DecisionItem, type DecisionsPage, type ProvePage } from './contract'
import { outcomeType } from './kpi'

const read = (f: string) => readFileSync(new URL(`../../../contract/${f}`, import.meta.url), 'utf8')
const sample = JSON.parse(read('cmd-result-sample.json'))
const textOf = (op: string) => { const id = sample.cmd.find((c: { op: string }) => c.op === op).id; return JSON.parse(sample.result.find((r: { id: string }) => r.id === id).text) }
const A = (x: Partial<Approval> = {}) => ({ task: 't', title: 'T', what: '', where: '', deploy: false, requested_at: 0, ...x }) as Approval

describe('banco_prova', () => {
  it('banco_prova: la fixture si legge e Tutte ok manda gli id mostrati', () => {
    const p: ProvePage = textOf('prove')
    expect(p.n).toBe(2); expect(allOk(p.groups[0])).toBe('41,42'); expect(p.ko[0].id).toBe(38)
  })
  it('banco_prova: solo le schermate conosciute', () => {
    expect(screen(' search ')).toBe('search'); expect(screen('checkout')).toBeNull(); expect(screen(null)).toBeNull()
  })
})
describe('cinque_minuti', () => {
  const p: DecisionsPage = textOf('decisioni')
  const keys = p.groups.map(g => g.key)
  it('cinque_minuti: nell\'ordine del relay e quante ne vuoi', () => {
    expect(nextDecisions(p, new Set(), 1).map(g => g.key)).toEqual(keys.slice(0, 1))
    expect(nextDecisions(p, new Set([keys[0]]), 5).map(g => g.key)).toEqual(keys.slice(1))
    expect(nextDecisions(p, new Set(keys), 5)).toEqual([])
  })
  it('cinque_minuti: cosa fa il tasto', () => {
    expect([act({ kind: 'ok', task: 'a', since: 0 }), act({ kind: 'pause', task: 'a', since: 0 }), act({ kind: 'needs_you', session: 's', since: 0 }), act({ kind: 'needs_you', since: 0 }), act({ kind: 'altro', task: 'a', since: 0 })]).toEqual(['approve', 'done', 'done', 'none', 'none'])
    expect([canDrop('  '), canDrop('a voce')]).toEqual([false, true])
  })
})
describe('ok_informato', () => {
  it('ok_informato: quello che manca non si inventa', () => {
    expect(okLines(A())).toEqual({ untested: null, undo: null, undoHow: null, precedent: null })
    expect(okAlso(A())).toBeUndefined()
    const st = decodeState(read('state-2-idle.json'))
    const a = st.approvals.find(x => x.untested)!
    expect(okLines(a)).toMatchObject({ untested: 'a real phone', undo: 'in-parte', undoHow: 'pull the build' })
    expect(okAlso(a)).toEqual(['atlas-seo-pages'])
    expect(okLines(A({ undo: 'boh' })).undo).toBeNull()
  })
})
describe('chat_esito', () => {
  it('chat_esito: solo l\'ultima riga che è davvero l\'esito', () => {
    expect(splitOutcome('Fatto.\n\nEsito: pubblicato\n', 'Esito')).toEqual({ body: 'Fatto.', outcome: 'Esito: pubblicato' })
    const mid = 'Esito: a metà\nAltro testo'
    expect(splitOutcome(mid, 'Esito')).toEqual({ body: mid, outcome: null })
    expect(splitOutcome('Esitone', 'Esito').outcome).toBeNull()
    expect(splitOutcome('Esito: solo', 'Esito')).toEqual({ body: '', outcome: 'Esito: solo' })
    expect(splitOutcome('x\nEsito: done', 'Outcome').outcome).toBe('Esito: done')
  })
  it('chat_esito: il tipo dalla voce, senza resta neutro', () => {
    expect([outcomeType('shipped'), outcomeType(null), outcomeType('boh')]).toEqual(['shipped', 'none', 'none'])
  })
})
describe('heads_up', () => {
  const s = (text: string | null) => ({ name: 'demo', heads_up: text == null ? null : { text, seen_at: 5 } })
  it('heads_up: Già lo so la nasconde, una nuova ricompare', () => {
    expect(visibleHeadsUp(s('Usa un worktree'), new Set())?.text).toBe('Usa un worktree')
    const seen = new Set([headsUpKey('demo', 'Usa un worktree')])
    expect(visibleHeadsUp(s('Usa un worktree'), seen)).toBeNull()
    expect(visibleHeadsUp(s('Altra'), seen)?.text).toBe('Altra')
    expect(visibleHeadsUp(s(null), new Set())).toBeNull()
    expect(visibleHeadsUp(s('  '), new Set())).toBeNull()
  })
})
describe('notte_due_mesi', () => {
  const name = (d: Date) => ['gen', 'feb', 'mar', 'apr', 'mag', 'giu', 'lug', 'ago', 'set', 'ott', 'nov', 'dic'][d.getUTCMonth()]
  it('notte_due_mesi: ogni giorno il suo mese', () => {
    expect(nightTitleParts('2026-10-31', '2026-11-01', name)).toEqual({ dayA: 31, monthA: 'ott', dayB: 1, monthB: 'nov' })
    expect(nightTitleParts('2026-10-06', '2026-10-07', name)).toEqual({ dayA: 6, monthA: null, dayB: 7, monthB: 'ott' })
  })
})

describe('chat_esito_allineata', () => {
  it('chat_esito_allineata: il prefisso non si vede, lo dice l\'icona', () => {
    expect(outcomeText('Esito: fase B uscita', 'Esito')).toBe('fase B uscita')
    expect(outcomeText('Esito: fase B uscita', 'Outcome')).toBe('fase B uscita')
    expect(outcomeText('Outcome: done', 'Outcome')).toBe('done')
    expect(outcomeText('Esito fatto', 'Esito')).toBe('Esito fatto')
  })
  it('chat_esito_allineata: colore solo sull\'icona, centrata sulla prima riga, linea sopra', () => {
    const feed = readFileSync(new URL('./Feed.svelte', import.meta.url), 'utf8')
    const rule = (sel: string) => feed.split('\n').filter(l => l.includes(sel)).join(' ')
    expect(rule('.fesito p')).toContain('color: var(--text)')
    expect(rule('.fesito .gl')).toContain('height: 1lh')
    expect(rule('.fesito .gl')).toContain('align-items: center')
    expect(rule('.fesito {')).toContain('border-top: 1px solid var(--line)')
    expect(feed).toContain('class="sr"')
  })
  it('chat_esito_allineata: aspetta te ha la mano di Ti aspettano, in chat e negli esiti', () => {
    for (const f of ['./Feed.svelte', './HomeBox.svelte']) expect(readFileSync(new URL(f, import.meta.url), 'utf8'), f).toContain("waiting: 'hand'")
    expect(readFileSync(new URL('./HomeBox.svelte', import.meta.url), 'utf8')).toContain("needs_you: 'hand'")
    expect(readFileSync(new URL('./Lavori.svelte', import.meta.url), 'utf8')).toContain("l.who === 'you' ? 'hand'")
  })
})

describe('decisione_spiegata', () => {
  const p: DecisionsPage = textOf('decisioni')
  it('decisione_spiegata: la card dice cosa si propone', () => {
    const g = p.groups[0]
    expect(g.items[0]).toMatchObject({ title: 'Order mails of atlas-shop', asks: 'ok', plan_start: 'Proposal: move the checkout to the new layout, with the old one kept behind a flag.' })
    const c = card(g)
    expect(c.head).toEqual(['With the mailbox the order mails and the receipts start', 'mailbox for the shop'])
    expect(c.items[0]).toEqual({ title: 'Order mails of atlas-shop', plan: g.items[0].plan_start, reason: "Needs the shop's mailbox" })
  })
  it('decisione_spiegata: mai lo stesso testo due volte', () => {
    const x = "Proposta del 28/10 22:22: aspetta l'ok di Franz"
    const c = card({ key: 'k', title: x, cause: null, unblocks: [], weight: 1, where: 'here', since: 0, note: null, refs: [], items: [{ kind: 'pause', task: 't', title: 'Logo ambra su Play', reason: x, plan_start: "Cambiare l'icona quando Google approva.", since: 0 }] })
    expect(c.head).toEqual([x])
    expect(c.items[0]).toEqual({ title: 'Logo ambra su Play', plan: "Cambiare l'icona quando Google approva.", reason: null })
  })
  it('decisione_spiegata: i tasti secondo cosa si aspetta', () => {
    const it0 = (o: Partial<DecisionItem>): DecisionItem => ({ kind: 'pause', task: 't', since: 0, ...o })
    expect(buttons(it0({ asks: 'ok' }))).toEqual([{ kind: 'proceed' }, { kind: 'discuss' }])
    expect(viaApprove(it0({ asks: 'ok' }))).toBe(false)
    expect(viaApprove(it0({ kind: 'ok', asks: 'ok' }))).toBe(true)
    expect(buttons(it0({ kind: 'ok', asks: 'choice', options: ['A', 'B'] }))).toEqual([{ kind: 'choice', label: 'A' }, { kind: 'choice', label: 'B' }])
    expect(buttons(it0({ kind: 'needs_you', task: null, session: 's', asks: 'pc' }))).toEqual([{ kind: 'done' }])
    expect(buttons(it0({ kind: 'needs_you', task: null, session: 's', asks: 'other' }))).toEqual([{ kind: 'done' }, { kind: 'discuss' }])
    expect(buttons(it0({}))).toEqual([{ kind: 'done' }])
    expect(buttons(it0({ kind: 'ok' }))).toEqual([{ kind: 'proceed' }])
  })
  // app-decidere-tasti-03-11 (Franz 03/11 14:22: «In da decidere non ci sono tasti per decidere»): 31 voci su 42 avevano solo Parliamone.
  it('decidere_tasti: una pausa si decide dal telefono', () => {
    const pause = (reason: string, asks = 'other', options?: string[]): DecisionItem => ({ kind: 'pause', task: 't', session: 's', since: 0, asks, reason, options })
    const works = [{ kind: 'works' }, { kind: 'broken' }]
    expect(buttons(pause('Aspetta la prova di Franz sul telefono: versione finale'))).toEqual(works)
    expect(buttons(pause('Franz prova la pre-release tabellone-4-2026-11-02 e la PWA'))).toEqual(works)
    expect(buttons(pause('Seconda prova di Franz: pre-release in-cima-2'))).toEqual(works)
    expect(buttons(pause('Franz guarda la v12 (copia leggera mandata in chat)'))).toEqual(works)
    expect(buttons(pause('Waits for you to try build 0.4.12 on the phone'))).toEqual(works)
    expect(buttons(pause('Aspetta la prova di Franz sul telefono: nomi, ordine e ok', 'ok'))).toEqual(works)
    expect(buttons(pause("Aspetta l'ok di Franz per approvare il piano", 'ok'))).toEqual([{ kind: 'proceed' }, { kind: 'discuss' }])
    expect(buttons(pause('Rimandato da Franz il 28/10 21:35'))).toEqual([{ kind: 'done' }, { kind: 'discuss' }])
    expect(buttons(pause('Franz prova al PC', 'pc'))).toEqual([{ kind: 'done' }])
    expect(buttons(pause('Aspetta la scelta di Franz', 'choice', ['A']))).toEqual([{ kind: 'choice', label: 'A' }])
    const noOpts = pause('Aspetta la scelta di Franz sulla pulizia', 'choice')
    expect(buttons(noOpts)).toEqual([{ kind: 'discuss' }])
    expect(chooseAt(noOpts)).toBe('s')
    expect(chooseAt(pause('Aspetta la scelta', 'choice', ['A']))).toBeNull()
    expect(chooseAt(pause('Rimandato'))).toBeNull()
  })
})
