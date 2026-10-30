<script lang="ts">
  import CloseDialog from './CloseDialog.svelte'
  import type { CmdOp, Model, Session, State } from './contract'
  import { modelKind, notes as notesOf, quotaLabel, resetLabel, sameModel, shortModel, tone, wider as widerOf, type QuotaForm, type Tone } from './header'
  import { MASTER } from './summary'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import SessionsMenu from './SessionsMenu.svelte'
  import PermGlyph from './PermGlyph.svelte'
  import Icon from './Icon.svelte'
  import { counts, inMenu, menuLine, MINUS, pill as changesOf } from './diff'
  import { canChange, choices as permChoices, glyph, needsConfirm } from './permissionModes'
  import { adviceOf, adviceSteps, ctxBand, handoffPrompt, tokens } from './masterService'

  // La testata della sessione (SheetHeader.kt): una riga con modello · effort in una pillola, la quota delle 5 ore con l'ora
  // dell'azzeramento, il contesto ad anello e il menu ⋮; sotto obiettivo, priorità e finestra. Su desktop a sinistra anche
  // badge e nome, come la plancia del tablet.
  let { st, s, wide, onBack, onCmd, onPrompt, onHandoff, onUsage = () => {}, onGo = null, onReopen = null, appMenu = null, onChanges = null, menuMove = null }: {
    /** La voce del menu ⋮ che sposta il pannello della master di lato, nella plancia (Franz 30/10 13:26). */
    menuMove?: { label: string; onMove: () => void } | null

    /** Sul telefono il menu delle sessioni («Vai a») sul nome e il ≡ dell'app, come la testata della sessione nell'app. */
    onGo?: ((name: string | null) => void) | null; onReopen?: ((name: string) => void) | null; appMenu?: import('svelte').Snippet | null
    /** `wide`: badge e nome a sinistra (la testata unica del desktop); in una colonna della plancia li ha la colonna. */
    st: State; s: Session; wide: boolean; onBack?: () => void
    onCmd: (op: CmdOp, arg?: string) => void; onPrompt: (text: string) => void
    /** Contratto 1.37: «Handoff, poi /clear»; lo gestisce App (prompt e /clear a turno finito). */
    onHandoff: () => void
    /** Il tocco sulla pillola della quota porta a Utilizzo (variante A, 09/10). */
    onUsage?: () => void
    /** Contratto 1.72: il tocco sulla pillola delle modifiche apre la vista «Modifiche»; null = il relay non ha l'op. */
    onChanges?: (() => void) | null
  } = $props()

  // La scelta resta in vista finché il PC non la conferma (Tune.model): qui finché si resta sulla sessione.
  let picked = $state<{ model?: Model; effort?: string; followed?: boolean }>({})
  $effect(() => { s.name; picked = {} })
  const model = $derived(picked.model ?? s.model)
  const effort = $derived(picked.effort ?? s.effort)
  const followed = $derived(picked.followed ?? !!s.followed)
  const tunable = $derived(!!st.choices && s.state !== 'gone')
  const quota = $derived(st.quota[s.account])
  const reset = $derived(resetLabel(quota?.reset_h5, st.ts))
  const notes = $derived(notesOf(s, t.notes))
  // Contratto 1.72: la pillola delle modifiche, solo con `changes`; anche su una sessione chiusa (la cartella c'è ancora).
  const changes = $derived(changesOf(s))
  // Sul telefono la pillola è la prima voce del menu ⋮, col puntino sul ⋮: la testata resta su una riga (Franz, 16/10 13:08).
  let viewport = $state(typeof window === 'undefined' ? 1280 : window.innerWidth)
  const changesMenu = $derived(!!changes && inMenu(viewport))
  const wider = $derived(widerOf({ ...s, model }, st.choices))
  const canExit = $derived(!!st.slash?.includes('exit') && s.state !== 'gone')
  // Contratto 1.37: il consiglio di fable-director, dentro il foglio; il puntino solo se la scelta attuale è diversa.
  const advice = $derived(adviceOf(s, st.choices, st.ts))
  // Tre pillole uguali (variante A, scelta da Franz il 09/10 alle 17:50): la quota prende la forma più lunga che ci sta,
  // «5h 4% · 20:00», «5h 4%» o «5h 4%» senza anello, invece di andare a capo. Come MeterPills nell'app la riga si misura:
  // le pillole accanto, le tre forme disegnate fuori vista, i margini della riga (padding 16 e spazi da 8; col tasto
  // indietro 2 più 36 e spazi da 6) e il menu da 44. Con soglie fisse a 430 px il ⋮ usciva dalla riga (12/10).
  let width = $state(0)
  // Sul telefono il tasto indietro e il nome in una riga sopra le pillole, come la barra in alto dell'app col nome della
  // sessione (15/10: si leggeva solo in «Scrivi a …»; nella riga delle pillole a 500 px restava «atlas-»).
  const top = $derived(!wide && !!onBack)
  const inRow = $derived(!!onBack && !top)
  let modelW = $state(0)
  let ctxW = $state(0)
  const formW = $state<Record<QuotaForm, number>>({ full: 0, short: 0, tiny: 0 })
  // Le larghezze intere di modello e contesto: misurate quando la pillola è nella forma lunga, così accorciarla non cambia
  // il conto che ha deciso di accorciarla.
  let modelFullW = $state(0)
  let ctxFullW = $state(0)
  let effW = $state(0)
  let ctxPctW = $state(0)
  let ctxLongW = $state(0)
  const fixed = $derived.by(() => {
    const items = (inRow ? 1 : 0) + 3 + (s.context != null ? 1 : 0) + 1
    return (inRow ? 2 + 36 : 16) + (items - 1) * (inRow ? 6 : 8) + modelFullW + (s.context != null ? ctxFullW : 0) + 44
  })
  // Franz, 24/10 18:23 e 18:58: «7 file · +161 −14» nella riga del modello quando ci sta, accorciando quello che serve:
  // prima la quota (forma corta, poi senza l'ora), poi la pillola senza effort, poi il contesto senza «ctx». Sulla colonna
  // larga il nome resta intero. Se non ci sta neanche così, nella sua riga sotto le pillole.
  let chW = $state(0)
  let nameW = $state(0)
  const chLevel = $derived.by(() => {
    if (!changes || changesMenu || width === 0 || chW === 0 || modelFullW === 0) return -1
    const q = quota?.h5 != null && width >= 300
    // 16 px di margine: le pillole accorciate non calano esattamente quanto il testo tolto, e la riga non deve sbordare.
    const base = fixed + (wide ? 26 + 12 + nameW : 0) + (inRow ? 6 : 8) + chW + 16
    const cut = (f: QuotaForm, eff: boolean, pct: boolean) => base + (q ? formW[f] : 0) - (eff ? effW : 0) - (pct && s.context != null ? Math.max(0, ctxLongW - ctxPctW) : 0)
    const steps: [QuotaForm, boolean, boolean][] = [['full', false, false], ['short', false, false], ['tiny', false, false], ['tiny', true, false], ['tiny', true, true]]
    return steps.findIndex(([f, e, c]) => cut(f, e, c) <= width)
  })
  const chInline = $derived(chLevel >= 0)
  const noEffort = $derived(chLevel >= 3)
  const ctxPct = $derived(chLevel >= 4)
  $effect(() => { if (!noEffort && modelW) modelFullW = modelW })
  $effect(() => { if (!ctxPct && !compact && ctxW) ctxFullW = ctxW })
  const quotaForm = $derived.by<QuotaForm>(() => {
    if (width === 0) return 'full'
    // Col chip nella riga la quota è quella scelta insieme al chip, non una più lunga.
    if (chInline) return (['full', 'short'] as const)[chLevel] ?? 'tiny'
    return (['full', 'short'] as const).find(f => fixed + formW[f] <= width) ?? 'tiny'
  })
  const forms: QuotaForm[] = ['full', 'short', 'tiny']
  // Una colonna molto stretta: «62%» senza «ctx», così il menu ⋮ resta in vista.
  const compact = $derived(width > 0 && width < 300)
  const ring: Record<Tone, string> = { neutral: 'var(--b-ring)', warn: 'var(--b-warn)', alert: 'var(--b-alert)' }
  const arc = (pct: number) => `${Math.max(0, Math.min(100, pct)) * 0.4712} 100`

  let menu = $state(false)
  // La scheda dei permessi a pressione lunga sulla pillola (PermissionUi dell'app): segno, nome e spiegazione.
  let permTip = $state(false)
  let held = false
  let holdTimer: ReturnType<typeof setTimeout> | undefined
  let tune: HTMLDialogElement | undefined = $state()
  let ctx: HTMLDialogElement | undefined = $state()
  let closer: CloseDialog | undefined = $state()
  function close(d?: HTMLDialogElement) { d?.close() }
  function setModel(m: Model) { picked.model = m; onCmd('model', m.id); close(tune); close(ctx) }
  function setEffort(e: string) { picked.effort = e; onCmd('effort', e); close(tune) }
  // Contratto 1.69: più autonomia chiede un secondo tocco; toglierne parte subito.
  let armed = $state<string | null>(null)
  const permName = (m: string | null | undefined) => t.perm.names[m || 'default'] ?? m ?? ''
  const permTitle = $derived(`${permName(s.permission_mode)} · ${t.perm.subs[s.permission_mode || 'default'] ?? t.perm.unknown}`)
  function setPermission(m: string) {
    if (m === (s.permission_mode || 'default')) return
    if (needsConfirm(s.permission_mode, m) && armed !== m) { armed = m; return }
    armed = null; onCmd('permission_mode', m); close(tune)
  }
  // Il pannello scende dalla pillola: sul telefono largo quanto lo schermo meno 12 px per lato, più largo 380 px sotto la
  // pillola, dentro la finestra (come il menu dell'app, AppBar).
  let pill: HTMLButtonElement | undefined = $state()
  let tunePos = $state('')
  function openTune() {
    const r = pill?.getBoundingClientRect()
    const vw = window.innerWidth
    const top = Math.round((r?.bottom ?? 48) + 6)
    const w = vw < 600 ? vw - 24 : 380
    const left = vw < 600 ? 12 : Math.round(Math.max(12, Math.min((r?.left ?? 12) - 8, vw - w - 12)))
    tunePos = `top:${top}px;left:${left}px;width:${w}px;max-height:calc(100dvh - ${top + 16}px)`
    tune?.showModal()
    // Il fuoco sul pannello, non sulla ✕: l'anello attorno alla ✕ sembrava una scelta.
    tune?.querySelector<HTMLElement>('.tpi')?.focus()
  }
  // «Usa il consiglio»: solo quello che cambia, con l'id della lista; poi il pannello si chiude.
  function useAdvice() {
    if (!advice) return
    const go = adviceSteps(advice, st.choices, model?.id, effort)
    if (go.model) { picked.model = go.model; onCmd('model', go.model.id) }
    if (go.effort) { picked.effort = go.effort; onCmd('effort', go.effort) }
    close(tune)
  }
  // Clic sul fondo del dialogo: si chiude, come il foglio dell'app.
  const backdrop = (e: MouseEvent) => { if (e.target === e.currentTarget) (e.currentTarget as HTMLDialogElement).close() }
</script>

<svelte:window bind:innerWidth={viewport} onkeydown={(e) => e.key === 'Escape' && (menu = false)} />

<header bind:clientWidth={width} class:bare={!notes.length && (!changes || changesMenu || chInline)}>
  {#if top}
    <div class="row top">
      <button class="ib back" onclick={onBack} aria-label={t.back}><svg viewBox="0 0 24 24" width="22" height="22"><path d="M15 6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
      {#if onGo}<SessionsMenu {st} {s} now={st.ts} {onGo} {onReopen} />{:else}<span class="lead"><Badge {s} size={26} /><h1>{s.name}</h1></span>{/if}
      {#if appMenu}{@render appMenu()}{/if}
    </div>
  {/if}
  <div class="row">
    {#if inRow}<button class="ib back" onclick={onBack} aria-label={t.back}><svg viewBox="0 0 24 24" width="22" height="22"><path d="M15 6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>{/if}
    {#if wide}
      <span class="lead"><Badge {s} size={26} /><h1>{s.name}</h1></span>
    {/if}
    <button class="pill" bind:this={pill} bind:offsetWidth={modelW} disabled={!tunable} onclick={() => { if (held) { held = false; return } openTune() }}
      oncontextmenu={(e) => { if (glyph(s.permission_mode) !== 'none') { e.preventDefault(); permTip = true } }}
      onpointerdown={(e) => { if (e.pointerType !== 'mouse' && glyph(s.permission_mode) !== 'none') holdTimer = setTimeout(() => { held = true; permTip = true }, 500) }}
      onpointerup={() => clearTimeout(holdTimer)} onpointerleave={() => clearTimeout(holdTimer)}>
      <span class="lbl">{[shortModel(model) ?? t.modelTitle, noEffort ? null : effort].filter(Boolean).join(' · ')}</span>
      <!-- Il segno della modalità dei permessi (mockup del 14/10); il nome e la spiegazione passandoci sopra. -->
      {#if glyph(s.permission_mode) !== 'none'}<span class="pm" title={permTitle}><PermGlyph g={glyph(s.permission_mode)} /></span>{/if}
      {#if tunable}<svg viewBox="0 0 24 24" width="18" height="18"><path d="M7 10l5 5 5-5z" fill="var(--text2)" /></svg>{/if}
      {#if tunable && advice?.dot}<span class="dot" title={t.adviceDot} aria-label={t.adviceDot}></span>{/if}
    </button>
    {#if permTip}
      <button class="tipscrim" aria-label={t.closeWord} onclick={() => (permTip = false)}></button>
      <div class="permtip" role="tooltip"><PermGlyph g={glyph(s.permission_mode)} /><span><b>{permName(s.permission_mode)}</b><small>{t.perm.subs[s.permission_mode || 'default'] ?? t.perm.unknown}</small></span></div>
    {/if}
    {#if chInline && changes}{@render changesPill(changes)}{/if}
    {#if wide}<span class="ghost" aria-hidden="true"><span class="gname" bind:offsetWidth={nameW}>{s.name}</span></span>{/if}
    <span class="ghost" aria-hidden="true"><span class="gl" bind:offsetWidth={effW}>{effort ? ` · ${effort}` : ''}</span>{#if s.context != null}<span class="gl" bind:offsetWidth={ctxLongW}>{t.ctx(s.context)}</span><span class="gl" bind:offsetWidth={ctxPctW}>{`${s.context}%`}</span>{/if}</span>
    {#if !wide}<span class="sp"></span>{/if}
    <!-- In una colonna stretta la quota lascia il posto a modello, contesto e menu (è anche nella home). -->
    {#if quota?.h5 != null && (width === 0 || width >= 300)}
      {@const bare = quotaForm === 'tiny'}
      <!-- Nella forma più corta l'anello lascia il posto a «5h», e il colore delle soglie passa al testo. -->
      <button class="mpill" class:bare title={reset ? t.quotaResetDesc(quota.h5, reset) : undefined} onclick={onUsage}>
        {#if !bare}<svg viewBox="0 0 20 20" width="16" height="16"><circle cx="10" cy="10" r="7.5" class="trk" /><circle cx="10" cy="10" r="7.5" pathLength="47.12" stroke-dasharray={arc(quota.h5)} style="stroke:{quota.stale ? 'var(--wait)' : ring[tone(quota.h5)]}" class="arc" /></svg>{/if}
        <span class:stale={quota.stale} style:color={bare && !quota.stale && tone(quota.h5) !== 'neutral' ? ring[tone(quota.h5)] : null}>{quotaLabel(quotaForm, t.quota5h(quota.h5), reset)}</span>
      </button>
      <span class="ghost" aria-hidden="true">
        {#each forms as f}
          <span class="mpill" class:bare={f === 'tiny'} bind:offsetWidth={formW[f]}>{#if f !== 'tiny'}<svg width="16" height="16"></svg>{/if}<span>{quotaLabel(f, t.quota5h(quota.h5), reset)}</span></span>
        {/each}
      </span>
    {/if}
    {#if s.context != null}
      <button class="mpill" bind:offsetWidth={ctxW} disabled={!tunable} onclick={() => ctx?.showModal()}>
        <svg viewBox="0 0 20 20" width="16" height="16"><circle cx="10" cy="10" r="7.5" class="trk" /><circle cx="10" cy="10" r="7.5" pathLength="47.12" stroke-dasharray={arc(s.context)} style="stroke:{ring[tone(s.context)]}" class="arc" /></svg>
        <span>{compact || ctxPct ? `${s.context}%` : t.ctx(s.context)}</span>
      </button>
    {/if}
    <span class="anchor">
      <button class="ib" aria-label={t.more} aria-expanded={menu} onclick={() => (menu = !menu)}>
        <svg viewBox="0 0 24 24" width="22" height="22"><g fill="var(--text2)"><circle cx="12" cy="5" r="2" /><circle cx="12" cy="12" r="2" /><circle cx="12" cy="19" r="2" /></g></svg>
        {#if changesMenu}<span class="mdot" aria-hidden="true"></span>{/if}
      </button>
      {#if menu}
        <button class="scrim" aria-label={t.closeWord} onclick={() => (menu = false)}></button>
        <div class="panel v3" role="menu">
          <div class="ph"><Badge {s} size={20} /><b>{s.name}</b></div>
          {#if changesMenu && changes}
            <button class="mi" role="menuitem" disabled={!onChanges} onclick={() => { menu = false; onChanges?.() }}>
              <Icon name="diff" size={22} color="var(--icon)" />
              <span><span class="mt">{menuLine(t.changesTitle, t.changesFiles(changes.files), changes)}</span><span class="msub">{t.changesOpen}</span></span>
            </button>
          {/if}
          <button class="mi" role="menuitem" onclick={() => { picked.followed = !followed; onCmd(followed ? 'unfollow' : 'follow'); menu = false }}>
            <svg viewBox="0 0 24 24" width="22" height="22"><path d={followed ? 'M8.7 3A6 6 0 0 1 18 8a21 21 0 0 0 .6 5M17 17H3s3-2 3-9a4.7 4.7 0 0 1 .3-1.7M10.3 21a1.9 1.9 0 0 0 3.4 0M2 2l20 20' : 'M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.9 1.9 0 0 0 3.4 0'} /></svg>
            <span><span class="mt">{followed ? t.unfollow : t.follow}</span><span class="msub">{followed ? t.unfollowSub : t.followSub}</span></span>
          </button>
          {#if s.name !== MASTER && st.sessions.some(x => x.name === MASTER && x.state !== 'gone')}
            <button class="mi" role="menuitem" onclick={() => { onCmd('prompt', t.askMasterPrompt(s.name)); menu = false }}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8" /></svg>
              <span><span class="mt">{t.askMaster}</span><span class="msub">{t.askMasterSub}</span></span>
            </button>
          {/if}
          {#if s.link?.trim()}
            <a class="mi" role="menuitem" href={s.link} target="_blank" rel="noopener" onclick={() => (menu = false)}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /></svg>
              <span><span class="mt">{t.openInClaude}</span><span class="msub">{t.openInClaudeSub}</span></span>
            </a>
          {/if}
          {#if menuMove}
            <button class="mi" role="menuitem" onclick={() => { menu = false; menuMove.onMove() }}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 7l-5 5 5 5M16 7l5 5-5 5M3 12h18" /></svg>
              <span><span class="mt">{menuMove.label}</span></span>
            </button>
          {/if}
          {#if canExit}
            <hr />
            <button class="mi danger" role="menuitem" onclick={() => { menu = false; closer?.ask(s) }}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg>
              <span><span class="mt">{t.menuExit}</span><span class="msub">{t.menuExitSub}</span></span>
            </button>
          {/if}
        </div>
      {/if}
    </span>
  </div>
  <!-- Contratto 1.72: nella riga delle pillole quando ci sta (chInline), altrimenti in una riga sua sotto. -->
  {#if changes && !changesMenu && !chInline}<div class="chrow">{@render changesPill(changes)}</div>{/if}
  {#each notes as n}<div class="note">{n}</div>{/each}
</header>

{#snippet changesPill(c: NonNullable<typeof changes>)}
  <button class="cpill" bind:offsetWidth={chW} disabled={!onChanges} title={t.changesOpen} aria-label={`${t.changesOpen}: ${t.changesFiles(c.files)} · ${counts(c.added, c.removed)}`} onclick={() => onChanges?.()}>
    <Icon name="diff" size={16} color="var(--icon)" />
    <span>{t.changesFiles(c.files)} · <b class="plus">+{c.added}</b> <b class="minus">{MINUS}{c.removed}</b></span>
  </button>
{/snippet}

<!-- Il pannello Modello ed effort (bozza approvata da Franz il 07/10 alle 00:06, docs/mockup/2026-10-06-modello-effort/):
     come il menu dell'app, scende dalla pillola; in testa il consiglio sul compito, poi i modelli con la riga che li spiega
     e l'effort come gruppo di tasti. -->
<dialog bind:this={tune} class="tunep" style={tunePos} onclick={backdrop} aria-label={t.tuneHead}>
  <div class="tpi" tabindex="-1">
    <div class="tph"><span>{t.tuneHead}</span><button class="ib" aria-label={t.closeWord} onclick={() => close(tune)}><svg viewBox="0 0 24 24" width="22" height="22"><path d="M6 6l12 12M18 6 6 18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg></button></div>
    {#if advice}
      {@const am = st.choices?.models.find(m => sameModel(m.id, advice.model))}
      {@const label = `${am?.label ?? shortModel(am) ?? advice.model} · ${advice.effort}`}
      {@const steps = adviceSteps(advice, st.choices, model?.id, effort)}
      {@const same = !steps.model && !steps.effort}
      <!-- Consiglio uguale alla scelta: verde con ✓, una conferma e non un suggerimento (Franz, 07/10 21:21). -->
      <div class="adv" class:same>
        <div class="advr">
          <span class="circ adc">{#if same}<svg viewBox="0 0 24 24" width="22" height="22"><path d="M20 6 9 17l-5-5" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>{:else}<svg viewBox="0 0 24 24" width="22" height="22"><path d="M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z" fill="currentColor" /></svg>{/if}</span>
          <span class="tx"><span class="ov">{t.adviceHead}</span><span class="at">{same ? t.adviceSame(label) : label}</span>{#if advice.reason?.trim()}<span class="aw">{advice.reason}</span>{/if}</span>
        </div>
        {#if !same && advice.cost}
          <p class="cost"><svg viewBox="0 0 24 24" width="16" height="16"><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{t.adviceCost(tokens(advice.cost))}</p>
        {/if}
        {#if !same}<button class="use" onclick={useAdvice}>{t.adviceUse}</button>{/if}
      </div>
    {/if}
    <p class="grp">{t.modelTitle}</p>
    <div role="radiogroup" aria-label={t.modelTitle}>
      {#each st.choices?.models ?? [] as m}
        {@const label = m.label ?? shortModel(m) ?? m.id}
        {@const on = sameModel(m.id, model?.id)}
        {@const kind = modelKind(m.id)}
        <button class="tr" class:on role="radio" aria-checked={on} onclick={() => setModel(m)}>
          <span class="circ">{label.charAt(0).toUpperCase()}</span>
          <span class="tx">
            <span class="tt">{label}{#if !on && advice && sameModel(m.id, advice.model)}<span class="rec">{t.adviceTag}</span>{/if}</span>
            {#if kind}<span class="ts">{t.modelSub[kind]}</span>{/if}
          </span>
          {#if on}<svg class="ok" viewBox="0 0 24 24" width="24" height="24"><path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" /></svg>{/if}
        </button>
      {/each}
    </div>
    <p class="grp">{t.effortTitle}</p>
    <div class="bgp" role="radiogroup" aria-label={t.effortTitle}>
      {#each st.choices?.efforts ?? [] as e}
        <button class:on={e === effort} role="radio" aria-checked={e === effort} style:flex-grow={e.length + 2} onclick={() => setEffort(e)}>{e}{#if e !== effort && advice?.effort === e}<i title={t.adviceTag}></i>{/if}</button>
      {/each}
    </div>
    {#if effort}<p class="ed">{[effort, t.effortSub[effort]].filter(Boolean).join(' · ')}</p>{/if}
    {#if canChange(st, s)}
      <p class="grp">{t.perm.title}</p>
      <div class="perms" role="radiogroup" aria-label={t.perm.title}>
        {#each permChoices(s) as m}
          {@const on = m === (s.permission_mode || 'default')}
          <button class:on class:armed={armed === m} role="radio" aria-checked={on} title={t.perm.subs[m]} onclick={() => setPermission(m)}>
            <PermGlyph g={glyph(m)} /><span>{permName(m)}</span>
          </button>
        {/each}
      </div>
      {#if armed}<p class="pconf">{t.perm.confirm(permName(armed))}</p>{/if}
      <p class="ed">{t.perm.note}</p>
    {/if}
  </div>
</dialog>

<dialog bind:this={ctx} onclick={backdrop}>
  {#if s.context != null}
    <h3>{t.ctxTitle(s.context)}</h3>
    <div class="col">
      {#if handoffPrompt(st, s, () => '') != null}<button class:filled={ctxBand(s.context) != null} class="tonal opt" onclick={() => { onHandoff(); close(ctx) }}>{t.ctxHandoffClear}<small>{t.ctxHandoffClearSub}</small></button>{/if}
      <button class="tonal opt" onclick={() => { onPrompt('/compact'); close(ctx) }}>{t.ctxCompact}<small>{t.ctxCompactSub}</small></button>
      {#if wider}<button class="tonal opt" onclick={() => setModel(wider!)}>{t.ctxWider}<small>{t.ctxWiderSub}</small></button>{/if}
    </div>
  {/if}
</dialog>

<CloseDialog bind:this={closer} onConfirm={() => onCmd('slash', 'exit')} />

<style>
  header { border-bottom: 1px solid var(--line); padding: 2px 0 6px; background: var(--bg); }
  /* Senza note sotto le pillole lo stesso margine di sopra, così stanno al centro della barra, come nell'app (Franz, 10/10
     14:32; sulla web 12/10 11:11: «dovrebbe essere un po' meno alta»). */
  header.bare { padding-bottom: 2px; }
  /* Franz 24/10 18:58: «header troppo alto»: la riga delle pillole da 48 a 42 px, le pillole restano alte 31-33. */
  .row { display: flex; align-items: center; gap: 8px; padding: 0 0 0 16px; min-height: 42px; }
  .row:has(.back) { padding-left: 2px; gap: 6px; }
  .lead { flex: 1; min-width: 0; display: flex; align-items: center; gap: 12px; }
  .row.top { padding-left: 2px; gap: 6px; min-height: 44px; }
  .row.top .lead { gap: 10px; }
  h1 { font-size: 19px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .sp { flex: 1; }
  .ib { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); flex: none; }
  .ib:hover { background: var(--surface); }
  .back { color: var(--text); width: 36px; }
  .pill { display: flex; align-items: center; min-width: min-content; overflow: hidden; background: var(--surface); border-radius: 999px; height: 31px; padding: 0 6px 0 12px; font-size: 13px; font-weight: 500; white-space: nowrap; flex: 0 1 auto; }
  /* Le pillole di quota e contesto, alte e larghe come quella del modello (variante A, 09/10). */
  .mpill { display: flex; align-items: center; gap: 5px; background: var(--surface); border-radius: 999px; height: 33px; padding: 0 10px 0 8px; font-size: 13px; font-weight: 500; white-space: nowrap; flex: none; color: var(--text); }
  /* Il testo al centro sulle maiuscole e le cifre, non sulla riga del carattere: con Roboto a 13 px stavano 1 px sopra il
     centro, e la barra sembrava avere più spazio sotto le pillole che sopra (Franz, 12/10 15:19). */
  .lbl, .mpill > span { text-box: trim-both cap alphabetic; }
  .mpill.bare { padding: 0 10px; }
  /* Le tre forme della quota fuori vista, misurate per scegliere quella che ci sta: niente spazio nella riga, niente tocco. */
  .ghost { position: absolute; width: 0; height: 0; overflow: hidden; visibility: hidden; display: flex; }
  .mpill:not(:disabled):hover { filter: brightness(1.2); }
  .mpill:disabled { cursor: default; }
  .mpill .stale { color: var(--wait); }
  .pill { position: relative; }
  .tipscrim { position: fixed; inset: 0; z-index: 30; background: transparent; }
  .row { position: relative; }
  .permtip { position: absolute; left: 12px; top: 42px; z-index: 31; width: min(320px, calc(100vw - 24px)); display: flex; gap: 12px; align-items: flex-start; padding: 14px 16px; border-radius: 16px; background: var(--high); box-shadow: 0 8px 28px rgb(0 0 0 / .5); }
  .permtip span { display: flex; flex-direction: column; gap: 4px; }
  .permtip small { color: var(--text2); font-size: 13.5px; line-height: 1.4; }
  .pill svg { flex: none; }
  /* Il puntino del consiglio, in alto a destra della pillola (mockup 1.37). */
  .dot { position: absolute; top: 2px; right: 2px; width: 9px; height: 9px; border-radius: 50%; background: var(--advice); border: 2px solid var(--bg); }
  /* Il pannello Modello ed effort, come il menu dell'app (AppBar): angoli da 28, voci da 64 con il cerchio da 40. */
  dialog.tunep { position: fixed; inset: auto; margin: 0; padding: 0; max-width: none; border-radius: 28px; box-shadow: 0 12px 40px rgb(0 0 0 / .5); overflow-y: auto; transform-origin: top left; animation: pop .2s cubic-bezier(.2, .8, .2, 1); }
  dialog.tunep::backdrop { animation: fade .15s; }
  @media (max-width: 599px) { dialog.tunep { margin: 0; border-radius: 28px; } }
  @media (prefers-reduced-motion: reduce) { dialog.tunep, dialog.tunep::backdrop { animation: none; } }
  @keyframes pop { from { opacity: 0; scale: .96; } }
  @keyframes fade { from { opacity: 0; } }
  .tpi { padding: 8px 0 14px; outline: none; }
  .tph { display: flex; align-items: center; min-height: 48px; padding: 0 2px 0 20px; font-size: 14px; color: var(--text2); }
  .tph span { flex: 1; }
  .circ { width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; flex: none; background: var(--high); color: var(--icon); font-size: 17px; font-weight: 600; }
  .tx { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; text-align: left; }
  /* Il consiglio sul compito: fondo lilla tenue, il motivo sotto, il costo e «Usa il consiglio» solo se è diverso. */
  .adv { margin: 2px 12px 4px; padding: 12px 14px 14px; border-radius: 20px; background: color-mix(in srgb, var(--advice) 10%, transparent); }
  .advr { display: flex; gap: 14px; align-items: flex-start; }
  .adc { background: color-mix(in srgb, var(--advice) 22%, transparent); color: var(--advice); }
  .ov { font: 600 11px/1.4 var(--mono); letter-spacing: .08em; text-transform: uppercase; color: var(--advice); }
  .adv.same { background: color-mix(in srgb, var(--good) 10%, transparent); }
  .adv.same .adc { background: color-mix(in srgb, var(--good) 22%, transparent); color: var(--good); }
  .adv.same .ov { color: var(--good); }
  .at { font-size: 16.5px; font-weight: 500; }
  .aw { font-size: 13.5px; line-height: 1.35; color: var(--text2); }
  .cost { display: flex; gap: 8px; align-items: flex-start; margin: 10px 0 0 54px; font-size: 13px; line-height: 1.35; color: var(--b-warn); }
  .cost svg { flex: none; margin-top: 1px; }
  .use { margin: 12px 0 0 54px; padding: 9px 18px; border-radius: 999px; font-size: 14.5px; font-weight: 500; color: color-mix(in srgb, var(--advice) 30%, white); background: color-mix(in srgb, var(--advice) 24%, transparent); }
  .use:hover { filter: brightness(1.15); }
  .grp { margin: 16px 20px 6px; font: 500 12px/1 var(--mono); letter-spacing: .14em; text-transform: uppercase; color: var(--text2); }
  .tr { display: flex; align-items: center; gap: 16px; width: 100%; min-height: 64px; padding: 10px 20px; }
  .tr:hover, .tr.on { background: var(--high); }
  .tr.on .circ { background: color-mix(in srgb, #4C7DFF 25%, transparent); }
  .tt { display: flex; align-items: center; gap: 10px; font-size: 16px; font-weight: 500; white-space: nowrap; }
  .ts { font-size: 12.5px; color: var(--text2); white-space: nowrap; overflow: hidden; }
  .ok { flex: none; color: var(--icon); }
  .rec { font: 600 11px/1.4 var(--mono); letter-spacing: .06em; text-transform: uppercase; color: var(--advice); background: color-mix(in srgb, var(--advice) 14%, transparent); border-radius: 999px; padding: 3px 9px; }
  /* L'effort come gruppo di tasti connessi (ButtonGroup dell'app): la scelta si arrotonda tutta. */
  .bgp { display: flex; gap: 2px; margin: 4px 20px 0; }
  /* Larghi quanto la loro parola (Franz, 07/10 21:16: «medium» usciva dal suo tasto a larghezze uguali). */
  .bgp button { position: relative; flex: 1 1 0; min-width: 0; height: 44px; border-radius: 8px; background: var(--high); font-size: 14px; font-weight: 500; transition: border-radius .2s cubic-bezier(.2, .8, .2, 1), background-color .2s; }
  .bgp button:first-child { border-radius: 22px 8px 8px 22px; }
  .bgp button:last-child { border-radius: 8px 22px 22px 8px; }
  .bgp button.on { border-radius: 22px; background: var(--primary); color: var(--on-primary); }
  .bgp button:not(.on):hover { filter: brightness(1.2); }
  .bgp i { position: absolute; top: 6px; right: 7px; width: 7px; height: 7px; border-radius: 50%; background: var(--advice); }
  .ed { margin: 10px 20px 0; font-size: 13px; color: var(--text2); }
  .opt { display: flex; flex-direction: column; align-items: flex-start; text-align: left; }
  .opt small { font-size: 12.5px; font-weight: 400; opacity: .8; margin-top: 2px; }
  .pill:disabled { padding-right: 12px; cursor: default; }
  .pm { display: inline-flex; margin-left: 4px; }
  /* Il gruppo Permessi (mockup del 14/10): tre per riga, la scelta piena, il tocco che chiede conferma col bordo ambra. */
  .perms { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin: 4px 20px 0; }
  .perms button { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; min-height: 64px; padding: 6px; border-radius: 12px; background: var(--high); font-size: 13px; text-align: center; border: 2px solid transparent; }
  .perms button.on { background: var(--primary); color: var(--on-primary); }
  .perms button.armed { border-color: var(--b-warn); }
  .perms button:not(.on):hover { filter: brightness(1.2); }
  .pconf { margin: 10px 20px 0; font-size: 14px; color: var(--b-warn); }
  .pill:not(:disabled):hover { filter: brightness(1.2); }
  .trk, .arc { fill: none; stroke-width: 3; }
  .trk { stroke: var(--b-track); }
  .arc { stroke-linecap: round; transform: rotate(-90deg); transform-origin: center; }
  .note { padding: 0 16px; font-size: 12px; font-weight: 500; color: var(--b-label); letter-spacing: .03em; }
  .chrow { display: flex; padding: 4px 0 6px 16px; }
  .mdot { position: absolute; top: 9px; right: 9px; width: 9px; height: 9px; border-radius: 50%; background: var(--icon); border: 2px solid var(--bg); }
  .gname { font-size: 19px; font-weight: 600; white-space: nowrap; }
  .gl { font-size: 13px; font-weight: 500; white-space: pre; }
  .cpill { flex: none; display: flex; align-items: center; gap: 6px; height: 33px; padding: 0 12px 0 10px; border-radius: 999px; background: var(--surface); font-size: 13px; font-weight: 500; white-space: nowrap; color: var(--text); }
  .cpill:not(:disabled):hover { filter: brightness(1.2); }
  .cpill:disabled { cursor: default; }
  .cpill b { font-weight: 500; }
  .plus { color: var(--b-good); }
  .minus { color: var(--b-alert); }
  .anchor { position: relative; }
  .scrim { position: fixed; inset: 0; z-index: 9; cursor: default; }
  /* Il menu a pannello dell'app (AppMenuPanel): 380 px su desktop, voci con la spiegazione sotto. */
  .panel { position: absolute; right: 4px; top: 46px; z-index: 10; width: min(380px, calc(100vw - 24px)); background: var(--surface); border-radius: 20px; padding: 8px; box-shadow: 0 12px 40px rgb(0 0 0 / .5); border: 1px solid var(--line); }
  .ph { display: flex; align-items: center; gap: 12px; padding: 10px 12px 12px; font-size: 16px; }
  .mi { display: flex; gap: 14px; align-items: flex-start; width: 100%; padding: 12px; border-radius: 14px; text-align: left; color: var(--text); text-decoration: none; }
  .mi:hover { background: var(--high); }
  .mi svg { flex: none; fill: none; stroke: var(--icon); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  .mi > span { display: flex; flex-direction: column; gap: 2px; }
  .mt { font-size: 15.5px; }
  .msub { font-size: 13px; color: var(--text2); }
  .danger svg { stroke: var(--b-alert); }
  .danger .mt { color: var(--b-alert); }
  hr { border: 0; height: 1px; background: var(--line); margin: 4px 8px; }
  /* Il menu ⋮ nella forma della v3 (app-menu-v3-30-10): icone grigie nel binario, una voce per riga col filo. */
  .panel { background: var(--s1); border: 1px solid var(--line); border-radius: 14px; padding: 0; overflow: hidden; }
  .ph { min-height: 54px; padding: 0 12px 0 16px; border-bottom: 1px solid var(--line); font: 600 17px var(--display); }
  .mi { align-items: center; gap: 8px; min-height: 58px; padding: 9px 12px 9px 0; border-radius: 0; border-top: 1px solid var(--line); }
  .ph + .mi { border-top: 0; }
  .mi:hover { background: var(--s2); }
  .mi > :global(svg) { margin: 0 15px; stroke: var(--tx2); }
  .mt { font: 600 17px/1.2 var(--display); }
  .msub { font-size: 14px; line-height: 1.3; color: var(--tx2); }
  hr { display: none; }
  /* I fogli: in basso sul telefono, una scheda al centro da 600 px (fogli adattivi dell'app). */
  dialog { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 20px 24px; width: min(560px, 100vw); max-height: 85vh; border-radius: 28px; }
  dialog::backdrop { background: rgb(0 0 0 / .55); }
  dialog :focus-visible { outline: 2px solid var(--icon); outline-offset: 2px; }
  @media (max-width: 599px) { dialog { margin: auto 0 0; max-width: 100vw; border-radius: 28px 28px 0 0; } }
  dialog h3 { font-size: 22px; font-weight: 600; margin: 12px 0 4px; }
  dialog h3:first-child { margin-top: 0; }
  .col { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
  .tonal { background: var(--high); border-radius: 999px; padding: 12px 20px; font-weight: 500; }
  .tonal.filled { background: var(--primary); color: var(--on-primary); }
  .tonal:hover { filter: brightness(1.12); }
</style>
