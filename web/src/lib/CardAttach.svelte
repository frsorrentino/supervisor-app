<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { State } from './contract'
  import Icon from './Icon.svelte'
  import { growFromPress } from './motion'
  import { MAX_ALLEGATI, type Allegato } from './allegatiCarta'
  import { t } from './t'
  // app-allegati-carta-27-11 (Franz 27/11 20:47, su una carta del Banco prova: «questo campo di testo dovrebbe avere la possibilità di
  // caricare allegati come nelle sessioni»): accanto al campo di risposta di una carta il + del campo delle sessioni (Composer), con le
  // stesse voci (galleria, foto, file solo con `share.any`), lo stesso tetto di cinque e la stessa anteprima con la ×; anche incollati
  // nel campo. `share` null (un relay che non li accetta, o niente da dove mandarli): solo il campo. Il menu si apre verso il basso:
  // la carta sta in una pagina che scorre, e verso l'alto finirebbe sotto la testata. Sotto, le parole della chat: «carico l'allegato»
  // mentre partono, «non consegnato» col motivo se non arrivano.
  let { files = $bindable([]), share, busy = false, failed = null, children }: {
    files?: Allegato[]; share: State['share'] | null | undefined; busy?: boolean; failed?: string | null; children: Snippet
  } = $props()
  const images = $derived(!!share)
  const any = $derived(!!share?.any)
  let menu = $state(false)
  let gallery: HTMLInputElement | undefined = $state()
  let camera: HTMLInputElement | undefined = $state()
  let other: HTMLInputElement | undefined = $state()

  function add(list: File[]): number {
    const ok = list.filter(f => (f.type.startsWith('image/') ? images : any))
    if (!ok.length || busy) return 0
    files = [...files, ...ok.map(file => ({ file, url: file.type.startsWith('image/') ? URL.createObjectURL(file) : null }))].slice(0, MAX_ALLEGATI)
    return ok.length
  }
  function picked(e: Event) {
    add([...((e.currentTarget as HTMLInputElement).files ?? [])])
    ;(e.currentTarget as HTMLInputElement).value = ''
  }
  function paste(e: ClipboardEvent) {
    const list = [...(e.clipboardData?.files ?? [])]
    if (list.length && add(list)) e.preventDefault()
  }
  function drop(i: number) { const f = files[i]; if (f?.url) URL.revokeObjectURL(f.url); files = files.filter((_, j) => j !== i) }
</script>

{#if files.length}
  <div class="thumbs" data-testid="carta-allegati">
    {#each files as f, i (f)}
      <div class="thumb">
        {#if f.url}<img src={f.url} alt={f.file.name} />{:else}<span class="fname"><Icon name="file" color="var(--icon)" />{f.file.name}</span>{/if}
        <button type="button" class="x" aria-label={t.removeImage} disabled={busy} onclick={() => drop(i)}><svg viewBox="0 0 24 24" width="16" height="16"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" /></svg></button>
      </div>
    {/each}
  </div>
{/if}
<div class="row" onpaste={paste}>
  {#if images}
    <span class="anchor">
      <button type="button" class="plus" aria-label={t.attach} aria-expanded={menu} disabled={busy} data-testid="carta-allega" onclick={() => (menu = !menu)}>
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M12 5v14M5 12h14" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" /></svg>
      </button>
      {#if menu}
        <button type="button" class="scrim" aria-label={t.closeWord} onclick={() => (menu = false)}></button>
        <div class="menu" transition:growFromPress role="menu">
          <button type="button" role="menuitem" onclick={() => { menu = false; gallery?.click() }}><Icon name="image" color="var(--icon)" size={22} />{t.attachGallery}</button>
          <button type="button" role="menuitem" onclick={() => { menu = false; camera?.click() }}><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3z" /><circle cx="12" cy="13" r="3" /></svg>{t.attachCamera}</button>
          {#if any}<button type="button" role="menuitem" onclick={() => { menu = false; other?.click() }}><Icon name="file" color="var(--icon)" size={22} />{t.attachFile}</button>{/if}
        </div>
      {/if}
    </span>
    <input bind:this={gallery} type="file" accept="image/*" multiple hidden onchange={picked} />
    <input bind:this={camera} type="file" accept="image/*" capture="environment" hidden onchange={picked} />
    <input bind:this={other} type="file" multiple hidden onchange={picked} />
  {/if}
  <div class="slot">{@render children()}</div>
</div>
{#if busy}
  <p class="st" role="status" data-testid="carta-allegati-carico"><Icon name="upload" color="var(--text2)" size={16} />{t.chatStatus.uploading('')}</p>
{:else if failed}
  <p class="st bad" role="alert" data-testid="carta-allegati-errore"><Icon name="error" color="var(--gone)" size={16} /><span>{t.chatStatus.failed('')} · {failed}</span></p>
{/if}

<style>
  /* Le anteprime e il + come nel campo delle sessioni (Composer). */
  .thumbs { display: flex; gap: 8px; overflow-x: auto; }
  .thumb { position: relative; flex: none; }
  .thumb img { width: 72px; height: 72px; object-fit: cover; border-radius: 14px; display: block; }
  .fname { display: flex; align-items: center; gap: 6px; height: 72px; padding: 0 30px 0 12px; border-radius: 14px; background: var(--surface); font-size: 12.5px; max-width: 220px; overflow-wrap: anywhere; }
  .x { position: absolute; top: 2px; right: 2px; width: 26px; height: 26px; border-radius: 50%; background: rgb(0 0 0 / .6); color: #fff; display: grid; place-items: center; }
  .row { display: flex; align-items: flex-start; gap: 4px; }
  .slot { flex: 1; min-width: 0; display: flex; flex-direction: column; }
  .anchor { position: relative; flex: none; }
  .plus { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; }
  .plus:hover:not(:disabled) { background: var(--surface); }
  .plus:disabled { opacity: .45; cursor: default; }
  .scrim { position: fixed; inset: 0; z-index: 9; cursor: default; }
  .menu { position: absolute; top: 48px; left: 0; z-index: 10; min-width: 240px; background: var(--surface); border-radius: 12px; padding: 8px 0; box-shadow: 0 12px 40px rgb(0 0 0 / .5); border: 1px solid var(--line); }
  .menu button { display: flex; align-items: center; gap: 12px; width: 100%; padding: 12px 16px; font-size: 15px; text-align: left; }
  .menu button:hover { background: var(--high); }
  /* Lo stato degli allegati con le parole e l'icona della chat. */
  .st { display: flex; align-items: center; gap: 6px; font-size: 13.5px; line-height: 19px; color: var(--text2); }
  .st.bad { align-items: flex-start; color: var(--gone); }
  .st.bad :global(svg) { margin-top: 1.5px; }
  .st span { min-width: 0; overflow-wrap: anywhere; }
</style>
