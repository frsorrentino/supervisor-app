<script lang="ts">
  import { locale } from './lib/lang'
  import * as files from './lib/fileActions'
  import { seenByPath } from './lib/guardare'
  import type { MenuKind } from './lib/sessionMenu'
  import { BAR_H, COLUMN, COMPACT_H, heightZones, mirrorRect, placeRect, sideAt, steady, widthAt, widthRect, widthStops, type Height, type Rect, type Side } from './lib/masterLanding'
  import { ghostPreview, MOVE_MS, PLACES } from './lib/landingMorph'
  import { DRAG_PX, isDrag, masterTaps, springFrames, step, velocity } from './lib/gesture'
  import { morphBlob } from './lib/morphBlob'
  import { failed, media, pages as pageSources, texts } from './lib/mediaCache.svelte'
  import { firstLines, previewKind } from './lib/media'
  import { firstPage } from './lib/pdfPreview'
  import { groups, together } from './lib/shareMulti'
  import type { FileAct, Fetched } from './lib/fileActions'
  import { FileStore } from './lib/fileStore'
  import { zipName } from './lib/zip'
  import { DemoTransport } from './lib/demoTransport'
  import Features from './lib/Features.svelte'
  import DaProvare from './lib/DaProvare.svelte'
  import TiAspettaView from './lib/TiAspettaView.svelte'
  import { decodeDecisions, decodeProve, readPage } from './lib/decideDecode'
  import type { DecisionGroup, ProvePage } from './lib/contract'
  import PrimaRaccolta from './lib/PrimaRaccolta.svelte'
  import Seguiti from './lib/Seguiti.svelte'
  import { autoOpen as primaAutoOpen } from './lib/primaRaccolta'
  import CinqueMinuti from './lib/CinqueMinuti.svelte'
  import { suggestionNames as suggestionNamesOf } from './lib/slotInfo'
  import { nightTitleParts, okAlso } from './lib/decide'
  import InAppBar from './lib/InAppBar.svelte'
  import { setContext, untrack, tick } from 'svelte'
  import type { Attachment } from 'svelte/attachments'
  import { moveInto, offstage, park } from './lib/park'
  import Badge from './lib/Badge.svelte'
  import { morphStart } from './lib/motion'
  import { flatten, lift, settle as settleCard } from './lib/cardFx'
  import { fly, retractNeck } from './lib/director'
  import type { AgendaPage, Cmd, CmdOp, CmdResult, DonePage, Event, KpiData, PluginsPage, SearchPage, Session, State, TimelinePage, TranscriptEntry, TranscriptPage } from './lib/contract'
  import { older } from './lib/chatFeed'
  import { blankState, decodeChanges, decodeDiff, transcriptArg, withAfterPage, withFirstPage, withTail } from './lib/contract'
  import { cleanEvent } from './lib/alertEvents'
  import { ask } from './lib/changes'
  import { fileArg } from './lib/diff'
  import { advance, prune, status, type PendingStatus, type Sent, type Status, type Upload } from './lib/chatRules'
  import type { Sample } from './lib/quotaHistory'
  import { LocalTransport, localAccess, newCmd, type Transport } from './lib/transport'
  import { CANONICAL, demoFallback, demoLink, demoReason, forget, loadRemote, needsHomeScreen, NO_DATA_MS, realLink, remoteFor, remoteTransport, wantsPairing } from './lib/remoteAccess'
  import { currentSubscription, pushStatus, subscribe, subscriptionDoc } from './lib/push'
  import PairPage from './lib/PairPage.svelte'
  import { approveText, canClear, clearDue, handoffPrompt } from './lib/masterService'
  import { toggle } from './lib/speech.svelte'
  import Home from './lib/Home.svelte'
  import Chat from './lib/Chat.svelte'
  import Desk from './lib/Desk.svelte'
  import { decode as decodeCats, top as topOf } from './lib/homeCats'
  import Dash from './lib/Dash.svelte'
  import Plancia from './lib/Plancia.svelte'
  import HomeOrder from './lib/HomeOrder.svelte'
  import Utilizzo from './lib/Utilizzo.svelte'
  import UsagePage from './lib/UsagePage.svelte'
  import MasterDock from './lib/MasterDock.svelte'
  import SwapPills from './lib/SwapPills.svelte'
  import DeskHead from './lib/DeskHead.svelte'
  import DeskTools from './lib/DeskTools.svelte'
  import DeskNight from './lib/DeskNight.svelte'
  import DeskWork from './lib/DeskWork.svelte'
  import { dashColumns } from './lib/dash'
  import ColumnHead from './lib/ColumnHead.svelte'
  import { t } from './lib/t'
  import HomePane from './lib/HomePane.svelte'
  import DockEmbed from './lib/DockEmbed.svelte'
  import { DOCK_CACHE, dockCacheOf, dockWanted, readDockCache } from './lib/dockEmbed'
  import Lavori from './lib/Lavori.svelte'
  import { NIGHT_TARGET, night as nightCards, setCmd, todayIso } from './lib/recapAgenda'
  import NightAgendaPicker from './lib/NightAgendaPicker.svelte'
  import Composer from './lib/Composer.svelte'
  import Guardare from './lib/Guardare.svelte'
  import ResultsRow from './lib/ResultsRow.svelte'
  import AppBar, { type Page as PageName } from './lib/AppBar.svelte'
  import HomeBox from './lib/HomeBox.svelte'
  import Page from './lib/Page.svelte'
  import Launch from './lib/Launch.svelte'
  import NightPage from './lib/NightPage.svelte'
  import { page as nightPage, showHomeNight, type NightReport } from './lib/night'
  import TodayPanel from './lib/TodayPanel.svelte'
  import { usageRows } from './lib/usageToday'
  import { visible as visibleSuggestions } from './lib/suggestions'
  import SuggestionsCard from './lib/SuggestionsCard.svelte'
  import { readPreview, type Preview } from './lib/closeFinished'
  import Search from './lib/Search.svelte'
  import FrameMeter from './lib/FrameMeter.svelte'
  import { meter } from './lib/meterSwitch.svelte'
  import Settings from './lib/Settings.svelte'
  import Feedback from './lib/Feedback.svelte'
  import { bytesOf, deviceOf, grabTab, parseChoice, parseDraft, relayKind, relayTech, type Draft } from './lib/feedback'
  import AddDevice from './lib/AddDevice.svelte'
  import Queue from './lib/Queue.svelte'
  import QuotaPanel from './lib/QuotaPanel.svelte'
  import Inspector from './lib/Inspector.svelte'
  import ColumnTab from './lib/ColumnTab.svelte'
  import { alert as elsewhereOf, away as awayOf, key as alertKey, keys as alertKeys, type Alert } from './lib/elsewhere'
  import { settled, withoutApproved } from './lib/approvals'
  import { content as elicitContent, openElicitations, type Values } from './lib/elicit'
  import type { Elicitation, Suggestion } from './lib/contract'
  import { build as devicesOf, linked as linkedDevices } from './lib/devices'
  import { freshness } from './lib/durations'
  import { setRate, setVoice } from './lib/speech.svelte'
  import { build as overviewOf } from './lib/overview'
  import ReadingPill from './lib/ReadingPill.svelte'
  import CloseSplash from './lib/CloseSplash.svelte'
  import { closeRefusals, closeSplash, ghostSession, type Leaving } from './lib/closeSplash'
  import { askedSession, askedWait } from './lib/sessionLinks'
  import { build, finishedKey, MASTER } from './lib/summary'
  import { nextKey, type ForYouRow } from './lib/masterHome'
  import { since as kpiSince, head as kpiHeadOf } from './lib/kpi'
  import { add, columns, columnsFromPref, columnsPref, inspect, replace as replaceCol, sharesFromPref, sharesPref, timelineArg, wide as isWide } from './lib/tablet'
  import { refusalText, superseded } from './lib/questionRules'
  import { shouldRead, due, activity, behind, BG_PARALLEL, firstTails, type LastRead, type BgRead } from './lib/readPlan'
  import { of as swipeOf, stable as swipeStable, step as swipeStep, type Pages } from './lib/swipePages'

  const load = (k: string) => { try { return localStorage.getItem(k) } catch { return null } }
  const save = (k: string, v: string) => { try { localStorage.setItem(k, v) } catch { /* resta per la sessione */ } }
  const json = <T,>(k: string, d: T): T => { try { return JSON.parse(load(k) ?? '') as T } catch { return d } }
  const nowS = () => Math.round(Date.now() / 1000)
  const startedAt = Date.now()

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
  // Il contenuto delle colonne entra con un'assolvenza breve al primo stato (app-avvio-senza-salto-04-11).
  let bootFade = $state(false)
  let down = $state(false)
  let clock = $state(nowS())
  // L'ora delle età e dei conti: quella vera con il relay, quella della fixture nella demo.
  const now = $derived(clock)
  let events = $state<Event[]>(demo ? demo.world.events.map(cleanEvent) : [])
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
      // Niente emoji nell'app (Franz, 22/10): gli eventi si puliscono qui, una volta, per tutte le pagine.
      const fresh = (await tr.fetchEvents(events[0]?.ts ?? 0)).map(cleanEvent)
      if (!fresh.length) return
      const keys = new Set(fresh.map(e => e.key))
      events = [...fresh, ...events.filter(e => !keys.has(e.key))].sort((a, b) => b.ts - a.ts).slice(0, 300)
    } catch { /* al prossimo stato */ }
  }

  if (tr) {
    tr.subscribe(s => {
      // Il primo stato (app-avvio-senza-salto-04-11): le colonne dello scheletro si riempiono, e una colonna salvata che non c'è
      // più esce con la coreografia della home.
      // (Se lo stato arriva già durante l'avvio, prima che la pagina sia pronta, entra e basta.)
      let entered = false
      if (!ready) try {
        if (wide) {
          const gone = cols.filter(n => !s.sessions.some(x => x.name === n))
          bootFade = true; setTimeout(() => (bootFade = false), 1000)
          smooth(() => { st = s; ready = true }, null, gone.length === 1 ? gone[0] : null)
          entered = true
        }
      } catch { /* sotto */ }
      if (!entered) { st = s; ready = true }
      down = false; clock = nowS()
      msgs = msgs.map(m => advance(m, s.sessions.find(x => x.name === m.session) ?? null, nowS()))
      record(s)
      clearsDue(s)
      refreshEvents()
    }, () => { down = true; if (rt) void noData() })
    setInterval(() => (clock = nowS()), 30_000)
    if (rt) setTimeout(() => void noData(), NO_DATA_MS)
  }
  // Mai arrivato uno stato: la demo al posto del vuoto, col suo avviso. Con uno stato già arrivato si resta lì, come oggi.
  async function noData() {
    if (ready || !rt) return
    const denied = await rt.denied()
    if (demoFallback({ hasState: ready, denied, elapsedMs: Date.now() - startedAt })) location.replace(demoLink(denied ? 'denied' : 'offline', new URL(location.href)))
  }
  const demoWhy = demo && loadRemote(store) ? demoReason(new URL(location.href)) : null
  const demoRetry = () => location.replace(realLink(new URL(location.href)))
  const demoPair = () => { forget(store); demoRetry() }
  if (demoWhy === 'offline') addEventListener('online', demoRetry)
  if (demo) document.body.classList.add('demo-on')
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
  // Franz 23/10 21:24: dopo Approva la voce sparisce subito; torna solo se il registro rifiuta (il rifiuto si mostra in basso).
  let okPending = $state(new Set<string>())
  const stShown = $derived(withoutApproved(st, okPending))
  $effect(() => { const s = settled(st, okPending); if (s.size) okPending = new Set([...okPending].filter(x => !s.has(x))) })
  async function approve(task: string, note: string, choice?: string) {
    okPending = new Set([...okPending, task])
    // Contratto 1.81: l'opzione scelta viaggia con l'ok.
    const a = st.approvals.find(x => x.task === task), also = a ? okAlso(a) : undefined
    const r = await run(newCmd('approve', null, task, approveText(note), undefined, choice || also ? { ...(choice ? { choice } : {}), ...(also ? { also } : {}) } : undefined))
    if (r?.ok) say(`✓ ${t.approved}`)
    else okPending = new Set([...okPending].filter(x => x !== task))
  }
  // «Superata» su una carta di scelta (app-scelte-chiudibili-06-11, Franz 06/11 15:06): la carta sparisce subito e l'op
  // `approval_close` (contratto 1.129) la chiude nel registro; ok anche se il compito non aspettava più. Se il relay rifiuta, la
  // carta torna (il motivo in basso, da `run`).
  async function closeApproval(task: string) {
    okPending = new Set([...okPending, task])
    const r = await run(newCmd('approval_close', null, task, t.approvalCloseReason))
    if (r?.ok) say(`✓ ${t.approvalClosed}`)
    else okPending = new Set([...okPending].filter(x => x !== task))
  }
  // Contratto 1.83: la risposta a un server MCP. La scheda sparisce subito e torna solo se il relay rifiuta (il motivo in basso).
  let elicitSent = $state(new Set<string>())
  async function elicit(e: Elicitation, action: 'accept' | 'decline', values: Values) {
    elicitSent = new Set([...elicitSent, e.id])
    const r = await run(newCmd('elicit', e.session, e.id, undefined, undefined, { action, ...(action === 'accept' ? { content: elicitContent(e, values) } : {}) }))
    if (r?.ok) say(`✓ ${t.elicitAnswered}`)
    else elicitSent = new Set([...elicitSent].filter(x => x !== e.id))
  }
  // Contratto 1.84 (scheda A): la risposta a un suggerimento della master; la voce sparisce subito e torna solo se il relay rifiuta.
  let sugDone = $state(new Set<string>())
  async function suggest(x: Suggestion, action: 'accept' | 'later' | 'never' | 'discuss' | 'choose', choice?: string) {
    // «Parliamone» (Franz 25/10 07:36; contratto 1.88): l'op suggestion con discuss, così il registro la vede; la voce resta
    // per Accetta. Se il relay non la prende, il prompt fisso di prima alla master.
    if (action === 'discuss') {
      pick(MASTER)
      const d = await run(newCmd('suggestion', null, x.id, undefined, undefined, { action }))
      if (!d?.ok) sendTo(MASTER, t.sugDiscussPrompt(x.text))
      return
    }
    sugDone = new Set([...sugDone, x.id])
    const r = await run(newCmd('suggestion', null, x.id, undefined, undefined, { action, ...(choice ? { choice } : {}) }))
    if (r?.ok) say(`✓ ${action === 'accept' || action === 'choose' ? t.sugSent : action === 'later' ? t.sugHidden : t.sugNeverDone}`)
    else sugDone = new Set([...sugDone].filter(id => id !== x.id))
  }
  const sug = $derived(visibleSuggestions(st, sugDone, now))
  // Contratto 1.84 (B4): prima cosa salva il PC di ognuna, poi la chiusura di quelle che si possono chiudere.
  async function closePreview(names: string[]): Promise<Preview | null> {
    const r = await run(newCmd('close_preview', null, names.join(',')))
    return r?.ok ? readPreview(r.text) : null
  }
  // Il relay chiude quelle che può e dice perché le altre restano (`<nome> not closed: <motivo>`): l'avviso conta solo le chiuse.
  async function closeMany(names: string[]) {
    const r = await run(newCmd('close', null, names.join(',')))
    if (!r?.ok) return
    const rf = refusals(r.text)
    const done = names.length - rf.length
    say([...(done > 0 ? [`✓ ${t.closedSaved(done)}`] : []), ...rf.map(x => `✗ ${t.closeRefusedSay(x.name, x.why)}`)].join(' · '))
  }
  /** Contratto 1.120: «Chiudi quando finisce» (after) o «Chiudi comunque» (force, già confermato) dopo un Chiudi rifiutato. */
  async function arm(name: string, when: 'after' | 'force') {
    const r = await run(newCmd('close', null, name, undefined, undefined, { when }))
    if (!r?.ok) return
    const rf = refusals(r.text)
    if (rf.length) { say(rf.map(x => `✗ ${t.closeRefusedSay(x.name, x.why)}`).join(' · ')); return }
    say(`✓ ${when === 'after' ? t.closeArmedAfter(name) : t.closeArmedForce(name)}`)
    // Quando finisce: la pagina resta com'è e la card lo dice (sessions[].closing); comunque: il pannello segue la chiusura.
    if (when === 'force') leaving = { name, sentAt: nowS(), delivered: true, force: true }
    else if (leaving?.name === name) leaving = null
  }
  /** Contratto 1.90: il tasto di un consiglio del Sistema (sys_kill o close con i nomi); il testo del relay dice cosa ha chiuso. */
  /** Contratto 1.95: l'elenco dei plugin (lettura passiva) e i comandi della sezione; il testo del relay dice com'è andata. */
  async function pluginsLoad(): Promise<PluginsPage | null> {
    const r = await run(newCmd('plugins', null))
    return r?.ok ? JSON.parse(r.text) : null
  }
  const pluginsRun = (op: 'plugin_check' | 'plugin_update' | 'plugin_update_all', arg: string | null) => run(newCmd(op, null, arg ?? undefined))
  async function sysAct(op: string, arg: string) {
    const r = await run(newCmd(op as CmdOp, null, arg))
    if (r?.ok) say(`✓ ${r.text}`)
  }

  /** Un comando al relay; il rifiuto si mostra in basso. */
  /** Contratto 1.80: «Fatto» su una ferma: la richiesta si chiude sul PC (kind e ref, since per una ferma del turno). */
  const attentionDone = (d: { kind: string; ref: string; since?: number }) => run(newCmd('attention_done', null, d.ref, undefined, undefined, { kind: d.kind, since: d.since })).then(r => !!r?.ok)
  /** app-chiudi-esito-04-11: i «not closed» del relay, col motivo in italiano. */
  const refusals = (text: string) => closeRefusals(text, t.closeBlocked, t.closeRefusedOther)
  async function run(c: Cmd): Promise<CmdResult | null> {
    if (!tr) { console.info('[cm] comando', c.session, c.op, c.arg, c.text); return null }
    try {
      const r = await tr.send(c)
      // Contratto 1.55: una risposta a una domanda già risolta al PC lo dice in chiaro.
      // Contratto 1.112: una lettura rimpiazzata da una più recente si chiude in silenzio.
      if (!r.ok && !superseded(r)) {
        const why = refusalText(r, c.question ?? null, st.sessions, t.answerAlreadyResolved)
        const rf = refusals(why)
        say(`✗ ${rf.length ? rf.map(x => t.closeRefusedSay(x.name, x.why)).join(' · ') : why}`)
      }
      return r
    } catch {
      say(`✗ ${t.noAnswer}`)
      return null
    }
  }

  // La conversazione delle sessioni a schermo: la prima volta le ultime 50 voci, poi solo quelle venute dopo.
  const loadingT = new Set<string>()
  /** Le chat che hanno già la pagina iniziale dall'avvio: solo loro leggono con `after=`. */
  const paged = new Set<string>()
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
    // Il relay tiene una lettura per sessione e la pagina «prima» veniva superata dalla lettura `after` che parte di continuo:
    // non arrivava mai (relay.log 03-04/11, nessun before=) e il tasto si spegneva. Superata, si richiede.
    let r: CmdResult | null = null
    for (let i = 0; i < 4; i++) {
      r = await run(transcriptCmd(name, `50:before=${first}`))
      if (!r || !superseded(r)) break
      await new Promise(res => setTimeout(res, 400 * (i + 1)))
    }
    if (r && superseded(r)) return
    // Franz 23/10 23:17: il tasto carica o sparisce (older): pagina vuota, già nota o rifiutata = niente tasto.
    const o = older(transcripts[name] ?? [], r?.ok ? (JSON.parse(r.text) as TranscriptPage) : null)
    transcripts = { ...transcripts, [name]: o.entries }
    olderOf = { ...olderOf, [name]: o.more }
  }
  // Un risultato di Cerca apre la conversazione nel punto trovato (app-cerca-pagina-03-11): la chat lo porta in vista e lo fa
  // lampeggiare. Contratto 1.127 (app-ricerca-al-punto-06-11, Franz 06/11 01:09: «mi apre semplicemente la master sull'ultimo
  // messaggio»): la pagina attorno alla voce, anche di prima di un /clear, al posto della chat; da lì si scorre in su con
  // «precedenti» e in giù con «successivi», e «Vai in fondo» torna all'ultimo. Finché ci sono voci più nuove la lettura
  // continua delle nuove si ferma, se no riempirebbe la chat da sola. Con un relay di prima: pagine più vecchie finché la
  // voce non c'è, al massimo 8 da 50.
  let focusOf = $state<Record<string, string>>({})
  let newerOf = $state<Record<string, boolean>>({})
  const AROUND_N = 50
  const jumping = new Set<string>()
  function focusOn(name: string, entry: string) {
    // Resta per 3 s: la chat aperta può essere rimontata e la nuova istanza deve trovarlo.
    focusOf = { ...focusOf, [name]: entry }
    setTimeout(() => { if (focusOf[name] === entry) { const { [name]: _, ...rest } = focusOf; focusOf = rest } }, 3000)
  }
  async function jumpTo(name: string, entry: string) {
    if (tr) {
      jumping.add(name)
      try {
        const r = await tr.send(transcriptCmd(name, `${AROUND_N}:around=${entry}`)).catch(() => null)
        const p: TranscriptPage | null = r?.ok ? JSON.parse(r.text) : null
        if (p && p.anchor != null) {
          transcripts = { ...transcripts, [name]: p.entries }
          olderOf = { ...olderOf, [name]: !!p.more }
          newerOf = { ...newerOf, [name]: !!p.more_after }
          paged.add(name)
          focusOn(name, entry)
          return
        }
      } finally { jumping.delete(name) }
    }
    for (let i = 0; i < 40 && !untrack(() => transcripts[name])?.length; i++) await new Promise(r => setTimeout(r, 250))
    for (let i = 0; i < 8; i++) {
      if (untrack(() => transcripts[name])?.some(e => e.id === entry)) { focusOn(name, entry); return }
      if (!untrack(() => olderOf[name])) return
      await loadOlder(name)
    }
  }
  /** «Successivi»: la pagina dopo l'ultima voce; più corta di una pagina intera vuol dire che si è arrivati in fondo. */
  async function loadNewer(name: string) {
    const last = untrack(() => transcripts[name])?.at(-1)?.id
    if (!tr || !last) return
    const r = await run(transcriptCmd(name, `${AROUND_N}:after=${last}`))
    if (!r?.ok) return
    const p: TranscriptPage = JSON.parse(r.text)
    transcripts = { ...transcripts, [name]: withAfterPage(untrack(() => transcripts[name]) ?? [], p) }
    if (p.entries.length < AROUND_N) newerOf = { ...newerOf, [name]: false }
  }
  /** «Vai in fondo»: si lascia il punto trovato e si torna alla pagina delle ultime voci, con la lettura continua. */
  function toLatest(name: string) {
    newerOf = { ...newerOf, [name]: false }
    transcripts = { ...transcripts, [name]: [] }
    paged.delete(name)
    void loadTranscript(name)
  }
  /** Vero quando la lettura è arrivata, anche senza voci nuove. */
  // Contratto 1.82: una conversazione chiusa aperta dalla Notte si legge per sid, in sola lettura.
  let sidOf = $state<Record<string, string>>({})
  // La conversazione chiusa aperta dalla Notte: un riquadro a tutto schermo sopra la pagina, in sola lettura, con ✕.
  let reading = $state<string | null>(null)
  const transcriptCmd = (name: string, arg: string) => {
    const sid = st.sessions.some(x => x.name === name && x.state !== 'gone') ? undefined : sidOf[name]
    return sid ? newCmd('transcript', null, arg, undefined, undefined, { sid }) : newCmd('transcript', name, arg)
  }
  async function loadTranscript(name: string): Promise<boolean> {
    if (!tr || loadingT.has(name) || jumping.has(name) || untrack(() => newerOf[name])) return false
    loadingT.add(name)
    try {
      const have = untrack(() => transcripts[name]) ?? []
      // Franz 28/10 11:02: dopo il riavvio il tail mette solo le ultime voci; la prima lettura chiede comunque la pagina.
      const after = paged.has(name) && have.length > 0
      const r = await tr.send(transcriptCmd(name, transcriptArg(have, paged.has(name))))
      if (!r.ok) {
        if (after && r.text.startsWith('no entry')) { transcripts = { ...transcripts, [name]: [] }; paged.delete(name) }
        return false
      }
      const p: TranscriptPage = JSON.parse(r.text)
      // Arrivata mentre la chat saltava al punto di un risultato: la pagina attorno vince.
      if (jumping.has(name) || untrack(() => newerOf[name])) return true
      if (!after || p.reset) { olderOf = { ...olderOf, [name]: !!p.more }; paged.add(name) }
      if (after && !p.entries.length && !p.reset) return true
      transcripts = { ...transcripts, [name]: after ? withAfterPage(have, p) : withFirstPage(have, p) }
      return true
    } catch { return false /* al prossimo stato */ } finally { loadingT.delete(name) }
  }

  // Da 840 px la plancia con le colonne, come il tablet; sotto, una colonna sola come il telefono.
  let width = $state(window.innerWidth)
  const wide = $derived(isWide(width))
  // `?s=nome` (notifiche e Telegram, Franz 21/10 19:04) o `#nome` apre quella sessione (link diretto, e i provini).
  const linked = askedSession(location.search, location.hash)
  // Contratto 1.130: `#/attesa/<id>` apre la scheda «Ti aspetta», dal link nel testo della master o di una sessione e da fuori.
  let waitId = $state<string | null>(askedWait(location.hash))
  $effect(() => {
    const onHash = () => { const w = askedWait(location.hash); if (w) { waitId = w; openPage('attesa') } }
    // Un link nel testo apre la scheda dentro l'app, senza ricaricare la pagina.
    const onClick = (e: MouseEvent) => {
      const a = (e.target as Element | null)?.closest?.('a[href]') as HTMLAnchorElement | null
      const w = a ? askedWait(new URL(a.href, location.href).hash) : null
      if (!w) return
      e.preventDefault(); waitId = w; openPage('attesa')
    }
    window.addEventListener('hashchange', onHash); document.addEventListener('click', onClick, true)
    return () => { window.removeEventListener('hashchange', onHash); document.removeEventListener('click', onClick, true) }
  })
  const known = (n: string) => (!!tr && !demo) || st.sessions.some(s => s.name === n && s.state !== 'gone')

  // La master vive nella home: ridotta è la barra in fondo, espansa occupa la home (mai una colonna o una pagina a parte).
  // Franz 24/10 09:41 (Studio, strada A): `/embed` è la sola chat della master, incorporata da studio.francescosorrentino.com.
  const embed = /^\/embed(\.html)?$/.test(location.pathname)
  // `/embed?dock=1` (studio-dock-coerente-24-10): la barra della master e il suo foglio, su fondo trasparente, dentro Studio.
  const dock = dockWanted(location.pathname, location.search)
  // Dentro Studio la barra parte dall'ultima vista (dockEmbed.ts), finché il relay non risponde; poi si aggiorna da sé.
  const dockCached = dock ? readDockCache<Session, TranscriptEntry>(load(DOCK_CACHE)) : null
  $effect(() => { const e = transcripts[MASTER]; if (dock && master && e) save(DOCK_CACHE, dockCacheOf(master, e)) })
  let masterOpen = $state(linked === MASTER || embed)
  // Il lato del pannello della master nella plancia, ricordato su questo dispositivo (Franz 30/10 13:26).
  let masterLeft = $state(load('cm.master_left') === '1')
  // Master sotto la dashboard, nella stessa colonna (Franz 03/11 16:52, forma B, app-master-sopra-dashboard-03-11): con le
  // sessioni aperte la chat della sessione tiene il resto. «Sposta» gira fra destra, sotto la dashboard e sinistra.
  let masterUnder = $state(load('cm.master_under') === '1')
  function moveMaster() {
    smooth(() => {
      masterLeft = !masterLeft
      save('cm.master_left', masterLeft ? '1' : '0'); save('cm.master_under', masterUnder ? '1' : '0')
    })
  }
  // Solo con le sessioni aperte nella vista larga, con la master aperta e fuori dalle colonne.
  const stacked = $derived(masterUnder && masterOpen && !!master && !cols.includes(MASTER))
  // Home sopra e master sotto non è un lato (Franz 03/11 17:30 e 17:31): la colonna con home e master prende il lato e la larghezza
  // che la master ha in quel momento; le sessioni occupano il resto.
  function placeMaster(p: 'right' | 'left') { smooth(() => { masterLeft = p === 'left'; save('cm.master_left', masterLeft ? '1' : '0') }) }
  // Due altezze fisse della master sotto la home (Franz 03/11 17:38 e 17:41, app-master-compatta-03-11): compatta (l'ultimo
  // messaggio, il ▶ e il campo) e metà colonna; intera c'è già la colonna sua. Il tocco sulla testata o sulla maniglia passa alla
  // successiva; la maniglia trascinata si ferma sullo scatto più vicino nel verso del gesto. Di base compatta (Franz 17:44).
  const STACK_H = ['compact', 'half'] as const
  let stackH = $state<(typeof STACK_H)[number]>((STACK_H as readonly string[]).includes(load('cm.master_stack_h') ?? '') ? (load('cm.master_stack_h') as (typeof STACK_H)[number]) : 'compact')
  function setStack(h: (typeof STACK_H)[number]) { corners(() => { stackH = h; save('cm.master_stack_h', h) }) }
  function stepStack(d: 1 | -1, wrap = true) {
    const i = STACK_H.indexOf(stackH) + d
    setStack(STACK_H[wrap ? (i + STACK_H.length) % STACK_H.length : Math.max(0, Math.min(STACK_H.length - 1, i))])
  }
  let stackHead = $state(false)
  function stackMaster() {
    smooth(() => { masterUnder = !masterUnder; save('cm.master_under', masterUnder ? '1' : '0') })
  }
  // Il tasto tondo Posizione accanto al ▶ della master (Franz 03/11 17:23, al posto del menu ⋮ della scelta C delle 17:05,
  // app-master-tasti-compatti-03-11): le posizioni, la misura e Chiudi al posto dei tre tasti tondi. «Sotto la dashboard» solo con le sessioni aperte: sul cruscotto la master sta di lato.
  const masterMenu = $derived([
    { label: t.masterPosRight, on: !masterLeft, icon: 'M3 5h18v14H3zM15 5v14', onMove: () => placeMaster('right') },
    { label: t.masterPosLeft, on: masterLeft, icon: 'M3 5h18v14H3zM9 5v14', onMove: () => placeMaster('left') },
    ...(deskDashboard ? [] : [{ label: t.masterPosUnder, on: masterUnder, icon: 'M3 5h18v14H3zM3 12h18', onMove: stackMaster }]),
    ...(masterOpen ? [{ label: masterHalf ? t.masterWiden : t.masterNarrowKey, icon: masterHalf ? 'M8 7l-5 5 5 5M16 7l5 5-5 5' : 'M4 7l5 5-5 5M20 7l-5 5 5 5', onMove: () => smooth(masterSizeKey) }] : []),
    ...(masterOpen ? [{ label: t.masterCloseKey, icon: 'M7 7l10 10M17 7L7 17', onMove: () => smooth(() => { masterOpen = false }) }] : []),
  ])
  const moveLabelOf = () => (masterLeft ? t.masterMoveRight : t.masterMoveLeft)
  // Sulla plancia la misura a metà fra barra e tutto schermo (Franz, 08/10 18:15); conta solo con `masterOpen`.
  let masterHalf = $state(false)
  // La misura del pannello, stretto o a metà schermo, ricordata come il lato (app-pwa-master-tasti-03-11).
  const masterNarrow = () => load('cm.master_wide') !== '1'
  function masterSize() { masterHalf = !masterHalf; save('cm.master_wide', masterHalf ? '0' : '1') }
  // I gesti della master nella plancia (Franz 03/11 22:06-22:08, app-master-trascina-03-11; masterLanding.ts): la testata
  // trascinata cambia il lato; il bordo verso le sessioni allarga e stringe (posti fissi a colonna e metà schermo); il bordo
  // superiore, o la riga in basso tirata su, cambia l'altezza a scatti. Anteprima a fantasma (scelta A di Franz, 04/11 17:21,
  // app-trascina-rifinitura-04-11): durante il gesto la pagina resta ferma, lo sfondo si scurisce appena e la sagoma gialla
  // occupa il posto d'arrivo con la misura vera (landingMorph.ts); al rilascio la master va lì, annullato il gesto niente cambia.
  const reducedMotion = () => typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches
  const masterGhost = ghostPreview(reducedMotion)
  // Il posto che la sagoma mostra, da applicare al rilascio.
  let aim: (() => void) | null = null
  function aimAt(r: Rect, text: string, go: () => void) { aim = go; masterGhost.show(r, text) }
  // Le misure di partenza della sagoma: l'area della pagina e il posto della master adesso.
  const rectOf = (el: Element | null): Rect | null => { const r = el?.getBoundingClientRect(); return r && r.width > 0 ? { x: r.left, y: r.top, w: r.width, h: r.height } : null }
  const areaRect = (): Rect => rectOf(document.querySelector('.deskwrap, .plancia')) ?? { x: 0, y: 0, w: window.innerWidth, h: window.innerHeight }
  const placeNow = (): Rect | null => PLACES.map(q => rectOf(document.querySelector(q))).find(Boolean) ?? null
  // La riga e la compatta si misurano quando ci sono; prima della prima volta valgono le misure di masterLanding.ts.
  const sizes = { bar: BAR_H, compact: COMPACT_H }
  function measureSizes() {
    const bar = rectOf(document.querySelector('.mbar')), compact = rectOf(document.querySelector('.dpanel.under.compact'))
    if (bar) sizes.bar = bar.h
    if (compact) sizes.compact = compact.h
  }
  // Il gesto continua sulla finestra: mentre la pagina si riorganizza l'elemento preso può sparire (la riga che diventa pannello).
  // Ogni gesto finisce in uno di quattro modi, e tutti passano da `onEnd`: rilascio, annullo del sistema, Esc, finestra che perde
  // il fuoco (app-trascina-rifinitura-04-11: dopo il rilascio restavano «Metà schermo» e la barra gialla).
  function follow(onMove: (e: PointerEvent) => void, onEnd: (e: PointerEvent | null) => void) {
    const up = (e: PointerEvent) => { stop(); onEnd(e) }
    const cancel = () => { stop(); onEnd(null) }
    const key = (e: KeyboardEvent) => { if (e.key === 'Escape') cancel() }
    const stop = () => {
      window.removeEventListener('pointermove', onMove); window.removeEventListener('pointerup', up); window.removeEventListener('pointercancel', cancel)
      window.removeEventListener('keydown', key); window.removeEventListener('blur', cancel)
    }
    window.addEventListener('pointermove', onMove); window.addEventListener('pointerup', up); window.addEventListener('pointercancel', cancel)
    window.addEventListener('keydown', key); window.addEventListener('blur', cancel)
  }
  // Il punto solo dove finisce un gesto: il fotogramma in attesa del bordo (che dopo il rilascio rimetteva l'anteprima), la sosta
  // in attesa, la selezione del testo.
  let dwellTimer: ReturnType<typeof setTimeout> | undefined
  function clearMarks() {
    if (wFrame) cancelAnimationFrame(wFrame)
    wFrame = 0; clearTimeout(dwellTimer); dwellTimer = undefined
    wDrag = null; document.body.style.userSelect = ''; taps.released(performance.now())
  }
  // Un solo riconoscitore per le maniglie della master (gesture.ts, app-master-gesti-senza-conflitti-04-11): un trascinamento
  // annulla i clic in sospeso; dopo il rilascio il click e il dblclick del browser sulla master non arrivano.
  const taps = masterTaps()
  for (const ev of ['click', 'dblclick'] as const) window.addEventListener(ev, (e) => {
    if (taps.swallow(performance.now()) && (e.target as Element | null)?.closest?.('.dpanel, .stackcol, .mbar, .mpanel')) { e.stopPropagation(); e.preventDefault() }
  }, true)
  function begin() { document.body.style.userSelect = 'none'; taps.dragged(performance.now()) }
  // Il rilascio: la master va al posto della sagoma, e la scelta si ricorda; la sagoma sparisce quando è arrivata. Arrivata si
  // ferma (app-altezza-senza-rimbalzo-06-11, Franz 06/11 10:59): lo schiacciamento d'atterraggio la allungava oltre la misura e
  // la riportava indietro, il rimbalzo dopo il ridimensionamento.
  function commit() {
    const go = aim
    aim = null; clearMarks()
    if (!go) { masterGhost.drop(); return }
    const vt = smooth(() => {
      go()
      save('cm.master_left', masterLeft ? '1' : '0'); save('cm.master_under', masterUnder ? '1' : '0'); save('cm.master_stack_h', stackH)
    })
    const arrived = vt ? vt.finished : tick().then(() => new Promise(r => setTimeout(r, MOVE_MS)))
    masterGhost.land(arrived)
  }
  function revert() { aim = null; masterGhost.drop(); clearMarks() }
  // La testata: il lato, dalla metà dello schermo sotto il dito; la sagoma è il posto di adesso, o lo stesso specchiato.
  let sideHold: ReturnType<typeof steady<Side>> | null = null
  let sideFrom: { area: Rect; r: Rect; side: Side } | null = null
  function placeDrag(phase: 'move' | 'end' | 'cancel', x: number, y = 0) {
    if (phase !== 'move') { sideHold = null; sideFrom = null; if (phase === 'end') commit(); else revert(); return }
    begin()
    const W = window.innerWidth
    sideFrom ??= { area: areaRect(), r: placeNow() ?? placeRect(areaRect(), masterLeft ? 'left' : 'right', 'column', colWidth()), side: masterLeft ? 'left' : 'right' }
    // Un margine di 48 px intorno al centro e la sosta: il lato non sfarfalla mentre il dito passa sul confine.
    sideHold ??= steady<Side>(sideFrom.side)
    const raw: Side = Math.abs(x - W / 2) < 48 ? sideHold.current : sideAt(W, x)
    const { on, wait } = sideHold.feed(raw, performance.now())
    clearTimeout(dwellTimer)
    if (wait != null) dwellTimer = setTimeout(() => placeDrag('move', x, y), wait + 5)
    const f = sideFrom
    aimAt(on === f.side ? f.r : mirrorRect(f.area, f.r), on === 'left' ? t.masterPosLeft : t.masterPosRight, () => { masterLeft = on === 'left' })
    // Le destinazioni (app-trascina-destinazioni-04-11): l'altro lato tratteggiato con la sua etichetta, e la linea a metà
    // schermo che si accende quando il dito la passa.
    const other: Side = on === 'left' ? 'right' : 'left'
    masterGhost.hint([{ b: other === f.side ? f.r : mirrorRect(f.area, f.r), text: other === 'left' ? t.masterPosLeft : t.masterPosRight }])
    const crossed = (x < W / 2) !== (f.side === 'left'), toward: Side = f.side === 'left' ? 'right' : 'left'
    masterGhost.midline(W / 2, crossed, y, crossed ? t.masterMidRelease(toward === 'left' ? t.masterMidLeft : t.masterMidRight) : t.masterMidCross(toward === 'left' ? t.masterMidLeft : t.masterMidRight, toward === 'left' ? '←' : '→'))
  }
  // Il cambio di lato continua il gesto (app-lato-animazione-06-11, Franz 06/11 00:40: l'atterraggio era «avulso dal contesto
  // del movimento appena fatto»): il pannello segue il dito; al rilascio cambia lato senza transizione di pagina, riparte da dove
  // l'ha lasciato il dito con la sua velocità e la plancia dal suo posto di prima, insieme, con la molla breve senza rimbalzo
  // (springFrames, come MasterFrame sul tablet); annullato torna indietro allo stesso modo. Col movimento ridotto, dissolvenza.
  const sidePanel = () => document.querySelector<HTMLElement>('aside.stackcol, aside.dpanel:not(.stackcol), aside.mpanel')
  const sideStage = () => document.querySelector<HTMLElement>('.deskwrap > .dstage, .plancia > .stage')
  function springX(el: HTMLElement, from: number, v: number) {
    const f = springFrames(from, v)
    el.animate(f.map(x => ({ transform: `translateX(${x}px)` })), { duration: (f.length / 60) * 1000, easing: 'linear' })
  }
  function sideEnd(dx: number, v: number, cancelled: boolean) {
    const go = cancelled ? null : aim
    sideHold = null; sideFrom = null; aim = null
    clearMarks(); masterGhost.drop()
    const p0 = sidePanel()?.getBoundingClientRect(), s0 = sideStage()?.getBoundingClientRect()
    const wasLeft = masterLeft
    if (go) { go(); save('cm.master_left', masterLeft ? '1' : '0') }
    void tick().then(() => {
      const p = sidePanel(), s = sideStage()
      if (!p) return
      p.style.transform = ''; p.style.zIndex = ''
      const changed = masterLeft !== wasLeft
      if (reducedMotion()) { if (changed) p.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 180 }); return }
      const p1 = p.getBoundingClientRect()
      springX(p, changed && p0 ? p0.left - p1.left : dx, v)
      const s1 = s?.getBoundingClientRect()
      if (changed && s && s0 && s1 && s0.left !== s1.left) springX(s, s0.left - s1.left, 0)
    })
  }
  // La maniglia della testata: il gesto continua sulla finestra, dopo 6 px.
  function sideGrip(e: PointerEvent) {
    const x0 = e.clientX, y0 = e.clientY
    let moved = false
    const speed = velocity()
    const panel = reducedMotion() ? null : sidePanel()
    follow((m) => {
      speed.add(m.clientX, m.timeStamp)
      if (!moved && !isDrag(m.clientX - x0, m.clientY - y0)) return
      moved = true
      if (panel) { panel.style.transform = `translateX(${m.clientX - x0}px)`; panel.style.zIndex = '60' }
      placeDrag('move', m.clientX, m.clientY)
    }, (m) => { if (!moved) return; sideEnd(m ? m.clientX - x0 : (panel ? Number(/-?[\d.]+/.exec(panel.style.transform)?.[0] ?? 0) : 0), speed.speed(), !m) })
  }
  // La larghezza lasciata col bordo, in px; null = quella del tasto Allarga/Stringi (420 o 720). Una misura libera di prima
  // (fino al 04/11) torna al posto fisso più vicino.
  const savedW = Number(load('cm.master_w')) || null
  let masterW = $state<number | null>(savedW == null ? null : widthAt(savedW, window.innerWidth))
  const colWidth = () => masterW ?? (masterHalf ? COLUMN : 720)
  let wDrag: number | null = null
  // La colonna all'inizio del gesto e la larghezza che la sagoma mostra; Esc e la perdita del fuoco annullano anche qui.
  let wCol: Rect | null = null, wAim = 0
  const wKey = (e: KeyboardEvent) => { if (e.key === 'Escape') widthCancel() }
  function widthListen(on: boolean) {
    if (on) { window.addEventListener('keydown', wKey); window.addEventListener('blur', widthCancel) }
    else { window.removeEventListener('keydown', wKey); window.removeEventListener('blur', widthCancel) }
  }
  // La corsa breve (app-master-corsa-breve-05-11): conta la direzione presa dal posto di partenza, non la metà della corsa.
  let wFrom = 0, wSpeed = velocity()
  function widthDown(e: PointerEvent) {
    e.preventDefault(); wDrag = e.clientX; wAim = colWidth(); begin(); widthListen(true)
    const st0 = widthStops(window.innerWidth)
    wFrom = st0.reduce((b, v, i) => (Math.abs(v - wAim) < Math.abs(st0[b] - wAim) ? i : b), 0)
    wSpeed = velocity(); wSpeed.add(e.clientX, e.timeStamp)
    const el = e.currentTarget as HTMLElement
    wCol = rectOf(el.parentElement); el.setPointerCapture(e.pointerId)
  }
  function widthCancel() {
    if (wDrag == null) return
    widthListen(false); revert()
  }
  // Posti fissi (Franz 04/11 10:29-10:30): la sagoma salta a un posto, mai nel mezzo. Quale lo dice la direzione: oltre 24 px il
  // posto dopo da quella parte, sotto quello di partenza (step in gesture.ts). Un aggiornamento per fotogramma.
  const widthOf = (x: number, v = 0) => {
    const st0 = widthStops(window.innerWidth), sgn = masterLeft ? 1 : -1
    return st0[step(st0, wFrom, (x - (wDrag ?? x)) * sgn, v * sgn)] ?? wAim
  }
  let wFrame = 0, wX = 0
  function aimWidth(w: number) {
    wAim = w
    const col = wCol ?? placeRect(areaRect(), masterLeft ? 'left' : 'right', 'column', colWidth())
    aimAt(widthRect(col, masterLeft ? 'left' : 'right', w), w === COLUMN ? t.masterLand.column : t.masterHalfScreen, () => {
      masterW = w; masterHalf = w < (COLUMN + window.innerWidth / 2) / 2
      save('cm.master_w', String(w)); save('cm.master_wide', masterHalf ? '0' : '1')
    })
  }
  function widthMove(e: PointerEvent) {
    if (wDrag == null) return
    wX = e.clientX; wSpeed.add(e.clientX, e.timeStamp)
    if (wFrame) return
    wFrame = requestAnimationFrame(() => { wFrame = 0; aimWidth(widthOf(wX)) })
  }
  function widthUp(e?: PointerEvent) {
    if (wDrag == null) return
    widthListen(false)
    // Al rilascio conta anche il colpo: la velocità decide la direzione e si proietta.
    if (e) { cancelAnimationFrame(wFrame); wFrame = 0; aimWidth(widthOf(e.clientX, wSpeed.speed())) }
    commit()
  }
  // L'altezza: le bande della colonna (masterLanding.ts), fissate all'inizio del gesto. Il lato non lo decide il dito
  // (app-master-riga-maniglie-04-11): `pick` è il lato della maniglia presa nella riga in basso, null = il lato di adesso; il
  // gesto è solo verticale.
  // La master di adesso (riga, pannello, master nella pila) e la molla in verticale: come springX del cambio di lato.
  const liftEl = () => PLACES.map(q => document.querySelector<HTMLElement>(q)).find(Boolean) ?? null
  function springY(el: HTMLElement, from: number, v: number) {
    const f = springFrames(from, v)
    el.animate(f.map(y => ({ transform: `translateY(${y}px)` })), { duration: (f.length / 60) * 1000, easing: 'linear' })
  }
  // Il rilascio di un cambio d'altezza: senza transizione di pagina né schiacciamento, la master d'arrivo parte da dove il dito ha
  // lasciato quella di prima, con la sua velocità, e va al suo posto con la molla breve senza rimbalzo.
  function heightEnd(el0: HTMLElement | null, vy: number, blob: ReturnType<typeof morphBlob> | null = null, vp = 0) {
    const go = aim
    aim = null; clearMarks(); masterGhost.drop()
    const top0 = el0?.getBoundingClientRect().top ?? null
    if (go) { go(); save('cm.master_left', masterLeft ? '1' : '0'); save('cm.master_under', masterUnder ? '1' : '0'); save('cm.master_stack_h', stackH) }
    void tick().then(() => {
      if (el0) { el0.style.transform = ''; el0.style.zIndex = '' }
      const el = liftEl()
      // Col blocco che cambia forma, la molla completa la forma sul posto vero; senza posto nuovo, torna indietro.
      if (blob) { if (el && go) blob.land(el, vp); else blob.back(vp); return }
      if (!el) return
      if (reducedMotion()) { if (go) el.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 180 }); return }
      if (top0 == null) return
      const from = top0 - el.getBoundingClientRect().top
      if (Math.abs(from) > 0.5) springY(el, from, vy)
    })
  }
  function heightGesture(y0: number, pick: Side | null) {
    let moved = false
    // Le zone si calcolano una volta, all'inizio del gesto (app-trascina-rifinitura-04-11): rifarle a ogni movimento faceva
    // sfarfallare il bersaglio dalla riga in basso. Anche le misure della sagoma.
    const W0 = window.innerWidth, H0 = window.innerHeight, under = !deskDashboard
    const w0 = barColW()
    const side0: Side = masterLeft ? 'left' : 'right', side = pick ?? side0
    const col: Rect = side === 'left' ? { x: 0, y: 0, w: w0, h: H0 } : { x: W0 - w0, y: 0, w: w0, h: H0 }
    const zs = heightZones(col, under)
    const h0: Height = !masterOpen ? 'bar' : !masterUnder ? 'column' : stackH === 'compact' ? 'compact' : 'half'
    measureSizes()
    const area = areaRect(), here = placeNow()
    const rectFor = (h: Height) => (side === side0 && h === h0 && here) || placeRect(area, side, h, w0, sizes)
    // La corsa breve (app-master-corsa-breve-05-11): le bande dall'alto in basso, e al rilascio conta la direzione presa dalla
    // banda di partenza; oltre 24 px, o con un colpo, la banda dopo da quella parte (step in gesture.ts).
    const keys = zs.map(z => z.key), mids = zs.map(z => z.rect.y + z.rect.h / 2)
    const from = Math.max(0, keys.indexOf(h0))
    const speed = velocity()
    speed.add(y0, performance.now())
    let aimR: Rect | null = null
    const aimH = (on: Height) => {
      aimR = rectFor(on)
      aimAt(aimR, t.masterLand[on], () => { if (!masterOpen && on !== 'bar') masterHalf = masterNarrow(); masterLeft = side === 'left'; setHeight(on) })
      // Gli altri posti possibili sul lato in cui si è, accennati (app-trascina-destinazioni-04-11).
      masterGhost.hint(zs.map(z => z.key).filter(k => k !== on).map(k => ({ b: rectFor(k), text: t.masterLand[k] })))
    }
    // Anche l'altezza continua il gesto (app-altezza-animazione-06-11): la master che si sposta segue il dito in verticale.
    // Il blocco cambia forma col dito (app-master-morph-m3-06-11, morphBlob): al posto della master che si alzava intera.
    const el0 = reducedMotion() ? null : liftEl()
    let blob: ReturnType<typeof morphBlob> | null = null
    let dy = 0
    const apply = (e: PointerEvent) => {
      speed.add(e.clientY, e.timeStamp)
      if (!moved && Math.abs(e.clientY - y0) <= DRAG_PX) return
      moved = true; begin()
      dy = e.clientY - y0
      aimH(keys[step(mids, from, e.clientY - y0)])
      if (el0 && aimR) { blob ??= morphBlob(el0); blob.move(aimR, dy, e.clientX, e.clientY) }
    }
    follow(apply, (e) => {
      clearTimeout(dwellTimer)
      if (!e) { revert(); blob?.back(0); return }
      if (!moved) {
        // Un tocco senza trascinare: la maniglia della riga apre la master da quel lato, all'ultima altezza; quella sotto la home
        // passa all'altezza dopo, come prima.
        if (pick) taps.tap(`bar-${pick}`, performance.now(), () => smooth(() => { masterLeft = pick === 'left'; masterOpen = true; masterHalf = masterNarrow(); save('cm.master_left', masterLeft ? '1' : '0') }))
        else if (stacked) taps.tap('stack', performance.now(), () => stepStack(1))
        return
      }
      aimH(keys[step(mids, from, e.clientY - y0, speed.speed())])
      const span = (aimR && blob ? aimR.y - blob.from.y : 0) || 1
      heightEnd(el0, speed.speed(), blob, speed.speed() / span)
    })
  }
  function heightDown(e: PointerEvent) { e.preventDefault(); heightGesture(e.clientY, null) }
  // La presa in alto e la maniglia della pila, come nell'app (app-cambio-lato-come-app-05-11, Franz 05/11 23:53): trascinate di
  // lato cambiano lato, in su o in giù l'altezza; il primo movimento oltre i 6 px decide. Un tocco resta quello di sempre.
  function topDown(e: PointerEvent) {
    e.preventDefault()
    const x0 = e.clientX, y0 = e.clientY
    const stop = () => { window.removeEventListener('pointermove', first); window.removeEventListener('pointerup', tap); window.removeEventListener('pointercancel', stop) }
    const tap = () => { stop(); heightGesture(y0, null); window.dispatchEvent(new PointerEvent('pointerup', { clientX: x0, clientY: y0 })) }
    const first = (m: PointerEvent) => {
      if (!isDrag(m.clientX - x0, m.clientY - y0)) return
      stop()
      if (Math.abs(m.clientX - x0) > Math.abs(m.clientY - y0)) sideGrip(e)
      else heightGesture(y0, null)
      // Il gesto scelto riparte da qui: il movimento che l'ha deciso conta già.
      window.dispatchEvent(new PointerEvent('pointermove', { clientX: m.clientX, clientY: m.clientY }))
    }
    window.addEventListener('pointermove', first); window.addEventListener('pointerup', tap); window.addEventListener('pointercancel', stop)
  }
  // La larghezza della colonna dove la master atterra: le maniglie della riga stanno sul suo asse, da un lato e dall'altro.
  // Chiusa, la master si apre stretta o larga come l'ultima volta (masterNarrow), e nella plancia senza la misura del bordo.
  const barColW = () => { const narrow = masterOpen ? masterHalf : masterNarrow(); return deskDashboard ? (narrow ? COLUMN : 720) : masterW ?? (narrow ? COLUMN : 720) }
  // La riga in basso tirata su dal testo (Franz 22:30 e 22:34): si apre alle altezze del bordo superiore, sul lato di adesso.
  let barGesture = false
  function barDrag(phase: 'move' | 'end' | 'cancel', _x: number, y: number) {
    if (phase !== 'move' || barGesture) return
    barGesture = true
    heightGesture(y, null)
    window.addEventListener('pointerup', () => { barGesture = false }, { once: true })
  }
  // Con la master sotto la home (app-home-sfuma-sopra-master-04-11, Franz 04/11 18:57): gli ultimi 32 px della home che scorre
  // sfumano verso il fondo della colonna, solo finché sotto c'è altro; in fondo l'ultima riga si legge intera. Una maschera sul
  // bordo, mai l'opacità delle righe.
  function fadeEnd(top: HTMLElement) {
    const check = () => {
      const s = top.querySelector<HTMLElement>('section.home')
      if (s) s.classList.toggle('fade-end', s.scrollTop + s.clientHeight < s.scrollHeight - 2)
    }
    top.addEventListener('scroll', check, true)
    const ro = new ResizeObserver(check); ro.observe(top)
    const mo = new MutationObserver(check); mo.observe(top, { childList: true, subtree: true })
    check()
    return { destroy() { top.removeEventListener('scroll', check, true); ro.disconnect(); mo.disconnect() } }
  }

  function setHeight(h: Height) {
    if (h === 'bar') { masterOpen = false; return }
    masterOpen = true
    masterUnder = h !== 'column'
    if (masterUnder) stackH = h === 'compact' ? 'compact' : 'half'
  }
  function masterSizeKey() { masterSize(); masterW = null; save('cm.master_w', '') }
  // Telefono: la sessione aperta, o la home.
  let open = $state<string | null>(linked != null && known(linked) && linked !== MASTER ? linked : null)
  // Chiusa una sessione, la sua pagina non sparisce (Franz, 08/10 18:06): resta com'era l'ultima volta, chiusa, sotto il
  // pannello di chiusura (`CloseSplash`), che dopo 3 s riporta alla home o toglie la colonna.
  let leaving = $state<Leaving | null>(null)
  let lastSeen = $state<Record<string, Session>>({})
  $effect(() => { const next = { ...untrack(() => lastSeen) }; for (const x of st.sessions) next[x.name] = x; lastSeen = next })
  const ghost = (n: string): Session => ghostSession(n, lastSeen)
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
  const showClosed = (x: Session, at: number, ms: number) => {
    justClosed = { s: { ...x, state: 'gone', question: null }, line: t.justClosedLine(new Date(at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })) }
    clearTimeout(justTimer)
    justTimer = setTimeout(() => smooth(() => { justClosed = null }), ms)
  }
  const splashHome = () => {
    const o = open
    const ph = o ? splashOf(o) : null
    const x = o ? (st.sessions.find(y => y.name === o) ?? lastSeen[o]) : null
    smooth(() => {
      if (ph?.kind === 'closed' && x) showClosed(x, ph.at, 2_500)
      if (leaving?.name === o) leaving = null
      open = null
    })
  }
  // Chiusa da una card della home, senza la sua pagina a schermo (Franz 24/10 13:20): la card dice «si sta chiudendo» e,
  // uscita la sessione, in cima resta la card «chiusa alle HH:MM» più a lungo, perché qui non c'è stato il pannello.
  $effect(() => {
    const l = leaving
    if (!l || open === l.name || (wide && cols.includes(l.name))) return
    const ph = splashOf(l.name)
    if (ph?.kind !== 'closed') return
    const x = st.sessions.find(y => y.name === l.name) ?? untrack(() => lastSeen[l.name])
    untrack(() => smooth(() => { if (x) showClosed(x, ph.at, 6_000); leaving = null }))
  })

  // Plancia: le colonne scelte (le prime tre senza una scelta salvata), le larghezze in dodicesimi, il lato della home.
  // Le colonne sono le sessioni della lista, senza la master: una master rimasta fra le scelte salvate si toglie da sé.
  const summary = $derived(build(st, sent, now, read))
  const live = $derived(summary.rows.map(r => r.session.name).filter(n => n !== MASTER))
  // Tocca a te dentro le sessioni (app-sessioni-compatte-06-11, Franz 06/11 18:28-20:25): le decisioni e le verifiche di una
  // sessione aperta stanno nella sua scheda; in Tocca a te resta solo quello che non è di una sessione aperta. Le pagine si leggono
  // quando cambiano i loro totali nello stato, non a ogni spinta.
  type ProvaItem = ProvePage['groups'][number]['prove'][number]
  let todoDec = $state<DecisionGroup[]>([]), todoProve = $state<ProvaItem[]>([])
  const liveSet = $derived(new Set(live))
  const sessOfTask = (task: string) => st.commitments?.find(c => c.task === task)?.session ?? null
  const provaSession = (p: ProvaItem) => p.session ?? sessOfTask(p.task)
  const todo = $derived.by(() => {
    const out: Record<string, { d: number; v: number }> = {}
    for (const g of todoDec) for (const i of g.items) if (i.session && liveSet.has(i.session)) (out[i.session] ??= { d: 0, v: 0 }).d++
    for (const p of todoProve) { const s = provaSession(p); if (s && liveSet.has(s)) (out[s] ??= { d: 0, v: 0 }).v++ }
    return out
  })
  const decOfSession = (g: DecisionGroup) => g.items.some(i => !!i.session && liveSet.has(i.session))
  // La testata di «Dalla master»: quello che Franz ha da fare fuori dalle sessioni aperte; i suggerimenti sono contesto.
  const fromMaster = $derived({ todo: todoDec.filter(g => !decOfSession(g)).length + todoProve.filter(p => { const s = provaSession(p); return !(s && liveSet.has(s)) }).length + (st.prima_raccolta ? 1 : 0), sug: sug.shown.length })
  $effect(() => {
    void st.decisions?.n; void st.prove?.n
    if (!tr) return
    untrack(() => {
      void run(newCmd('decisioni', null)).then(r => { const { page } = readPage(r, decodeDecisions, null); if (page) todoDec = page.groups })
      void run(newCmd('prove', null)).then(r => { const { page } = readPage(r, decodeProve, null); if (page) todoProve = page.groups.flatMap(g => g.prove) })
    })
  })
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
  const dropCol = (n: string) => smooth(() => { heldCols = heldCols.filter(x => x !== n); if (leaving?.name === n) leaving = null }, null, n)
  let pinned = $state<string[] | null>(columnsFromPref(load('cm.columns')))
  if (linked != null && known(linked) && linked !== MASTER) pinned = add(pinned ?? columns(null, live), linked)
  // Prima della risposta del relay le colonne sono quelle salvate, con lo scheletro al loro posto (app-avvio-senza-salto-04-11):
  // mai la plancia su quattro colonne per prima.
  const seenCols = columnsFromPref(load('cm.columns_seen'))
  const cols = $derived(ready ? columns(pinned, [...live, ...heldCols]) : (pinned ?? seenCols ?? []).filter(n => n !== MASTER))
  // Le colonne che si vedevano, per il prossimo avvio anche senza una scelta salvata.
  $effect(() => { if (ready) save('cm.columns_seen', columnsPref(cols)) })
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
  const headRow = (n: string) => rowOf[n] ?? (heldCols.includes(n) && rowSeen[n] ? { ...rowSeen[n], session: lastSeen[n] ? ghost(n) : rowSeen[n].session } : null)

  // Il cambio con la transizione del browser, quando c'è (Chrome): niente ridisegni continui.
  // Con la pagina nascosta niente transizione: il browser la interrompe e la promessa rifiutata finiva in console (15/10).
  // `morph`: aprendo o chiudendo una pagina, la voce premuta diventa la pagina e ci torna (morphStart in motion.ts).
  let reflowN = 0
  // `focus`: la sessione che entra o esce dalle colonne; la sua scheda e la sua colonna sono la stessa cosa nel cambio.
  const smooth = (go: () => void, morph: 'open' | 'back' | null = null, focus: string | null = null): ViewTransition | null => {
    if (!document.startViewTransition || document.hidden) { go(); return null }
    const m = morph ? morphStart(morph) : null
    // Nella vista larga la home si riposiziona scheda per scheda, una sola coreografia nei due versi
    // (app-home-riposiziona-animata-04-11, `html.reflow` in v3.css).
    const reflow = wide && !m
    const delays = reflow ? waveDelays() : null
    if (reflow) { reflowN++; document.documentElement.classList.add('reflow'); oneName(focus) }
    const vt = document.startViewTransition(m ? async () => { m.before(); go(); await tick(); m.after() } : reflow ? async () => { const was = stacked; go(); await tick(); oneName(focus); order(was, stacked) } : go)
    for (const p of [vt.ready, vt.finished, vt.updateCallbackDone]) p.catch(() => {})
    if (delays?.size) vt.ready.then(() => waveApply(delays), () => {})
    if (m) vt.finished.catch(() => {}).finally(m.done)
    if (reflow) vt.finished.catch(() => {}).finally(() => {
      if (--reflowN > 0) return
      document.documentElement.classList.remove('reflow', 'reflow-master-first', 'reflow-home-first')
      for (const el of document.querySelectorAll<HTMLElement>('[data-vt], .desk .col[data-col]')) { el.style.removeProperty('view-transition-name'); el.style.removeProperty('view-transition-class') }
    })
    return vt
  }
  // I nomi del cambio, prima e dopo: un nome doppio annulla tutta la transizione, quindi il secondo resta senza. La sessione
  // `focus` è una cosa sola: la sua colonna se c'è, se no la sua scheda, così aprendo la scheda si espande nella colonna e
  // chiudendo la colonna rientra nella scheda, mentre le altre scivolano al loro posto.
  // Chi si muove prima (Franz 04/11 dal telefono: «durante la riduzione della master da una colonna a metà colonna alcuni elementi si
  // sovrappongono»): quando la master scende sotto la home si ritira prima lei e poi entrano le schede; quando risale escono prima
  // le schede e poi lei cresce. Così le une non passano mai sopra l'altra.
  // A onda (app-effetti-movimenti-04-11, punto 6): ogni scheda parte un poco dopo quella sopra e a sinistra, 18 ms a passo e al
  // massimo 160, così il riposizionamento corre come un'onda dall'angolo in alto a sinistra. I ritardi stanno in uno stile della
  // transizione, tolto alla fine; la scheda che diventa colonna parte subito.
  // L'onda senza toccare i fogli di stile (app-fluidita-06-11): prima si leggeva lo stile calcolato di ogni scheda e si scriveva
  // uno <style> con un ritardo per gruppo, poi lo si toglieva; ogni cambio di foglio rifaceva lo stile di tutta la pagina (fino a
  // 1 s a CPU 4x). Ora l'ordine viene dai nomi (data-vt) e dalle posizioni, e i ritardi vanno alle animazioni della transizione.
  function waveDelays(): Map<string, number> {
    const m = new Map<string, number>()
    if (reducedMotion()) return m
    const els = [...document.querySelectorAll<HTMLElement>('[data-vt]')].map(el => ({ n: el.dataset.vt!, r: el.getBoundingClientRect() }))
      .filter(x => x.n && !x.n.startsWith('col-') && x.r.width > 0 && x.r.bottom > 0 && x.r.top < window.innerHeight)
      .sort((a, b) => (a.r.top + a.r.left / 3) - (b.r.top + b.r.left / 3))
    els.forEach((x, i) => { if (!m.has(x.n)) m.set(x.n, Math.min(160, i * 18)) })
    return m
  }
  function waveApply(delays: Map<string, number>) {
    for (const a of document.documentElement.getAnimations({ subtree: true })) {
      const pe = (a.effect as KeyframeEffect | null)?.pseudoElement ?? ''
      const n = /^::view-transition-group\((.+)\)$/.exec(pe)?.[1]
      const d = n == null ? undefined : delays.get(n) ?? delays.get(n.replace(/\\(.)/g, '$1'))
      if (d) a.effect?.updateTiming({ delay: d })
    }
  }
  // Compatta ed estesa (app-effetti-movimenti-04-11, punto 8): mentre la master cambia misura gli angoli si piegano verso
  // l'interno e tornano con la molla (`html.fx-corners` in v3.css).
  function corners(go: () => void) {
    if (!reducedMotion()) { document.documentElement.classList.add('fx-corners'); setTimeout(() => document.documentElement.classList.remove('fx-corners'), 900) }
    smooth(go)
  }
  function order(was: boolean, now: boolean) {
    const c = document.documentElement.classList
    c.remove('reflow-master-first', 'reflow-home-first')
    if (!was && now) c.add('reflow-master-first')
    else if (was && !now) c.add('reflow-home-first')
  }
  function oneName(focus: string | null) {
    const seen = new Set<string>()
    for (const el of document.querySelectorAll<HTMLElement>('[data-vt]')) {
      const n = el.dataset.vt!
      if (seen.has(n)) el.style.setProperty('view-transition-name', 'none'); else { seen.add(n); el.style.removeProperty('view-transition-name') }
    }
    if (document.documentElement.matches('.fx-open, .fx-close')) for (const el of document.querySelectorAll<HTMLElement>('.desk .col[data-col]')) {
      if (el.dataset.col === focus) continue
      el.style.setProperty('view-transition-name', `col-${el.dataset.col!.replace(/[^A-Za-z0-9_-]/g, '_')}`)
      el.style.setProperty('view-transition-class', 'home-reflow fx-col')
    }
    if (!focus) return
    const col = document.querySelector<HTMLElement>(`.desk .col[data-col="${CSS.escape(focus)}"]`)
    const el = col ?? document.querySelector<HTMLElement>(`[data-sess="${CSS.escape(focus)}"][data-vt]`)
    if (!el) return
    el.style.setProperty('view-transition-name', `col-${focus.replace(/[^A-Za-z0-9_-]/g, '_')}`)
    el.style.setProperty('view-transition-class', 'home-reflow fx-focus')
  }
  // La sessione che entra o esce, se è una sola.
  const changed = (a: string[], b: string[]) => { const d = [...a.filter(x => !b.includes(x)), ...b.filter(x => !a.includes(x))]; return d.length === 1 ? d[0] : null }
  // La scheda che diventa colonna (app-effetti-movimenti-04-11, punto 1, cardFx.ts): `from` è la scheda toccata, che prima si
  // alza col collo; senza (il +, una ricerca, un avviso) solo lo split. Chiudendo, l'inverso più svelto e la scheda che riassorbe.
  async function setCols(c: string[], focus: string | null = changed(cols, c), from: HTMLElement | null = null) {
    const opening = focus != null && c.includes(focus) && !cols.includes(focus)
    const closing = focus != null && cols.includes(focus) && !c.includes(focus)
    const fx = wide && (opening || closing) && !reducedMotion() && !document.hidden
    if (!fx) { smooth(() => { pinned = c; save('cm.columns', columnsPref(c)) }, null, focus); return }
    // Il regista solo da plancia a plancia (seconda colonna, chiusura di una fra più): lì il volo è identico a quello della
    // transizione. Fra cruscotto e plancia, nei due versi, resta la View Transition: il cruscotto vecchio sfuma mentre le schede
    // attraversano lo schermo, e il regista non lo rifà (fotogrammi del 06/11, scelta B della master).
    if (!cols.length || !c.length) { await viaTransition(c, focus, opening, closing, from); return }
    if (opening) preChat = focus
    let done = () => {}
    if (opening && from) {
      // La colonna vicina al posto nuovo si tende verso di lui.
      const at = c.indexOf(focus!), near = c[at - 1] ?? c[at + 1]
      const el = near ? document.querySelector<HTMLElement>(`.desk .col[data-col="${CSS.escape(near)}"]`) : null
      done = await lift(from, el, c[at - 1] ? 1 : -1)
    }
    // La chat della colonna che arriva è già montata (preChat), e il volo non si ferma per lei (app-volo-web-taratura-05-11).
    // Il regista (lib/director.ts, app-fluidita-06-11) al posto della View Transition: stessi effetti, solo transform e opacity.
    await fly({ focus: focus!, opening, card: () => (opening ? from : null) ?? cardOf(focus!), go: async () => { retractNeck(); done(); pinned = c; save('cm.columns', columnsPref(c)); await tick() } })
    preChat = null
    if (closing) { const el = cardOf(focus!); if (el) settleCard(el) }
  }
  // Apertura e chiusura con la View Transition (fino a 44720875 per tutte): il collo, poi la transizione coi tempi di
  // `html.fx-open`/`fx-close` e il volo solo con transform (flatten).
  async function viaTransition(c: string[], focus: string | null, opening: boolean, closing: boolean, from: HTMLElement | null) {
    if (!document.startViewTransition) { pinned = c; save('cm.columns', columnsPref(c)); return }
    if (opening) preChat = focus
    let done = () => {}
    if (opening && from) {
      const at = c.indexOf(focus!), near = c[at - 1] ?? c[at + 1]
      const el = near ? document.querySelector<HTMLElement>(`.desk .col[data-col="${CSS.escape(near)}"]`) : null
      done = await lift(from, el, c[at - 1] ? 1 : -1)
    }
    document.documentElement.classList.add(opening ? 'fx-open' : 'fx-close')
    const vt = smooth(() => { done(); pinned = c; save('cm.columns', columnsPref(c)) }, null, focus)
    let over = false
    const end = () => {
      if (over) return
      over = true
      // Le colonne ferme durante il volo non ripartono con la loro dissolvenza d'ingresso togliendo fx-open (data-shown, Desk.svelte).
      for (const el of document.querySelectorAll<HTMLElement>('.desk .col[data-col]')) el.dataset.shown = ''
      document.documentElement.classList.remove('fx-open', 'fx-close')
      preChat = null
      if (closing) { const el = cardOf(focus!); if (el) settleCard(el) }
    }
    void (vt?.finished ?? Promise.resolve()).catch(() => {}).finally(end)
    // Contati da quando il volo parte: su una macchina lenta la fotografia della pagina dura a lungo.
    void vt?.ready.then(flatten, () => {})
    void (vt?.ready ?? Promise.resolve()).catch(() => {}).finally(() => setTimeout(end, 1200))
  }
  // La scheda di una sessione nella home, se si vede.
  function cardOf(name: string): HTMLElement | null {
    const el = document.querySelector<HTMLElement>(`[data-sess="${CSS.escape(name)}"][data-vt]`)
    const r = el?.getBoundingClientRect()
    return r && r.width > 0 && r.bottom > 0 && r.top < window.innerHeight ? el : null
  }
  // Dal trascinamento niente View Transition: sfumerebbe due fotografie della plancia invece di far scorrere la colonna lasciata
  // fino al suo posto (Franz 03/11 22:50: «al rilascio c'è uno scatto»); il movimento lo fa la transizione di `left` in Desk.
  function dropCols(c: string[]) { pinned = c; save('cm.columns', columnsPref(c)) }
  function setShares(s: number[]) { shares = s; save('cm.shares', sharesPref(s)) }
  // Aprire una sessione (da «Per te», da una ricerca, dal controller della lettura): la master si espande nella home,
  // le altre entrano in colonna sulla plancia o diventano la pagina sul telefono.
  function pick(name: string) {
    // Aprire una sessione la toglie da «Hanno finito», come nell'app.
    const o = st.sessions.find(x => x.name === name)?.outcome
    if (o && !read.has(finishedKey(name, o.at))) read = new Set([...read, finishedKey(name, o.at)])
    if (name === MASTER) smooth(() => { masterOpen = true; open = null })
    else if (wide) setCols(add(cols, name), name, cardOf(name))
    else smooth(() => { open = name })
  }
  // Le pagine del menu: sul telefono al posto della home, sulla plancia al posto delle colonne.
  // `?page=diary` apre una pagina (link diretto, e i provini).
  const asked = new URLSearchParams(location.search).get('page')
  // «diary» e «recap» nei link vecchi aprono la pagina unica Lavori (22/10).
  const askedPage = asked === 'diary' || asked === 'recap' ? 'lavori' : asked
  // Da guardare (contratto 1.106): la pagina di tutti i risultati o di una sessione, e il risultato da aprire.
  let watchSession = $state<string | null>(null), watchOpenId = $state<string | null>(null)
  let page = $state<PageName | null>(askedWait(location.hash) ? 'attesa' : askedPage && ['launch', 'lavori', 'night', 'search', 'settings', 'homeorder', 'queue', 'box-aspetta', 'box-lavoro', 'box-esiti'].includes(askedPage) ? (askedPage as PageName) : null)
  // La pagina Notte (specifica del 07/10, approvata alle 21:50): il rapporto si chiede al PC con l'op `night` (contratto 1.44)
  // quando la pagina si apre e col tasto aggiorna; nella demo è la fixture inventata.
  let night = $state<{ report: NightReport | null; error: string | null; loading: boolean }>({ report: null, error: null, loading: false })
  const nightZone = Intl.DateTimeFormat().resolvedOptions().timeZone
  // Fra le cose da fare solo quello ancora vero adesso (Franz, 08/10 14:20: /clear chiesto a una sessione chiusa).
  const nightModel = $derived(night.report ? nightPage(night.report, nightZone, { live: new Set(st.sessions.filter(s => s.state !== 'gone').map(s => s.name)), pending: new Set((st.approvals ?? []).map(a => a.task)), asking: new Set(st.sessions.filter(s => s.question).map(s => s.name)) }) : null)
  const nightTitle = $derived.by(() => {
    if (!nightModel) return t.menuNight
    const p = nightTitleParts(nightModel.dayBefore, nightModel.day, d => new Intl.DateTimeFormat(locale, { month: 'long', timeZone: 'UTC' }).format(d))
    return p.monthA ? t.nightPageTitleTwo(p.dayA, p.monthA, p.dayB, p.monthB) : t.nightPageTitle(p.dayA, p.dayB, p.monthB)
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
  $effect(() => { if (page === 'lavori' && canAgenda) untrack(() => loadAgenda()) })
  // Contratto 1.77: il blocco Fatto dall'op `done`, una pagina per giorno. Oggi appena il relay offre l'op (la riga di home conta
  // «fatto oggi» da lì, come il blocco) e di nuovo a ogni cambio di pagina; un giorno prima al tocco.
  let donePages = $state<Record<string, DonePage>>({})
  const canDone = $derived(!!st.ops?.includes('done'))
  async function loadDone(date: string | null) {
    if (!tr) return
    try { const r = await tr.send(newCmd('done', null, date)); if (r.ok) { const pg = JSON.parse(r.text) as DonePage; donePages = { ...donePages, [pg.date]: pg } } } catch { /* resta quello che c'è */ }
  }
  $effect(() => { if (canDone) { page; untrack(() => loadDone(null)) } })
  // Le Azioni sulle schede (piano del 09/10). «Parlane con la master»: la scheda citata nel campo della master, sotto quello
  // che c'era già, e la sua conversazione aperta. Le scritture (Fatto, Rimanda, Rimuovi, Passa) con l'op del contratto 1.47.
  const canWrite = $derived(!!st.ops?.includes('agenda_set'))
  // Il cruscotto della plancia (variante B): senza colonne né pagine, Recap e Utilizzo stanno a destra e non nella home.
  const deskDashboard = $derived(wide && !cols.length && !page)
  // Il cruscotto a tre colonne (21/10) ha sempre il riquadro della notte: il rapporto si chiede appena c'è.
  $effect(() => { if (deskDashboard && st.night?.report && !night.report && !night.loading && !night.error) untrack(() => loadNight()) })
  // Cruscotto e plancia montati una volta (app-fluidita-montaggio-06-11, lib/park.ts): all'avvio solo quello che si vede; l'altro
  // la prima volta che la pagina è ferma, così il primo cambio non lo rifà.
  let keepBoth = $state(false)
  $effect(() => {
    if (!wide || !ready || keepBoth) return
    if (!window.requestIdleCallback) { const id = setTimeout(() => (keepBoth = true), 2000); return () => clearTimeout(id) }
    const id = requestIdleCallback(() => (keepBoth = true), { timeout: 5000 })
    return () => cancelIdleCallback(id)
  })
  // La chat della master, una sola: sta nel pannello di quello che si vede (cruscotto o plancia), fuori dal documento se lì il
  // pannello non c'è (la master in una colonna ha la sua).
  const mchatHosts: Record<'dash' | 'desk', HTMLElement | null> = $state({ dash: null, desk: null })
  let mchatNode = $state<HTMLElement | null>(null)
  const chatHost = (where: 'dash' | 'desk'): Attachment<HTMLElement> => (el) => {
    mchatHosts[where] = el
    return () => { if (mchatHosts[where] === el) mchatHosts[where] = null }
  }
  const chatNode: Attachment<HTMLElement> = (el) => { mchatNode = el; return () => { mchatNode = null; el.remove() } }
  $effect(() => { const n = mchatNode, h = mchatHosts[deskDashboard ? 'dash' : 'desk']; if (n) moveInto(n, h ?? offstage) })
  // Le chat delle colonne, una per sessione, montate fuori dalla colonna e spostate nel suo posto (app-cambio-sessione-fermo-06-11,
  // Franz 06/11 11:32-11:38: «l'animazione rimane ferma per un secondo», «il reload dopo l'animazione»): la colonna in arrivo volava
  // con lo scheletro e la chat si montava a volo finito, 100-135 ms di fermo a CPU 4x e il riquadro che si riempiva come ricaricato.
  // Ora la chat di quella che arriva si monta al tocco, fuori dal documento, mentre la scheda si alza, e vola già piena. La master
  // in colonna resta com'era.
  let preChat = $state<string | null>(null)
  const colChats = $derived(wide ? [...new Set([...cols, preChat])].filter((n): n is string => !!n && n !== MASTER) : [])
  const colHostEl = new Map<string, HTMLElement>(), colChatEl = new Map<string, HTMLElement>()
  const colHost = (name: string): Attachment<HTMLElement> => (el) => {
    colHostEl.set(name, el)
    const c = colChatEl.get(name)
    if (c) moveInto(c, el)
    return () => {
      if (colHostEl.get(name) === el) colHostEl.delete(name)
      const n = colChatEl.get(name)
      if (n && n.parentElement === el) offstage.append(n)
    }
  }
  const colChat = (name: string): Attachment<HTMLElement> => (el) => {
    colChatEl.set(name, el)
    moveInto(el, colHostEl.get(name) ?? offstage)
    return () => { if (colChatEl.get(name) === el) colChatEl.delete(name); el.remove() }
  }
  let masterInject = $state<{ text: string; n: number } | null>(null)
  // «Fatto» su una pausa che aspetta te (pagina Lavori, 22/10): la master riprende il compito; nessuna op nuova.
  function doneForPerson(c: import('./lib/contract').Commitment) { sendTo(MASTER, t.lavoriDonePrompt(c.paused?.reason ?? '', c.task)) }
  function talk(text: string) {
    masterInject = { text, n: (masterInject?.n ?? 0) + 1 }
    smooth(() => { page = null; masterOpen = true })
  }
  // Contratto 1.47: la scrittura va al relay con `agenda_set`; riuscita, l'agenda si ricarica; «agenda row changed» ricarica
  // e lo dice. Nella demo cambia la riga in memoria, come farebbe il relay.
  // Contratto 1.97: l'ordine delle sezioni della home, unico sul relay; vuoto = il predefinito.
  async function saveHomeOrder(arg: string | null, back = true) {
    if (!tr) return
    try {
      const r = await tr.send(newCmd('home_order', null, arg ?? ''))
      if (!r.ok) { say(r.text); return }
      if (back) openPage('settings')
    } catch { say(t.noAnswer) }
  }
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
  // Contratto 1.100: all'abbinamento la prima raccolta si apre da sola, una volta per browser; mai nella demo.
  $effect(() => {
    const s = st.prima_raccolta
    if (demo || !s) return
    let seen = true
    try { seen = localStorage.getItem('cm.primaSeen') === '1' } catch { /* senza memoria non si riapre a ogni giro */ }
    if (primaAutoOpen(s, seen)) { try { localStorage.setItem('cm.primaSeen', '1') } catch { /* come sopra */ } ; untrack(() => openPage('prima-raccolta')) }
  })
  const pageTitle: Record<PageName, string> = $derived({ attesa: t.tiAspettaTitle, seguiti: t.seguitiTitle, 'prima-raccolta': t.primaTitle, prove: t.proveTitle, decisions: t.decisionsTitle, features: t.featuresTitle, usage: t.menuQuadro, homeorder: t.reorderTitle, lavori: t.lavoriTitle, launch: t.menuLaunch, night: nightTitle, overview: t.menuQuadro, search: t.search, settings: t.settingsTitle, queue: t.queueTitle, 'box-aspetta': t.boxWaitingTitle, 'box-lavoro': t.boxBusyTitle, 'box-esiti': t.boxOutcomesTitle, guardare: watchSession ? `${t.watchResults} · ${watchSession}` : t.watchPageTitle })
  // «Utilizzo e limiti» (Utilizzo unito, approvato da Franz il 09/10 alle 16:17): niente più pagina. Il menu, la riga della
  // quota e il Registro portano alla sezione Utilizzo della home, che si apre e si illumina; sulla plancia senza colonne è
  // già a destra, nel cruscotto.
  let usageFocus = $state(0)
  // La pagina «Utilizzo e limiti» (scelta D, 30/10 20:11): nella home resta il blocco compatto.
  function goUsage() { smooth(() => { page = 'usage'; open = null; masterOpen = false }, 'open') }
  // Feedback dal menù (app-feedback-menu-04-11, scelta A): un foglio sopra la pagina dove si è, che resta sotto e si può fotografare.
  // L'invio (contratto 1.121, `feedback_app`, solo se il relay ha l'op): il primo passo manda tipo, testo, dati tecnici e la
  // schermata (su /share, solo se accesa) e riceve la bozza col suo hash; il secondo manda la scelta. La bozza vale una volta.
  let fbDlg = $state<HTMLDialogElement | null>(null)
  let fbOn = $state(false)
  let fbScreen = $state('')
  function openFeedback() {
    fbScreen = page ? pageTitle[page] : open ? t.feedbackSession(open) : t.feedbackHome
    fbOn = true
    void tick().then(() => fbDlg?.showModal())
  }
  const fbTech = $derived({ version: __APP_VERSION__, commit: __APP_COMMIT__, device: deviceOf(navigator.userAgent, window.innerWidth, window.innerHeight), screen: t.feedbackScreen(fbScreen), lang: t.feedbackLang(locale) })
  // Il primo passo parte a «Prepara la bozza» (contratto 1.123): così la riga del PC sulla schermata e il tasto GitHub, solo per
  // chi legge il repo privato dei feedback, ci sono prima dei tasti. La bozza vale una volta.
  let fbDraft: string | null = null
  async function fbPrepare(d: Draft, photo: string | null): Promise<{ notice: string | null; github: boolean } | string> {
    if (!tr) return t.noAnswer
    fbDraft = null
    let share: string | undefined
    const b = photo ? bytesOf(photo) : null
    if (b) { share = crypto.randomUUID(); await tr.share(share, b.mime, b.data, b.mime === 'image/jpeg' ? 'feedback.jpg' : 'feedback.png') }
    const r = await run(newCmd('feedback_app', null, null, d.text.slice(0, 4000), undefined, { kind: relayKind(d.kind), tech: relayTech(fbTech), share }))
    const dr = r?.ok ? parseDraft(r.text) : null
    if (!dr) return r?.text || t.noAnswer
    fbDraft = dr.hash
    return { notice: dr.screenshot ? dr.screenshot_notice ?? null : null, github: dr.github_ok === true }
  }
  async function fbSend(as: 'github' | 'anon' | 'later'): Promise<{ url: string | null; text: string } | string> {
    if (!tr || !fbDraft) return t.noAnswer
    const r = await run(newCmd('feedback_app', null, null, undefined, undefined, { hash: fbDraft, choice: as === 'anon' ? 'anonymous' : as }))
    const c = r?.ok ? parseChoice(r.text) : null
    if (!c) return r?.text || t.noAnswer
    fbDraft = null
    return { url: c.url, text: c.text }
  }
  const fbGrab = () => grabTab(async () => { fbDlg?.close(); await new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r))) }, () => { if (fbOn) fbDlg?.showModal() })
  const openPage = (p: PageName | null) => { if (p === 'overview') goUsage(); else smooth(() => { page = p; if (p !== 'queue') queueSkip = null }, page && !p ? 'back' : !page && p ? 'open' : null) }
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
  // Il tocco su una scheda della home: sulla plancia apre e chiude la sua colonna (Franz, 05/10 11:09), sul telefono apre.
  // Il clic sulla scheda va al posto della colonna in vista; già in colonna la illumina invece di chiuderla (Franz 04/11,
  // app-scheda-sostituisce-04-11). Il + nel bordo aggiunge, il filo chiude.
  function card(name: string) {
    if (name === MASTER || !wide) { pick(name); return }
    if (cols.includes(name)) document.querySelector<HTMLElement>(`.desk .col[data-col="${CSS.escape(name)}"]`)?.animate([{ boxShadow: '0 0 0 2px var(--wait)' }, { boxShadow: '0 0 0 2px transparent' }], { duration: 1200, easing: 'ease-out' })
    else setCols(replaceCol(cols, name), name, cardOf(name))
  }
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
  /** Vero se il PC ha preso il messaggio (app-feedback-azioni-02-11: chi ha toccato sa se la scheda può uscire). */
  async function sendTo(name: string, text: string): Promise<boolean> {
    // Senza trasporto il messaggio resta «inviato al PC».
    const m: Sent = { id: crypto.randomUUID(), session: name, text, sentAt: nowS() }
    msgs = [...msgs, m]
    if (!tr) return true
    pend = { ...pend, [m.id]: 'sending' }
    // Contratto 1.65: a una sessione chiusa il relay la riapre e poi consegna.
    const closed = st.sessions.find(x => x.name === name)?.state === 'gone'
    if (closed) say(t.reopening(name))
    try {
      const r = await tr.send({ ...newCmd('prompt', name, text), id: m.id, ...(closed ? { reopen: true } : {}) })
      res = { ...res, [m.id]: r }
      return r.ok
    } catch { pend = { ...pend, [m.id]: 'failed' }; return false }
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
    const blob = new Blob([f.data as Uint8Array<ArrayBuffer>], { type: f.mime })
    // Il testo letto subito: «Copia» lo scrive negli appunti senza await (Safari).
    return { blob, name: f.name ?? path.split('/').pop() ?? 'file', mime: f.mime, text: files.copyKind(f.mime) === 'text' ? await blob.text() : undefined }
  }
  const getFile = (name: string, path: string, key: string, quiet = false) => fileStore.get(key, loadFile(name, path, quiet))
  // Un file aperto in chat è visto anche in Risultati (app-risultati-visti-03-11, Franz: «rimangono presenti anche se li vedo altrove»).
  // Il menu unico della sessione (app-menu-sessione-03-11): una voce di Tocca a te apre la sua pagina; Guarda solo i lavori di quella sessione.
  function mineGo(name: string, kind: MenuKind) {
    if (kind === 'guarda') { watchSession = name; watchOpenId = null; openPage('guardare') }
    else if (kind === 'prove') openPage('prove')
    else if (kind === 'decidi') openPage('decisions')
    else if (kind === 'seguiti') openPage('seguiti')
  }
  function seenPath(path: string) { for (const id of seenByPath(st.results?.items ?? [], path)) void run(newCmd('result_seen', null, id)) }
  function fileAction(name: string, path: string, act: FileAct | 'prepare' | 'preview' | 'seen', key: string) {
    if (!tr) return
    if (act !== 'prepare') seenPath(path)
    if (act === 'seen') return
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
  // Due o più file di seguito (Franz, 16/10 16:01): «Salva tutti» in uno zip, «Condividi tutti» in un solo foglio; come per
  // il singolo file, con tutti pronti la condivisione parte senza await.
  function filesAction(name: string, list: { path: string; key: string }[], act: 'save' | 'share' | 'prepare') {
    if (!tr) return
    if (act === 'prepare') { for (const { path, key } of list) void getFile(name, path, key, true); return }
    if (!list.every(({ key }) => fileStore.peek(key))) say(t.fileOpening(t.filesGroup(list.length)))
    const done = fileStore.tapAll(list.map(({ path, key }) => ({ key, load: loadFile(name, path, false) })), fs => {
      notice = null
      return act === 'share' ? files.shareAll(fs) : files.saveAll(fs, zipName(name, new Date()))
    })
    Promise.resolve(done).catch(e => {
      const n = (e as Error)?.name
      if (n === 'NotAllowedError') say(t.fileTapAgain)
      else if (n !== 'AbortError') say(`✗ ${t.fileShareFailed}`)
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
      // Rifiutata, pannello e card dicono perché finché non li togli (Franz, 04/11: il Chiudi dà sempre un esito); senza
      // risposta la chiusura può essere in corso.
      run(newCmd(op, name, arg, text)).then(r => {
        if (r && leaving?.name === name && leaving.sentAt === l.sentAt) {
          const rf = r.ok ? null : refusals(r.text)[0]
          leaving = r.ok ? { ...l, delivered: true } : { ...l, refused: rf?.why ?? r.text, armable: rf?.armable ?? false }
        }
      })
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
  // Le chat a schermo di cui è già partito il /tail (firstTails).
  const tailed = new Set<string>()
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
      for (const n of names) if (n && !shownBefore.has(n)) { reads.delete(n); delete fresh[n]; tailed.delete(n) }
      shownBefore = new Set(names.filter((n): n is string => !!n))
      // Il /tail di tutte insieme, subito: le ultime voci compaiono senza aspettare in fila le letture dal relay.
      for (const n of firstTails(names, st.sessions.map(x => x.name), tailed)) { tailed.add(n); loadTail(n).then(t => { if (t) fresh[n] = true }) }
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
        // Contratto 1.59: il /tail è già partito sopra; qui la pagina dal relay, poi le voci dopo l'ultima.
        loadTranscript(n)
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
  // I KPI 6, 11 e 18 della home (compito app-kpi-medie-25-10): la cronologia di oggi di tutte le sessioni, una lettura
  // passiva al più ogni 5 minuti, solo se il relay ha l'op.
  let kpiPage = $state<TimelinePage | null>(null)
  let kpiAsked = 0
  $effect(() => {
    if (!tr || !st.ops?.includes('timeline')) return
    void st.ts
    const at = nowS()
    if (at - kpiAsked < 300) return
    kpiAsked = at
    untrack(() => run(newCmd('timeline', null, String(kpiSince(at))))).then(r => { if (r?.ok) kpiPage = JSON.parse(r.text) })
  })
  // Le proposte 8, 9, 10, 12, 13 e 17 (contratto 1.96, compito app-kpi-dati-relay-25-10): l'op `kpi`, una lettura passiva
  // al più ogni 5 minuti, solo se il relay ha l'op.
  let kpiData = $state<KpiData | null>(null)
  let kpiDataAsked = 0
  $effect(() => {
    // Anche la demo risponde (app-demo-sempre-allineata-03-11): i riquadri che vivono dell'op non restano vuoti.
    if (!tr || !st.ops?.includes('kpi')) return
    void st.ts
    const at = nowS()
    if (at - kpiDataAsked < 300) return
    kpiDataAsked = at
    untrack(() => run(newCmd('kpi', null))).then(r => { if (r?.ok) kpiData = JSON.parse(r.text) })
  })
  const answer = (n: string, x: number) => { pick(n); cmd(n)('answer', String(x)) }
</script>

<svelte:window bind:innerWidth={width} />

{#if pairing && dock}
<DockEmbed master={null} entries={[]} onSpeak={() => {}}>
  {#snippet chat()}{/snippet}
  {#snippet pair()}<PairPage {store} onPaired={() => location.reload()} />{/snippet}
</DockEmbed>
{:else if pairing}
<PairPage {store} onPaired={() => location.reload()} />
{:else}

{#snippet chatOf(name: string, inColumn: boolean)}
  {@const s = st.sessions.find(x => x.name === name) ?? ghost(name)}
  {#snippet resultsRow()}{#if st.ops?.includes('results')}<ResultsRow {run} session={name} summary={st.results} onOpen={() => { watchSession = name; watchOpenId = null; openPage('guardare') }} />{/if}{/snippet}
  <Chat {resultsRow} focus={focusOf[name] ?? null} onMine={(k) => mineGo(name, k)} onHeadsUp={(text) => sendTo(name, text)} onTask={() => openPage('lavori')} {st} {s} entries={transcripts[name] ?? []} mine={mine.filter(([m]) => m.session === name)} onSend={(x) => sendTo(name, x)} onPick={pick} waitingSince={waitSince[name] ?? null}
    updating={!!tr && s.state !== 'gone' && !fresh[name] && behind(s, bgReads[name])}
    onSendAt={st.ops?.includes('project_new') && name !== MASTER ? (x, at) => sendAtTo(name, x, at) : null}
    onSendTo={name === MASTER ? (n, x) => { sendTo(n, x); say(t.mentionSent(n)) } : null}
    onAnswer={answer} onCmd={cmd(name)} {events} {sent} {read} onRead={(k) => (read = new Set([...read, k]))} onPromptTo={sendTo} onAttach={(fs, x) => attach(name, fs, x)} onFile={(p, a, k) => fileAction(name, p, a, k)} onFiles={(fs, a) => filesAction(name, fs, a)} onHandoff={() => handoff(name)}
    wide={false} {slots} elsewhere={elsewhereFor(name)} elsewhereAway={embed ? (() => { const a = elsewhereFor(name); return a ? awayOf(a, st.sessions, location.origin) : null })() : null} onElsewhere={() => { const a = elsewhereFor(name); if (a) openAlert(a, name) }}
    onElsewhereDismiss={() => { const a = elsewhereFor(name); if (a) seenAlerts = new Set([...seenAlerts, ...alertKeys(a)]) }} onBack={inColumn ? undefined : () => smooth(() => { open = null })}
    inject={name === MASTER ? masterInject : null} onInjected={() => (masterInject = null)} onUsage={goUsage} onOlder={olderOf[name] ? () => loadOlder(name) : null} onNewer={newerOf[name] ? () => loadNewer(name) : null} onLatest={newerOf[name] ? () => toLatest(name) : null}
    onGo={inColumn ? null : (n) => (n == null ? smooth(() => { open = null }) : pick(n))} onReopen={st.ops?.includes('reopen') ? (n) => cmd(n)('reopen') : null} appMenu={inColumn ? null : appMenu} menuMove={null}
    onDiff={st.ops?.includes('diff') ? (id) => ask(tr, newCmd('diff', name, id), decodeDiff) : null}
    onChanges={st.ops?.includes('changes') ? (f) => ask(tr, newCmd('changes', name, f == null ? null : fileArg(f)), decodeChanges) : null} />
{/snippet}

{#snippet appMenu()}<AppBar {st} now={now} openCount={summary.open} onPage={openPage} onFeedback={openFeedback} menuOnly />{/snippet}

{#snippet homePane()}
  <!-- Larga con le sessioni aperte la master non sta più nella colonna della home: ha il pannello e la barra della plancia
       (Franz 03/11 13:10, variante A, app-pwa-master-tasti-03-11). -->
  <HomePane master={wide ? null : master} entries={transcripts[MASTER] ?? []} open={masterOpen} onToggle={(o) => smooth(() => { masterOpen = o; masterHalf = o && wide })} onSpeak={(x) => toggle(x, MASTER)}
    halfStops={wide} half={masterHalf} onHalf={(h) => (masterHalf = h)}>
    {#snippet menu()}<AppBar {st} now={now} openCount={summary.open} onPage={openPage} onFeedback={openFeedback} menuOnly />{/snippet}
    {#snippet quick()}{@render masterQuick()}{/snippet}
    {#snippet list()}
      <AppBar {st} now={now} openCount={summary.open} onPage={openPage} onFeedback={openFeedback} quota={false} />
      <div class="list" use:fadeEnd>{@render homeView(null)}</div>
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

<!-- La home, intera (part null: telefono e colonna home della web larga) o per parti nelle colonne della plancia (pwa2). -->
<!-- Le decisioni e le verifiche di una sessione, nella sua scheda aperta (app-sessioni-compatte-06-11). -->
{#snippet sessionTodo(name: string)}<CinqueMinuti rows allOpen only={(g) => g.items.some(i => i.session === name)} onAsk={(x) => sendTo(MASTER, x)} {st} {run} onApprove={approve} onDone={(i) => { if (i.kind === 'needs_you' && i.session) attentionDone({ kind: 'needs_you', ref: i.session, since: i.since }); else { const c = st.commitments?.find(x => x.task === i.task); if (c) doneForPerson(c) } }} />{#if todo[name]?.v}<DaProvare {run} only={(p) => provaSession(p) === name} onScreen={(s) => openPage(s === 'home' ? null : s === 'usage' ? 'overview' : s as PageName)} onShot={(file, note) => attach(MASTER, [file], note)} />{/if}{/snippet}
{#snippet decisionsBody(rows = false)}<CinqueMinuti {rows} only={(g) => !decOfSession(g)} onAsk={(x) => sendTo(MASTER, x)} {st} {run} onApprove={approve} onDone={(i) => { if (i.kind === 'needs_you' && i.session) attentionDone({ kind: 'needs_you', ref: i.session, since: i.since }); else { const c = st.commitments?.find(x => x.task === i.task); if (c) doneForPerson(c) } }} />{/snippet}
<!-- I corpi delle voci del programma del 01/11, gli stessi delle pagine: si aprono sul posto nella home (Franz 02/11 08:05). -->
{#snippet sectionBody(k: string)}
  {#if k === 'seguiti'}<Seguiti {run} summary={st.seguiti} onLavori={() => openPage('lavori')} onMaster={() => { openPage(null); pick(MASTER) }} />
  {:else if k === 'prima_raccolta'}<PrimaRaccolta {run} summary={st.prima_raccolta} onScreen={(s) => openPage(s === 'home' ? null : s === 'usage' ? 'overview' : s as PageName)} onFeatures={() => openPage('features')} />
  <!-- Verifica (app-tocca-sezioni-06-11): le prove, poi i lavori consegnati da guardare. -->
  {:else if k === 'prove'}{#if st.prove && st.prove.n + st.prove.ko + st.prove.red > 0}<DaProvare {run} only={(p) => { const s = provaSession(p); return !(s && liveSet.has(s)) }} onScreen={(s) => openPage(s === 'home' ? null : s === 'usage' ? 'overview' : s as PageName)} onShot={(file, note) => attach(MASTER, [file], note)} />{/if}{#if st.results?.n_unseen}{@render guardare('body')}{/if}
  <!-- Prima raccolta sta dentro Decidi finché c'è (app-sezioni-accorpate-03-11, scelta A). -->
  {:else if k === 'decisioni'}{#if st.prima_raccolta}<PrimaRaccolta {run} summary={st.prima_raccolta} onScreen={(s) => openPage(s === 'home' ? null : s === 'usage' ? 'overview' : s as PageName)} onFeatures={() => openPage('features')} />{/if}{#if st.decisions?.n}{@render decisionsBody(true)}{/if}
  {:else if k === 'da_guardare'}{@render guardare('body')}{/if}
{/snippet}
<!-- Contratto 1.106 (app-da-guardare-02-11): i file consegnati dalle sessioni; i file arrivano con l'op `file`, a nome della sessione. -->
{#snippet guardare(mode: 'body' | 'page')}<Guardare {run} summary={st.results} {mode} session={mode === 'page' ? watchSession : null} openId={mode === 'page' ? watchOpenId : null}
  getFile={(r, f, quiet) => getFile(r.session ?? '', f.path, `result:${r.id}:${f.path}`, quiet).then(x => x?.blob ?? null)}
  onExternal={(r) => fileAction(r.session ?? '', r.path, 'open', `result:${r.id}:${r.path}`)} onSession={(n) => { openPage(null); pick(n) }}
  onAll={() => { watchSession = null; watchOpenId = null; openPage('guardare') }} onOpen={(r) => { watchSession = null; watchOpenId = r.id; openPage('guardare') }} />{/snippet}
{#snippet homeView(part: 'you' | 'work' | 'out' | 'tools' | 'top' | null)}<Home st={stShown} {part} onMine={mineGo} onRestart={handoff} onHomeOrder={!part && st.ops?.includes('home_order') ? (a) => saveHomeOrder(a, false) : null} afterSessions={part === 'work' ? deskTasks : undefined} selected={[]} onPick={card} onAnswer={answer} onStep={(n, x) => { pick(n); sendTo(n, x) }} usage={quotaPanels} usageBlock={usageV3}
        night={nightBoxShown ? nightModel : undefined} {nightTitle} {justClosed} closing={leaving && !leaving.refused ? leaving.name : null} closeRefused={leaving?.refused ? { name: leaving.name, why: leaving.refused, armable: !!leaving.armable } : null} onCloseArm={arm} onCloseSeen={() => (leaving = null)} elicitations={openElicitations(st, now).filter(e => !elicitSent.has(e.id))} onElicit={elicit} newBody={sectionBody} suggestionNames={suggestionNamesOf(sug.all, k => t.sugKind[k] ?? t.sugKindOther)} suggestionsAt={sug.shown[0]?.at} suggestions={sugCard} suggestionsN={sug.shown.length} suggestionsTotal={sug.total} onClosePreview={st.ops?.includes('close_preview') && st.ops?.includes('close') ? closePreview : null} onCloseMany={closeMany} onSystem={tr && !demo ? sysAct : null} {kpiPage} {kpiData} onBox={(b) => openPage(`box-${b}`)} onQuota={() => openPage('overview')} pluginsLoad={tr && st.ops?.includes('plugins') ? pluginsLoad : null} pluginsRun={tr && st.ops?.includes('plugin_update') ? pluginsRun : null} onNight={() => openPage('night')}
        onApprove={approve} {todo} {sessionTodo} onApprovalClose={closeApproval} onDone={attentionDone} onOk={(n, x) => sendTo(n, x)} onClose={(n) => cmd(n)('slash', 'exit')} onSessionCmd={(n, op, arg) => cmd(n)(op, arg)} agenda={agenda.page} {donePages} onLavori={() => openPage('lavori')} onEdit={editAgenda} onStart={(a) => sendTo(a.to, a.send)} onDoneForPerson={doneForPerson}
        {usageFocus} onUsageFocused={() => (usageFocus = 0)} onSpeak={(x) => toggle(x)}
        {events} sent={scheduled} {read} {now} onService={service} lastNight={nightModel} onReopen={st.ops?.includes('reopen') ? (n) => cmd(n)('reopen') : null} />{/snippet}
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
  <!-- La scheda «Ti aspetta» (contratto 1.130, app-scheda-ti-aspetta-07-11): ha la sua testata, con la sessione e da quanto aspetta. -->
  {#if p === 'attesa' && waitId}
  <TiAspettaView id={waitId} {st} now={st.ts} wide={wide} {run} onBack={() => { waitId = null; if (location.hash.startsWith('#/attesa/')) history.replaceState(null, '', location.pathname + location.search); openPage(null) }}
    onOpenWait={(w) => (waitId = w)} onJump={(n, entry, sid) => { const live = st.sessions.some(x => x.name === n && x.state !== 'gone'); waitId = null; openPage(null); if (!live && sid) { sidOf = { ...sidOf, [n]: sid }; reading = n } else pick(n); void jumpTo(n, entry) }}
    onSession={(n) => { waitId = null; openPage(null); pick(n) }} onApprove={approve} onAsk={(x) => sendTo(MASTER, x)}
    onDone={(i) => { if (i.kind === 'needs_you' && i.session) attentionDone({ kind: 'needs_you', ref: i.session, since: i.since }); else { const c = st.commitments?.find(x => x.task === i.task); if (c) doneForPerson(c) } }}
    onAnswer={(n, x) => cmd(n)('answer', String(x))} onPrompt={(n, text) => sendTo(n, text)} onAttentionDone={(kind, ref) => attentionDone({ kind, ref })}
    onScreen={(s) => { waitId = null; openPage(s === 'home' ? null : s === 'usage' ? 'overview' : s as PageName) }} onShot={(file, note) => attach(MASTER, [file], note)} />
  {:else}
  <Page title={pageTitle[p]} v3 onBack={() => openPage(null)}>
    {#if p === 'launch'}
      <Launch {st} onSession={(n, reopen) => { if (reopen) cmd(n)('reopen'); else { openPage(null); pick(n) } }}
        onLaunch={(pr, first) => { cmd(pr.name)('launch', pr.path, first || undefined); openPage(null) }}
        onNewProject={st.ops?.includes('project_new') && st.project_parents?.length ? newProject : null} />
    {:else if p === 'lavori'}
      <Lavori onSpeak={(x) => toggle(x)} st={stShown} agenda={agenda.page} {events} {now} header="none" kpi={kpiData} onApprove={approve} onAgendaEdit={editAgenda} onStart={(a) => sendTo(a.to, a.send)} onDoneForPerson={doneForPerson}
        onAddNight={() => nightDlg?.showModal()} onRemoveNight={(id) => cmd('')('night_remove', id)} onNight={() => openPage('night')} onSession={(n) => { openPage(null); pick(n) }} {donePages} onLoadDay={loadDone} />
    {:else if p === 'night'}
      <NightPage model={nightModel} error={night.error} loading={night.loading} onRefresh={loadNight}
        onChat={(n, sid) => { if (sid) { sidOf = { ...sidOf, [n]: sid }; reading = n; loadTranscript(n) } else { openPage(null); pick(n) } }} onAnswer={(n, x) => cmd(n)('answer', String(x))}
        onApprove={(task) => approve(task, '')} onSend={(n, text) => sendTo(n, text)} />
    {:else if p === 'search'}
      <Search {sent} {events} remote onQuery={search} page={searchPage}
        known={new Set(st.sessions.map(x => x.name))} sessions={st.sessions} now={st.ts} onOpen={(n, entry, sid, live) => { if (n && live === false && sid && entry) { sidOf = { ...sidOf, [n]: sid }; openPage(null); reading = n; void jumpTo(n, entry) } else if (n) { openPage(null); pick(n); if (entry) jumpTo(n, entry) } else openPage('lavori') }} />
    {:else if p === 'usage'}
      <UsagePage panels={quotaPanels} st={stShown} page={kpiPage} data={kpiData} {now} />
    {:else if p === 'guardare'}
      {@render guardare('page')}
    {:else if p === 'seguiti'}
      <Seguiti {run} summary={st.seguiti} onLavori={() => openPage('lavori')} onMaster={() => { openPage(null); pick(MASTER) }} />
    {:else if p === 'prima-raccolta'}
      <PrimaRaccolta {run} summary={st.prima_raccolta} onScreen={(s) => openPage(s === 'home' ? null : s === 'usage' ? 'overview' : s as PageName)} onFeatures={() => openPage('features')} />
    {:else if p === 'prove'}
      <DaProvare {run} onScreen={(s) => openPage(s === 'home' ? null : s === 'usage' ? 'overview' : s as PageName)} onShot={(file, note) => attach(MASTER, [file], note)} />
    {:else if p === 'decisions'}
      {@render decisionsBody()}
    {:else if p === 'features'}
      <Features onRoute={(r) => openPage(r === 'usage' ? 'overview' : r as PageName)} />
    {:else if p === 'homeorder'}
      <HomeOrder saved={st.home_order} onSave={saveHomeOrder} />
    {:else if p === 'settings'}
      <Settings m={devicesOf(st.host, st, freshness(st, now), now, '', __APP_VERSION__, true, null, false, null)}
        devices={linkedDevices(st, remote?.uid ?? null, now) ?? []} now={now} channel={access ? t.channelLocal : remote ? t.channelFirebase : t.channelDemo} onRate={setRate} onVoice={setVoice}
        details={wide ? details : null} onDetails={(on) => { details = on; save('cm.details', on ? '1' : '0') }} {push} onPush={setPush}
        onFeatures={() => openPage('features')} onHomeOrder={st.ops?.includes('home_order') ? () => openPage('homeorder') : null} onAdd={tr && st.ops?.includes('pair_add') ? () => addDlg?.open() : null} demo={!!demo}
        onRevoke={tr && !demo && st.ops?.includes('unpair') ? (uid) => run(newCmd('unpair', null, uid)) : null} />
    {:else if p === 'box-aspetta' || p === 'box-lavoro' || p === 'box-esiti'}
      <HomeBox box={p === 'box-aspetta' ? 'aspetta' : p === 'box-lavoro' ? 'lavoro' : 'esiti'} st={stShown} page={kpiPage} {now} onSession={(n) => { openPage(null); pick(n) }} onProve={() => openPage('prove')} />
    {:else if p === 'queue'}
      <Queue {st} now={now} skip={queueSkip} onAnswer={(n, arg, op = 'answer') => cmd(n)(op, arg || undefined)} onSession={(n) => { openPage(null); pick(n) }} onDone={attentionDone} onOk={(n, x) => sendTo(n, x)} />
    {:else}
      <p class="soon">{t.soon}</p>
    {/if}
  </Page>{/if}
{/snippet}

<!-- Un account fermo (dato vecchio) non ha il suo riquadro se l'altro è aggiornato, come nella riga in alto (Franz, 08/10 20:32). -->
{#snippet usageV3()}<Utilizzo rings={overview.rings} st={stShown} page={kpiPage} data={kpiData} {now} onDetails={goUsage} />{/snippet}
{#snippet quotaPanels()}{#each overview.rings.some(r => !r.stale) ? overview.rings.filter(r => !r.stale) : overview.rings as r (r.account)}<QuotaPanel ring={r} now={now} />{/each}<TodayPanel bars={overview.today} usage={usageRows(st.usage_today)} />{/snippet}

{#snippet inspector()}
  {@const first = st.sessions.find(x => x.name === cols[0])}
  {#if first}<Inspector i={inspect(first, timeline, now)} quotaH5={st.quota[first.account]?.h5 ?? null} loading={!!tr && !timeline} />{/if}
{/snippet}

{#snippet columnTab(name: string, grab: (e: PointerEvent) => void)}
  {@const s = st.sessions.find(x => x.name === name)}
  {#if s}<ColumnTab {s} r={rowOf[name]} {now} onGrab={grab} onFocus={() => focusColumn(name)} onClose={() => setCols(cols.filter(c => c !== name))} />{/if}
{/snippet}

{#snippet sugCard()}{#if sug.shown.length}<SuggestionsCard shown={sug.shown} onAnswer={suggest} />{/if}{/snippet}
{#snippet lavoriLeft()}<Lavori onSpeak={(x) => toggle(x)} st={stShown} agenda={agenda.page} {events} {now} header="desk" part="left" suggestionsAt={sug.shown[0]?.at} suggestions={sugCard} onApprove={approve} onAgendaEdit={editAgenda} onStart={(a) => sendTo(a.to, a.send)} onDoneForPerson={doneForPerson}
  onAddNight={() => nightDlg?.showModal()} onRemoveNight={(id) => cmd('')('night_remove', id)} onNight={() => openPage('night')} onSession={(n) => pick(n)} {donePages} onLoadDay={loadDone} />{/snippet}
{#snippet lavoriRight()}<Lavori onSpeak={(x) => toggle(x)} st={stShown} agenda={agenda.page} {events} {now} header="none" part="right" kpi={kpiData} onApprove={approve} onAgendaEdit={editAgenda} onStart={(a) => sendTo(a.to, a.send)} onDoneForPerson={doneForPerson}
  onNight={() => openPage('night')} onSession={(n) => pick(n)} {donePages} onLoadDay={loadDone} />{/snippet}
{#snippet deskTasks()}<DeskWork part="progress" st={stShown} agenda={agenda.page} {now} {events} {donePages} onLavori={() => openPage('lavori')} />{/snippet}
<!-- La master nella plancia (pwa2): la barra in fondo, fuori dalla griglia; a metà o aperta un pannello a destra, alto quanto lo
     schermo (420 o 720 px). Il tasto tondo allarga o stringe, la × chiude; il tocco sulla barra stringe e poi chiude. -->
<!-- La barra larga: prima il campo, poi il testo della master (Franz 03/11 16:45, dalla PWA). -->
<!-- Lo scheletro di una colonna della plancia finché il relay non risponde (app-avvio-senza-salto-04-11, Franz 04/11: «compare
     prima semivuota e poi di colpo piena»). -->
{#snippet pskel()}<div class="skel" aria-hidden="true"><i class="h"></i><i></i><i class="s"></i><i></i><i class="s"></i><i></i></div>{/snippet}
{#snippet masterBar()}{#if master}<div class="mbar" style:view-transition-name={wide && !masterOpen ? 'master-morph' : null}>{#if wide}{#each ['left', 'right'] as const as sd (sd)}<button class="hgrip bargrip" class:r={sd === 'right'} style={sd === 'left' ? `left:${barColW() / 2}px` : `right:${barColW() / 2}px`} aria-label={sd === 'left' ? t.masterBarGripLeft : t.masterBarGripRight} title={sd === 'left' ? t.masterBarGripLeft : t.masterBarGripRight} onpointerdown={(e) => { e.preventDefault(); heightGesture(e.clientY, sd) }}><i></i></button>{/each}{/if}<div class="mquick">{@render masterQuick()}</div><MasterDock {master} entries={transcripts[MASTER] ?? []} bar onPlace={wide ? barDrag : undefined} onToggle={() => smooth(() => { masterOpen = true; masterHalf = masterNarrow() })} onSpeak={(x) => toggle(x, MASTER)} /><button class="mup" aria-label={t.dockConversation} title={t.dockConversation} onclick={() => smooth(() => { masterOpen = true; masterHalf = masterNarrow() })}><span class="ms" aria-hidden="true">expand_less</span></button></div>{/if}{/snippet}
<!-- Il pannello della master nel cruscotto con le prese di quello accanto alle colonne (web-maniglia-riga-colonna-06-11, Franz
     06/11 00:05): la presa in alto ‹ ━ › porta alla riga in basso o cambia lato, la corsia la larghezza; come MasterFrame sul tablet. -->
{#snippet masterPanelGrips()}<button class="wgrip" class:onleft={masterLeft} aria-label={t.masterWidthEdge} title={t.masterWidthEdge} onpointerdown={widthDown} onpointermove={widthMove} onpointerup={widthUp} onpointercancel={widthCancel}></button><button class="hgrip top" aria-label={t.masterGrab} title={t.masterGrab} onpointerdown={topDown}><i></i></button>{@render masterPanel('dash')}{/snippet}
{#snippet masterPanel(where: 'dash' | 'desk')}{#if master}<MasterDock {master} entries={transcripts[MASTER] ?? []} expanded side keys={false} places={masterMenu} mirror={masterLeft} toggleUp={masterHalf} onToggle={() => taps.tap('head', performance.now(), () => smooth(() => { if (stacked) stepStack(1); else if (masterHalf) masterOpen = false; else masterHalf = true }))}
  onToggleKey={() => corners(masterSizeKey)} onClose={() => smooth(() => { masterOpen = false })} onMove={moveMaster} moveLabel={moveLabelOf()} onSpeak={(x) => toggle(x, MASTER)} />{#if stacked && stackH === 'half'}<button class="headkey" aria-expanded={stackHead} onclick={() => smooth(() => (stackHead = !stackHead))}>{t.masterHeadRow}<span class="ms" aria-hidden="true">{stackHead ? 'expand_less' : 'expand_more'}</span></button>{/if}<div class="mchat" {@attach chatHost(where)}></div>{/if}{/snippet}


{#snippet deskPage()}{#if page}{@render pageView(page)}{/if}{/snippet}

{#if dock}
  <DockEmbed master={master ?? dockCached?.master ?? ghost(MASTER)} entries={transcripts[MASTER] ?? dockCached?.entries ?? []} onSpeak={(x) => toggle(x, MASTER)}>
    {#snippet chat()}{@render chatOf(MASTER, true)}{/snippet}
  </DockEmbed>
{:else if embed}
  <div class="embedded v3">{@render chatOf(MASTER, true)}</div>
{:else if wide}
  <!-- Cruscotto e plancia montati una volta (app-fluidita-montaggio-06-11, lib/park.ts): quello che non si vede resta montato fuori
       dal documento; il secondo si monta la prima volta che la pagina è ferma, e da lì aprire e chiudere una sessione li scambia
       invece di rifarli. La chat della master è una sola e passa nel pannello di quello che si vede. -->
  {#if keepBoth || deskDashboard}<div class="wslot"><div class="wslot" {@attach park(deskDashboard)}>
  <!-- Le sessioni prima (app-sessioni-evidenti-06-11, scelta J2 di Franz, 06/11 00:52): Sessioni, Tocca a te con Esiti chiuso in
       fondo, Risorse; le altre nell'ordine della home. -->
  <!-- Tre colonne come la home (app-home-tre-categorie-06-11): Sessioni (con Notte, In corso, Esiti e Fatto oggi), Tocca a te, Risorse. -->
  <Plancia {width} order={['work', ...decodeCats(st.home_order).map(c => c.id).filter(id => id !== 'work' && id !== 'out')]} top={null} panel={master && masterOpen ? masterPanelGrips : null} panelWide={!masterHalf} panelLeft={masterLeft}>
    {#snippet head()}<AppBar {st} now={now} openCount={summary.open} onPage={openPage} onFeedback={openFeedback} quota={false} />{/snippet}
    <!-- Lo spazio in cima della home (app-home-in-cima-02-11): una fascia a tutta larghezza sopra le colonne. -->
    {#snippet topBand()}{@render homeView('top')}{/snippet}
    {#snippet you()}{#if ready}<div class="pboot" class:boot={bootFade} style:animation-delay="0ms"><DeskHead kind="you" h={kpiHeadOf(st, kpiPage, now)} master={fromMaster} {st} {now} onOpen={() => openPage('box-aspetta')} />{@render homeView('you')}</div>{:else}{@render pskel()}{/if}{/snippet}
    {#snippet work()}{#if ready}<div class="pboot" class:boot={bootFade} style:animation-delay="60ms"><DeskHead kind="work" h={kpiHeadOf(st, kpiPage, now)} {st} {now} open={summary.open} onOpen={() => openPage('box-lavoro')} />{@render homeView('work')}<DeskWork part="done" st={stShown} agenda={agenda.page} {now} {events} {donePages} onLavori={() => openPage('lavori')} /></div>{:else}{@render pskel()}{/if}{/snippet}
    {#snippet out()}{#if ready}<div class="pboot" class:boot={bootFade} style:animation-delay="120ms"><DeskHead kind="out" h={kpiHeadOf(st, kpiPage, now)} {st} {now} onOpen={() => openPage('box-esiti')} />{@render homeView('out')}<DeskNight night={nightModel} loading={night.loading} onNight={() => openPage('night')} /><DeskWork part="done" st={stShown} agenda={agenda.page} {now} {events} {donePages} onLavori={() => openPage('lavori')} /></div>{:else}{@render pskel()}{/if}{/snippet}
    {#snippet tools()}{#if ready}<div class="pboot" class:boot={bootFade} style:animation-delay="180ms"><DeskHead kind="tools" h={kpiHeadOf(st, kpiPage, now)} {st} {now} onOpen={() => openPage('overview')} />{@render homeView('tools')}<DeskTools {st} {now} onPick={card} onTalk={talk} onLaunch={(pr) => cmd(pr.name)('launch', pr.path)} /></div>{:else}{@render pskel()}{/if}{/snippet}
    {#snippet bar()}{@render masterBar()}{/snippet}
  </Plancia>
  </div></div>{/if}
  <!-- Con le sessioni aperte la master si comporta come senza (variante A, app-pwa-master-tasti-03-11): chiusa la barra in fondo,
       aperta il pannello di lato con i tasti di sempre (lato, metà schermo, chiudi); lato e misura restano ricordati. -->
  {#if keepBoth || !deskDashboard}<div class="wslot">
  <div class="deskwrap" class:left={masterLeft} class:stacked {@attach park(!deskDashboard)}>
  <div class="dstage">
  <!-- Proposta 1 (Franz 03/11 13:30, app-pwa-master-tasti-03-11): home e master stanno insieme, la master sul bordo e la home
       accanto; «Sposta» della master porta di lato tutte e due. Le frecce della home restano solo senza master. -->
  <Desk {cols} {shares} homeRight={master ? !masterLeft : homeRight} tab={wco ? columnTab : null} onCols={dropCols} onShares={setShares} override={page ? deskPage : null} details={details && cols.length && !page ? inspector : null}
    onHomeSide={master ? null : () => smooth(() => { homeRight = !homeRight; save('cm.home_right', homeRight ? '1' : '0') })}>
    {#snippet home()}{#if !stacked}{@render homePane()}{/if}{/snippet}
    <!-- app-colonne-strette-05-11 (scelta A): la colonna senza posto leggibile è una striscia; la X la toglie come quella della testata. -->
    {#snippet strip(name, open)}
      {@const ses = st.sessions.find(x => x.name === name)}
      <div class="cstrip" role="button" tabindex="0" aria-label={t.stripOpen(name)} title={name} onclick={open} onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); open() } }}>
        <button class="sx" aria-label={t.tabletClose(name)} title={t.tabletClose(name)} onclick={(e) => { e.stopPropagation(); setCols(cols.filter(c => c !== name)) }}>
          <svg viewBox="0 0 24 24" width="18" height="18"><path d="M6 6l12 12M18 6 6 18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg>
        </button>
        {#if ses}<Badge s={ses} size={22} />{/if}
        <span class="sname">{name}</span>
      </div>
    {/snippet}
    {#snippet column(name, grab)}
      {@const held = heldCols.includes(name)}
      {@const phase = splashOf(name, true)}
      {@const hr = headRow(name)}
      <div class="column" data-col={name} class:boot={bootFade}>
        {#if !ready}
          <!-- Lo scheletro della colonna salvata finché il relay non risponde (app-avvio-senza-salto-04-11). -->
          <div class="skel" aria-hidden="true"><i class="h"></i><i></i><i class="s"></i><i></i><i class="s"></i></div>
        {:else}
        {#if hr && !wco}<ColumnHead r={hr} now={now} onClose={() => (held ? dropCol(name) : setCols(cols.filter(c => c !== name)))} onGrab={grab} />{/if}
        <div class="cbody">{#if name === MASTER}{@render chatOf(name, true)}{:else}<div class="wslot" {@attach colHost(name)}></div>{/if}
          {#if phase}<CloseSplash {phase} column onArm={(w) => arm(name, w)} onDismiss={() => (leaving = null)} onHome={() => dropCol(name)} onReopen={phase.kind === 'closed' && st.ops?.includes('reopen') ? () => { cmd(name)('reopen'); dropCol(name) } : null} />{/if}
        </div>
        {/if}
      </div>
    {/snippet}
    <!-- Senza colonne il cruscotto a tre colonne (Franz, 21/10 13:02): cosa serve da te, il lavoro, numeri e strumenti. -->
    {#snippet empty()}
      {#if !deskDashboard}<Dash {st} {now} cols={dashColumns(width)} night={nightModel} {nightTitle} nightLoading={night.loading} onNight={() => openPage('night')}
        {lavoriLeft} {lavoriRight} quota={quotaPanels} onPick={card} onTalk={talk}
        onLaunch={(pr) => cmd(pr.name)('launch', pr.path)} />{/if}
    {/snippet}
  </Desk>
  {#if master && !masterOpen && !cols.includes(MASTER)}{@render masterBar()}{/if}
  <!-- + e › nel bordo accanto a ogni sessione della home (app-sessione-sostituisci-03-11). -->
  {#if cols.length && !page}<SwapPills shown={cols} homeRight={master ? !masterLeft : homeRight} onAdd={(n) => { if (!cols.includes(n)) setCols(add(cols, n)) }} onClose={(n) => setCols(cols.filter(c => c !== n))} />{/if}
  </div>
  {#if master && masterOpen && !cols.includes(MASTER) && !stacked}<aside class="dpanel" class:wide={!masterHalf} style:view-transition-name="master-morph" style:width={masterW ? `${masterW}px` : null}><button class="wgrip" aria-label={t.masterWidthEdge} title={t.masterWidthEdge} onpointerdown={widthDown} onpointermove={widthMove} onpointerup={widthUp} onpointercancel={widthCancel}></button><button class="hgrip top" aria-label={t.masterGrab} title={t.masterGrab} onpointerdown={topDown}><i></i></button>{@render masterPanel('desk')}</aside>{/if}
  <!-- Home sopra e master sotto, nel lato e nella larghezza della master (Franz 03/11 17:30 e 17:31). -->
  {#if stacked}<aside class="dpanel stackcol" class:wide={!masterHalf} style:width={masterW ? `${masterW}px` : null}><button class="wgrip" aria-label={t.masterWidthEdge} title={t.masterWidthEdge} onpointerdown={widthDown} onpointermove={widthMove} onpointerup={widthUp} onpointercancel={widthCancel}></button><div class="hstack" data-h={stackH}><div class="hs-top" use:fadeEnd>{@render homePane()}</div><button class="grip" aria-label={t.masterStackStep} title={t.masterStackStep} onpointerdown={topDown}><i></i></button><div class="dpanel under" style:view-transition-name="master-morph" class:compact={stackH === 'compact'} class:nohead={stackH === 'half' && !stackHead}>{@render masterPanel('desk')}</div></div></aside>{/if}
  </div>
  </div>{/if}
  {#if master && (masterOpen || stacked)}<div class="wslot"><div class="wslot" {@attach chatNode}>{@render chatOf(MASTER, true)}</div></div>{/if}
  {#each colChats as n (n)}<div class="wslot"><div class="wslot" {@attach colChat(n)}>{@render chatOf(n, true)}</div></div>{/each}
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
                  {#if phase}<CloseSplash {phase} onArm={(w) => arm(phase.name, w)} onDismiss={() => (leaving = null)} onHome={splashHome} onReopen={phase.kind === 'closed' && st.ops?.includes('reopen') ? () => { cmd(sn)('reopen'); splashHome() } : null} />{/if}
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
<!-- Il contatore dei fotogrammi nascosto (app-fluidita-06-11): montato solo quando è acceso. -->
{#if meter.on}<FrameMeter />{/if}
{#if reading}
  <div class="embedded reading">
    <div class="rbar"><button class="rclose" aria-label={t.close} onclick={() => (reading = null)}>✕</button><b>{reading}</b><small>{t.readOnly}</small></div>
    {@render chatOf(reading, true)}
  </div>
{/if}
{#if demo}<div class="demobar" data-testid="demo-avviso"><span>{t.demoBanner}</span>{#if demoWhy === 'offline'}<button onclick={demoRetry}>{t.demoRetry}</button>{:else if demoWhy === 'denied'}<button onclick={demoPair}>{t.demoPair}</button>{/if}</div>{/if}
{#if !demo && (!ready || down)}<span class="demo mono">{ready ? t.relayDown : t.relayConnecting}</span>{/if}
{#if notice}<div class="notice" role="status">{notice}</div>{/if}
{/if}

<!-- «Aggiungi alla notte»: lo stesso foglio di Lancia, solo progetti (LaunchSheet con night_add). -->
<dialog bind:this={nightDlg} class="sheet" onclick={(e) => e.target === e.currentTarget && nightDlg?.close()}>
  <h2>{t.nightAddTitle}</h2>
  <NightAgendaPicker onDone={() => nightDlg?.close()} />
  <Launch {st} action={t.nightAddTitle} onLaunch={(pr, text) => { cmd(pr.name)('night_add', pr.path, text); nightDlg?.close() }} />
</dialog>

{#if fbOn}
<dialog bind:this={fbDlg} class="sheet fbsheet" aria-label={t.feedbackTitle} onclick={(e) => e.target === e.currentTarget && (fbOn = false)} oncancel={() => (fbOn = false)}>
  <h2>{t.feedbackTitle}</h2>
  <Feedback tech={fbTech} onGrab={'getDisplayMedia' in (navigator.mediaDevices ?? {}) ? fbGrab : null} onPrepare={st.ops?.includes('feedback_app') ? fbPrepare : null} onSend={st.ops?.includes('feedback_app') ? fbSend : null} onDone={() => (fbOn = false)} />
</dialog>
{/if}

<style>
  .mquick { padding: 0 0 4px; }
  .phone { height: 100%; display: flex; flex-direction: column; max-width: 760px; margin: 0 auto; }
  .list { flex: 1; min-height: 0; }
  .strip { flex: 1; min-height: 0; display: flex; overflow-x: auto; overflow-y: hidden; scroll-snap-type: x mandatory; overscroll-behavior-x: contain; scrollbar-width: none; }
  .strip::-webkit-scrollbar { display: none; }
  .demobar button { flex: none; padding: 4px 10px; border-radius: 999px; background: var(--wait); color: var(--bg); font-weight: 600; font-size: 12.5px; }
  .demobar { position: fixed; top: env(safe-area-inset-top); left: 0; right: 0; z-index: 60; display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 36px; box-sizing: border-box; padding: 4px 12px; background: color-mix(in srgb, var(--wait) 22%, var(--bg)); color: var(--wait); font-size: 12.5px; font-weight: 500; }
  .pg { flex: 0 0 100%; min-width: 0; scroll-snap-align: start; scroll-snap-stop: always; display: flex; flex-direction: column; }
  .reading:empty { display: none; }
  .nomaster { display: flex; align-items: center; gap: 12px; margin: 8px 16px; padding: 14px 16px; border-radius: 20px; background: var(--low); }
  .nomaster span { flex: 1; display: flex; flex-direction: column; gap: 2px; }
  .nomaster small { color: var(--text2); font-size: 14px; }
  .nomaster button { height: 44px; padding: 0 20px; border-radius: 999px; background: var(--primary); color: var(--on-primary); font-weight: 500; }
  .reading { padding: 8px 0; }
  .column { height: 100%; display: flex; flex-direction: column; }
  /* All'avvio (app-avvio-senza-salto-04-11): lo scheletro della colonna salvata, poi il contenuto entra con un'assolvenza breve e
     un leggero spostamento, con la durata e la curva della coreografia della home (v3.css). */
  .skel { display: flex; flex-direction: column; gap: 12px; padding: 18px 16px; }
  .skel i { display: block; height: 14px; border-radius: 7px; background: var(--s2); width: 70%; }
  .skel i.h { height: 22px; width: 45%; margin-bottom: 10px; }
  .skel i.s { width: 50%; }
  .column.boot > :global(*) { animation: boot-in calc(var(--reflow-ms) * .35) var(--reflow-ease) both; }
  /* La plancia al primo stato: le colonne entrano una dopo l'altra, 60 ms fra l'una e l'altra. */
  .pboot.boot { animation: boot-in calc(var(--reflow-ms) * .6) var(--spring-fx) both; }
  @media (prefers-reduced-motion: reduce) { .pboot.boot { animation: boot-fade 150ms ease-out both; } }
  /* A onda (app-effetti-movimenti-04-11, punto 6): ogni colonna sale di 14 px e si assesta con la molla, una dopo l'altra. */
  @keyframes boot-in { from { opacity: 0; translate: 0 14px; scale: .985; } }
  @media (prefers-reduced-motion: reduce) { .column.boot > :global(*) { animation: boot-fade 150ms ease-out both; } }
  @keyframes boot-fade { from { opacity: 0; } }
  .cbody { flex: 1; min-height: 0; position: relative; display: flex; flex-direction: column; }
  .soon { color: var(--text2); padding: 24px; }
  .sheet { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 0 0; width: min(600px, 100vw); max-height: 90vh; border-radius: 28px; }
  .sheet::backdrop { background: rgb(0 0 0 / .55); }
  .fbsheet { overflow-y: auto; }
  .sheet h2 { font-size: 22px; font-weight: 600; padding: 0 20px; }
  @media (max-width: 599px) { .sheet { margin: auto 0 0; max-width: 100vw; border-radius: 28px 28px 0 0; } }
  .notice { position: fixed; left: 50%; bottom: 72px; transform: translateX(-50%); max-width: min(560px, calc(100vw - 32px)); padding: 10px 16px; border-radius: 16px; background: var(--surface); color: var(--text); box-shadow: 0 4px 16px rgb(0 0 0 / .35); z-index: 20; }
  .demo { position: fixed; right: 12px; bottom: 6px; opacity: .6; pointer-events: none; }
  @media (max-width: 839px) { .demo { bottom: 0; right: 50%; transform: translateX(50%); font-size: 11px; } }
  .embedded { position: fixed; inset: 0; display: flex; flex-direction: column; background: var(--bg); }
  .embedded.reading { z-index: 70; }
  /* Sola lettura: niente campo di scrittura, né avvisi di altre sessioni. */
  .embedded.reading :global(form.field), .embedded.reading :global(.elsewhere) { display: none; }
  .rbar { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-bottom: 1px solid var(--line); }
  .rbar b { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; }
  .rbar small { color: var(--text2); font: 500 12.5px/1 var(--mono); }
  .rclose { width: 40px; height: 40px; border-radius: 50%; background: var(--high); color: var(--text); }
  /* La plancia (pwa2, 30/10): la barra della master in fondo, col campo accanto; il pannello a destra con la conversazione. */
  .deskwrap { height: 100%; display: flex; min-height: 0; }
  /* Esiti chiuso in fondo a Tocca a te (J2): come i titoli dei gruppi; il tocco apre il foglio degli esiti. */
  /* Le stesse misure dei titoli dei gruppi (.sec della home: 18/700, icona a 8 px, numero 15/600; web-tocca-apribile-06-11):
     icona allineata a 13 px e stesso passo fra le righe. */
  /* La striscia (app-colonne-strette-05-11): la X in alto, il pallino, il nome in verticale dal basso in alto. */
  .cstrip { height: 100%; display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 8px 0; cursor: pointer; background: var(--bg); min-height: 0; }
  .cstrip .sx { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 16px; color: var(--text2); }
  .cstrip .sname { writing-mode: vertical-rl; transform: rotate(180deg); white-space: nowrap; overflow: hidden; min-height: 0; font: 600 14px var(--display); color: var(--text); }
  /* L'anteprima a fantasma (landingMorph.ts; scelta A, app-trascina-rifinitura-04-11): lo sfondo appena più scuro e la sagoma
     gialla traslucida del posto d'arrivo, con l'etichetta piccola dentro, in alto. */
  :global(.master-ghost-dim) { position: fixed; inset: 0; z-index: 70; pointer-events: none; background: rgb(0 0 0 / .22); }
  :global(.master-ghost) { position: fixed; z-index: 71; pointer-events: none; box-sizing: border-box; border-radius: 14px; background: color-mix(in srgb, var(--wait) 16%, transparent); border: 2px solid color-mix(in srgb, var(--wait) 70%, transparent); }
  :global(.master-ghost > span) { position: absolute; left: 50%; top: 10px; translate: -50% 0; max-width: calc(100% - 16px); overflow: hidden; text-overflow: clip; white-space: nowrap; padding: 3px 10px; border-radius: 10px; background: var(--s2); color: var(--tx); font: 600 13px var(--display); }
  /* Le altre destinazioni (app-trascina-destinazioni-04-11): contorno tratteggiato giallo leggero, senza riempimento, con l'etichetta
     piccola; la linea sottile a metà schermo, accesa quando il dito l'ha passata. */
  :global(.master-ghost-hint) { position: fixed; z-index: 71; pointer-events: none; box-sizing: border-box; border-radius: 14px; border: 2px dashed color-mix(in srgb, var(--wait) 30%, transparent); }
  :global(.master-ghost-hint > span) { position: absolute; left: 50%; top: 10px; translate: -50% 0; max-width: calc(100% - 16px); overflow: hidden; white-space: nowrap; padding: 3px 10px; border-radius: 10px; background: var(--s2); color: var(--tx2); font: 600 13px var(--display); }
  :global(.master-ghost-mid) { position: fixed; z-index: 72; top: 0; bottom: 0; width: 0; margin-left: -1.5px; pointer-events: none; border-left: 3px dashed color-mix(in srgb, var(--wait) 55%, transparent); transition: border-color .12s; }
  :global(.master-ghost-mid.crossed) { border-left-style: solid; border-color: var(--wait); filter: drop-shadow(0 0 6px color-mix(in srgb, var(--wait) 60%, transparent)); }
  /* La pillola sulla linea, all'altezza del dito: «Oltre questa linea: a sinistra ←», passata «Rilascia: a sinistra». */
  :global(.master-ghost-mid > b) { position: absolute; left: 0; translate: -50% -50%; white-space: nowrap; padding: 6px 12px; border-radius: 14px; background: var(--s2); border: 1px solid color-mix(in srgb, var(--wait) 55%, transparent); color: var(--tx); font: 600 14px var(--display); box-shadow: 0 6px 18px rgb(0 0 0 / .45); }
  :global(.master-ghost-mid.crossed > b) { background: var(--wait); border-color: var(--wait); color: #1a1300; }
  @media (prefers-reduced-motion: reduce) { :global(.master-ghost-mid) { transition: none; } }
  /* Il bordo della master si trascina senza transizioni sotto: le colonne seguono il dito subito. */
  .deskwrap.left { flex-direction: row-reverse; }
  .dstage { flex: 1; min-width: 0; display: flex; flex-direction: column; min-height: 0; }
  .dstage > :global(:first-child) { flex: 1; min-height: 0; }
  .dpanel { flex: none; width: 420px; display: flex; flex-direction: column; min-height: 0; background: var(--s1); border-left: 1px solid var(--line); }
  .dpanel.wide { width: 720px; }
  .deskwrap.left .dpanel { border-left: 0; border-right: 1px solid var(--line); }
  /* La corsia, una per tutte le posizioni della master come nell'app (app-tablet-barre-bordi-05-11): fra le sessioni e il
     pannello 28 px con la linea a metà; la presa della larghezza è tutta la corsia. Nella pila la corsia è quella della plancia. */
  .deskwrap > .dpanel { border-left: 0; border-right: 0; margin-left: 28px; }
  .deskwrap.left > .dpanel { margin-left: 0; margin-right: 28px; }
  .deskwrap > .dpanel.stackcol { margin: 0; }
  /* I bordi della master (app-master-trascina-03-11): quello verso le sessioni allarga e stringe, quello in alto cambia l'altezza. */
  .dpanel { position: relative; }
  .wgrip { position: absolute; z-index: 5; top: 0; bottom: 0; left: -28px; width: 28px; cursor: ew-resize; touch-action: none; }
  .deskwrap.left .wgrip, .wgrip.onleft { left: auto; right: -28px; }
  .wgrip::before { content: ''; position: absolute; left: 13.5px; top: 0; bottom: 0; width: 1px; background: var(--line); }
  .stackcol > .wgrip::before { display: none; }
  .wgrip::after { content: ''; position: absolute; left: 12px; top: 50%; width: 4px; height: 48px; margin-top: -24px; border-radius: 2px; background: var(--tx3); opacity: .55; transition: opacity .15s, height .15s, margin-top .15s; }
  .wgrip:hover::after, .wgrip:active::after { opacity: 1; height: 72px; margin-top: -36px; background: var(--tx2); }
  .hgrip { position: absolute; z-index: 5; top: 0; left: 30%; right: 30%; height: 12px; display: grid; place-items: center; cursor: ns-resize; touch-action: none; }
  .hgrip i { width: 48px; height: 4px; border-radius: 2px; background: var(--tx3); opacity: .55; transition: opacity .15s, width .15s; }
  .hgrip:hover i, .hgrip:active i { opacity: 1; width: 72px; background: var(--tx2); }
  /* ‹ ━ ›: la presa in alto e la maniglia della pila cambiano anche lato, di traverso (come TopGripMark nell'app). */
  .hgrip.top, .hstack .grip { display: flex; align-items: center; justify-content: center; gap: 5px; cursor: move; }
  .hgrip.top::before, .hgrip.top::after, .hstack .grip::before, .hstack .grip::after { content: ''; width: 5px; height: 5px; border-left: 1.5px solid var(--tx3); border-bottom: 1.5px solid var(--tx3); opacity: .8; }
  .hgrip.top::before, .hstack .grip::before { transform: rotate(45deg); }
  .hgrip.top::after, .hstack .grip::after { transform: rotate(-135deg); }
  .dpanel.under .wgrip, .dpanel.under .hgrip { display: none; }
  .hstack { height: 100%; display: flex; flex-direction: column; min-height: 0; }
  .hs-top { flex: 1; min-height: 0; overflow: auto; }
  /* La stessa sfumatura sopra la barra della master al telefono (app-sfumatura-master-home-06-11, Franz 06/11 12:52). */
  .list :global(section.home.fade-end), .hs-top :global(section.home.fade-end) { -webkit-mask-image: linear-gradient(to bottom, #000 calc(100% - 32px), transparent); mask-image: linear-gradient(to bottom, #000 calc(100% - 32px), transparent); }
  /* Con home e master nella colonna della master, la griglia delle sessioni non tiene più il posto della home. */
  /* La maniglia della plancia resta: è la corsia fra le sessioni e la colonna della pila, col + e il filo delle schede. */
  .deskwrap.stacked :global(.desk) { grid-template-columns: 0 28px minmax(0, 1fr); }
  .deskwrap.stacked :global(.desk.right) { grid-template-columns: minmax(0, 1fr) 28px 0; }
  .deskwrap.stacked :global(.desk.details) { grid-template-columns: 0 28px minmax(0, 1fr) 341px; }
  .deskwrap.stacked :global(.desk.right.details) { grid-template-columns: 341px minmax(0, 1fr) 28px 0; }
  .deskwrap.stacked :global(.desk > .home) { display: none; }
  .dpanel.stackcol { background: var(--bg); }
  .hstack[data-h='compact'] > .dpanel.under { flex: none; }
  .hstack .grip { flex: none; height: 14px; margin: -8px 0 -6px; display: grid; place-items: center; cursor: ns-resize; touch-action: none; }
  .hstack .grip i { width: 48px; height: 4px; border-radius: 2px; background: var(--tx3); opacity: .55; transition: opacity .15s, width .15s; }
  .hstack .grip:hover i, .hstack .grip:active i { opacity: 1; width: 72px; background: var(--tx2); }
  /* Col dito le prese sottili si toccano su 28 px, il segno resta quello (app-tablet-barre-bordi-05-11). */
  @media (pointer: coarse) {
    .hgrip { height: 28px; }
    .mbar .bargrip { top: -14px; }
    .hstack .grip { height: 28px; margin: -15px 0 -13px; }
  }
  .dpanel.under.compact .mchat :global(section.chat > :not(.bar)) { display: none; }
  .dpanel.under.compact .mchat { flex: none; }
  /* Compatta: il testo della master si legge tutto scorrendo con la rotella, nelle due righe che occupa (Franz 03/11 22:01). */
  .dpanel.under.compact :global(.dock .col > .hl) { display: block; -webkit-line-clamp: unset; line-clamp: none; max-height: 2.9em; overflow-y: auto; overscroll-behavior: contain; scrollbar-width: thin; }
  /* A metà la riga di modello, modifiche e contesto è chiusa; si apre col tasto a fisarmonica (Franz 03/11 17:44). */
  .dpanel.under.nohead .mchat :global(section.chat > header) { display: none; }
  .headkey { display: flex; align-items: center; gap: 6px; margin: 0 12px; padding: 4px 8px; align-self: flex-start; border-radius: 12px; color: var(--text2); font: 400 12.5px var(--mono); }
  .headkey:hover { background: var(--s2); }
  /* Uno stacco netto fra home e master nella stessa colonna (app-master-tasti-compatti-03-11): un vuoto e la master come scheda. */
  .hstack { gap: 12px; background: var(--bg); }
  .dpanel.under { flex: 1; width: auto; border: 1px solid var(--line); border-radius: 18px 18px 0 0; box-shadow: 0 -8px 24px rgb(0 0 0 / .4); }
  .mbar { flex: none; display: flex; align-items: center; gap: 12px; padding: 5px 12px; border-top: 1px solid var(--line); background: var(--s1); }
  .mbar { position: relative; }
  /* Due maniglie sul bordo superiore della riga, ognuna sull'asse della colonna dove la master atterra da quel lato
     (app-master-riga-maniglie-04-11): a metà colonna a sinistra, a W meno metà colonna a destra. */
  .mbar .bargrip { top: -12px; height: 20px; left: auto; right: auto; width: 96px; translate: -50% 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px; }
  /* ⌃ sopra la barretta: tirata su, la master sale in colonna da quel lato (web-maniglia-riga-colonna-06-11, come le ‹ › della presa in alto). */
  .mbar .bargrip::before { content: ''; width: 6px; height: 6px; margin-top: 3px; border-left: 1.5px solid var(--tx3); border-top: 1.5px solid var(--tx3); transform: rotate(45deg); opacity: .8; }
  .mbar .bargrip.r { translate: 50% 0; }
  /* Il campo centrato in altezza con il testo della master e la freccia (Franz 03/11 18:01): niente margine in basso qui. */
  .mbar .mquick { padding: 0; align-self: center; }
  .mbar .mquick :global(.bar) { margin-top: 0; margin-bottom: 0; }
  /* Più spazio alla risposta che al campo (Franz 04/11 11:11, app-master-riga-risposta-04-11): la risposta prende almeno il 60 %
     e va su due righe; il campo resta stretto e si allarga quando ci si scrive (fuoco o testo dentro), poi torna stretto. */
  .mbar > :global(.dock) { flex: 1 1 0; min-width: 60%; border-radius: 0; padding: 6px 4px 6px 8px; transition: min-width 200ms cubic-bezier(0.23, 1, 0.32, 1); }
  .mbar > .mquick { flex: 0 1 clamp(240px, 26%, 380px); transition: flex-basis 200ms cubic-bezier(0.23, 1, 0.32, 1); }
  .mbar:has(> .mquick:focus-within, > .mquick :global(textarea:not(:placeholder-shown))) > .mquick { flex: 1 1 60%; }
  .mbar:has(> .mquick:focus-within, > .mquick :global(textarea:not(:placeholder-shown))) > :global(.dock) { min-width: 30%; }
  @media (prefers-reduced-motion: reduce) { .mbar > .mquick, .mbar > :global(.dock) { transition: none; } }
  /* La barra della plancia (pwa2): il campo è una pillola sola, senza il promemoria dei tasti. */
  .mbar :global(.bar) { padding: 0; }
  .mbar :global(.keys), .mchat :global(.keys) { display: none; }
  .mbar :global(.field.master) { min-height: 52px; background: var(--s2); }
  .mup { flex: none; width: 44px; height: 44px; border-radius: 22px; display: grid; place-items: center; background: var(--s3); color: var(--tx); }
  .mquick { flex: 1; min-width: 0; }
  .mchat { flex: 1; min-height: 0; display: flex; flex-direction: column; }
  .wslot { display: contents; }
</style>
