<script lang="ts">
  import type { ProofSteps } from './contract'
  import { fill, line, look } from './passiProva'
  import { t } from './t'
  // Voce 5 della prima proposta di animazioni (Passi della prova, app-animazioni-passi-26-11, contratto 1.162), come ProofRail sul
  // telefono: le tappe del compito in verifica su un filo che si riempie fino alla tappa di adesso; le passate spunte verdi, quella in
  // corso con l'arco che gira, una fallita rossa; sotto, a parole, dove si è. Con le animazioni ridotte niente movimento.
  let { steps }: { steps: ProofSteps } = $props()
  const items = $derived(steps.items)
  const l = $derived(line(steps))
  const say = $derived(!l ? '' : l.say === 'running' ? t.proofRunning(l.label) : l.say === 'failed' ? t.proofFailed(l.label) : l.say === 'todo' ? t.proofTodo(l.label) : t.proofAllDone)
</script>

{#if items.length}
  <div class="pr" data-testid="passi-prova" style:--n={items.length}>
    <div class="steps">
      <div class="rail" aria-hidden="true"><i style:width="{fill(items) * 100}%"></i></div>
      {#each items as s (s.key)}
        <div class="st {look(s)}">
          <span class="dot" aria-hidden="true">
            {#if look(s) === 'done'}<svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="#0B0B0B" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5" /></svg>
            {:else if look(s) === 'failed'}<svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#0B0B0B" stroke-width="3.2" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18" /></svg>{/if}
          </span>
          <span class="lb">{s.label}</span>
        </div>
      {/each}
    </div>
    {#if say}<p class="say mono">{say}</p>{/if}
  </div>
{/if}

<style>
  .pr { margin-top: 8px; }
  .steps { position: relative; display: grid; grid-template-columns: repeat(var(--n), 1fr); }
  .rail { position: absolute; left: calc(50% / var(--n)); right: calc(50% / var(--n)); top: 10px; height: 2px; background: var(--s3); border-radius: 1px; }
  .rail i { position: absolute; left: 0; top: 0; bottom: 0; background: var(--good); border-radius: 1px; transition: width 400ms cubic-bezier(.3, 0, .2, 1); }
  .st { position: relative; display: grid; justify-items: center; gap: 4px; font-size: 13px; color: var(--tx2); }
  .dot { width: 22px; height: 22px; border-radius: 50%; display: grid; place-items: center; background: var(--s1); border: 2px solid var(--tx3); box-sizing: border-box; }
  .done .dot { background: var(--good); border-color: var(--good); }
  .done { color: var(--tx); }
  .failed .dot { background: var(--red); border-color: var(--red); }
  .failed { color: var(--red); }
  .now { color: var(--busy); }
  .now .dot { position: relative; border-color: color-mix(in srgb, var(--busy) 35%, transparent); }
  .now .dot::after { content: ''; position: absolute; inset: -2px; border-radius: 50%; border: 2px solid transparent; border-top-color: var(--busy); animation: pr-spin 1.2s linear infinite; }
  @keyframes pr-spin { to { transform: rotate(360deg); } }
  .lb { white-space: nowrap; }
  .say { margin: 4px 0 0; }
  @media (prefers-reduced-motion: reduce) { .rail i { transition: none; } .now .dot::after { animation: none; border-color: var(--busy); } }
</style>
