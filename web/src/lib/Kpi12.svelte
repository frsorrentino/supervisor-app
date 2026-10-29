<script lang="ts">
  import type { KpiHealth } from './contract'
  import { locale } from './lang'
  import { t } from './t'
  // Proposta 12 (25/10, contratto 1.96), nella sezione «Sistema»: quanto ci mette una push ad arrivare, le sessioni di
  // questo computer sul massimo, e per ogni altra macchina la RAM usata o «spento».
  let { h }: { h: KpiHealth } = $props()
  const num = (x: number) => x.toLocaleString(locale, { minimumFractionDigits: 1, maximumFractionDigits: 1 })
  const lat = $derived(h.latency && h.latency.n > 0 && h.latency.p50_s != null ? h.latency : null)
  type Fig = { v: string; k: string; tone: string | null }
  const figs = $derived.by(() => {
    const out: Fig[] = []
    out.push(lat ? { v: `${num(lat.p50_s!)} s`, k: lat.max_s != null ? t.kpi12LatencyMax(num(lat.max_s)) : t.kpi12Latency, tone: lat.p50_s! > 5 ? 'var(--wait)' : null } : { v: '–', k: t.kpi12LatencyNone, tone: null })
    if (h.sessions_here != null) out.push({ v: h.sessions_max != null ? `${h.sessions_here} / ${h.sessions_max}` : String(h.sessions_here), k: t.kpi12SessionsHere, tone: h.sessions_max != null && h.sessions_here >= h.sessions_max ? 'var(--wait)' : null })
    for (const x of h.hosts) out.push(!x.up ? { v: t.kpi12Down, k: x.name, tone: 'var(--b-alert)' } : { v: x.ram_pct != null ? `${x.ram_pct}%` : '–', k: x.ram_pct != null ? t.kpi12Ram(x.name) : t.kpi12Up(x.name), tone: (x.ram_pct ?? 0) >= 85 ? 'var(--wait)' : null })
    return out
  })
</script>

<div class="k12" data-kpi="kpi-12">
  {#each figs as f, i (i)}<div class="fig"><b style:color={f.tone}>{f.v}</b><span>{f.k}</span></div>{/each}
</div>

<style>
  .k12 { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
  .fig { background: var(--low); border-radius: 16px; padding: 10px 12px; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
  .fig b { font: 500 24px/1.1 var(--sans, inherit); }
  .fig span { font-size: 13px; color: var(--text2); overflow-wrap: anywhere; }
</style>
