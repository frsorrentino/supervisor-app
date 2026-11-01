<script lang="ts">
  import type { Approval } from './contract'
  import { okLines } from './decide'
  import { t } from './t'
  // Le tre righe dell'ok informato (contratto 1.99): quello che manca dice «non dichiarato», non si inventa. Poi i compiti che partono con l'ok.
  let { a }: { a: Approval } = $props()
  const l = $derived(okLines(a))
  const undo = $derived((l.undo === 'si' ? t.okUndoYes : l.undo === 'no' ? t.okUndoNo : l.undo === 'in-parte' ? t.okUndoPartly : t.okUndeclared) + (l.undoHow ? ` · ${l.undoHow}` : ''))
</script>

<div class="ki" data-testid="ok-informato">
  <span>{t.okUntested}</span><span class:m={!l.untested}>{l.untested ?? t.okUndeclared}</span>
  <span>{t.okUndo}</span><span class:m={!l.undo}>{undo}</span>
  {#if l.precedent}<span>{t.okPrecedent}</span><span>{(l.precedent.relation === 'fixes' ? t.okPrecedentFixes : t.okPrecedentDefect)(l.precedent.short ?? l.precedent.task)}</span>{/if}
  {#if a.also?.length}<span>{t.okAlso}</span><span>{a.also.map(x => x.short ?? x.task).join(', ')}</span>{/if}
</div>

<style>
  .ki { display: grid; grid-template-columns: max-content 1fr; gap: 2px 10px; font-size: 13px; margin: 4px 0; }
  .ki span:nth-child(odd), .m { color: var(--text2); }
</style>
