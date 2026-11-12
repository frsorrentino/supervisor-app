<script lang="ts">
  import { getContext } from 'svelte'
  import AttesaFiles from './AttesaFiles.svelte'
  import type { WaitCardsHost } from './scelta'
  // Le immagini di cosa guardare di una prova, nella sua scheda di Verifica (app-scheda-scelta-12-11, punto 5: «la stessa scheda per le
  // verifiche, con le immagini di cosa guardare»): i file della sua richiesta (`wid`, contratto 1.130), letti con `attesa` come le schede
  // compatte e con la stessa cache. Senza file, o senza la richiesta, niente.
  let { wid }: { wid: string } = $props()
  const host = getContext<WaitCardsHost | undefined>('wait-cards')
  $effect(() => { host?.ask(wid) })
  const files = $derived.by(() => { const w = host?.read(wid); return w && w !== 'gone' ? w.files ?? [] : [] })
</script>

{#if host && files.length}<AttesaFiles {files} onFile={(f, act) => (act === 'open' ? host.openFile(f) : host.file(f))} />{/if}
