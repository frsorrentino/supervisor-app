<script lang="ts">
  import type { Span } from './kpi'
  import { workSeconds } from './kpi'
  import { t } from './t'
  // Proposta 11 (25/10): la giornata a fasce dalla mezzanotte a ora; le etichette delle ore sotto, il totale in alto.
  let { spans, from, now }: { spans: Span[]; from: number; now: number } = $props()
  const len = $derived(Math.max(1, now - from))
  const tone = { work: 'var(--busy)', wait: 'var(--wait)', still: 'var(--b-track)' }
  const hours = $derived.by(() => {
    const out: { x: number; h: number }[] = []
    for (let h = 0; from + h * 3600 <= now; h += 4) out.push({ x: (h * 3600) / len, h })
    return out.filter(o => o.x <= 0.92)
  })
  const work = $derived(workSeconds(spans))
</script>

<div class="k11" data-kpi="kpi-11">
  <div class="head"><span>{t.kpi11Title}</span><span class="tot">{t.kpi11Work(Math.floor(work / 3600), Math.round((work % 3600) / 60))}</span></div>
  <div class="bar">{#each spans as s (s.from)}<i style:width="{((s.to - s.from) / len) * 100}%" style:background={tone[s.band]} title={t.kpi11Band[s.band]}></i>{/each}</div>
  <div class="hrs">{#each hours as o (o.h)}<span style:left="{o.x * 100}%">{String(o.h).padStart(2, '0')}</span>{/each}</div>
</div>

<style>
  .k11 { display: flex; flex-direction: column; gap: 4px; }
  .head { display: flex; justify-content: space-between; gap: 8px; font: 500 11px/1.3 var(--mono); color: var(--text2); letter-spacing: .06em; }
  .head span:first-child { text-transform: uppercase; overflow: hidden; text-overflow: clip; white-space: nowrap; }
  .tot { flex: none; letter-spacing: 0; }
  .bar { display: flex; gap: 0; height: 10px; border-radius: 5px; overflow: hidden; }
  .bar i { display: block; height: 100%; min-width: 1px; }
  .hrs { position: relative; height: 12px; font: 10px/1 var(--mono); color: var(--text2); }
  .hrs span { position: absolute; top: 0; }
</style>
