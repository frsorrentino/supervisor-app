<script lang="ts">
  import type { DayFigures } from './kpi'
  import { t } from './t'
  import { locale } from './lang'
  // Proposta 18 (25/10): quattro cifre della giornata e i prompt e i commit per ora. L'attesa di oggi non c'è nella
  // cronologia: la quarta cifra dice chi ti aspetta adesso (master, 29/10 08:07).
  let { f }: { f: DayFigures } = $props()
  const plus = $derived(f.atLeast ? '+' : '')
  const tokens = $derived(new Intl.NumberFormat(locale, { notation: 'compact', maximumFractionDigits: 1 }).format(f.tokens))
  const top = $derived(Math.max(1, ...f.perHour.map(([p, c]) => p + c)))
</script>

<div class="k18" data-kpi="kpi-18">
  <div class="grid">
    <div class="fig"><b>{f.prompts}{plus}</b><span>{t.kpi18Prompts}</span></div>
    <div class="fig"><b>{f.commits}{plus}</b><span>{t.kpi18Commits}</span></div>
    <div class="fig"><b class:wait={f.waitingNow > 0}>{f.waitingNow}</b><span>{f.waitingNow ? t.kpi18WaitingSince(Math.max(1, Math.round((f.oldestWaitS ?? 0) / 60))) : t.kpi18NoneWaiting}</span></div>
    <div class="fig"><b>{tokens}</b><span>{f.topSession ? t.kpi18Tokens(f.topSession, Math.round(f.topShare * 100)) : t.kpi18TokensNone}</span></div>
  </div>
  <div class="lbl"><span class="pl">{t.kpi18Prompt}</span> {t.kpi18And} <span class="cl">{t.kpi18Commit}</span> {t.kpi18PerHour}</div>
  <div class="hours" role="img" aria-label={t.kpi18HoursAria}>
    {#each f.perHour as [p, c], i (i)}
      <div class="col">
        <span class="stack">{#if c}<i class="c" style:height="{(c / top) * 56}px"></i>{/if}{#if p}<i class="p" style:height="{(p / top) * 56}px"></i>{/if}</span>
        <small>{(f.firstHour + i) % 4 === 0 ? f.firstHour + i : ''}</small>
      </div>
    {/each}
  </div>
</div>

<style>
  .k18 { display: flex; flex-direction: column; gap: 8px; }
  .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
  .fig { background: var(--low); border-radius: 16px; padding: 10px 12px; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
  .fig b { font: 500 24px/1.1 var(--sans, inherit); }
  .fig b.wait { color: var(--wait); }
  .fig span { font-size: 13px; color: var(--text2); overflow-wrap: anywhere; }
  .hours { display: flex; align-items: flex-end; gap: 3px; background: var(--low); border-radius: 16px; padding: 10px 12px 6px; }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: stretch; gap: 3px; }
  .stack { display: flex; flex-direction: column; justify-content: flex-end; gap: 2px; height: 58px; }
  .stack i { display: block; border-radius: 2px; }
  .p { background: var(--busy); } .c { background: var(--good); }
  .col small { font: 10px/1 var(--mono); color: var(--text2); text-align: center; height: 10px; }
  .lbl { font-size: 12px; color: var(--text2); display: flex; gap: 12px; }
  .pl { color: var(--busy); } .cl { color: var(--good); }
</style>
