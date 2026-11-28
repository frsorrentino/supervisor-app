import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { ResultGroup, WaitAction, WaitItem } from './contract'
import { adviceOf, groupOf, groupsOf, mainOf, splitBlocking, voceOf, voci } from './perTe'

// Gli stessi casi di PerTeTest.kt.
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/per-te-cases.json', import.meta.url), 'utf8')) as {
  main: { name: string; actions: Pick<WaitAction, 'label' | 'style' | 'recommended' | 'text_field'>[]; out: unknown }[]
  voci: { name: string; session: string; waits: Partial<WaitItem>[]; results: { session: string; unseen: boolean; id: string; title: string; at: number }[]; out: unknown[] }[]
}
const group = (r: { session: string; unseen: boolean; id: string; title: string; at: number }): ResultGroup => ({
  title: r.title, session: r.session, kind: 'document', at: r.at, versions: 1, unseen: r.unseen,
  items: [{ id: r.id, title: r.title, path: `out/${r.id}.md`, kind: 'document', at: r.at, version: 1, versions: 1, session: r.session, seen_at: r.unseen ? null : r.at }] as ResultGroup['items'],
})

describe('formato unico: il tasto sulla voce', () => {
  for (const c of file.main) it(c.name, () => expect(mainOf(c.actions)).toEqual(c.out))
  it('senza elenco di tasti (un relay di prima)', () => expect(mainOf(null)).toBeNull())
})

describe('formato unico: le voci della sessione', () => {
  for (const c of file.voci) it(c.name, () => {
    const got = voci(c.waits as WaitItem[], c.results.map(group), c.session)
    expect(got.map(v => ({ key: v.key, group: v.group, title: v.title, context: v.context }))).toEqual(c.out)
  })
  it('una verifica porta il risultato da segnare visto, una richiesta sé stessa', () => {
    const got = voci([{ id: 'w1', label: 'reply', session: 'a', text: 'Sì?', at: 1, actions: [{ label: 'sì', style: 'option', cmd: { op: 'answer' } }] } as WaitItem], [group({ session: 'a', unseen: true, id: 'r1', title: 'Rapporto', at: 2 })], 'a')
    expect(got[0].wait?.id).toBe('w1'); expect(got[0].main).toEqual({ index: 0, label: '1 · sì', direct: true, recommended: false }); expect(got[0].result).toBeNull()
    expect(got[1].result).toEqual({ id: 'r1', kind: 'document', version: null }); expect(got[1].wait).toBeNull()
  })
  it('le etichette', () => expect(['reply', 'decide', 'choose', 'verify', 'altro'].map(groupOf)).toEqual(['aspetta', 'decidi', 'decidi', 'prova', 'decidi']))
})

// Lo stato aperto o chiuso dei blocchi ora è per sessione: riquadri.test.ts (app-riquadri-chiusi-16-11).

// app-riall-2-viste-16-11 (direzione A): Dalla master per sessione, con le stesse voci di «Per te»; la fixture del relay (contratto
// 1.151), letta anche da PerTeTest.kt. Ogni gruppo ha tante voci quante ne conta `sessions[].tocca` della sua sessione.
describe('Dalla master per sessione e il consiglio sulla riga', () => {
  const rd = JSON.parse(readFileSync(new URL('../../../contract/attese-rd-sample.json', import.meta.url), 'utf8')) as {
    attese: { items: WaitItem[] }; state: { sessions: { name: string; tocca: { prove: number; decidi: number; rispondi: number } }[] }
  }
  const items = rd.attese.items
  it('un gruppo per sessione, prima chi aspetta da più tempo, in fondo le voci senza sessione', () => {
    const g = groupsOf(items, [])
    expect(g.map(x => x.session)).toEqual(['win:alfa', 'delta', null])
    expect(g.flatMap(x => x.voci).length).toBe(items.length)
    for (const s of rd.state.sessions) expect(g.find(x => x.session === s.name)?.voci.length).toBe(s.tocca.prove + s.tocca.decidi + s.tocca.rispondi)
    // Le righe sono quelle di «Per te» nella sessione.
    expect(g[0].voci).toEqual(voci(items, [], 'win:alfa'))
  })
  it('i risultati da guardare stanno nel gruppo della loro sessione; le voci risposte escono, e un gruppo vuoto con loro', () => {
    const res = [group({ session: 'zeta', unseen: true, id: 'r1', title: 'Rapporto', at: 1 }), group({ session: 'delta', unseen: false, id: 'r2', title: 'Visto', at: 1 })]
    const g = groupsOf(items, res, new Set(['w17f9a258a0', 'wdd48019c59']))
    expect(g.map(x => x.session)).toEqual(['zeta', 'delta', null])
    expect(g[0].voci.map(v => [v.key, v.group])).toEqual([['r1', 'verifica']])
    expect(groupsOf([], [])).toEqual([])
  })
  it('il consiglio: approvare su un ok, la scelta consigliata col numero del tasto, niente senza', () => {
    const ok = voceOf(items.find(w => w.id === 'w17f9a258a0')!)
    expect(ok.advice).toEqual({ approve: true, option: null })
    // app-tieni-premuto-28-11: l'ok di «pubblica 1.4» pubblica, quindi si tiene premuto nella sua scheda e la riga la apre.
    expect(ok.main).toEqual({ index: 0, label: '1 · Approva', direct: false, recommended: true })
    // Il Va di una prova resta un tocco sulla riga, col finale del cerchio verde.
    expect(voceOf(items.find(w => w.id === 'wdd48019c59')!).main).toEqual({ index: 0, label: 'Va', direct: true, recommended: false, tap: true })
    expect(voceOf(items.find(w => w.id === 'wdd48019c59')!).advice).toBeNull()
    const pick = mainOf([{ label: 'no', style: 'option' }, { label: 'sì', style: 'option', recommended: true }])
    expect(adviceOf({ advice: { text: 'già provato' } }, pick)).toEqual({ approve: null, option: '2 · sì' })
    expect(adviceOf({ advice: { text: 'x', approve: false } }, pick)).toEqual({ approve: false, option: null })
    expect(adviceOf({ advice: { text: 'x', option: '  Domani   mattina ' } }, null)).toEqual({ approve: null, option: 'Domani mattina' })
    expect(adviceOf({ advice: { text: 'solo il motivo' } }, null)).toBeNull()
    expect(adviceOf({ advice: null }, pick)).toBeNull()
  })
})

// app-riall-2-viste-16-11: le due viste collegate, dal sorgente come gli altri test di questo tipo.
describe("le due viste: «Per te» nella sessione e Dalla master per sessione", () => {
  const read = (f: string) => readFileSync(new URL(f, import.meta.url), 'utf8')
  it("con la lista unica Decidi è «Per te» per sessione e Verifica non c'è più", () => {
    const home = read('./Home.svelte'), app = read('../App.svelte')
    expect(home).toMatch(/k === 'prove' && !unified/)
    expect(home).toMatch(/k === 'decisioni' && unified && perTeRows > 0\}\{@render sec\('decisioni', perTeSec\(\)\.name/)
    expect(app).toMatch(/<PerTeGruppi \{run\} groups=\{perTeG\}/)
  })
  it('nella sessione il rimando a Dalla master, e sullo schermo largo solo il numero', () => {
    expect(read('../App.svelte')).toMatch(/<PerTe \{run\} session=\{name\} items=\{uItems\} wide=\{wide && unified\} onMaster=\{unified \? toMaster : null\}/)
    expect(read('./PerTe.svelte')).toMatch(/data-testid="per-te-in-master"/)
  })
  it('le due viste usano la stessa riga e lo stesso tasto', () => {
    for (const f of ['./PerTe.svelte', './PerTeGruppi.svelte']) expect(read(f)).toMatch(/actVoce\(run, v, /)
  })
})

// app-per-te-ordine-23-11 (contratto 1.160): le carte che bloccano un compito in alto, il resto nei gruppi. Come PerTeTest.kt.
describe('Per te in ordine', () => {
  const w = (id: string, session: string, at: number, blocking?: boolean): WaitItem =>
    ({ id, type: 'carta', kind: 'ok', label: 'decide', label_text: 'Decidi', session, task: id, text: id, at, card: null, link: '', state: 'open', ...(blocking ? { blocking } : {}) }) as WaitItem
  it('in alto le bloccanti dalla più vecchia, con la loro sessione; i gruppi tengono il resto', () => {
    const groups = groupsOf([w('a1', 'albo', 100), w('a2', 'albo', 200), w('s1', 'shop', 300, true), w('a3', 'albo', 50, true)], [])
    const { top, groups: rest } = splitBlocking(groups)
    expect(top.map(v => [v.key, v.session])).toEqual([['a3', 'albo'], ['s1', 'shop']])
    expect(rest.map(g => [g.session, g.voci.map(v => v.key)])).toEqual([['albo', ['a1', 'a2']]])
  })
  it('un relay prima della 1.160: niente in alto, tutto nei gruppi', () => {
    const groups = groupsOf([w('a1', 'albo', 100), w('s1', 'shop', 300)], [])
    expect(splitBlocking(groups)).toEqual({ top: [], groups })
  })
})
