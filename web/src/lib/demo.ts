import stateRaw from '../../../contract/state-1-question.json?raw'
import { decodeState } from './contract'
import { lang } from './lang'
import nightRaw from '../../../contract/night-report-sample.json?raw'

// In inglese i testi della demo sono inglesi (Franz, 13/10 21:48); le righe «Esito:» e «Prossimi:» restano: sono il formato
// con cui le sessioni chiudono il turno, e la web app le mostra come «Outcome» e come tasti.
const en = lang === 'en'

// I dati di prova: lo stato della fixture e una conversazione breve per sessione (finché non arriva il relay locale).
// In più la master, come nel provino della sua casa, e field-notes seguita, così «Per te» ha chi ha finito.
const base = decodeState(stateRaw)
const stopLogin = { reason: en ? 'Log in to the payment sandbox and send me the code' : 'Fai il login nella sandbox dei pagamenti e mandami il codice', at: base.ts - 420 }
const stopOk = { text: en ? 'ok to publish the docs site' : 'ok alla pubblicazione del sito della documentazione', at: base.ts - 240 }
export const demoState = {
  ...base,
  // Il recap di oggi con un progetto senza sessione, così «Per te» propone il suo prossimo passo.
  // La finestra delle 5 ore cominciata da due ore, così il ritmo ha letture da mostrare.
  // Il riferimento al rapporto della notte di prova, come lo mette il relay (1.44): così la home mostra il riquadro Notte.
  night: { ...base.night, report: (({ date, generated_at }) => ({ date, generated_at }))(JSON.parse(nightRaw) as { date: string; generated_at: number }) },
  quota: { ...base.quota, personal: { ...base.quota.personal, reset_h5: base.ts + 3 * 3600 } },
  recap: { ...base.recap, items: [...base.recap.items, { project: 'orbit-docs', done: 'API pages drafted', next: 'Review the API pages' }] },
  sessions: [
    { id: 'm', name: 'master', account: 'personal', project: 'personal/control-room', state: 'idle' as const, since: base.ts - 3600, context: 34, attached: true,
      model: { id: 'claude-opus-5-5', label: 'Opus 5.5' }, effort: 'high', suggestion: 'commit the README changes and open a PR' },
    // Contesto al 64 %: la proposta «handoff, poi /clear» del contratto 1.37 si vede nella sua chat.
    ...base.sessions.map(s => (s.name === 'field-notes' ? { ...s, followed: true, context: 64, outcome: { ...s.outcome!, at: base.ts - 900 }, turn_blocking: stopOk }
      : s.name === 'atlas-shop' ? { ...s, needs_you: stopLogin } : s)),
  ],
  // Contratto 1.68 (Franz, 15/10 dalla web app): una ferma per un login, una per un ok a fine turno, in «Serve te» con la domanda.
  attention: [...(base.attention ?? []),
    { kind: 'needs_you', session: 'atlas-shop', ref: 'atlas-shop', title: stopLogin.reason, since: stopLogin.at },
    { kind: 'turn_blocking', session: 'field-notes', ref: 'field-notes', title: stopOk.text, since: stopOk.at },
  ].sort((a, b) => a.since - b.since),
}

import eventsRaw from '../../../contract/events-sample.json?raw'
import type { Event } from './contract'
// Il comando della fixture porta il vecchio nome del plugin: nella demo quello di oggi.
export const demoEvents: Event[] = JSON.parse(eventsRaw.replace(/team-supervisor launch/g, 'supervisor launch'))

import type { TranscriptEntry } from './contract'
import type { Sent, Status } from './chatRules'

// Le conversazioni di prova, come i provini della chat (SessionSheetTest: sheetTranscript, sheetTables).
const t0 = demoState.ts - 600
const said = (id: string, text: string, at: number, origin = 'pc'): TranscriptEntry => ({ id, role: 'user', text, at, origin })
const claude = (id: string, text: string, at: number, extra: Partial<TranscriptEntry> = {}): TranscriptEntry => ({ id, role: 'assistant', text, at, ...extra })
const tool = (id: string, tool: string, text: string, at: number, extra: Partial<TranscriptEntry> = {}): TranscriptEntry => ({ id, role: 'tool', tool, text, at, ...extra })
const numbers = en ? 'Give me the numbers' : 'Dammi i numeri'
const numbersIt = 'I file toccati:\n\n| file | righe |\n|---|---|\n| Repo.kt | 336 |\n| Slash.kt | 47 |\n\nI numeri\n\n| cosa | ora | a mezzogiorno |\n|---|---|---|\n| post del film (01/10) | 223 visualizzazioni, 4 repost | 121 visualizzazioni, 1 repost |\n| nuovi follower | 1 | — |\n\nDettagli in https://claude.ai/artifact/RGdmD5fx e nel file `docs/numeri.md`.\n\nEsito: README rewritten\nProssimi: apri la PR · aggiorna il changelog · tagga la v1.2'
const numbersEn = 'The files touched:\n\n| file | lines |\n|---|---|\n| Repo.kt | 336 |\n| Slash.kt | 47 |\n\nThe numbers\n\n| what | now | at noon |\n|---|---|---|\n| film post (01/10) | 223 views, 4 reposts | 121 views, 1 repost |\n| new followers | 1 | — |\n\nDetails in https://claude.ai/artifact/RGdmD5fx and in the file `docs/numbers.md`.\n\nEsito: README rewritten\nProssimi: open the PR · update the changelog · tag v1.2'
export const demoTranscripts: Record<string, TranscriptEntry[]> = {
  master: [
    said('u1', en ? 'Launch payments-api on phase 2.2' : 'Lancia payments-api sulla fase 2.2', demoState.ts - 660, 'phone'),
    claude('a1', en
      ? 'payments-api launched on phase 2.2. Local commits only: push and release with your ok.\n\nEsito: Phase 2.2 started on payments-api\nProssimi: review the refund flow · try the checkout live'
      : 'Lanciata payments-api sulla fase 2.2. Solo commit locali: push e release con il tuo ok.\n\nEsito: Fase 2.2 avviata su payments-api\nProssimi: rivedi il flusso dei rimborsi · prova il checkout dal vivo', demoState.ts - 600),
  ],
  'ledger-api': [
    said('u1', 'Prepare the deploy of 2.4 and wait for my ok', t0 - 300),
    tool('a1', 'Bash', './gradlew assembleRelease', t0 - 250, { note: 'Build the release' }),
    tool('a2', 'Read', 'docs/migrations/2.4.md', t0 - 200),
    claude('a3', "The build is ready and the **migration notes** are checked. I'm waiting for the client's ok before the deploy.", t0 - 120, { turn: { started: t0 - 300, ended: t0 - 118, out: 412 } }),
  ],
  'atlas-shop': [
    said('u1.0', 'Add the Tuesday meeting notes to the draft', t0),
    claude('a1.0', "I'll read the draft first.", t0 + 5),
    tool('a1.1', 'Read', 'docs/draft.md', t0 + 5),
    tool('a3.0', 'Bash', 'grep -n Tuesday notes/*.md', t0 + 20, { note: 'Find the Tuesday notes', error: true }),
    claude('a4.0', 'The notes file was missing, so I added the Tuesday section to the draft by hand.', t0 + 40, { turn: { started: t0, ended: t0 + 45, in: 4020, out: 130 } }),
    tool('a6.0', 'Write', 'docs/cover.png', t0 + 152, { files: [{ path: '/w/field-notes/docs/cover.png', mime: 'image/png', size: 48_000 }] }),
    tool('a7.0', 'SendUserFile', 'Tuesday minutes', t0 + 155, { files: [{ path: '/w/field-notes/docs/minutes.pdf', mime: 'application/pdf', size: 212_000 }] }),
    said('p1.0', 'Also add the attendees list', t0 + 158, 'phone'),
  ],
  'field-notes': [
    said('u1', numbers, t0, 'phone'),
    claude('a1', en ? numbersEn : numbersIt, t0 + 60, { turn: { started: t0 + 5, ended: t0 + 60, out: 288 } }),
  ],
}
/** I messaggi mandati da qui, col loro stato. */
export const demoMine: [Sent, Status][] = [
  [{ id: 'c1', session: 'atlas-shop', text: 'Also add the attendees list', sentAt: t0 + 156 }, 'working'],
  [{ id: 'c2', session: 'field-notes', text: numbers, sentAt: t0 - 2 }, 'done'],
]

/** Le letture della quota delle 5 ore che l'app tiene da sé, per il ritmo (QuotaHistory): qui tre dell'account personale. */
export const demoSamples = {
  personal: [{ ts: demoState.ts - 7200, pct: 4 }, { ts: demoState.ts - 3600, pct: 7 }, { ts: demoState.ts, pct: 11 }],
}

/**
 * Quello che il relay risponderebbe a `search` (contratto 1.27) cercando nelle conversazioni di prova: un punto trovato per
 * voce, la riga intorno (fino a 160 caratteri) e la parte trovata. Finto: serve finché la web app non ha il trasporto.
 */
import type { SearchPage } from './contract'
export function demoSearch(q: string): SearchPage {
  const needle = q.toLowerCase()
  const hits = Object.entries(demoTranscripts).flatMap(([session, entries]) => entries.flatMap(e => {
    const text = e.text ?? ''
    const line = text.split('\n').find(l => l.toLowerCase().includes(needle))
    if (!line) return []
    const snippet = line.slice(0, 160)
    const i = snippet.toLowerCase().indexOf(needle)
    return i < 0 ? [] : [{ session, live: demoState.sessions.some(s => s.name === session && s.state !== 'gone'), entry: e.id, role: e.role, at: e.at ?? null, snippet, match: [i, i + needle.length] }]
  }))
  return { hits, more: false }
}

/** La cronologia di oggi (contratto 1.29) come la manderebbe il relay, per i dettagli della plancia. */
import type { TimelinePage } from './contract'
const d0 = demoState.ts - 5 * 3600
export const demoTimeline: TimelinePage = { since: d0, sessions: [
  { session: 'ledger-api', live: true, events: [{ at: d0 + 600, kind: 'prompt', text: 'Prepare the deploy of 2.4', ref: 'ledger-api' }, { at: d0 + 4000, kind: 'commit', text: 'bump version to 2.4.0', ref: '4c1e9a2' }, { at: d0 + 9000, kind: 'test', text: 'migration suite', ok: true }] },
  { session: 'atlas-shop', live: true, events: [{ at: d0 + 1200, kind: 'commit', text: 'fix the cart totals', ref: '4be1c2a' }, { at: d0 + 2400, kind: 'prompt', text: 'ok, run the checkout tests', ref: 'atlas-shop' }, { at: d0 + 6000, kind: 'test', text: 'checkout suite', ok: true }, { at: d0 + 12000, kind: 'test', text: 'payment suite', ok: false }, { at: d0 + 15000, kind: 'outcome', text: 'release candidate tagged, payments to fix' }] },
] }

/** La notte della demo: la fixture inventata del contratto 1.44 (il rapporto vero ha dati personali). */
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
/** Il rapporto della notte in inglese: le stesse voci, i testi tradotti (i titoli «nome — prompt» compresi). */
function nightEn(v: unknown): unknown {
  if (typeof v === 'string') {
    if (NIGHT_EN[v]) return NIGHT_EN[v]
    if (v.startsWith('Prima esecuzione del lavoro notturno')) return CHECKOUT_EN
    const dash = v.indexOf(' — ')
    if (dash > 0) {
      const rest = v.slice(dash + 3)
      const tr = NIGHT_EN[rest] ?? (rest.startsWith('Prima esecuzione') ? `${CHECKOUT_EN.slice(0, 160)}…` : null)
      if (tr) return `${v.slice(0, dash).replace(/-notte$/, '-night')} — ${tr}`
    }
    return v.replace(/-notte\b/g, '-night').replace(/^notte\//, 'night/')
  }
  if (Array.isArray(v)) return v.map(nightEn)
  if (v && typeof v === 'object') return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, nightEn(x)]))
  return v
}
export const demoNight: import('./night').NightReport = en ? nightEn(JSON.parse(nightRaw)) as import('./night').NightReport : JSON.parse(nightRaw)

/** Contratto 1.46: la demo ha un'agenda inventata, con un gruppo per ognuno di chi deve muoversi (come FakeTransport). */
export const demoAgenda: import('./contract').AgendaPage = { more: false, owner: 'you', rows: [
  { state: 'aperto', scope: 'agenzia', blocks: 'you', title: 'Confirm the 6 client ids with a candidate', ref: '.claude/to-decide-client-id.md', detail: 'Six ids have a likely client from the invoices.\nConfirm them one by one, then the import can run.', key: 'd1' },
  { state: 'aperto', scope: 'personale', blocks: 'you', title: 'Renew the domain of the docs site', ref: 'orbit-docs', key: 'd2' },
  { state: 'aperto', scope: 'personale', blocks: 'claude', title: 'Move the docs site to the new host', ref: 'orbit-docs', key: 'd3' },
  { state: 'aperto', scope: 'postazione', blocks: 'claude', title: 'Clear the orphan plugin caches', ref: 'after restarting every session', key: 'd4' },
  { state: 'sospeso', scope: 'agenzia', blocks: 'terzi', title: 'Staging of atlas-shop not reachable from the CLI', ref: '', until: '2026-10-20', key: 'd5' },
  { state: 'aperto', scope: 'agenzia', blocks: 'terzi', title: 'Client answer on the domain', ref: '', key: 'd6' },
  { state: 'fatto', scope: 'postazione', blocks: 'nessuno', title: 'Backups of the workstation every night', ref: 'crontab', key: 'd7' },
] }
