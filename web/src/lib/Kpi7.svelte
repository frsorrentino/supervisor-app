<script lang="ts">
  import type { Head } from './kpi'
  import type { Box } from './HomeBox.svelte'
  import { t } from './t'
  // Proposta 7 (25/10): tre cifre in testa alla home. Ognuna apre la sua pagina, anche a zero (app-box-cliccabili-29-10).
  // I rossi in rosso, e il primo box ambrato quando qualcuno ti aspetta: è l'unico avviso della home (app-home-riordino-29-10).
  let { h, onBox = () => {} }: { h: Head; onBox?: (b: Box) => void } = $props()
</script>

{#snippet chevron()}<svg width="14" height="14" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" /></svg>{/snippet}

<div class="k7" data-kpi="kpi-7">
  <button class="fig" class:wait={h.waiting > 0} data-testid="home-box-aspetta" onclick={() => onBox('aspetta')}><b class:wait={h.waiting > 0}>{h.waiting}</b><span>{h.waiting ? t.kpi7Waiting(Math.max(1, Math.round((h.oldestWaitS ?? 0) / 60))) : t.kpi7NoneWaiting}</span>{@render chevron()}</button>
  <button class="fig" data-testid="home-box-lavoro" onclick={() => onBox('lavoro')}><b class:busy={h.busy > 0}>{h.busy}</b><span>{t.kpi7Busy}</span>{@render chevron()}</button>
  <button class="fig" data-testid="home-box-esiti" onclick={() => onBox('esiti')}><b>{h.outcomes ?? '–'}</b><span>{#if h.outcomes == null}{t.kpi7OutcomesUnknown}{:else if h.red}{t.kpi7Outcomes}, <b class="red">{t.kpi7Red(h.red)}</b>{:else}{t.kpi7Outcomes}{/if}</span>{@render chevron()}</button>
</div>

<style>
  .k7 { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin: 4px 0 2px; }
  .fig { position: relative; background: var(--low); border-radius: 16px; padding: 10px; display: flex; flex-direction: column; gap: 2px; min-width: 0; text-align: left; color: var(--text); cursor: pointer; }
  .fig:hover { background: var(--surface); }
  .fig b { font: 500 22px/1.1 var(--sans, inherit); }
  .fig b.wait { color: var(--wait); } .fig b.busy { color: var(--busy); }
  .fig.wait { background: color-mix(in srgb, var(--wait) 16%, var(--low)); }
  .fig.wait span { color: var(--text); }
  .fig span b.red { font: inherit; color: var(--gone); font-weight: 500; }
  .fig span { font-size: 12.5px; line-height: 1.25; color: var(--text2); overflow-wrap: anywhere; }
  .fig svg { position: absolute; right: 8px; top: 12px; color: var(--text2); }
</style>
