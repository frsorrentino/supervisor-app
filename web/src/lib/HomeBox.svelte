<script module lang="ts">
  export type Box = 'aspetta' | 'lavoro' | 'esiti'
</script>

<script lang="ts">
  import type { State, TimelinePage } from './contract'
  import { head, outcomes as outcomesOf, stillRows, waitingRows, workingRows, type WaitKind } from './kpi'
  import { since } from './durations'
  import Icon from './Icon.svelte'
  import { t } from './t'
  // Le pagine dietro i tre box in alto della home (compito app-box-cliccabili-29-10, mockup approvato da Franz il 29/10
  // alle 18:57), porta di HomeBoxScreen.kt: il numero spiegato in una frase, sotto le righe che lo fanno.
  let { box, st, page, now, onSession }: { box: Box; st: State; page: TimelinePage | null; now: number; onSession: (name: string) => void } = $props()

  const PAGE = 20
  const h = $derived(head(st, page, now))
  const waiting = $derived(waitingRows(st))
  const working = $derived(workingRows(st, now))
  const still = $derived(stillRows(st))
  const out = $derived(outcomesOf(page, now))
  let stillAll = $state(false)
  let shown = $state(PAGE)
  const live = $derived(new Set(st.sessions.filter(s => s.state !== 'gone').map(s => s.name)))
  const hm = (at: number) => { const d = new Date(at * 1000); return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}` }
  const kindText: Record<WaitKind, string> = { question: t.boxKindQuestion, needs_you: t.boxKindNeedsYou, blocking: t.boxKindBlocking }
  const kindIcon = { question: 'question', needs_you: 'user', blocking: 'hourglass' } as const
</script>

{#snippet chevron()}<svg class="chev" width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" /></svg>{/snippet}

<div class="box" data-testid="home-box-page-{box}">
  {#if box === 'aspetta'}
    <div class="hero"><b class="n" class:wait={waiting.length > 0}>{waiting.length}</b>
      <p>{waiting.length ? t.boxWaitingHead(since(waiting[0].since, now)) : t.boxWaitingNone}</p></div>
    {#if waiting.length}
      <div class="card">
        {#each waiting as w (w.session)}
          <button class="row" onclick={() => onSession(w.session)}>
            <span class="ic w"><Icon name={kindIcon[w.kind]} size={16} /></span>
            <span class="bd">
              <span class="hd"><b>{w.session}</b><span>{t.queueAge(since(w.since, now))}</span></span>
              <span class="kind w">{kindText[w.kind]}{#if w.closesItself}{` · ${t.boxClosesItself}`}{/if}</span>
              <span class="tx">{w.text}</span>
            </span>
            {@render chevron()}
          </button>
        {/each}
      </div>
      <p class="note">{t.boxWaitingNote}</p>
    {/if}
  {:else if box === 'lavoro'}
    <div class="hero"><b class="n" class:busy={working.length > 0}>{working.length}</b><p>{working.length ? t.boxBusyHead : t.boxBusyNone}</p></div>
    {#if working.length}
      <div class="card">
        {#each working as w (w.session)}
          <button class="row" onclick={() => onSession(w.session)}>
            <span class="ic b"><Icon name="run" size={16} /></span>
            <span class="bd">
              <span class="hd"><b>{w.session}</b>{#if w.turnS != null}<span>{t.queueAge(since(now - w.turnS, now))}</span>{/if}</span>
              {#if w.goal}<span class="tx">{w.goal}</span>{/if}
              {#if w.tool}{@const [name, ...rest] = w.tool.split(' ')}<span class="meta"><span class="busy">{name}</span>{rest.length ? ` ${rest.join(' ')}` : ''}{#if w.toolNote}{` · ${w.toolNote}`}{/if}</span>{/if}
              {#if w.model || w.context != null}<span class="meta">{[w.model, w.context != null ? `ctx ${w.context}%` : null].filter(Boolean).join(' · ')}</span>{/if}
            </span>
            {@render chevron()}
          </button>
        {/each}
      </div>
      <p class="note">{t.boxBusyNote}</p>
    {/if}
    {#if still.length}
      <div class="sec"><span>{t.boxStill}</span><i></i><small>{still.length}</small></div>
      <div class="card">
        {#each stillAll ? still : still.slice(0, 2) as s (s.session)}
          <button class="row" onclick={() => onSession(s.session)}>
            <span class="ic g"><Icon name="check" size={16} /></span>
            <span class="bd"><span class="hd"><b>{s.session}</b>{#if s.at}<span>{hm(s.at)}</span>{/if}</span>{#if s.outcome}<span class="tx">{s.outcome}</span>{/if}</span>
            {@render chevron()}
          </button>
        {/each}
      </div>
      {#if !stillAll && still.length > 2}<div class="more"><span>{t.boxMore(still.length - 2)}</span><button class="text" onclick={() => (stillAll = true)}>{t.boxShow}</button></div>{/if}
    {/if}
  {:else}
    <div class="hero"><b class="n">{h.outcomes ?? '–'}</b>
      <p>{out ? t.boxOutcomesHead(out.red.length) : t.boxOutcomesLoading}</p></div>
    {#if out}
      {#if out.red.length}
        <div class="sec red"><span>{t.boxRed}</span><i></i><small>{out.red.length}</small></div>
        <div class="card">
          {#each out.red as r, i (i)}
            <button class="row" disabled={!live.has(r.session)} onclick={() => onSession(r.session)}>
              <span class="ic r"><Icon name="x" size={16} /></span>
              <span class="bd">
                <span class="hd"><b>{r.session}</b><span>{hm(r.at)}</span></span>
                <span class="kind r">{t.boxRedKind}{#if r.ref}{` · ${r.ref}`}{/if}</span>
                <span class="tx">{r.text}</span>
              </span>
              {#if live.has(r.session)}{@render chevron()}{/if}
            </button>
          {/each}
        </div>
      {/if}
      {#if out.done.length}
        <div class="sec"><span>{t.boxOutcomes}</span><i></i><small>{t.boxNewestFirst(out.done.length)}</small></div>
        <div class="card">
          {#each out.done.slice(0, shown) as d, i (i)}
            <button class="row" disabled={!live.has(d.session)} onclick={() => onSession(d.session)}>
              <span class="ic g"><Icon name="check" size={16} /></span>
              <span class="bd"><span class="hd"><b>{d.session}</b><span>{hm(d.at)}</span></span><span class="tx">{d.text}</span></span>
              {#if live.has(d.session)}{@render chevron()}{/if}
            </button>
          {/each}
        </div>
        {#if out.done.length > shown}<div class="more"><span>{t.boxRest(out.done.length - shown)}</span><button class="tonal" onclick={() => (shown += PAGE)}>{t.boxShowMore(Math.min(PAGE, out.done.length - shown))}</button></div>{/if}
      {:else}
        <p class="note">{t.boxOutcomesNone}</p>
      {/if}
      <p class="note">{t.boxOutcomesNote}{#if out.atLeast} {t.boxAtLeast}{/if}</p>
    {/if}
  {/if}
</div>

<style>
  .box { padding: 0 12px 24px; max-width: 720px; }
  .hero { display: flex; gap: 14px; align-items: flex-start; background: var(--low); border-radius: 20px; padding: 14px 16px; margin-bottom: 14px; }
  .n { font-size: 44px; font-weight: 500; line-height: 1; flex: none; }
  .n.wait { color: var(--wait); } .n.busy { color: var(--busy); }
  .hero p { font-size: 14.5px; line-height: 1.35; color: var(--text2); }
  .sec { display: flex; align-items: center; gap: 8px; margin: 16px 0 6px; padding: 0 4px; font: 500 12px/1 var(--mono); letter-spacing: .14em; color: var(--text2); text-transform: uppercase; }
  .sec.red span { color: var(--gone); }
  .sec i { flex: 1; height: 1px; background: var(--line); }
  .sec small { font-size: 13px; letter-spacing: 0; text-transform: none; font-family: inherit; }
  .card { background: var(--low); border-radius: 20px; padding: 4px 14px; margin-bottom: 8px; }
  .row { display: flex; gap: 10px; padding: 10px 0; width: 100%; text-align: left; color: var(--text); align-items: flex-start; border-top: 1px solid var(--line); }
  .row:first-child { border-top: 0; }
  .row:disabled { cursor: default; }
  .ic { width: 30px; height: 30px; border-radius: 50%; display: grid; place-items: center; flex: none; margin-top: 1px; }
  .ic.w { background: color-mix(in srgb, var(--wait) 18%, transparent); color: var(--wait); }
  .ic.b { background: color-mix(in srgb, var(--busy) 18%, transparent); color: var(--busy); }
  .ic.g { background: color-mix(in srgb, var(--idle) 18%, transparent); color: var(--idle); }
  .ic.r { background: color-mix(in srgb, var(--gone) 20%, transparent); color: var(--gone); }
  .bd { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .hd { display: flex; align-items: baseline; gap: 8px; }
  .hd b { font-weight: 600; flex: 1; min-width: 0; overflow-wrap: anywhere; }
  .hd span { font: 12px var(--mono); color: var(--text2); flex: none; }
  .kind { font: 500 11.5px var(--mono); letter-spacing: .08em; text-transform: uppercase; overflow-wrap: anywhere; }
  .kind.w { color: var(--wait); } .kind.r { color: var(--gone); }
  .tx { font-size: 14.5px; overflow-wrap: anywhere; }
  .meta { font: 12px var(--mono); color: var(--text2); overflow-wrap: anywhere; }
  .meta .busy { color: var(--busy); }
  .chev { flex: none; align-self: center; color: var(--text2); }
  .more { display: flex; align-items: center; gap: 8px; padding: 6px 4px 0; }
  .more span { flex: 1; font: 12px var(--mono); color: var(--text2); }
  .text { color: var(--icon); font-weight: 500; font-size: 14px; padding: 8px 6px; }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; white-space: nowrap; }
  .note { font-size: 13px; color: var(--text2); padding: 8px 4px 0; }
</style>
