<script lang="ts">
  import { dayAt, formOf, lastIndex, maxIndex, meter, minIndex, scale, yOf, type Chart } from './monitors'
  import { t } from './t'
  import { lang } from './lang'
  // Un grafico di un monitoraggio nella sua forma (app-monitoraggi-09-11), come ChartOf in Kotlin: una scala per grafico, righe da 2 px,
  // griglia leggera, il pallino sul minimo e sull'ultimo valore, al tocco il giorno e il valore; le serie in grigio chiaro, l'ambra
  // solo per lo stato. Una forma fuori dal catalogo non si disegna.
  let { c }: { c: Chart } = $props()
  const form = $derived(formOf(c))
  const days = $derived(c.days ?? [])
  const series = $derived(c.series ?? [])
  const first = $derived(series[0]?.values ?? [])
  let w = $state(320)
  let sel = $state<number | null>(null)
  const num = (v: number, unit?: string | null) => {
    const n = new Intl.NumberFormat(lang === 'en' ? 'en-GB' : 'it-IT', { maximumFractionDigits: v % 1 === 0 ? 0 : 1 }).format(v)
    return !unit ? n : unit === '%' ? `${n}%` : `${n} ${unit}`
  }
  const tap = (e: MouseEvent, n: number) => { const r = (e.currentTarget as Element).getBoundingClientRect(); const i = dayAt((e.clientX - r.left) / r.width, n); sel = sel === i ? null : i }
  // La serie come linea (o a gradini) nel riquadro largo `cw` e alto `h` da `top`; null salta.
  function path(vs: (number | null)[], cw: number, h: number, top: number, step = false, n = vs.length) {
    const s = scale(vs)
    let d = '', last: [number, number] | null = null
    vs.forEach((v, i) => {
      if (v == null) return
      const p: [number, number] = [n <= 1 ? 0 : (i * cw) / (n - 1), top + (1 - yOf(s, v)) * h]
      d += !last ? `M${p[0]} ${p[1]}` : step ? `L${p[0]} ${last[1]}L${p[0]} ${p[1]}` : `L${p[0]} ${p[1]}`
      last = p
    })
    return d
  }
  const at = (vs: (number | null)[], i: number, cw: number, h: number, top: number, n = vs.length) => ({ x: n <= 1 ? 0 : (i * cw) / (n - 1), y: top + (1 - yOf(scale(vs), vs[i]!)) * h })
  const LEFT = 40
  const three = (n: number) => [...new Set([0, Math.floor(n / 2), n - 1])]
</script>

{#if form}
<div class="chart" data-testid="monitor-grafico-{form}" bind:clientWidth={w}>
  {#if c.title || c.sub}<p class="hd"><b>{c.title ?? ''}</b>{#if c.sub}<small>{c.sub}</small>{/if}</p>{/if}

  {#if form === 'tile'}
    <p class="big">{c.value ?? ''}{#if c.delta}<small>{c.delta}</small>{/if}</p>
    {#if first.filter(v => v != null).length >= 2}
      {@const cw = w - 8}{@const li = lastIndex(first)}
      <svg width={w} height="64" aria-hidden="true"><path class="ln" d={path(first, cw, 58, 3)} />{#if li != null}{@const p = at(first, li, cw, 58, 3)}<circle class="dot" cx={p.x} cy={p.y} r="3.5" />{/if}</svg>
    {/if}

  {:else if form === 'line' && first.length}
    {@const s = scale(first)}{@const cw = w - LEFT - 4}{@const h = 150 - 32}{@const mx = maxIndex(first)}{@const li = lastIndex(first)}
    <p class="pick" data-testid="monitor-tocco">{sel != null && first[sel] != null ? `${days[sel] ?? ''} · ${num(first[sel]!, c.unit)}` : ' '}</p>
    <svg width={w} height="150" role="img" aria-label={c.title ?? ''} onclick={(e) => tap(e, first.length)}>
      {#each s.ticks as tk}{@const y = 14 + (1 - yOf(s, tk)) * h}<line class="grid" x1={LEFT} x2={w} y1={y} y2={y} /><text class="lab" x="0" y={y + 4}>{num(tk)}</text>{/each}
      <g transform="translate({LEFT} 0)">
        {#if sel != null}{@const x = (sel * cw) / Math.max(1, first.length - 1)}<line class="sel" x1={x} x2={x} y1="14" y2={14 + h} />{/if}
        <path class="ln" d={path(first, cw, h, 14, !!c.step)} />
        {#if mx != null && mx !== li}{@const p = at(first, mx, cw, h, 14)}<circle class="dot" cx={p.x} cy={p.y} r="3.5" /><text class="lab" x={Math.min(Math.max(p.x, 24), cw - 24)} y={p.y - 8} text-anchor="middle">{t.monitorMax(num(first[mx]!, c.unit))}</text>{/if}
        {#if li != null}{@const p = at(first, li, cw, h, 14)}<circle class="dot" cx={p.x} cy={p.y} r="3.5" /><text class="lab strong" x={p.x - 6} y={p.y + 16} text-anchor="end">{num(first[li]!, c.unit)}</text>{/if}
        {#if days.length >= 2}{#each three(days.length) as i, k}<text class="lab" x={(i * cw) / (days.length - 1)} y="148" text-anchor={k === 0 ? 'start' : i === days.length - 1 ? 'end' : 'middle'}>{days[i]}</text>{/each}{/if}
      </g>
    </svg>

  {:else if form === 'multiples'}
    {@const n = Math.max(days.length, ...series.map(x => x.values.length))}
    {#if sel != null}<p class="pick" data-testid="monitor-tocco">{days[sel] ?? ''} · {series.filter(x => x.values[sel!] != null).map(x => `${x.name ?? ''} ${num(x.values[sel!]!, c.unit)}`).join(' · ')}</p>{/if}
    {#each series as x, k (k)}
      {@const cw = Math.max(40, w - 120 - 8)}{@const mn = minIndex(x.values)}{@const li = lastIndex(x.values)}
      <div class="mult" class:first={k === 0}>
        <div class="mg">
          <p class="mn"><b>{x.name ?? ''}</b>{#if x.sub}<small>{x.sub}</small>{/if}</p>
          <svg width={cw + 8} height="52" role="img" aria-label={x.name ?? ''} onclick={(e) => tap(e, n)}>
            <line class="grid" x1="0" x2={cw + 8} y1="51" y2="51" />
            {#each c.marks ?? [] as mk}{@const i = days.indexOf(mk.day)}{#if i >= 0}<line class="mark" x1={(i * cw) / (n - 1)} x2={(i * cw) / (n - 1)} y1="0" y2="52" />{/if}{/each}
            {#if sel != null}<line class="sel" x1={(sel * cw) / (n - 1)} x2={(sel * cw) / (n - 1)} y1="0" y2="52" />{/if}
            <path class="ln" d={path(x.values, cw, 40, 6, !!c.step, n)} />
            {#if x.look}{#if li != null}{@const p = at(x.values, li, cw, 40, 6, n)}<circle class="look" cx={p.x} cy={p.y} r="4" />{/if}
            {:else}
              {#if mn != null && mn !== li}{@const p = at(x.values, mn, cw, 40, 6, n)}<circle class="hole" cx={p.x} cy={p.y} r="4" />{/if}
              {#if li != null}{@const p = at(x.values, li, cw, 40, 6, n)}<circle class="dot" cx={p.x} cy={p.y} r="3.5" />{/if}
            {/if}
          </svg>
        </div>
        <p class="mv">{#if x.value}<b>{x.value}</b>{/if}{#if x.note}<small class:look={x.look}>{x.note}</small>{/if}{#if x.delta}<small>{x.delta}</small>{/if}</p>
      </div>
    {/each}
    {#if days.length >= 2}{@const picks = [...new Set([0, Math.floor(days.length / 3), Math.floor((2 * days.length) / 3), days.length - 1])]}
      <p class="days" style:padding-right="120px">{#each picks as i}<span>{days[i]}</span>{/each}</p>
    {/if}

  {:else if form === 'bars' && first.length}
    {@const s = scale(first, true)}{@const cw = w - LEFT}{@const h = 130 - 32}{@const slot = cw / first.length}{@const bw = Math.max(1, slot * 0.7)}{@const mx = maxIndex(first)}
    <p class="pick" data-testid="monitor-tocco">{sel != null && first[sel] != null ? `${days[sel] ?? ''} · ${num(first[sel]!, c.unit)}` : ' '}</p>
    <svg width={w} height="130" role="img" aria-label={c.title ?? ''} onclick={(e) => tap(e, first.length)}>
      {#each s.ticks as tk}{@const y = 14 + (1 - yOf(s, tk)) * h}<line class="grid" x1={LEFT} x2={w} y1={y} y2={y} /><text class="lab" x="0" y={y + 4}>{num(tk)}</text>{/each}
      <g transform="translate({LEFT} 0)">
        {#each first as v, i}{#if v != null}{@const y = 14 + (1 - yOf(s, v)) * h}<rect class="bar" class:on={sel === i} x={i * slot + (slot - bw) / 2} y={y} width={bw} height={14 + h - y} />{/if}{/each}
        {#if mx != null}{@const y = 14 + (1 - yOf(s, first[mx]!)) * h}<text class="lab" x={Math.min(Math.max(mx * slot + slot / 2, 40), cw - 40)} y={Math.max(10, y - 4)} text-anchor="middle">{t.monitorMaxOn(num(first[mx]!, c.unit), days[mx] ?? '')}</text>{/if}
        {#if days.length >= 2}{#each three(days.length) as i, k}<text class="lab" x={i * slot + slot / 2} y="128" text-anchor={k === 0 ? 'start' : i === days.length - 1 ? 'end' : 'middle'}>{days[i]}</text>{/each}{/if}
      </g>
    </svg>

  {:else if form === 'meter' && c.limit != null}
    {@const v = [...first].reverse().find(x => x != null)}
    {#if v != null}{@const [fill, over] = meter(v, c.limit)}
      <p class="big" class:look={over}>{c.value ?? num(v, c.unit)}<small>{t.monitorLimit(num(c.limit, c.unit))}</small></p>
      <div class="meter" class:over><i style:width="{fill * 100}%"></i><b></b></div>
    {/if}

  {:else if form === 'table'}
    {#each c.rows ?? [] as r, i (i)}<p class="trow"><span>{r.name}</span><b class:look={r.look}>{r.value}</b>{#if r.note}<small>{r.note}</small>{/if}</p>{/each}

  {:else if form === 'status' && first.length}
    {@const ok = first.filter(v => v != null && v > 0).length}
    <p class="pick strong" data-testid="monitor-tocco">{sel != null ? `${days[sel] ?? ''} · ${(first[sel] ?? 0) > 0 ? t.monitorYes : t.monitorNo}` : t.monitorDaysOk(ok, first.filter(v => v != null).length)}</p>
    <div class="cells">{#each first as v, i}<button class="cell" class:off={v != null && v <= 0} class:none={v == null} class:sel={sel === i} aria-label={days[i] ?? ''} onclick={() => (sel = sel === i ? null : i)}></button>{/each}</div>
    {#if days.length >= 2}<p class="days"><span>{days[0]}</span><span>{days[days.length - 1]}</span></p>{/if}
  {/if}
</div>
{/if}

<style>
  .chart { display: flex; flex-direction: column; gap: 8px; padding: 12px 14px; border-radius: 14px; background: var(--s1); border: 1px solid var(--line); min-width: 0; }
  .hd { display: flex; align-items: baseline; gap: 8px; }
  .hd b { flex: 1; font: 600 17px/20px var(--display); }
  .hd small { font-size: 14px; color: var(--tx2); text-align: right; }
  .big { display: flex; align-items: baseline; gap: 10px; font: 700 34px/38px var(--display); color: var(--tx); }
  .big.look { color: var(--wait); }
  .big small { font: 400 14px var(--sans, inherit); color: var(--tx2); }
  .pick { font: 12.5px/17px var(--mono); color: var(--tx); min-height: 17px; white-space: pre; overflow: hidden; text-overflow: ellipsis; }
  svg { display: block; overflow: visible; cursor: pointer; }
  .ln { fill: none; stroke: var(--tx); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  .grid { stroke: var(--line); stroke-width: 1; }
  .sel { stroke: var(--tx3); stroke-width: 1; }
  .mark { stroke: var(--tx3); stroke-width: 1; stroke-dasharray: 4 4; }
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
  .mn { display: flex; align-items: baseline; gap: 6px; margin-bottom: 4px; white-space: nowrap; }
  .mn b { font: 600 17px/20px var(--display); }
  .mn small { font-size: 14px; color: var(--tx3); }
  .mv { width: 108px; flex: none; display: flex; flex-direction: column; align-items: flex-end; white-space: nowrap; }
  .mv b { font: 700 17px/20px var(--display); }
  .mv small { font-size: 14px; line-height: 19px; color: var(--tx2); }
  .mv small.look { color: var(--wait); }
  .days { display: flex; justify-content: space-between; font: 11.5px var(--mono); color: var(--tx3); }
  .meter { position: relative; height: 14px; border-radius: 7px; background: var(--s3); margin: 4px 0; }
  .meter i { position: absolute; inset: 0 auto 0 0; border-radius: 7px; background: var(--tx); }
  .meter.over i { background: var(--wait); }
  .meter b { position: absolute; left: 80%; top: -3px; bottom: -3px; width: 2px; background: var(--tx2); }
  .trow { display: flex; align-items: center; min-height: 40px; border-top: 1px solid var(--line); gap: 12px; }
  .trow:first-of-type { border-top: 0; }
  .trow span { flex: 1; min-width: 0; font-size: 15.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .trow b { font: 700 17px var(--display); white-space: nowrap; }
  .trow b.look { color: var(--wait); }
  .trow small { min-width: 72px; text-align: right; font-size: 14px; color: var(--tx2); white-space: nowrap; }
  .cells { display: flex; gap: 3px; }
  .cell { flex: 1; height: 22px; border-radius: 3px; background: var(--tx2); }
  .cell.off { background: var(--wait); }
  .cell.none { background: transparent; }
  .cell.sel { box-shadow: inset 0 0 0 1.5px var(--tx); }
</style>
