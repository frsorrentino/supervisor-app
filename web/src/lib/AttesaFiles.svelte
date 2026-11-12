<script lang="ts">
  import type { WaitFile } from './contract'
  import AttesaImmagine from './AttesaImmagine.svelte'
  import Icon from './Icon.svelte'
  import ImageViewer from './ImageViewer.svelte'
  import { imageRatios, media } from './mediaCache.svelte'
  import { frameRatio, sizeText, splitFiles, waitFileKey } from './scelta'
  // I file di una richiesta che non stanno in un'opzione (app-scheda-scelta-12-11, contratto 1.137): le immagini di cosa guardare di
  // una verifica, gli allegati di un ok, i file in più di una scelta. Le immagini nelle loro cornici, affiancate quando sono più
  // d'una, e si ingrandiscono al tocco (nel visore si passa dall'una all'altra); i documenti una riga l'uno, un tocco li apre.
  let { files, onFile }: { files: WaitFile[]; onFile: (f: WaitFile, act: 'preview' | 'open') => void } = $props()
  const parts = $derived(splitFiles(files))
  const ar = $derived(frameRatio(parts.images.map(f => imageRatios[waitFileKey(f)])))
  let zoom = $state<{ items: { src: string; name: string }[]; start: number } | null>(null)
  function enlarge(f: WaitFile) {
    const ready = parts.images.filter(x => media[waitFileKey(x)])
    const start = ready.indexOf(f)
    if (start >= 0) zoom = { items: ready.map(x => ({ src: media[waitFileKey(x)], name: x.name })), start }
  }
</script>

<div class="af" data-testid="attesa-file">
  {#if parts.images.length}
    <div class="pics" class:one={parts.images.length === 1} class:many={parts.images.length > 2} style:--ar={ar}>
      {#each parts.images as f (waitFileKey(f))}<AttesaImmagine file={f} label={f.name} onAsk={(x) => onFile(x, 'preview')} onOpen={() => enlarge(f)} />{/each}
    </div>
  {/if}
  {#if parts.docs.length}
    <div class="docs">
      {#each parts.docs as f (waitFileKey(f))}
        <button class="doc" onclick={() => onFile(f, 'open')}><Icon name={f.mime === 'application/pdf' ? 'pdf' : 'file'} size={18} /><span class="dn">{f.name}</span>{#if f.size != null}<span class="ds">{sizeText(f.size)}</span>{/if}</button>
      {/each}
    </div>
  {/if}
</div>

{#if zoom}<ImageViewer items={zoom.items} start={zoom.start} onClose={() => (zoom = null)} />{/if}

<style>
  .af { container-type: inline-size; display: flex; flex-direction: column; gap: 8px; }
  .pics { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .pics.one { grid-template-columns: minmax(0, 520px); }
  .docs { display: flex; flex-direction: column; }
  .doc { display: flex; align-items: center; gap: 8px; min-height: 44px; text-align: left; color: var(--tx); touch-action: manipulation; }
  .dn { min-width: 0; overflow-wrap: anywhere; font-size: 15px; text-decoration: underline; text-decoration-color: var(--tx3); text-underline-offset: 3px; }
  .ds { flex: none; font: 12.5px var(--mono); color: var(--tx2); }
  .doc:focus-visible { outline: 2px solid var(--tx); outline-offset: 2px; border-radius: 8px; }
  @media (hover: hover) and (pointer: fine) { .doc:hover .dn { text-decoration-color: var(--tx); } }
  @container (min-width: 620px) { .pics.many { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
</style>
