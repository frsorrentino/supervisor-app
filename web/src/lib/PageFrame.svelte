<script lang="ts">
  // Il riquadro di una pagina HTML o SVG del visore, nel feed e a schermo intero (app-visore-web-script-28-11): sul sito delle anteprime,
  // dove gli script e i font della pagina girano; se il sito non dice «pronta» entro PREVIEW_WAIT_MS, lo srcdoc di prima (PAGE_SANDBOX,
  // origine opaca), dove la CSP dell'app ferma gli script.
  import { PAGE_SANDBOX } from './fileActions'
  import { isReady, PREVIEW_ORIGIN, PREVIEW_SANDBOX, PREVIEW_WAIT_MS } from './anteprime'
  let { doc, title, full = false, height = null }: { doc: string; title: string; full?: boolean; height?: number | null } = $props()
  let frame: HTMLIFrameElement | null = $state(null)
  let fallback = $state(!PREVIEW_ORIGIN)
  $effect(() => {
    const f = frame, html = doc
    if (fallback || !f) return
    let sent = false
    const t = setTimeout(() => { if (!sent) fallback = true }, PREVIEW_WAIT_MS)
    const on = (e: MessageEvent) => {
      if (sent || e.source !== f.contentWindow || !isReady(e)) return
      sent = true
      clearTimeout(t)
      f.contentWindow?.postMessage({ html }, PREVIEW_ORIGIN)
    }
    addEventListener('message', on)
    return () => { clearTimeout(t); removeEventListener('message', on) }
  })
</script>

{#if fallback}
  <iframe class:full {title} style:height={height != null ? `${height}px` : null} sandbox={PAGE_SANDBOX} referrerpolicy="no-referrer" srcdoc={doc}></iframe>
{:else}
  {#key doc}<iframe bind:this={frame} class:full {title} style:height={height != null ? `${height}px` : null} sandbox={PREVIEW_SANDBOX} referrerpolicy="no-referrer" src={`${PREVIEW_ORIGIN}/`} data-testid="anteprima"></iframe>{/key}
{/if}

<style>
  iframe { display: block; width: 100%; border: 1px solid var(--line); border-radius: 14px; background: #fff; }
  iframe.full { flex: 1; border: 0; border-radius: 0; }
</style>
