<script lang="ts">
  import type { Snippet } from 'svelte'
  import { untrack } from 'svelte'
  import type { Session } from './contract'
  import type { Entry } from './masterHome'
  import MasterSheet from './MasterSheet.svelte'
  import { ROW_PX, type Stop } from './masterSheet'
  import { STUDIO, dockFrame, dockMessage, learnSpace, type DockState } from './dockEmbed'

  // La master dentro Studio (`/embed?dock=1`, studio-dock-coerente-24-10). app-posizioni-sistema-27-11 gruppo 6: il foglio della master
  // della web app (MasterSheet) a tutta larghezza, con le tre misure e gli stessi gesti, al posto della barra di prima; parte dalla riga.
  // La pagina è trasparente: il foglio sta in fondo a un posto alto quanto il riquadro può diventare (`space`), e Studio dà al riquadro
  // l'altezza del foglio che questa gli manda a ogni passo (dockEmbed.ts), così le misure del foglio non cambiano mentre il riquadro lo segue.
  // Senza collegamento (`pair`) la pagina del collegamento, nel riquadro: lo spazio di un iframe è separato da quello della web app,
  // quindi il browser si collega qui una volta.
  let { master, entries, onSpeak, chat, pair }: {
    master: Session | null; entries: Entry[]; onSpeak: (text: string) => void; chat: Snippet; pair?: Snippet
  } = $props()

  // Dentro Studio i messaggi vanno a lui; aperta da sola (prove, demo) il posto è la finestra.
  const inStudio = parent !== window
  const send = (state: DockState, height: number) => { if (inStudio) parent.postMessage(dockMessage(state, height), STUDIO) }

  let box = $state<HTMLElement>()
  let stop = $state<Stop>('row')
  // Aperto, il foglio chiede tutto lo schermo; Studio ferma il riquadro sotto la sua barra in alto e il posto impara quanto resta.
  let space = $state(Math.round(inStudio ? (screen.availHeight || innerHeight) : innerHeight))
  // Un menu o una finestra aperti nel foglio: il riquadro prende tutto il posto, se no il menu resterebbe chiuso nella riga.
  let popup = $state(false)
  // Fuori dalla reattività: l'altezza del foglio, la misura dove sta e l'ultimo messaggio, per non ripeterlo.
  let h = ROW_PX
  let at: Stop | 'move' = 'row'
  let sent: { state: DockState | null; height: number } = { state: null, height: -1 }
  function report() {
    if (pair) return
    const f = dockFrame(at, h, space, popup)
    if (f.state === sent.state && f.height === sent.height) return
    sent = f
    send(f.state, f.height)
  }
  function onHeight(x: number, a: Stop | 'move') { h = x; at = a; untrack(report) }
  $effect(() => { void popup; void space; untrack(report) })

  $effect(() => {
    if (pair) return
    const check = () => { popup = !!document.querySelector('.msheet [role="menu"], dialog[open]') }
    const mo = new MutationObserver(check)
    mo.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['open'] })
    check()
    return () => mo.disconnect()
  })
  // Il riquadro più basso del chiesto, a riquadro fermo (Studio può animarlo): il posto cala di quanto manca (learnSpace).
  $effect(() => {
    if (pair) return
    let timer: ReturnType<typeof setTimeout> | undefined
    const onResize = () => {
      if (!inStudio) { space = innerHeight; return }
      clearTimeout(timer)
      timer = setTimeout(() => { if (at !== 'move' && sent.state === 'open') space = learnSpace(space, sent.height, innerHeight) }, 400)
    }
    addEventListener('resize', onResize)
    return () => { removeEventListener('resize', onResize); clearTimeout(timer) }
  })
  // Senza collegamento l'altezza della pagina del collegamento, a ogni cambio.
  $effect(() => {
    if (!pair || !box) return
    const el = box
    const ro = new ResizeObserver(() => send('unpaired', el.scrollHeight))
    ro.observe(el)
    return () => ro.disconnect()
  })
</script>

{#if pair}
  <div class="pair v3" bind:this={box}>{@render pair()}</div>
{:else if master}
  <!-- Il posto del foglio, in fondo al riquadro: sopra il foglio la pagina è trasparente e si vede Studio. -->
  <div class="host v3" style:height="{space}px">
    <MasterSheet {master} {entries} {stop} onStop={(s) => (stop = s)} {onSpeak} {chat} screen={inStudio} {onHeight} />
  </div>
{/if}

<style>
  /* Il posto del foglio: alto quanto il riquadro può diventare e ancorato in fondo; il foglio ci sta dentro in basso, a tutta larghezza. */
  .host { position: fixed; left: 0; right: 0; bottom: 0; background: none; }
  .pair { position: fixed; left: 0; right: 0; bottom: 0; max-height: 100%; overflow: auto; border-radius: 28px 28px 0 0; background: var(--bg); }
</style>
