<script lang="ts">
  import { UNDO_MS } from './animazioni'
  import { cancel, undoState } from './undo.svelte'
  import { t } from './t'
  // Voce 4 di app-animazioni-seconde-26-11 (Annulla col tempo): l'avviso in basso con «Annulla» e un anello che si svuota in 5 s; finito
  // l'anello il comando è partito. Entra salendo con la molla veloce; con le animazioni ridotte l'anello è fermo, i 5 s restano.
</script>

{#if undoState.shown}{@const u = undoState.shown}
  {#key u.id}
    <div class="snack" role="status" data-testid="annulla">
      <p>{u.text}</p>
      <button class="undo" onclick={cancel}>
        <svg class="ring" viewBox="0 0 22 22" aria-hidden="true" style:--undo="{UNDO_MS}ms"><circle class="bg" cx="11" cy="11" r="9" /><circle class="fg" cx="11" cy="11" r="9" /></svg>{t.undo}
      </button>
    </div>
  {/key}
{/if}

<style>
  .snack { position: fixed; left: 50%; bottom: calc(16px + env(safe-area-inset-bottom, 0px)); transform: translateX(-50%); z-index: 50; display: flex; align-items: center; gap: 12px; max-width: min(92vw, 480px); padding: 8px 8px 8px 16px; border-radius: 14px; background: var(--s3); color: var(--tx); box-shadow: 0 6px 24px rgb(0 0 0 / .35); animation: sn-in var(--spring-fast-t) var(--spring-fast) both; }
  @keyframes sn-in { from { opacity: 0; transform: translate(-50%, 8px); } }
  p { flex: 1; min-width: 0; margin: 0; font-size: 14.5px; }
  .undo { flex: none; display: inline-flex; align-items: center; gap: 8px; min-height: 40px; padding: 0 10px; font: 600 15px var(--display); color: var(--tx); }
  .ring { width: 22px; height: 22px; transform: rotate(-90deg); }
  circle { fill: none; stroke-width: 3; }
  .bg { stroke: color-mix(in srgb, var(--tx) 18%, transparent); }
  .fg { stroke: var(--tx); stroke-dasharray: 56.55; stroke-dashoffset: 0; animation: drain var(--undo) linear forwards; }
  @keyframes drain { to { stroke-dashoffset: 56.55; } }
  @media (prefers-reduced-motion: reduce) { .fg { animation: none; } .snack { animation: none; } }
</style>
