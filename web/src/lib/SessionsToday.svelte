<script lang="ts">
  import type { KpiData, State, TimelinePage } from './contract'
  import { day, hourly, midnight, todayShare, turnsOf, waitingSince } from './kpi'
  import Kpi6 from './Kpi6.svelte'
  import Kpi10 from './Kpi10.svelte'
  import Kpi11 from './Kpi11.svelte'
  import { todaySessions } from './sessionK'
  import { t } from './t'
  // app-scheda-sessione-semplice-19-11: con la scheda K la carta aperta tiene solo il contesto; la giornata della sessione (barra
  // oraria e attività per ora) e il suo costo (token per turno, «oggi Y%» dell'account) stanno qui, una sessione per riquadro.
  let { st, page, data, now }: { st: State; page: TimelinePage | null; data: KpiData | null; now: number } = $props()
  const from = $derived(midnight(now))
  const names = $derived(todaySessions(page, data, from))
  const events = $derived(new Map((page?.sessions ?? []).map(x => [x.session, x.events])))
</script>

{#if names.length}
  <section class="st" data-testid="sessioni-oggi">
    <h3>{t.sessionsTodayTitle}</h3>
    {#each names as name (name)}
      {@const s = st.sessions.find(x => x.name === name)}
      {@const ev = events.get(name) ?? []}
      {@const share = todayShare(st, name)}
      {@const turns = turnsOf(data, name)}
      <div class="row" data-testid="sessione-oggi">
        <div class="head">
          <span class="name">{name}</span>
          {#if ev.length && s && s.state !== 'gone'}<Kpi6 counts={hourly(ev, now)} />{/if}
          {#if share != null}<span class="share" data-kpi="kpi-5-oggi">{t.kpi5Today(share)}</span>{/if}
        </div>
        {#if ev.length}<Kpi11 spans={day(ev, from, now, s && s.state !== 'gone' ? waitingSince(s) : null)} {from} {now} />{/if}
        {#if turns.length}<Kpi10 {turns} />{/if}
      </div>
    {/each}
  </section>
{/if}

<style>
  .st { border-radius: 14px; background: var(--s1); border: 1px solid var(--line); padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 14px; }
  .st h3 { font: 600 17px var(--display); }
  .row { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
  .row + .row { border-top: 1px solid var(--line); padding-top: 14px; }
  .head { display: flex; align-items: center; gap: 10px; min-width: 0; }
  .name { flex: 1; min-width: 0; font: 500 15px var(--sans); white-space: nowrap; overflow: hidden; mask-image: linear-gradient(to right, #000 85%, transparent); }
  .share { font: 500 13px var(--mono); color: var(--tx2); font-variant-numeric: tabular-nums; flex: none; }
</style>
