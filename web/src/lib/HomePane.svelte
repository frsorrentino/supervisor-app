<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { Session } from './contract'
  import type { Entry } from './masterHome'
  import MasterDock from './MasterDock.svelte'
  import { reducedMotion } from './motion'

  // La home come sul telefono (summaryPage di MainActivity): la lista con la barra della master in fondo; la master si
  // espande sul posto, dentro la home, con la sua barra in cima, e torna ridotta toccando la barra. Non diventa mai una
  // colonna o una pagina a parte (segnalazione di Franz, 05/10 17:45: si sganciava dalla home e non tornava più).
  // Da chiusa, sotto la barra, il campo della master: chi scrive apre la conversazione (Franz, 06/10 10:45).
  // Le misure (Franz, 08/10 18:15): sul telefono barra e tutto schermo, con una testata sola che porta il menu ≡ (`menu`);
  // sulla plancia (`halfStops`) anche la metà, con la home sopra e la conversazione sotto (`half`, cambiata da `onHalf`).
  // A tutta altezza la testata della home non si vede: il menu ≡ va nella barra della master, come sul telefono (09/10 21:26).
  let { master, entries, open, onToggle, onSpeak, quick, list, chat, menu, halfStops = false, half = false, onHalf = () => {} }: {
    master: Session | null; entries: Entry[]; open: boolean; onToggle: (open: boolean) => void; onSpeak: (text: string) => void
    menu?: Snippet; halfStops?: boolean; half?: boolean; onHalf?: (half: boolean) => void
    /** Il campo completo della master (lo stesso Composer della conversazione, con il +). */
    quick: Snippet
    list: Snippet; chat: Snippet
  } = $props()
  // Il trascinamento della barra sposta tutta la conversazione (Franz, 01/11: «si deve muovere il contenuto con la barra del
  // titolo»; SessionSheet.kt). Mentre si muove ci sono tutte e due le pagine: la home sotto, sopra il pannello rigido alto come
  // sarà da aperto, che ha la barra in cima. Il campo resta fermo: quello della home sta sopra il pannello finché non arriva.
  let paneH = $state(0)
  let closedEl = $state<HTMLElement | null>(null)
  let dockTop = $state(0)
  let dy = $state<number | null>(null)
  let target = $state<boolean | null>(null)
  let carried = $state(false)
  const moving = $derived(dy != null || target != null)
  const panelY = $derived(target != null ? (target ? 0 : dockTop) : open ? Math.min(dockTop, Math.max(0, dy ?? 0)) : Math.max(0, dockTop + Math.min(0, dy ?? 0)))
  const shade = $derived(dockTop ? 1 - panelY / dockTop : open ? 1 : 0)
  $effect(() => { if (!open && !moving) carried = false })
  $effect(() => { if (moving && closedEl) dockTop = closedEl.offsetTop })
  function drag(d: number) {
    if (reducedMotion()) return
    carried = true; dy = d
  }
  function drop(d: number) {
    const to = open ? d <= 40 : d < -40
    if (reducedMotion() || dy == null) { dy = null; if (to !== open) onToggle(to); return }
    // Una molla sola verso la fine; lo stato cambia a corsa finita, così nessuna pagina salta.
    target = to; dy = null
    setTimeout(() => { if (to !== open) onToggle(to); target = null }, 450)
  }
  const sheetBasis = $derived(dy == null ? null : `${Math.min(paneH, Math.max(64, (half ? paneH * 0.55 : paneH) - dy))}px`)
  function sheetDrop(d: number) {
    dy = null
    if (d < -40 && half) onHalf(false)
    else if (d > 40) { if (half) onToggle(false); else onHalf(true) }
  }
</script>

<div class="pane v3" bind:clientHeight={paneH}>
  {#if master && open && halfStops}
    <!-- La plancia: la home resta sotto e il foglio cresce fino a metà o a tutta altezza; a metà la home sopra si tocca. -->
    <div class="body above" inert={!half}>{@render list()}</div>
    <div class="sheet" class:full={!half} class:held={dy != null} style:flex-basis={sheetBasis}>
      <MasterDock {master} {entries} expanded toggleUp={half} onToggle={() => (half ? onToggle(false) : onHalf(true))}
        onToggleKey={() => onHalf(!half)} onClose={half ? () => onToggle(false) : undefined} trailing={half ? undefined : menu} {onSpeak} onDrag={(d) => { if (!reducedMotion()) dy = d }} onDrop={sheetDrop} />
      <div class="body">{@render chat()}</div>
    </div>
  {:else}
    {#if !master || !open || moving}
      <div class="body" style:opacity={moving ? 1 - shade : null} style:scale={moving ? 1 - 0.04 * shade : null}>{@render list()}</div>
      {#if master}<div class="closed" bind:this={closedEl}><MasterDock {master} {entries} onToggle={() => onToggle(true)} {onSpeak} onDrag={halfStops ? undefined : drag} onDrop={halfStops ? undefined : drop} /><div class="quick">{@render quick()}</div></div>{/if}
    {/if}
    {#if master && (open || moving)}
      <div class="slide" class:carried class:moving class:settling={target != null} style:height={moving ? `${paneH}px` : null} style:transform={moving ? `translateY(${panelY}px)` : null}>
        <MasterDock {master} {entries} expanded screen trailing={menu} onToggle={() => onToggle(false)} {onSpeak} onDrag={drag} onDrop={drop} />
        <div class="body">{@render chat()}</div>
      </div>
    {/if}
  {/if}
</div>

<style>
  .pane { position: relative; overflow: hidden; height: 100%; display: flex; flex-direction: column; min-height: 0; }
  .slide { flex: 1; min-height: 0; display: flex; flex-direction: column; animation: up .45s var(--spring); }
  .slide.carried { animation: none; }
  .slide.moving { position: absolute; z-index: 2; inset: 0 0 auto; background: var(--bg); border-radius: 18px 18px 0 0; overflow: hidden; }
  .slide.settling { transition: transform .45s var(--spring); }
  .quick { position: relative; z-index: 3; background: var(--master-high); }
  .sheet.held { transition: none; }
  /* La master chiusa: linguetta e campo su un solo fondo, velato di celeste (Celeste velato, 06/10 21:23). */
  .closed { border-radius: 18px 18px 0 0; background: var(--master-high); border-top: 1px solid var(--line); }
  .body { flex: 1; min-height: 0; display: flex; flex-direction: column; }
  .above { overflow: hidden; }
  /* Il foglio della plancia: 55 % della colonna, tutta a schermo intero; cresce dalla barra quando si apre. */
  .sheet { flex: 0 0 55%; min-height: 0; display: flex; flex-direction: column; border-radius: 18px 18px 0 0; border-top: 1px solid var(--line); overflow: hidden;
    transition: flex-basis .45s var(--spring); animation: grow .45s var(--spring); }
  .sheet.full { flex-basis: 100%; }
  @keyframes grow { from { flex-basis: 64px; } }
  /* La master sale dal basso, dalla sua barra. */
  @keyframes up { from { translate: 0 40px; opacity: 0; } }
  @media (prefers-reduced-motion: reduce) { .slide, .sheet { animation: none; transition: none; } }
</style>
