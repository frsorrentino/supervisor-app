<script lang="ts">
  import type { KpiTask, KpiTasks } from './contract'
  import { ageFraction, tasksOrder } from './kpi'
  import { t } from './t'
  // Proposta 8 (25/10, contratto 1.96): quattro cifre in cima a «In corso» e, per compito, la barra d'età (piena da un
  // giorno) e i tentativi del controllo già spesi.
  let { k }: { k: KpiTasks } = $props()
  const tone = (x: KpiTask) => (x.stalled ? 'var(--b-alert)' : x.state === 'running' ? 'var(--busy)' : x.state === 'paused' ? 'var(--stale)' : 'var(--wait)')
  const word = (x: KpiTask) => (x.stalled ? t.kpi8State.stalled : (t.kpi8State[x.state] ?? x.state))
</script>

<div class="k8" data-kpi="kpi-8">
  <div class="grid">
    <div class="fig"><b style:color={k.open ? 'var(--busy)' : null}>{k.open}</b><span>{t.kpi8Open}</span></div>
    <div class="fig"><b style:color={k.waiting_you ? 'var(--wait)' : null}>{k.waiting_you}</b><span>{t.kpi8WaitingYou}</span></div>
    <div class="fig"><b style:color={k.stalled ? 'var(--b-alert)' : null}>{k.stalled}</b><span>{t.kpi8StalledN}</span></div>
    <div class="fig"><b style:color={k.closed_today ? 'var(--good)' : null}>{k.closed_today}</b><span>{t.kpi8ClosedToday}</span></div>
  </div>
  {#if k.tasks.length}
    <div class="card">
      <div class="head"><span>{t.kpi8Title}</span><b>{t.kpi8Axis}</b></div>
      {#each tasksOrder(k) as x (x.task)}
        <div class="task">
          <div class="name">{x.short ?? x.task}</div>
          <div class="line">
            <span class="bar"><i style:width="{Math.max(2, ageFraction(x.age_s) * 100)}%" style:background={tone(x)}></i></span>
            <span class="age">{t.kpiAge(x.age_s)}</span>
            <span class="dots" role="img" aria-label={t.kpi8Tries(x.attempts, x.attempts_max)}>{#each Array.from({ length: Math.min(6, x.attempts_max) }) as _, i (i)}<i class:spent={i < x.attempts}></i>{/each}</span>
          </div>
          <div class="who" style:color={tone(x)}>{[word(x), x.who].filter(Boolean).join(' · ')}</div>
        </div>
      {/each}
    </div>
  {/if}
</div>

<style>
  .k8 { display: flex; flex-direction: column; gap: 8px; }
  .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
  .fig { background: var(--low); border-radius: 16px; padding: 10px 12px; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
  .fig b { font: 500 24px/1.1 var(--sans, inherit); }
  .fig span { font-size: 13px; color: var(--text2); overflow-wrap: anywhere; }
  .card { background: var(--low); border-radius: 16px; padding: 12px 14px; display: flex; flex-direction: column; gap: 10px; min-width: 0; }
  .head { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 4px 12px; font: 500 11px/1.3 var(--mono); color: var(--text2); letter-spacing: .06em; }
  .head span { text-transform: uppercase; }
  .head b { font: 400 12px/1.3 var(--mono); letter-spacing: 0; color: var(--text2); }
  .task { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
  .name { font-size: 14px; overflow-wrap: anywhere; }
  .line { display: flex; align-items: center; gap: 8px; }
  .bar { flex: 1; height: 6px; border-radius: 3px; background: var(--b-track); overflow: hidden; }
  .bar i { display: block; height: 100%; border-radius: 3px; }
  .age { font: 12px var(--mono); color: var(--text2); white-space: nowrap; }
  .dots { display: flex; gap: 3px; }
  .dots i { width: 8px; height: 8px; border-radius: 50%; background: var(--b-track); }
  .dots i.spent { background: var(--b-alert); }
  .who { font-size: 12.5px; overflow-wrap: anywhere; }
</style>
