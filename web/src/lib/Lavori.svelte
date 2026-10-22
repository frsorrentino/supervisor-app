<script lang="ts">
  // La pagina unica «Lavori» (mockup approvato da Franz il 22/10, LavoriScreen.kt): al posto di Registro, Recap e della
  // sezione Impegni della home. Quattro blocchi per domanda: Per te (ognuno col suo tasto), In corso (un compito per riga
  // con la barra dei sette giorni), Stanotte (la coda, solo qui), Fatto (per giorno, con la prova). La quota resta in
  // «Utilizzo e limiti». Un solo bottone pieno: «Approva» (o «Aggiungi» quando non c'è un ok da dare).
  import { locale } from './lang'
  import type { AgendaPage, Approval, Commitment, Event, State } from './contract'
  import { done as doneOf, forYou as forYouOf, inProgress as inProgressOf, ymd, type ForYou, type Work } from './lavori'
  import { doIt, mark, type Edit } from './recapAgenda'
  import type { RecapAction } from './recapActions'
  import { t } from './t'

  let { st, agenda = null, events = [], now, timeZone = null, header = 'bar', part = 'all', today = null, onBack = () => {}, onApprove = () => {}, onAgendaEdit = () => {}, onStart = () => {}, onDoneForPerson = () => {}, onAddNight = () => {}, onRemoveNight = () => {}, onNight = () => {}, onSession = () => {} }: {
    st: State; agenda?: AgendaPage | null; events?: Event[]; now: number; timeZone?: string | null
    /** bar = ← e titolo; none = la testata la dà chi ospita; desk = nel cruscotto, il titolo come le sezioni. */
    header?: 'bar' | 'none' | 'desk'
    /** Le due metà del cruscotto: left = Per te e Stanotte, right = In corso e Fatto. */
    part?: 'all' | 'left' | 'right'
    today?: string | null
    onBack?: () => void; onApprove?: (task: string, note: string) => void; onAgendaEdit?: (e: Edit) => void
    onStart?: (a: RecapAction) => void; onDoneForPerson?: (c: Commitment) => void
    onAddNight?: () => void; onRemoveNight?: (id: string) => void; onNight?: () => void; onSession?: (name: string) => void
  } = $props()

  const zone = $derived(timeZone ?? Intl.DateTimeFormat().resolvedOptions().timeZone)
  const day = $derived(today ?? ymd(now, zone))
  const fy = $derived(forYouOf(st, agenda, now, day))
  const ip = $derived(inProgressOf(st, agenda, now, zone, day))
  const dn = $derived(doneOf(events, zone, day))
  const left = $derived(part !== 'right')
  const right = $derived(part !== 'left')
  const oldRelay = $derived(st.commitments == null)
  const progressN = $derived(ip.active.length + ip.paused.length + ip.others.length)
  let open = $state<Record<string, boolean>>({})
  const toggle = (k: string) => { open = { ...open, [k]: !open[k] } }
  const hhmm = (s: number) => new Intl.DateTimeFormat(locale, { timeZone: zone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s * 1000))
  const dm = (d: string) => `${d.slice(8, 10)}/${d.slice(5, 7)}`
  const yesterday = $derived(new Date(new Date(day + 'T12:00:00Z').getTime() - 86400000).toISOString().slice(0, 10))
  function weekday(d: string): string {
    const w = new Intl.DateTimeFormat(locale, { weekday: 'long' }).format(new Date(d + 'T12:00:00Z'))
    return `${w.charAt(0).toUpperCase()}${w.slice(1)} ${dm(d)}`
  }
  function sub(r: ForYou): string | null {
    if (r.kind === 'ok') return r.who && r.at ? t.lavoriAsks(r.who, hhmm(r.at)) : r.who ? t.lavoriAsksWho(r.who) : t.lavoriWaitsOk
    if (r.commitment) return r.who ? t.lavoriUnblocks(r.who) : null
    return [r.who, r.agenda?.scope].filter(x => x && x.trim()).join(' · ') || null
  }
  function meta(w: Work): string {
    const c = w.row?.c
    if (c) {
      const okAt = c.approved_at ? (ymd(c.approved_at, zone) === day ? t.lavoriOkAt(hhmm(c.approved_at)) : t.lavoriOkOn(dm(ymd(c.approved_at, zone)), hhmm(c.approved_at))) : null
      const last = c.paused ? t.lavoriPausedReason(c.paused.reason) : c.next?.trim() ? t.lavoriNext(c.next) : null
      return [c.session, okAt, last].filter(Boolean).join(' · ')
    }
    const a = w.agenda!
    return [a.ref?.trim() || null, a.blocks?.trim() ? t.lavoriBlocks(a.blocks) : null].filter(Boolean).join(' · ')
  }
  const toneOf = (w: Work) => (w.row?.tone === 'alert' ? 'var(--gone)' : w.row?.tone === 'warn' ? 'var(--b-warn)' : 'var(--text2)')
  let pending = $state<Approval | null>(null)
  let note = $state('')
  let dlg: HTMLDialogElement | undefined = $state()
  function ask(a: Approval) { pending = a; note = ''; dlg?.showModal() }
  function outsideClick(r: ForYou) { if (r.agenda) onAgendaEdit({ item: 'done', row: r.agenda }); else if (r.commitment) onDoneForPerson(r.commitment) }
</script>

{#snippet fold(text: string, state: boolean | null, onClick: () => void)}
  <button class="fold" onclick={onClick}><span>{text}</span>
    <svg viewBox="0 0 24 24" width="20" height="20"><path d={state === null ? 'M10 7l5 5-5 5' : state ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'} fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" /></svg>
  </button>
{/snippet}

{#snippet row(r: ForYou, button: string | null, filled: boolean, onClick: () => void)}
  <div class="row">
    {#if r.agenda}<span class="mark {mark(r.agenda.scope)}"></span>
    {:else if r.commitment?.paused}<svg viewBox="0 0 24 24" width="22" height="22" fill="var(--text2)"><path d="M6 5h4v14H6zM14 5h4v14h-4z" /></svg>
    {:else}<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--advice)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76z" /><path d="m9 12 2 2 4-4" /></svg>{/if}
    <span class="t"><b>{r.title}</b>{#if sub(r)}<small>{sub(r)}</small>{/if}</span>
    {#if button}<button class={filled ? 'filled' : 'tonal'} onclick={onClick}>{button}</button>{/if}
  </div>
{/snippet}

{#snippet work(w: Work)}
  {@const c = w.row?.c}
  <div class="card">
    <div class="top"><b>{c?.title ?? w.agenda?.title}</b><span class="tag" style:color={c?.paused ? 'var(--text2)' : c ? 'var(--busy)' : 'var(--text2)'}>{c?.paused ? t.lavoriStatePaused : c ? t.lavoriStateActive : t.lavoriStateOthers}</span></div>
    {#if meta(w)}<div class="meta" style:color={toneOf(w)}>{meta(w)}</div>{/if}
    {#if w.bars.length}
      <div class="track">
        {#each w.bars as b}<i class:dashed={b.dashed} style:left="{b.from * 100}%" style:width="{(b.to - b.from) * 100}%"></i>{/each}
        <i class="now" style:left="{ip.axis.now * 100}%"></i>
      </div>
    {/if}
  </div>
{/snippet}

<div class="lavori">
  {#if header === 'bar'}
    <div class="head">
      <button class="back" onclick={onBack} aria-label={t.back}><svg viewBox="0 0 24 24" width="24" height="24"><path d="M19 12H5M12 19l-7-7 7-7" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
      <h2>{t.lavoriTitle}</h2><small>{t.lavoriForYouN(fy.total)}</small>
    </div>
  {:else if header === 'desk'}<h3 class="desk">{t.lavoriTitle.toUpperCase()}<i></i></h3>{/if}

  {#if left}
    <h3 class="sec"><span>{t.lavoriForYou.toUpperCase()} · {fy.total}</span><i></i></h3>
    <p class="expl">{t.lavoriForYouExpl}</p>
    {#if !fy.total}<p class="expl bright">{t.lavoriNothing}</p>{/if}
    {#if fy.ok.length}
      <h4 class="gh" style="--t:var(--primary)"><span>{t.lavoriOk.toUpperCase()} · {fy.ok.length}</span><i></i></h4>
      {#each fy.ok as r, i (`ok-${i}`)}{@render row(r, r.approval ? t.approve : null, true, () => r.approval && ask(r.approval))}{/each}
    {/if}
    {#if fy.outside.length}
      <h4 class="gh" style="--t:var(--b-warn)"><span>{t.lavoriOutside.toUpperCase()} · {fy.outside.length}</span><i></i></h4>
      {#each open.outside ? fy.outside : fy.outside.slice(0, 2) as r, i (`out-${i}`)}{@render row(r, t.agendaDone, false, () => outsideClick(r))}{/each}
      {#if fy.outside.length > 2}{@render fold(open.outside ? t.lavoriLess : t.lavoriMore(fy.outside.length - 2), !!open.outside, () => toggle('outside'))}{/if}
    {/if}
    {#if fy.claude.length}
      <h4 class="gh" style="--t:var(--busy)"><span>{t.lavoriClaude.toUpperCase()} · {fy.claude.length}</span><i></i></h4>
      {#each open.claude ? fy.claude : fy.claude.slice(0, 2) as r, i (`cl-${i}`)}{@render row(r, t.lavoriStart, false, () => r.agenda && onStart(doIt(r.agenda)))}{/each}
      {#if fy.claude.length > 2}{@render fold(open.claude ? t.lavoriLess : t.lavoriMore(fy.claude.length - 2), !!open.claude, () => toggle('claude'))}{/if}
    {/if}
  {/if}

  {#if right}
    <h3 class="sec"><span>{t.lavoriProgress.toUpperCase()} · {progressN}</span><i></i></h3>
    <p class="expl">{t.lavoriProgressExpl}</p>
    {#if oldRelay}<p class="expl bright">{t.lavoriOldRelay}</p>
    {:else if !progressN}<p class="expl bright">{t.lavoriNothingProgress}</p>{/if}
    {#if ip.active.length || ip.paused.length}
      <div class="axis">{#each ip.axis.days as [label]}<span>{label}</span>{/each}<b>{t.lavoriAxisToday}</b></div>
      <p class="key">{t.lavoriKey}</p>
    {/if}
    {#each ip.active as w (w.row!.c.task)}{@render work(w)}{/each}
    {#if ip.paused.length}
      {@render fold(`${t.lavoriPausedN(ip.paused.length)}: ${ip.paused.map(w => w.row!.c.title).join(', ')}`, !!open.paused, () => toggle('paused'))}
      {#if open.paused}{#each ip.paused as w (w.row!.c.task)}{@render work(w)}{/each}{/if}
    {/if}
    {#if ip.others.length}
      {@render fold(`${t.lavoriOthersN(ip.others.length)}: ${ip.others.map(w => w.agenda!.title).join(', ')}`, !!open.others, () => toggle('others'))}
      {#if open.others}{#each ip.others as w, i (`ot-${i}`)}{@render work(w)}{/each}{/if}
    {/if}
  {/if}

  {#if left}
    <h3 class="sec"><span>{t.lavoriTonight.toUpperCase()}</span><i></i></h3>
    <p class="expl">{t.lavoriTonightExpl}</p>
    <div class="card night">
      <div class="trow">
        <span class="tcol"><span class="big">{st.night.queued > 0 ? t.regQueued(st.night.queued) : t.regQueueEmpty}</span></span>
        {#if st.night.items}<button class={fy.ok.some(r => r.approval) ? 'tonal' : 'filled'} onclick={onAddNight}>{t.regAdd}</button>{/if}
      </div>
      {#if st.night.running}<p class="running">{t.nightRunning(st.night.running)}</p>{/if}
      {#if !st.night.items}<p class="muted">{t.nightUpdatePc}</p>{/if}
      {#each st.night.items ?? [] as job (job.id)}
        <div class="job">
          <span class="jcol"><b>{job.name}</b><span>{job.prompt}</span>{#if job.started}<span class="started">{t.nightStarted}</span>{/if}</span>
          <button class="link" disabled={job.started != null} onclick={() => onRemoveNight(job.id)}>{t.nightRemove}</button>
        </div>
      {/each}
    </div>
    {#if st.night.report}{@render fold(t.lavoriLastNight(/^\d{4}-\d{2}-\d{2}$/.test(st.night.report.date) ? dm(st.night.report.date) : st.night.report.date), null, onNight)}{/if}
  {/if}

  {#if right}
    <h3 class="sec"><span>{t.lavoriDone.toUpperCase()}</span><i></i></h3>
    <p class="expl">{t.lavoriDoneExpl}</p>
    {#if !dn.length}<p class="expl bright">{t.lavoriDoneEmpty}</p>{/if}
    {#each dn as d (d.date)}
      {@const isToday = d.date === day}
      {@const shown = isToday || !!open[`day:${d.date}`]}
      <button class="day" disabled={isToday} onclick={() => toggle(`day:${d.date}`)}>
        <span>{(isToday ? t.lavoriToday(dm(d.date), d.rows.length) : d.date === yesterday ? t.lavoriYesterday(dm(d.date), d.rows.length) : t.lavoriDay(weekday(d.date), d.rows.length)).toUpperCase()}</span><i></i>
        {#if !isToday}<svg viewBox="0 0 24 24" width="18" height="18"><path d={shown ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'} fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" /></svg>{/if}
      </button>
      {#if shown}
        {#each d.rows as x (x.task)}
          <div class="done">
            <svg viewBox="0 0 24 24" width="18" height="18"><path d="M5 12l5 5 9-10" fill="none" stroke="var(--good)" stroke-width="2.4" stroke-linecap="round" /></svg>
            <span class="tm">{hhmm(x.at)}</span>
            <span class="t"><b>{x.title}</b><small>{#if x.session}<button class="who" onclick={() => onSession(x.session!)}>{x.session}</button> · {/if}<span class="proof">{x.proof ?? t.lavoriProofCheck}</span></small></span>
          </div>
        {/each}
      {/if}
    {/each}
    <p class="expl quota">{t.lavoriQuotaNote}</p>
  {/if}
</div>

<dialog bind:this={dlg} class="alert" onclick={(e) => e.target === e.currentTarget && dlg?.close()}>
  {#if pending}
    <h3>{pending.deploy ? t.approveProdTitle : t.approveTitle(pending.title)}</h3>
    <p>{t.approveText(pending.title, pending.what ?? '', pending.where ?? '')}</p>
    <input bind:value={note} placeholder="ok" aria-label={t.approveNote} />
    <div class="btns">
      <button class="text2" onclick={() => dlg?.close()}>{t.cancel}</button>
      <button class="filled" onclick={() => { onApprove(pending!.task, note); dlg?.close() }}>{t.approve}</button>
    </div>
  {/if}
</dialog>

<style>
  .lavori { display: flex; flex-direction: column; gap: 6px; padding: 0 12px 28px; }
  .head { display: flex; align-items: center; gap: 8px; height: 56px; }
  .head h2 { font: 500 22px/1 var(--sans, inherit); margin: 0; } .head small { color: var(--text2); font-size: 14px; }
  .back { color: var(--text); padding: 8px; border-radius: 50%; }
  .desk, .sec { display: flex; align-items: center; gap: 10px; margin: 14px 0 2px; padding: 0 4px; font: 500 12px/1 var(--mono); letter-spacing: .14em; color: var(--text2); }
  .sec i, .desk i, .gh i, .day i { flex: 1; height: 1px; background: var(--line); }
  .gh { display: flex; align-items: center; gap: 8px; margin: 10px 4px 2px; font: 600 11px/1.4 var(--mono); letter-spacing: .1em; color: var(--t); } .gh i { background: color-mix(in srgb, var(--t) 30%, transparent); }
  .expl { margin: 0 6px 6px; color: var(--text2); font-size: 14px; line-height: 1.4; } .expl.bright { color: var(--text); } .expl.quota { margin-top: 14px; color: var(--b-good-ink, #BCE4C7); border-top: 1px solid var(--line); padding-top: 10px; }
  .row, .card, .done { background: var(--low); border-radius: 20px; }
  .row { display: flex; align-items: center; gap: 12px; padding: 12px 12px 12px 16px; }
  .row .t, .done .t { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; } .row b { font-weight: 600; font-size: 15px; } .row small, .done small { color: var(--text2); font-size: 13px; }
  .mark { width: 18px; height: 18px; flex: none; border: 2px solid var(--text2); border-radius: 5px; } .mark.personal { border-radius: 50%; } .mark.desk { border-radius: 3px; transform: rotate(45deg) scale(.8); }
  .filled { background: var(--primary); color: var(--on-primary); border-radius: 999px; padding: 9px 16px; font-weight: 500; font-size: 14px; white-space: nowrap; }
  .tonal { background: var(--high); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; white-space: nowrap; }
  .fold { display: flex; align-items: center; gap: 8px; width: 100%; padding: 10px 6px; color: var(--text2); font-size: 14px; text-align: left; border-radius: 12px; } .fold span { flex: 1; }
  .axis { display: flex; padding: 2px 18px; font: 11px var(--mono); color: var(--b-axis, #8B929C); } .axis span { flex: 1; } .axis b { font-weight: 400; color: var(--text); }
  .key { margin: 0 6px 6px; font: 11px/1.4 var(--mono); color: var(--b-axis, #8B929C); }
  .card { display: flex; flex-direction: column; gap: 4px; padding: 14px; } .card .top { display: flex; align-items: center; gap: 10px; } .card .top b { flex: 1; font-weight: 600; font-size: 15px; }
  .tag { font: 600 11px var(--mono); letter-spacing: .06em; text-transform: uppercase; border-radius: 999px; padding: 3px 9px; background: color-mix(in srgb, currentColor 15%, transparent); white-space: nowrap; }
  .meta { font-size: 14px; }
  .track { position: relative; height: 8px; border-radius: 4px; background: var(--b-track); margin: 10px 0 2px; }
  .track i { position: absolute; top: 0; height: 8px; border-radius: 4px; background: var(--busy); }
  .track i.dashed { background: repeating-linear-gradient(90deg, color-mix(in srgb, var(--text2) 70%, transparent) 0 6px, transparent 6px 10px); }
  .track i.now { width: 2px; top: -4px; height: 16px; border-radius: 0; background: var(--text); }
  .card.night { gap: 8px; } .trow { display: flex; align-items: center; gap: 12px; } .tcol { flex: 1; } .big { font-size: 16px; }
  .running { color: var(--busy); font-size: 14px; margin: 0; } .muted { color: var(--text2); font-size: 14px; margin: 0; }
  .job { display: flex; align-items: center; gap: 10px; } .jcol { flex: 1; display: flex; flex-direction: column; gap: 2px; font-size: 14px; } .jcol > span:not(:first-child) { color: var(--text2); font-size: 13px; } .jcol b { font-weight: 600; } .started { color: var(--busy); }
  .link { color: var(--icon); padding: 8px 12px; border-radius: 16px; font-weight: 500; font-size: 14px; }
  .day { display: flex; align-items: center; gap: 8px; width: 100%; margin: 10px 0 2px; padding: 0 6px; font: 600 12px/1.6 var(--mono); letter-spacing: .12em; color: var(--text2); text-align: left; } .day:disabled { color: var(--text2); }
  .done { display: flex; align-items: flex-start; gap: 10px; padding: 10px 14px; border-radius: 18px; } .done .tm { font: 12px/1.5 var(--mono); color: var(--text2); } .done b { font-weight: 600; font-size: 14px; }
  .done .who { color: var(--text2); font-size: 13px; padding: 0; } .proof { color: var(--icon); }
  dialog.alert { background: var(--surface); color: var(--text); border: none; border-radius: 28px; padding: 24px; max-width: 420px; width: calc(100% - 48px); }
  dialog.alert::backdrop { background: rgb(0 0 0 / .6); } dialog h3 { margin: 0 0 8px; font-size: 20px; font-weight: 500; } dialog p { margin: 0 0 12px; color: var(--text2); font-size: 14px; }
  dialog input { width: 100%; background: var(--low); color: var(--text); border: 1px solid var(--line); border-radius: 12px; padding: 10px 12px; font-size: 15px; }
  .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; } .btns .text2 { color: var(--text2); padding: 10px 16px; border-radius: 999px; }
  @media (prefers-reduced-motion: reduce) { .track i { transition: none; } }
</style>
