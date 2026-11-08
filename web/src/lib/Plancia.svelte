<script lang="ts">
  import type { Snippet } from 'svelte'
  // La plancia della web larga senza sessioni aperte (pwa2, approvata da Franz il 30/10 alle 10:5x, stile «Studio e Diretta»):
  // la testata fuori dalla griglia, quattro colonne per argomento con la loro cifra in testa (Richiede te, Attività e code,
  // Esiti, Numeri e strumenti) e la master come barra in fondo; a metà o aperta è un pannello a destra alto quanto lo schermo,
  // 420 o 720 px. Le colonne sono quante ne stanno a 360 px l'una (320 fino alle sessioni a sé, app-home-sessioni-a-se-08-11: nella colonna
  // delle sessioni nome, tasto e scelta devono starci); quelle che non stanno scendono in fondo all'ultima.
  // L'ordine delle colonne è quello delle categorie della home (app-home-categorie-02-11), lo stesso di Riordina.
  // `sessions` (app-home-sessioni-a-se-08-11): la colonna delle sessioni, che stanno a sé; `work` è quella di Lavori.
  let { width, head, sessions, you, work, out, tools, bar, top = null, panel = null, panelWide = false, panelLeft = false, order = ['sessions', 'you', 'work', 'tools'] }: {
    width: number; head: Snippet; top?: Snippet | null; sessions: Snippet; you: Snippet; work: Snippet; out: Snippet; tools: Snippet; bar: Snippet; order?: string[]
    panel?: Snippet | null; panelWide?: boolean; panelLeft?: boolean
  } = $props()
  const PANEL = 420, PANEL_WIDE = 720, COL = 360
  const all = $derived(order.map(id => ({ sessions, you, work, out, tools } as Record<string, Snippet>)[id]).filter(Boolean))
  const n = $derived(Math.max(1, Math.min(all.length, Math.floor((width - (panel ? (panelWide ? PANEL_WIDE : PANEL) : 0)) / COL))))
  // Le colonne che non stanno: in fondo all'ultima che si vede.
  const shownCols = $derived(all.slice(0, n).map((c, i) => (i === n - 1 ? all.slice(i) : [c])))
</script>

<div class="plancia v3" class:left={panelLeft}>
  <div class="stage">
    {@render head()}
    <!-- Lo spazio in cima della home (app-home-in-cima-02-11): le sue voci, a tutta larghezza sopra le colonne. -->
    {#if top}<div class="topband">{@render top()}</div>{/if}
    <div class="cols" style:--n={n}>
      {#each shownCols as parts, i (i)}
        <section class="col lit">{#each parts as p, k (k)}{@render p()}{/each}</section>
      {/each}
    </div>
    {#if !panel}{@render bar()}{/if}
  </div>
  {#if panel}<aside class="mpanel" class:wide={panelWide}>{@render panel()}</aside>{/if}
</div>

<style>
  .plancia { height: 100%; display: flex; min-height: 0; }
  .stage { flex: 1; min-width: 0; display: flex; flex-direction: column; min-height: 0; }
  /* Le colonne sono pannelli staccati sullo sfondo illuminato (app-luci-02-11, luci2-web-largo). */
  .cols { flex: 1; min-height: 0; display: grid; grid-template-columns: repeat(var(--n), minmax(0, 1fr)); gap: 12px; padding: 12px; }
  .topband { flex: none; max-height: 40%; overflow-y: auto; border-top: 1px solid var(--line); }
  .col { min-width: 0; min-height: 0; overflow-y: auto; padding-bottom: 24px; border-radius: 14px; --lit-bg: var(--s1); --lit-angle: 150deg; }
  /* La corsia di 28 px verso le colonne, come il pannello accanto alle sessioni (la disegna la presa della larghezza). */
  .mpanel { position: relative; flex: none; width: 420px; display: flex; flex-direction: column; min-height: 0; background: var(--s1); margin-left: 28px; }
  .mpanel.wide { width: 720px; }
  /* Il pannello a sinistra: le colonne si stringono dall'altra parte. */
  .plancia.left { flex-direction: row-reverse; }
  .plancia.left .mpanel { margin-left: 0; margin-right: 28px; }
</style>
