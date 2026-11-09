<script lang="ts">
  import type { MonitorCard } from './contract'
  import { formOf, isLook, lastIndex, maxIndex, meter, minIndex, nearest, scale, span, times, up, values, xOf, yOf } from './monitors'
  import { t } from './t'
  import { lang } from './lang'
  // Il grafico di un monitoraggio nella forma della sua scheda (app-monitoraggi-09-11, contratto 1.132), come ChartOf in Kotlin: una
  // scala per grafico, righe da 2 px, griglia leggera, il pallino sul minimo e sull'ultimo valore, al tocco il giorno e il valore; le
  // serie in grigio chiaro, l'ambra solo per lo stato. Una forma fuori dal catalogo non si disegna.
  let { c }: { c: MonitorCard } = $props()
  const form = $derived(formOf(c))
  const sp = $derived(span(c))
  const first = $derived(c.series[0] ? values(c.series[0]) : [])
  const firstT = $derived(c.series[0] ? times(c.series[0]) : [])
  let w = $state(320)
  let sel = $state<number | null>(null)
  const num = (v: number, unit?: string | null) => {
    const n = new Intl.NumberFormat(lang === 'en' ? 'en-GB' : 'it-IT', { maximumFractionDigits: v % 1 === 0 ? 0 : 1, useGrouping: 'always' }).format(v)
    return !unit ? n : unit === '%' ? `${n}%` : unit === '€' ? `${n} €` : `${n} ${unit}`
  }
  const day = (ts: number) => { const d = new Date(ts * 1000); return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}` }
  const tapX = (e: MouseEvent) => { const r = (e.currentTarget as Element).getBoundingClientRect(); return (e.clientX - r.left) / r.width }
  // La serie come linea nel riquadro largo `cw` e alto `h` da `top`, sull'asse del tempo comune.
  function path(vs: (number | null)[], ts: number[], cw: number, h: number, top: number) {
    const s = scale(vs)
    return vs.map((v, i) => (v == null ? '' : `${i ? 'L' : 'M'}${(xOf(ts[i], sp) * cw).toFixed(1)} ${(top + (1 - yOf(s, v)) * h).toFixed(1)}`)).join('')
  }
  const at = (vs: (number | null)[], ts: number[], i: number, cw: number, h: number, top: number) => ({ x: xOf(ts[i], sp) * cw, y: top + (1 - yOf(scale(vs), vs[i]!)) * h })
  const LEFT = 40
  const three = $derived([...new Set([0, Math.floor(firstT.length / 2), firstT.length - 1])].filter(i => i >= 0))
</script>

{#if form}
<div class="chart" data-testid="monitor-grafico-{form}" bind:clientWidth={w}>
  {#if form === 'tile'}
    <p class="big">{c.value ?? ''}{#if c.delta}<small>{c.delta}</small>{/if}</p>
    {#if first.filter(v => v != null).length >= 2}
      {@const cw = w - 8}{@const li = lastIndex(first)}
      <svg width={w} height="64" aria-hidden="true"><path class="ln" d={path(first, firstT, cw, 58, 3)} />{#if li != null}{@const p = at(first, firstT, li, cw, 58, 3)}<circle class="dot" cx={p.x} cy={p.y} r="3.5" />{/if}</svg>
    {/if}

  {:else if form === 'line' && first.length}
    {@const s = scale(first)}{@const cw = w - LEFT - 4}{@const h = 150 - 32}{@const mx = maxIndex(first)}{@const li = lastIndex(first)}
    <p class="pick" data-testid="monitor-tocco">{sel != null && first[sel] != null ? `${day(firstT[sel])} · ${num(first[sel]!, c.unit)}` : ' '}</p>
    <svg width={w} height="150" role="img" aria-label={c.title} onclick={(e) => { const i = nearest(firstT, (tapX(e) * w - LEFT) / cw, sp); sel = sel === i ? null : i }}>
      {#each s.ticks as tk}{@const y = 14 + (1 - yOf(s, tk)) * h}<line class="grid" x1={LEFT} x2={w} y1={y} y2={y} /><text class="lab" x="0" y={y + 4}>{num(tk)}</text>{/each}
      <g transform="translate({LEFT} 0)">
        {#if sel != null}{@const x = xOf(firstT[sel], sp) * cw}<line class="sel" x1={x} x2={x} y1="14" y2={14 + h} />{/if}
        <path class="ln" d={path(first, firstT, cw, h, 14)} />
        {#if mx != null && mx !== li}{@const p = at(first, firstT, mx, cw, h, 14)}<circle class="dot" cx={p.x} cy={p.y} r="3.5" /><text class="lab" x={Math.min(Math.max(p.x, 24), cw - 24)} y={p.y - 8} text-anchor="middle">{t.monitorMax(num(first[mx]!, c.unit))}</text>{/if}
        {#if li != null}{@const p = at(first, firstT, li, cw, h, 14)}<circle class="dot" cx={p.x} cy={p.y} r="3.5" /><text class="lab strong" x={p.x - 6} y={p.y + 16} text-anchor="end">{c.series[0].last_text ?? num(first[li]!, c.unit)}</text>{/if}
        {#each three as i, k}<text class="lab" x={xOf(firstT[i], sp) * cw} y="148" text-anchor={k === 0 ? 'start' : i === firstT.length - 1 ? 'end' : 'middle'}>{day(firstT[i])}</text>{/each}
      </g>
    </svg>

  {:else if form === 'multiples'}
    {#if sel != null}<p class="pick" data-testid="monitor-tocco">{day(sel)} · {c.series.map(x => { const i = nearest(times(x), xOf(sel!, sp), sp); return i != null && x.points[i] ? `${x.name ?? ''} ${num(x.points[i][1], c.unit)}` : '' }).filter(Boolean).join(' · ')}</p>{/if}
    {#each c.series as x, k (k)}
      {@const vs = values(x)}{@const ts = times(x)}{@const cw = Math.max(40, w - 120 - 8)}{@const mn = minIndex(vs)}{@const li = lastIndex(vs)}
      {@const hit = c.hits.find(h => h.series === x.name)}
      <div class="mult" class:first={k === 0}>
        <div class="mg">
          <p class="mn"><b>{x.name ?? ''}</b></p>
          <svg width={cw + 8} height="52" role="img" aria-label={x.name ?? ''} onclick={(e) => { const tt = sp[0] + tapX(e) * (cw + 8) / cw * (sp[1] - sp[0]); sel = sel != null && Math.abs(sel - tt) < (sp[1] - sp[0]) / 60 ? null : tt }}>
            <line class="grid" x1="0" x2={cw + 8} y1="51" y2="51" />
            {#if sel != null}<line class="sel" x1={xOf(sel, sp) * cw} x2={xOf(sel, sp) * cw} y1="0" y2="52" />{/if}
            <path class="ln" d={path(vs, ts, cw, 40, 6)} />
            {#if hit}{#if li != null}{@const p = at(vs, ts, li, cw, 40, 6)}<circle class="look" cx={p.x} cy={p.y} r="4" />{/if}
            {:else}
              {#if mn != null && mn !== li}{@const p = at(vs, ts, mn, cw, 40, 6)}<circle class="hole" cx={p.x} cy={p.y} r="4" />{/if}
              {#if li != null}{@const p = at(vs, ts, li, cw, 40, 6)}<circle class="dot" cx={p.x} cy={p.y} r="3.5" />{/if}
            {/if}
          </svg>
        </div>
        <p class="mv"><b>{x.last_text ?? (x.last != null ? num(x.last, c.unit) : '')}</b>{#if hit}<small class="look">{hit.text}</small>{:else if x.min != null}<small>{t.monitorMin(num(x.min, c.unit))}</small>{/if}</p>
      </div>
    {/each}
    {#if firstT.length >= 2}<p class="days" style:padding-right="120px"><span>{day(sp[0])}</span><span>{day(Math.round((sp[0] + sp[1]) / 2))}</span><span>{day(sp[1])}</span></p>{/if}

  {:else if form === 'bars' && first.length}
    {@const s = scale(first, true)}{@const cw = w - LEFT}{@const h = 130 - 32}{@const slot = cw / first.length}{@const bw = Math.max(1, slot * 0.7)}{@const mx = maxIndex(first)}
    <p class="pick" data-testid="monitor-tocco">{sel != null && first[sel] != null ? `${day(firstT[sel])} · ${num(first[sel]!, c.unit)}` : ' '}</p>
    <svg width={w} height="130" role="img" aria-label={c.title} onclick={(e) => { const i = Math.min(first.length - 1, Math.max(0, Math.floor((tapX(e) * w - LEFT) / slot))); sel = sel === i ? null : i }}>
      {#each s.ticks as tk}{@const y = 14 + (1 - yOf(s, tk)) * h}<line class="grid" x1={LEFT} x2={w} y1={y} y2={y} /><text class="lab" x="0" y={y + 4}>{num(tk)}</text>{/each}
      <g transform="translate({LEFT} 0)">
        {#each first as v, i}{#if v != null}{@const y = 14 + (1 - yOf(s, v)) * h}<rect class="bar" class:on={sel === i} x={i * slot + (slot - bw) / 2} y={y} width={bw} height={14 + h - y} />{/if}{/each}
        {#if mx != null}{@const y = 14 + (1 - yOf(s, first[mx]!)) * h}<text class="lab" x={Math.min(Math.max(mx * slot + slot / 2, 40), cw - 40)} y={Math.max(10, y - 4)} text-anchor="middle">{t.monitorMaxOn(num(first[mx]!), day(firstT[mx]))}</text>{/if}
        {#each three as i, k}<text class="lab" x={i * slot + slot / 2} y="128" text-anchor={k === 0 ? 'start' : i === first.length - 1 ? 'end' : 'middle'}>{day(firstT[i])}</text>{/each}
      </g>
    </svg>

  {:else if form === 'meter'}
    {@const [fill, mark] = meter(c)}
    <p class="big" class:look={isLook(c)}>{c.value ?? ''}{#if c.limit}<small>{t.monitorLimit(c.limit)}</small>{/if}</p>
    <div class="meter" class:over={isLook(c)}><i style:width="{fill * 100}%"></i>{#if mark != null}<b style:left="{mark * 100}%"></b>{/if}</div>

  {:else if form === 'table'}
    {#each c.rows ?? [] as r, i (i)}<p class="trow"><span>{r.name}</span><b>{r.value}</b>{#if r.text}<small>{r.text}</small>{/if}</p>{/each}

  {:else if form === 'status' && first.length}
    {@const ok = first.filter(up).length}
    <p class="pick strong" data-testid="monitor-tocco">{sel != null ? `${day(firstT[sel])} · ${up(first[sel]) ? t.monitorYes : t.monitorNo}` : t.monitorDaysOk(ok, first.length)}</p>
    <div class="cells">{#each first as v, i}<button class="cell" class:off={!up(v)} class:sel={sel === i} aria-label={day(firstT[i])} onclick={() => (sel = sel === i ? null : i)}></button>{/each}</div>
    {#if firstT.length >= 2}<p class="days"><span>{day(firstT[0])}</span><span>{day(firstT[firstT.length - 1])}</span></p>{/if}
  {/if}
</div>
{/if}

<style>
  .chart { display: flex; flex-direction: column; gap: 8px; padding: 12px 14px; border-radius: 14px; background: var(--s1); border: 1px solid var(--line); min-width: 0; }
  .big { display: flex; align-items: baseline; gap: 10px; font: 700 34px/38px var(--display); color: var(--tx); }
  .big.look { color: var(--wait); }
  .big small { font: 400 14px var(--sans, inherit); color: var(--tx2); }
  .pick { font: 12.5px/17px var(--mono); color: var(--tx); min-height: 17px; white-space: pre; overflow: hidden; text-overflow: ellipsis; }
  svg { display: block; overflow: visible; cursor: pointer; }
  .ln { fill: none; stroke: var(--tx); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  .grid { stroke: var(--line); stroke-width: 1; }
  .sel { stroke: var(--tx3); stroke-width: 1; }
  .dot { fill: var(--tx); }
  .hole { fill: var(--s1); stroke: var(--tx); stroke-width: 1.5; }
  .look { fill: var(--wait); color: var(--wait); }
  .lab { font: 11.5px var(--mono); fill: var(--tx3); }
  .lab.strong { fill: var(--tx); font-weight: 500; }
  .bar { fill: var(--tx); }
  .bar.on { fill: var(--tx2); }
  .mult { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-top: 1px solid var(--line); }
  .mult.first { border-top: 0; padding-top: 0; }
  .mg { flex: 1; min-width: 0; }
  .mn { margin-bottom: 4px; white-space: nowrap; }
  .mn b { font: 600 17px/20px var(--display); }
  .mv { width: 108px; flex: none; display: flex; flex-direction: column; align-items: flex-end; white-space: nowrap; }
  .mv b { font: 700 17px/20px var(--display); }
  .mv small { font-size: 14px; line-height: 19px; color: var(--tx2); white-space: normal; text-align: right; }
  .mv small.look { color: var(--wait); }
  .days { display: flex; justify-content: space-between; font: 11.5px var(--mono); color: var(--tx3); }
  .meter { position: relative; height: 14px; border-radius: 7px; background: var(--s3); margin: 4px 0; }
  .meter i { position: absolute; inset: 0 auto 0 0; border-radius: 7px; background: var(--tx); }
  .meter.over i { background: var(--wait); }
  .meter b { position: absolute; top: -3px; bottom: -3px; width: 2px; background: var(--tx2); }
  .trow { display: flex; align-items: center; min-height: 40px; border-top: 1px solid var(--line); gap: 12px; }
  .trow:first-of-type { border-top: 0; }
  .trow span { flex: 1; min-width: 0; font-size: 15.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .trow b { font: 700 17px var(--display); white-space: nowrap; }
  .trow small { min-width: 72px; text-align: right; font-size: 14px; color: var(--tx2); white-space: nowrap; }
  .cells { display: flex; gap: 3px; }
  .cell { flex: 1; height: 22px; border-radius: 3px; background: var(--tx2); }
  .cell.off { background: var(--wait); }
  .cell.sel { box-shadow: inset 0 0 0 1.5px var(--tx); }
</style>
