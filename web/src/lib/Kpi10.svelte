<script lang="ts">
  import type { KpiTurn } from './contract'
  import { turnsSummary } from './kpi'
  import { locale } from './lang'
  import { t } from './t'
  // Proposta 10 (25/10, contratto 1.96): un blocco per turno di oggi, lungo quanto il turno e col colore dell'esito; sopra,
  // la linea del costo per turno con il primo e l'ultimo valore scritti sul grafico.
  let { turns }: { turns: KpiTurn[] } = $props()
  const s = $derived(turnsSummary(turns))
  const fmt = new Intl.NumberFormat(locale, { notation: 'compact', maximumFractionDigits: 0 })
  const W = 360, H = 58, BAR = 14, GAP = 2, MIN = 4, LINE_TOP = 14
  const TONE: Record<string, string> = { ok: 'var(--good)', fail: 'var(--b-alert)', question: 'var(--wait)', running: 'var(--busy)' }
  const geo = $derived.by(() => {
    const durs = turns.map(x => Math.max(1, x.end - x.start))
    const avail = W - GAP * Math.max(0, turns.length - 1)
    const k = Math.max(0, avail - MIN * turns.length) / Math.max(1, durs.reduce((a, b) => a + b, 0))
    const lineH = H - BAR - LINE_TOP - 8
    const top = Math.max(1, s.tokensMax)
    let x = 0
    return turns.map((tu, i) => {
      const w = MIN + durs[i] * k
      const g = { x, w, cx: x + w / 2, cy: LINE_TOP + lineH * (1 - tu.tokens / top), tone: TONE[tu.outcome] ?? 'var(--busy)' }
      x += w + GAP
      return g
    })
  })
  const first = $derived(fmt.format(s.tokensFirst))
  const last = $derived(fmt.format(s.tokensLast))
</script>

<div class="k10" data-kpi="kpi-10">
  <div class="head"><span>{t.kpi10Title}</span><b>{t.kpi10Turns(s.n, s.minutes)}{#if s.failed}<em> · {t.kpi10Failed(s.failed)}</em>{/if}</b></div>
  <div class="plot">
  <svg viewBox="0 0 {W} {H}" preserveAspectRatio="none" role="img" aria-label={t.kpi10Aria(s.n, s.minutes, first, last)}>
    {#each geo as g, i (i)}<rect x={g.x} y={H - BAR} width={g.w} height={BAR} rx="3" fill={g.tone} />{/each}
    {#if geo.length > 1}<polyline points={geo.map(g => `${g.cx},${g.cy}`).join(' ')} fill="none" stroke="var(--b-week)" stroke-width="2" vector-effect="non-scaling-stroke" />{/if}
    {#each geo as g, i (i)}<circle cx={g.cx} cy={g.cy} r="3" fill="var(--b-week)" />{/each}
  </svg>
  {#if geo.length}
    <div class="vals">
      <span style:left="{(geo[0].cx / W) * 100}%" style:top="{geo[0].cy - 14}px" class="first">{first}</span>
      {#if geo.length > 1 && geo.at(-1)!.cx - geo[0].cx > 70}<span style:left="{(geo.at(-1)!.cx / W) * 100}%" style:top="{geo.at(-1)!.cy - 14}px" class="last">{last}</span>{/if}
    </div>
  {/if}
  </div>
  <small>{t.kpi10Caption}</small>
</div>

<style>
  .k10 { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
  .plot { position: relative; }
  .head { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 2px 12px; font: 500 11px/1.3 var(--mono); color: var(--text2); letter-spacing: .06em; }
  .head span { text-transform: uppercase; white-space: nowrap; }
  .head b { font-weight: 400; letter-spacing: 0; }
  .head em { font-style: normal; color: var(--b-alert); }
  svg { width: 100%; height: 58px; display: block; }
  .vals { position: absolute; left: 0; right: 0; top: 0; height: 0; }
  .vals span { position: absolute; font: 10px/1 var(--mono); color: var(--b-week); white-space: nowrap; }
  .vals .first { transform: translateX(0); }
  .vals .last { transform: translateX(-100%); }
  small { font: 10px/1.2 var(--mono); color: var(--b-week); }
</style>
