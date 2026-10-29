<script lang="ts">
  import type { Split } from './kpi'
  import { t } from './t'
  // Proposta 4 (25/10): la quota 5h di un account divisa per sessione, senza il tempo. I nomi sono le etichette della barra,
  // nello stesso colore del loro tratto.
  let { s }: { s: Split } = $props()
  const TONES = ['var(--b-ring)', 'var(--b-week)', 'var(--good)', 'var(--text2)']
</script>

<div class="k4" data-kpi="kpi-4">
  <div class="head"><span>{t.kpi4Title(s.account)}</span><b>{s.h5}%</b></div>
  <div class="bar">
    {#each s.slices as x, i (x.session)}<i style:width="{x.pct}%" style:background={TONES[i]}></i>{/each}
    {#if s.others}<i style:width="{s.others}%" style:background={TONES[3]}></i>{/if}
  </div>
  <div class="names">
    {#each s.slices as x, i (x.session)}<span style:color={TONES[i]}>{x.session} {x.pct}%</span>{/each}
    {#if s.others}<span style:color={TONES[3]}>{t.kpi4Others} {s.others}%</span>{/if}
  </div>
</div>

<style>
  .k4 { background: var(--low); border-radius: 16px; padding: 12px 14px; display: flex; flex-direction: column; gap: 8px; }
  .head { display: flex; justify-content: space-between; gap: 8px; font: 500 11px/1.3 var(--mono); color: var(--text2); letter-spacing: .06em; }
  .head span { text-transform: uppercase; min-width: 0; overflow-wrap: anywhere; }
  .head b { font-weight: 500; color: var(--text); letter-spacing: 0; }
  .bar { display: flex; height: 8px; border-radius: 4px; background: var(--b-track); overflow: hidden; gap: 1px; }
  .bar i { display: block; height: 100%; }
  .names { display: flex; flex-wrap: wrap; gap: 4px 12px; font-size: 13px; }
  .names span { overflow-wrap: anywhere; }
</style>
