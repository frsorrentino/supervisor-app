<script lang="ts">
  import { lang, locale } from './lang'
  import Kpi6 from './Kpi6.svelte'
  import Kpi11 from './Kpi11.svelte'
  import Kpi18 from './Kpi18.svelte'
  import Kpi10 from './Kpi10.svelte'
  import Kpi12 from './Kpi12.svelte'
  import Kpi13 from './Kpi13.svelte'
  import Kpi17 from './Kpi17.svelte'
  import Kpi7 from './Kpi7.svelte'
  import Kpi4 from './Kpi4.svelte'
  import PluginsSection from './PluginsSection.svelte'
  import type { KpiData, PluginsPage } from './contract'
  import { day as kpiDay, dayFigures, HANDOFF, head as kpiHead, hourly, midnight, outcomes as kpiOutcomes, split as kpiSplit, todayShare, turnsOf, waitingSince } from './kpi'
  import type { TimelinePage } from './contract'
  import { actionOf, consumersLine, isOpen as sysIsOpen, num as sysNum, summaryLine, type Manual } from './system'
  import PermGlyph from './PermGlyph.svelte'
  import { homeGlyph } from './permissionModes'
  import { tick, untrack } from 'svelte'
  import { flip } from 'svelte/animate'
  import type { Approval, Commitment, Session, State, DonePage } from './contract'
  import { cleanupOf } from './masterService'
  import { details as lavoriDetails, doneToday as lavoriDoneToday, forYou as lavoriForYou, inProgress as lavoriProgress, place as lavoriPlace, shortTitle, type ForYou as LavoriRow } from './lavori'
  import { askedFor, attachedTasks, build, MASTER, staleHours, type Group, type Row } from './summary'
  import { doneOf, type Done } from './queue'
  import { expiry } from './durations'
  import { groups as deskGroups } from './tablet'
  import { summary as outcomeSummary } from './outcome'
  import { blockingOf, parseSteps } from './nextSteps'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import { choices } from './approvals'
  import ElicitationCard from './ElicitationCard.svelte'
  import type { Elicitation } from './contract'
  import type { Values } from './elicit'
  import WorkBadge from './WorkBadge.svelte'
  import ApproveBody from './ApproveBody.svelte'
  import Options from './Options.svelte'
  import type { RecapAction } from './recapActions'
  import { doIt, mark, todayIso, type Edit } from './recapAgenda'
  import { readable } from './slashText'
  import { forYou, type ForYouRow, type Scheduled } from './masterHome'
  import { count as outsideCount, groups as outsideGroups, place as placeOutside, tonight as tonightOf, type Category } from './outside'
  import type { Event } from './contract'
  import StateGlyph from './StateGlyph.svelte'
  import { DEFAULT_ORDER, normalize as normalizeOrder } from './homeOrder'
  import { glyphOf, span as spanOf, type GlyphKind } from './rail'
  import { badge as badgeOf, GREY, personal } from './badge'

  // La home del telefono (SummaryList.kt): ogni sessione una card nei gruppi del bisogno; il tocco apre la sessione, ▼ la
  // apre sul posto. La domanda ha le opzioni subito, chi ha finito i consigli come tasti, sotto la barretta del contesto.
  import type { Snippet } from 'svelte'
  import { candidates as finishedOf, closable as closableOf, type Preview } from './closeFinished'
  import type { NightPage } from './night'
  let { st, selected, onPick, onAnswer, onStep, withMaster = false, footer, onApprove = () => {}, onDone = () => {}, onClose = () => {}, onSessionCmd = () => {}, night, nightTitle = '', onNight = () => {}, usage, justClosed = null, closing = null, elicitations = [], onElicit = () => {}, agenda = null, onLavori = () => {}, onEdit = () => {}, onStart = () => {}, onDoneForPerson = () => {}, showLavori = true, showUsage = true, usageFocus = 0, onUsageFocused = () => {}, onSpeak = null, donePages = {}, events = [], sent = [], read = new Set<string>(), now = st.ts, onService = () => {}, onReopen = null, lastNight = null, suggestions, onClosePreview = null, onCloseMany = () => {}, onSystem = null, kpiPage = null, kpiData = null, onBox = () => {}, onQuota = () => {}, part = null, afterSessions, pluginsLoad = null, pluginsRun = null, suggestionsN = 0, suggestionsTotal, usageBlock }: {
    /** Il blocco Utilizzo aperto, scelta D (30/10 20:11); le colonne della plancia tengono le schede (`usage`). */
    usageBlock?: Snippet
    /** La riga della quota nel tabellone apre il quadro (stile «Studio e Diretta», 30/10: era nella testata). */
    onQuota?: () => void
    /** Una colonna della plancia (pwa2, Franz 30/10): «you» le attese e i suggerimenti, «work» sessioni e lavori, «out» gli
     *  esiti, «tools» Altro; senza, la home intera col tabellone. */
    part?: 'you' | 'work' | 'out' | 'tools' | null
    /** Nella colonna «Attività e code» della plancia: i compiti in corso del registro, subito dopo le sessioni Claude. */
    afterSessions?: Snippet
    /** Contratto 1.84 (B4): l'anteprima di cosa si salva chiudendo queste sessioni, poi la chiusura; null = il relay non le ha. */
    onClosePreview?: ((names: string[]) => Promise<Preview | null>) | null; onCloseMany?: (names: string[]) => void
    /** Contratto 1.90: il tasto di un consiglio del Sistema; null = sola lettura (demo, relay senza l'op). */
    onSystem?: ((op: string, arg: string) => void) | null
    /** I KPI 6, 11 e 18 (compito app-kpi-medie-25-10): la cronologia di oggi di tutte le sessioni; null finché non c'è. */
    kpiPage?: TimelinePage | null
    /** Contratto 1.96: le cifre dell'op `kpi` (proposte 10, 12, 13, 17, compito app-kpi-dati-relay-25-10); null finché non arrivano. */
    kpiData?: KpiData | null
    /** I tre box in alto aprono la loro pagina (app-box-cliccabili-29-10). */
    onBox?: (b: import('./HomeBox.svelte').Box) => void
    /** Contratto 1.95: l'elenco dei plugin e i comandi della sezione «Plugin e skill»; null = il relay non ha le op. */
    pluginsLoad?: (() => Promise<PluginsPage | null>) | null
    pluginsRun?: ((op: 'plugin_check' | 'plugin_update' | 'plugin_update_all', arg: string | null) => Promise<{ ok: boolean; text: string } | null>) | null
    /** Contratto 1.84 (scheda A): la scheda dei suggerimenti della master, dopo chi ti aspetta; assente nel cruscotto.
     *  `suggestionsN`: quanti ne mostra, perché «Ti aspettano» compaia anche quando ci sono solo loro. */
    suggestions?: Snippet; suggestionsN?: number; suggestionsTotal?: number
    /** «Fuori dalle sessioni» (OutsideSessions.kt): le righe di servizio vengono da «Per te», con quello che si è letto e mandato. */
    donePages?: Record<string, DonePage>; events?: Event[]; sent?: Scheduled[]; read?: Set<string>; now?: number; onService?: (r: ForYouRow) => void
    /** «Chiuse»: l'elenco con «Riapri» (contratto 1.9), se il relay lo sa fare. */
    onReopen?: ((name: string) => void) | null
    /** L'ultima notte in fondo alla scheda Stanotte, dal rapporto. */
    lastNight?: NightPage | null
    /** `selected`: le sessioni aperte, evidenziate; `withMaster`: la master nella lista come le altre (la plancia). */
    st: State; selected: string[]; onPick: (name: string) => void; withMaster?: boolean
    /** In fondo alla lista: sulla plancia i pannelli della quota (footer di SummaryList). */
    footer?: Snippet
    onAnswer: (session: string, n: number) => void; onStep: (session: string, text: string) => void
    /** Contratto 1.37: l'ok a un compito (`approve`) e «Chiudi» per una sessione finita o doppione (/exit). */
    onApprove?: (task: string, note: string, choice?: string) => void; onClose?: (name: string) => void
    /** Contratto 1.80: il tasto «Fatto» di una ferma (op `attention_done`). */
    onDone?: (d: Done) => void
    /** Il menu contestuale della card (Franz, 14/10 20:58): segui, fai controllare alla master, come il ⋮ della sessione. */
    onSessionCmd?: (name: string, op: 'follow' | 'unfollow' | 'prompt', arg?: string) => void
    /** Il riquadro Notte (mockup approvato l'08/10 alle 12:57): `undefined` = nascosto, null = rapporto in arrivo. */
    night?: NightPage | null | undefined; nightTitle?: string; onNight?: () => void
    /** «Utilizzo»: i pannelli della quota in una sezione richiudibile. */
    usage?: Snippet
    /** La sessione appena chiusa dal pannello di chiusura (Franz, 08/10 18:59): la sua card in cima alle sessioni è dove la
     *  pagina atterra tornando alla home (`view-transition-name: page-fly`), poi si richiude da sola. */
    justClosed?: { s: Session; line: string } | null
    /** La sessione chiusa da una card, finché non esce dallo stato o il PC rifiuta (Franz 24/10 13:20: «nessun feedback»). */
    closing?: string | null
    /** Contratto 1.83: le domande aperte dei server MCP, in cima; `onElicit` manda la risposta. */
    elicitations?: Elicitation[]; onElicit?: (e: Elicitation, action: 'accept' | 'decline', values: Values) => void
    /** La sezione «Lavori» (22/10): l'agenda (null = non arrivata), «Tutti i lavori» apre la pagina; Fatto su una riga dell'agenda
     *  (`agenda_set`), Avvia (il «Fallo» dell'agenda alla sua sessione), Fatto su una pausa che aspetta te (la master riprende). */
    agenda?: import('./contract').AgendaPage | null; onLavori?: () => void; onEdit?: (e: Edit) => void
    onStart?: (a: RecapAction) => void; onDoneForPerson?: (c: Commitment) => void
    /** Sulla plancia senza colonne Recap e Utilizzo stanno nel cruscotto (variante B, 09/10): qui non si ripetono. */
    showLavori?: boolean; showUsage?: boolean
    /** «Utilizzo e limiti» dal menu e dalla riga della quota: a ogni nuovo valore la sezione si apre, si vede e si illumina. */
    usageFocus?: number; onUsageFocused?: () => void
    /** ▶ sul dettaglio di «Approfondisci» (09/10 16:31). */
    onSpeak?: ((text: string) => void) | null
  } = $props()
  const today = todayIso()
  const order = $derived(part ? DEFAULT_ORDER : normalizeOrder(st.home_order))
  let usageEl: HTMLElement | undefined = $state()
  let usageFlash = $state(false)
  $effect(() => {
    if (!usageFocus || !usage || !showUsage) return
    // Il salto si consuma: una home ricreata (ritorno da una sessione) non lo rifà (Franz, 09/10 18:27).
    untrack(() => {
      onUsageFocused()
      if (!open.usage) toggleSec('usage')
      tick().then(() => { usageEl?.scrollIntoView({ block: 'start', behavior: 'smooth' }); usageFlash = true; setTimeout(() => (usageFlash = false), 1200) })
    })
  })
  // Sezioni richiudibili (Franz, 08/10 12:30): restano come le hai lasciate, anche alla prossima apertura.
  // Chiave nuova col riordino (app-home-riordino-29-10): Notte e Utilizzo partono chiuse col numero in vista, anche per chi
  // le aveva aperte; sessioni, lavori ed esiti aperti.
  const SEC = 'home-sections-2'
  const SEC_START = { suggestions: true, sessions: true, lavori: true, esiti: true, usage: false, night: false }
  let open = $state<Record<string, boolean>>((() => { try { return { ...SEC_START, ...JSON.parse(localStorage.getItem(SEC) ?? '{}') } } catch { return { ...SEC_START } } })())
  function toggleSec(k: string) { open = { ...open, [k]: !open[k] }; try { localStorage.setItem(SEC, JSON.stringify(open)) } catch { /* senza memoria resta per questa visita */ } }
  // La sezione «Lavori» (mockup approvato il 22/10): al posto di Da approvare, Impegni e Recap, solo le righe con un tasto
  // (gli ok, le prime due cose da fare fuori dalla chat, la prima che può fare Claude) e la riga «Tutti i lavori».
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
  const fy = $derived(st.commitments != null || agenda ? lavoriForYou(st, agenda, now, today) : null)
  const lavoriLine = $derived.by(() => {
    const ip = lavoriProgress(st, agenda, now, zone, today)
    const doneToday = lavoriDoneToday(donePages, events, zone, today)
    return t.lavoriHomeLine(ip.total, st.night.queued, doneToday)
  })
  function fySub(r: LavoriRow): string | null {
    if (r.kind === 'ok') return r.who && r.at ? t.lavoriAsks(r.who, hhmm(r.at)) : r.who ? t.lavoriAsksWho(r.who) : t.lavoriWaitsOk
    if (r.commitment) return r.who ? t.lavoriUnblocks(r.who) : null
    return r.who?.trim() || null
  }
  function fyOutside(r: LavoriRow) { if (r.agenda) onEdit({ item: 'done', row: r.agenda }); else if (r.commitment) onDoneForPerson(r.commitment) }
  const hhmm = (s: number) => new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s * 1000))
  // Come la home dell'app: chi ha finito resta finché non lo apri o non gli scrivi (Summary.kt con sent e read).
  const model = $derived(build(st, sent, now, read))
  const outside = $derived(outsideGroups(forYou(st, events, sent, now, { read, limit: Infinity }).rows.filter(r => r.kind !== 'question' && r.kind !== 'finished'), model.closed))
  // Franz 24/10 00:46: un suggerimento che riguarda una sessione aperta (il contesto alto) sta nella sua scheda; in Altro restano gli altri.
  const placedOut = $derived(placeOutside(outside, new Set(model.rows.map(r => r.session.name))))
  // Da riprendere, Stanotte, Da sistemare e Programmati sono lavori: stanno in «Lavori»; le chiuse hanno la loro riga.
  const workGroups = $derived(placedOut.groups.filter(g => g.category !== 'closed'))
  // Franz 24/10 00:44: ogni voce di Lavori è una scheda della famiglia; il chevron apre i dettagli (lavoriDetails).
  let openWork = $state<string | null>(null)
  const workKey = (r: LavoriRow, i: number) => r.approval?.task ?? r.commitment?.task ?? (r.agenda?.key || null) ?? `${r.kind}-${i}`
  // Franz 24/10 09:22: per un ok «chi chiede» sta nella riga in fondo; in testa la meta resta corta.
  const fyMeta = (r: LavoriRow): string => r.agenda?.scope?.trim() || r.commitment?.session || ''
  const tonightCard = $derived(tonightOf(st.night, now))
  let closedDlg: HTMLDialogElement | undefined = $state()
  const catLabel = (c: Category) => ({ fix: t.catFix, scheduled: t.catScheduled, resume: t.catResume, night: t.catNight, closed: t.catClosed })[c]
  const CAT_ICON: Record<Category, string> = {
    fix: 'M12 4l9 16H3zM12 10v4M12 17v.5', scheduled: 'M12 7v5l3 2M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z', resume: 'M4 12a8 8 0 1 0 2.3-5.6M4 4v4h4',
    night: 'M20 15A8 8 0 0 1 9 4a8 8 0 1 0 11 11z', closed: 'M4 5h16v4H4zM5 9h14v10H5zM10 13h4',
  }
  function serviceText(r: ForYouRow): { title: string; detail: string | null; action: string } {
    const first = (x?: string | null) => x?.split('\n').find(l => l.trim()) ?? null
    switch (r.kind) {
      case 'context': return { title: t.fyContext(r.title, r.number ?? 0), detail: t.fyContextDetail, action: t.fyHandoff }
      case 'night_report': return { title: r.title, detail: first(r.detail), action: t.listen }
      case 'night': return { title: (r.number ?? 0) > 0 ? t.fyNightQueued(r.number!) : t.fyNightEmptyShort, detail: t.fyNightDetail, action: t.fyAdd }
      // Il nome del progetto basta: «prossimo passo» lo dice già l'etichetta della categoria.
      case 'next_step': return { title: r.title, detail: r.detail ?? null, action: t.fyStart }
      case 'scheduled': return { title: t.fyScheduled(r.number ?? 1), detail: r.at ? t.fyScheduledDetail(hhmm(r.at)) : null, action: t.fySee }
      default: return { title: r.title, detail: r.detail ?? null, action: t.fySee }
    }
  }
  let expanded = $state<string | null>(null)
  const groups = $derived(withMaster ? deskGroups(model) : (['waiting', 'finished', 'working', 'still'] as Group[]).map(g => [g, model.rows.filter(r => r.group === g)] as [Group, Row[]]).filter(([, rows]) => rows.length > 0))
  // Il riordino (Franz 29/10 20:48, app-home-riordino-29-10): chi aspetta te in cima, in «Ti aspettano»; le altre in «Sessioni».
  const waitRows = $derived(groups.find(([g]) => g === 'waiting')?.[1] ?? [])
  const restGroups = $derived(groups.filter(([g]) => g !== 'waiting'))
  // Al massimo un bottone pieno: «Fatto» della prima che ti aspetta, se la richiesta non si chiude da sola (correzione 2 del
  // 30/10); senza nessuna, il primo ok di un compito. Le risposte consigliate hanno il bordo chiaro, mai il pieno.
  const primary = $derived(waitRows[0]?.session.name ?? null)
  // Gli esiti di oggi dalla cronologia (null finché non arriva) e le sessioni vive, che una riga può aprire.
  const out = $derived(kpiPage ? kpiOutcomes(kpiPage, now) : null)
  const liveNames = $derived(new Set(st.sessions.filter(s => s.state !== 'gone').map(s => s.name)))
  // Lo stile «Studio e Diretta» (30/10): a sinistra di ogni riga il glifo e, sotto, da quanto aspetta o lavora (o l'ora
  // dell'esito, o dell'ultima attività); sulla riga di una sessione l'ora del suo ultimo rosso di oggi.
  const lastRed = $derived(new Map((out?.red ?? []).slice().sort((a, b) => a.at - b.at).map(d => [d.session, d.at])))
  function railOf(r: Row): { kind: GlyphKind; text: string | null } {
    const s = r.session
    const kind = glyphOf(s, r.group)
    const w = waitingSince(s)
    const text = kind === 'waiting' && w != null ? t.railSpan(spanOf(w, now))
      : kind === 'busy' && s.turn_started != null ? t.railSpan(spanOf(s.turn_started, now))
      : kind === 'done' && s.outcome?.at ? hhmm(s.outcome.at)
      : kind === 'still' ? hhmm(s.since) : null
    return { kind, text }
  }
  // Il colore che Franz dà alla sessione resta, piccolo, accanto al nome: il glifo dice lo stato, il segno dice chi è.
  const idColor = (s: Session) => { const f = badgeOf(s.account, s.color, s.state, s.icon, s.account_kind).fill; return f === GREY ? null : f }
  const square = (s: Session) => !personal(s.account, s.account_kind)
  const sessionsLine = $derived.by(() => {
    const n = (g: Group) => restGroups.find(([x]) => x === g)?.[1].length ?? 0
    return t.homeSessionsLine(n('working'), n('finished'), n('still'))
  })
  // Chiusa, «Utilizzo» dice la settimana di ogni account e i token di oggi; la quota 5h sta già nella barra in cima.
  const usageLine = $derived.by(() => {
    const weeks = Object.entries(st.quota).filter(([, q]) => !q.stale && q.w7 != null).map(([a, q]) => `${a.length > 5 ? `${a.slice(0, 4)}.` : a} ${q.w7}%`)
    const tok = kpiPage ? dayFigures(kpiPage, st, now).tokens : 0
    return [weeks.length ? `${t.weekShort} ${weeks.join(' · ')}` : null, tok ? t.tokensShort(tok >= 1e6 ? `${Math.round(tok / 1e6)} M` : `${Math.round(tok / 1e3)} k`) : null].filter(Boolean).join(' · ') || null
  })
  // Chiusa, «Notte» dice com'è andata l'ultima notte.
  const nightLine = $derived(night ? [night.counts.done && t.nightDone(night.counts.done), night.counts.running && t.nightRunningN(night.counts.running), night.counts.partial && t.nightPartial(night.counts.partial), night.counts.stopped && t.nightStopped(night.counts.stopped), night.counts.asking && t.nightAsking(night.counts.asking)].filter(Boolean).join(' · ') || null : null)
  // Con un ok da dare le opzioni delle domande restano tonali, così sulla schermata c'è un solo bottone pieno.
  // Franz 23/10 19:34: gli ok che una sessione aperta chiede stanno nella sua card, non in schede a parte.
  const attached = $derived(attachedTasks(model.rows))
  // Strada B (Franz 23/10 23:08, mockup del 23/10): un lavoro di una sessione aperta sta nella sua scheda, come gli ok;
  // in Lavori restano quelli senza sessione, in una lista compatta (titolo breve, riferimento in mono, tasto piccolo).
  const placed = $derived(fy ? lavoriPlace(fy, new Set(model.rows.map(r => r.session.name)), attached) : null)
  const looseOk = $derived(placed?.loose.ok ?? [])
  // Franz 24/10 19:46 (B1): in alto solo gli ok che fermano un lavoro; le cose fuori dalla chat e da avviare in «Più tardi».
  let laterOpen = $state(false)
  const laterRows = $derived(placed ? [...placed.loose.outside, ...placed.loose.claude] : [])
  const fyShort = (r: LavoriRow) => shortTitle(r.title, r.agenda?.short ?? r.commitment?.short)
  function fyAct(r: LavoriRow) { if (r.kind === 'outside') fyOutside(r); else if (r.kind === 'claude' && r.agenda) onStart(doIt(r.agenda)) }
  const canExit = $derived(!!st.slash?.includes('exit'))
  let pending = $state<Approval | null>(null)
  let pendingChoice = $state<string | null>(null)
  let note = $state('')
  let approveDlg: HTMLDialogElement | undefined = $state()
  // Contratto 1.84 (B4, Franz 24/10 19:46): «Chiudi le finite» chiede prima al PC cosa salva di ognuna e cosa ne impedisce
  // la chiusura; la conferma lo elenca, avvisa dei file non committati e chiude solo quelle che si possono chiudere.
  const finished = $derived(onClosePreview ? finishedOf(model.rows, now) : [])
  let closeDlg: HTMLDialogElement | undefined = $state()
  let preview = $state<Preview | null>(null)
  let previewing = $state(false)
  async function askCloseFinished() {
    if (!onClosePreview) return
    previewing = true; preview = null; closeDlg?.showModal()
    preview = await onClosePreview(finished.map(s => s.name))
    previewing = false
  }
  let confirming = $state<string | null>(null)
  // Contratto 1.90, la sezione «Sistema» (Franz 28/10 13:19): compatta in verde, aperta da sola in giallo e in rosso;
  // il tocco sull'intestazione vale finché il livello non cambia. Ogni tasto chiede conferma nella card, come «Chiudi».
  const sys = $derived(st.system ?? null)
  // I KPI coi dati già nel relay: per ogni sessione viva i suoi eventi di oggi (la voce viva prima di una chiusa omonima).
  const kpiEvents = $derived(new Map((kpiPage?.sessions ?? []).slice().sort((a, b) => Number(a.live) - Number(b.live)).map(x => [x.session, x.events])))
  const dayStart = $derived(midnight(now))
  let sysManual = $state<Manual>(null)
  const sysOpen = $derived(sys ? sysIsOpen(sys.level, sysManual) : false)
  let sysAsk = $state<string | null>(null)
  let sysSent = $state(new Set<string>())
  const SYS_TONE = { green: 'var(--good)', yellow: 'var(--b-warn)', red: 'var(--b-alert)' } as const
  // Il menu contestuale della sessione sulla card (Franz, 14/10 20:58: «il menu contestuale di una sessione non lo vedo»):
  // tasto destro, o pressione lunga sul telefono; le voci del ⋮ della sessione, «Chiudi» con la conferma nella card.
  let ctx = $state<string | null>(null)
  // Il pannello sta sopra tutto (anche sopra il riquadro della master), vicino al punto toccato e dentro lo schermo.
  let at = $state({ x: 0, y: 0 })
  const MENU_W = 360, MENU_H = 290
  function openCtx(name: string, x: number, y: number) {
    at = { x: Math.max(8, Math.min(x, innerWidth - Math.min(MENU_W, innerWidth - 24) - 8)), y: Math.max(8, Math.min(y, innerHeight - MENU_H - 8)) }
    ctx = name
  }
  let held: ReturnType<typeof setTimeout> | null = null
  let longFired = false
  const HOLD_MS = 500
  function holdStart(name: string, e: PointerEvent) {
    if (e.pointerType === 'mouse') return
    longFired = false
    const x = e.clientX, y = e.clientY
    held = setTimeout(() => { longFired = true; openCtx(name, x, y) }, HOLD_MS)
  }
  function holdEnd() { if (held) clearTimeout(held); held = null }
  const hasMaster = $derived(st.sessions.some(x => x.name === MASTER && x.state !== 'gone'))
  // Il tasto di un passo toccato resta segnato «mandato» finché la sessione non cambia esito (Franz, 06/10 12:32): la card
  // risponde subito, poi la sessione al lavoro la aggiorna.
  let sentSteps = $state(new Set<string>())
  const stepKey = (s: { name: string; outcome?: { at: number } | null }, step: string) => `${s.name}@${s.outcome?.at ?? 0}:${step}`
  // Contratto 1.81: con le opzioni, un tasto per opzione; la finestra conferma quella scelta.
  function askApprove(a: Approval, choice: string | null = null) { pending = a; pendingChoice = choice; note = ''; approveDlg?.showModal() }
  const ctxTone = (p: number) => (p >= 90 ? 'var(--b-alert)' : p >= 75 ? 'var(--b-warn)' : 'var(--b-ring)')
  const expiryText = (x: number) => { const e = expiry(x, now); return e.day === 'today' ? t.expiresAt(e.hm) : e.day === 'tomorrow' ? t.expiresTomorrow(e.hm) : t.expiresOn(e.date ?? '', e.hm) }
  function text(r: Row) {
    const parsed = parseSteps(r.text ?? '')
    const shown = r.group === 'waiting' ? parsed.text : r.session.outcome ? outcomeSummary(r.session.outcome) : parsed.text
    return { body: shown.replace(/\*\*|__|`/g, '').trim(), steps: r.group === 'waiting' ? [] : parsed.steps, blocking: blockingOf(r.session, parsed.blocking) }
  }
</script>

<svelte:window onkeydown={(e) => e.key === 'Escape' && ctx && (ctx = null)} />

<!-- La testata di sezione dello stile «Studio e Diretta» (30/10): l'icona grigia nel binario dei glifi, il titolo allineato ai
     nomi, il numero accanto; a destra il riepilogo e la freccina che apre e chiude. -->
{#snippet sec(key: string, title: string, icon: string, count: string | null = null, testid: string | null = null, right: string | null = null)}
  {#if part}
    <!-- Nelle colonne della plancia le sezioni restano aperte: lo spazio c'è, e ogni colonna ha già il suo argomento. -->
    <div class="sec static" data-testid={testid}><span class="ic"><span class="ms">{icon}</span></span><span class="st">{title}</span>{#if count}<small>{count}</small>{/if}</div>
  {:else}
    <button class="sec" aria-expanded={open[key]} data-testid={testid} onclick={() => toggleSec(key)}>
      <span class="ic"><span class="ms">{icon}</span></span><span class="st">{title}</span>{#if count}<small>{count}</small>{/if}
      <span class="sp"></span>{#if right}<span class="rt">{right}</span>{/if}<span class="ms tog">{open[key] ? 'expand_less' : 'expand_more'}</span>
    </button>
  {/if}
{/snippet}

<section class="home" class:part={!!part}>
  <!-- Contratto 1.83: dopo 120 s la domanda torna al terminale, quindi sta sopra a tutto il resto. -->
  {#if !part || part === 'you'}{#each elicitations as e (e.id)}<ElicitationCard {e} {now} onAnswer={onElicit} />{/each}{/if}
<!-- Proposta 7 (compito app-kpi-piccole-25-10): tre cifre in testa alla home, prima delle sezioni. Il primo box è l'unico
     avviso di chi aspetta: niente più banner giallo né «N per te» qui (app-home-riordino-29-10). -->
{#if !part}<Kpi7 h={kpiHead(st, kpiPage, now)} {st} {now} {onBox} {onQuota} />{/if}
<!-- L'ordine (Franz 29/10 20:48, app-home-riordino-29-10): chi aspetta te, le sessioni, i lavori, gli esiti di oggi; poi
     in «Altro» Utilizzo, Sistema, Plugin e Chiuse col numero in vista. La Notte sta nei Lavori (stile «Studio e Diretta»). -->
{#if (!part || part === 'you') && (waitRows.length || looseOk.length)}
  <div class="sec static" data-testid="home-sez-aspettano"><span class="ic"><span class="ms">pan_tool</span></span><span class="st">{t.homeWaiting}</span><small>{waitRows.length + looseOk.length}</small></div>
  {#each waitRows as r (r.session.name)}<div class="cw" animate:flip={{ duration: 250 }}>{@render card(r, 'waiting')}</div>{/each}
  <!-- Gli ok ai compiti senza una sessione aperta: aspettano te come le domande. -->
  {#each looseOk as r, i (workKey(r, i))}{@render workCard(r, i, !primary && i === 0)}{/each}
{/if}
<!-- Contratto 1.84 (scheda A): i suggerimenti della master dopo le sessioni, fuori da «Ti aspettano» e dal suo
     numero, a filo e non pieni: un consiglio non blocca nessuno (correzione 1 del 30/10). -->
{#if part === 'you' && !waitRows.length && !looseOk.length && !(suggestions && suggestionsN)}<p class="wnote">{t.deskYouEmpty}</p>{/if}
<!-- I suggerimenti come sezione richiudibile (Franz 30/10 17:33), dopo le sessioni (16:44): la barra dei 7 giorni sta dentro.
     Nella plancia restano nella colonna «Richiede te». -->
{#snippet secSuggestions()}
{#if (!part || part === 'you') && suggestions && suggestionsN}
  {@render sec('suggestions', t.sugTitle, 'auto_awesome', String(suggestionsTotal ?? suggestionsN), 'home-sez-suggerimenti')}
  {#if part || open.suggestions}<div class="sugg">{@render suggestions()}</div>{#if kpiData?.kpi_suggestions_7d}<Kpi13 s={kpiData.kpi_suggestions_7d} />{/if}{/if}
{/if}
{/snippet}
{#snippet secSessions()}
{#if (!part || part === 'work') && (restGroups.length || justClosed)}
  {@render sec('sessions', part ? t.deskSessions : t.homeSessions, 'play_circle', String(restGroups.reduce((n, [, rows]) => n + rows.length, 0)), 'home-sez-sessioni', sessionsLine)}
  {#if justClosed}
    <div class="card just" style:view-transition-name="page-fly">
      <div class="jrow"><Badge s={justClosed.s} size={24} /><span class="jcol"><b>{justClosed.s.name}</b><small>{justClosed.line}</small></span></div>
    </div>
  {/if}
  {#if part || open.sessions}
    <!-- Una lista sola, nell'ordine dei gruppi (hanno finito, al lavoro, ferme): il glifo dice lo stato al posto delle
         intestazioni dei gruppi (stile «Studio e Diretta», 30/10). -->
    <div class="list">
      {#each restGroups as [group, rows] (group)}
        {#each rows as r (r.session.name)}<div class="cw" animate:flip={{ duration: 250 }}>{@render card(r, group)}</div>{/each}
      {/each}
    </div>
    {#if finished.length}<div class="after"><button class="tonal mini" onclick={askCloseFinished}>{t.closeFinished(finished.length)}</button></div>{/if}
  {/if}
{/if}
{#if part === 'work' && afterSessions}{@render afterSessions()}{/if}
{/snippet}
{#snippet secLavori()}
{#if (!part || part === 'work') && showLavori && (fy || workGroups.length)}
  {#if !part}{@render sec('lavori', t.lavoriTitle, 'checklist', null, 'home-sez-lavori', open.lavori ? null : lavoriLine)}{/if}
  {#if part || open.lavori}
    {#if laterRows.length && !part}
      <button class="all later" aria-expanded={laterOpen} onclick={() => (laterOpen = !laterOpen)}><span class="wic"></span><span class="otext"><b>{t.lavoriLater(laterRows.length)}</b></span>
        <span class="ms tog">{laterOpen ? 'expand_less' : 'expand_more'}</span></button>
      {#if laterOpen}{#each laterRows as r, i (workKey(r, i))}{@render workCard(r, i, false)}{/each}{/if}
    {/if}
    <!-- Da riprendere, Stanotte, Da sistemare e Programmati, che stavano in «Altro», sono lavori (Franz 29/10 20:32). -->
    {#each workGroups as g (g.category)}
      {#if g.category === 'night'}
        {#each g.rows as r, i (`n-${i}`)}
          {#if r.kind !== 'night'}
            {@const x = serviceText(r)}
            <div class="card wrows"><div class="wrow"><span class="wic"><span class="ms">bedtime</span></span><span class="otext"><b>{x.title}</b>{#if x.detail}<small>{x.detail}</small>{/if}</span><button class="tonal mini" onclick={() => onService(r)}>{x.action}</button></div></div>
          {:else if tonightCard.items.length}
            {@render tonight(r)}
          {:else}
            <!-- La Notte su una riga (stile «Studio e Diretta», 30/10): la coda di stanotte e com'è andata la notte scorsa, che
                 prima stava in una sezione chiusa in fondo; il tocco apre la pagina Notte, «Aggiungi» mette in coda. -->
            <div class="card wrows">
              <div class="wrow orow" role="button" tabindex="0" onclick={onNight} onkeydown={(e) => e.key === 'Enter' && onNight()}><span class="wic"><span class="ms">bedtime</span></span>
                <span class="otext"><b>{t.homeNight}</b><small class="txt">{t.homeNightRow(0, night ? night.counts.done : null, night ? night.cards.length : null)}</small></span>
                <button class="tonal mini" onclick={(e) => { e.stopPropagation(); onService(r) }}>{t.fyAdd}</button></div>
            </div>
          {/if}
        {/each}
      {:else}
        {#if part}<div class="sec static"><span class="ic"><span class="ms">{g.category === 'resume' ? 'replay' : g.category === 'scheduled' ? 'timer' : 'event_list'}</span></span><span class="st">{catLabel(g.category)}</span><small>{outsideCount(g)}</small></div>{:else}
        <h2 class="gh cat" style="--t:{g.category === 'fix' ? 'var(--wait)' : 'var(--text2)'}">
          <svg viewBox="0 0 24 24" width="16" height="16"><path d={CAT_ICON[g.category]} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
          <span>{t.catCount(catLabel(g.category), outsideCount(g))}</span>
          {#if g.category === 'resume'}<small class="note">{t.catResumeNote}</small>{/if}
        </h2>{/if}
        <div class="card wrows">
          {#each g.rows as r, i (`${g.category}-${i}`)}
            {@const x = serviceText(r)}
            <div class="wrow">
              <span class="wic"><svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d={CAT_ICON[g.category]} fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" /></svg></span>
              <span class="otext"><b>{r.kind === 'context' ? r.title : x.title}</b>{#if r.kind === 'context'}<small>{x.title}. {x.detail}</small>{:else if x.detail}<small>{x.detail}</small>{/if}</span>
              <button class="tonal mini" onclick={() => onService(r)}>{x.action}</button>
            </div>
          {/each}
        </div>
      {/if}
    {/each}
    {#if fy && !part}
      <button class="all" onclick={onLavori}><span class="wic"><span class="ms">event_list</span></span><span class="otext"><b>{lavoriLine}</b></span>
        <span class="ms chevr">chevron_right</span></button>
    {/if}
  {/if}
{/if}
{/snippet}
{#snippet secEsiti()}
{#if (!part || part === 'out') && out}
  <!-- Gli esiti di oggi dalla cronologia: i rossi in cima, poi «Tutti gli esiti» apre la pagina del terzo box. -->
  {@render sec('esiti', part && out.red.length ? t.deskRedToday : t.homeOutcomes, 'sports_score', part && out.red.length ? String(out.red.length) : null, 'home-sez-esiti')}
  {#if part || open.esiti}
    <div class="card wrows">
      {#each out.red.length ? out.red.slice(0, 3) : out.done.slice(0, 2) as d, i (i)}
        <button class="wrow orow" onclick={() => (liveNames.has(d.session) ? onPick(d.session) : onBox('esiti'))}>
          <span class="wic rail"><StateGlyph kind={d.ref != null ? 'red' : 'done'} /><b class="rt" class:red={d.ref != null}>{hhmm(d.at)}</b></span>
          <span class="otext"><b>{d.session}</b><small class="txt">{d.text}</small></span>
        </button>
      {:else}
        <p class="wnote">{t.boxOutcomesNone}</p>
      {/each}
      <button class="wrow orow" onclick={() => onBox('esiti')}>
        <span class="wic"><StateGlyph kind="done" /></span>
        <span class="otext"><b>{t.homeAllOutcomes}</b></span>
        <span class="mono">{out.red.length > 3 ? `${t.homeMoreRed(out.red.length - 3)} · ` : ''}{out.done.length}</span>
        <span class="ms chevr">chevron_right</span>
      </button>
    </div>
  {/if}
{/if}
{/snippet}
{#snippet secAltro()}
  {#if (!part || part === 'tools') && ((usage && showUsage) || sys || (pluginsLoad && pluginsRun) || model.closed.length)}
    <!-- «Altro» (stile «Studio e Diretta», 30/10): Utilizzo, Sistema, Plugin e skill e Chiuse come righe di una lista, col
         numero in vista; il tocco apre la riga sul posto, Chiuse apre l'elenco. La Notte sta nei Lavori. -->
    <!-- Nella plancia Utilizzo sta aperto in cima alla colonna col suo grafico (pwa2, 30/10); in «Altro» il resto. -->
    {#if part && usage && showUsage}
      <div class="sec static"><span class="ic"><span class="ms">data_usage</span></span><span class="st">{t.homeUsage}</span></div>
      <div class="card wrows altro"><div class="footer exp" class:flash={usageFlash} bind:this={usageEl}>{@render usage()}</div></div>
    {/if}
    <div class="sec static"><span class="ic"><span class="ms">more_horiz</span></span><span class="st">{t.homeMore}</span></div>
    <div class="card wrows altro">
      {#if usage && showUsage && !part}
        <div bind:this={usageEl}>
          <button class="wrow orow" aria-expanded={open.usage} onclick={() => toggleSec('usage')}><span class="wic"><span class="ms">data_usage</span></span><span class="otext"><b>{t.homeUsage}</b></span>{#if usageLine && !open.usage}<span class="mono">{usageLine}</span>{/if}<span class="ms tog">{open.usage ? 'expand_less' : 'expand_more'}</span></button>
        </div>
        {#if open.usage}{@render (usageBlock ?? usage)()}{/if}
      {/if}
      {#if sys}
        <button class="wrow orow" aria-expanded={sysOpen} onclick={() => (sysManual = { level: sys.level, open: !sysOpen })}><span class="wic"><span class="ms">memory</span></span><span class="otext"><b>{t.homeSystem}</b></span>{#if !sysOpen}<span class="mono"><b class="sysdot" style:background={SYS_TONE[sys.level]}></b>{summaryLine(sys, lang)}</span>{/if}<span class="ms tog">{sysOpen ? 'expand_less' : 'expand_more'}</span></button>
        {#if sysOpen}
          <div class="exp sys" data-testid="system_advice">
      <div class="kv sysm">
        <span>{t.sysLoad}</span><span class="trackrow"><span class="track"><span class="fill" style="width:{Math.min(100, sys.load_per_core / 3 * 100)}%;background:{SYS_TONE[sys.load_level]}"></span></span><span class="ctxv">{t.sysLoadV(sysNum(sys.load_per_core, lang))}</span></span>
        <span>{t.sysMem}</span><span class="trackrow"><span class="track"><span class="fill" style="width:{Math.min(100, (1 - sys.mem_available_mb / Math.max(1, sys.mem_total_mb)) * 100)}%;background:{SYS_TONE[sys.mem_level]}"></span></span><span class="ctxv">{t.sysMemV(sysNum(sys.mem_available_mb / 1024, lang), sysNum(sys.mem_total_mb / 1024, lang))}</span></span>
      </div>
      <!-- Lo stato tagliato a 8 KB arriva spesso senza consumatori: allora niente riga, il livello e i consigli restano.
           Un consumatore per riga: in una riga sola il testo era tagliato a metà altezza (app-home-riordino-29-10). -->
      {#if sys.consumers.length}<p class="mono2 who">{#each consumersLine(sys, lang).split(' · ') as c, i (i)}<span>{c}</span>{/each}</p>{/if}
      {#if sys.advice.length}
        <div class="oks">
          {#each sys.advice as a (a.id)}
            {@const act = onSystem ? actionOf(a, st.ops) : null}
            <i class="oline"></i>
            <div class="ok">
              <b class="sysdot" style:background={SYS_TONE[a.level]}></b>
              <span class="okt">{a.text}</span>
              {#if act && sysAsk !== a.id}<button class="tonal mini" disabled={sysSent.has(a.id)} onclick={() => (sysAsk = a.id)}>{act.label}</button>{/if}
            </div>
            {#if act && sysAsk === a.id}
              <div class="confirm" role="group" aria-label={act.label}>
                <span>{act.op === 'sys_unit_stop' ? t.sysConfirmStop : t.sysConfirm}</span>
                <button class="keep" onclick={() => (sysAsk = null)}>{t.sysKeep}</button>
                <button class="danger" onclick={() => { sysAsk = null; sysSent = new Set([...sysSent, a.id]); onSystem?.(act.op, act.arg) }}>{act.label}</button>
              </div>
            {/if}
          {/each}
        </div>
      {:else}<p class="okt mono2">{t.sysCalm}</p>{/if}
          </div>
          {#if kpiData?.kpi_health}<div class="exp"><Kpi12 h={kpiData.kpi_health} /></div>{/if}
        {/if}
      {/if}
      {#if pluginsLoad && pluginsRun}<PluginsSection {st} load={pluginsLoad} run={pluginsRun} />{/if}
      {#if model.closed.length}
        <!-- «Chiuse · N» (ClosedSheet dell'app): il tocco apre l'elenco con «Riapri». -->
        <button class="wrow orow" onclick={() => closedDlg?.showModal()}><span class="wic"><span class="ms">inventory_2</span></span><span class="otext"><b>{t.catClosed}</b></span><span class="mono">{model.closed.length}</span><span class="ms chevr">chevron_right</span></button>
      {/if}
    </div>
  {/if}
{/snippet}
<!-- L'ordine scelto da Franz (Impostazioni, Riordina la home); tabellone e «Ti aspettano» restano fissi qui sopra. -->
{#each order as k (k)}
  {#if k === 'sessions'}{@render secSessions()}{:else if k === 'suggestions'}{@render secSuggestions()}{:else if k === 'lavori'}{@render secLavori()}{:else if k === 'esiti'}{@render secEsiti()}{:else}{@render secAltro()}{/if}
{/each}
  {#if footer && !part}<div class="footer">{@render footer()}</div>{/if}
</section>

<!-- «Chiuse · N» (ClosedSheet dell'app): una riga per sessione chiusa, «Riapri» la riapre e chiude il foglio. -->
<dialog bind:this={closedDlg} class="sheet" onclick={(e) => e.target === e.currentTarget && closedDlg?.close()}>
  <h2 class="sh">{t.closedCat(model.closed.length)}</h2>
  {#each model.closed as c (c.name)}
    <!-- Contratto 1.84 (B4): lo stato salvato alla chiusura, e «Riprendi» quando c'è l'id della conversazione. -->
    <div class="crow"><Badge s={c} size={24} />
      <span class="ccol"><b>{c.name}</b>
        {#if c.closed?.outcome}<small>{t.closeSavesOutcome(c.closed.outcome)}</small>{/if}
        {#if c.closed?.next}<small>{t.closeSavesNext(c.closed.next)}</small>{/if}
        {#if c.closed?.dirty_n}<small class="warn">{t.closeDirty(c.closed.dirty_n, (c.closed.dirty ?? []).slice(0, 3).join(', '))}</small>{/if}
      </span>
      {#if onReopen}<button class="obtn" onclick={() => { closedDlg?.close(); onReopen(c.name) }}>{c.closed?.session_id ? t.resumeSaved : t.reopen}</button>{/if}</div>
  {/each}
</dialog>

{#snippet card(r: Row, group: Group)}
    {@const s = r.session}
    {@const x = text(r)}
    {@const open = expanded === s.name}
    {@const c = cleanupOf(s, canExit, st.ts)}
    {@const mine = placed?.inCards.get(s.name) ?? []}
    {@const svc = placedOut.inCards.get(s.name) ?? []}
    {@const rl = railOf(r)}
    <div class="card r" class:waiting={group === 'waiting'} class:on={selected.includes(s.name)}
      role="button" tabindex="0" onclick={() => { if (longFired) { longFired = false; return } onPick(s.name) }} onkeydown={(e) => e.key === 'Enter' && onPick(s.name)}
      oncontextmenu={(e) => { e.preventDefault(); holdEnd(); openCtx(s.name, e.clientX, e.clientY) }} onpointerdown={(e) => holdStart(s.name, e)} onpointerup={holdEnd} onpointerleave={holdEnd} onpointercancel={holdEnd}>
      {#if ctx === s.name}
        <button class="scrim" aria-label={t.closeWord} onclick={(e) => { e.stopPropagation(); ctx = null }}></button>
        <div class="cmenu" role="menu" tabindex="-1" style="left:{at.x}px; top:{at.y}px" onclick={(e) => e.stopPropagation()} onkeydown={(e) => { if (e.key === 'Escape') ctx = null; e.stopPropagation() }}>
          <div class="ph"><Badge {s} size={20} /><b>{s.name}</b></div>
          <button class="mi" role="menuitem" onclick={() => { onSessionCmd(s.name, s.followed ? 'unfollow' : 'follow'); ctx = null }}>
            <svg viewBox="0 0 24 24" width="22" height="22"><path d={s.followed ? 'M8.7 3A6 6 0 0 1 18 8a21 21 0 0 0 .6 5M17 17H3s3-2 3-9a4.7 4.7 0 0 1 .3-1.7M10.3 21a1.9 1.9 0 0 0 3.4 0M2 2l20 20' : 'M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.9 1.9 0 0 0 3.4 0'} /></svg>
            <span><span class="mt">{s.followed ? t.unfollow : t.follow}</span><span class="msub">{s.followed ? t.unfollowSub : t.followSub}</span></span>
          </button>
          {#if s.name !== MASTER && hasMaster}
            <button class="mi" role="menuitem" onclick={() => { onSessionCmd(s.name, 'prompt', t.askMasterPrompt(s.name)); ctx = null }}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8" /></svg>
              <span><span class="mt">{t.askMaster}</span><span class="msub">{t.askMasterSub}</span></span>
            </button>
          {/if}
          {#if canExit && s.state !== 'gone'}
            <hr />
            <button class="mi danger" role="menuitem" onclick={() => { ctx = null; confirming = s.name }}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg>
              <span><span class="mt">{t.menuExit}</span><span class="msub">{t.menuExitSub}</span></span>
            </button>
          {/if}
        </div>
      {/if}
      <!-- Il binario (stile «Studio e Diretta», 30/10): il glifo dello stato e, sotto, da quanto aspetta o lavora, o l'ora. -->
      <span class="rail"><StateGlyph kind={rl.kind} ctx={s.context} square={square(s)} label={t.states[s.state]} />{#if rl.text}<b class="rt {rl.kind}">{rl.text}</b>{/if}</span>
      <div class="cnt">
      <div class="top">
        <!-- Il colore che dai alla sessione, piccolo accanto al nome; il segno della modalità dei permessi dopo il nome, solo
             se diverso dal solito (mockup del 14/10, schermata 2). -->
        <span class="nm">{#if idColor(s)}<i class="idc" class:sq={square(s)} style:background={idColor(s)}></i>{/if}<span class="name">{s.name}</span><PermGlyph g={homeGlyph(s)} /></span>
        {#if s.duplicate_of}<span class="dup">{t.cleanupDupTag.toUpperCase()}</span>{/if}
        <!-- La quota 5h sta una volta sola, nel tabellone; qui il contesto e l'ora dell'ultimo rosso di oggi. -->
        {#if s.context != null && (group === 'waiting' || group === 'working')}<span class="mono">{t.ctx(s.context)}</span>{/if}
        {#if group !== 'waiting' && lastRed.get(s.name)}<span class="redtag"><StateGlyph kind="red" size={14} />{hhmm(lastRed.get(s.name)!)}</span>{/if}
        <button class="chev" aria-label={open ? t.close : t.open} onclick={(e) => { e.stopPropagation(); expanded = open ? null : s.name }}>
          <svg viewBox="0 0 24 24" width="22" height="22"><path d={open ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
        </button>
      </div>
      {#if open && s.goal?.text}<div class="goal">{t.goal}: {s.goal.text}</div>{/if}
      {#if group === 'working' && !open && s.tool}
        <!-- Chiusa, una sessione al lavoro è una riga: lo strumento, da quanto dura il turno, il contesto (app-home-riordino-29-10). -->
        {@const [tool, ...rest] = s.tool.split(' ')}
        <p class="meta mono2"><span class="tool">{tool}</span>{rest.join(' ')}</p>
      {:else if x.body}{@const old = staleHours(r, now)}<p class="body" class:full={open} class:one={!open && group !== 'waiting'} class:strong={group === 'waiting' || open} class:stale={old != null}>{x.body}{#if old != null}<span class="ago">{t.staleAgo(old)}</span>{/if}</p>{/if}
      {#if group === 'waiting' && askedFor(s)}{@const a = askedFor(s)!}<p class="asked mono2">{a.task ? t.askedForTask(a.from, a.task) : t.askedFor(a.from)}</p>{/if}
      {#if group === 'waiting' && !s.question}
        {@const d = doneOf(s)}
        <!-- Contratto 1.80: una ferma si chiude da qui col tasto «Fatto»; la scadenza di un needs-you accanto. -->
        {#if d}<div class="oks"><div class="ok"><span class="okt mono2">{s.needs_you?.expires_at ? expiryText(s.needs_you.expires_at) : ''}</span><button class={s.name === primary && !s.needs_you?.until ? 'approve mini' : 'tonal mini'} onclick={(e) => { e.stopPropagation(); onDone(d) }}>{t.attentionDone}</button></div></div>{/if}
      {/if}
      {#if open}<div class="details">{[s.model?.label, s.effort, s.account].filter(Boolean).join(' · ')}</div>{/if}
      {#if open && kpiPage}<Kpi11 spans={kpiDay(kpiEvents.get(s.name) ?? [], dayStart, now, s.state === 'gone' ? null : waitingSince(s))} from={dayStart} {now} />{/if}
      {#if open && turnsOf(kpiData, s.name).length}<Kpi10 turns={turnsOf(kpiData, s.name)} />{/if}
      {#if r.approvals.length}
        <!-- Franz 23/10 19:34: gli ok del registro che questa sessione chiede, ognuno col suo Approva, qui e non in schede a parte. -->
        <div class="oks">
          {#each r.approvals as a (a.task)}
            <div class="ok" class:wrap={choices(a).length > 0}>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="var(--advice)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76M9 12l2 2 4-4" /></svg>
              <span class="okt">{a.title}</span>
              {#if a.deploy}<span class="dup">{t.approvalProd.toUpperCase()}</span>{/if}
              {@render okButtons(a, false)}
            </div>
          {/each}
        </div>
      {/if}
      {#if mine.length}
        <!-- Strada B (Franz 23/10 23:08): i lavori di questa sessione che aspettano te, qui come gli ok. -->
        <div class="oks">
          {#each mine as w, i (`w-${i}`)}
            <div class="ok">
              <WorkBadge kind={w.kind} bare />
              <span class="okt col"><b>{fyShort(w)}</b><small class="mono2">{w.kind === 'outside' ? t.lavoriYou(w.at ? hhmm(w.at) : null) : t.lavoriCanStart}</small></span>
              {#if w.kind === 'outside'}<button class="approve mini" onclick={(e) => { e.stopPropagation(); fyAct(w) }}>{t.agendaDone}</button>
              {:else}<button class="tonal mini" onclick={(e) => { e.stopPropagation(); fyAct(w) }}>{t.lavoriStart}</button>{/if}
            </div>
          {/each}
        </div>
      {/if}
      {#if svc.length}
        <!-- Franz 24/10 00:46: un suggerimento che riguarda questa sessione (il contesto alto) sta qui, con il suo tasto tonale. -->
        <div class="oks">
          {#each svc as r, i (`svc-${i}`)}
            {@const x = serviceText(r)}
            <div class="ok">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="var(--wait)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d={CAT_ICON.fix} /></svg>
              <span class="okt col"><b>{x.title}</b>{#if x.detail}<small class="mono2">{x.detail}</small>{/if}</span>
              <button class="tonal mini" onclick={(e) => { e.stopPropagation(); onService(r) }}>{x.action}</button>
            </div>
          {/each}
        </div>
      {/if}
      {#if group === 'waiting' && s.question}
        <Options q={s.question} onAnswer={(n) => onAnswer(s.name, n)} firstFilled={s.name === primary} tonal="color-mix(in srgb, var(--primary) 12%, var(--surface))" />
      {/if}
      {#if x.steps.length}
        <div class="chips">
          {#each x.steps as step}{@const k = stepKey(s, step)}<button class="chip" class:unblock={x.blocking.has(step)} class:sent={sentSteps.has(k)} disabled={sentSteps.has(k)} aria-label={sentSteps.has(k) ? t.stepSent(step) : undefined} onclick={(e) => { e.stopPropagation(); sentSteps = new Set([...sentSteps, k]); onStep(s.name, step) }}>{#if sentSteps.has(k)}<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="var(--good)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5" /></svg>{:else if x.blocking.has(step)}<svg class="lock" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="var(--wait)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 9.9-1" /></svg>{/if}{step}</button>{/each}
        </div>
      {/if}
      {#if c}
        <div class="clean">
          <!-- Chi ha finito o è un doppione resta qui e dice di essere aperta (Franz, 06/10 10:57). -->
          <span>{#if s.attached}{t.cleanupAttached}{:else}<b class="alive">● {t.cleanupOpen}</b> · {c.kind === 'duplicate' ? t.cleanupDuplicate(c.of ?? '') : s.outcome?.at ? t.cleanupFinishedAt(new Date(s.outcome.at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })) : t.cleanupFinished}{/if}</span>
          {#if closing !== s.name && c.canClose && confirming !== s.name}
            <button class="close" onclick={(e) => { e.stopPropagation(); confirming = s.name }}>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="var(--b-alert)" stroke-width="2" stroke-linecap="round"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg>{t.cleanupCloseShort}
            </button>
          {/if}
        </div>
        <!-- Su una riga sua (Franz, 27/10 13:23): accanto, col nome lungo, lasciava allo stato due-tre lettere per riga. -->
        {#if closing === s.name}
          <div class="clean"><span class="closing" role="status"><i class="spin" aria-hidden="true"></i>{t.splashClosing(s.name)}</span></div>
        {/if}
        <!-- La conferma sta nella card, senza finestre (Franz, 06/10 12:40): il secondo tocco, col nome, chiude. -->
        {#if c.canClose && confirming === s.name}
          <div class="confirm" role="group" aria-label={t.closeTitle(s.name)}>
            <span>{t.closeInline}</span>
            <button class="keep" onclick={(e) => { e.stopPropagation(); confirming = null }}>{t.closeKeep}</button>
            <button class="danger" onclick={(e) => { e.stopPropagation(); confirming = null; onClose(s.name) }}>{t.closeOk(s.name)}</button>
          </div>
        {/if}
      {/if}
      {#if closing === s.name && !c}
        <div class="clean"><span class="closing" role="status"><i class="spin" aria-hidden="true"></i>{t.splashClosing(s.name)}</span></div>
      {/if}
      {#if confirming === s.name && !c?.canClose}
        <div class="confirm" role="group" aria-label={t.closeTitle(s.name)}>
          <span>{t.closeInline}</span>
          <button class="keep" onclick={(e) => { e.stopPropagation(); confirming = null }}>{t.closeKeep}</button>
          <button class="danger" onclick={(e) => { e.stopPropagation(); confirming = null; onClose(s.name) }}>{t.closeOk(s.name)}</button>
        </div>
      {/if}
      {#if !open}
        <!-- Le barre del contesto e delle ultime sei ore (kpi-5, kpi-6) solo nella card aperta (app-home-riordino-29-10). -->
      {:else if s.context != null}
        <!-- Grammatica unica (Franz 24/10 00:01): il contesto sta sulla barra, col valore in mono accanto; non in testa. -->
        <div class="trackrow">{#if kpiPage && s.state !== 'gone'}<Kpi6 counts={hourly(kpiEvents.get(s.name) ?? [], now)} />{/if}<div class="track hand" data-kpi="kpi-5" style="--hand:{HANDOFF}%"><div class="fill" style="width:{Math.min(100, s.context)}%;background:{ctxTone(s.context)}"></div></div><span class="ctxv">{t.ctx(s.context)}{#if todayShare(st, s.name) != null}{' · '}{t.kpi5Today(todayShare(st, s.name)!)}{/if}</span></div>
      {:else if kpiPage && s.state !== 'gone'}
        <!-- Proposta 6 senza contesto letto: le colonnine da sole, sulla riga dove starebbe la barra. -->
        <div class="trackrow"><Kpi6 counts={hourly(kpiEvents.get(s.name) ?? [], now)} /></div>
      {/if}
      </div>
    </div>
{/snippet}

{#snippet workCard(r: LavoriRow, i: number, filled: boolean)}
    {@const k = workKey(r, i)}
    {@const openW = openWork === k}
    {@const ds = lavoriDetails(r)}
    <div class="card" class:waiting={r.kind === 'ok'}>
      <div class="top">
        <WorkBadge kind={r.kind} shape={r.agenda ? mark(r.agenda.scope) : 'none'} />
        <span class="nm"><span class="name">{fyShort(r)}</span></span>
        {#if fyMeta(r)}<span class="mono">{fyMeta(r)}</span>{/if}
        <button class="chev" aria-label={openW ? t.close : t.open} onclick={() => (openWork = openW ? null : k)}>
          <svg viewBox="0 0 24 24" width="22" height="22"><path d={openW ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
        </button>
      </div>
      <!-- Franz 24/10 09:22: per una scelta la domanda intera, sempre. -->
      <p class="body" class:full={openW || (!!r.approval && choices(r.approval).length > 0)}>{r.title}</p>
      {#if openW && ds.length}
        <div class="kv wkv">{#each ds as d (d.kind)}<span>{t.lavoriDetail[d.kind]}</span><span>{d.kind === 'asked_at' ? hhmm(Number(d.value)) : d.value}</span>{/each}</div>
      {/if}
      <div class="oks"><div class="ok" class:stack={!!r.approval && choices(r.approval).length > 0}>
        <span class="okt mono2">{fySub(r) ?? ''}</span>
        {#if r.kind === 'ok'}{#if r.approval}{@render okButtons(r.approval, filled)}{/if}
        {:else}<button class="tonal mini" onclick={() => fyAct(r)}>{r.kind === 'outside' ? t.agendaDone : t.lavoriStart}</button>{/if}
      </div></div>
    </div>
{/snippet}

<!-- La scheda Stanotte con lavori in coda (mockup approvato l'08/10 alle 20:43): quello in corso per primo, poi l'ultima notte. -->
{#snippet tonight(r: ForYouRow)}
    <div class="card svc">
      <div class="top">
        <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="12" fill="var(--b-track)" /><g transform="translate(4.8 4.8) scale(0.6)" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d={CAT_ICON.night} /></g></svg>
        <span class="nm"><span class="name">{t.catNight}</span></span>
        <span class="mono">{t.tonightMeta(tonightCard.running, tonightCard.queued)}</span>
      </div>
      <p class="body">{tonightCard.items.length ? t.tonightTitle(tonightCard.items.length) : t.fyNightEmptyShort}</p>
      {#each tonightCard.items as it, k (it.id)}
        <i class="oline"></i>
        <div class="tjob">
          <span class="tn" class:run={it.runningMin != null}>{#if it.runningMin != null}<svg viewBox="0 0 24 24" width="16" height="16"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" /></svg>{:else}{k + 1}{/if}</span>
          <span class="otext"><b>{it.name}</b><small class="two">{it.prompt}</small></span>
          <span class="ttag" class:run={it.runningMin != null}>{it.runningMin != null ? t.tonightRunning(it.runningMin) : t.tonightQueued}</span>
        </div>
      {/each}
      {#if lastNight}
        <i class="oline"></i>
        <div class="tlast">
          <small>{t.tonightLast(Number(lastNight.dayBefore.slice(8)), Number(lastNight.day.slice(8)))}</small>
          {#if lastNight.counts.done}<small style:color="var(--good)">{t.nightDone(lastNight.counts.done)}</small>{/if}
          {#if lastNight.counts.stopped}<small style:color="var(--gone-dim)">{t.nightStopped(lastNight.counts.stopped)}</small>{/if}
          <span class="sp"></span>
          <button class="olink" onclick={onNight}>{t.tonightReport}</button>
        </div>
      {/if}
      <div class="oks"><div class="ok"><span class="okt mono2">{tonightCard.items.length ? t.tonightSub(tonightCard.running, tonightCard.queued) : t.tonightEmptySub}</span><button class="tonal mini" onclick={() => onService(r)}>{t.fyAdd}</button></div></div>
    </div>
{/snippet}

<!-- `filled`: il solo bottone pieno della schermata, quando prima non c'è chi aspetta (app-home-riordino-29-10). -->
{#snippet okButtons(a: Approval, filled: boolean)}
  {@const opts = choices(a)}
  {#if opts.length}<span class="opts">{#each opts as o (o)}<button class="tonal mini" onclick={(e) => { e.stopPropagation(); askApprove(a, o) }}>{o}</button>{/each}</span>
  {:else}<button class={filled ? 'approve mini' : 'tonal mini'} onclick={(e) => { e.stopPropagation(); askApprove(a) }}>{t.approve}</button>{/if}
{/snippet}

<dialog bind:this={closeDlg} class="alert closef" onclick={(e) => e.target === e.currentTarget && closeDlg?.close()}>
  <h3>{t.closeFinishedTitle}</h3>
  {#if previewing}<p class="sub">{t.closeFinishedLoading}</p>
  {:else if !preview}<p class="sub">{t.closeFinishedNoPreview}</p>
  {:else}
    <p class="sub">{t.closeFinishedSub}</p>
    <div class="cfl">
      {#each preview.sessions as x (x.name)}
        <div class="cf" class:no={!x.closable}>
          <b>{x.name}</b>
          {#if !x.closable}<span class="why">{t.closeBlocked[x.blocked_by ?? ''] ?? t.closeBlockedOther}</span>
          {:else}
            {#if x.save?.outcome}<span>{t.closeSavesOutcome(x.save.outcome)}</span>{/if}
            {#if x.save?.next}<span>{t.closeSavesNext(x.save.next)}</span>{/if}
            {#if x.save?.dirty_n}<span class="warn">{t.closeDirty(x.save.dirty_n, (x.save.dirty ?? []).slice(0, 3).join(', '))}</span>{/if}
            {#if x.save?.session_id}<span class="ok">{t.closeResumable}</span>{/if}
          {/if}
        </div>
      {/each}
    </div>
  {/if}
  <div class="btns">
    <button class="text2" onclick={() => closeDlg?.close()}>{t.cancel}</button>
    {#if preview && closableOf(preview).length}<button class="filled" onclick={() => { onCloseMany(closableOf(preview!)); closeDlg?.close() }}>{t.closeFinishedGo(closableOf(preview).length)}</button>{/if}
  </div>
</dialog>

<dialog bind:this={approveDlg} class="alert" onclick={(e) => e.target === e.currentTarget && approveDlg?.close()}>
  {#if pending}
    {#key `${pending.task}:${pendingChoice}`}
      <ApproveBody a={pending} choice={pendingChoice} {onSpeak} onCancel={() => approveDlg?.close()} onConfirm={(n) => { onApprove(pending!.task, n, pendingChoice ?? undefined); approveDlg?.close() }} />
    {/key}
  {/if}
</dialog>


<style>
  .alive { color: var(--idle); font-weight: 500; }
  .home { padding: 12px 16px 24px; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; height: 100%; }
  .gh { display: flex; align-items: center; gap: 8px; margin: 6px 8px 0; font: 500 12px/1.4 var(--mono); letter-spacing: .08em; color: var(--t); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--t) 25%, transparent); }
  .card.just { background: var(--surface); outline: 1.5px solid color-mix(in srgb, var(--icon) 70%, transparent); cursor: default; padding: 14px; }
  .jrow { display: flex; align-items: center; gap: 12px; }
  .jcol { display: flex; flex-direction: column; min-width: 0; }
  .jcol b { font-size: 16px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .jcol small { font-size: 14px; color: var(--text2); white-space: nowrap; overflow: hidden; }
  .scrim { position: fixed; inset: 0; z-index: 60; cursor: default; background: transparent; }
  .cmenu { position: fixed; z-index: 61; width: min(360px, calc(100vw - 24px)); background: var(--surface); border-radius: 20px; padding: 8px; box-shadow: 0 12px 40px rgb(0 0 0 / .5); border: 1px solid var(--line); cursor: default; }
  .cmenu .ph { display: flex; gap: 10px; align-items: center; padding: 8px 12px 10px; }
  .cmenu hr { border: 0; border-top: 1px solid var(--line); margin: 4px 0; }
  /* Il menu contestuale nella forma della v3 (app-menu-v3-30-10). */
  .cmenu { background: var(--s1); border: 1px solid var(--line); border-radius: 14px; padding: 0; overflow: hidden; }
  .cmenu .ph { min-height: 54px; padding: 0 12px 0 16px; border-bottom: 1px solid var(--line); font: 600 17px var(--display); }
  .cmenu hr { display: none; }
  .cmenu .mi { align-items: center; gap: 8px; min-height: 58px; padding: 9px 12px 9px 0; border-radius: 0; border-top: 1px solid var(--line); }
  .cmenu .ph + .mi { border-top: 0; }
  .cmenu .mi:hover { background: var(--s2); }
  .cmenu .mi svg { margin: 0 15px; stroke: var(--tx2); }
  .cmenu .mt { font: 600 17px/1.2 var(--display); }
  .cmenu .msub { font-size: 14px; line-height: 1.3; color: var(--tx2); }
  .cmenu .mi.danger .mt { color: var(--red); }
  .mi { display: flex; gap: 14px; align-items: flex-start; width: 100%; padding: 12px; border-radius: 14px; text-align: left; color: var(--text); }
  .mi:hover { background: var(--high); }
  .mi svg { flex: none; fill: none; stroke: var(--icon); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  .mi > span { display: flex; flex-direction: column; gap: 2px; }
  .mt { font-size: 15.5px; }
  .msub { font-size: 13px; color: var(--text2); }
  .mi.danger svg { stroke: var(--b-alert); }
  .card { position: relative; -webkit-touch-callout: none; background: var(--low); border-radius: 20px; padding: 8px 10px 12px 14px; display: flex; flex-direction: column; gap: 6px; cursor: pointer; transition: background .15s, box-shadow .15s; outline: none; }
  .card.waiting { background: color-mix(in srgb, var(--low) 94%, var(--b-warn)); }
  .card:hover, .card:focus-visible { box-shadow: inset 0 0 0 1px var(--line); }
  .card.on { box-shadow: inset 0 0 0 1.5px var(--b-ring); }
  .top { display: flex; align-items: center; gap: 10px; }
  .nm { flex: 1; min-width: 0; display: flex; align-items: center; gap: 6px; }
  .nm .name { flex: 0 1 auto; }
  .name { flex: 1; min-width: 0; font-weight: 600; font-size: 16px; white-space: nowrap; overflow: hidden; }
  .stale { color: var(--wait); }
  .chev { width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .chev:hover { background: var(--surface); }
  .goal { font-size: 12.5px; color: var(--b-label); }
  /* Franz 24/10 19:46 (B2): un esito di più di 6 ore fa si spegne e dice da quanto. */
  .body.stale { font-style: italic; opacity: .75; } .body .ago { font: 600 10.5px var(--mono); letter-spacing: .06em; color: var(--b-warn); background: color-mix(in srgb, var(--b-warn) 14%, transparent); border-radius: 999px; padding: 1px 7px; margin-left: 6px; font-style: normal; white-space: nowrap; }
  .asked { margin: 2px 0 0; }
  .body { font-size: 14px; color: var(--text2); white-space: pre-wrap; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; text-overflow: clip; }
  .body.full { display: block; }
  .body.strong { color: var(--text); }
  .details { font-size: 12.5px; color: var(--text2); }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .approve.mini { align-self: center; margin: 0; padding: 9px 16px; font-size: 14px; }
  .tonal { background: var(--high); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; white-space: nowrap; }
  /* Strada B (23/10): la lista compatta dei lavori senza sessione e la riga di un lavoro nella scheda della sua sessione. */
  .wkv { display: grid; grid-template-columns: auto 1fr; gap: 3px 12px; font-size: 13px; overflow-wrap: anywhere; }
  .okt.col { display: flex; flex-direction: column; gap: 1px; } .okt.col b { font-weight: 500; font-size: 15px; white-space: nowrap; overflow: hidden; }
  .mono2 { font-family: ui-monospace, monospace; font-size: 12px; color: var(--text2); }
  .opts { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 6px; }
  .opts button { white-space: nowrap; }
  /* Franz 24/10 09:22: con le opzioni chi chiede (o il titolo) sopra e i tasti interi sotto, che vanno a capo fra loro. */
  .ok.stack { flex-direction: column; align-items: stretch; } .ok.stack .opts { justify-content: flex-start; }
  .ok.wrap { flex-wrap: wrap; } .ok.wrap .opts { flex-basis: 100%; justify-content: flex-start; padding-left: 28px; }
  .card .top .mono { max-width: 40%; white-space: nowrap; overflow: hidden; }
  /* Grammatica unica (24/10): la Notte e le voci di Altro come schede della famiglia; il contesto sulla barra col valore accanto. */
  .ncard { width: 100%; text-align: left; }
  .svc { cursor: default; }
  .chevr { flex: none; color: var(--text2); }
  .trackrow { display: flex; align-items: center; gap: 8px; } .trackrow .track { flex: 1; }
  .ctxv { flex: none; font: 500 11px/1 var(--mono); color: var(--text2); }
  /* Contratto 1.76: gli impegni del registro, ambra da 2 ore ferme, rosso da 8. */
  .cmt .line { font-size: 14px; }
  .cmt .tone, .cmt .dim { color: var(--text2); }
  .cmt.warn .tone { color: var(--b-warn, #FFB020); }
  .cmt.alert .tone, .cmt .red { color: var(--gone, #FF453A); }
  .cmt .red { font-size: 13px; }
  .stalled { color: var(--b-warn-ink, #1d1300); background: var(--b-warn, #FFB020); }
  .prod, .dup { font: 600 11px/1.4 var(--mono); letter-spacing: .08em; border-radius: 999px; padding: 3px 10px; flex: none; }
  .prod { color: var(--prod); background: color-mix(in srgb, var(--prod) 16%, transparent); }
  .dup { color: var(--b-warn); background: color-mix(in srgb, var(--b-warn) 14%, transparent); }
  .kv { display: grid; grid-template-columns: auto 1fr; gap: 3px 10px; font-size: 14px; }
  .kv span:nth-child(odd) { color: var(--text2); }
  .approve { align-self: stretch; margin-top: 4px; background: var(--primary); color: var(--on-primary); border-radius: 999px; padding: 11px 20px; font-weight: 500; }
  /* Testo e tasto: se non ci stanno, il tasto scende sotto; il testo non si schiaccia mai (Franz 30/10 19:46). */
  .clean { display: flex; flex-wrap: wrap; justify-content: flex-end; align-items: center; gap: 10px; font-size: 13.5px; color: var(--text2); }
  .clean span { flex: 1 1 160px; min-width: 0; }
  /* Mandato /exit da una card: la riga lo dice finché la sessione non esce (poi la card «chiusa alle» in cima). */
  .closing { flex: none !important; display: flex; align-items: center; gap: 8px; padding: 8px 14px; border-radius: 999px; background: var(--high); color: var(--text); font-weight: 500; }
  .closing .spin { width: 14px; height: 14px; border-radius: 50%; border: 2px solid var(--b-track); border-top-color: var(--icon); animation: turn 1s linear infinite; }
  @keyframes turn { to { rotate: 360deg; } }
  @media (prefers-reduced-motion: reduce) { .closing .spin { animation: none; } }
  .confirm { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; font-size: 13.5px; color: var(--text2); }
  .confirm span { flex: 1 1 160px; min-width: 0; }
  .confirm button { border-radius: 999px; padding: 8px 14px; font-weight: 500; flex: none; }
  .confirm .keep { color: var(--icon); }
  .confirm .keep:hover { background: var(--high); }
  .confirm .danger { background: var(--gone); color: #fff; }
  .close { display: flex; align-items: center; gap: 8px; background: var(--high); border-radius: 999px; padding: 8px 14px; color: var(--text); font-weight: 500; flex: none; }
  dialog.alert { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 22px; width: min(400px, 92vw); border-radius: 28px; }
  dialog.alert::backdrop { background: rgb(0 0 0 / .55); }
  .ccol { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; overflow-wrap: anywhere; } .ccol small { color: var(--text2); font-size: 12.5px; } .ccol small.warn { color: var(--b-warn); }
  .ghact { font: 500 13px Roboto, system-ui, sans-serif; letter-spacing: 0; color: var(--icon); padding: 3px 10px; border-radius: 14px; background: color-mix(in srgb, var(--icon) 12%, transparent); }
  .ghact:hover { background: color-mix(in srgb, var(--icon) 20%, transparent); }
  .cfl { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; max-height: 50vh; overflow-y: auto; }
  .cf { display: flex; flex-direction: column; gap: 2px; font-size: 13.5px; color: var(--text2); overflow-wrap: anywhere; }
  .cf b { color: var(--text); font-size: 15px; font-weight: 600; } .cf.no b { color: var(--text2); }
  .cf .warn { color: var(--b-warn); } .cf .ok { color: var(--good); } .cf .why { font-style: italic; }
  dialog h3 { font-size: 22px; font-weight: 600; }
  dialog .sub { color: var(--text2); font-size: 14px; margin-top: 6px; }
  dialog .lab { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: var(--text2); margin-top: 14px; }
  dialog input { background: var(--high); color: var(--text); border: 0; border-radius: 14px; padding: 11px 14px; font: inherit; font-size: 15px; }
  dialog .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
  dialog .btns button { padding: 10px 16px; border-radius: 20px; font-weight: 500; }
  dialog .text2 { color: var(--text2); }
  dialog .filled { background: var(--primary); color: var(--on-primary); }
  .chip { border-radius: 999px; padding: 8px 14px; font-size: 14px; background: color-mix(in srgb, var(--primary) 12%, transparent); }
  .chip:hover { filter: brightness(1.15); }
  /* Contratto 1.38, variante 2 (Franz, 05/10 22:06): il Prossimo che sblocca ha un filo ambra e il lucchetto aperto. */
  .chip.sent { display: inline-flex; align-items: center; gap: 6px; background: color-mix(in srgb, var(--good) 14%, transparent); color: var(--text2); cursor: default; }
  .chip.unblock { display: inline-flex; align-items: center; gap: 6px; box-shadow: inset 0 0 0 1.5px color-mix(in srgb, var(--wait) 65%, transparent); }
  .track { margin-right: 4px; height: 3px; border-radius: 2px; background: var(--b-track); overflow: hidden; }
  .fill { height: 100%; }
  /* Proposta 5: il segno della soglia di handoff sulla barra del contesto. */
  .track.hand { position: relative; overflow: visible; }
  .track.hand::after { content: ''; position: absolute; left: var(--hand); top: -2px; width: 2px; height: 7px; border-radius: 1px; background: var(--text); opacity: .8; }
  .track.hand .fill { border-radius: 2px; }
  .sec { display: flex; align-items: center; gap: 8px; width: 100%; margin: 14px 0 4px; padding: 0 4px; font: 500 12px/1 var(--mono); letter-spacing: .14em; color: var(--text2); text-align: left; }
  .sec i { flex: 1; height: 1px; background: var(--line); }
  .sec small { font: 13px/1 var(--sans, inherit); letter-spacing: 0; }
  .sysdot { flex: none; width: 8px; height: 8px; border-radius: 50%; }
  .card.sys { cursor: default; padding: 12px 14px; }
  .sysm { align-items: center; } .sysm .trackrow { min-width: 0; } .sysm .track, .sysm .fill { display: block; }
  .nchips { display: flex; gap: 8px; flex-wrap: wrap; }
  .nchips span { display: inline-flex; align-items: center; gap: 4px; padding: 6px 12px 6px 10px; border-radius: 999px; background: var(--high); font-weight: 500; font-size: 14px; }
  .outside { margin: 18px 4px 0; }
  .oh { display: flex; align-items: center; gap: 12px; }
  .oh h3 { font-size: 16px; font-weight: 600; }
  .oh i { flex: 1; height: 1px; background: var(--line); }
  .osub { font-size: 12.5px; color: var(--stale); }
  .cat { margin-top: 6px; }
  .gh .note { font: 400 11.5px/1 var(--sans, inherit); letter-spacing: 0; text-transform: none; color: var(--stale); }
  .oline { display: block; height: 1px; margin: 0 14px; background: var(--line); }
  .otext { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; text-align: left; }
  .otext b { font-size: 16px; font-weight: 600; color: var(--text); }
  .otext small { font-size: 14px; color: var(--text2); }
  .two { display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  .obtn { flex: none; padding: 9px 16px; border-radius: 999px; background: var(--surface); color: var(--primary); font-size: 14px; font-weight: 500; }
  .tonight .otop { align-items: flex-start; }
  .tjob { display: flex; align-items: flex-start; gap: 12px; }
  .tn { width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center; background: var(--surface); color: var(--text2); font-size: 12px; font-weight: 500; flex: none; }
  .tn.run { background: var(--busy); color: var(--bg); }
  .ttag { flex: none; font: 500 11px/1 var(--mono); padding: 4px 7px; border-radius: 6px; background: var(--surface); color: var(--text2); }
  .ttag.run { color: var(--busy); background: color-mix(in srgb, var(--busy) 14%, transparent); }
  .tlast { display: flex; align-items: center; gap: 10px; font-size: 14px; color: var(--text2); }
  .tlast .sp { flex: 1; }
  .olink { padding: 6px; border-radius: 8px; color: var(--icon); font-size: 14px; font-weight: 500; }
  dialog.sheet { margin: auto auto 0; border: 0; color: var(--text); background: var(--surface); padding: 20px 0 16px; width: min(600px, 100vw); max-height: 80vh; border-radius: 28px 28px 0 0; }
  dialog.sheet::backdrop { background: rgb(0 0 0 / .55); }
  .sh { font-size: 20px; font-weight: 600; padding: 0 20px 8px; }
  .crow { display: flex; align-items: center; gap: 12px; padding: 10px 20px; }
  .crow b { flex: 1; font-weight: 500; }
  .crow .obtn { background: var(--high); }
  .act { display: inline-flex; align-items: baseline; gap: 8px; border-radius: 999px; padding: 10px 16px; background: var(--low); color: var(--text); font-size: 15px; text-align: left; }
  .act:hover { filter: brightness(1.15); }
  .act small { font-size: 12.5px; color: var(--text2); }
  .act.sent { align-items: center; background: color-mix(in srgb, var(--good) 14%, transparent); color: var(--text2); cursor: default; }
  .act small { white-space: nowrap; }
  .all { display: flex; justify-content: space-between; align-items: center; padding: 12px 4px; color: var(--text2); font-size: 15px; text-align: left; }
  .footer.flash { outline: 2px solid color-mix(in srgb, var(--icon) 80%, transparent); outline-offset: 4px; border-radius: 18px; }
  .footer { transition: outline-color .7s; display: flex; flex-direction: column; gap: 12px; padding: 12px 0; }
  .oks { display: flex; flex-direction: column; gap: 6px; }
  .ok { display: flex; flex-wrap: wrap; justify-content: flex-end; align-items: center; gap: 10px; }
  .ok svg { flex: none; }
  .okt.mono2 { font-family: ui-monospace, monospace; font-size: 13px; color: var(--text2); }
  .okt { flex: 1 1 140px; min-width: 0; font-size: 14px; color: var(--text); display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  /* Il riordino (app-home-riordino-29-10): «Ti aspettano» ha un'intestazione fissa ambra; le card stanno in un involucro
     per l'animazione; chiuse, le card fuori da «Ti aspettano» tengono una riga; Lavori ed Esiti hanno righe compatte. */
  .sec.static { cursor: default; }
  .sec.wait span, .sec.wait small { color: var(--wait); }
  .cw { display: flex; flex-direction: column; }
  .body.one { -webkit-line-clamp: 1; line-clamp: 1; white-space: normal; }
  .meta { margin: 0; white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .meta .tool { color: var(--busy); }
  .who { display: flex; flex-direction: column; gap: 1px; }
  .wrows { cursor: default; padding: 2px 14px; gap: 0; }
  .wrow { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid var(--line); width: 100%; text-align: left; color: var(--text); }
  .wrow:first-child { border-top: 0; }
  .wrow .otext b { font-size: 15px; font-weight: 500; }
  .wrow .otext small { font: 12.5px/1.35 var(--mono); color: var(--text2); display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  .wrow .otext small.txt { font: 14px/1.35 var(--sans, inherit); color: var(--text); }
  .wrow { flex-wrap: wrap; }
  .wrow .otext { flex: 1 1 140px; }
  .wrow .mono { font: 12px var(--mono); color: var(--text2); white-space: nowrap; }
  .wic { width: 30px; height: 30px; border-radius: 50%; display: grid; place-items: center; flex: none; background: var(--high); color: var(--text2); }
  .wic.red { background: color-mix(in srgb, var(--gone) 20%, transparent); color: var(--gone); }
  .wic.good { background: color-mix(in srgb, var(--idle) 18%, transparent); color: var(--idle); }
  .orow { cursor: pointer; }
  .wnote { font-size: 14px; color: var(--text2); padding: 10px 0; }
  .text.mini { color: var(--icon); font-weight: 500; font-size: 14px; padding: 8px 6px; white-space: nowrap; }

  /* Lo stile «Studio e Diretta», terza versione (approvato da Franz il 30/10 alle 09:49, app-home-riordino-29-10): solo la
     home e solo di notte, coi token di v3.css. Il binario di 52 px tiene glifi e icone; il pieno è per quello che aspetta te
     (schede), il resto sta in liste a filo; i tasti sono pillole in Barlow Semi Condensed, uno solo pieno e bianco. */
  .home { padding: 8px 12px 24px; gap: 8px; }
  /* La home scorre: i blocchi con overflow nascosto (tabellone, liste) non devono restringersi a una riga. */
  .home > * { flex-shrink: 0; }
  /* Nella plancia scorre la colonna, non la home: dopo vengono la notte, il fatto di oggi e gli strumenti. */
  .home.part { height: auto; overflow: visible; padding-bottom: 0; }
  .home.part :global(.vsec), .home.part :global(.vlist) { margin-left: 0; margin-right: 0; }
  /* In «Altro» il titolo non cede mai: è il riepilogo a destra che va a capo, su una colonna stretta. */
  .altro .otext { flex: 1 0 auto; }
  .altro .wrow .mono { min-width: 0; white-space: normal; text-align: right; }
  .sec { gap: 0; margin: 18px 0 2px; padding: 0; font: 700 18px/1.2 var(--display); letter-spacing: .005em; color: var(--tx); }
  .sec .ic { width: 52px; flex: none; display: flex; justify-content: center; margin-right: 8px; }
  .sec .ic .ms { color: var(--tx2); }
  .sec .st { white-space: nowrap; }
  .sec small { font: 600 15px var(--display); letter-spacing: 0; color: var(--tx2); margin-left: 8px; }
  .sec .sp { flex: 1; min-width: 8px; }
  .sec .rt { font: 400 14px var(--sans); color: var(--tx2); white-space: nowrap; overflow: hidden; min-width: 0; }
  .sec .tog { color: var(--tx3); margin-left: 6px; }
  .card { background: var(--s1); border-radius: 14px; padding: 12px; gap: 6px; }
  .card.waiting { background: var(--s2); }
  .card.r { flex-direction: row; align-items: flex-start; gap: 8px; padding: 12px 12px 12px 0; }
  .card.r .cnt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
  .rail { width: 52px; flex: none; display: flex; flex-direction: column; align-items: center; gap: 4px; padding-top: 1px; }
  .rt { font: 700 15px/1 var(--display); letter-spacing: .01em; color: var(--tx2); white-space: nowrap; }
  .rt.waiting { color: var(--wait); } .rt.busy { color: var(--busy); } .rt.red { color: var(--red); }
  .card.on { box-shadow: inset 0 0 0 1.5px color-mix(in srgb, var(--tx) 45%, transparent); }
  .list { background: var(--s1); border: 1px solid var(--line); border-radius: 14px; overflow: hidden; display: flex; flex-direction: column; }
  .list .card { background: none; border-radius: 0; }
  .list .cw + .cw .card { border-top: 1px solid var(--line); }
  .list .card:hover, .list .card:focus-visible { box-shadow: none; background: var(--s2); }
  .list .card.on { background: var(--s2); box-shadow: none; }
  .after { display: flex; justify-content: flex-end; }
  .top { gap: 8px; min-height: 22px; }
  .name { font: 600 17px/1.2 var(--display); }
  .idc { width: 8px; height: 8px; border-radius: 50%; flex: none; } .idc.sq { border-radius: 2px; }
  .card .top .mono { font: 400 12.5px var(--mono); letter-spacing: 0; color: var(--tx2); max-width: none; }
  .redtag { display: inline-flex; align-items: center; gap: 5px; font: 500 12.5px var(--mono); color: var(--red); white-space: nowrap; flex: none; }
  .chev { width: 28px; height: 28px; color: var(--tx3); }
  .body { color: var(--tx2); }
  .body.strong { color: var(--tx); font-size: 16px; line-height: 1.36; }
  .meta { font: 400 12.5px var(--mono); color: var(--tx2); }
  .meta .tool { color: var(--tx2); margin-right: .6em; }
  .mono2, .okt.mono2 { font-family: var(--mono); }
  .approve, .tonal, .text.mini, .close, .confirm button, .obtn, .ghact { font: 600 15px var(--display); letter-spacing: .02em; }
  .approve.mini, .tonal { min-height: 40px; padding: 0 16px; display: inline-flex; align-items: center; justify-content: center; border-radius: 20px; }
  .tonal { background: var(--s3); color: var(--tx); }
  .approve { background: var(--act); color: var(--on-act); }
  .text.mini { color: var(--tx2); }
  .chip { background: var(--s3); color: var(--tx); font: 600 14.5px var(--display); min-height: 36px; padding: 0 14px; display: inline-flex; align-items: center; border-radius: 18px; }
  .chip.unblock { box-shadow: inset 0 0 0 1.5px color-mix(in srgb, var(--tx) 70%, transparent); }
  .wrows { background: var(--s1); border: 1px solid var(--line); border-radius: 14px; padding: 0; }
  .wrow { gap: 8px; min-height: 52px; padding: 10px 12px 10px 0; }
  .wic { width: 52px; height: auto; border-radius: 0; background: none; flex-direction: column; gap: 4px; display: flex; align-items: center; color: var(--tx2); }
  .wic .ms, .wic svg { color: var(--tx2); }
  .wrow .otext b, .all .otext b { font: 600 17px/1.2 var(--display); color: var(--tx); }
  .wrow .otext small { color: var(--tx2); }
  .wrow .otext small.txt { font: 400 14px/1.35 var(--sans); color: var(--tx2); }
  .wrow .mono { font: 400 12.5px var(--mono); color: var(--tx2); }
  .wrow .mono .sysdot { display: inline-block; margin-right: 6px; vertical-align: 1px; }
  .chevr, .tog { color: var(--tx3); }
  .all { gap: 8px; min-height: 52px; padding: 10px 12px 10px 0; background: var(--s1); border: 1px solid var(--line); border-radius: 14px; color: var(--tx); width: 100%; }
  .all .otext { flex: 1; }
  .gh.cat { margin: 8px 0 0 60px; font: 400 13.5px var(--sans); letter-spacing: 0; color: var(--tx2); }
  .gh.cat svg, .gh.cat i { display: none; }
  .exp { padding: 2px 12px 12px 60px; }
  .footer.exp { padding: 8px 12px 12px; }
  .wnote { padding: 12px 12px 12px 60px; }
  .sugg { margin-top: 10px; }
</style>
