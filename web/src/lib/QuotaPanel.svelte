<script lang="ts">
  import { locale } from './lang'
  import type { Ring } from './overview'
  import { weekProjected, weekRunsOut } from './tablet'
  import { rhythm, type Rhythm } from './rhythm5h'
  import { t } from './t'
  import QuotaRing from './QuotaRing.svelte'

  // Il pannello delle quote in fondo alla home della plancia (TabletQuotaPanel): la finestra di 5 ore con la previsione al
  // ritmo di adesso fino alla ripartenza (piena fino a ora, tratteggiata dopo, soglie 80 e 100), poi la settimana col
  // ritmo medio al rinnovo.
  let { ring, now, dataStale = false, timeZone }: { ring: Ring; now: number; dataStale?: boolean; timeZone?: string } = $props()
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit', timeZone })
  const dayTime = (at: number) => new Date(at * 1000).toLocaleString(locale, { weekday: 'long', hour: '2-digit', minute: '2-digit', timeZone }).replace(',', ' ')
  // Il ritmo delle cinque ore (1A e 1B, 25/10): solo con la quota letta e non vecchia, altrimenti la frase del perché.
  const r = $derived(ring.resetAt != null && ring.h5 != null && !ring.stale && !dataStale ? rhythm(ring.pace?.points ?? [], ring.h5, ring.resetAt, now) : null)
  const sentence = $derived(!r ? '' : r.fullAt != null ? t.rhythmFullAt(hm(r.fullAt), hm(r.resetAt)) : r.projected != null && r.pct < 100 ? t.rhythmAtReset(r.projected, hm(r.resetAt)) : r.points.length < 2 ? t.tabletForecastNone(hm(r.resetAt)) : t.tabletForecastFlat(hm(r.resetAt)))
  let cw = $state(0)
  const week = $derived(weekProjected(ring.w7, ring.weekResetAt, now))
  const runsOut = $derived(weekRunsOut(ring.w7, ring.weekResetAt, now))
  // Il grafico in pixel veri (larghezza della card): il testo resta a 10,5 px e ogni etichetta si tiene dentro.
  const CH = 150, Y100 = 34, YB = 128, PAD = 2
  const CHAR = 6.4
  const tw = (s: string) => s.length * CHAR
  function layout(v: Rhythm, w: number) {
    const xr = w - 6
    const x = (f: number) => PAD + (xr - PAD) * f
    const y = (p: number) => Y100 + (YB - Y100) * (1 - Math.min(100, Math.max(0, p)) / 100)
    const now = { x: x(v.nowX), y: y(v.pct) }
    const fullX = v.fullAt != null ? x((v.fullAt - v.start) / (v.resetAt - v.start)) : null
    const proj = fullX != null ? { x: fullX, y: Y100 } : v.projected != null && v.pct < 100 ? { x: xr, y: y(v.projected) } : null
    // «100% alle 13:20» finisce sul punto del 100%, mai oltre i bordi; «100%» a sinistra solo se non ci si sovrappone.
    const ht = fullX != null && v.fullAt != null ? t.rhythmHundredAt(hm(v.fullAt)) : null
    const hundred = ht ? { text: ht, x: Math.min(xr, Math.max(PAD + tw(ht), fullX! + 4)) } : null
    const left100 = !hundred || hundred.x - tw(hundred.text) > PAD + tw('100%') + 8
    // La fascia rossa dei minuti fermi; la scritta verticale solo se ci sta.
    const idle = v.idleMin != null ? t.rhythmIdle(v.idleMin) : null
    const band = fullX != null ? { x: fullX, w: xr - fullX, label: idle && xr - fullX >= 13 && tw(idle) <= YB - Y100 - 8 ? idle : null } : null
    // «62% ora» a sinistra del punto, o a destra se a sinistra non ci sta; sotto se sopra tocca la riga del 100%.
    const nl = t.rhythmNow(v.pct)
    const above = now.y - 8 > Y100 + 14
    const nowLabel = now.x - 7 - tw(nl) >= PAD
      ? { x: now.x - 7, y: above ? now.y - 8 : now.y + 16, anchor: 'end' }
      : { x: Math.min(now.x + 7, xr - tw(nl)), y: above ? now.y - 8 : now.y + 16, anchor: 'start' }
    // L'ora di adesso sotto il punto, se non tocca l'ora d'inizio né il bordo.
    const half = tw('00:00') / 2
    const nowTime = now.x - half > PAD + tw('00:00') + 8 && now.x + half <= xr ? now.x : null
    const line = v.points.map(([px, p]) => `${x(px)},${y(p)}`).join(' ')
    return { xr, now, proj, hundred, left100, band, nowLabel, nowTime, line }
  }
</script>

<div class="panel">
  <!-- In testa l'anello del polso col numero grande e l'ora di ripartenza (Utilizzo unito, 09/10). -->
  <div class="head">
    <QuotaRing {ring} stale={ring.stale || dataStale} />
    <div class="hcol">
      <span class="lab">{t.tabletQuotaLabel.toUpperCase()} · {ring.account.toUpperCase()}</span>
      <div class="num"><b class:dim={ring.stale}>{ring.h5 != null && !ring.stale ? `${ring.h5}%` : '–'}</b>{#if ring.resetAt}<small>{t.quotaResetsAt(hm(ring.resetAt))}</small>{/if}</div>
      {#if ring.stale}<span class="old">{t.quotaOldWord}</span>{/if}
    </div>
  </div>
  {#if r && ring.resetAt}
    <!-- 1A: tempo e quota sulla stessa scala; il pezzo di quota oltre il tempo è ambra. -->
    <div class="bars">
      <span class="bl">{t.rhythmTime}</span><span class="bar"><i style="width:{r.elapsed}%;background:var(--text2)"></i><span class="tick" style="left:{r.elapsed}%"></span></span><span class="bv">{r.elapsed}%</span>
      <span class="bl">{t.rhythmQuota}</span><span class="bar"><i style="width:{Math.min(r.pct, r.elapsed)}%"></i>{#if r.ahead > 0}<i class="over" style="left:{r.elapsed}%;width:{Math.min(100, r.pct) - r.elapsed}%"></i>{/if}<span class="tick" style="left:{r.elapsed}%"></span></span><span class="bv">{r.pct}%</span>
    </div>
    <p class="ahead" class:warn={r.ahead > 0}>{r.ahead > 0 ? t.rhythmAhead(r.ahead) : r.ahead < 0 ? t.rhythmBehind(-r.ahead) : t.rhythmEven}</p>
    {#if r.points.length >= 2}
      <!-- 1B: la linea della finestra, la proiezione tratteggiata, la fascia dei minuti fermi; etichette sul grafico. -->
      <div class="chartbox" bind:clientWidth={cw}>
        {#if cw > 0}
          {@const g = layout(r, cw)}
          <svg width={cw} height={CH} viewBox="0 0 {cw} {CH}" class="chart" role="img" aria-label={sentence}>
            {#if g.band}
              <rect x={g.band.x} y={Y100} width={g.band.w} height={YB - Y100} fill="var(--b-alert)" fill-opacity=".18" />
              {#if g.band.label}<text transform="translate({g.band.x + g.band.w / 2 + 3.5},{(Y100 + YB) / 2}) rotate(-90)" class="ax alert" text-anchor="middle">{g.band.label}</text>{/if}
            {/if}
            <line x1={g.xr} y1={Y100 - 14} x2={g.xr} y2={YB} stroke="var(--text2)" stroke-opacity=".6" />
            <text x={g.xr} y="10" class="ax" text-anchor="end">{t.rhythmReset(hm(ring.resetAt))}</text>
            <line x1={PAD} y1={Y100} x2={g.xr} y2={Y100} stroke="var(--text2)" stroke-dasharray="3 4" />
            {#if g.left100}<text x={PAD} y={Y100 - 5} class="ax">100%</text>{/if}
            {#if g.hundred}<text x={g.hundred.x} y={Y100 - 5} class="ax val" text-anchor="end">{g.hundred.text}</text>{/if}
            <polyline points={g.line} fill="none" stroke="var(--b-ring)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            {#if g.proj}
              <line x1={g.now.x} y1={g.now.y} x2={g.proj.x} y2={g.proj.y} stroke="var(--b-ring)" stroke-width="2" stroke-dasharray="6 5" />
              <circle cx={g.proj.x} cy={g.proj.y} r="4" fill={r.fullAt ? 'var(--b-alert)' : 'var(--b-ring)'} />
            {/if}
            <circle cx={g.now.x} cy={g.now.y} r="4" fill="var(--b-ring)" />
            <text x={g.nowLabel.x} y={g.nowLabel.y} class="ax val" text-anchor={g.nowLabel.anchor}>{t.rhythmNow(r.pct)}</text>
            <text x={PAD} y={CH - 2} class="ax">{hm(r.start)}</text>
            {#if g.nowTime}<text x={g.nowTime} y={CH - 2} class="ax" text-anchor="middle">{hm(now)}</text>{/if}
          </svg>
        {/if}
      </div>
    {/if}
    <p class="txt">{sentence}</p>
  {:else}
    {#if ring.h5 != null}<span class="mini"><i style="width:{Math.min(100, ring.h5)}%"></i></span>{/if}
    <p class="txt dim">{ring.resetAt == null ? t.ovNoQuota : dataStale || ring.stale ? t.tabletForecastStale(hm(ring.resetAt)) : t.tabletForecastNone(hm(ring.resetAt))}</p>
  {/if}
  <hr />
  <div class="title"><span class="lab">{t.tabletWeekLabel.toUpperCase()} · {ring.account.toUpperCase()}</span>{#if ring.w7 != null}<b>{ring.w7}%</b>{/if}</div>
  {#if ring.w7 != null}
    <span class="week">
      {#if week != null}<i class="proj" style="width:{week}%"></i>{/if}
      <i class="used" style="width:{Math.min(100, ring.w7)}%"></i><i class="mark"></i>
    </span>
    {#if ring.weekResetAt}<p class="txt">{ring.w7 >= 100 ? t.tabletWeekFull(dayTime(ring.weekResetAt)) : runsOut != null ? t.tabletWeekRunsOut(dayTime(runsOut), dayTime(ring.weekResetAt)) : week != null ? t.tabletWeekForecast(week, dayTime(ring.weekResetAt)) : t.tabletWeekReset(dayTime(ring.weekResetAt))}</p>{/if}
  {/if}
</div>

<style>
  .head { display: flex; gap: 14px; align-items: center; }
  .hcol { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .num { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
  .num b { font-size: 28px; font-weight: 500; line-height: 1.1; font-variant-numeric: tabular-nums; }
  .num b.dim { color: var(--text2); }
  .num small { font-size: 12.5px; color: var(--text2); }
  .old { font-size: 12.5px; color: var(--b-warn); }
  .panel { border-radius: 16px; background: var(--low); border: 1px solid rgb(255 255 255 / .12); padding: 14px; display: flex; flex-direction: column; gap: 8px; }
  .title { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
  .lab { flex: 1; font: 11px var(--mono); letter-spacing: .12em; color: var(--text2); }
  .title b { font: 500 14px var(--mono); }
  .chartbox { width: 100%; }
  .chart { display: block; }
  .bars { display: grid; grid-template-columns: auto 1fr auto; gap: 6px 10px; align-items: center; }
  .bl { font-size: 13px; color: var(--text2); }
  .bv { font: 12.5px var(--mono); text-align: right; min-width: 3.2em; }
  .bar { position: relative; height: 10px; border-radius: 5px; background: var(--b-track); }
  .bar i { position: absolute; left: 0; top: 0; bottom: 0; border-radius: 5px; background: var(--b-ring); }
  .bar i.over { background: var(--b-warn); border-radius: 0 5px 5px 0; }
  .tick { position: absolute; top: -3px; bottom: -3px; width: 2px; margin-left: -1px; background: var(--text); }
  .ahead { font: 12.5px var(--mono); color: var(--text2); overflow-wrap: anywhere; }
  .ahead.warn { color: var(--b-warn); }
  .ax.alert { fill: var(--b-alert); }
  .ax { font: 10.5px var(--mono); fill: var(--text2); }
  .ax.val { fill: var(--text); }
  .txt { font-size: 12.5px; color: var(--text); }
  .txt.dim { color: var(--text2); }
  .mini { height: 4px; border-radius: 2px; background: var(--b-track); overflow: hidden; display: block; }
  .mini i { display: block; height: 100%; background: var(--b-ring); }
  hr { border: 0; height: 1px; background: var(--line); margin: 2px 0; }
  .week { position: relative; height: 10px; border-radius: 5px; background: var(--b-track); display: block; }
  .week i { position: absolute; left: 0; top: 0; bottom: 0; border-radius: 5px; }
  .proj { background: color-mix(in srgb, var(--b-week) 35%, transparent); }
  .used { background: var(--b-week); }
  .mark { left: 80% !important; width: 1.5px; top: -2px !important; bottom: -2px !important; background: var(--b-warn); border-radius: 0 !important; }
</style>
