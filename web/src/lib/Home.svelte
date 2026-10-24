<script lang="ts">
  import { locale } from './lang'
  import Icon from './Icon.svelte'
  import { forMaster } from './strongAlert'
  import PermGlyph from './PermGlyph.svelte'
  import { homeGlyph } from './permissionModes'
  import { tick, untrack } from 'svelte'
  import { flip } from 'svelte/animate'
  import type { Approval, Commitment, Session, State, DonePage } from './contract'
  import { cleanupOf } from './masterService'
  import { since } from './durations'
  import { details as lavoriDetails, doneToday as lavoriDoneToday, forYou as lavoriForYou, inProgress as lavoriProgress, place as lavoriPlace, shortTitle, type ForYou as LavoriRow } from './lavori'
  import { attachedTasks, build, MASTER, type Group, type Row } from './summary'
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

  // La home del telefono (SummaryList.kt): ogni sessione una card nei gruppi del bisogno; il tocco apre la sessione, ▼ la
  // apre sul posto. La domanda ha le opzioni subito, chi ha finito i consigli come tasti, sotto la barretta del contesto.
  import type { Snippet } from 'svelte'
  import type { NightPage } from './night'
  let { st, selected, onPick, onAnswer, onStep, withMaster = false, footer, onApprove = () => {}, onDone = () => {}, onClose = () => {}, onSessionCmd = () => {}, night, nightTitle = '', onNight = () => {}, usage, justClosed = null, closing = null, elicitations = [], onElicit = () => {}, agenda = null, onLavori = () => {}, onEdit = () => {}, onStart = () => {}, onDoneForPerson = () => {}, showLavori = true, showUsage = true, usageFocus = 0, onUsageFocused = () => {}, onSpeak = null, onAttention = () => {}, donePages = {}, events = [], sent = [], read = new Set<string>(), now = st.ts, onService = () => {}, onReopen = null, lastNight = null }: {
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
    /** Contratto 1.68: il tocco sul banner di «Serve te» porta alla prima voce. */
    onAttention?: () => void
  } = $props()
  // Contratto 1.68: in cima alla casa della master quante cose aspettano Franz nelle altre sessioni, coi nomi.
  const waitingYou = $derived(forMaster(st))
  const today = todayIso()
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
  const SEC = 'home-sections'
  let open = $state<Record<string, boolean>>((() => { try { return { night: true, sessions: true, lavori: true, usage: false, other: false, ...JSON.parse(localStorage.getItem(SEC) ?? '{}') } } catch { return { night: true, sessions: true, commitments: true, usage: false, other: false } } })())
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
  const tone: Record<Group, string> = { waiting: 'var(--b-warn)', finished: 'var(--b-good)', working: 'var(--b-ring)', still: 'var(--text2)' }
  const groups = $derived(withMaster ? deskGroups(model) : (['waiting', 'finished', 'working', 'still'] as Group[]).map(g => [g, model.rows.filter(r => r.group === g)] as [Group, Row[]]).filter(([, rows]) => rows.length > 0))
  // Con un ok da dare le opzioni delle domande restano tonali, così sulla schermata c'è un solo bottone pieno.
  // Franz 23/10 19:34: gli ok che una sessione aperta chiede stanno nella sua card, non in schede a parte.
  const attached = $derived(attachedTasks(model.rows))
  // Strada B (Franz 23/10 23:08, mockup del 23/10): un lavoro di una sessione aperta sta nella sua scheda, come gli ok;
  // in Lavori restano quelli senza sessione, in una lista compatta (titolo breve, riferimento in mono, tasto piccolo).
  const placed = $derived(fy ? lavoriPlace(fy, new Set(model.rows.map(r => r.session.name)), attached) : null)
  const looseOk = $derived(placed?.loose.ok ?? [])
  const looseRows = $derived(placed ? [...placed.loose.ok, ...placed.loose.outside.slice(0, 2), ...placed.loose.claude.slice(0, 1)] : [])
  const fyShort = (r: LavoriRow) => shortTitle(r.title, r.agenda?.short ?? r.commitment?.short)
  function fyAct(r: LavoriRow) { if (r.kind === 'outside') fyOutside(r); else if (r.kind === 'claude' && r.agenda) onStart(doIt(r.agenda)) }
  const hasOk = $derived(looseOk.some(r => r.approval))
  const canExit = $derived(!!st.slash?.includes('exit'))
  let pending = $state<Approval | null>(null)
  let pendingChoice = $state<string | null>(null)
  let note = $state('')
  let approveDlg: HTMLDialogElement | undefined = $state()
  let confirming = $state<string | null>(null)
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

{#snippet sec(key: string, title: string, summary: string | null = null)}
  <button class="sec" aria-expanded={open[key]} onclick={() => toggleSec(key)}>
    <svg viewBox="0 0 24 24" width="16" height="16"><path d={open[key] ? 'M6 9l6 6 6-6' : 'M9 6l6 6-6 6'} fill="none" stroke="var(--icon)" stroke-width="2.4" stroke-linecap="round" /></svg>
    <span>{title.toUpperCase()}</span><i></i>{#if summary && !open[key]}<small>{summary}</small>{/if}
  </button>
{/snippet}

<section class="home">
  <!-- Contratto 1.83: dopo 120 s la domanda torna al terminale, quindi sta sopra a tutto il resto. -->
  {#each elicitations as e (e.id)}<ElicitationCard {e} {now} onAnswer={onElicit} />{/each}
  <!-- Franz 24/10 09:36: «L'ordine in home deve essere: Sessioni, Utilizzo, Lavoro, Altro»; la Notte, che non nomina, prima di Altro. -->
  {#if waitingYou.length}
  <button class="attn-banner" onclick={() => { if (!open.sessions) toggleSec('sessions'); onAttention() }}>
    <b>{t.attentionBanner(waitingYou.length)}</b><span>{[...new Set(waitingYou.map(a => a.session).filter(Boolean))].join(', ')}</span>
  </button>
{/if}
{@render sec('sessions', t.homeSessions, String(model.rows.length))}
  {#if justClosed}
    <div class="card just" style:view-transition-name="page-fly">
      <div class="jrow"><Badge s={justClosed.s} size={24} /><span class="jcol"><b>{justClosed.s.name}</b><small>{justClosed.line}</small></span></div>
    </div>
  {/if}
  {#if open.sessions}
  {#each groups as [group, rows] (group)}
    <h2 class="gh" style="--t:{tone[group]}"><span>{t.summary[group](rows.length).toUpperCase()}</span><i></i></h2>
    {#each rows as r (r.session.name)}
      {@const s = r.session}
      {@const x = text(r)}
      {@const open = expanded === s.name}
      {@const c = cleanupOf(s, canExit, st.ts)}
      {@const mine = placed?.inCards.get(s.name) ?? []}
      {@const svc = placedOut.inCards.get(s.name) ?? []}
      <div class="card" class:waiting={group === 'waiting'} class:on={selected.includes(s.name)} animate:flip={{ duration: 250 }}
        role="button" tabindex="0" onclick={() => { if (longFired) { longFired = false; return } onPick(s.name) }} onkeydown={(e) => e.key === 'Enter' && onPick(s.name)}
        oncontextmenu={(e) => { e.preventDefault(); holdEnd(); openCtx(s.name, e.clientX, e.clientY) }} onpointerdown={(e) => holdStart(s.name, e)} onpointerup={holdEnd} onpointerleave={holdEnd} onpointercancel={holdEnd}>
        {#if ctx === s.name}
          <button class="scrim" aria-label={t.closeWord} onclick={(e) => { e.stopPropagation(); ctx = null }}></button>
          <div class="cmenu" role="menu" tabindex="-1" style="left:{at.x}px; top:{at.y}px" onclick={(e) => e.stopPropagation()} onkeydown={(e) => { if (e.key === 'Escape') ctx = null; e.stopPropagation() }}>
            <div class="ph"><Badge {s} size={20} /><b>{s.name}</b></div>
            <button class="mi" role="menuitem" onclick={() => { onSessionCmd(s.name, s.followed ? 'unfollow' : 'follow'); ctx = null }}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d={s.followed ? 'M8.7 3A6 6 0 0 1 18 8a21 21 0 0 0 .6 5M17 17H3s3-2 3-9a4.7 4.7 0 0 1 .3-1.7M10.3 21a1.9 1.9 0 0 0 3.4 0M2 2l20 20' : 'M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.9 1.9 0 0 0 3.4 0'} /></svg>
              <span><span class="mt">{s.followed ? t.unfollow : t.follow}</span><span class="ms">{s.followed ? t.unfollowSub : t.followSub}</span></span>
            </button>
            {#if s.name !== MASTER && hasMaster}
              <button class="mi" role="menuitem" onclick={() => { onSessionCmd(s.name, 'prompt', t.askMasterPrompt(s.name)); ctx = null }}>
                <svg viewBox="0 0 24 24" width="22" height="22"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8" /></svg>
                <span><span class="mt">{t.askMaster}</span><span class="ms">{t.askMasterSub}</span></span>
              </button>
            {/if}
            {#if canExit && s.state !== 'gone'}
              <hr />
              <button class="mi danger" role="menuitem" onclick={() => { ctx = null; confirming = s.name }}>
                <svg viewBox="0 0 24 24" width="22" height="22"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg>
                <span><span class="mt">{t.menuExit}</span><span class="ms">{t.menuExitSub}</span></span>
              </button>
            {/if}
          </div>
        {/if}
        <div class="top">
          <Badge {s} size={24} />
          <!-- Il segno della modalità dei permessi dopo il nome, solo se diverso dal solito (mockup del 14/10, schermata 2). -->
          <span class="nm"><span class="name">{s.name}</span><PermGlyph g={homeGlyph(s)} /></span>
          {#if s.duplicate_of}<span class="dup">{t.cleanupDupTag.toUpperCase()}</span>{/if}
          {#if r.quota?.h5 != null}<span class="mono" class:stale={r.quota.stale}>{t.quota5h(r.quota.h5)}</span>{/if}
          <button class="chev" aria-label={open ? t.close : t.open} onclick={(e) => { e.stopPropagation(); expanded = open ? null : s.name }}>
            <svg viewBox="0 0 24 24" width="22" height="22"><path d={open ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
          </button>
        </div>
        {#if s.goal?.text}<div class="goal">{t.goal}: {s.goal.text}</div>{/if}
        {#if x.body}<p class="body" class:full={open} class:strong={group === 'waiting' || open}>{x.body}</p>{/if}
        {#if group === 'waiting' && !s.question}
          {@const d = doneOf(s)}
          <!-- Contratto 1.80: una ferma si chiude da qui col tasto «Fatto»; la scadenza di un needs-you accanto. -->
          {#if d}<div class="oks"><div class="ok"><span class="okt mono2">{s.needs_you?.expires_at ? expiryText(s.needs_you.expires_at) : ''}</span><button class="approve mini" onclick={(e) => { e.stopPropagation(); onDone(d) }}>{t.attentionDone}</button></div></div>{/if}
        {/if}
        {#if open}<div class="details">{[s.model?.label, s.effort, s.account].filter(Boolean).join(' · ')}</div>{/if}
        {#if r.approvals.length}
          <!-- Franz 23/10 19:34: gli ok del registro che questa sessione chiede, ognuno col suo Approva, qui e non in schede a parte. -->
          <div class="oks">
            {#each r.approvals as a (a.task)}
              <div class="ok" class:wrap={choices(a).length > 0}>
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="var(--advice)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76M9 12l2 2 4-4" /></svg>
                <span class="okt">{a.title}</span>
                {#if a.deploy}<span class="dup">{t.approvalProd.toUpperCase()}</span>{/if}
                {@render okButtons(a)}
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
          <Options q={s.question} onAnswer={(n) => onAnswer(s.name, n)} firstFilled={!hasOk} tonal="color-mix(in srgb, var(--primary) 12%, var(--surface))" />
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
            {#if closing === s.name}
              <span class="closing" role="status"><i class="spin" aria-hidden="true"></i>{t.splashClosing(s.name)}</span>
            {:else if c.canClose && confirming !== s.name}
              <button class="close" onclick={(e) => { e.stopPropagation(); confirming = s.name }}>
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="var(--b-alert)" stroke-width="2" stroke-linecap="round"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg>{t.cleanupClose(s.name)}
              </button>
            {/if}
          </div>
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
        {#if s.context != null}
          <!-- Grammatica unica (Franz 24/10 00:01): il contesto sta sulla barra, col valore in mono accanto; non in testa. -->
          <div class="trackrow"><div class="track"><div class="fill" style="width:{Math.min(100, s.context)}%;background:{ctxTone(s.context)}"></div></div><span class="ctxv">{t.ctx(s.context)}</span></div>
        {/if}
      </div>
    {/each}
  {/each}

  {/if}
  {#if usage && showUsage}
    <div bind:this={usageEl}>{@render sec('usage', t.homeUsage)}</div>
    {#if open.usage}<div class="footer" class:flash={usageFlash}>{@render usage()}</div>{/if}
  {/if}
{#if showLavori && fy}
  {@render sec('lavori', t.lavoriTitle, t.lavoriForYouN(fy.total))}
  {#if open.lavori}
    {#each looseRows as r, i (workKey(r, i))}
      {@const k = workKey(r, i)}
      {@const openW = openWork === k}
      {@const ds = lavoriDetails(r)}
      <div class="card">
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
          {#if r.kind === 'ok'}{#if r.approval}{@render okButtons(r.approval)}{/if}
          {:else}<button class="tonal mini" onclick={() => fyAct(r)}>{r.kind === 'outside' ? t.agendaDone : t.lavoriStart}</button>{/if}
        </div></div>
      </div>
    {/each}
    <button class="all" onclick={onLavori}><span>{lavoriLine}</span>
      <svg viewBox="0 0 24 24" width="20" height="20"><path d="M10 7l5 5-5 5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg></button>
  {/if}
{/if}
  {#if night !== undefined}
    {@render sec('night', t.homeNight)}
    {#if open.night}
      <!-- Grammatica unica (Franz 24/10 00:01): la Notte è una scheda come le altre: luna, «Stanotte», la finestra in mono, una riga di testo, «Apri» tonale. -->
      <button class="card ncard" onclick={onNight}>
        <div class="top">
          <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="12" fill="var(--b-track)" /><g transform="translate(4.8 4.8) scale(0.6)" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 15A8 8 0 0 1 9 4a8 8 0 1 0 11 11z" /></g></svg>
          <span class="nm"><span class="name">{t.catNight}</span></span>
          {#if night}<span class="mono">{hhmm(night.start)}–{hhmm(night.end)} · {night.cards.length}</span>{/if}
          <svg class="chevr" viewBox="0 0 24 24" width="22" height="22"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
        </div>
        {#if night}
          <p class="body">{t.nightCardBody(nightTitle, night.cards.length)}</p>
          {#if night.counts.done || night.counts.running || night.counts.partial || night.counts.stopped || night.counts.asking}
          <span class="nchips">
            {#if night.counts.done}<span style:color="var(--good)"><Icon name="check" size={14} />{t.nightDone(night.counts.done)}</span>{/if}
            {#if night.counts.running}<span style:color="var(--icon)"><Icon name="play" size={14} />{t.nightRunningN(night.counts.running)}</span>{/if}
            {#if night.counts.partial}<span style:color="var(--b-warn)"><Icon name="half" size={14} />{t.nightPartial(night.counts.partial)}</span>{/if}
            {#if night.counts.stopped}<span style:color="var(--gone-dim)"><Icon name="x" size={14} />{t.nightStopped(night.counts.stopped)}</span>{/if}
            {#if night.counts.asking}<span style:color="var(--advice)"><Icon name="question" size={14} />{t.nightAsking(night.counts.asking)}</span>{/if}
          </span>
          {/if}
          <div class="oks"><div class="ok"><span class="okt mono2">{night.needs.length ? t.homeNightNeeds(night.needs.length, night.needs[0].session, readable(night.needs[0].text, t.slashPhrases)) : t.nightNoNeeds}</span><span class="tonal mini">{t.homeNightOpen}</span></div></div>
        {:else}<p class="body">{t.nightLoading}</p>{/if}
      </button>
    {/if}
  {/if}
  {#if placedOut.groups.length}
    {@render sec('other', t.homeOther, outside.map(g => outsideCount(g)).join(' · '))}
  {/if}
  {#if placedOut.groups.length && open.other}
    <div class="outside">
      <div class="oh"><h3>{t.outsideTitle}</h3><i></i></div>
      <div class="osub">{t.outsideSub}</div>
    </div>
    {#each placedOut.groups as g (g.category)}
      <h2 class="gh cat" style="--t:{g.category === 'fix' ? 'var(--wait)' : 'var(--text2)'}">
        <svg viewBox="0 0 24 24" width="16" height="16"><path d={CAT_ICON[g.category]} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
        <span>{(g.category === 'night' ? catLabel(g.category) : t.catCount(catLabel(g.category), outsideCount(g))).toUpperCase()}</span>
        {#if g.category === 'resume'}<small class="note">{t.catResumeNote}</small>{/if}
      </h2>
      {#if g.category === 'closed'}
        <!-- Grammatica unica (Franz 24/10 00:01): ogni voce di Altro è una scheda della famiglia: badge col glifo della categoria, nome, meta in mono, testo, tasto in fondo. -->
        <button class="card ncard" onclick={() => closedDlg?.showModal()}>
          <div class="top">
            <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="12" fill="var(--b-track)" /><g transform="translate(4.8 4.8) scale(0.6)" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d={CAT_ICON.closed} /></g></svg>
            <span class="nm"><span class="name">{t.catClosed}</span></span>
            <span class="mono">{g.closed.length}</span>
            <svg class="chevr" viewBox="0 0 24 24" width="22" height="22"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
          </div>
          <p class="body">{g.closed.slice(0, 3).map(s => s.name).join(', ')}{g.closed.length > 3 ? ` ${t.closedMore(g.closed.length - 3)}` : ''}</p>
        </button>
      {:else}
        {#each g.rows as r, i}
          {#if r.kind === 'night'}
            <!-- La scheda Stanotte (mockup approvato l'08/10 alle 20:43): i lavori, quello in corso per primo, poi l'ultima notte. -->
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
          {:else}
            {@const x = serviceText(r)}
            <div class="card svc">
              <div class="top">
                <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="12" fill="var(--b-track)" /><g transform="translate(4.8 4.8) scale(0.6)" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d={CAT_ICON[g.category]} /></g></svg>
                <span class="nm"><span class="name">{r.kind === 'context' ? r.title : x.title}</span></span>
                {#if r.kind === 'context'}<span class="mono">{t.ctx(r.number ?? 0)}</span>{:else if r.at}<span class="mono">{hhmm(r.at)}</span>{/if}
              </div>
              {#if r.kind === 'context'}<p class="body">{x.title}. {x.detail}</p>{:else if x.detail}<p class="body">{x.detail}</p>{/if}
              <div class="oks"><div class="ok"><span class="okt mono2">{catLabel(g.category)}</span><button class="tonal mini" onclick={() => onService(r)}>{x.action}</button></div></div>
            </div>
          {/if}
        {/each}
      {/if}
    {/each}
  {/if}
  {#if footer}<div class="footer">{@render footer()}</div>{/if}
</section>

<!-- «Chiuse · N» (ClosedSheet dell'app): una riga per sessione chiusa, «Riapri» la riapre e chiude il foglio. -->
<dialog bind:this={closedDlg} class="sheet" onclick={(e) => e.target === e.currentTarget && closedDlg?.close()}>
  <h2 class="sh">{t.closedCat(model.closed.length)}</h2>
  {#each model.closed as c (c.name)}
    <div class="crow"><Badge s={c} size={24} /><b>{c.name}</b>
      {#if onReopen}<button class="obtn" onclick={() => { closedDlg?.close(); onReopen(c.name) }}>{t.reopen}</button>{/if}</div>
  {/each}
</dialog>

{#snippet okButtons(a: Approval)}
  {@const opts = choices(a)}
  {#if opts.length}<span class="opts">{#each opts as o (o)}<button class="tonal mini" onclick={(e) => { e.stopPropagation(); askApprove(a, o) }}>{o}</button>{/each}</span>
  {:else}<button class="approve mini" onclick={(e) => { e.stopPropagation(); askApprove(a) }}>{t.approve}</button>{/if}
{/snippet}

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
  .mi { display: flex; gap: 14px; align-items: flex-start; width: 100%; padding: 12px; border-radius: 14px; text-align: left; color: var(--text); }
  .mi:hover { background: var(--high); }
  .mi svg { flex: none; fill: none; stroke: var(--icon); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  .mi > span { display: flex; flex-direction: column; gap: 2px; }
  .mt { font-size: 15.5px; }
  .ms { font-size: 13px; color: var(--text2); }
  .mi.danger svg { stroke: var(--b-alert); }
  .attn-banner { display: flex; align-items: center; gap: 8px; width: 100%; text-align: left; padding: 10px 14px; border-radius: 16px; background: var(--b-warn); color: var(--b-warn-ink, #1d1300); font-size: 15px; cursor: pointer; }
  .attn-banner b { flex: none; font-weight: 600; }
  .attn-banner span { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
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
  .clean { display: flex; align-items: center; gap: 10px; font-size: 13.5px; color: var(--text2); }
  .clean span { flex: 1; min-width: 0; }
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
  .sec { display: flex; align-items: center; gap: 8px; width: 100%; margin: 14px 0 4px; padding: 0 4px; font: 500 12px/1 var(--mono); letter-spacing: .14em; color: var(--text2); text-align: left; }
  .sec i { flex: 1; height: 1px; background: var(--line); }
  .sec small { font: 13px/1 var(--sans, inherit); letter-spacing: 0; }
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
  .ok { display: flex; align-items: center; gap: 10px; }
  .ok svg { flex: none; }
  .okt.mono2 { font-family: ui-monospace, monospace; font-size: 13px; color: var(--text2); }
  .okt { flex: 1; min-width: 0; font-size: 14px; color: var(--text); display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
