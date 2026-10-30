<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { Session } from './contract'
  import type { Entry } from './masterHome'
  import MasterDock from './MasterDock.svelte'
  import { STUDIO, dockMessage, dragHeight, settle, type DockState } from './dockEmbed'

  // La master dentro Studio (`/embed?dock=1`, studio-dock-coerente-24-10): la stessa barra della home, chiusa in basso, e
  // lo stesso foglio aperto, col gesto in su e in giù come nella web app. La pagina è trasparente: il riquadro di Studio
  // prende l'altezza che questa gli manda (dockEmbed.ts), aperta fin sotto la barra in alto di Studio.
  // Senza collegamento (`pair`) la pagina del collegamento, nel riquadro: lo spazio di un iframe è separato da quello della
  // web app, quindi il browser si collega qui una volta.
  let { master, entries, onSpeak, chat, pair }: {
    master: Session | null; entries: Entry[]; onSpeak: (text: string) => void; chat: Snippet; pair?: Snippet
  } = $props()

  let expanded = $state(false)
  let drag = $state<number | null>(null)
  let bar = $state<HTMLElement>()
  let box = $state<HTMLElement>()
  let closedH = 64
  // Aperta chiede tutto lo schermo: Studio la ferma sotto la sua barra in alto.
  const openH = () => Math.round(screen.availHeight || innerHeight)
  const send = (state: DockState, height: number) => { if (parent !== window) parent.postMessage(dockMessage(state, height), STUDIO) }

  const phase = $derived<DockState>(pair ? 'unpaired' : drag != null ? 'dragging' : expanded ? 'open' : 'closed')
  // L'altezza della barra chiusa (o della pagina di collegamento) si misura; le altre si mandano a ogni cambio.
  $effect(() => {
    const el = pair ? box : expanded || drag != null ? null : bar
    if (!el) return
    const s = phase
    const ro = new ResizeObserver(() => { const h = pair ? el.scrollHeight : el.offsetHeight; if (!pair) closedH = h; send(s, h) })
    ro.observe(el)
    return () => ro.disconnect()
  })
  $effect(() => { if (phase === 'open') send('open', openH()) })

  function onDrag(dy: number) {
    drag = dy
    send('dragging', dragHeight(expanded ? openH() : closedH, dy, closedH, openH()))
  }
  function onDrop(dy: number) { expanded = settle(expanded, dy); drag = null }
  function toggle() { expanded = !expanded }
</script>

{#if pair}
  <div class="pair v3" bind:this={box}>{@render pair()}</div>
{:else if master}
  <!-- Una sola barra per chiusa, gesto e aperta: rimontarla a metà gesto perderebbe il dito. -->
  <div class="dock v3" class:sheet={expanded || drag != null} bind:this={bar}>
    <MasterDock {master} {entries} {expanded} onToggle={toggle} {onSpeak} {onDrag} {onDrop} />
    {#if expanded || drag != null}<div class="body">{@render chat()}</div>{/if}
  </div>
{/if}

<style>
  /* Il foglio riempie il riquadro che Studio dimensiona: la barra in cima, la conversazione sotto, su fondo pieno. */
  /* Sotto .v3 come il resto (Studio, 30/10 22:01); da chiusa la barra resta su fondo trasparente. */
  .dock { position: fixed; left: 0; right: 0; bottom: 0; background: none; }
  /* In home la barra della v3 è trasparente sulla colonna; qui sotto c'è la pagina di Studio e il fondo deve restare. */
  .dock.v3 > :global(.dock) { background: var(--master-highest); }
  .dock.sheet { top: 0; display: flex; flex-direction: column; border-radius: 26px 26px 0 0; overflow: hidden; background: var(--bg); }
  .body { flex: 1; min-height: 0; display: flex; flex-direction: column; }
  .pair { position: fixed; left: 0; right: 0; bottom: 0; max-height: 100%; overflow: auto; border-radius: 26px 26px 0 0; background: var(--bg); }
</style>
