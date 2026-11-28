<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { Session } from './contract'
  import type { Entry } from './masterHome'
  import MasterSheet from './MasterSheet.svelte'
  import { ROW_PX, type Stop } from './masterSheet'

  // La home come sul telefono (summaryPage di MainActivity), con la master sopra come foglio (app-posizioni-sistema-27-11 gruppo 4,
  // B «Foglio»): la testata della home in cima, sotto la lista; il foglio (MasterSheet) sale dal fondo fino alla riga, a metà o
  // intero, sopra la lista e sotto la testata, e la lista non si sposta mai: sotto lascia solo lo spazio della riga. Sul telefono a
  // tutta larghezza, sulla web larga nella colonna della home (400 px, 440 da 1680), a 8 px dai bordi.
  // Decisione (gruppo 4): la home tiene il contenuto di oggi (il tabellone con Sessioni dentro, i blocchi fissati, le sezioni); niente
  // riga di icone della home (Per te · Sessioni · Risultati · Lavori) né schede al posto delle sezioni: servono l'ok di Franz.
  let { master, entries, stop, onStop, onSpeak, head, list, chat, inset = false }: {
    master: Session | null; entries: Entry[]; stop: Stop; onStop: (s: Stop) => void; onSpeak: (text: string) => void
    /** La testata della home (AppBar): resta sopra il foglio anche a tutta altezza. */
    head?: Snippet
    list: Snippet
    /** La conversazione della master, o il posto dove arriva quella spostata (web larga). */
    chat: Snippet
    inset?: boolean
  } = $props()
  let rowH = $state(ROW_PX)
</script>

<div class="pane v3">
  {@render head?.()}
  <div class="host" data-sheet-host>
    <div class="under" style:padding-bottom={master ? `${rowH}px` : null}>{@render list()}</div>
    {#if master}<MasterSheet {master} {entries} {stop} {onStop} {onSpeak} {chat} {inset} bind:rowH />{/if}
  </div>
</div>

<style>
  .pane { position: relative; height: 100%; display: flex; flex-direction: column; min-height: 0; }
  /* Il posto del foglio: tutto sotto la testata; la lista ci sta ferma, il foglio sopra. */
  .host { position: relative; flex: 1; min-height: 0; overflow: hidden; }
  .under { position: absolute; inset: 0; display: flex; flex-direction: column; min-height: 0; }
  /* app-posizioni-sistema-27-11, prova 1447: mentre il foglio sale la home dietro arretra (fino a 0,96, dal centro in alto) e si vela (fino
     al 40 % di nero); i valori li scrive il foglio (masterSheet.cues). A scala 1 niente trasformazione: i menu fissi della home restano
     riferiti alla finestra. Il velo non prende i tocchi. */
  .under { scale: var(--sheet-scale, none); transform-origin: 50% 0; }
  .host::after { content: ''; position: absolute; inset: 0; z-index: 5; pointer-events: none; background: rgb(0 0 0 / var(--sheet-dim, 0)); }
</style>
