<script lang="ts">
  import type { Snippet } from 'svelte'
  // La plancia della web larga senza sessioni aperte (pwa2, approvata da Franz il 30/10 alle 10:5x, stile «Studio e Diretta»):
  // la testata fuori dalla griglia, quattro colonne per argomento con la loro cifra in testa (Richiede te, Attività e code,
  // Esiti, Numeri e strumenti). Le colonne sono quante ne stanno a 360 px l'una (320 fino alle sessioni a sé,
  // app-home-sessioni-a-se-08-11: nella colonna delle sessioni nome, tasto e scelta devono starci); quelle che non stanno scendono
  // in fondo all'ultima.
  // L'ordine delle colonne è quello delle categorie della home (app-home-categorie-02-11), lo stesso di Riordina.
  // `sessions` (app-home-sessioni-a-se-08-11): la colonna delle sessioni, che stanno a sé; `work` è quella di Lavori.
  // app-posizioni-sistema-27-11 gruppo 4: la master è il foglio (`sheet`) sopra le colonne, a destra e largo quanto la colonna della
  // home (400 px, 440 da 1680), ancorato in fondo; le colonne non si spostano e lasciano in fondo il posto della riga (`sheetRow`).
  // app-posizioni-sistema-27-11 gruppo 5: via il pannello di lato e la barra in fondo, le posizioni di prima.
  let { width, head, sessions, you, work, out, tools, top = null, order = ['sessions', 'you', 'work', 'tools'], sheet = null, sheetRow = 0 }: {
    width: number; head: Snippet; top?: Snippet | null; sessions: Snippet; you: Snippet; work: Snippet; out: Snippet; tools: Snippet; order?: string[]
    /** Il foglio della master, sopra le colonne a destra. */
    sheet?: Snippet | null
    /** L'altezza della riga del foglio: le colonne la lasciano libera in fondo. */
    sheetRow?: number
  } = $props()
  const COL = 360
  const all = $derived(order.map(id => ({ sessions, you, work, out, tools } as Record<string, Snippet>)[id]).filter(Boolean))
  const n = $derived(Math.max(1, Math.min(all.length, Math.floor(width / COL))))
  // Le colonne che non stanno: in fondo all'ultima che si vede.
  const shownCols = $derived(all.slice(0, n).map((c, i) => (i === n - 1 ? all.slice(i) : [c])))
</script>

<div class="plancia v3" style:--sheet-row={sheet && sheetRow ? `${sheetRow}px` : null}>
  <div class="stage">
    {@render head()}
    <!-- Lo spazio in cima della home (app-home-in-cima-02-11): le sue voci, a tutta larghezza sopra le colonne. -->
    {#if top}<div class="topband">{@render top()}</div>{/if}
    <div class="body">
      <div class="cols" style:--n={n}>
        {#each shownCols as parts, i (i)}
          <section class="col lit">{#each parts as p, k (k)}{@render p()}{/each}</section>
        {/each}
      </div>
      {#if sheet}<div class="sheethost" data-sheet-host>{@render sheet()}</div>{/if}
    </div>
  </div>
</div>

<style>
  .plancia { height: 100%; display: flex; min-height: 0; }
  .stage { flex: 1; min-width: 0; display: flex; flex-direction: column; min-height: 0; }
  /* Le colonne sono pannelli staccati sullo sfondo illuminato (app-luci-02-11, luci2-web-largo). */
  .body { position: relative; flex: 1; min-height: 0; display: flex; flex-direction: column; }
  .cols { flex: 1; min-height: 0; display: grid; grid-template-columns: repeat(var(--n), minmax(0, 1fr)); gap: 12px; padding: 12px; }
  /* Il posto del foglio della master: a destra, largo quanto la colonna della home; fuori dal foglio la pagina si tocca. */
  .plancia { --home-w: 400px; }
  @media (min-width: 1680px) { .plancia { --home-w: 440px; } }
  .sheethost { position: absolute; top: 0; right: 0; bottom: 0; width: var(--home-w); pointer-events: none; }
  /* app-posizioni-sistema-27-11, prova 1447: la colonna sotto il foglio si vela con lui (masterSheet.cues); le colonne non arretrano, il
     foglio ne copre una sola. Come la plancia del tablet. Il velo non prende i tocchi. */
  .sheethost::after { content: ''; position: absolute; inset: 0; z-index: 5; pointer-events: none; background: rgb(0 0 0 / var(--sheet-dim, 0)); }
  .sheethost > :global(*) { pointer-events: auto; }
  .topband { flex: none; max-height: 40%; overflow-y: auto; border-top: 1px solid var(--line); }
  .col { min-width: 0; min-height: 0; overflow-y: auto; padding-bottom: calc(24px + var(--sheet-row, 0px)); border-radius: 14px; --lit-bg: var(--s1); --lit-angle: 150deg; }
</style>
