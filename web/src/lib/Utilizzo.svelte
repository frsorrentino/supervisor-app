<script lang="ts">
  import type { KpiData, State, TimelinePage } from './contract'
  import type { Ring } from './overview'
  import { dayFigures, split } from './kpi'
  import { locale } from './lang'
  import { t } from './t'
  import { elapsedPct, H5_S, peakHour, W7_S, waitsLine, weekDelta } from './utilizzo'
  // Il blocco Utilizzo aperto (scelta D di Franz, 30/10 20:11; UtilizzoV3.kt nell'app): righe compatte, un tocco apre il mini-grafico
  // di quella riga, una aperta per volta. Tutte le frecce su una verticale. Barra e grafico hanno la stessa colonna, così la tacca e
  // «adesso» cadono nello stesso punto; il resto sta nella pagina «Utilizzo e limiti», da «Tutti i dettagli».
  let { rings, st, page, data, now, onDetails, startOpen = null }: { rings: Ring[]; st: State; page: TimelinePage | null; data: KpiData | null; now: number; onDetails: () => void; startOpen?: string | null } = $props()
  let open = $state<string | null>(startOpen)
  const toggle = (k: string) => (open = open === k ? null : k)
  const shown = $derived(rings.some(r => !r.stale) ? rings.filter(r => !r.stale) : rings)
  const many = $derived(shown.length > 1)
  const hm = (s: number) => new Date(s * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
  const dayTime = (s: number) => new Date(s * 1000).toLocaleString(locale, { weekday: 'short', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
  const pc = (x: number | null | undefined) => Math.max(0, Math.min(100, Math.round(x ?? 0)))
  const tone = (p: number) => (p >= 90 ? 'var(--red)' : p >= 75 ? 'var(--wait)' : 'var(--tx2)')
  const top = $derived(shown.filter(r => !r.stale).sort((a, b) => (b.h5 ?? 0) - (a.h5 ?? 0))[0] ?? null)
  const sp = $derived(top ? split(st, top.account, 99) : null)
  const f = $derived(page ? dayFigures(page, st, now) : null)
  const tok = (n: number) => (n >= 1e6 ? `${Math.floor(n / 1e6)} M` : `${Math.floor(n / 1e3)} k`)
  const maxHour = $derived(Math.max(1, ...(f?.perHour ?? []).map(([p, c]: [number, number]) => p + c)))
  const waits = $derived(data?.kpi_waits ? waitsLine(data.kpi_waits.week ?? []) : null)
  function curve(r: Ring) {
    if (!r.resetAt) return null
    const start = r.resetAt - H5_S
    const x = (s: number) => Math.max(0, Math.min(100, ((s - start) / H5_S) * 100))
    const y = (p: number) => 58 * (1 - pc(p) / 100)
    const pts = r.pace?.points ?? []
    const line = pts.map(s => `${x(s.ts)},${y(s.pct)}`).join(' ')
    const last = pts[pts.length - 1]
    return { line, area: pts.length > 1 ? `${x(pts[0].ts)},58 ${line} ${x(last.ts)},58` : '', last: last ? { x: x(last.ts), y: y(last.pct) } : null, end: r.pace?.projected != null ? y(r.pace.projected) : null }
  }
</script>

{#snippet arrow(k: string)}<span class="ms ar" aria-hidden="true">{open === k ? 'expand_less' : 'expand_more'}</span>{/snippet}
{#snippet label(text: string, personal: boolean)}<span class="lab">{#if many}<i class="shape" class:sq={!personal}></i>{/if}{text}</span>{/snippet}
{#snippet meter(p: number, projected: number | null, tick: number | null)}
  <span class="meter"><i style="width:{p}%;background:{tone(p)}"></i>{#if projected != null && projected > p}<u style="left:{p}%;width:{projected - p}%"></u>{/if}{#if tick != null}<s style="left:{tick}%"></s>{/if}</span>
{/snippet}

<div class="ut" data-testid="utilizzo-v3">
  {#each shown as r (r.account)}
    {@const p = pc(r.h5)}
    {@const proj = r.pace?.projected ?? null}
    {@const tk = elapsedPct(r.resetAt, H5_S, now)}
    {@const c = curve(r)}
    <button class="row" aria-expanded={open === `h5:${r.account}`} onclick={() => toggle(`h5:${r.account}`)}>
      {@render label(t.kpi7Quota, r.personal)}{@render meter(p, proj, tk)}
      <span class="val">{r.stale ? t.quotaLineStale : proj != null ? `${p}% → ${proj}%` : `${p}%`}</span>{@render arrow(`h5:${r.account}`)}
    </button>
    {#if open === `h5:${r.account}`}
      <div class="open">
        {#if c}
          <div class="col2">
            <svg viewBox="0 0 100 58" preserveAspectRatio="none" width="100%" height="58" aria-hidden="true">
              {#if c.area}<polygon points={c.area} fill="var(--s2)" /><polyline points={c.line} fill="none" stroke="var(--tx2)" stroke-width="2" vector-effect="non-scaling-stroke" />{/if}
              {#if c.last && c.end != null}<line x1={c.last.x} y1={c.last.y} x2="100" y2={c.end} stroke="var(--tx3)" stroke-width="2" stroke-dasharray="3 4" vector-effect="non-scaling-stroke" />{/if}
              {#if tk != null}<line x1={tk} y1="0" x2={tk} y2="58" stroke="var(--tx)" stroke-width="2" vector-effect="non-scaling-stroke" />{/if}
            </svg>
            <div class="axis"><span>{hm(r.resetAt! - H5_S)}</span>{#if tk != null && tk > 22 && tk < 78}<b style="left:{tk}%">{t.usageNow}</b>{/if}<span>{hm(r.resetAt!)}</span></div>
          </div>
        {/if}
        <p class="note">{proj != null && r.resetAt ? t.usageProjection(proj, hm(r.resetAt)) : r.resetAt ? t.quotaResetsAt(hm(r.resetAt)) : t.quotaLineStale}</p>
      </div>
    {/if}
  {/each}
  {#each shown as r (r.account)}
    {@const p = pc(r.w7)}
    {@const d = weekDelta(r.w7, r.weekResetAt, now)}
    <button class="row" aria-expanded={open === `w7:${r.account}`} onclick={() => toggle(`w7:${r.account}`)}>
      {@render label(t.usageWeek, r.personal)}{@render meter(p, null, elapsedPct(r.weekResetAt, W7_S, now))}
      <span class="val">{p}%</span>{@render arrow(`w7:${r.account}`)}
    </button>
    {#if open === `w7:${r.account}`}
      <div class="open">
        {#if d != null}<p class="note">{d < 0 ? t.usageBelowPace(-d) : d > 0 ? t.usageAbovePace(d) : t.weekEven}</p>{/if}
        {#if r.weekResetAt}<p class="note">{t.weekRenewal(dayTime(r.weekResetAt))}</p>{/if}
        {#if r.weekOrigins}<p class="note">{t.weekOriginSessions(Math.round(r.weekOrigins.sessions))} · {t.weekOriginAutomations(Math.round(r.weekOrigins.automations))} · {t.weekOriginRequests(Math.round(r.weekOrigins.requests))}</p>{/if}
      </div>
    {/if}
  {/each}
  {#if sp?.slices.length}
    <button class="row wide" aria-expanded={open === 'sessions'} onclick={() => toggle('sessions')}>
      <span class="lab">{t.homeSessions}</span><span class="name">{sp.slices[0].session}</span><span class="val auto">{sp.slices[0].pct}%</span>{@render arrow('sessions')}
    </button>
    {#if open === 'sessions'}
      <div class="open list">
        {#each sp.slices as s (s.session)}<div class="sl"><span>{s.session}</span><b>{s.pct}%</b></div>{/each}
        {#if sp.others > 0}<div class="sl dim"><span>{t.kpi4Others}</span><b>{sp.others}%</b></div>{/if}
      </div>
    {/if}
  {/if}
  {#if f}
    <button class="row wide" aria-expanded={open === 'today'} onclick={() => toggle('today')}>
      <span class="lab">{t.usageToday}</span><span class="val grow">{f.prompts} prompt · {f.commits} commit · {tok(f.tokens)}</span>{@render arrow('today')}
    </button>
    {#if open === 'today'}
      <div class="open">
        <div class="hours">{#each f.perHour as [p, c], i (i)}<i class:zero={p + c === 0} style="height:{p + c === 0 ? 8 : Math.max(15, ((p + c) / maxHour) * 100)}%"></i>{/each}</div>
        {#if peakHour(f.perHour, f.firstHour) != null}<p class="note">{t.usagePeak(peakHour(f.perHour, f.firstHour)!)}</p>{/if}
        {#if waits}<p class="note">{t.usageWaits(waits.medianMin, waits.closed)}</p>{/if}
      </div>
    {/if}
  {/if}
  <button class="row wide more" onclick={onDetails}><span class="lab grow">{t.usageDetails}</span><span class="ms ar" aria-hidden="true">chevron_right</span></button>
</div>

<style>
  .ut { --lab: 84px; --val: 80px; --ar: 24px; }
  .row { display: grid; grid-template-columns: var(--lab) minmax(0, 1fr) var(--val) var(--ar); column-gap: 10px; align-items: center; width: 100%; min-height: 44px; padding: 6px 12px 6px 14px; border-top: 1px solid var(--line); text-align: left; color: var(--tx); }
  .row:hover { background: var(--s2); }
  .row.wide { grid-template-columns: var(--lab) minmax(0, 1fr) auto var(--ar); }
  .row.more { grid-template-columns: minmax(0, 1fr) var(--ar); }
  .lab { display: flex; align-items: center; gap: 5px; font: 600 15px var(--display); }
  .shape { width: 9px; height: 9px; border: 2px solid var(--tx2); border-radius: 50%; flex: none; }
  .shape.sq { border-radius: 3px; }
  .meter { position: relative; height: 6px; border-radius: 3px; background: var(--s3); }
  .meter i { position: absolute; inset: 0 auto 0 0; border-radius: 3px; z-index: 1; }
  .meter u { position: absolute; top: 0; bottom: 0; background: repeating-linear-gradient(90deg, var(--tx3) 0 4px, transparent 4px 7px); }
  .meter s { position: absolute; top: -3px; width: 2px; height: 12px; margin-left: -1px; border-radius: 1px; background: var(--tx); z-index: 2; }
  .val { font: 400 12.5px var(--mono); color: var(--tx); text-align: right; }
  .val.auto { white-space: nowrap; }
  .val.grow { grid-column: 2 / 4; }
  .name { font-size: 14px; color: var(--tx); text-align: right; overflow-wrap: anywhere; }
  .ms.ar { color: var(--tx3); font-size: 24px; }
  .open { padding: 0 calc(12px + var(--ar) + 10px) 10px 14px; }
  /* La colonna del grafico è quella della barra. */
  .col2 { margin: 0 calc(var(--val) + 10px) 0 calc(var(--lab) + 10px); }
  .col2 svg { display: block; }
  .axis { position: relative; display: flex; justify-content: space-between; font: 400 12px var(--mono); color: var(--tx2); margin-top: 2px; }
  .axis b { position: absolute; transform: translateX(-50%); font-weight: 400; color: var(--tx); }
  .note { margin: 4px 0 0; font-size: 13.5px; line-height: 1.35; color: var(--tx2); }
  .sl { display: flex; justify-content: space-between; gap: 10px; padding: 2px 0; font-size: 14px; color: var(--tx); overflow-wrap: anywhere; }
  .sl b { font: 400 12.5px var(--mono); }
  .sl.dim span { color: var(--tx2); }
  .hours { display: flex; align-items: flex-end; gap: 3px; height: 28px; }
  .hours i { flex: 1; border-radius: 1.5px; background: var(--tx2); }
  .hours i.zero { background: var(--s3); }
</style>
