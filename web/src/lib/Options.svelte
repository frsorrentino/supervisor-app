<script lang="ts">
  import type { Question } from './contract'
  import { inline as isInline, needsLongPress, optionLabel, optionNote, optionText, primaryIndex } from './questionRules'
  import { t } from './t'
  // Le opzioni di una domanda, uguali dovunque (QuestionOptions dell'app): la prima piena se è l'azione della schermata,
  // brevi su una riga in parti uguali, lunghe una sotto l'altra; col rischio alto risponde solo la pressione lunga, e il
  // clic breve lo ricorda.
  let { q, onAnswer, firstFilled = true, tonal = 'var(--surface)' }: { q: Question; onAnswer: (n: number) => void; firstFilled?: boolean; tonal?: string } = $props()
  const long = $derived(needsLongPress(q.tier))
  const row = $derived(isInline(q.options))
  // Contratto 1.53: piena l'opzione consigliata, e sotto ogni tasto la riga che la spiega.
  const primary = $derived(primaryIndex(q.options))
  let hint = $state(false)
  let held: ReturnType<typeof setTimeout> | null = null
  let fired = false
  const HOLD_MS = 600
  function down(n: number, e: PointerEvent) {
    if (!long || e.button !== 0) return
    fired = false
    held = setTimeout(() => { fired = true; onAnswer(n) }, HOLD_MS)
  }
  function up() { if (held) clearTimeout(held); held = null }
  function click(n: number, e: MouseEvent) {
    e.stopPropagation()
    if (!long) onAnswer(n); else if (!fired) hint = true
  }
</script>

{#if long}<p class="warn" class:hint>{hint ? t.questionHold : t.questionHighRisk}</p>{/if}
<div class="opts" class:row>
  {#each q.options as o, i}
    {@const note = optionNote(o, t.optionRecommended)}
    <button class="opt" class:filled={i === primary && firstFilled} class:rec={i === primary} class:long style="--tonal:{tonal}"
      onpointerdown={(e) => down(o.n, e)} onpointerup={up} onpointerleave={up} onpointercancel={up} onclick={(e) => click(o.n, e)}
      oncontextmenu={(e) => long && e.preventDefault()}><span class="lbl">{optionLabel(o)}</span><span class="v3l"><span class="k">{o.n}</span>{optionText(o.label)}</span>{#if note}<small>{note}</small>{/if}</button>
  {/each}
</div>

<style>
  .warn { font-size: 14px; font-weight: 500; color: var(--b-warn); }
  .warn.hint { color: var(--wait); }
  .opts { display: flex; flex-direction: column; gap: 6px; }
  .opts.row { flex-direction: row; gap: 2px; }
  .opt { min-height: 44px; padding: 10px 18px; border-radius: 22px; background: var(--tonal); color: var(--text); font-size: 16px; font-weight: 500; text-align: left; }
  .row .opt { flex: 1; text-align: center; white-space: nowrap; overflow: hidden; border-radius: 6px; }
  .row .opt:first-child { border-radius: 22px 6px 6px 22px; }
  .row .opt:last-child { border-radius: 6px 22px 22px 6px; }
  .row .opt:only-child { border-radius: 22px; }
  .opt.filled { background: var(--primary); color: var(--on-primary); }
  .opt small { display: block; font-size: 13px; font-weight: 400; color: var(--text2); }
  .opt.filled small { color: inherit; opacity: .8; }
  .opt:hover { filter: brightness(1.12); }
  /* La pressione lunga si vede: il tasto si riempie mentre lo tieni. */
  .opt.long:active { background-image: linear-gradient(90deg, rgb(255 255 255 / .18) 0 0); background-size: 0 100%; background-repeat: no-repeat; animation: hold 600ms linear forwards; }
  @keyframes hold { to { background-size: 100% 100%; } }
  /* Nella home in stile «Studio e Diretta» (30/10): una per riga a tutta larghezza, la consigliata col bordo chiaro e il
     numero nel cerchio pieno; mai una lastra piena (correzione 1 della quarta critica). Fuori dalla home resta com'era. */
  .v3l { display: none; }
  :global(.v3) .lbl { display: none; }
  :global(.v3) .v3l { display: flex; align-items: center; gap: 8px; }
  :global(.v3) .opts, :global(.v3) .opts.row { flex-direction: column; gap: 8px; }
  :global(.v3) .opt, :global(.v3) .row .opt { min-height: 52px; padding: 9px 14px 10px; border-radius: 12px; background: var(--s3); color: var(--tx); text-align: left; white-space: normal; font: 600 16px/1.25 var(--display); }
  :global(.v3) .opt.filled { background: var(--s3); color: var(--tx); }
  :global(.v3) .opt.rec { box-shadow: inset 0 0 0 1.5px color-mix(in srgb, var(--tx) 70%, transparent); }
  :global(.v3) .k { display: inline-grid; place-items: center; width: 20px; height: 20px; flex: none; border-radius: 50%; box-shadow: inset 0 0 0 1.2px var(--tx3); font: 500 11.5px/1 var(--mono); color: var(--tx2); }
  :global(.v3) .rec .k { background: var(--tx); box-shadow: none; color: var(--s3); }
  :global(.v3) .opt small, :global(.v3) .opt.filled small { margin: 2px 0 0 28px; font: 400 13px/1.3 var(--sans); color: var(--tx2); opacity: 1; }
  :global(.v3) .opt:hover { filter: none; background: color-mix(in srgb, var(--tx) 8%, var(--s3)); }
</style>
