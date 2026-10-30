<script lang="ts">
  import type { Snippet } from 'svelte'
  // La plancia della web larga senza sessioni aperte (pwa2, approvata da Franz il 30/10 alle 10:5x, stile «Studio e Diretta»):
  // la testata fuori dalla griglia, quattro colonne per argomento con la loro cifra in testa (Richiede te, Attività e code,
  // Esiti, Numeri e strumenti) e la master come barra in fondo; a metà o aperta è un pannello a destra alto quanto lo schermo,
  // 420 o 720 px. Le colonne sono quante ne stanno a 320 px l'una; quelle che non stanno scendono in fondo all'ultima.
  let { width, head, you, work, out, tools, bar, panel = null, panelWide = false }: {
    width: number; head: Snippet; you: Snippet; work: Snippet; out: Snippet; tools: Snippet; bar: Snippet
    panel?: Snippet | null; panelWide?: boolean
  } = $props()
  const PANEL = 420, PANEL_WIDE = 720, COL = 320
  const n = $derived(Math.max(1, Math.min(4, Math.floor((width - (panel ? (panelWide ? PANEL_WIDE : PANEL) : 0)) / COL))))
  const all = $derived([you, work, out, tools])
  // Le colonne che non stanno: in fondo all'ultima che si vede.
  const shownCols = $derived(all.slice(0, n).map((c, i) => (i === n - 1 ? all.slice(i) : [c])))
</script>

<div class="plancia v3">
  <div class="stage">
    {@render head()}
    <div class="cols" style:--n={n}>
      {#each shownCols as parts, i (i)}
        <section class="col">{#each parts as p, k (k)}{@render p()}{/each}</section>
      {/each}
    </div>
    {#if !panel}{@render bar()}{/if}
  </div>
  {#if panel}<aside class="mpanel" class:wide={panelWide}>{@render panel()}</aside>{/if}
</div>

<style>
  .plancia { height: 100%; display: flex; min-height: 0; }
  .stage { flex: 1; min-width: 0; display: flex; flex-direction: column; min-height: 0; }
  .cols { flex: 1; min-height: 0; display: grid; grid-template-columns: repeat(var(--n), minmax(0, 1fr)); border-top: 1px solid var(--line); }
  .col { min-width: 0; min-height: 0; overflow-y: auto; padding-bottom: 24px; }
  .col + .col { border-left: 1px solid var(--line); }
  .mpanel { flex: none; width: 420px; display: flex; flex-direction: column; min-height: 0; background: var(--s1); border-left: 1px solid var(--line); }
  .mpanel.wide { width: 720px; }
</style>
