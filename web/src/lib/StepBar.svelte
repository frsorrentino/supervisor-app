<script lang="ts">
  import type { ProofSteps } from './contract'
  import { line, look } from './passiProva'
  import { t } from './t'
  // Le tappe della prova come barra a 4 segmenti col nome dentro (app-scheda-sessione-a-26-11, proposta A approvata da Franz): sotto il
  // compito che verificano; passata verde con ✓, fallita rossa con ✕, quella in corso col bordo blu che pulsa piano, da fare grigia.
  // Sotto, il motivo a parole. Prende il posto del filo in alto (app-passi-centrati-26-11).
  let { steps }: { steps: ProofSteps } = $props()
  const l = $derived(line(steps))
  const say = $derived(!l ? '' : l.say === 'running' ? t.proofRunning(l.label) : l.say === 'failed' ? t.proofFailed(l.label) : l.say === 'todo' ? t.proofTodo(l.label) : t.proofAllDone)
</script>

{#if steps.items.length}
  <div class="sb" data-testid="passi-prova" style:--n={steps.items.length}>
    {#each steps.items as s (s.key)}
      <span class="seg {look(s)}">{#if look(s) === 'done'}✓ {:else if look(s) === 'failed'}✕ {/if}{s.label}</span>
    {/each}
  </div>
  {#if say}<p class="why" class:bad={l?.say === 'failed'}>{say}</p>{/if}
{/if}

<style>
  .sb { display: grid; grid-template-columns: repeat(var(--n), minmax(0, 1fr)); gap: 4px; margin-top: 8px; }
  .seg { min-width: 0; padding: 7px 4px; border-radius: 6px; text-align: center; font-size: 13px; line-height: 18px; white-space: nowrap; overflow: hidden; background: var(--s2); color: var(--tx2); border: 1px solid var(--line); }
  .seg.done { background: color-mix(in srgb, var(--good) 16%, var(--s1)); border-color: color-mix(in srgb, var(--good) 45%, transparent); color: var(--good); }
  .seg.failed { background: color-mix(in srgb, var(--red) 16%, var(--s1)); border-color: color-mix(in srgb, var(--red) 45%, transparent); color: var(--red); }
  .seg.now { border-color: var(--busy); color: var(--busy); animation: sb-now 1.6s ease-in-out infinite; }
  @keyframes sb-now { 50% { border-color: color-mix(in srgb, var(--busy) 35%, transparent); } }
  .why { margin: 6px 0 0; font-size: 13.5px; color: var(--tx2); }
  .why.bad { color: var(--red); }
  @media (prefers-reduced-motion: reduce) { .seg.now { animation: none; } }
</style>
