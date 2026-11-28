<script lang="ts">
  import { plays } from './anteprimeAnimate'
  import { movimento } from './riduciMovimento.svelte'
  // Un'anteprima che può muoversi (app-anteprime-webp-27-11, Franz 27/11 20:49: «ovunque ci siano delle anteprime che devono mostrare
  // un'animazione, queste siano animate in loop o a tocco»). Senza `blob` è l'immagine di sempre. Con `blob` (un WebP o una GIF che si
  // muovono davvero, mediaCache.moving) l'<img> gira in loop finché è a schermo; fuori schermo, o con «riduci movimento», al suo posto
  // c'è il primo fotogramma, disegnato in un canvas da createImageBitmap(blob), e con le animazioni ridotte il ▶ sopra. Il tocco lo
  // gestisce chi la contiene: apre l'immagine o la bozza a schermo intero, dove gira. `fill`: riempie la cornice di chi la contiene;
  // senza, alta quanto vuole l'immagine fino a --am-max.
  let { src, blob = null, alt = '', fit = 'contain', pos = 'center', fill = true, small = false, onsize = null }: {
    src: string; blob?: Blob | null; alt?: string; fit?: 'contain' | 'cover'; pos?: string; fill?: boolean; small?: boolean
    onsize?: ((w: number, h: number) => void) | null
  } = $props()
  let box: HTMLSpanElement | undefined = $state()
  let cv: HTMLCanvasElement | undefined = $state()
  // A schermo finché l'osservatore non dice il contrario: un'immagine fuori schermo non si disegna e non gira comunque.
  let visible = $state(true)
  const play = $derived(!blob || plays(visible, movimento.ridotto))
  $effect(() => {
    const el = box
    if (!el || !blob || typeof IntersectionObserver !== 'function') return
    const io = new IntersectionObserver(es => { visible = es[es.length - 1].isIntersecting })
    io.observe(el)
    return () => io.disconnect()
  })
  // Il primo fotogramma, solo mentre serve: tornata a schermo l'immagine riparte e il canvas se ne va.
  $effect(() => {
    const c = cv, b = blob
    if (!c || !b) return
    let live = true
    createImageBitmap(b).then(bmp => {
      if (live) {
        c.width = bmp.width; c.height = bmp.height
        c.getContext('2d')?.drawImage(bmp, 0, 0)
        onsize?.(bmp.width, bmp.height)
      }
      bmp.close()
    }).catch(() => {})
    return () => { live = false }
  })
  const loaded = (e: Event) => { const im = e.currentTarget as HTMLImageElement; if (im.naturalHeight > 0) onsize?.(im.naturalWidth, im.naturalHeight) }
</script>

<span class="am" class:fill class:auto={!fill} bind:this={box} data-testid={blob ? 'anteprima-animata' : undefined} data-gira={blob ? String(play) : undefined}>
  <img {src} alt={blob && !play ? '' : alt} draggable="false" class:off={!play} style:object-fit={fit} style:object-position={pos} onload={loaded} />
  {#if blob && !play}<canvas bind:this={cv} class="still" style:object-fit={fit} style:object-position={pos} role={alt ? 'img' : undefined} aria-label={alt || undefined} aria-hidden={alt ? undefined : 'true'}></canvas>{/if}
  {#if blob && movimento.ridotto}
    <span class="play" class:small aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg></span>
  {/if}
</span>

<style>
  .am { position: relative; display: block; width: 100%; }
  .am.fill { height: 100%; }
  img, canvas { display: block; width: 100%; user-select: none; -webkit-user-select: none; -webkit-touch-callout: none; }
  .fill img { height: 100%; }
  .auto img { height: auto; max-height: var(--am-max, none); }
  /* Ferma: l'immagine tiene il suo posto senza disegnarsi, il primo fotogramma la copre con la stessa forma. */
  img.off { visibility: hidden; }
  canvas { position: absolute; inset: 0; height: 100%; }
  /* Il ▶ delle anteprime ferme per le animazioni ridotte, come quello dei video: il tocco è dell'immagine intera. */
  .play { position: absolute; left: 50%; top: 50%; width: 52px; height: 52px; margin: -26px 0 0 -26px; border-radius: 50%; display: grid; place-items: center; background: rgb(0 0 0 / .55); color: #fff; pointer-events: none; }
  .play svg { width: 30px; height: 30px; }
  .play.small { width: 30px; height: 30px; margin: -15px 0 0 -15px; }
  .play.small svg { width: 18px; height: 18px; }
</style>
