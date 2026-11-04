<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { Head } from './kpi'
  import type { Box } from './HomeBox.svelte'
  import type { State } from './contract'
  import { t } from './t'
  // Proposta 7 (25/10): tre cifre in testa alla home. Ognuna apre la sua pagina, anche a zero (app-box-cliccabili-29-10).
  // Lo stile «Studio e Diretta» (terza versione, 30/10): il tabellone, con la quota delle 5 ore sotto le cifre al posto della
  // riga nella testata. Si colora solo chi ti aspetta, l'unico avviso della home; i rossi restano rossi. La barra della quota
  // è neutra e diventa ambra da 75 % e rossa da 90 %; la tacca bianca è quanto è passato della finestra delle 5 ore.
  // quota-senza-cinque-ore: dove il dato manca, per quell'account, la settimana col giorno e il ritmo.
  let { h, st = null, now = 0, onBox = () => {}, onQuota = () => {}, slot }: { h: Head; st?: State | null; now?: number; onBox?: (b: Box) => void; onQuota?: () => void; slot?: Snippet } = $props()
</script>

<div class="board lit" data-kpi="kpi-7">
  <button class="cell" data-testid="home-box-aspetta" onclick={() => onBox('aspetta')}><b class="n" class:wait={h.waiting > 0}>{h.waiting}</b><span class="l">{h.waiting ? t.kpi7Waiting(h.waiting, Math.max(1, Math.round((h.oldestWaitS ?? 0) / 60))) : t.kpi7NoneWaiting}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  <button class="cell" data-testid="home-box-lavoro" onclick={() => onBox('lavoro')}><b class="n">{h.busy}</b><span class="l">{t.kpi7Busy}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  <button class="cell" data-testid="home-box-esiti" onclick={() => onBox('esiti')}><b class="n">{h.outcomes ?? '–'}</b><span class="l">{#if h.outcomes == null}{t.kpi7OutcomesUnknown}{:else if h.red}{t.kpi7Outcomes}, <em>{t.kpi7Red(h.red)}</em>{:else}{t.kpi7Outcomes}{/if}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  <!-- La barra fissa delle 5 ore non c'è più (app-utilizzo-una-barra-03-11): le finestre d'uso stanno nella riga Utilizzo, e una vicina al
       limite entra da sola nel tabellone. -->
  {#if slot}<div class="slot">{@render slot()}</div>{/if}
</div>

<style>
  .slot { grid-column: 1 / -1; min-width: 0; }
  .slot:empty { display: none; }
  .board { flex: none; display: grid; grid-template-columns: 1.15fr 1fr 1.15fr; margin: 4px 0 2px; background: var(--s1); border: 1px solid var(--line); border-radius: 14px; overflow: hidden; }
  /* In alto, non al centro come fa un button: i tre numeri sulla stessa linea; l'etichetta ha sempre due righe (Franz 03/11 09:38,
     app-tabellone-numeri-03-11). */
  .cell { position: relative; display: flex; flex-direction: column; justify-content: flex-start; padding: 12px 12px 13px; border-left: 1px solid var(--line); text-align: left; color: var(--tx); cursor: pointer; min-width: 0; }
  .cell:first-child { border-left: 0; }
  .cell:hover { background: var(--s2); }
  .cell:focus-visible { outline: 2px solid var(--tx); outline-offset: -2px; }
  .n { display: block; font: 700 42px/1 var(--display); letter-spacing: -0.01em; }
  .n.wait { color: var(--wait); }
  .l { display: block; min-height: 2.5em; font-size: 14px; line-height: 1.25; color: var(--tx2); margin-top: 6px; overflow-wrap: anywhere; }
  .l em { font-style: normal; font-weight: 500; color: var(--red); }
  .chev { position: absolute; top: 12px; right: 8px; color: var(--tx3); }
</style>
