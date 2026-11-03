<script lang="ts">
  import type { KpiHealth } from './contract'
  import { locale } from './lang'
  import { t } from './t'
  import { restingDisk, shownHosts } from './kpiHosts'
  // Proposta 12 (25/10, contratto 1.96), nella sezione «Sistema»: quanto ci mette una push ad arrivare, le sessioni di
  // questo computer sul massimo, e per ogni altra macchina la RAM usata o «spento».
  let { h }: { h: KpiHealth } = $props()
  const gb = (b: number) => (b / 2 ** 30).toLocaleString(locale, { maximumFractionDigits: b >= 100 * 2 ** 30 ? 0 : 1 })
  const num = (x: number) => x.toLocaleString(locale, { minimumFractionDigits: 1, maximumFractionDigits: 1 })
  const lat = $derived(h.latency && h.latency.n > 0 && h.latency.p50_s != null ? h.latency : null)
  type Fig = { v: string; k: string; tone: string | null }
  const figs = $derived.by(() => {
    const out: Fig[] = []
    out.push(lat ? { v: `${num(lat.p50_s!)} s`, k: lat.p95_s != null ? t.kpi12LatencyP95(num(lat.p95_s), lat.max_s != null ? num(lat.max_s) : null) : lat.max_s != null ? t.kpi12LatencyMax(num(lat.max_s)) : t.kpi12Latency, tone: lat.p50_s! > 5 ? 'var(--wait)' : null } : { v: '–', k: t.kpi12LatencyNone, tone: null })
    if (h.sessions_here != null) out.push({ v: h.sessions_max != null ? `${h.sessions_here} / ${h.sessions_max}` : String(h.sessions_here), k: t.kpi12SessionsHere, tone: h.sessions_max != null && h.sessions_here >= h.sessions_max ? 'var(--wait)' : null })
    // Contratto 1.110 (app-kpi-win-disco-03-11): una macchina compare solo mentre lavora; a riposo, il disco del PC al suo posto.
    for (const x of shownHosts(h)) out.push(!x.up ? { v: t.kpi12Down, k: x.name, tone: 'var(--b-alert)' } : { v: x.ram_pct != null ? `${x.ram_pct}%` : '–', k: x.ram_pct != null ? t.kpi12Ram(x.name) : t.kpi12Up(x.name), tone: (x.ram_pct ?? 0) >= 85 ? 'var(--wait)' : null })
    const d = restingDisk(h)
    if (d) out.push({ v: `${gb(d.free)} GB`, k: t.kpi12DiskHere(gb(d.total)), tone: d.free * 10 < d.total ? 'var(--wait)' : null })
    // Il quarto riquadro (Franz 03/11 17:40, app-sistema-barra-kpi-03-11): la risposta dei comandi di oggi, p95 (contratto 1.109);
    // con un relay precedente il carico di ogni macchina accesa accanto alla RAM, così a destra non resta il buco.
    const c = h.commands && h.commands.n > 0 && h.commands.p95_s != null ? h.commands : null
    if (c) out.push({ v: `${num(c.p95_s!)} s`, k: t.kpi12Commands(c.p50_s != null ? num(c.p50_s) : '–'), tone: c.p95_s! > 10 ? 'var(--wait)' : null })
    else for (const x of h.hosts) if (x.up && x.cpu != null) out.push({ v: `${Math.round(x.cpu * 100)}%`, k: t.kpi12Cpu(x.name), tone: x.cpu >= 0.85 ? 'var(--wait)' : null })  // `cpu` fra 0 e 1 (contratto 1.75)
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
