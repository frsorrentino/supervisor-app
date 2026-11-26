<script lang="ts">
  import { t } from './t'
  import { LINES } from './citazione'
  // Una citazione (app-citazioni-estetica-26-11, Franz 26/11 15:46): barra a sinistra, testo più piccolo e tenue, staccata dal resto;
  // lunga si ferma a tre righe con «altro», che la apre.
  let { text }: { text: string } = $props()
  let open = $state(false)
  let el: HTMLParagraphElement | undefined = $state()
  let over = $state(false)
  $effect(() => { void text; if (el && !open) over = el.scrollHeight > el.clientHeight + 1 })
</script>

<blockquote class="cq" data-testid="citazione">
  <p bind:this={el} class:clamp={!open} style:--lines={LINES}>{text}</p>
  {#if over && !open}<button class="more" onclick={(e) => { e.stopPropagation(); open = true }}>{t.quoteMore}</button>{/if}
</blockquote>

<style>
  .cq { margin: 2px 0 8px; padding: 2px 0 2px 10px; border-left: 3px solid color-mix(in srgb, var(--tx) 28%, transparent); }
  p { margin: 0; font-size: 14px; line-height: 20px; color: var(--tx2); white-space: pre-line; overflow-wrap: anywhere; }
  p.clamp { display: -webkit-box; -webkit-line-clamp: var(--lines); line-clamp: var(--lines); -webkit-box-orient: vertical; overflow: hidden; }
  .more { margin-top: 2px; padding: 2px 0; font-size: 13.5px; color: var(--tx); text-decoration: underline; text-decoration-color: var(--tx3); text-underline-offset: 3px; }
</style>
