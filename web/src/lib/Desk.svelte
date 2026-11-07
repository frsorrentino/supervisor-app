<script lang="ts">
  import { untrack, type Snippet } from 'svelte'
  import { drag, equal, parts, stripsOf, stripWidths, swap as swapCols, touchRecent, TOTAL } from './tablet'
  import { t } from './t'

  // La plancia (TabletDesk, seconda versione): la home di lato, a sinistra o a destra con ⇄; da una a quattro sessioni in
  // colonne affiancate. Una colonna presa per la testata e lasciata sopra un'altra scambia il posto con lei; i bordi fra le
  // colonne si trascinano a scatti di un dodicesimo (la colonna che cresce prende da tutte le altre).
  let { cols, shares, homeRight, onCols, onShares, onHomeSide, home, column, empty, override = null, details = null, tab = null, strip = null }: {
    cols: string[]; shares: number[]; homeRight: boolean
    onCols: (c: string[]) => void; onShares: (s: number[]) => void; onHomeSide?: (() => void) | null
    home: Snippet; column: Snippet<[string, (e: PointerEvent) => void]>; empty: Snippet
    /** Al posto delle colonne, per esempio il Registro aperto dal menu della home (TabletDesk.override). */
    override?: Snippet | null
    /** La linguetta sopra ogni colonna nella barra del titolo della web app installata: posata e animata con la colonna. */
    tab?: Snippet<[string, (e: PointerEvent) => void]> | null
    /** I dettagli della prima colonna, dall'altra parte della home (TabletInspector). */
    details?: Snippet | null
    /** app-colonne-strette-05-11: la colonna a striscia, col tocco che la riapre; null = mai strisce. */
    strip?: Snippet<[string, () => void]> | null
  } = $props()

  const GAP = 12
  // Il margine delle card della home (13) dai due lati: la linea della corsia sta a metà (app-gutter-centrato-04-11), e con la
  // master di lato la corsia c'è anche dall'altra parte (app-tablet-barre-bordi-05-11).
  const PAD = 13
  const PAD_HANDLE = 13
  const padLeft = $derived(homeRight ? PAD : PAD_HANDLE)
  let cw = $state(0)
  const widthPx = $derived(Math.max(0, cw - PAD - PAD_HANDLE - GAP * (cols.length - 1)))
  const unit = $derived(widthPx / TOTAL)
  // Le colonne dalla usata più di recente (Tablet.Strips): una nuova entra in testa, un tocco la porta in testa. Sotto la larghezza
  // leggibile le colonne in più sono strisce: larghezze fisse, niente bordi né scambi da trascinare.
  let recent = $state<string[]>([])
  $effect(() => {
    const c = cols
    const r = untrack(() => recent)
    const next = [...c.filter(n => !r.includes(n)), ...r.filter(n => c.includes(n))]
    if (next.join('\n') !== r.join('\n')) recent = next
  })
  const touch = (name: string) => { if (recent[0] !== name) recent = touchRecent(recent, name) }
  const strips = $derived(strip ? stripsOf(cols, widthPx, recent) : new Set<string>())

  // Il bordo che si trascina e di quanto; la colonna presa per la testata e di quanto.
  let border = $state(-1)
  let borderPx = $state(0)
  let dragged = $state(-1)
  let draggedPx = $state(0)
  const live = $derived(strips.size ? stripWidths(cols, shares, widthPx, strips) : (border >= 0 ? drag(shares, border, parts(borderPx, widthPx)) : shares).map(p => p * unit))
  const lefts = $derived(live.reduce<number[]>((acc, w, i) => [...acc, i === 0 ? padLeft : acc[i - 1] + live[i - 1] + GAP], []))
  // La colonna sotto il centro di quella trascinata: lì cadrebbe, e intanto scivola già nel posto lasciato libero.
  const over = $derived.by(() => {
    if (dragged < 0) return -1
    const c = lefts[dragged] + live[dragged] / 2 + draggedPx
    let k = 0
    lefts.forEach((l, i) => { if (l <= c) k = i })
    return k
  })
  const slot = (i: number) => (dragged >= 0 && i === over && over !== dragged ? dragged : i)

  function grabColumn(i: number, e: PointerEvent) {
    if (e.button !== 0 || strips.size) return
    const el = e.currentTarget as HTMLElement
    const x0 = e.clientX
    let moved = false
    try { el.setPointerCapture(e.pointerId) } catch { /* un puntatore già rilasciato */ }
    const move = (m: PointerEvent) => {
      const dx = m.clientX - x0
      if (!moved && Math.abs(dx) < 5) return
      moved = true; dragged = i; draggedPx = dx
    }
    const up = () => {
      el.removeEventListener('pointermove', move); el.removeEventListener('pointerup', up); el.removeEventListener('pointercancel', up)
      const target = over
      const from = dragged
      dragged = -1; draggedPx = 0
      if (moved && target >= 0 && target !== from) onCols(swapCols(cols, from, target))
    }
    el.addEventListener('pointermove', move); el.addEventListener('pointerup', up); el.addEventListener('pointercancel', up)
  }
  function grabBorder(i: number, e: PointerEvent) {
    const el = e.currentTarget as HTMLElement
    const x0 = e.clientX
    try { el.setPointerCapture(e.pointerId) } catch { /* un puntatore già rilasciato */ }
    border = i; borderPx = 0
    const move = (m: PointerEvent) => { borderPx = m.clientX - x0 }
    const up = () => {
      el.removeEventListener('pointermove', move); el.removeEventListener('pointerup', up); el.removeEventListener('pointercancel', up)
      onShares(drag(shares, i, parts(borderPx, widthPx)))
      border = -1; borderPx = 0
    }
    el.addEventListener('pointermove', move); el.addEventListener('pointerup', up); el.addEventListener('pointercancel', up)
  }
  const unequal = $derived(cols.length > 1 && shares.join() !== equal(cols.length).join())
</script>

<div class="desk" class:right={homeRight} class:details={!!details}>
  <aside class="home">{@render home()}</aside>
  <div class="handle">
    <i></i>
    <!-- Senza `onHomeSide` la home segue la master (app-pwa-master-tasti-03-11, proposta 1): niente secondo tasto di posizione. -->
    {#if onHomeSide}<button class="hb" aria-label={homeRight ? t.tabletHomeLeft : t.tabletHomeRight} title={homeRight ? t.tabletHomeLeft : t.tabletHomeRight} onclick={onHomeSide}>
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m16 3 4 4-4 4M20 7H4M8 21l-4-4 4-4M4 17h16" /></svg>
    </button>{/if}
    {#if unequal}
      <button class="hb" aria-label={t.tabletResetWidths} title={t.tabletResetWidths} onclick={() => onShares(equal(cols.length))}>
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M9 4v16M15 4v16" /></svg>
      </button>
    {/if}
  </div>
  {#if details}<aside class="det">{@render details()}</aside>{/if}
  <!-- Senza colonne lo spazio è del cruscotto (variante B, 09/10): niente puntini dietro. -->
  <div class="cols" class:resizing={border >= 0} class:dash={!override && !cols.length} bind:clientWidth={cw}>
    {#if override}
      <div class="override">{@render override()}</div>
    {:else if !cols.length}
      <div class="empty">{@render empty()}</div>
    {:else}
      <!-- Dove atterra la colonna presa, tratteggiato come il posto della master (Franz 03/11 22:39, come le icone di Android). -->
      {#if dragged >= 0 && over >= 0}<div class="landslot" aria-hidden="true" style="left:{lefts[over]}px;width:{live[over] ?? 0}px"></div>{/if}
      <!-- In ordine stabile (per nome): spostare i nodi nel DOM fermerebbe la transizione di `left` al rilascio (Franz 03/11 22:50).
           Le colonne stanno in posizione assoluta, conta solo l'indice in `cols`. -->
      {#each [...cols].sort() as name (name)}
        {@const i = cols.indexOf(name)}
        {@const k = slot(i)}
        <div class="col" data-col={name} class:dragging={dragged === i} onpointerdowncapture={() => touch(name)} onanimationend={(e) => { if (e.target === e.currentTarget) e.currentTarget.dataset.shown = '' }}
          style="left:{dragged === i ? lefts[i] + draggedPx : lefts[k]}px;width:{live[k] ?? 0}px">
          {#if strip && strips.has(name)}{@render strip(name, () => touch(name))}{:else}{@render column(name, (e) => grabColumn(i, e))}{/if}
        </div>
        {#if tab}
          <div class="tabslot" class:dragging={dragged === i} style="left:{dragged === i ? lefts[i] + draggedPx : lefts[k]}px;max-width:{live[k] ?? 0}px">{@render tab(name, (e) => grabColumn(i, e))}</div>
        {/if}
      {/each}
      {#each strips.size ? [] : cols.slice(0, -1) as _, i}
        <div class="border" class:on={border === i} style="left:{lefts[i] + live[i]}px" role="separator" aria-orientation="vertical"
          onpointerdown={(e) => grabBorder(i, e)}><i></i></div>
      {/each}
    {/if}
  </div>
</div>

<style>
  .desk { display: grid; grid-template-columns: 400px 28px minmax(0, 1fr); grid-template-areas: 'home handle cols'; height: 100%; background: var(--bg); }
  .desk.right { grid-template-columns: minmax(0, 1fr) 28px 400px; grid-template-areas: 'cols handle home'; }
  .desk.details { grid-template-columns: 400px 28px minmax(0, 1fr) 341px; grid-template-areas: 'home handle cols det'; }
  .desk.right.details { grid-template-columns: 341px minmax(0, 1fr) 28px 400px; grid-template-areas: 'det cols handle home'; }
  .det { grid-area: det; min-height: 0; border-left: 1px solid var(--line); }
  .desk.right .det { border-left: 0; border-right: 1px solid var(--line); }
  /* view-transition-name fa della home un contesto di impilamento: senza z-index il menu ≡ (z-index 21 dentro la home)
     resta sotto .cols, che viene dopo nel DOM, quando la home è a sinistra. */
  .home { grid-area: home; min-height: 0; display: flex; flex-direction: column; view-transition-name: desk-home; position: relative; z-index: 1; }
  .handle { grid-area: handle; position: relative; display: flex; flex-direction: column; align-items: center; gap: 8px; padding-top: 10px; }
  .handle > i { position: absolute; inset: 0 auto 0 calc(50% - .5px); width: 1px; background: var(--line); }
  .hb { position: relative; width: 28px; height: 28px; border-radius: 50%; background: var(--surface); color: var(--text2); display: grid; place-items: center; }
  .hb:hover { filter: brightness(1.25); }
  /* La griglia di puntini dietro le colonne (TechStyle.dotGrid). */
  .cols { grid-area: cols; position: relative; min-width: 0; overflow: hidden; view-transition-name: desk-cols;
    background-image: radial-gradient(rgb(255 255 255 / .07) 1px, transparent 1.4px); background-size: 16px 16px; }
  /* La colonna della sessione è una scheda come quelle della home: il loro grigio e il loro bordo; dentro, il suo grigio fa da fondo
     (app-superfici-plancia-07-11). */
  .col { position: absolute; top: 10px; bottom: 10px; border-radius: 18px; border: 1px solid var(--line); overflow: hidden; --bg: var(--s1); background: var(--bg);
    transition: left .32s cubic-bezier(.2, .8, .2, 1), width .32s cubic-bezier(.2, .8, .2, 1), scale .2s, box-shadow .2s; animation: appear .22s ease-out; }
  .resizing .col, .resizing .tabslot { transition: none; }
  /* Durante il volo scheda↔colonna (html.fx-open/fx-close) la colonna è già alla sua misura quando la transizione la fotografa:
     con la sua transizione di larghezza la fotografia era una striscia di 2 px e la scheda ci volava dentro (app-volo-web-taratura-05-11). */
  :global(html:is(.fx-open, .fx-close)) .col, :global(html:is(.fx-open, .fx-close)) .tabslot { transition: none; animation: none; }
  .tabslot { position: absolute; top: 6px; height: calc(env(titlebar-area-height, 40px) - 5px); z-index: 3; display: flex;
    transition: left .32s cubic-bezier(.2, .8, .2, 1), max-width .32s cubic-bezier(.2, .8, .2, 1); }
  .tabslot.dragging { transition: none; z-index: 4; }
  .landslot { position: absolute; top: 10px; bottom: 10px; z-index: 1; border: 2px dashed var(--wait); border-radius: 18px; background: color-mix(in srgb, var(--wait) 8%, transparent); transition: left .32s cubic-bezier(.2, .8, .2, 1), width .32s cubic-bezier(.2, .8, .2, 1); pointer-events: none; }
  @media (prefers-reduced-motion: reduce) { .landslot { transition: none; } }
  .col.dragging { transition: scale .2s, box-shadow .2s; z-index: 2; scale: 1.025; box-shadow: 0 18px 50px rgb(0 0 0 / .6); border-color: color-mix(in srgb, var(--icon) 55%, transparent); }
  /* Solo dissolvenza: niente scala che riparte dopo il volo (app-sessione-apertura-06-11). Una volta comparsa, la colonna non
     ricompare più (app-cambio-sessione-fermo-06-11, Franz 06/11 12:09: «si ricarica dopo la prima animazione»): il volo le toglie
     l'animazione e rimettendola il browser la faceva ripartire, la colonna appena arrivata tornava trasparente e sfumava di nuovo. */
  @keyframes appear { from { opacity: 0; } }
  .col:global([data-shown]) { animation: none; }
  .border { position: absolute; top: 0; bottom: 0; width: 12px; cursor: col-resize; display: grid; place-items: center; touch-action: none; }
  .border i { width: 4px; height: 40px; border-radius: 2px; background: var(--line); }
  .border:hover i, .border.on i { background: var(--icon); }
  .override { position: absolute; inset: 10px; border-radius: 18px; border: 1px solid rgb(255 255 255 / .12); overflow: hidden; background: var(--bg); }
  .cols.dash { background-image: none; }
  .empty { position: absolute; inset: 0; display: grid; place-items: center; color: var(--text2); padding: 40px; text-align: center; }
  @media (prefers-reduced-motion: reduce) { .col, .tabslot { transition: none; animation: none; } }
</style>
