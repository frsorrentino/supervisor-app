<script lang="ts">
  import type { Approval } from './contract'
  import { okLines } from './decide'
  import { t } from './t'
  import LongText from './LongText.svelte'
  // Le tre righe dell'ok informato (contratto 1.99): quello che manca dice «non dichiarato», non si inventa. Poi i compiti che partono con l'ok.
  let { a }: { a: Approval } = $props()
  const l = $derived(okLines(a))
  const undo = $derived((l.undo === 'si' ? t.okUndoYes : l.undo === 'no' ? t.okUndoNo : l.undo === 'in-parte' ? t.okUndoPartly : t.okUndeclared) + (l.undoHow ? ` · ${l.undoHow}` : ''))
</script>

<!-- app-play-lettura-ovunque-27-11: ogni valore col ▶ accanto, se va oltre una riga; le etichette restano nella colonna a sinistra. -->
<div class="ki" data-testid="ok-informato">
  <span>{t.okUntested}</span><div class:m={!l.untested}><LongText text={l.untested ?? t.okUndeclared} /></div>
  <span>{t.okUndo}</span><div class:m={!l.undo}><LongText text={undo} /></div>
  {#if l.precedent}<span>{t.okPrecedent}</span><div><LongText text={(l.precedent.relation === 'fixes' ? t.okPrecedentFixes : t.okPrecedentDefect)(l.precedent.short ?? l.precedent.task)} /></div>{/if}
  {#if a.also?.length}<span>{t.okAlso}</span><div><LongText text={a.also.map(x => x.short ?? x.task).join(', ')} /></div>{/if}
</div>

<style>
  .ki { display: grid; grid-template-columns: max-content 1fr; gap: 2px 10px; font-size: 12.5px; margin: 4px 0; }
  .ki span:nth-child(odd), .m { color: var(--text2); }
</style>
