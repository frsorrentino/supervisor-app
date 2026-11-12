import stateRaw from '../../../contract/state-1-question.json?raw'
import monitorsMockup from '../../../core/src/test/resources/monitors-mockup.json'
import type { Approval, MonitorCard, MonitorsPage } from './contract'
import { DEFAULT_CATS, encode as encodeCats, toTop } from './homeCats'
import idleRaw from '../../../contract/state-2-idle.json?raw'
import changesRaw from '../../../contract/changes-sample.json?raw'
import eventsRaw from '../../../contract/events-sample.json?raw'
import nightRaw from '../../../contract/night-report-sample.json?raw'
import { decodeChanges, decodeState } from './contract'
import type { AgendaPage, ChangedFile, Event, SearchPage, Session, State, StepDiff, TimelinePage, TranscriptEntry, PrimaRaccoltaPage, SeguitiPage, ResultItem, ResultsPage } from './contract'
import type { Sent, Status } from './chatRules'
import type { Sample } from './quotaHistory'
import type { Commitment, DecisionsPage, WaitFile, WaitItem, KpiData, PluginsPage, ProvePage, SystemStatus, TimelineEvent } from './contract'
import { midnight } from './kpi'
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
  ['Review the seeds', 'Review the seed data and the new orders admin page'],
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

/** Il risultato dell'op `plugins`: gli stessi due di `state.plugins`, più gli aggiornati e le skill. */
export const pluginsDemo = (now: number): PluginsPage => ({ checked_at: now - 2400, accounts: [{ account: 'personal', plugins: [
  { id: 'docs-helper@demo', name: 'docs-helper', installed: '1.2.0', latest: '1.3.0', update: true },
  { id: 'release-notes@demo', name: 'release-notes', installed: '0.9.1', latest: '0.9.4', update: true },
  { id: 'test-runner@demo', name: 'test-runner', installed: '2.0.0', latest: '2.0.0', update: false },
], skills: [{ name: 'changelog', version: '1.0' }] }] })

/** Contratto 1.99: gli elenchi dietro le due righe della home, solo nomi della demo. */
export const proveDemo = (now: number): ProvePage => ({ at: now, n: 2, builds: 1, ko: [], red: [], groups: [{ build: 'demo-rc-2', n: 2, at: now - 3600, tasks: ['demo-cart', 'demo-users'], prove: [
  { id: 1, task: 'demo-cart', short: en ? 'Cart totals' : 'Totali del carrello', build: 'demo-rc-2', dove: 'box-esiti', fai: en ? 'Open today\'s outcomes' : 'Apri gli esiti di oggi', atteso: en ? 'The cart outcome is under Done' : 'L\'esito del carrello sta sotto Fatto', at: now - 3600, wid: 'w00000000d4' },
  { id: 2, task: 'demo-users', short: en ? 'Users table' : 'Tabella utenti', build: 'demo-rc-2', dove: 'lavori', fai: en ? 'Open Work' : 'Apri Lavori', atteso: en ? 'The users table is in progress' : 'La tabella utenti è in corso', at: now - 3600 },
] }] })
/** Contratto 1.100: la prima raccolta a un passo dalla fine, dati del mondo della demo; manca il sì sulle sessioni passate. */
export const primaDemo = (now: number): PrimaRaccoltaPage => ({ at: now, status: 'ask', done: 9, total: 10, consent: null, cost: 0,
  ask_text: en ? 'May I read your past sessions of the last 14 days, to count them and pick up what they left open? The data stays on this machine; it reaches the phone encrypted.' : 'Posso leggere le sessioni passate degli ultimi 14 giorni, per contarle e riprendere quello che hanno lasciato aperto? I dati restano su questa macchina; al telefono arrivano cifrati.',
  sections: [
    { key: 'machine', dove: 'usage', n: 1, text: en ? 'This PC: 16 GB, 2 sessions at work' : 'Questo PC: 16 GB, 2 sessioni al lavoro' },
    { key: 'hosts', dove: 'settings', n: 0, why: en ? 'No other machine is connected.' : 'Nessuna altra macchina collegata.', when: en ? 'It fills when you add one from Settings.' : 'Si riempie quando ne aggiungi una dalle Impostazioni.' },
    { key: 'quota', dove: 'usage', n: 1, text: en ? '1 account, 22% of the 5 hours' : '1 account, 22% delle 5 ore' },
    { key: 'plugins', dove: 'settings', n: 12, text: en ? '12 plugins, 2 to update, 30 skills' : '12 plugin, 2 da aggiornare, 30 skill' },
    { key: 'projects', dove: 'launch', n: 6, text: en ? '6 projects' : '6 progetti' },
    { key: 'live', dove: 'home', n: 4, text: en ? '4 open sessions' : '4 sessioni aperte' },
    { key: 'sessions', dove: 'search', n: 0, why: en ? 'Past sessions are read only with your yes.' : 'Le sessioni passate si leggono solo col tuo sì.', when: en ? 'It fills as soon as you answer.' : 'Si riempie appena rispondi.' },
    { key: 'suggestions', dove: 'home', n: 2, text: en ? '2 suggestions from the master' : '2 suggerimenti della master' },
    { key: 'night', dove: 'night', n: 1, text: en ? '1 job for tonight' : '1 lavoro per stanotte' },
    { key: 'recap', dove: 'box-esiti', n: 3, text: en ? '3 outcomes today' : '3 esiti di oggi' },
  ],
  first_round: { at: null, cost: en ? "The master's first round uses your plan's quota: usually a small part of the 5 hours. It starts only if you touch it." : 'Il primo giro della master consuma la quota del tuo piano: di solito una piccola parte delle 5 ore. Parte solo se lo tocchi.' } })
/** Contratto 1.102: quello che resta fuori dal codice, dati del mondo della demo. */
export const seguitiDemo = (now: number): SeguitiPage => ({ at: now,
  rimasto: [
    { product: 'storefront', key: 'demo', title: en ? 'Site demo' : 'Demo del sito', where: en ? 'the demo on the site' : 'la demo sul sito', stale: true, why: 'paths:3', session: 'storefront', aligned_version: 'v2.3.0', aligned_at: now - 12 * 86400, task: null },
    { product: 'storefront', key: 'store', title: en ? 'Store listing' : 'Scheda dello store', where: en ? 'the store page' : 'la pagina dello store', stale: null, why: 'no_git', session: null, aligned_version: null, aligned_at: null, task: null },
    { product: 'blog', key: 'readme', title: 'README', where: 'README.md', stale: false, why: null, session: 'blog', aligned_version: 'v1.8.0', aligned_at: now - 2 * 86400, task: null },
  ],
  altrove: [
    { id: 'demo-dump', text: en ? 'Public dumps in the web root' : 'Dump pubblici nella cartella del sito', complete: true, at: now - 3600, total: 6, done: 6, positive: ['blog'], errors: [], running: false, task: 'altrove-demo-dump' },
    { id: 'demo-log', text: en ? 'Debug log left on' : 'Log di debug rimasto acceso', complete: false, at: now - 7200, total: 6, done: 4, positive: [], errors: ['admin-panel'], running: true, task: null },
  ],
  promemoria: [
    { id: 'demo-store', text: en ? 'When the store approves, the amber icon' : "Quando lo store approva, l'icona ambra", created_at: now - 86400, expires_at: null, at_time: null },
    { id: 'demo-domain', text: en ? 'Renew the domain' : 'Rinnova il dominio', created_at: now - 86400, expires_at: null, at_time: now + 5 * 86400 },
  ],
  promemoria_scattati: [{ id: 'demo-cert', text: en ? 'Renew the certificate' : 'Rinnova il certificato', outcome: 'fired', at: now - 7200 }],
})
/**
 * L'op `kpi` nella demo (contratto 1.96-1.110, app-demo-sempre-allineata-03-11): i compiti, i controlli della settimana, i turni
 * di oggi, la salute (disco di questo computer, tempi dei comandi, win al lavoro), i suggerimenti e le attese. Prima la demo non
 * rispondeva e i riquadri che ne vivono (il quarto KPI di Sistema, il disco) restavano vuoti.
 */
export const kpiDemo = (now: number): KpiData => {
  const day = (k: number) => new Date((now - k * 86400) * 1000).toISOString().slice(0, 10)
  const week = [6, 5, 4, 3, 2, 1, 0]
  return {
    at: now,
    kpi_tasks: { open: 6, waiting_you: 2, stalled: 1, closed_today: 3, tasks: [
      { task: 'shop-checkout', short: 'Checkout', state: 'running', age_s: 5400, attempts: 1, attempts_max: 3, stalled: false, who: 'storefront' },
      { task: 'blog-post', short: 'Post', state: 'paused', age_s: 7200, attempts: 1, attempts_max: 3, stalled: false, who: 'blog' },
      { task: 'docs-migrate', short: 'Docs', state: 'assigned', age_s: 90000, attempts: 2, attempts_max: 3, stalled: true, who: null },
    ] },
    kpi_checks_week: week.map((k, i) => ({ date: day(k), green: 3 + (i % 3), red: i === 4 ? 1 : 0 })),
    kpi_turns_today: [
      { session: 'storefront', turns: [{ start: now - 5400, end: now - 4800, outcome: 'ok', tokens: 41000, cache_read: 380000 }, { start: now - 1500, end: now - 420, outcome: 'question', tokens: 22000, cache_read: 210000 }] },
      { session: 'blog', turns: [{ start: now - 3000, end: now - 240, outcome: 'question', tokens: 18000, cache_read: 160000 }] },
    ],
    kpi_health: {
      local_disk: { free: 84e9, total: 256e9 }, commands: { p50_s: 0.8, p95_s: 2.4, n: 146 },
      latency: { p50_s: 1.1, p95_s: 3.2, max_s: 9.5, n: 146 }, sessions_here: 5, sessions_max: 12,
      hosts: [{ name: 'win', up: true, cpu: 22, ram_pct: 58, disk_pct: 61, working: true, working_why: ['job'] }],
    },
    kpi_suggestions_7d: { accepted: 6, discussed: 2, later: 3, never: 1, expired: 2 },
    kpi_waits: { now: [{ session: 'storefront', kind: 'needs_you', since: now - 420 }], week: week.map((k, i) => ({ date: day(k), median_min: 4 + i, n: 3 + (i % 2) })) },
  }
}

export const decisionsDemo = (now: number): DecisionsPage => ({ at: now, n: 3, here: 2, tasks: 4, snoozed: 0, groups: [
  { key: 'demo-a', title: en ? 'Cover image for the post' : 'Immagine di copertina del post', cause: null, unblocks: ['demo-cover', 'demo-docs'], weight: 2, where: 'here', since: now - 1200, note: null, refs: ['demo-cover'], items: [{ wid: 'w00000000a1', kind: 'pause', task: 'demo-cover', short: en ? 'Cover image' : 'Immagine di copertina', session: 'blog', reason: en ? 'Send me the cover image' : 'Mandami l\'immagine di copertina', since: now - 1200, where: 'here', asks: 'other' }] },
  { key: 'demo-c', title: en ? 'Try the new checkout on the phone' : 'Prova il nuovo checkout sul telefono', cause: null, unblocks: ['demo-checkout'], weight: 1, where: 'here', since: now - 300, note: null, refs: ['demo-checkout'], items: [{ kind: 'pause', task: 'demo-checkout', short: en ? 'New checkout' : 'Nuovo checkout', session: 'storefront', reason: en ? 'Waits for you to try build 0.4.12 on the phone' : 'Aspetta la tua prova della versione 0.4.12 sul telefono', since: now - 300, where: 'here', asks: 'other' }] },
  { key: 'demo-b', title: en ? 'Log in to the payment sandbox' : 'Login nella sandbox dei pagamenti', cause: null, unblocks: ['demo-cart'], weight: 1, where: 'pc', since: now - 420, note: null, refs: [], items: [{ kind: 'needs_you', task: null, session: 'storefront', reason: en ? 'Log in to the sandbox' : 'Fai il login nella sandbox', since: now - 420, where: 'pc', asks: 'pc' }] },
] })

export type World = {
  state: State; events: Event[]; transcripts: Record<string, TranscriptEntry[]>; mine: [Sent, Status][]; samples: Record<string, Sample[]>
  night: NightReport; agenda: AgendaPage; timeline: TimelinePage
  /** Contratto 1.72: il passaggio intero che l'op `diff` dà per «Mostra tutto», per «sessione/id della voce». */
  fullDiffs: Record<string, StepDiff>
}

/** L'ultimo messaggio di blog: la proposta e le scelte, come le scrive una sessione (app-attesa-dettagli-02-11). */
const blogOutcome = (now: number) => en
  ? { short: 'post ready, only publishing is left', at: now - 240, full: 'I re-read the post and fixed the links: the draft is in content/posts/release-2-8.md and the cover is already in the repo.\nShall I publish it now, or schedule it for tomorrow morning when the newsletter goes out?\nEsito: post ready, only publishing is left\nProssimi: !ok to publish the post · schedule it for tomorrow 9:00 · send me the preview first' }
  : { short: 'post pronto, manca solo la pubblicazione', at: now - 240, full: "Ho riletto il post e sistemato i link: la bozza è in content/posts/release-2-8.md e la copertina è già nel repo.\nLo pubblico adesso o lo programmo per domattina, quando parte la newsletter?\nEsito: post pronto, manca solo la pubblicazione\nProssimi: !ok alla pubblicazione del post · programmalo per domani alle 9 · mandami prima l'anteprima" }

/** Da guardare nella demo (contratto 1.106): una schermata in due versioni con la nota di Franz, una pagina e un rapporto in PDF. */
export function resultsDemo(now: number): ResultsPage {
  const img = { path: '/home/dev/storefront/checkout-before.png', mime: 'image/png', size: 182000 }
  const item = (x: Partial<ResultItem> & Pick<ResultItem, 'id' | 'session' | 'title' | 'kind' | 'path' | 'at'>): ResultItem => ({ mime: null, size: null, duration_s: null, version: 1, versions: 1, changed: null, seen_at: null, caption: null, files: [], notes: [], replaced: false, ...x })
  return { n_unseen: 3, more: false, groups: [
    { title: en ? 'Checkout page' : 'Pagina del checkout', project: 'work/storefront', session: 'storefront', kind: 'image', at: now - 900, versions: 2, unseen: true, items: [
      item({ id: 'demo-r2', session: 'storefront', title: en ? 'Checkout page' : 'Pagina del checkout', kind: 'image', ...img, at: now - 900, version: 2, versions: 2, changed: en ? 'Pay button above the fold' : 'il tasto Paga sale sopra la piega', caption: en ? 'Checkout page, version 2: Pay button above the fold' : 'Pagina del checkout, versione 2: il tasto Paga sale sopra la piega' }),
      item({ id: 'demo-r1', session: 'storefront', title: en ? 'Checkout page' : 'Pagina del checkout', kind: 'image', ...img, at: now - 7200, version: 1, versions: 2, seen_at: now - 6000, notes: [{ at: now - 5900, text: en ? 'The Pay button is too low' : 'Il tasto Paga è troppo in basso', pos_s: null }] }),
    ] },
    { title: en ? 'Cache preview' : 'Anteprima della cache', project: 'personal/blog', session: 'blog', kind: 'mockup', at: now - 1800, versions: 1, unseen: true, items: [
      item({ id: 'demo-r3', session: 'blog', title: en ? 'Cache preview' : 'Anteprima della cache', kind: 'mockup', path: '/home/dev/blog/edge-caching-preview.html', mime: 'text/html', size: 24000, at: now - 1800, files: [img] }),
    ] },
    { title: en ? 'Payments test report' : 'Rapporto dei test dei pagamenti', project: 'work/payments-api', session: 'payments-api', kind: 'document', at: now - 3000, versions: 1, unseen: true, items: [
      item({ id: 'demo-r4', session: 'payments-api', title: en ? 'Payments test report' : 'Rapporto dei test dei pagamenti', kind: 'document', path: '/home/dev/payments-api/checkout-tests.pdf', mime: 'application/pdf', size: 88000, at: now - 3000 }),
    ] },
  ] }
}

/** Il mondo della demo all'istante `now` (epoch s). */
export function world(now: number): World {
  const base = decodeState(dress(stateRaw))
  const d = now - base.ts
  const sh = <T extends number | null | undefined>(x: T): T => (x == null ? x : ((x + d) as T))
  const stopLogin = { reason: en ? 'Log in to the payment sandbox and send me the code' : 'Fai il login nella sandbox dei pagamenti e mandami il codice', at: now - 420 }
  const stopOk = { text: en ? 'ok to publish the post' : 'ok alla pubblicazione del post', at: now - 240 }
  const blogSteps = en ? [{ text: 'ok to publish the post', blocking: true }, { text: 'schedule it for tomorrow 9:00', blocking: false }, { text: 'send me the preview first', blocking: false }]
    : [{ text: 'ok alla pubblicazione del post', blocking: true }, { text: 'programmalo per domani alle 9', blocking: false }, { text: "mandami prima l'anteprima", blocking: false }]
  const sessions: Session[] = [
    { id: 'm', name: 'master', account: 'personal', account_kind: 'personal', project: 'personal/control-room', state: 'idle', since: now - 3600, context: 34, attached: true,
      model: { id: 'claude-opus-5[1m]', label: 'Opus 5' }, effort: 'high', suggestion: 'commit the README changes and open a PR', handoff: '/supervisor:handoff --here' },
    ...base.sessions.map((s): Session => {
      const x: Session = { ...s, since: s.since + d, turn_started: sh(s.turn_started), question: s.question ? { ...s.question, asked_at: s.question.asked_at + d, kind: 'permission', what: en ? 'Publish the 2.8.0 deploy: ./deploy.sh --env production' : 'Pubblica il deploy 2.8.0: ./deploy.sh --env production', text: 'Do you want to proceed?' } : s.question,
        outcome: s.outcome ? { ...s.outcome, at: s.outcome.at + d } : s.outcome, goal: s.goal ? { ...s.goal, since: s.goal.since + d } : s.goal,
        advice: s.advice ? { ...s.advice, at: s.advice.at + d } : s.advice,
        // Contratto 1.72: i conteggi della pillola sono di due minuti fa (nella fixture `at` non segue `ts`).
        changes: s.changes ? { ...s.changes, at: now - 120 } : s.changes }
      // Contratto 1.68 nella demo (FakeTransport.stopped): storefront ferma per te con un login, blog con un ok alla fine del
      // turno. Blog è seguita e al 64 % di contesto: si vedono «ha finito» e la proposta «handoff, poi /clear».
      // Contratto 1.113 per sessione (app-demo-sempre-allineata-03-11): i conti di Tocca a te, che sommati fanno quelli di `tocca`.
      if (x.name === 'storefront') return { ...x, needs_you: stopLogin, tocca: { prove: 2, decidi: 2, guarda: 1, segui: 1 } }
      // app-attesa-dettagli-02-11: la ferma ha la sua proposta e tre scelte, così la scheda aperta mostra cosa si sceglie.
      if (x.name === 'blog') return { ...x, tocca: { prove: 0, decidi: 1, guarda: 0, segui: 0 }, turn_blocking: stopOk, followed: true, context: 64, outcome: blogOutcome(now), next_steps: blogSteps, heads_up: { text: en ? 'This conversation is getting long: a handoff would help' : 'Questa conversazione si sta allungando: conviene un handoff', seen_at: now - 780 } }
      return x
    }),
    // Al lavoro adesso: nella fixture l'unica in turno è ferma su un login, e il tabellone direbbe 0.
    { id: 'a', name: 'admin-panel', account: 'personal', account_kind: 'personal', project: 'personal/admin-panel', state: 'busy', since: now - 1500, turn_started: now - 1500, context: 41,
      tool: 'Bash npm test', model: { id: 'claude-sonnet-5', label: 'Sonnet 5' }, goal: { text: en ? 'New users table with filters' : 'Nuova tabella utenti con i filtri', since: now - 1500 } },
  ]
  // Lavori: due aspettano te e tre lavorano. L'ok della fixture è della release di storefront e sta nella sua scheda; la pausa di blog
  // chiede un'altra cosa rispetto al suo «ok alla pubblicazione», così nessuna scheda chiede la stessa cosa due volte.
  const task = (id: string, title: string, session: string, on: string, extra: Partial<Commitment> = {}): Commitment => ({
    task: id, title, short: title, session, state: on === 'person' ? 'awaiting_ok' : 'running', approved: on !== 'person', age_s: 3 * 3600, idle_s: 300, stalled: false, waits: { who: on === 'person' ? 'you' : session, on }, ...extra,
  })
  const commitments: Commitment[] = [
    ...base.approvals.map(a => task(a.task, a.title, 'storefront', 'person')),
    task('demo-cart-layout', en ? 'Cart on the phone' : 'Carrello sul telefono', 'storefront', 'person'),
    task('demo-cover', en ? 'Cover image' : 'Immagine di copertina', 'blog', 'person', { paused: { reason: en ? 'Send me the cover image' : 'Mandami l\'immagine di copertina', at: now - 1200 } }),
    task('demo-docs', en ? 'API pages' : 'Pagine API', 'payments-api', 'session'),
    task('demo-users', en ? 'Users table' : 'Tabella utenti', 'admin-panel', 'session'),
    task('demo-cart', en ? 'Cart totals' : 'Totali del carrello', 'storefront', 'session'),
  ]
  const state: State = {
    ...base, ts: now, published_at: now, sessions,
    // Contratto 1.75: win acceso e le tre pubblicazioni di state-2-idle, spostate su adesso come il resto.
    ...(() => { const idle = decodeState(dress(idleRaw)), di = now - idle.ts
      return { hosts_status: idle.hosts_status.map(h => ({ ...h, at: now - 40 })), published_today: idle.published_today.map(x => ({ ...x, at: x.at + di })) } })(),
    // La settimana personale coi suoi punti, sul ritmo pari a un terzo della settimana: «36% ora» e «ritmo pari» si cercavano lo
    // stesso posto, e il 100% cade prima del rinnovo (web-plancia-j2-ripasso-06-11).
    quota: Object.fromEntries(Object.entries(base.quota).map(([k, q]) => [k, { ...q, reset_h5: q.h5 != null && q.reset_h5 != null ? now + WINDOW_LEFT_S : sh(q.reset_h5), reset_w7: sh(q.reset_w7),
      ...(k === 'personal' && q.reset_w7 != null ? { w7_points: { start: sh(q.reset_w7)! - 7 * 86400, steps: [[2, 1], [10, 6], [20, 12], [30, 18], [40, 25], [50, 31], [56, 35]] as [number, number][] } } : {}) }])),
    projects: base.projects.map(p => ({ ...p, last_used: sh(p.last_used) })),
    approvals: [...base.approvals.map(a => ({ ...a, requested_at: a.requested_at + d, untested: en ? 'a real phone' : 'un telefono vero', undo: 'in-parte', undo_how: en ? 'pull the build' : 'si ritira la build' })), cartApproval(now)],
    devices: base.devices?.map(x => ({ ...x, seen: sh(x.seen) })) ?? null,
    night: { ...base.night, items: base.night.items?.map(i => ({ ...i, added: i.added + d, started: sh(i.started) })) ?? null },
    // Il recap di oggi, con un progetto senza sessione: così «Da riprendere» propone il suo prossimo passo.
    recap: { date: isoDay(now), items: [...base.recap.items, { project: 'docs-site', done: en ? 'API pages drafted' : 'Bozze delle pagine API', next: en ? 'Review the API pages' : 'Rivedi le pagine API' }] },
    attention: [...(base.attention ?? []).map(a => ({ ...a, since: a.since + d })),
      { kind: 'needs_you', session: 'storefront', ref: 'storefront', title: stopLogin.reason, since: stopLogin.at },
      { kind: 'turn_blocking', session: 'blog', ref: 'blog', title: stopOk.text, since: stopOk.at },
    ].sort((a, b) => a.since - b.since),
    // Contratto 1.84: suggerimenti, anteprima e chiusura salvando lo stato.
    ops: [...new Set([...(base.ops ?? []), 'suggestion', 'close_preview', 'close', 'home_order', 'prima_raccolta', 'prima_raccolta_azione', 'seguiti', 'seguiti_azione', 'results', 'result_seen', 'result_note', 'thumb', 'plugins', 'plugin_check', 'plugin_update', 'plugin_update_all'])],
    commitments,
    // Contratto 1.99: le righe di Da provare e Da decidere, coi conti degli elenchi `proveDemo` e `decisionsDemo`.
    prove: { n: 2, builds: 1, red: 0, ko: 0 }, decisions: { n: 3, here: 2, tasks: 4, snoozed: 0 },
    // Contratto 1.113 (app-pulizia-tocca-03-11): lo stesso conto delle righe qui sopra e tre voci uscite da sole stamattina.
    // Il consumo di oggi per sessione (Utilizzo, «chi ha speso di più»; app-demo-sempre-allineata-03-11): le quote per account sommano a 1.
    usage_today: [
      { session: 'storefront', account: 'personal', tokens: 1840000, share: 0.52 }, { session: 'master', account: 'personal', tokens: 1130000, share: 0.32 },
      { session: 'blog', account: 'personal', tokens: 560000, share: 0.16 },
    ],
    // Contratto 1.83: una domanda di un server MCP in cima a Tocca a te (app-demo-sempre-allineata-03-11); nella demo non scade.
    elicitations: [{
      id: 'e-demo-1', session: 'storefront', server: 'issue-tracker', mode: 'form', asked_at: now - 40, expires_at: now + 3600,
      message: en ? 'Which project should the checkout issue go to?' : 'In quale progetto apro la issue del checkout?',
      fields: [
        { key: 'project', title: en ? 'Project' : 'Progetto', required: true, kind: 'choice', options: [{ value: 'SHOP', label: en ? 'Shop' : 'Negozio' }, { value: 'OPS', label: en ? 'Operations' : 'Operazioni' }] },
        { key: 'urgent', title: en ? 'Urgent' : 'Urgente', required: false, kind: 'boolean', default: false },
      ],
    }],
    tocca: { n: 5, prove: 2, decidi: 3, today: { n: 3, items: [
      { task: 'shop-banner', short: en ? 'Banner on the home' : 'Banner in home', kind: 'prova', rule: 1, at: now - 5400 },
      { task: 'blog-feed', short: en ? 'RSS feed' : 'Feed RSS', kind: 'pause', rule: 2, at: now - 9000 },
      { task: 'storefront-login', short: en ? 'Login to the shop' : 'Accesso al negozio', kind: 'needs_you', rule: 3, at: now - 12600 },
    ] } },
    prima_raccolta: { status: 'ask', done: 9, total: 10, ask: true, first_round_at: null },
    seguiti: { rimasto: 1, altrove: 0, promemoria: 2 },
    // Contratto 1.106 (app-da-guardare-02-11): tre consegne da guardare, l'ultima versione di ogni lavoro.
    results: (() => { const g = resultsDemo(now).groups.filter(x => x.unseen); return { n_unseen: g.length, items: g.map(x => { const { caption: _c, files: _f, notes: _n, replaced: _r, ...i } = x.items[0]; return i }) } })(),
    // Utilizzo agganciato: dalla prima schermata si vede il dato in più dello slot.
    // La home per categorie (app-home-categorie-02-11), con Utilizzo agganciato allo slot e Sessioni nello spazio in cima
    // (app-home-in-cima-02-11), così la prima schermata mostra anche quello.
    // Il tabellone a quattro voci (app-tabellone-quattro-02-11): Utilizzo ci sta sopra; le sessioni stanno a sé, subito sotto
    // (app-home-sessioni-a-se-08-11).
    home_order: JSON.parse(encodeCats(toTop(DEFAULT_CATS, [], 'usage').cats, null, ['usage'])!) as string[],
    // Contratto 1.90: la macchina tranquilla, così «Sistema» parte chiuso come di solito.
    system: { at: now - 30, cores: 8, load1: 3.4, load_per_core: 0.42, mem_total_mb: 16000, mem_available_mb: 10100, level: 'green', load_level: 'green', mem_level: 'green',
      consumers: [{ group: 'node', count: 6, cpu: 1.4, rss_mb: 1800 }, { group: 'chrome', count: 9, cpu: 0.6, rss_mb: 1200 }], advice: [] } satisfies SystemStatus,
    // Contratto 1.95: lo stesso elenco di `pluginsDemo`, due da aggiornare.
    plugins: { checked_at: now - 2400, accounts: [{ account: 'personal', updates: ['docs-helper@demo', 'release-notes@demo'] }] },
    // Contratto 1.84 (scheda A): tre suggerimenti della master del giro delle 8 o delle 19, uno per tipo.
    suggestions: [
      { id: 'sg-1', kind: 'recurring', key: 'recurring:ci-storefront', text: en ? 'storefront: you often ask how the CI went, add it to Recurring?' : 'storefront: chiedi spesso com\'è andata la CI, la aggiungo ai Ricorrenti?', prompt: 'Add «how did the storefront CI go» to Recurring', at: now - 900 },
      { id: 'sg-2', kind: 'cleanup', key: 'cleanup:paused:docs', text: en ? 'admin-panel: two tasks paused for 2 days, archive them?' : 'admin-panel: due compiti in pausa da 2 giorni, li archivio?', prompt: 'Archive the two tasks paused for 2 days', at: now - 900 },
      { id: 'sg-3', kind: 'deadline', key: 'deadline:cron:blog', text: en ? 'blog: the nightly cron never ran, turn it on tonight?' : 'blog: il cron notturno non è mai partito, lo attivo stasera?', prompt: 'Turn on the blog nightly cron', at: now - 900 },
      { id: 'sg-4', kind: 'quota', key: 'quota:week', text: en ? 'payments-api: week at 62 %, move the heavy jobs to the night?' : 'payments-api: settimana al 62 %, sposto i lavori pesanti alla notte?', prompt: 'Move the heavy jobs to the night', at: now - 960 },
    ],
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
  // La mattina della master prima del giro di adesso: una chat lunga, che scorre, per vedere il morph della master partire dal
  // punto in vista (app-fluidita-06-11).
  const morning: [string, string, string, string][] = [
    ['Apri la giornata: cosa è rimasto di ieri?', 'Ieri sono rimasti tre lavori: il deploy di payments-api, le prove del checkout e la copertina del blog.', 'Open the day: what is left from yesterday?', 'Three jobs are left from yesterday: the payments-api deploy, the checkout tests and the blog cover.'],
    ['Comincia dalle prove del checkout', 'Le prove del checkout girano su storefront: 42 su 44 verdi, due rosse sui totali con i buoni regalo.', 'Start with the checkout tests', 'The checkout tests run on storefront: 42 of 44 green, two red on totals with gift cards.'],
    ['Perché i buoni regalo sbagliano i totali?', 'Il buono si sottrae prima delle tasse invece che dopo: la regola sta in due posti diversi del carrello.', 'Why do gift cards break the totals?', 'The card is subtracted before tax instead of after: the rule lives in two places of the cart.'],
    ['Correggilo in un posto solo', 'Fatto: la regola vive in totals.ts, con un test per il buono più grande del totale.', 'Fix it in one place', 'Done: the rule lives in totals.ts, with a test for a card larger than the total.'],
    ['E la copertina del blog?', 'La copertina è pronta in due varianti, chiara e scura; aspetta la tua scelta.', 'And the blog cover?', 'The cover is ready in two variants, light and dark; it waits for your pick.'],
    ['Prendi la scura', 'Presa la scura: la mando alla sessione del blog insieme al testo del post.', 'Take the dark one', 'Dark one taken: I send it to the blog session with the post text.'],
  ]
  const transcripts: Record<string, TranscriptEntry[]> = {
    master: [
      // La notte prima: venti giri, così un risultato vecchio della ricerca apre una pagina con altre voci dopo (contratto 1.127).
      ...Array.from({ length: 20 }, (_, k) => [said(`n${k}u`, en ? `Night round ${k + 1}: anything to report?` : `Giro della notte ${k + 1}: novità?`, now - 30000 + k * 1200, 'pc'),
        claude(`n${k}a`, k === 3 ? (en ? 'Round 4: the lighthouse backup finished, 212 files copied.' : 'Giro 4: il backup del faro è finito, 212 file copiati.') : (en ? `Round ${k + 1}: nothing new, the sessions are still.` : `Giro ${k + 1}: niente di nuovo, le sessioni sono ferme.`), now - 30000 + k * 1200 + 30)]).flat(),
      ...morning.flatMap(([q, a, qe, ae], i) => [said(`m${i}u`, en ? qe : q, now - 5400 + i * 600), claude(`m${i}a`, en ? ae : a, now - 5400 + i * 600 + 40)]),
      claude('a0', en
        ? 'demo-cover is ready for you, the other jobs are running.\n\n**For you**\n1. Send the cover image to the blog session.\n2. Log in to the payment sandbox.\n\n**Not tested:** the checkout on a real phone.\n\nEsito: two things wait for you.'
        : 'demo-cover è pronto per te, gli altri lavori vanno avanti.\n\n**Da te**\n1. Manda l\'immagine di copertina alla sessione del blog.\n2. Fai il login nella sandbox dei pagamenti.\n\n**Non provato:** il checkout su un telefono vero.\n\nLe prove #1 e #2 aspettano te in Verifica.\n\nEsito: due cose aspettano te.', now - 900, { outcome_type: 'waiting' }),
      said('u1', en ? 'Launch payments-api on phase 2.2' : 'Lancia payments-api sulla fase 2.2', now - 660, 'phone'),
      claude('a1', en
        ? 'payments-api launched on phase 2.2. Local commits only: push and release with your ok.\n• **Link:** https://claude.ai/code/session_01ledger\n\nEsito: Phase 2.2 started on payments-api\nProssimi: review the refund flow · try the checkout live'
        : 'Lanciata payments-api sulla fase 2.2. Solo commit locali: push e release con il tuo ok.\n• **Link:** https://claude.ai/code/session_01ledger\n\nEsito: Fase 2.2 avviata su payments-api\nProssimi: rivedi il flusso dei rimborsi · prova il checkout dal vivo', now - 600, { outcome_type: 'running' }),
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
      // La scelta del carrello chiesta in chat (app-scheda-scelta-12-11): il link della richiesta diventa la sua scheda compatta.
      claude('demo-store-cart', `${cartChoice(now).msg!.text}

https://supervisor.example.com/#/attesa/w00000000c3`, t0 + 175),
    ],
    blog: [
      tool('a0', 'Write', 'posts/edge-caching.md', t0 - 120, { files: [{ path: '/w/blog/posts/edge-caching.md', mime: 'text/markdown', size: 547 }, { path: '/w/blog/bench/bench-data.zip', mime: 'application/zip', size: 166 }, { path: '/w/blog/posts/edge-caching-preview.html', mime: 'text/html', size: 868 }] }),
      said('u1', numbers, t0, 'phone'),
      tool('w1', 'Write', 'bench/results.csv', t0 + 20, { diff: benchStep }),
      // La riga Esito tipizzata come la manda il relay (app-chat-esito-01-11): nella demo mancava e l'Esito restava testo.
      claude('a1', table, t0 + 60, { turn: { started: t0 + 5, ended: t0 + 60, out: 288 }, outcome_type: 'done' }),
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
    // Un titolo lungo con un progetto lungo: il progetto cede il posto al titolo (web-plancia-j2-ripasso-06-11).
    { state: 'aperto', scope: 'joyconceptstore-com', blocks: 'claude', title: en ? 'Joy: October SEO batch, skipped, and its report' : 'Joy: lotto SEO di ottobre, saltato, e il suo report', ref: '', key: 'd8' },
    { state: 'fatto', scope: 'postazione', blocks: 'nessuno', title: en ? 'Backups of the workstation every night' : 'Backup della postazione ogni notte', ref: 'crontab', key: 'd7' },
  ] }

  // La cronologia di oggi (contratto 1.29) per i dettagli della plancia.
  const d0 = now - 5 * 3600
  const timeline: TimelinePage = { since: d0, sessions: [
    { session: 'payments-api', live: true, events: [{ at: d0 + 600, kind: 'prompt', text: 'Prepare the 2.8.0 deploy', ref: 'payments-api' }, { at: d0 + 4000, kind: 'commit', text: 'bump version to 2.8.0', ref: '4c1e9a2' },
      { at: d0 + 9000, kind: 'test', text: 'refund endpoint tests', ok: true }, { at: d0 + 15000, kind: 'question', text: q?.text ?? '' }] },
    { session: 'storefront', live: true, events: [{ at: d0 + 1200, kind: 'commit', text: 'fix the cart totals', ref: '4be1c2a' }, { at: d0 + 2400, kind: 'prompt', text: 'ok, run the checkout tests', ref: 'storefront' },
      { at: d0 + 6000, kind: 'test', text: 'checkout tests', ok: true }] },
  ] }
  // Gli esiti di oggi, uno per tipo (contratto 1.98), sempre da mezzanotte in poi: il tabellone non scende a zero di notte.
  const today = midnight(now)
  const esito = (ago: number, text: string, type: string | null): TimelineEvent => ({ at: Math.max(today + 1, now - ago), kind: 'outcome', text, ok: null, ref: null, type })
  const add = (session: string, ...e: TimelineEvent[]) => { timeline.sessions.find(s => s.session === session)?.events.push(...e) }
  timeline.sessions.push({ session: 'admin-panel', live: true, events: [] }, { session: 'blog', live: true, events: [] })
  add('payments-api', esito(5400, en ? 'Refund endpoint tests are red on the partial case' : 'I test dei rimborsi sono rossi sul caso parziale', 'problem'), esito(2700, en ? 'Deploy 2.8.0 ready: waiting for your ok' : 'Rilascio 2.8.0 pronto: aspetta il tuo ok', 'waiting'))
  add('storefront', esito(4800, en ? 'Cart totals fixed, checkout tests green' : 'Totali del carrello corretti, test del checkout verdi', 'done'), esito(600, en ? 'Tests running on the checkout' : 'Test in corso sul checkout', 'running'))
  add('blog', esito(3600, en ? 'Post published on the staging site' : 'Post pubblicato sul sito di prova', 'shipped'), esito(7200, en ? 'The feed is generated at build time' : 'Il feed si genera al momento della build', 'answer'))
  add('admin-panel', esito(6300, en ? 'Filters added to the users table' : 'Filtri aggiunti alla tabella utenti', null), esito(300, en ? 'Build running on the new table' : 'Build in corso sulla nuova tabella', 'running'))
  for (const s of timeline.sessions) s.events.sort((a, b) => a.at - b.at)

  return { state, events, transcripts, mine, samples, night, agenda, timeline, fullDiffs }
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
    return i < 0 ? [] : [{ session, live: w.state.sessions.some(s => s.name === session && s.state !== 'gone'), entry: e.id, role: e.role, at: e.at ?? null, snippet, match: [i, i + needle.length], sid: `demo-${session}` }]
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
  'Dalla master: sposta il sito della documentazione sul nuovo host, senza cambiare il dominio.': 'From the master: move the documentation site to the new host, without changing the domain.',
  'sposta il sito della documentazione sul nuovo host, senza cambiare il dominio.': 'move the documentation site to the new host, without changing the domain.',
  'Dalla master: porta a termine la migrazione del registro e non fare release.': 'From the master: finish the ledger migration and make no release.',
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

/**
 * Contratto 1.130 nella demo (app-scheda-ti-aspetta-07-11): due domande che aspettano, la pausa di blog del registro (la stessa voce
 * di decisionsDemo, col suo wid) e una scelta nata in chat su payments-api; `attesa` aggiunge il pezzo di trascrizione.
 */
/**
 * I Monitoraggi della demo (app-monitoraggi-09-11): i dati dei mockup approvati nella forma del contratto 1.132, una scheda per forma,
 * gli stessi dei test di Kotlin e della web.
 */
export const monitorsDemo = (): MonitorCard[] =>
  // «Aggiorna adesso» nella demo (app-monitoraggi-aggiorna-10-11): il primo si aggiorna, gli altri no.
  structuredClone((monitorsMockup as MonitorsPage).items).map((m, i) => (i === 0
    ? { ...m, refreshable: true, refresh: { state: 'idle' as const, started: null, ended: null, rc: null, message: null, retry_at: null, timeout_s: 60 } }
    : { ...m, refreshable: false, refresh: null }))

/** Un file dichiarato in una richiesta della demo (contratto 1.137): servito da /demo-files come gli altri. */
const demoShot = (name: string, size: number): WaitFile => ({ path: `/home/demo/workspaces/work/storefront/docs/mockups/${name}`, name, mime: 'image/png', size, kind: 'image', session: null })
/**
 * La scheda di scelta della demo (app-scheda-scelta-12-11): due proposte del carrello di storefront con le loro immagini, la B
 * consigliata, «Nessuna, rifare», e un primo giro scartato col suo motivo.
 */
const cartChoice = (now: number): WaitItem => {
  const a = demoShot('cart-a.png', 37719), b = demoShot('cart-b.png', 37043), r1 = demoShot('cart-round1.png', 33952)
  const A = en ? 'A summary on top' : 'A riepilogo in alto', B = en ? 'B bar at the bottom' : 'B barra in fondo', Bv = `${B} (${en ? 'recommended' : 'consigliata'})`
  const R = en ? 'A dense list' : 'A lista fitta'
  return {
    id: 'w00000000c3', type: 'carta', kind: 'choice', label: 'choose', label_text: en ? 'Choose' : 'Scegli', session: 'storefront', task: 'demo-cart-layout',
    title: en ? 'Cart on the phone' : 'Carrello sul telefono',
    text: en ? 'Where the total and the Pay button go in the cart' : 'Dove stanno il totale e il tasto Paga nel carrello',
    description: en ? 'Two proposals for the cart on the phone: only the place of the total and of the Pay button changes. B keeps the button under the thumb, A shows it before the items.'
      : 'Due proposte per il carrello sul telefono: cambia solo dove stanno il totale e il tasto Paga. La B tiene il tasto sotto il pollice, la A lo mostra prima degli articoli.',
    options: [
      { n: 1, label: A, value: A, description: en ? 'The total shows at once; with many items the button stays at the top.' : 'Il totale si vede subito; con molti articoli il tasto resta in cima.', files: [a] },
      { n: 2, label: B, value: Bv, recommended: true, description: en ? 'Pay is always under the thumb; it covers one row of the list.' : 'Paga è sempre sotto il pollice; copre una riga della lista.', files: [b] },
    ],
    at: now - 540, card: null, link: '#/attesa/w00000000c3', answer: { op: 'approve', arg: 'demo-cart-layout' },
    actions: [
      { label: A, style: 'option', cmd: { op: 'approve', session: null, arg: 'demo-cart-layout', text: 'ok', choice: A } },
      { label: B, style: 'primary', recommended: true, cmd: { op: 'approve', session: null, arg: 'demo-cart-layout', text: 'ok', choice: Bv } },
      { label: en ? 'None, redo' : 'Nessuna, rifare', style: 'secondary', cmd: { op: 'approval_redo', session: null, arg: 'demo-cart-layout' }, text_field: 'text' },
      { label: en ? 'Close without ok' : 'Chiudi senza ok', style: 'secondary', cmd: { op: 'approval_close', session: null, arg: 'demo-cart-layout' }, text_field: 'text' },
    ],
    files: [a, b], card_id: 'w00000000c1', state: 'open', answered: null,
    rounds: [{
      id: 'w00000000c1', at: now - 5400, text: en ? 'Cart on the phone' : 'Carrello sul telefono', description: en ? 'A dense list with the button at the end of the page.' : 'Una lista fitta, col tasto in fondo alla pagina.',
      options: [{ n: 1, label: R, value: R, description: en ? 'five items on screen' : 'cinque articoli a schermo', files: [r1] }], files: [r1],
      state: 'redo', reason: en ? 'Too dense: the prices are hard to read and the button ends below the fold.' : 'Troppo fitta: i prezzi si leggono male e il tasto finisce sotto la piega.', by: 'phone', answered_at: now - 4800, choice: null,
    }],
    msg: { session: 'storefront', id: 'demo-store-cart', at: now - 560,
      text: en ? 'The cart on the phone is redone after your note on the first round.\n\nWhich of the two do I build? A keeps the summary on top, B puts total and Pay in a bar at the bottom.'
        : 'Il carrello sul telefono è rifatto dopo la tua nota sul primo giro.\n\nQuale delle due costruisco? La A tiene il riepilogo in alto, la B mette totale e Paga in una barra in fondo.',
      key: en ? 'Which of the two do I build?' : 'Quale delle due costruisco?' },
  }
}

/** La stessa scelta come ok che aspetta nello stato (app-scheda-scelta-12-11, punto 3): la home e la scheda di storefront la tengono fissa. */
const cartApproval = (now: number): Approval => {
  const w = cartChoice(now)
  return { task: w.task!, title: w.title!, what: w.text, where: en ? "storefront's cart, on the phone" : 'il carrello di storefront, sul telefono', deploy: false, requested_at: w.at, options: w.options!.map(o => o.value), wid: w.id,
    untested: en ? 'a real phone' : 'un telefono vero', undo: 'si', undo_how: en ? 'the cart of before comes back' : 'torna il carrello di prima' }
}

/** La prova 1 della demo come richiesta, coi file di cosa guardare (app-scheda-scelta-12-11, punto 5): l'immagine e il resoconto. */
const cartProva = (now: number): WaitItem => ({
  id: 'w00000000d4', type: 'carta', kind: 'prova', label: 'verify', label_text: en ? 'Verify' : 'Verifica', session: 'storefront', task: 'demo-cart',
  title: en ? 'Cart totals' : 'Totali del carrello', text: en ? "Open today's outcomes: the cart outcome is under Done" : "Apri gli esiti di oggi: l'esito del carrello sta sotto Fatto",
  options: null, at: now - 3600, card: null, link: '#/attesa/w00000000d4', answer: { op: 'prova', arg: '1' },
  actions: [
    { label: en ? 'Works' : 'Va', style: 'primary', cmd: { op: 'prova', session: null, arg: '1', result: 'ok' } },
    { label: en ? 'Does not work' : 'Non va', style: 'secondary', cmd: { op: 'prova', session: null, arg: '1', result: 'ko' }, text_field: 'text' },
  ],
  files: [demoShot('checkout-before.png', 35449), { ...demoShot('checkout-tests.pdf', 1872), mime: 'application/pdf', kind: 'document' }],
  description: null, card_id: 'w00000000d4', state: 'open', answered: null,
})

export const waitsDemo = (now: number): WaitItem[] => [
  cartChoice(now),
  cartProva(now),
  { id: 'w00000000a1', type: 'carta', kind: 'pausa', label: 'decide', label_text: en ? 'Decide' : 'Decidi', session: 'blog', task: 'demo-cover',
    text: en ? 'Send me the cover image' : "Mandami l'immagine di copertina", options: null, at: now - 1200, card: null, link: '#/attesa/w00000000a1',
    answer: { op: 'decisione', arg: 'demo-a' },
    title: en ? 'Cover image' : 'Immagine di copertina',
    actions: [
      { label: en ? 'Reply' : 'Rispondi', style: 'primary', cmd: { op: 'prompt', session: 'blog' }, text_field: 'arg' },
      { label: en ? 'Not now' : 'Non ora', style: 'secondary', cmd: { op: 'decisione', session: null, arg: 'demo-a', action: 'not_now' } },
      { label: en ? 'Drop it' : 'Lascia perdere', style: 'secondary', cmd: { op: 'decisione', session: null, arg: 'demo-a', action: 'drop' }, text_field: 'reason' },
    ],
    msg: { session: 'blog', id: 'demo-blog-cover', text: en ? 'The post is ready and the links are checked.\n\nI need the cover image: send it to me and I publish with the newsletter.' : "Il post è pronto e i link sono controllati.\n\nMi serve l'immagine di copertina: mandamela e pubblico con la newsletter.", key: en ? 'I need the cover image' : "Mi serve l'immagine di copertina", at: now - 1260 } },
  { id: 'w00000000b2', type: 'chat', kind: 'prossimi', label: 'choose', label_text: en ? 'Choose' : 'Scegli', session: 'payments-api', task: null,
    text: en ? 'Which branch name for the refunds fix?' : 'Che nome do al branch della correzione dei rimborsi?', at: now - 720, card: null, link: '#/attesa/w00000000b2',
    options: [{ n: 1, label: 'hotfix/refunds', value: 'hotfix/refunds', recommended: true, description: en ? 'it goes out with 2.8.1' : 'esce con la 2.8.1' }, { n: 2, label: 'release/2.9', value: 'release/2.9' }],
    answer: { op: 'prompt', arg: 'payments-api', done: { op: 'attention_done', arg: 'payments-api', kind: 'turn_blocking' } },
    title: en ? 'Branch name for the refunds fix' : 'Nome del branch dei rimborsi',
    actions: [
      { label: 'hotfix/refunds', style: 'primary', recommended: true, cmd: { op: 'prompt', session: 'payments-api', arg: 'hotfix/refunds' } },
      { label: 'release/2.9', style: 'option', cmd: { op: 'prompt', session: 'payments-api', arg: 'release/2.9' } },
      { label: en ? 'Reply' : 'Rispondi', style: 'secondary', cmd: { op: 'prompt', session: 'payments-api' }, text_field: 'arg' },
      { label: en ? 'Done' : 'Fatto', style: 'secondary', cmd: { op: 'attention_done', session: null, arg: 'payments-api', kind: 'turn_blocking' } },
    ],
    msg: { session: 'payments-api', id: 'demo-pay-branch', text: en ? 'The refunds fix is ready and the tests pass.\n\nWhich branch name for the refunds fix? hotfix/refunds goes out with 2.8.1.' : 'La correzione dei rimborsi è pronta e i test passano.\n\nChe nome do al branch della correzione dei rimborsi? hotfix/refunds esce con la 2.8.1.', key: en ? 'Which branch name for the refunds fix?' : 'Che nome do al branch della correzione dei rimborsi?', at: now - 760 } },
]
