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
  // Variante B del mockup del 13/11 (app-tabellone-utilizzo-b-13-11, Franz 12:40): il terzo blocco è l'utilizzo, la percentuale
  // grande come le altre cifre, sotto la finestra che dice, e sul bordo in basso la barra con la tacca del tempo passato; il tocco
  // apre e chiude i dettagli sotto. Gli esiti di oggi stanno nella riga agganciata (Home.svelte). Senza il dato dell'utilizzo il
  // terzo blocco resta «esiti oggi».
  let { h, st = null, now = 0, onBox = () => {}, onQuota = () => {}, slot, usage = null, onUsage = () => {} }: { h: Head; st?: State | null; now?: number; onBox?: (b: Box) => void; onQuota?: () => void; slot?: Snippet
    usage?: { pct: number; tick: number | null; tone: string; label: string; open: boolean } | null; onUsage?: () => void } = $props()
</script>

<div class="board lit" data-kpi="kpi-7">
  <button class="cell" data-testid="home-box-aspetta" onclick={() => onBox('aspetta')}><b class="n" class:wait={h.waiting > 0}>{h.waiting}</b><span class="l">{h.waiting ? t.kpi7Waiting(h.waiting, Math.max(1, Math.round((h.oldestWaitS ?? 0) / 60))) : t.kpi7NoneWaiting}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  <button class="cell" data-testid="home-box-lavoro" onclick={() => onBox('lavoro')}><b class="n">{h.busy}</b><span class="l">{t.kpi7Busy}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  {#if usage}
    <button class="cell use" data-testid="home-box-utilizzo" aria-expanded={usage.open} onclick={onUsage}><b class="n" style:color={usage.tone === 'var(--tx2)' ? null : usage.tone}>{usage.pct}<sup>%</sup></b><span class="l">{usage.label}</span><span class="ms chev" aria-hidden="true">{usage.open ? 'expand_less' : 'expand_more'}</span>
      <span class="edge" style:color={usage.tone}><i style="width:{Math.min(100, usage.pct)}%"></i>{#if usage.tick != null}<u style="left:{usage.tick}%"></u>{/if}</span></button>
  {:else}
  <button class="cell" data-testid="home-box-esiti" onclick={() => onBox('esiti')}><b class="n">{h.outcomes ?? '–'}</b><span class="l">{#if h.outcomes == null}{t.kpi7OutcomesUnknown}{:else if h.red}{t.kpi7Outcomes}, <em>{t.kpi7Red(h.red)}</em>{:else}{t.kpi7Outcomes}{/if}</span><span class="ms chev" aria-hidden="true">chevron_right</span></button>
  {/if}
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
  /* Il blocco dell'utilizzo: il segno % più piccolo, la barra sul bordo in basso con la tacca del tempo passato. */
  .n sup { font-size: 22px; font-weight: 600; vertical-align: 14px; margin-left: 2px; color: var(--tx2); }
  .cell.use[aria-expanded='true'] { background: var(--s2); }
  .edge { position: absolute; left: 12px; right: 12px; bottom: 10px; height: 4px; border-radius: 2px; background: color-mix(in srgb, var(--tx) 14%, transparent); }
  .edge i { position: absolute; left: 0; top: 0; bottom: 0; border-radius: 2px; background: currentColor; }
  .edge u { position: absolute; top: -3px; bottom: -3px; width: 2px; border-radius: 1px; background: var(--tx); }
</style>
