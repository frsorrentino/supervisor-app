<script lang="ts">
  import { onMount } from 'svelte'
  import { pageDoc, PAGE_SANDBOX } from './fileActions'
  import { t } from './t'
  // Una pagina HTML (o un SVG) a tutto schermo nel riquadro isolato di PAGE_SANDBOX: «Schermo intero» di un HTML in chat (Franz 24/10
  // 00:40) e, da app-anteprime-webp-27-11, la bozza dal vivo che il tocco su un'anteprima animata apre. Finché la bozza non arriva dal
  // PC lo dice; se non arriva c'è il tasto per richiederla. Nel livello più alto della pagina (showModal), come il visore delle immagini.
  let { name, page, missed = false, onRetry = null, onClose }: {
    name: string; page: { mime: string; text: string } | null | undefined
    missed?: boolean; onRetry?: (() => void) | null; onClose: () => void
  } = $props()
  let dlg: HTMLDialogElement | undefined = $state()
  onMount(() => { dlg?.showModal(); return () => dlg?.close() })
</script>

<dialog class="pagefull" bind:this={dlg} aria-label={name} data-testid="pagina-intera" oncancel={(e) => { e.preventDefault(); onClose() }}>
  <div class="pfbar">
    <span class="pfname">{name}</span>
    <button class="pfclose" aria-label={t.closeWord} onclick={onClose}><svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg></button>
  </div>
  {#if page}
    <iframe class="pfframe" title={name} sandbox={PAGE_SANDBOX} referrerpolicy="no-referrer" srcdoc={pageDoc(page.mime, page.text)}></iframe>
  {:else}
    <div class="pfwait" role="status">
      {#if missed}
        <span>{t.previewMissed}</span>
        {#if onRetry}<button class="again" onclick={onRetry}>{t.previewRetry}</button>{/if}
      {:else}<span>{t.previewComing}</span>{/if}
    </div>
  {/if}
</dialog>

<style>
  .pagefull { position: fixed; inset: 0; width: 100vw; height: 100dvh; max-width: none; max-height: none; margin: 0; padding: 0; border: 0; background: #000; color: var(--text); display: flex; flex-direction: column; }
  .pagefull::backdrop { background: #000; }
  .pfbar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; padding-top: calc(8px + env(safe-area-inset-top, 0px)); color: var(--text2); font-size: 14px; }
  .pfname { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; }
  .pfclose { flex: none; width: 44px; height: 44px; border-radius: 50%; background: var(--high); color: var(--text); display: grid; place-items: center; touch-action: manipulation; }
  .pfclose:focus-visible, .again:focus-visible { outline: 2px solid #fff; outline-offset: 2px; }
  .pfframe { flex: 1; width: 100%; border: 0; background: #fff; }
  .pfwait { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; padding: 16px; text-align: center; font-size: 14px; color: var(--text2); }
  .again { min-height: 40px; padding: 0 16px; border-radius: 999px; background: var(--high); color: var(--text); font: 600 14px var(--display, inherit); touch-action: manipulation; }
</style>
