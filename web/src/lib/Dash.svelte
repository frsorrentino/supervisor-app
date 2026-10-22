<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { Project, State } from './contract'
  import { hostNumbers, publishedRows, recentProjects, sessionsByState, waitingFor } from './dash'
  import { since } from './durations'
  import type { NightPage } from './night'
  import { readable } from './slashText'
  import { t } from './t'

  // Il cruscotto della plancia senza sessioni aperte, a tre colonne (Franz, 21/10 13:02): cosa serve da te, il lavoro, numeri
  // e strumenti. Riusa i riquadri che esistono (Recap, notte, quota, ricorrenti); stato di win e pubblicazioni di oggi
  // dal contratto 1.75 (su Firebase, con /state entro 8 KB, al massimo 5 pubblicazioni; la web locale ne vede fino a 30).
  let { st, now, cols, night, nightTitle, nightLoading, onNight, lavoriLeft, lavoriRight, quota, onPick, onTalk, onLaunch }: {
    st: State; now: number; cols: 1 | 2 | 3
    night: NightPage | null; nightTitle: string; nightLoading: boolean; onNight: () => void
    lavoriLeft: Snippet; lavoriRight: Snippet; quota: Snippet
    onPick: (name: string) => void; onTalk: (text: string) => void; onLaunch: (p: Project) => void
  } = $props()

  const waiting = $derived(waitingFor(st.attention, now))
  const by = $derived(sessionsByState(st.sessions))
  const recent = $derived(recentProjects(st))
  const recurring = $derived(st.recurring ?? [])
  const hosts = $derived(st.hosts_status ?? [])
  const published = $derived(publishedRows(st.published_today))
  let chosen = $state<Project | null>(null)
  const hhmm = (s: number) => new Date(s * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
</script>

{#snippet head(title: string)}<h3 class="dt"><span>{title.toUpperCase()}</span><i></i></h3>{/snippet}

<div class="dash" style:--cols={cols}>
  <section class="dcol">
    {@render head(t.dashYou)}
    {#if waiting.length}
      <div class="box">
        <h4>{t.dashWaiting(waiting.length)}</h4>
        {#each waiting as w}
          <button class="row" disabled={!w.session} onclick={() => w.session && onPick(w.session)}>
            <span class="txt"><b>{w.session ?? t.dashNoSession}</b><small>{readable(w.title, t.slashPhrases)}</small></span>
            <small class="ago">{since(now - w.waitedS, now)}</small>
          </button>
        {/each}
      </div>
    {/if}
    {@render lavoriLeft()}
  </section>

  <section class="dcol">
    {@render head(t.dashWork)}
    {#if st.night?.report || night}
      <button class="box nightbox" onclick={onNight}>
        <b>{nightTitle}</b>
        {#if night}
          <small>{t.homeNightWindow(hhmm(night.start), hhmm(night.end), night.cards.length)}</small>
          <span class="nchips">
            {#if night.counts.done}<span style:color="var(--good)">{t.nightDone(night.counts.done)}</span>{/if}
            {#if night.counts.running}<span style:color="var(--icon)">{t.nightRunningN(night.counts.running)}</span>{/if}
            {#if night.counts.partial}<span style:color="var(--b-warn)">{t.nightPartial(night.counts.partial)}</span>{/if}
            {#if night.counts.stopped}<span style:color="var(--gone-dim)">{t.nightStopped(night.counts.stopped)}</span>{/if}
            {#if night.counts.asking}<span style:color="var(--advice)">{t.nightAsking(night.counts.asking)}</span>{/if}
          </span>
        {:else}<small>{nightLoading ? t.nightLoading : t.dashNightLater}</small>{/if}
      </button>
    {/if}
    {@render lavoriRight()}
    <div class="box">
      <h4>{t.dashSessions}</h4>
      <p class="counts">
        <span style:color="var(--icon)">{t.dashBusy(by.busy)}</span>
        <span style:color="var(--wait)">{t.dashWaitingN(by.waiting)}</span>
        <span>{t.dashIdle(by.idle)}</span>
      </p>
      {#if by.closed.length}
        <small class="sub">{t.dashClosed}</small>
        {#each by.closed as s (s.name)}
          <button class="row" onclick={() => onPick(s.name)}>
            <span class="txt"><b>{s.name}</b>{#if s.outcome?.short}<small>{s.outcome.short}</small>{/if}</span>
            <small class="ago">{since(s.since, now)}</small>
          </button>
        {/each}
      {/if}
    </div>
  </section>

  <section class="dcol">
    {@render head(t.dashTools)}
    <div class="quota">{@render quota()}</div>
    <div class="box">
      <h4>{t.dashWin}</h4>
      {#each hosts as h (h.name)}
        <div class="host">
          <span class="txt">
            <b><span class="dot" class:up={h.up}></span>{h.name}</b>
            <small>{h.up ? hostNumbers(h) : t.dashHostDown(h.error ?? '')}</small>
            {#if h.sessions.length}
              <span class="chips">{#each h.sessions as n (n)}<button class="chip" onclick={() => onPick(n)}>{n}</button>{/each}</span>
            {:else if h.up}<small>{t.dashHostNoSessions}</small>{/if}
          </span>
          {#if h.at}<small class="ago">{now - h.at < 60 ? t.dashHostNow : t.dashHostRead(since(h.at, now))}</small>{/if}
        </div>
      {:else}<small class="sub">{t.dashWinNone}</small>{/each}
    </div>
    <div class="box">
      <h4>{t.dashPublished}</h4>
      {#each published as p, i (i)}
        <svelte:element this={p.url ? 'a' : 'div'} class="row" href={p.url ?? undefined} target={p.url ? '_blank' : undefined} rel={p.url ? 'noopener' : undefined}>
          <span class="txt"><b>{t.dashPubKind[p.kind] ?? p.kind} · {p.name}</b><small>{p.text}</small></span>
          <small class="ago">{hhmm(p.at)}</small>
        </svelte:element>
      {:else}<small class="sub">{t.dashPublishedNone}</small>{/each}
    </div>
    {#if recurring.length || recent.length}
      <div class="box">
        {#if recurring.length}
          <h4>{t.recurring}</h4>
          {#each recurring as r (r.id)}
            <button class="row" onclick={() => onTalk(r.prompt)}><span class="txt"><b>{r.label}</b></span></button>
          {/each}
        {/if}
        {#if recent.length}
          <h4 class:gap={recurring.length > 0}>{t.dashLaunch}</h4>
          <div class="chips">
            {#each recent as p (p.path)}
              <button class="chip" class:on={chosen === p} aria-pressed={chosen === p} onclick={() => (chosen = chosen === p ? null : p)}>{p.name}</button>
            {/each}
          </div>
          {#if chosen}<button class="go" onclick={() => { if (chosen) onLaunch(chosen); chosen = null }}>{t.dashLaunchGo(chosen.name)}</button>{/if}
        {/if}
      </div>
    {/if}
  </section>
</div>

<style>
  /* Colonne di al massimo 440 px, in alto; tutta la plancia scorre, così a 2 colonne la terza va sotto senza tagli. */
  .dash { position: absolute; inset: 0; overflow-y: auto; display: grid; grid-template-columns: repeat(var(--cols), minmax(0, 440px));
    justify-content: center; align-items: start; gap: 18px; padding: 0 16px 24px 14px; text-align: left; color: var(--text); }
  .dcol { display: flex; flex-direction: column; gap: 12px; min-width: 0; }
  .dt { display: flex; align-items: center; gap: 8px; margin: 14px 4px 0; font: 500 12px/1 var(--mono); letter-spacing: .14em; color: var(--text2); }
  .dt i { flex: 1; height: 1px; background: var(--line); }
  .dcol :global(.recap) { padding: 0; }
  .quota { display: flex; flex-direction: column; gap: 12px; }
  .box { display: flex; flex-direction: column; gap: 6px; padding: 14px 16px; border-radius: 24px; background: var(--surface); text-align: left; width: 100%; }
  h4 { font: 500 12px/1.4 var(--mono); letter-spacing: .08em; color: var(--text2); text-transform: uppercase; }
  h4.gap { margin-top: 10px; }
  .sub { color: var(--text2); font-size: 14px; line-height: 1.4; }
  .row { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-top: 1px solid var(--line); text-align: left; width: 100%; color: var(--text); }
  .row:first-of-type { border-top: 0; }
  .row:disabled { cursor: default; }
  .txt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .txt b { font-size: 15px; font-weight: 500; }
  .txt small { color: var(--text2); font-size: 13.5px; line-height: 1.35; }
  .ago { color: var(--text2); font: 12px var(--mono); white-space: nowrap; }
  .host { display: flex; align-items: flex-start; gap: 12px; padding: 8px 0; border-top: 1px solid var(--line); }
  .host:first-of-type { border-top: 0; }
  .host .chips { margin-top: 4px; }
  .dot { display: inline-block; width: 8px; height: 8px; margin-right: 8px; border-radius: 50%; background: var(--gone-dim); vertical-align: middle; }
  .dot.up { background: var(--good); }
  a.row { text-decoration: none; }
  .counts { display: flex; gap: 14px; flex-wrap: wrap; font-size: 15px; }
  .nightbox { gap: 10px; }
  .nightbox b { font-size: 17px; font-weight: 500; }
  .nightbox small { color: var(--text2); font-size: 14px; }
  .nchips { display: flex; gap: 8px; flex-wrap: wrap; }
  .nchips span { padding: 6px 12px; border-radius: 999px; background: var(--high); font-weight: 500; font-size: 14px; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .chip { border: 1px solid var(--line); border-radius: 999px; padding: 7px 14px; font-size: 14px; color: var(--text2); }
  .chip.on { background: var(--high); color: var(--text); border-color: transparent; }
  .go { align-self: flex-start; margin-top: 6px; padding: 10px 18px; border-radius: 22px; background: var(--high); color: var(--text); font-weight: 500; }
</style>
