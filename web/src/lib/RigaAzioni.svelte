<script lang="ts">
  import { t } from './t'
  // app-posizioni-sistema-27-11 gruppo 3 (in basso si scrive): le Azioni della sessione, i Prossimi, in una riga di chip sopra il campo
  // che scorre di lato; c'è solo a campo vuoto (sessionBottom.ts). Il tocco su un'azione la porta nel campo, come la riga dell'elenco di
  // prima; quelle che sbloccano un lavoro fermo hanno il lucchetto ambra al posto del fulmine. Dal 70 % di contesto il primo chip, ambra,
  // è «Contesto 72% · Handoff» e fa l'handoff di prima.
  let { actions, ctx = null, onPick, onHandoff }: {
    actions: { text: string; blocking: boolean }[]
    /** La percentuale del chip del contesto (`ctxChip`); null = niente chip. */
    ctx?: number | null
    onPick: (text: string) => void; onHandoff: () => void
  } = $props()
</script>

<div class="az" role="toolbar" aria-label={t.next} data-testid="azioni">
  {#if ctx != null}
    <button type="button" class="chip amb" data-testid="azioni-contesto" onclick={onHandoff}>
      <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M12 3 2.5 8 12 13l9.5-5L12 3z" /><path d="M2.5 16 12 21l9.5-5M2.5 12 12 17l9.5-5" /></svg>
      <span class="tx">{t.ctxChip(ctx)}</span>
    </button>
  {/if}
  {#each actions as a (a.text)}
    <button type="button" class="chip" class:lock={a.blocking} title={a.text} onclick={() => onPick(a.text)}>
      {#if a.blocking}<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><rect width="18" height="11" x="3" y="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 9.9-1" /></svg>
      {:else}<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M13 2.5 4.5 13.5H11l-1 8 8.5-11H12z" /></svg>{/if}
      <span class="tx">{a.text}</span>
    </button>
  {/each}
</div>

<style>
  /* Le misure del mockup (m/a-regia.html, `.az`): chip da 32, 6 di spazio, la riga che scorre di lato senza barra. */
  .az { flex: none; display: flex; align-items: center; gap: 6px; overflow-x: auto; padding: 4px 12px 0; scrollbar-width: none; overscroll-behavior-x: contain; }
  .az::-webkit-scrollbar { display: none; }
  .chip { flex: none; display: inline-flex; align-items: center; gap: 6px; max-width: 85%; height: 32px; padding: 0 12px 0 10px; border-radius: 16px;
    background: var(--s2); box-shadow: inset 0 0 0 1px var(--line); color: var(--tx); font: 500 13.5px var(--sans); white-space: nowrap; touch-action: manipulation;
    transition: transform 140ms cubic-bezier(.23, 1, .32, 1), background-color 140ms; }
  .chip:hover { background: var(--s3); }
  .chip svg { flex: none; fill: none; stroke: var(--tx2); stroke-width: 1.9; stroke-linecap: round; stroke-linejoin: round; }
  /* Una riga sola, tagliata senza «…»: il testo intero va nel campo col tocco. */
  .tx { min-width: 0; overflow: hidden; text-overflow: clip; }
  .chip.lock svg { stroke: var(--wait); }
  .chip.amb { color: var(--wait); background: color-mix(in srgb, var(--wait) 12%, var(--s2)); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--wait) 50%, transparent); }
  .chip.amb:hover { background: color-mix(in srgb, var(--wait) 18%, var(--s2)); }
  .chip.amb svg { stroke: var(--wait); }
  .chip:active { transform: scale(.97); }
  @media (prefers-reduced-motion: reduce) { .chip { transition: none; } .chip:active { transform: none; } }
</style>
