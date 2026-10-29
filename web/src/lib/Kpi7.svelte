<script lang="ts">
  import type { Head } from './kpi'
  import { t } from './t'
  // Proposta 7 (25/10): tre cifre in testa alla home; un tocco su «ti aspettano» porta alla prima che aspetta.
  let { h, onWaiting = () => {} }: { h: Head; onWaiting?: () => void } = $props()
</script>

<div class="k7" data-kpi="kpi-7">
  <button class="fig" disabled={!h.waiting} onclick={onWaiting}><b class:wait={h.waiting > 0}>{h.waiting}</b><span>{h.waiting ? t.kpi7Waiting(Math.max(1, Math.round((h.oldestWaitS ?? 0) / 60))) : t.kpi7NoneWaiting}</span></button>
  <div class="fig"><b class:busy={h.busy > 0}>{h.busy}</b><span>{t.kpi7Busy}</span></div>
  <div class="fig"><b>{h.outcomes ?? '–'}</b><span>{h.outcomes == null ? t.kpi7OutcomesUnknown : h.red ? t.kpi7OutcomesRed(h.red) : t.kpi7Outcomes}</span></div>
</div>

<style>
  .k7 { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin: 4px 0 2px; }
  .fig { background: var(--low); border-radius: 16px; padding: 10px; display: flex; flex-direction: column; gap: 2px; min-width: 0; text-align: left; color: var(--text); }
  .fig b { font: 500 22px/1.1 var(--sans, inherit); }
  .fig b.wait { color: var(--wait); } .fig b.busy { color: var(--busy); }
  .fig span { font-size: 12.5px; line-height: 1.25; color: var(--text2); overflow-wrap: anywhere; }
</style>
