<script module lang="ts">
  /** Le voci con un riquadro «Prova» (Franz 01/11 22:13): il componente vero coi dati della demo, senza relay. */
  export const DEMO_KEYS = ['tabellone', 'ti_aspettano', 'slot', 'esiti', 'lavori', 'suggestions', 'master', 'system', 'notte_report', 'web_app']
</script>

<script lang="ts">
  import { world } from './demo'
  import { head } from './kpi'
  import { page as nightPage } from './night'
  import { visible } from './suggestions'
  import { CANONICAL } from './remoteAccess'
  import { t } from './t'
  import Home from './Home.svelte'
  import HomeBox from './HomeBox.svelte'
  import Kpi7 from './Kpi7.svelte'
  import Lavori from './Lavori.svelte'
  import NightPage from './NightPage.svelte'
  import SuggestionsCard from './SuggestionsCard.svelte'
  import Feed from './Feed.svelte'
  import { group, merge } from './chatFeed'
  // Tutti i tasti restano qui dentro: nessuna chiamata al relay, la demo cambia solo in memoria.
  let { key }: { key: string } = $props()
  const now = Math.floor(Date.now() / 1000)
  const w = world(now)
  let st = $state(w.state)
  const none = () => {}
  let sugDone = $state(new Set<string>())
  let slotSt = $state({ ...w.state, sessions: w.state.sessions.filter(s => s.state === 'gone' || s.name === 'admin-panel'), commitments: [], approvals: [], attention: [], suggestions: [], home_order: ['system', 'closed', 'esiti', 'sessions', 'lavori'] })
  const master = st.sessions.find(s => s.name === 'master')
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
  const today = new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(now * 1000))
</script>

<div class="try v3" data-testid="prova-{key}">
  {#if key === 'tabellone'}
    <Kpi7 h={head(st, w.timeline, now)} {st} {now} />
  {:else if key === 'ti_aspettano'}
    <HomeBox box="aspetta" {st} page={w.timeline} {now} onSession={none} />
  {:else if key === 'esiti'}
    <HomeBox box="esiti" {st} page={w.timeline} {now} onSession={none} />
  {:else if key === 'slot'}
    <!-- Poche voci, così quello da trascinare e il tabellone stanno insieme nel riquadro. -->
    <Home st={slotSt} kpiPage={w.timeline} {now} selected={[]} onPick={none} onAnswer={none} onStep={none} onHomeOrder={(arg: string) => { slotSt = { ...slotSt, home_order: JSON.parse(arg) } }} />
  {:else if key === 'system'}
    <Home part="tools" st={{ ...st, sessions: [] }} {now} selected={[]} onPick={none} onAnswer={none} onStep={none} />
  {:else if key === 'lavori'}
    <Lavori {st} agenda={w.agenda} events={w.events} {now} header="desk" timeZone={zone} {today} />
  {:else if key === 'suggestions'}
    {@const v = visible(st, sugDone, now)}
    {#if v.shown.length}<SuggestionsCard shown={v.shown} onAnswer={(s) => { sugDone = new Set([...sugDone, s.id]) }} />{:else}<p class="done">{t.sugTitle} · 0</p>{/if}
  {:else if key === 'notte_report'}
    <NightPage model={nightPage(w.night, zone)} onRefresh={none} onChat={none} onAnswer={none} onApprove={none} onSend={none} />
  {:else if key === 'master'}
    {#if master}<div class="mhead"><b>master</b><small>{master.model?.label}</small></div>{/if}
    <Feed s={master!} items={group(merge(w.transcripts.master ?? [], []))} {now} />
  {:else if key === 'web_app'}
    <a class="open" href="{CANONICAL}/?demo" target="_blank" rel="noopener">{CANONICAL.replace('https://', '')}/?demo</a>
  {/if}
</div>

<style>
  .try { width: 100%; max-height: 460px; overflow: auto; border: 1px solid var(--line); border-radius: 14px; background: var(--bg); padding: 8px; box-sizing: border-box; position: relative; }
  .mhead { display: flex; gap: 8px; align-items: baseline; padding: 4px; }
  .mhead small { color: var(--text2); }
  p { padding: 4px; font-size: 14px; white-space: pre-line; }
  p.mine { color: var(--text2); }
  .done { color: var(--text2); }
  .open { color: var(--icon); text-decoration: underline; overflow-wrap: anywhere; }
</style>
