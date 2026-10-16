<script lang="ts">
  import { locale } from './lib/lang'
  import * as files from './lib/fileActions'
  import { failed, media, pages as pageSources, texts } from './lib/mediaCache.svelte'
  import { firstLines, previewKind } from './lib/media'
  import { firstPage } from './lib/pdfPreview'
  import { groups, together } from './lib/shareMulti'
  import type { FileAct, Fetched } from './lib/fileActions'
  import { FileStore } from './lib/fileStore'
  import { DemoTransport } from './lib/demoTransport'
  import InAppBar from './lib/InAppBar.svelte'
  import { setContext, untrack } from 'svelte'
  import type { AgendaPage, Cmd, CmdOp, CmdResult, Event, SearchPage, Session, State, TimelinePage, TranscriptEntry, TranscriptPage } from './lib/contract'
  import { blankState, decodeChanges, decodeDiff, withTail } from './lib/contract'
  import { ask } from './lib/changes'
  import { fileArg } from './lib/diff'
  import { advance, prune, status, type PendingStatus, type Sent, type Status, type Upload } from './lib/chatRules'
  import type { Sample } from './lib/quotaHistory'
  import { LocalTransport, localAccess, newCmd, type Transport } from './lib/transport'
  import { CANONICAL, needsHomeScreen, remoteFor, remoteTransport, wantsPairing } from './lib/remoteAccess'
  import { currentSubscription, pushStatus, subscribe, subscriptionDoc } from './lib/push'
  import PairPage from './lib/PairPage.svelte'
  import { approveText, canClear, clearDue, handoffPrompt } from './lib/masterService'
  import { toggle } from './lib/speech.svelte'
  import Home from './lib/Home.svelte'
  import Chat from './lib/Chat.svelte'
  import Desk from './lib/Desk.svelte'
  import ColumnHead from './lib/ColumnHead.svelte'
  import { t } from './lib/t'
  import HomePane from './lib/HomePane.svelte'
  import RecapPage from './lib/RecapPage.svelte'
  import { recapActions } from './lib/recapActions'
  import { NIGHT_TARGET, night as nightCards, setCmd, todayIso } from './lib/recapAgenda'
  import NightAgendaPicker from './lib/NightAgendaPicker.svelte'
  import Composer from './lib/Composer.svelte'
  import AppBar, { type Page as PageName } from './lib/AppBar.svelte'
  import Page from './lib/Page.svelte'
  import Launch from './lib/Launch.svelte'
  import Diary from './lib/Diary.svelte'
  import NightPage from './lib/NightPage.svelte'
  import { page as nightPage, showHomeNight, type NightReport } from './lib/night'
  import TodayPanel from './lib/TodayPanel.svelte'
  import Search from './lib/Search.svelte'
  import Settings from './lib/Settings.svelte'
  import AddDevice from './lib/AddDevice.svelte'
  import Queue from './lib/Queue.svelte'
  import QuotaPanel from './lib/QuotaPanel.svelte'
  import Inspector from './lib/Inspector.svelte'
  import ColumnTab from './lib/ColumnTab.svelte'
  import { alert as elsewhereOf, key as alertKey, type Alert } from './lib/elsewhere'
  import { build as devicesOf, linked as linkedDevices } from './lib/devices'
  import { freshness } from './lib/durations'
  import { setRate, setVoice } from './lib/speech.svelte'
  import { build as overviewOf } from './lib/overview'
  import ReadingPill from './lib/ReadingPill.svelte'
  import CloseSplash from './lib/CloseSplash.svelte'
  import { closeSplash, type Leaving } from './lib/closeSplash'
  import { build, finishedKey, MASTER } from './lib/summary'
  import { nextKey, type ForYouRow } from './lib/masterHome'
  import { add, columns, columnsFromPref, columnsPref, inspect, sharesFromPref, sharesPref, timelineArg, toggle as toggleCol, wide as isWide } from './lib/tablet'
  import { refusalText } from './lib/questionRules'
  import { shouldRead, due, activity, behind, BG_PARALLEL, type LastRead, type BgRead } from './lib/readPlan'
  import { of as swipeOf, stable as swipeStable, step as swipeStep, type Pages } from './lib/swipePages'

  const load = (k: string) => { try { return localStorage.getItem(k) } catch { return null } }
  const save = (k: string, v: string) => { try { localStorage.setItem(k, v) } catch { /* resta per la sessione */ } }
  const json = <T,>(k: string, d: T): T => { try { return JSON.parse(load(k) ?? '') as T } catch { return d } }
  const nowS = () => Math.round(Date.now() / 1000)

  // Strada locale (contratto 1.35) sulla pagina servita dal relay; altrove la strada remota del browser collegato (fase 3),
  // e senza collegamento la pagina che lo fa. Il server di sviluppo e `?demo` mostrano i dati di prova.
  const access = localAccess(new URL(location.href), { get: load, set: save })
  if (access?.clean != null) history.replaceState(null, '', access.clean)
  const store = { get: load, set: save, remove: (k: string) => { try { localStorage.removeItem(k) } catch { /* niente */ } } }
  const remote = access ? null : remoteFor(new URL(location.href), store)
  const pairing = !access && wantsPairing(new URL(location.href), remote)
  const rt = !access && remote ? remoteTransport(remote, store) : null
  // Senza relay e senza collegamento la demo: un PC finto che risponde come quello vero (FakeTransport dell'app).
  const demo = !access && !rt && !pairing ? new DemoTransport() : null
  const tr: Transport | null = access ? new LocalTransport(access.base, access.token) : rt ?? demo
  let st = $state<State>(demo ? demo.state : blankState(nowS()))
  let ready = $state(!!demo)
  let down = $state(false)
  let clock = $state(nowS())
  // L'ora delle età e dei conti: quella vera con il relay, quella della fixture nella demo.
  const now = $derived(clock)
  let events = $state<Event[]>(demo ? demo.world.events : [])
  // Nella demo le conversazioni ci sono già, anche per le pagine accanto della striscia.
  let transcripts = $state<Record<string, TranscriptEntry[]>>(demo ? structuredClone(demo.world.transcripts) : {})
  // I messaggi mandati da qui: lo stato di ognuno viene dal comando (in volo, risposta del PC) e dalla sessione (turno).
  let msgs = $state<Sent[]>(demo ? demo.world.mine.map(([m]) => m) : tr ? prune(json<Sent[]>('cm.sent', []), nowS()) : [])
  let pend = $state<Record<string, PendingStatus>>({})
  let res = $state<Record<string, CmdResult>>({})
  let uploads = $state<Record<string, Upload>>({})
  // Nella demo i messaggi di prova hanno lo stato scritto; quelli mandati adesso seguono le regole, come col relay.
  const demoStatus = new Map(demo?.world.mine.map(([m, x]) => [m.id, x]) ?? [])
  const mine = $derived<[Sent, Status][]>(msgs.map(m => [m, demoStatus.get(m.id)
    ?? status(m, pend[m.id] ?? null, res[m.id] ?? null, st.sessions.find(x => x.name === m.session) ?? null, uploads[m.id] ?? null)]))
  // Le letture della quota delle 5 ore, per il ritmo: le tiene la web app, come QuotaHistory sul telefono.
  let samples = $state<Record<string, Sample[]>>(demo ? demo.world.samples : json('cm.samples', {}))
  // Quello che si è letto o avviato (turni finiti, resoconto, prossimi passi) e i messaggi mandati da qui, per «Per te».
  let read = $state(new Set<string>())
  const sent = $derived(mine.map(([m]) => m))
  const master = $derived(st.sessions.find(s => s.name === MASTER && s.state !== 'gone') ?? null)
  // Un comando rifiutato o senza risposta: una riga in basso per qualche secondo.
  let notice = $state<string | null>(null)
  let addDlg: AddDevice | undefined = $state()
  let noticeTimer: ReturnType<typeof setTimeout> | undefined
  function say(text: string) { notice = text; clearTimeout(noticeTimer); noticeTimer = setTimeout(() => (notice = null), 6000) }

  function record(s: State) {
    const next = { ...samples }
    for (const [acct, q] of Object.entries(s.quota)) {
      if (q.h5 == null || q.stale) continue
      const list = (next[acct] ?? []).filter(x => x.ts >= s.ts - 6 * 3600)
      const last = list.at(-1)
      if (!last || last.pct !== q.h5 || s.ts - last.ts >= 300) list.push({ ts: s.ts, pct: q.h5 })
      next[acct] = list
    }
    samples = next
    if (!demo) save('cm.samples', JSON.stringify(next))
  }

  async function refreshEvents() {
    if (!tr) return
    try {
      const fresh = await tr.fetchEvents(events[0]?.ts ?? 0)
      if (!fresh.length) return
      const keys = new Set(fresh.map(e => e.key))
      events = [...fresh, ...events.filter(e => !keys.has(e.key))].sort((a, b) => b.ts - a.ts).slice(0, 300)
    } catch { /* al prossimo stato */ }
  }

  if (tr) {
    tr.subscribe(s => {
      st = s; ready = true; down = false; clock = nowS()
      msgs = msgs.map(m => advance(m, s.sessions.find(x => x.name === m.session) ?? null, nowS()))
      record(s)
      clearsDue(s)
      refreshEvents()
    }, () => { down = true })
    setInterval(() => (clock = nowS()), 30_000)
  }
  $effect(() => { if (tr && !demo) save('cm.sent', JSON.stringify(msgs)) })

  // Notifiche web push (fase 4), solo sulla strada remota: l'iscrizione si riscrive a ogni avvio, perché il browser può
  // cambiarla; l'interruttore sta nelle Impostazioni e chiede il permesso dal tocco, come vuole Safari.
  const pushable = 'serviceWorker' in navigator && 'PushManager' in globalThis && 'Notification' in globalThis
  const standalone = matchMedia('(display-mode: standalone)').matches || (navigator as { standalone?: boolean }).standalone === true
  let pushOn = $state(false)
  let permission = $state<NotificationPermission>(pushable ? Notification.permission : 'denied')
  if (rt && pushable) currentSubscription().then(sub => {
    const doc = sub && subscriptionDoc(sub.toJSON())
    pushOn = !!doc
    if (doc) rt.savePush(doc).catch(() => {})
  }, () => {})
  const push = $derived(pushStatus({
    remote: !!rt, vapid: st.push?.vapid ?? null, supported: pushable, permission, subscribed: pushOn,
    homeFirst: needsHomeScreen(navigator.userAgent, standalone, navigator.maxTouchPoints ?? 0),
  }))
  async function setPush(on: boolean) {
    if (!rt) return
    try {
      if (on && st.push?.vapid) {
        const doc = await subscribe(st.push.vapid)
        permission = Notification.permission
        if (!doc) { say(t.pushFail); return }
        await rt.savePush(doc)
        pushOn = true
      } else if (!on) {
        await (await currentSubscription())?.unsubscribe()
        await rt.savePush(null)
        pushOn = false
      }
    } catch { say(t.pushFail) }
  }

  // Contratto 1.37, «Handoff, poi /clear»: il /clear parte a turno finito, una volta sola; dopo 3 ore si lascia perdere.
  let clearAfter = $state<Record<string, number>>(tr && !demo ? json('cm.clear_after', {}) : {})
  function handoff(name: string) {
    const s = st.sessions.find(x => x.name === name)
    if (!s) return
    const clear = canClear(st)
    const prompt = handoffPrompt(st, s, clear ? t.ctxHandoffClearPrompt : t.ctxHandoffPrompt)
    if (prompt == null) return
    sendTo(name, prompt)
    if (clear) { clearAfter = { ...clearAfter, [name]: nowS() }; if (!demo) save('cm.clear_after', JSON.stringify(clearAfter)) }
  }
  function clearsDue(s: State) {
    const next = { ...clearAfter }
    let changed = false
    for (const [name, sentAt] of Object.entries(clearAfter)) {
      const due = clearDue(s.sessions.find(x => x.name === name), sentAt)
      if (due) run(newCmd('slash', name, 'clear'))
      if (due || nowS() - sentAt > 3 * 3600) { delete next[name]; changed = true }
    }
    if (changed) { clearAfter = next; if (!demo) save('cm.clear_after', JSON.stringify(next)) }
  }
  async function approve(task: string, note: string) {
    const r = await run(newCmd('approve', null, task, approveText(note)))
    if (r?.ok) say(`✓ ${t.approved}`)
  }
  async function decide(text: string, project: string | null) {
    const r = await run(newCmd('decision', null, project, text))
    if (r?.ok) say(`✓ ${t.decisionSent}`)
  }

  /** Un comando al relay; il rifiuto si mostra in basso. */
  async function run(c: Cmd): Promise<CmdResult | null> {
    if (!tr) { console.info('[cm] comando', c.session, c.op, c.arg, c.text); return null }
    try {
      const r = await tr.send(c)
      // Contratto 1.55: una risposta a una domanda già risolta al PC lo dice in chiaro.
      if (!r.ok) say(`✗ ${refusalText(r, c.question ?? null, st.sessions, t.answerAlreadyResolved)}`)
      return r
    } catch {
      say(`✗ ${t.noAnswer}`)
      return null
    }
  }

  // La conversazione delle sessioni a schermo: la prima volta le ultime 50 voci, poi solo quelle venute dopo.
  const loadingT = new Set<string>()
  /** Contratto 1.59: le ultime voci da /tail, senza passare dal relay. Vero se c'erano. */
  async function loadTail(name: string): Promise<boolean> {
    const s = st.sessions.find(x => x.name === name)
    if (!tr?.fetchTail || !s) return false
    const tail = await tr.fetchTail(s.id).catch(() => null)
    if (!tail?.entries.length) return false
    transcripts = { ...transcripts, [name]: withTail(untrack(() => transcripts[name]) ?? [], tail) }
    return true
  }

  // Contratto 1.22: «Carica i messaggi precedenti» in cima alla chat, finché il relay dice che ce ne sono.
  let olderOf = $state<Record<string, boolean>>({})
  async function loadOlder(name: string) {
    const first = transcripts[name]?.[0]?.id
    if (!tr || !first) return
    const r = await run(newCmd('transcript', name, `50:before=${first}`))
    if (!r?.ok) return
    const p: TranscriptPage = JSON.parse(r.text)
    transcripts = { ...transcripts, [name]: [...p.entries, ...(transcripts[name] ?? [])] }
    olderOf = { ...olderOf, [name]: !!p.more }
  }
  /** Vero quando la lettura è arrivata, anche senza voci nuove. */
  async function loadTranscript(name: string): Promise<boolean> {
    if (!tr || loadingT.has(name)) return false
    loadingT.add(name)
    try {
      const have = untrack(() => transcripts[name])
      const last = have?.at(-1)?.id
      const r = await tr.send(newCmd('transcript', name, last ? `200:after=${last}` : '50'))
      if (!r.ok) {
        if (last && r.text.startsWith('no entry')) transcripts = { ...transcripts, [name]: [] }
        return false
      }
      const p: TranscriptPage = JSON.parse(r.text)
      if (!last) olderOf = { ...olderOf, [name]: !!p.more }
      if (last && !p.entries.length) return true
      transcripts = { ...transcripts, [name]: last ? [...(have ?? []), ...p.entries] : p.entries }
      return true
    } catch { return false /* al prossimo stato */ } finally { loadingT.delete(name) }
  }

  // Da 840 px la plancia con le colonne, come il tablet; sotto, una colonna sola come il telefono.
  let width = $state(window.innerWidth)
  const wide = $derived(isWide(width))
  // `#nome` apre quella sessione (link diretto, e i provini).
  const linked = decodeURIComponent(location.hash.slice(1))
  const known = (n: string) => (!!tr && !demo) || st.sessions.some(s => s.name === n && s.state !== 'gone')

  // La master vive nella home: ridotta è la barra in fondo, espansa occupa la home (mai una colonna o una pagina a parte).
  let masterOpen = $state(linked === MASTER)
  // Sulla plancia la misura a metà fra barra e tutto schermo (Franz, 08/10 18:15); conta solo con `masterOpen`.
  let masterHalf = $state(false)
  // Telefono: la sessione aperta, o la home.
  let open = $state<string | null>(known(linked) && linked !== MASTER ? linked : null)
  // Chiusa una sessione, la sua pagina non sparisce (Franz, 08/10 18:06): resta com'era l'ultima volta, chiusa, sotto il
  // pannello di chiusura (`CloseSplash`), che dopo 3 s riporta alla home o toglie la colonna.
  let leaving = $state<Leaving | null>(null)
  let lastSeen = $state<Record<string, Session>>({})
  $effect(() => { const next = { ...untrack(() => lastSeen) }; for (const x of st.sessions) next[x.name] = x; lastSeen = next })
  const ghost = (n: string): Session | null => { const x = lastSeen[n]; return x ? { ...x, state: 'gone', question: null } : null }
  const session = $derived(st.sessions.find(s => s.name === open) ?? (open && open !== MASTER ? ghost(open) : null))
  const splashOf = (n: string, column = false) => {
    // In colonna una gone non resta: conta come chiusa.
    const ss = column ? st.sessions.filter(x => x.state !== 'gone') : st.sessions
    const g = st.sessions.find(x => x.name === n && x.state === 'gone')
    return closeSplash(n, ss, leaving, g ?? lastSeen[n] ?? null, now)
  }
  // Tornando alla home la pagina vola al contrario verso la card della sessione appena chiusa, in cima alle sessioni, che resta
  // un attimo e poi si richiude (Franz, 08/10 18:59): le due hanno lo stesso view-transition-name.
  let justClosed = $state<{ s: Session; line: string } | null>(null)
  let justTimer: ReturnType<typeof setTimeout> | undefined
  const splashHome = () => {
    const o = open
    const ph = o ? splashOf(o) : null
    const x = o ? (st.sessions.find(y => y.name === o) ?? lastSeen[o]) : null
    smooth(() => {
      if (ph?.kind === 'closed' && x) {
        justClosed = { s: { ...x, state: 'gone', question: null }, line: t.justClosedLine(new Date(ph.at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })) }
        clearTimeout(justTimer)
        justTimer = setTimeout(() => smooth(() => { justClosed = null }), 2_500)
      }
      if (leaving?.name === o) leaving = null
      open = null
    })
  }

  // Plancia: le colonne scelte (le prime tre senza una scelta salvata), le larghezze in dodicesimi, il lato della home.
  // Le colonne sono le sessioni della lista, senza la master: una master rimasta fra le scelte salvate si toglie da sé.
  const summary = $derived(build(st, sent, now, read))
  const live = $derived(summary.rows.map(r => r.session.name).filter(n => n !== MASTER))
  // La colonna di una sessione appena chiusa resta sotto il pannello di chiusura finché il pannello non la toglie.
  let heldCols = $state<string[]>([])
  let shownCols: string[] = []
  $effect.pre(() => {
    const l = live
    untrack(() => {
      const next = [...heldCols.filter(n => !l.includes(n)), ...shownCols.filter(n => !l.includes(n) && !heldCols.includes(n))]
      if (next.length !== heldCols.length || next.some((n, i) => n !== heldCols[i])) heldCols = next
    })
  })
  const dropCol = (n: string) => smooth(() => { heldCols = heldCols.filter(x => x !== n); if (leaving?.name === n) leaving = null })
  let pinned = $state<string[] | null>(columnsFromPref(load('cm.columns')))
  if (known(linked) && linked !== MASTER) pinned = add(pinned ?? columns(null, live), linked)
  const cols = $derived(columns(pinned, [...live, ...heldCols]))
  $effect(() => { shownCols = cols })
  let shares = $state(sharesFromPref(load('cm.shares'), 0))
  $effect(() => { if (shares.length !== cols.length) shares = sharesFromPref(load('cm.shares'), cols.length) })
  let homeRight = $state(load('cm.home_right') === '1')
  // La barra del titolo nostra: solo nella web app installata con Window Controls Overlay acceso (`?wco=1` la simula).
  const wcoApi = (navigator as Navigator & { windowControlsOverlay?: EventTarget & { visible: boolean } }).windowControlsOverlay
  const wcoForced = new URLSearchParams(location.search).has('wco')
  let wco = $state(wcoForced || !!wcoApi?.visible)
  wcoApi?.addEventListener('geometrychange', () => { wco = wcoForced || !!wcoApi?.visible })
  $effect(() => { document.body.classList.toggle('wco', wco) })
  $effect(() => { document.body.classList.toggle('wcodesk', wco && wide) })
  // Il clic su una scheda porta il cursore nel campo di quella colonna.
  const focusColumn = (name: string) => document.querySelector<HTMLTextAreaElement>(`[data-col="${CSS.escape(name)}"] textarea`)?.focus()
  // I dettagli della prima colonna accanto alle colonne: dalle Impostazioni, spenti di default (Franz, 04/10 14:40).
  let details = $state(load('cm.details') === '1')
  const rowOf = $derived(Object.fromEntries(summary.rows.map(r => [r.session.name, r])))
  // La riga di una colonna appena chiusa, com'era: la sua testata resta finché il pannello non toglie la colonna.
  let rowSeen = $state<Record<string, (typeof summary.rows)[number]>>({})
  $effect(() => { rowSeen = { ...untrack(() => rowSeen), ...rowOf } })
  const headRow = (n: string) => rowOf[n] ?? (heldCols.includes(n) && rowSeen[n] ? { ...rowSeen[n], session: ghost(n) ?? rowSeen[n].session } : null)

  // Il cambio con la transizione del browser, quando c'è (Chrome): niente ridisegni continui.
  // Con la pagina nascosta niente transizione: il browser la interrompe e la promessa rifiutata finiva in console (15/10).
  const smooth = (go: () => void) => {
    if (!document.startViewTransition || document.hidden) { go(); return }
    const vt = document.startViewTransition(go)
    for (const p of [vt.ready, vt.finished, vt.updateCallbackDone]) p.catch(() => {})
  }
  function setCols(c: string[]) { smooth(() => { pinned = c; save('cm.columns', columnsPref(c)) }) }
  function setShares(s: number[]) { shares = s; save('cm.shares', sharesPref(s)) }
  // Aprire una sessione (da «Per te», da una ricerca, dal controller della lettura): la master si espande nella home,
  // le altre entrano in colonna sulla plancia o diventano la pagina sul telefono.
  function pick(name: string) {
    // Aprire una sessione la toglie da «Hanno finito», come nell'app.
    const o = st.sessions.find(x => x.name === name)?.outcome
    if (o && !read.has(finishedKey(name, o.at))) read = new Set([...read, finishedKey(name, o.at)])
    if (name === MASTER) smooth(() => { masterOpen = true; open = null })
    else if (wide) setCols(add(cols, name))
    else smooth(() => { open = name })
  }
  // Le pagine del menu: sul telefono al posto della home, sulla plancia al posto delle colonne.
  // `?page=diary` apre una pagina (link diretto, e i provini).
  const asked = new URLSearchParams(location.search).get('page')
  let page = $state<PageName | null>(asked && ['launch', 'diary', 'night', 'recap', 'search', 'settings', 'queue'].includes(asked) ? (asked as PageName) : null)
  // La pagina Notte (specifica del 07/10, approvata alle 21:50): il rapporto si chiede al PC con l'op `night` (contratto 1.44)
  // quando la pagina si apre e col tasto aggiorna; nella demo è la fixture inventata.
  let night = $state<{ report: NightReport | null; error: string | null; loading: boolean }>({ report: null, error: null, loading: false })
  const nightZone = Intl.DateTimeFormat().resolvedOptions().timeZone
  // Fra le cose da fare solo quello ancora vero adesso (Franz, 08/10 14:20: /clear chiesto a una sessione chiusa).
  const nightModel = $derived(night.report ? nightPage(night.report, nightZone, { live: new Set(st.sessions.filter(s => s.state !== 'gone').map(s => s.name)), pending: new Set((st.approvals ?? []).map(a => a.task)), asking: new Set(st.sessions.filter(s => s.question).map(s => s.name)) }) : null)
  const nightTitle = $derived.by(() => {
    if (!nightModel) return t.menuNight
    const d = new Date(`${nightModel.day}T12:00:00Z`), b = new Date(`${nightModel.dayBefore}T12:00:00Z`)
    return t.nightPageTitle(b.getUTCDate(), d.getUTCDate(), new Intl.DateTimeFormat(locale, { month: 'long', timeZone: 'UTC' }).format(d))
  })
  async function loadNight() {
    if (!tr) return
    night = { ...night, loading: true, error: null }
    try {
      const r = await tr.send(newCmd('night', null, null))
      if (r.ok) night = { report: JSON.parse(r.text), error: null, loading: false }
      else night = { report: night.report, error: /unknown|not allowed|unsupported/i.test(r.text) ? t.nightOld : r.text.startsWith('no night report') ? t.nightNone : r.text, loading: false }
    } catch { night = { ...night, error: t.noAnswer, loading: false } }
  }
  $effect(() => { if (page === 'night' && !night.report && !night.loading) untrack(() => loadNight()) })
  // Il riquadro Notte in home (mockup approvato l'08/10 alle 12:57): finché quella notte non si apre; il rapporto si chiede
  // una volta, per i conteggi. Il giorno aperto si ricorda nel browser.
  let nightSeen = $state<string | null>((() => { try { return localStorage.getItem('night-opened') } catch { return null } })())
  const nightBoxShown = $derived(showHomeNight(st.night?.report, nightSeen))
  $effect(() => { if (nightBoxShown && !night.report && !night.loading && !night.error) untrack(() => loadNight()) })
  $effect(() => {
    const d = st.night?.report?.date
    if (page === 'night' && d && d !== nightSeen) { nightSeen = d; try { localStorage.setItem('night-opened', d) } catch { /* solo per questa visita */ } }
  })
  // Il Recap (Franz, 08/10 20:41): le Azioni e le righe dell'agenda (contratto 1.46), chieste quando il relay le conosce, a
  // ogni recap nuovo e all'apertura della pagina; nella demo un'agenda inventata.
  // Solo i «prossimo» del recap del giorno (Franz, 09/10 17:21): i «Prossimi:» delle sessioni stanno sulle loro schede.
  const actions = $derived(recapActions(st, t.recapResume, false))
  const recapDay = $derived(/^\d{4}-\d{2}-\d{2}$/.test(st.recap.date) ? `${st.recap.date.slice(8, 10)}/${st.recap.date.slice(5, 7)}` : '')
  let agenda = $state<{ page: AgendaPage | null; error: string | null; loading: boolean }>({ page: null, error: null, loading: false })
  const canAgenda = $derived(!!st.ops?.includes('agenda'))
  async function loadAgenda() {
    if (!tr) return
    agenda = { ...agenda, loading: true, error: null }
    try {
      const r = await tr.send(newCmd('agenda', null, null))
      if (r.ok) agenda = { page: JSON.parse(r.text), error: null, loading: false }
      else agenda = { page: agenda.page, error: r.text.startsWith('no agenda file') ? t.recapAgendaNone : r.text, loading: false }
    } catch { agenda = { ...agenda, error: t.noAnswer, loading: false } }
  }
  $effect(() => { void st.recap.date; if (canAgenda) untrack(() => loadAgenda()) })
  $effect(() => { if (page === 'recap' && canAgenda) untrack(() => loadAgenda()) })
  // Le Azioni sulle schede (piano del 09/10). «Parlane con la master»: la scheda citata nel campo della master, sotto quello
  // che c'era già, e la sua conversazione aperta. Le scritture (Fatto, Rimanda, Rimuovi, Passa) con l'op del contratto 1.47.
  const canWrite = $derived(!!st.ops?.includes('agenda_set'))
  // Il cruscotto della plancia (variante B): senza colonne né pagine, Recap e Utilizzo stanno a destra e non nella home.
  const deskDashboard = $derived(wide && !cols.length && !page)
  let masterInject = $state<{ text: string; n: number } | null>(null)
  function talk(text: string) {
    masterInject = { text, n: (masterInject?.n ?? 0) + 1 }
    smooth(() => { page = null; masterOpen = true })
  }
  // Contratto 1.47: la scrittura va al relay con `agenda_set`; riuscita, l'agenda si ricarica; «agenda row changed» ricarica
  // e lo dice. Nella demo cambia la riga in memoria, come farebbe il relay.
  async function editAgenda(e: import('./lib/recapAgenda').Edit) {
    const c = setCmd(e, agenda.page?.owner ?? null)
    if (!c) return
    if (!tr) return
    try {
      const cmd = { ...newCmd('agenda_set', null, c.key), action: c.action, ...(c.until ? { until: c.until } : {}), ...(c.blocks ? { blocks: c.blocks } : {}) }
      const r = await tr.send(cmd)
      if (!r.ok) say(r.text.startsWith('agenda row changed') ? t.agendaChanged : r.text.startsWith('agenda busy') ? t.agendaBusy : r.text)
      if (r.ok || r.text.startsWith('agenda row changed')) await loadAgenda()
    } catch { say(t.noAnswer) }
  }
  // Contratto 1.49 (Franz, 09/10 21:27, scelta A): le schede del Recap stanotte, alla master, un lavoro per scheda col testo
  // di «Fallo»; dal menu Azioni o spuntate nel foglio «Aggiungi alla notte». Un avviso solo, alla fine; nella demo solo quello.
  const canNight = $derived(!!st.night?.master)
  async function queueNight(prompts: string[]) {
    if (!prompts.length) return
    if (!tr) return
    let ok = 0
    for (const p of prompts) {
      try {
        const r = await tr.send(newCmd('night_add', null, NIGHT_TARGET, p))
        if (!r.ok) { say(r.text); return }
        ok++
      } catch { say(t.noAnswer); return }
    }
    say(t.nightAgendaAdded(ok))
  }
  setContext('agendaNight', { get can() { return canNight }, get rows() { return nightCards(agenda.page, todayIso()) }, queue: queueNight })
  $effect(() => { if (canNight && canAgenda && !agenda.page && !agenda.loading) untrack(() => loadAgenda()) })
  const pageTitle: Record<PageName, string> = $derived({ recap: t.homeRecap, launch: t.menuLaunch, diary: t.menuRegister, night: nightTitle, overview: t.menuQuadro, search: t.menuSearch, settings: t.settingsTitle, queue: t.queueTitle })
  // «Utilizzo e limiti» (Utilizzo unito, approvato da Franz il 09/10 alle 16:17): niente più pagina. Il menu, la riga della
  // quota e il Registro portano alla sezione Utilizzo della home, che si apre e si illumina; sulla plancia senza colonne è
  // già a destra, nel cruscotto.
  let usageFocus = $state(0)
  function goUsage() { smooth(() => { page = null; open = null; masterOpen = false }); usageFocus += 1 }
  const openPage = (p: PageName | null) => { if (p === 'overview') goUsage(); else smooth(() => { page = p; if (p !== 'queue') queueSkip = null }) }
  if (asked === 'overview') queueMicrotask(goUsage)
  // La quota per account, come la Panoramica; i campioni del ritmo arrivano col trasporto.
  const overview = $derived(overviewOf(st, events, samples, now, undefined, down))
  let nightDlg: HTMLDialogElement | undefined = $state()
  // La ricerca nelle conversazioni (contratto 1.27): per ora la risposta di prova, poi il comando `search` al relay.
  let searchPage = $state<SearchPage | null>(null)
  // Gli avvisi delle altre sessioni chiusi con ✕ (una domanda non si chiude: resta finché qualcuno risponde).
  let seenAlerts = $state(new Set<string>())
  // Sulla plancia niente avviso per una sessione che è già in una colonna accanto.
  function elsewhereFor(name: string) {
    const a = elsewhereOf(st, name, now, new Set(sent.map(m => m.session)), seenAlerts)
    if (!a || !wide) return a
    return (a.type === 'waiting' ? a.sessions.every(n => cols.includes(n)) : cols.includes(a.session)) ? null : a
  }
  // Dall'avviso di una chat la coda mostra le sessioni che l'avviso conta, senza quella aperta: «2 sessioni ti aspettano»
  // apre «1 di 2» (15/10: diceva «1 di 1»). Dal menu tutte.
  let queueSkip = $state<string | null>(null)
  function openAlert(a: Alert, from: string | null = null) {
    if (a.type === 'finished') { seenAlerts = new Set([...seenAlerts, alertKey(a)]); pick(a.session) }
    else if (a.sessions.length === 1) pick(a.sessions[0])
    else openQueue(from)
  }
  const openQueue = (from: string | null = null) => { queueSkip = from; openPage('queue') }
  // Il tocco su «N per te» e sul banner (MainActivity, Tablet.toggle): sulla plancia la sessione che aspetta va in fondo alle
  // colonne, anche se c'era già, e il campo prende il fuoco; sul telefono si apre.
  function attend(n: string) {
    if (n === MASTER || !wide) { pick(n); return }
    setCols(toggleCol(cols.filter(c => c !== n), n))
    setTimeout(() => focusColumn(n), 400)
  }
  // Il tocco su una scheda della home: sulla plancia apre e chiude la sua colonna (Franz, 05/10 11:09), sul telefono apre.
  function card(name: string) { if (name !== MASTER && wide) setCols(toggleCol(cols, name)); else pick(name) }
  // Telefono: si scorre di lato fra la home e le sessioni, come nell'app (HorizontalPager con SwipePages; 15/10 20:24: «non
  // funziona lo scroll tra sessioni?»). Mentre una sessione è aperta l'ordine delle pagine resta quello di prima, le nuove in
  // fondo; si rifà tornando alla home. Si disegnano solo la pagina a schermo e le due accanto.
  let swipeOrder: Pages = []
  $effect.pre(() => { if (open == null) swipeOrder = [] })
  const pages = $derived.by(() => { const p = open == null ? swipeOf(live, null) : swipeStable(swipeOrder, swipeOf(live, open)); swipeOrder = p; return p })
  let strip: HTMLElement | undefined = $state()
  let stripAt = $state(0)
  let settledAt: number | null = null
  let settleTimer: ReturnType<typeof setTimeout> | undefined
  const stripTo = (i: number, near: boolean) => strip?.scrollTo({ left: i * strip.clientWidth, behavior: near ? 'smooth' : 'instant' })
  function settle() {
    if (!strip) return
    const i = Math.round(strip.scrollLeft / Math.max(1, strip.clientWidth))
    const go = swipeStep(settledAt, i, false, pages, open)
    settledAt = i
    if (go.type === 'open') open = go.name
    // Il riallineamento salta senza animazione: l'apertura da fuori ha già la transizione della pagina, e uno scorrimento
    // animato dentro la transizione si fermava a metà (15/10).
    else if (go.type === 'scrollTo') stripTo(go.page, false)
  }
  function swiped() {
    if (!strip) return
    fingerAt = Date.now()
    stripAt = strip.scrollLeft / Math.max(1, strip.clientWidth)
    clearTimeout(settleTimer)
    settleTimer = setTimeout(settle, 140)
  }
  // La striscia appena comparsa sta sulla sessione aperta, senza scorrere; poi segue le aperture da fuori (avviso, coda).
  const mountStrip = (el: HTMLElement) => {
    strip = el
    const i = Math.max(0, pages.indexOf(open))
    el.scrollLeft = i * el.clientWidth
    stripAt = i; settledAt = i
    return () => { strip = undefined; settledAt = null; clearTimeout(settleTimer) }
  }
  // Un'apertura da fuori (card, avviso, coda, «Vai a») sposta la striscia subito, dentro la transizione; mai mentre il dito scorre.
  let fingerAt = 0
  $effect(() => {
    void pages; void open
    untrack(() => { if (strip && settledAt != null && Date.now() - fingerAt > 200) { clearTimeout(settleTimer); settle() } })
  })
  const slots = $derived<(string | null)[]>(wide ? [masterOpen ? MASTER : null, ...cols] : [session ? session.name : masterOpen ? MASTER : null])

  // «Fuori dalle sessioni»: un messaggio con l'ora nel futuro è programmato (contratto 1.63).
  const scheduled = $derived(sent.map(m => (m.sentAt > now ? { ...m, sentAt: now, scheduledFor: m.sentAt } : m)))
  // Il tasto delle righe di servizio, come nell'app (MasterHome.Row): handoff, ascolta, aggiungi alla notte, avvia, vedi.
  function service(r: ForYouRow) {
    switch (r.kind) {
      case 'context': if (r.session) handoff(r.session); break
      case 'night_report': if (r.detail) toggle(r.detail); if (r.key) read = new Set([...read, r.key]); break
      case 'night': nightDlg?.showModal(); break
      case 'next_step':
        read = new Set([...read, nextKey(r.title, r.detail ?? '')])
        if (r.project) cmd(r.title)('launch', r.project, r.detail ?? undefined)
        break
      default: if (r.session) pick(r.session)
    }
  }
  // Il gesto Indietro del telefono (PredictiveBackHandler dell'app): una pagina, una sessione o la master aperta sono un
  // passo nella cronologia del browser, così Indietro torna alla home invece di uscire dalla web app.
  const depth = $derived((page ? 1 : 0) + (!wide && open ? 1 : 0) + (masterOpen ? 1 : 0))
  let pushed = 0
  let skipPop = false
  $effect(() => {
    const d = depth
    untrack(() => {
      while (pushed < d) history.pushState({ cm: ++pushed }, '')
      if (pushed > d) { skipPop = true; history.go(d - pushed); pushed = d }
    })
  })
  addEventListener('popstate', () => {
    if (skipPop) { skipPop = false; return }
    pushed = Math.max(0, pushed - 1)
    if (page) openPage(null)
    else if (!wide && open) smooth(() => { open = null })
    else if (masterOpen) smooth(() => { masterOpen = false })
  })
  let masterDraft = $state('')
  async function sendTo(name: string, text: string) {
    // Senza trasporto il messaggio resta «inviato al PC».
    const m: Sent = { id: crypto.randomUUID(), session: name, text, sentAt: nowS() }
    msgs = [...msgs, m]
    if (!tr) return
    pend = { ...pend, [m.id]: 'sending' }
    // Contratto 1.65: a una sessione chiusa il relay la riapre e poi consegna.
    const closed = st.sessions.find(x => x.name === name)?.state === 'gone'
    if (closed) say(t.reopening(name))
    try {
      const r = await tr.send({ ...newCmd('prompt', name, text), id: m.id, ...(closed ? { reopen: true } : {}) })
      res = { ...res, [m.id]: r }
    } catch { pend = { ...pend, [m.id]: 'failed' } }
  }
  // Contratto 1.64: un tocco = un comando; riuscito, si apre la sessione nata.
  async function newProject(parent: string, name: string, first: string, confirm: boolean): Promise<CmdResult | null> {
    if (!tr) return null
    const c = { ...newCmd('project_new', null, parent, name), ...(confirm ? { confirm: true } : {}), ...(first ? { prompt: first } : {}) }
    try {
      const r = await tr.send(c)
      if (r.ok) { openPage(null); if (r.session) pick(r.session) }
      return r
    } catch { say(`✗ ${t.noAnswer}`); return null }
  }
  // Contratto 1.63, «Manda alle…»: il relay tiene il prompt per quell'ora; in chat sta all'ora dell'invio, così il turno che
  // lo prende comincia da lì.
  async function sendAtTo(name: string, text: string, at: number) {
    const m: Sent = { id: crypto.randomUUID(), session: name, text, sentAt: at }
    msgs = [...msgs, m]
    if (!tr) return
    pend = { ...pend, [m.id]: 'sending' }
    try {
      const r = await tr.send({ ...newCmd('prompt', name, text), id: m.id, at })
      res = { ...res, [m.id]: r }
      if (!r.ok) say(`✗ ${r.text}`)
    } catch { pend = { ...pend, [m.id]: 'failed' } }
  }
  // Allegati: un messaggio per file, il testo con l'ultimo; ogni file va prima in /share/<id>, poi `report` con quell'id.
  // Contratto 1.50 (Franz, 10/10 16:52): se il relay lo sa fare, un messaggio solo per gruppo, col testo nel primo.
  async function attach(name: string, files: File[], text: string) {
    if (together(st.share, files.length)) {
      for (const [g, group] of groups(files, st.share).entries()) await attachTogether(name, group, g === 0 ? text : '')
      return
    }
    for (const [i, f] of files.entries()) {
      const m: Sent = { id: crypto.randomUUID(), session: name, text: i === files.length - 1 ? text : '', sentAt: nowS(), attachment: f.name }
      msgs = [...msgs, m]
      if (!tr) continue
      const sid = crypto.randomUUID()
      uploads = { ...uploads, [m.id]: { type: 'going' } }
      try {
        await tr.share(sid, f.type || 'application/octet-stream', new Uint8Array(await f.arrayBuffer()), f.name)
      } catch (e) {
        uploads = { ...uploads, [m.id]: { type: 'failed', reason: e instanceof Error ? e.message : String(e) } }
        continue
      }
      const { [m.id]: _, ...rest } = uploads
      uploads = rest
      pend = { ...pend, [m.id]: 'sending' }
      try {
        res = { ...res, [m.id]: await tr.send({ ...newCmd('report', name, sid, m.text || undefined), id: m.id }) }
      } catch { pend = { ...pend, [m.id]: 'failed' } }
    }
  }
  async function attachTogether(name: string, files: File[], text: string) {
    const m: Sent = { id: crypto.randomUUID(), session: name, text, sentAt: nowS(), attachments: files.map(f => f.name) }
    msgs = [...msgs, m]
    if (!tr) return
    uploads = { ...uploads, [m.id]: { type: 'going' } }
    const ids: string[] = []
    for (const f of files) {
      const sid = crypto.randomUUID()
      try {
        await tr.share(sid, f.type || 'application/octet-stream', new Uint8Array(await f.arrayBuffer()), f.name)
      } catch (e) {
        uploads = { ...uploads, [m.id]: { type: 'failed', reason: e instanceof Error ? e.message : String(e) } }
        return
      }
      ids.push(sid)
    }
    const { [m.id]: _, ...rest } = uploads
    uploads = rest
    pend = { ...pend, [m.id]: 'sending' }
    try {
      res = { ...res, [m.id]: await tr.send({ ...newCmd('report', name, ids.join(','), text || undefined), id: m.id }) }
    } catch { pend = { ...pend, [m.id]: 'failed' } }
  }
  // Un file della chat: il comando `file`, poi i byte da /api/file/<id>; il menu del file li chiede appena si apre
  // («prepare») e l'anteprima appena la card è a schermo. Restano pronti finché la chat è a schermo (`FileStore`), con la
  // chiave del file della chat (`fileKey`): lo stesso percorso mandato di nuovo con un'altra misura non riprende quello vecchio.
  const fileStore = new FileStore()
  $effect(() => { fileStore.keep([MASTER, ...(open ? [open] : [])]) })
  const loadFile = (name: string, path: string, quiet: boolean) => async (): Promise<Fetched | null> => {
    if (!tr) return null
    const c = newCmd('file', name, path)
    // L'anteprima che nasce da sola non avvisa se il file non arriva.
    const r = quiet ? await tr.send(c).catch(() => null) : await run(c)
    if (!r?.ok) return null
    const f = await tr.fetchFile(c.id).catch(() => null)
    if (!f) { if (!quiet) say(`✗ ${t.noAnswer}`); return null }
    return { blob: new Blob([f.data as Uint8Array<ArrayBuffer>], { type: f.mime }), name: f.name ?? path.split('/').pop() ?? 'file', mime: f.mime }
  }
  const getFile = (name: string, path: string, key: string, quiet = false) => fileStore.get(key, loadFile(name, path, quiet))
  function fileAction(name: string, path: string, act: FileAct | 'prepare' | 'preview', key: string) {
    if (!tr) return
    if (act === 'prepare') { void getFile(name, path, key, true); return }
    if (act === 'preview') { void preview(name, path, key); return }
    // Niente await prima dell'azione quando il file è pronto: condividi e copia vogliono il tocco ancora vivo.
    if (!fileStore.peek(key)) say(t.fileOpening(path.split('/').pop() ?? path))
    const done = fileStore.tap(key, loadFile(name, path, false), f => {
      notice = null
      if (act === 'open') files.open(f)
      else if (act === 'download') files.download(f)
      else if (act === 'copy') return files.copy(f).then(k => { if (k) say(t.fileCopied(k)) })
      else return files.share(f, path)
    })
    Promise.resolve(done).catch(e => {
      // Se il file è arrivato dopo il tocco, il tocco è scaduto: al secondo è già pronto e parte subito.
      const n = (e as Error)?.name
      if (n === 'NotAllowedError') say(t.fileTapAgain)
      else if (n !== 'AbortError') say(`✗ ${act === 'share' ? t.fileShareFailed : t.fileCopyFailed}`)
    })
  }
  async function preview(name: string, path: string, key: string) {
    // Ogni allegato in anteprima sotto il suo nome (Franz, 10/10 15:24 per immagini e video, 15/10 22:20 per tutti): i PDF
    // con la prima pagina, i testi con le prime righe.
    if (media[key] || texts[key] || failed[key]) return
    const f = await getFile(name, path, key, true)
    if (!f || media[key] || texts[key]) return
    const kind = previewKind(f.mime, path)
    if (kind === 'pdf') { const png = await firstPage(f.blob); if (png) media[key] = URL.createObjectURL(png); else failed[key] = true }
    else if (kind === 'text') texts[key] = firstLines(await f.blob.text())
    // Franz, 16/10 11:18: HTML e SVG come pagina nel riquadro sandbox; le prime righe restano per «Codice».
    else if (kind === 'page') { const text = await f.blob.text(); pageSources[key] = { mime: f.mime, text }; texts[key] = firstLines(text) }
    else media[key] = URL.createObjectURL(f.blob)
  }
  // «Chiedi alla master» arriva come `prompt` della sessione: va alla master, come messaggio nella sua chat.
  const cmd = (name: string) => (op: CmdOp, arg?: string, text?: string) => {
    if (op === 'prompt' && arg) sendTo(MASTER, arg)
    else if (op === 'slash' && arg === 'exit' && name && name !== MASTER) {
      const l: Leaving = { name, sentAt: nowS(), delivered: false }
      leaving = l
      run(newCmd(op, name, arg, text)).then(r => { if (r && leaving?.name === name && leaving.sentAt === l.sentAt) leaving = { ...l, delivered: true } })
    } else run(newCmd(op, name || null, arg, text, st.sessions.find(x => x.name === name)?.question?.id))
  }
  // La conversazione di quello che è a schermo, la master sempre (vive nella home), alla cadenza del telefono e una lettura
  // alla volta per la pagina (piano prestazioni, Task 5: 888 letture all'ora il 07/10). Il passo dei 5 s fa partire le
  // letture a cadenza anche senza uno stato nuovo; la fine di una lettura fa partire la prossima.
  const reads = new Map<string, LastRead & { key: string }>()
  // Da quando la lettura in volo di ogni conversazione aspetta il PC (ms): la riga «in attesa del PC» della chat (Task 7).
  let waitSince = $state<Record<string, number>>({})
  let readTick = $state(0)
  if (tr) setInterval(() => readTick++, 5_000)
  // Le chat che non sono a schermo si rileggono in sottofondo (Franz, 14/10 18:39: «vedo per qualche secondo il testo
  // vecchio»): a ogni attività nuova, al massimo ogni 12 s per sessione, due alla volta; con la pagina nascosta niente, al
  // ritorno partono subito quelle con novità. `fresh`: le chat a schermo che hanno avuto la loro lettura da quando sono
  // aperte; fino ad allora una copia più vecchia dell'ultima attività dice che si aggiorna.
  let bgReads = $state<Record<string, BgRead>>({})
  let fresh = $state<Record<string, boolean>>({})
  let bgBusy = 0
  let shownBefore = new Set<string>()
  function bgTick() {
    // Senza /tail (il relay locale) ogni rilettura in sottofondo è un `transcript` che fa lavorare il PC: 1.378 il 15/10 entro
    // le 11:30, circa 6 s ciascuno (Franz, 11:40). Lì si legge solo la chat aperta, come prima del 14/10.
    if (!tr || document.hidden || !tr.fetchTail) return
    for (const n of due(st.sessions, bgReads, [MASTER, ...slots], Date.now())) {
      if (bgBusy >= BG_PARALLEL) return
      const s = st.sessions.find(x => x.name === n)
      if (!s || loadingT.has(n)) continue
      const sig = activity(s), at = Date.now()
      bgReads[n] = { sig, atMs: at, done: false }
      bgBusy++
      loadTail(n).then(ok => ok || loadTranscript(n)).then(ok => { bgReads[n] = { sig: ok ? sig : '', atMs: at, done: ok } }).finally(() => { bgBusy-- })
    }
  }
  if (tr) { setInterval(bgTick, 1_000); document.addEventListener('visibilitychange', bgTick) }
  $effect(() => {
    void st.ts; void readTick
    const names = [...new Set([MASTER, ...slots])]
    untrack(() => {
      // Una chat appena aperta si legge subito, senza aspettare la cadenza della volta prima.
      for (const n of names) if (n && !shownBefore.has(n)) { reads.delete(n); delete fresh[n] }
      shownBefore = new Set(names.filter((n): n is string => !!n))
      // Una lettura in sottofondo in volo non ferma quelle a schermo.
      if (names.some(n => n && loadingT.has(n))) return
      const now = nowS()
      for (const n of names) {
        const s = n ? st.sessions.find(x => x.name === n) : undefined
        if (!n || !s) continue
        const key = `${s.state}|${s.turn_started ?? ''}`
        const last = reads.get(n)
        if (!shouldRead(s, last, !!last && last.key !== key, now)) continue
        reads.set(n, { at: now, answered: false, key })
        waitSince[n] ??= Date.now()
        const sig = activity(s), at = Date.now()
        // Contratto 1.59: la prima lettura di una chat appena aperta parte da /tail, poi le voci dopo l'ultima.
        ;(last ? Promise.resolve(false) : loadTail(n)).then(t => { if (t) fresh[n] = true; return loadTranscript(n) })
          .then(ok => { if (ok) { fresh[n] = true; bgReads[n] = { sig, atMs: at, done: true } } })
          .finally(() => { const r = reads.get(n); if (r) r.answered = true; delete waitSince[n]; readTick++ })
        return
      }
    })
  })
  // La ricerca (1.27) e la cronologia di oggi per i dettagli della prima colonna (1.29).
  async function search(q: string) {
    const r = await run(newCmd('search', null, q))
    if (r?.ok) searchPage = JSON.parse(r.text)
  }
  let timeline = $state<TimelinePage | null>(null)
  // Al massimo una al minuto per la stessa sessione, subito per una sessione nuova (piano prestazioni, Task 5).
  let timelineAsked = { name: '', at: 0 }
  $effect(() => {
    if (!tr || !details || !cols[0]) return
    const name = cols[0]
    void st.ts
    const now = nowS()
    if (timelineAsked.name === name && now - timelineAsked.at < 60) return
    timelineAsked = { name, at: now }
    untrack(() => run(newCmd('timeline', name, timelineArg(now)))).then(r => { if (r?.ok) timeline = JSON.parse(r.text) })
  })
  const answer = (n: string, x: number) => { pick(n); cmd(n)('answer', String(x)) }
</script>

<svelte:window bind:innerWidth={width} />

{#if pairing}
<PairPage {store} onPaired={() => location.reload()} />
{:else}

{#snippet chatOf(name: string, inColumn: boolean)}
  {@const s = (st.sessions.find(x => x.name === name) ?? ghost(name))!}
  <Chat {st} {s} entries={transcripts[name] ?? []} mine={mine.filter(([m]) => m.session === name)} onSend={(x) => sendTo(name, x)} onPick={pick} waitingSince={waitSince[name] ?? null}
    updating={!!tr && s.state !== 'gone' && !fresh[name] && behind(s, bgReads[name])}
    onSendAt={st.ops?.includes('project_new') && name !== MASTER ? (x, at) => sendAtTo(name, x, at) : null}
    onSendTo={name === MASTER ? (n, x) => { sendTo(n, x); say(t.mentionSent(n)) } : null}
    onAnswer={answer} onCmd={cmd(name)} {events} {sent} {read} onRead={(k) => (read = new Set([...read, k]))} onPromptTo={sendTo} onAttach={(fs, x) => attach(name, fs, x)} onFile={(p, a, k) => fileAction(name, p, a, k)} onHandoff={() => handoff(name)} onDecision={decide}
    wide={false} {slots} elsewhere={elsewhereFor(name)} onElsewhere={() => { const a = elsewhereFor(name); if (a) openAlert(a, name) }}
    onElsewhereDismiss={() => { const a = elsewhereFor(name); if (a) seenAlerts = new Set([...seenAlerts, alertKey(a)]) }} onBack={inColumn ? undefined : () => smooth(() => { open = null })}
    inject={name === MASTER ? masterInject : null} onInjected={() => (masterInject = null)} onUsage={goUsage} onOlder={olderOf[name] ? () => loadOlder(name) : null}
    onGo={inColumn ? null : (n) => (n == null ? smooth(() => { open = null }) : pick(n))} onReopen={st.ops?.includes('reopen') ? (n) => cmd(n)('reopen') : null} appMenu={inColumn ? null : appMenu}
    onDiff={st.ops?.includes('diff') ? (id) => ask(tr, newCmd('diff', name, id), decodeDiff) : null}
    onChanges={st.ops?.includes('changes') ? (f) => ask(tr, newCmd('changes', name, f == null ? null : fileArg(f)), decodeChanges) : null} />
{/snippet}

{#snippet appMenu()}<AppBar {st} now={now} openCount={summary.open} onPage={openPage} menuOnly />{/snippet}

{#snippet homePane()}
  <HomePane {master} entries={transcripts[MASTER] ?? []} open={masterOpen} onToggle={(o) => smooth(() => { masterOpen = o; masterHalf = o && wide })} onSpeak={(x) => toggle(x, MASTER)}
    halfStops={wide} half={masterHalf} onHalf={(h) => (masterHalf = h)}>
    {#snippet menu()}<AppBar {st} now={now} openCount={summary.open} onPage={openPage} menuOnly />{/snippet}
    {#snippet quick()}{@render masterQuick()}{/snippet}
    {#snippet list()}
      {#if demo}<div class="demobar">{t.demoBanner}</div>{/if}
      <AppBar {st} now={now} openCount={summary.open} onPage={openPage} onAttention={() => { const n = st.attention?.[0]?.session; if (n) { openPage(null); attend(n) } }} />
      <div class="list"><Home {st} selected={[]} onPick={card} onAnswer={answer} onStep={(n, x) => { pick(n); sendTo(n, x) }} usage={quotaPanels}
        night={nightBoxShown ? nightModel : undefined} {nightTitle} {justClosed} onNight={() => openPage('night')}
        onApprove={approve} onClose={(n) => cmd(n)('slash', 'exit')} onSessionCmd={(n, op, arg) => cmd(n)(op, arg)} onRecapAction={(a) => sendTo(a.to, a.send)} agenda={agenda.page} onRecapPage={() => openPage('recap')} {canWrite} onTalk={talk} onEdit={editAgenda}
        showRecap={!deskDashboard} showUsage={!deskDashboard} {usageFocus} onUsageFocused={() => (usageFocus = 0)} onSpeak={(x) => toggle(x)}
        onAttention={() => { const n = st.attention?.find(a => a.session && a.session !== MASTER)?.session; if (n) attend(n) }}
        {events} sent={scheduled} {read} {now} onService={service} lastNight={nightModel} onReopen={st.ops?.includes('reopen') ? (n) => cmd(n)('reopen') : null} /></div>
      {#if !master && ready}
        <!-- Senza master (MasterHome.kt): una card con «Riapri», invece della barra che non c'è. -->
        <div class="nomaster"><span><b>{t.masterAbsent}</b><small>{t.masterAbsentDetail}</small></span>
          {#if st.ops?.includes('reopen')}<button onclick={() => cmd(MASTER)('reopen')}>{t.reopen}</button>{/if}</div>
      {/if}
      <div class="reading"><ReadingPill {slots} here={null} onOpen={pick} /></div>
    {/snippet}
    {#snippet chat()}{@render chatOf(MASTER, true)}{/snippet}
  </HomePane>
{/snippet}

{#snippet masterQuick()}
  {@const m = st.sessions.find(x => x.name === MASTER)}
  {#if m}
    <!-- Il campo della master da chiusa (Franz, 06/10 10:45-10:54): lo stesso della conversazione, + compreso; mandare apre
         la conversazione. -->
    <div class="mquick">
      <Composer {st} s={m} bind:draft={masterDraft} field={null} toMaster onSend={(x) => { sendTo(MASTER, x); smooth(() => { masterOpen = true }) }}
        onAnswerText={(arg) => cmd(MASTER)('answer', arg)} onSlash={(c, a) => { cmd(MASTER)('slash', c, a ?? undefined); smooth(() => { masterOpen = true }) }}
        onStop={() => cmd(MASTER)('interrupt')} onReopen={() => cmd(MASTER)('reopen')}
        onAttach={(fs, x) => { attach(MASTER, fs, x); smooth(() => { masterOpen = true }) }} onRecurring={null} />
    </div>
  {/if}
{/snippet}

{#snippet pageView(p: PageName)}
  <Page title={pageTitle[p]} onBack={() => openPage(null)}>
    {#if p === 'launch'}
      <Launch {st} onSession={(n, reopen) => { if (reopen) cmd(n)('reopen'); else { openPage(null); pick(n) } }}
        onLaunch={(pr, first) => { cmd(pr.name)('launch', pr.path, first || undefined); openPage(null) }}
        onNewProject={st.ops?.includes('project_new') && st.project_parents?.length ? newProject : null} />
    {:else if p === 'diary'}
      <Diary {st} {events} rings={overview.rings} now={now} onAdd={() => nightDlg?.showModal()} onRemove={(id) => cmd('')('night_remove', id)}
        onQuadro={() => openPage('overview')} onSession={(n) => { openPage(null); pick(n) }} />
    {:else if p === 'night'}
      <NightPage model={nightModel} error={night.error} loading={night.loading} onRefresh={loadNight}
        onChat={(n) => { openPage(null); pick(n) }} onAnswer={(n, x) => cmd(n)('answer', String(x))}
        onApprove={(task) => approve(task, '')} onSend={(n, text) => sendTo(n, text)} />
    {:else if p === 'recap'}
      <RecapPage {actions} day={recapDay} agenda={agenda.page} loading={agenda.loading}
        error={canAgenda ? agenda.error : t.recapAgendaOld} onSend={(a) => sendTo(a.to, a.send)} {canWrite} onTalk={talk} onEdit={editAgenda} onSpeak={(x) => toggle(x)} />
    {:else if p === 'search'}
      <Search {sent} {events} remote onQuery={search} page={searchPage}
        known={new Set(st.sessions.map(x => x.name))} onOpen={(n) => { if (n) { openPage(null); pick(n) } else openPage('diary') }} />
    {:else if p === 'settings'}
      <Settings m={devicesOf(st.host, st, freshness(st, now), now, '', __APP_VERSION__, true, null, false, null)}
        devices={linkedDevices(st, remote?.uid ?? null, now) ?? []} now={now} channel={access ? t.channelLocal : remote ? t.channelFirebase : t.channelDemo} onRate={setRate} onVoice={setVoice}
        details={wide ? details : null} onDetails={(on) => { details = on; save('cm.details', on ? '1' : '0') }} {push} onPush={setPush}
        onAdd={tr && st.ops?.includes('pair_add') ? () => addDlg?.open() : null} demo={!!demo} />
    {:else if p === 'queue'}
      <Queue {st} now={now} skip={queueSkip} onAnswer={(n, arg, op = 'answer') => cmd(n)(op, arg || undefined)} onSession={(n) => { openPage(null); pick(n) }} />
    {:else}
      <p class="soon">{t.soon}</p>
    {/if}
  </Page>
{/snippet}

<!-- Un account fermo (dato vecchio) non ha il suo riquadro se l'altro è aggiornato, come nella riga in alto (Franz, 08/10 20:32). -->
{#snippet quotaPanels()}{#each overview.rings.some(r => !r.stale) ? overview.rings.filter(r => !r.stale) : overview.rings as r (r.account)}<QuotaPanel ring={r} now={now} />{/each}<TodayPanel bars={overview.today} />{/snippet}

{#snippet inspector()}
  {@const first = st.sessions.find(x => x.name === cols[0])}
  {#if first}<Inspector i={inspect(first, timeline, now)} quotaH5={st.quota[first.account]?.h5 ?? null} loading={!!tr && !timeline} />{/if}
{/snippet}

{#snippet columnTab(name: string, grab: (e: PointerEvent) => void)}
  {@const s = st.sessions.find(x => x.name === name)}
  {#if s}<ColumnTab {s} r={rowOf[name]} {now} onGrab={grab} onFocus={() => focusColumn(name)} onClose={() => setCols(cols.filter(c => c !== name))} />{/if}
{/snippet}

{#snippet deskPage()}{#if page}{@render pageView(page)}{/if}{/snippet}

{#if wide}
  <Desk {cols} {shares} {homeRight} tab={wco ? columnTab : null} onCols={setCols} onShares={setShares} override={page ? deskPage : null} details={details && cols.length && !page ? inspector : null}
    onHomeSide={() => smooth(() => { homeRight = !homeRight; save('cm.home_right', homeRight ? '1' : '0') })}>
    {#snippet home()}{@render homePane()}{/snippet}
    {#snippet column(name, grab)}
      {@const held = heldCols.includes(name)}
      {@const phase = splashOf(name, true)}
      {@const hr = headRow(name)}
      <div class="column" data-col={name}>
        {#if hr && !wco}<ColumnHead r={hr} now={now} onClose={() => (held ? dropCol(name) : setCols(cols.filter(c => c !== name)))} onGrab={grab} />{/if}
        <div class="cbody">{@render chatOf(name, true)}
          {#if phase}<CloseSplash {phase} column onHome={() => dropCol(name)} onReopen={phase.kind === 'closed' && st.ops?.includes('reopen') ? () => { cmd(name)('reopen'); dropCol(name) } : null} />{/if}
        </div>
      </div>
    {/snippet}
    <!-- Senza colonne il cruscotto (variante B, approvata da Franz il 09/10 alle 16:17): Recap e Utilizzo larghi. -->
    {#snippet empty()}
      <div class="dash">
        <section class="dcol"><h3 class="dt"><span>{t.homeRecap.toUpperCase()}</span><i></i></h3>
          <div class="dscroll"><RecapPage {actions} day={recapDay} agenda={agenda.page} loading={agenda.loading}
            error={canAgenda ? agenda.error : t.recapAgendaOld} onSend={(a) => sendTo(a.to, a.send)} {canWrite} onTalk={talk} onEdit={editAgenda} onSpeak={(x) => toggle(x)} /></div></section>
        <section class="dcol"><h3 class="dt"><span>{t.homeUsage.toUpperCase()}</span><i></i></h3>
          <div class="dscroll usage">{@render quotaPanels()}</div></section>
      </div>
    {/snippet}
  </Desk>
{:else}
  <div class="phone">
    <InAppBar />
    {#if page}{@render pageView(page)}{:else}
      {@const phase = session ? splashOf(session.name) : null}
      <div class="strip" {@attach mountStrip} onscroll={swiped}>
        {#each pages as p, i (p ?? '')}
          <div class="pg">
            {#if i >= Math.floor(stripAt) - 1 && i <= Math.ceil(stripAt) + 1}
              {#if p == null}{@render homePane()}
              {:else if session && p === session.name}
                {@const sn = session.name}
                <div class="cbody" style:view-transition-name="page-fly">{@render chatOf(sn, false)}
                  {#if phase}<CloseSplash {phase} onHome={splashHome} onReopen={phase.kind === 'closed' && st.ops?.includes('reopen') ? () => { cmd(sn)('reopen'); splashHome() } : null} />{/if}
                </div>
              {:else}<div class="cbody">{@render chatOf(p, false)}</div>{/if}
            {/if}
          </div>
        {/each}
      </div>
    {/if}
  </div>
{/if}
<AddDevice bind:this={addDlg} origin={CANONICAL} ask={() => tr ? tr.send(newCmd('pair_add', null, null)) : Promise.resolve(null)} />
{#if !demo && (!ready || down)}<span class="demo mono">{ready ? t.relayDown : t.relayConnecting}</span>{/if}
{#if notice}<div class="notice" role="status">{notice}</div>{/if}
{/if}

<!-- «Aggiungi alla notte»: lo stesso foglio di Lancia, solo progetti (LaunchSheet con night_add). -->
<dialog bind:this={nightDlg} class="sheet" onclick={(e) => e.target === e.currentTarget && nightDlg?.close()}>
  <h2>{t.nightAddTitle}</h2>
  <NightAgendaPicker onDone={() => nightDlg?.close()} />
  <Launch {st} action={t.nightAddTitle} onLaunch={(pr, text) => { cmd(pr.name)('night_add', pr.path, text); nightDlg?.close() }} />
</dialog>

<style>
  .mquick { padding: 0 0 4px; }
  .phone { height: 100%; display: flex; flex-direction: column; max-width: 760px; margin: 0 auto; }
  .list { flex: 1; min-height: 0; }
  .strip { flex: 1; min-height: 0; display: flex; overflow-x: auto; overflow-y: hidden; scroll-snap-type: x mandatory; overscroll-behavior-x: contain; scrollbar-width: none; }
  .strip::-webkit-scrollbar { display: none; }
  .demobar { flex: none; padding: 6px 16px; background: color-mix(in srgb, var(--wait) 22%, var(--bg)); color: var(--wait); font-size: 13px; font-weight: 500; }
  .pg { flex: 0 0 100%; min-width: 0; scroll-snap-align: start; scroll-snap-stop: always; display: flex; flex-direction: column; }
  .reading:empty { display: none; }
  .nomaster { display: flex; align-items: center; gap: 12px; margin: 8px 16px; padding: 14px 16px; border-radius: 20px; background: var(--low); }
  .nomaster span { flex: 1; display: flex; flex-direction: column; gap: 2px; }
  .nomaster small { color: var(--text2); font-size: 14px; }
  .nomaster button { height: 44px; padding: 0 20px; border-radius: 999px; background: var(--primary); color: var(--on-primary); font-weight: 500; }
  .reading { padding: 8px 0; }
  .column { height: 100%; display: flex; flex-direction: column; }
  .dash { position: absolute; inset: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 18px; padding: 0 16px 0 14px; text-align: left; place-items: stretch; color: var(--text); }
  @media (max-width: 1240px) { .dash { grid-template-columns: 1fr; grid-template-rows: 3fr 2fr; } }
  .dcol { min-height: 0; display: flex; flex-direction: column; }
  .dt { display: flex; align-items: center; gap: 8px; margin: 14px 4px 8px; font: 500 12px/1 var(--mono); letter-spacing: .14em; color: var(--text2); }
  .dt i { flex: 1; height: 1px; background: var(--line); }
  .dscroll { flex: 1; min-height: 0; overflow-y: auto; }
  .dscroll.usage { display: flex; flex-direction: column; gap: 12px; padding-bottom: 16px; }
  .cbody { flex: 1; min-height: 0; position: relative; display: flex; flex-direction: column; }
  .soon { color: var(--text2); padding: 24px; }
  .sheet { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 0 0; width: min(600px, 100vw); max-height: 90vh; border-radius: 28px; }
  .sheet::backdrop { background: rgb(0 0 0 / .55); }
  .sheet h2 { font-size: 22px; font-weight: 600; padding: 0 20px; }
  @media (max-width: 599px) { .sheet { margin: auto 0 0; max-width: 100vw; border-radius: 28px 28px 0 0; } }
  .notice { position: fixed; left: 50%; bottom: 72px; transform: translateX(-50%); max-width: min(560px, calc(100vw - 32px)); padding: 10px 16px; border-radius: 16px; background: var(--surface); color: var(--text); box-shadow: 0 4px 16px rgb(0 0 0 / .35); z-index: 20; }
  .demo { position: fixed; right: 12px; bottom: 6px; opacity: .6; pointer-events: none; }
  @media (max-width: 839px) { .demo { bottom: 0; right: 50%; transform: translateX(50%); font-size: 10px; } }
</style>
