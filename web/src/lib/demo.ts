import stateRaw from '../../../contract/state-1-question.json?raw'
import changesRaw from '../../../contract/changes-sample.json?raw'
import eventsRaw from '../../../contract/events-sample.json?raw'
import nightRaw from '../../../contract/night-report-sample.json?raw'
import { decodeChanges, decodeState } from './contract'
import type { AgendaPage, ChangedFile, Event, SearchPage, Session, State, StepDiff, StudioPage, TimelinePage, TranscriptEntry } from './contract'
import type { Sent, Status } from './chatRules'
import type { Sample } from './quotaHistory'
import type { NightReport } from './night'
import { lang } from './lang'

// Il mondo della demo, porta di FakeTransport.kt e DemoText.kt: le fixture del contratto con i nomi e i testi della demo
// dell'app (payments-api, storefront, blog, pricing-page), portate ad adesso. In più, rispetto all'app, la master e una
// conversazione breve per sessione, perché la web app mostri la chat.
//
// I conti tornano (Franz, 15/10 20:36: «la demo deve essere affidabile»): niente eventi nel futuro, nessuna risposta a una
// domanda ancora aperta, nessun limite di quota su un account all'11 %, la domanda della notte è quella aperta adesso.
//
// In inglese i testi della demo sono inglesi (Franz, 13/10 21:48); le righe «Esito:» e «Prossimi:» restano: sono il
// formato con cui le sessioni chiudono il turno, e la web app le mostra come «Outcome» e come tasti.
const en = lang === 'en'

/** DemoText.kt: frasi più lunghe prima delle più corte che le contengono. Più, solo qui, la domanda col token in chiaro. */
const DRESS: [string, string][] = [
  ["Deploy ready with DEPLOY_TOKEN=dpl_9f8e7d6c5b4a, waiting for the client's ok. Deploy now?", 'Staging is green. Deploy 2.8.0?'],
  ["Deploy ready with DEPLOY_TOKEN=\u2022\u2022\u2022, waiting for the client's ok. Deploy now?", 'Staging is green. Deploy 2.8.0?'],
  ['Deploy ready with DEPLOY_TOKEN=\\u2022\\u2022\\u2022, waiting for the client\'s ok. Deploy now?', 'Staging is green. Deploy 2.8.0?'],
  // Il comando e lo schema col vecchio nome del plugin: nella demo quello di oggi.
  ['team-supervisor', 'supervisor'],
  ["Deploy ready, waiting for the client's ok. Deploy now?", 'Staging is green. Deploy 2.8.0?'],
  ['Esito: migrations 008-011 applied, tests green.\\nThe test seeds and the admin page are still to review.',
    'Applied migrations 008-011 for saved carts and gift cards; all 214 tests are green.\\nThe seed data and the new orders admin page are still to review, and the gift card flow needs a manual check on staging.'],
  ['Esito: README rewritten with the three sections asked for.',
    'Rewrote the post on edge caching with the three sections you asked for, added the benchmark table and fixed three broken links.'],
  ['Esito: seeds and admin page reviewed, 42 tests green.', 'Reviewed the seed data and the orders admin page; 214 tests green and the gift card flow checked on staging.'],
  ['Migrations 008-011 applied, tests green', 'Applied migrations 008-011 for saved carts and gift cards; all 214 tests green'],
  ['Review the seeds and the admin page', 'Review the seed data and the new orders admin page'],
  ['README rewritten', 'Rewrote the post on edge caching, added the benchmark table and fixed three broken links'],
  ['Run the test suite', 'Run the checkout test suite after the Stripe webhook refactor'],
  ['Wait for the go', "Wait for the client's go, then deploy 2.8.0"],
  ['Pick up the pricing page', 'Pick up the pricing page redesign for the App Store listing'],
  ['Deploy ready', 'Refund endpoint ready'],
  ['Deploy 2.4 to production now', 'Deploy 2.8.0 to production now'],
  ['Update the changelog for 2.4 and check the migration notes', 'Update the changelog for 2.8.0 and check the release notes'],
  ...NAMES(),
  ['crostini-demo', 'dev-laptop'],
  // La quota di lavoro della fixture è «stale» e senza finestra di 5 ore: nel video sembrava un errore (17/09 02:30).
  ['"h5": null,\n      "w7": 75,', '"h5": 38,\n      "w7": 75,'],
  ['"stale": true,\n      "kind": "work"', '"stale": false,\n      "kind": "work"'],
]
function NAMES(): [string, string][] {
  return [['ledger-api', 'payments-api'], ['atlas-shop', 'storefront'], ['field-notes', 'blog'], ['orbit-docs', 'pricing-page']]
}
export const dress = (json: string, pairs = DRESS) => pairs.reduce((s, [a, b]) => s.split(a).join(b), json)

/** La finestra delle 5 ore in corso da tre ore (FakeTransport: DEMO_WINDOW_LEFT_S). */
const WINDOW_LEFT_S = 2 * 3600

const isoDay = (t: number) => new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(t * 1000))

export type World = {
  state: State; events: Event[]; transcripts: Record<string, TranscriptEntry[]>; mine: [Sent, Status][]; samples: Record<string, Sample[]>
  night: NightReport; agenda: AgendaPage; timeline: TimelinePage
  /** Contratto 1.7x: la pagina Studio inventata, pratiche sulle cartelle delle sessioni della demo. */
  studio: StudioPage
  /** Contratto 1.72: il passaggio intero che l'op `diff` dà per «Mostra tutto», per «sessione/id della voce». */
  fullDiffs: Record<string, StepDiff>
}

/** Il mondo della demo all'istante `now` (epoch s). */
export function world(now: number): World {
  const base = decodeState(dress(stateRaw))
  const d = now - base.ts
  const sh = <T extends number | null | undefined>(x: T): T => (x == null ? x : ((x + d) as T))
  const stopLogin = { reason: en ? 'Log in to the payment sandbox and send me the code' : 'Fai il login nella sandbox dei pagamenti e mandami il codice', at: now - 420 }
  const stopOk = { text: en ? 'ok to publish the post' : 'ok alla pubblicazione del post', at: now - 240 }
  const sessions: Session[] = [
    { id: 'm', name: 'master', account: 'personal', account_kind: 'personal', project: 'personal/control-room', state: 'idle', since: now - 3600, context: 34, attached: true,
      model: { id: 'claude-opus-5[1m]', label: 'Opus 5' }, effort: 'high', suggestion: 'commit the README changes and open a PR', handoff: '/supervisor:handoff --here' },
    ...base.sessions.map((s): Session => {
      const x: Session = { ...s, since: s.since + d, turn_started: sh(s.turn_started), question: s.question ? { ...s.question, asked_at: s.question.asked_at + d } : s.question,
        outcome: s.outcome ? { ...s.outcome, at: s.outcome.at + d } : s.outcome, goal: s.goal ? { ...s.goal, since: s.goal.since + d } : s.goal,
        advice: s.advice ? { ...s.advice, at: s.advice.at + d } : s.advice,
        // Contratto 1.72: i conteggi della pillola sono di due minuti fa (nella fixture `at` non segue `ts`).
        changes: s.changes ? { ...s.changes, at: now - 120 } : s.changes }
      // Contratto 1.68 nella demo (FakeTransport.stopped): storefront ferma per te con un login, blog con un ok alla fine del
      // turno. Blog è seguita e al 64 % di contesto: si vedono «ha finito» e la proposta «handoff, poi /clear».
      if (x.name === 'storefront') return { ...x, needs_you: stopLogin }
      if (x.name === 'blog') return { ...x, turn_blocking: stopOk, followed: true, context: 64 }
      return x
    }),
  ]
  const state: State = {
    ...base, ts: now, published_at: now, sessions,
    // La pagina Studio (contratto 1.7x) finché le fixture del relay non hanno l'op.
    ops: base.ops?.includes('studio') ? base.ops : [...(base.ops ?? []), 'studio'],
    quota: Object.fromEntries(Object.entries(base.quota).map(([k, q]) => [k, { ...q, reset_h5: q.h5 != null && q.reset_h5 != null ? now + WINDOW_LEFT_S : sh(q.reset_h5), reset_w7: sh(q.reset_w7) }])),
    projects: base.projects.map(p => ({ ...p, last_used: sh(p.last_used) })),
    approvals: base.approvals.map(a => ({ ...a, requested_at: a.requested_at + d })),
    devices: base.devices?.map(x => ({ ...x, seen: sh(x.seen) })) ?? null,
    night: { ...base.night, items: base.night.items?.map(i => ({ ...i, added: i.added + d, started: sh(i.started) })) ?? null },
    // Il recap di oggi, con un progetto senza sessione: così «Da riprendere» propone il suo prossimo passo.
    recap: { date: isoDay(now), items: [...base.recap.items, { project: 'docs-site', done: en ? 'API pages drafted' : 'Bozze delle pagine API', next: en ? 'Review the API pages' : 'Rivedi le pagine API' }] },
    attention: [...(base.attention ?? []).map(a => ({ ...a, since: a.since + d })),
      { kind: 'needs_you', session: 'storefront', ref: 'storefront', title: stopLogin.reason, since: stopLogin.at },
      { kind: 'turn_blocking', session: 'blog', ref: 'blog', title: stopOk.text, since: stopOk.at },
    ].sort((a, b) => a.since - b.since),
  }
  state.projects = [...state.projects, { path: '/home/demo/workspaces/personal/docs-site', name: 'docs-site', account: 'personal', last_used: now - 26 * 3600 }]

  // Gli eventi della fixture con lo stesso spostamento dello stato: nessuno nel futuro, nessuna risposta alla domanda ancora
  // aperta, nessun limite di quota (l'account personale è all'11 %) e nessun turno fallito di una sessione al lavoro.
  const open = new Set(sessions.filter(s => s.question).map(s => s.name))
  const fixture = (JSON.parse(dress(eventsRaw)) as Event[]).map(e => ({ ...e, ts: e.ts + d, reset_at: e.reset_at != null ? e.reset_at + d : e.reset_at }))
    .filter(e => e.ts <= now && e.kind !== 'quota_limit' && e.kind !== 'turn_failed' && !(e.kind === 'answered' && open.has(e.session ?? '')))
    .map(e => (e.kind === 'question' ? { ...e, ts: sessions.find(s => s.name === e.session)?.question?.asked_at ?? e.ts } : e))
  // Un giro di eventi lungo la giornata per le colonne di «Oggi» (FakeTransport.demoEvents).
  const names = base.sessions.map(s => s.name)
  const kinds = ['outcome', 'launched', 'outcome', 'launched'] as const
  const spread: Event[] = Array.from({ length: 14 }, (_, i) => {
    const k = i + 1, name = names[k % names.length]
    return { key: `demo-${k}`, kind: kinds[k % kinds.length], session: name, ts: now - k * 47 * 60 - 3600, title: name }
  })
  const events = [...fixture, ...spread].sort((a, b) => b.ts - a.ts)

  const t0 = now - 600
  const said = (id: string, text: string, at: number, origin = 'pc'): TranscriptEntry => ({ id, role: 'user', text, at, origin })
  const claude = (id: string, text: string, at: number, extra: Partial<TranscriptEntry> = {}): TranscriptEntry => ({ id, role: 'assistant', text, at, ...extra })
  const tool = (id: string, name: string, text: string, at: number, extra: Partial<TranscriptEntry> = {}): TranscriptEntry => ({ id, role: 'tool', tool: name, text, at, ...extra })
  const numbers = en ? 'Give me the numbers of the post' : 'Dammi i numeri del post'
  const table = en
    ? 'The files touched:\n\n| file | lines |\n|---|---|\n| edge-caching.md | 336 |\n| bench.csv | 47 |\n\nThe numbers\n\n| what | now | at noon |\n|---|---|---|\n| post on edge caching | 223 views, 4 reposts | 121 views, 1 repost |\n| new followers | 1 | — |\n\nEsito: numbers of the post on edge caching collected\nProssimi: publish the post · share it on X'
    : 'I file toccati:\n\n| file | righe |\n|---|---|\n| edge-caching.md | 336 |\n| bench.csv | 47 |\n\nI numeri\n\n| cosa | ora | a mezzogiorno |\n|---|---|---|\n| post sull\'edge caching | 223 visualizzazioni, 4 repost | 121 visualizzazioni, 1 repost |\n| nuovi follower | 1 | — |\n\nEsito: numeri del post sull\'edge caching raccolti\nProssimi: pubblica il post · condividilo su X'
  // Contratto 1.72: i diff veri della fixture del contratto (changes-sample.json) nei passaggi di storefront, e su blog un CSV
  // lungo, tagliato a 200 righe come fa il PC, per «Mostra tutto».
  const sample = decodeChanges(changesRaw)
  const stepOf = (f: ChangedFile, kind: string): StepDiff => ({ path: `${sample.repo}/${f.path}`, kind, added: f.added, removed: f.removed, hunks: f.hunks, cut: false })
  const benchPages = ['home', 'product', 'category', 'cart']
  const bench = ['page,run,before_ms,after_ms', ...Array.from({ length: 260 }, (_, i) => `${benchPages[i % 4]},${Math.floor(i / 4) + 1},${380 + ((i * 37) % 160)},${34 + ((i * 11) % 12)}`)]
  const benchHunk = (n: number) => ({ old_start: 0, old_lines: 0, new_start: 1, new_lines: bench.length, lines: bench.slice(0, n).map(l => `+${l}`) })
  const benchStep: StepDiff = { path: '/w/blog/bench/results.csv', kind: 'create', added: bench.length, removed: 0, hunks: [benchHunk(200)], cut: true }
  const fullDiffs: Record<string, StepDiff> = { 'blog/w1': { ...benchStep, hunks: [benchHunk(bench.length)], cut: false } }
  const transcripts: Record<string, TranscriptEntry[]> = {
    master: [
      said('u1', en ? 'Launch payments-api on phase 2.2' : 'Lancia payments-api sulla fase 2.2', now - 660, 'phone'),
      claude('a1', en
        ? 'payments-api launched on phase 2.2. Local commits only: push and release with your ok.\n\nEsito: Phase 2.2 started on payments-api\nProssimi: review the refund flow · try the checkout live'
        : 'Lanciata payments-api sulla fase 2.2. Solo commit locali: push e release con il tuo ok.\n\nEsito: Fase 2.2 avviata su payments-api\nProssimi: rivedi il flusso dei rimborsi · prova il checkout dal vivo', now - 600),
    ],
    'payments-api': [
      said('u1', 'Prepare the 2.8.0 deploy of the refund endpoint and wait for my ok', t0 - 300),
      tool('a1', 'Bash', 'npm run build && npm run test:staging', t0 - 250, { note: 'Build and test on staging' }),
      tool('a2', 'Read', 'docs/releases/2.8.0.md', t0 - 200),
      claude('a3', 'The build is ready and **staging is green**: refunds, webhooks and checkout pass. I deploy 2.8.0 only with your ok.', t0 - 120, { turn: { started: t0 - 300, ended: t0 - 118, out: 412 } }),
    ],
    storefront: [
      said('u1.0', 'Refactor the Stripe webhook, then run the checkout tests', t0),
      claude('a1.0', "I'll read the webhook handler first.", t0 + 5),
      tool('a1.1', 'Read', 'src/webhooks/stripe.ts', t0 + 5),
      tool('a3.0', 'Bash', 'grep -rn constructEvent src/', t0 + 20, { note: 'Find where the events are verified', error: true }),
      claude('a4.0', 'The signature check lived in two places: I moved it into the handler and wrote a test for a forged event.', t0 + 40, { turn: { started: t0, ended: t0 + 45, in: 4020, out: 130 } }),
      tool('a6.0', 'Write', 'docs/checkout-before.png', t0 + 152, { files: [{ path: '/w/storefront/docs/checkout-before.png', mime: 'image/png', size: 35_449 }] }),
      tool('a7.0', 'SendUserFile', 'Checkout test report', t0 + 155, { files: [{ path: '/w/storefront/docs/checkout-tests.pdf', mime: 'application/pdf', size: 1_872 }] }),
      said('p1.0', 'Also run the gift card flow on staging', t0 + 158, 'phone'),
      tool('a8.0', 'Edit', '/home/dev/storefront/src/checkout/totals.ts', t0 + 160, { diff: stepOf(sample.files[0], 'edit') }),
      tool('a8.1', 'Write', '/home/dev/storefront/tests/checkout/totals.test.ts', t0 + 170, { diff: stepOf(sample.files[1], 'create') }),
    ],
    blog: [
      tool('a0', 'Write', 'posts/edge-caching.md', t0 - 120, { files: [{ path: '/w/blog/posts/edge-caching.md', mime: 'text/markdown', size: 547 }, { path: '/w/blog/bench/bench-data.zip', mime: 'application/zip', size: 166 }, { path: '/w/blog/posts/edge-caching-preview.html', mime: 'text/html', size: 868 }] }),
      said('u1', numbers, t0, 'phone'),
      tool('w1', 'Write', 'bench/results.csv', t0 + 20, { diff: benchStep }),
      claude('a1', table, t0 + 60, { turn: { started: t0 + 5, ended: t0 + 60, out: 288 } }),
    ],
  }
  const mine: [Sent, Status][] = [
    [{ id: 'c1', session: 'storefront', text: 'Also run the gift card flow on staging', sentAt: t0 + 156 }, 'working'],
    [{ id: 'c2', session: 'blog', text: numbers, sentAt: t0 - 2 }, 'done'],
  ]

  // Le letture della quota delle 5 ore che l'app avrebbe registrato nelle ultime tre ore (FakeTransport.demoQuotaSamples).
  const samples: Record<string, Sample[]> = Object.fromEntries(Object.entries(state.quota).filter(([, q]) => q.h5 != null && q.reset_h5 != null).map(([k, q]) => {
    const from = Math.max(q.reset_h5! - 5 * 3600, now - 3 * 3600) + 600, steps = 9
    return [k, Array.from({ length: steps + 1 }, (_, i) => ({ ts: Math.round(from + (now - from) * i / steps), pct: Math.floor(q.h5! * (i + 1) / (steps + 1)) }))]
  }))

  // La notte appena passata: il rapporto inventato del contratto 1.44 con i nomi della demo, finito alle 02:36 di oggi; la
  // domanda che aspetta è quella aperta adesso.
  const raw = JSON.parse(nightRaw) as NightReport & { generated_at: number; window: { end: number } }
  const q = sessions.find(s => s.name === 'payments-api')?.question
  const end = (() => { const x = new Date(now * 1000); x.setHours(2, 36, 0, 0); const e = Math.round(x.getTime() / 1000); return e > now ? e - 86400 : e })()
  const nd = end - raw.window.end
  const shift = (v: unknown, key = ''): unknown => {
    if (typeof v === 'number' && v > 1e9 && /at$|^start$|^end$|^floor$|_at$/.test(key)) return v + nd
    if (Array.isArray(v)) return v.map(x => shift(x, key))
    if (v && typeof v === 'object') return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, shift(x, k)]))
    return v
  }
  const nightText = dress(JSON.stringify(en ? nightEn(raw) : raw))
    .split(en ? 'Shall I publish release 1.4 now?' : 'Pubblico la release 1.4 adesso?').join(q?.text ?? '')
  const night = { ...(shift(JSON.parse(nightText)) as NightReport), date: isoDay(end) } as NightReport
  const nq = (night as unknown as { attention?: { questions?: { session: string; id: string; options: unknown; asked_at: number }[] } }).attention?.questions
  if (nq && q) for (const x of nq) if (x.session === 'payments-api') { x.id = q.id; x.options = q.options.map(o => ({ n: o.n, label: o.label })); x.asked_at = q.asked_at }
  state.night = { ...state.night, report: { date: night.date, generated_at: (night as unknown as { generated_at: number }).generated_at } }

  // Contratto 1.46: l'agenda inventata, un gruppo per ognuno di chi deve muoversi (come FakeTransport).
  const agenda: AgendaPage = { more: false, owner: 'you', rows: [
    { state: 'aperto', scope: 'agenzia', blocks: 'you', title: en ? 'Confirm the 6 client ids with a candidate' : 'Conferma i 6 id cliente con un candidato', ref: '.claude/to-decide-client-id.md',
      detail: en ? 'Six ids have a likely client from the invoices.\nConfirm them one by one, then the import can run.' : 'Sei id hanno un cliente probabile dalle fatture.\nConfermali uno per uno, poi l\'importazione può partire.', key: 'd1' },
    { state: 'aperto', scope: 'personale', blocks: 'you', title: en ? 'Renew the domain of the docs site' : 'Rinnova il dominio del sito della documentazione', ref: 'docs-site', key: 'd2' },
    { state: 'aperto', scope: 'personale', blocks: 'claude', title: en ? 'Move the docs site to the new host' : 'Sposta il sito della documentazione sul nuovo host', ref: 'docs-site', key: 'd3' },
    { state: 'aperto', scope: 'postazione', blocks: 'claude', title: en ? 'Clear the orphan plugin caches' : 'Pulisci le cache orfane dei plugin', ref: en ? 'after restarting every session' : 'dopo aver riavviato le sessioni', key: 'd4' },
    { state: 'sospeso', scope: 'agenzia', blocks: 'terzi', title: en ? 'Staging of storefront not reachable from the CLI' : 'Lo staging di storefront non si raggiunge dalla CLI', ref: '', until: isoDay(now + 5 * 86400), key: 'd5' },
    { state: 'aperto', scope: 'agenzia', blocks: 'terzi', title: en ? 'Client answer on the domain' : 'Risposta del cliente sul dominio', ref: '', key: 'd6' },
    { state: 'fatto', scope: 'postazione', blocks: 'nessuno', title: en ? 'Backups of the workstation every night' : 'Backup della postazione ogni notte', ref: 'crontab', key: 'd7' },
  ] }

  // La pagina Studio (spec del 18/10): tre pratiche sulle cartelle di storefront, blog e pricing-page, una scadenza in avviso.
  const dir = (n: string) => state.sessions.find(x => x.name === n)?.project ?? n
  const studio: StudioPage = { error: '',
    cases: [
      { id: 'c1', name: en ? 'Storefront: supplier contract' : 'Storefront: contratto fornitore', project: dir('storefront') },
      { id: 'c2', name: en ? 'Blog: image licences' : 'Blog: licenze delle immagini', project: dir('blog') },
      { id: 'c3', name: en ? 'Pricing: VAT check' : 'Prezzi: verifica IVA', project: dir('pricing-page') },
    ],
    deadlines: [
      { uid: 's1', summary: en ? 'Sign the supplier contract' : 'Firma del contratto fornitore', date: isoDay(now + 2 * 86400), days_left: 2, due: true, case: 'c1' },
      { uid: 's2', summary: en ? 'Renew the image licences' : 'Rinnovo delle licenze delle immagini', date: isoDay(now + 20 * 86400), days_left: 20, due: false, case: 'c2' },
    ],
    outcomes: [
      { project: dir('storefront'), session: 'storefront', at: now - 3600, short: en ? 'Contract draft checked, two clauses to discuss' : 'Bozza del contratto controllata, due clausole da discutere' },
      { project: dir('pricing-page'), session: 'pricing-page', at: now - 3 * 86400, short: en ? 'VAT rates aligned' : 'Aliquote IVA allineate' },
    ] }

  // La cronologia di oggi (contratto 1.29) per i dettagli della plancia.
  const d0 = now - 5 * 3600
  const timeline: TimelinePage = { since: d0, sessions: [
    { session: 'payments-api', live: true, events: [{ at: d0 + 600, kind: 'prompt', text: 'Prepare the 2.8.0 deploy', ref: 'payments-api' }, { at: d0 + 4000, kind: 'commit', text: 'bump version to 2.8.0', ref: '4c1e9a2' },
      { at: d0 + 9000, kind: 'test', text: 'refund endpoint tests', ok: true }, { at: d0 + 15000, kind: 'question', text: q?.text ?? '' }] },
    { session: 'storefront', live: true, events: [{ at: d0 + 1200, kind: 'commit', text: 'fix the cart totals', ref: '4be1c2a' }, { at: d0 + 2400, kind: 'prompt', text: 'ok, run the checkout tests', ref: 'storefront' },
      { at: d0 + 6000, kind: 'test', text: 'checkout tests', ok: true }] },
  ] }

  return { state, events, transcripts, mine, samples, night, agenda, timeline, fullDiffs, studio }
}

/**
 * Quello che il relay risponderebbe a `search` (contratto 1.27) cercando nelle conversazioni di prova: un punto trovato per
 * voce, la riga intorno (fino a 160 caratteri) e la parte trovata.
 */
export function demoSearch(w: Pick<World, 'state' | 'transcripts'>, q: string): SearchPage {
  const needle = q.toLowerCase()
  const hits = Object.entries(w.transcripts).flatMap(([session, entries]) => entries.flatMap(e => {
    const line = (e.text ?? '').split('\n').find(l => l.toLowerCase().includes(needle))
    if (!line) return []
    const snippet = line.slice(0, 160)
    const i = snippet.toLowerCase().indexOf(needle)
    return i < 0 ? [] : [{ session, live: w.state.sessions.some(s => s.name === session && s.state !== 'gone'), entry: e.id, role: e.role, at: e.at ?? null, snippet, match: [i, i + needle.length] }]
  }))
  return { hits, more: false }
}

/** Il rapporto della notte in inglese: le stesse voci, i testi tradotti (i titoli «nome — prompt» compresi). */
const NIGHT_EN: Record<string, string> = {
  'vado a dormire, prosegui tu e segui le sessioni': "I'm going to sleep, carry on and follow the sessions",
  'Pubblico la release 1.4 adesso?': 'Shall I publish release 1.4 now?',
  'domanda: Pubblico la release 1.4 adesso?': 'question: Shall I publish release 1.4 now?',
  'Migrazione del registro committata in locale, nessuna release': 'Ledger migration committed locally, no release',
  'Porta a termine la migrazione del registro: committala in locale e non fare release finché non torno.': "Finish the ledger migration: commit it locally and don't release until I'm back.",
  'Checkout rifatto, test verdi': 'Checkout redone, tests green',
  'fermo prima del piano: la specifica non si legge.': 'stopped before the plan: the spec cannot be read.',
  'Dati del registro dei pagamenti per la pagina mensile.': 'Payment ledger data for the monthly page.',
  'dati fatti e verificati, 3 commit sul ramo notte/registro.': 'data done and checked, 3 commits on the night/ledger branch.',
  'Riordino delle note di campo per cartella e data, con un indice in testa.': 'Sort the field notes by folder and date, with an index at the top.',
  'Migrazione del registro': 'Ledger migration', 'Pagina mensile': 'Monthly page', 'Esportazione CSV': 'CSV export',
  'Indice delle note per data': 'Index of the notes by date',
}
const CHECKOUT_EN = "First run of the night job on the checkout: redo the cart and the integration tests without touching production. Tonight nobody answers questions: write the choices you cannot make alone in docs/night/questions.md and stop before any step that needs them. Read the spec in docs/spec/checkout.md, redo the cart following the spec, then the tests"
function nightEn(v: unknown): unknown {
  if (typeof v === 'string') {
    if (NIGHT_EN[v]) return NIGHT_EN[v]
    if (v.startsWith('Prima esecuzione del lavoro notturno')) return CHECKOUT_EN
    const dash = v.indexOf(' — ')
    if (dash > 0) {
      const rest = v.slice(dash + 3)
      const tr = NIGHT_EN[rest] ?? (rest.startsWith('Prima esecuzione') ? CHECKOUT_EN.slice(0, CHECKOUT_EN.lastIndexOf(' ', 160)) : null)
      if (tr) return `${v.slice(0, dash).replace(/-notte$/, '-night')} — ${tr}`
    }
    return v.replace(/-notte\b/g, '-night').replace(/^notte\//, 'night/')
  }
  if (Array.isArray(v)) return v.map(nightEn)
  if (v && typeof v === 'object') return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, nightEn(x)]))
  return v
}
