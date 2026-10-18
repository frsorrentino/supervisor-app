<script lang="ts">
  import type { Snippet } from 'svelte'
  import { t } from './t'
  // Una pagina del menu (Registro, Utilizzo e limiti, Cerca, Impostazioni, Lancia): ← e il titolo, sotto il contenuto che
  // scorre. Sul telefono prende lo schermo, sulla plancia il posto delle colonne, come il Registro sul tablet.
  // `fill`: il contenuto prende tutta l'altezza e scorre da sé, come la home col dock della master (pagina Studio).
  let { title, onBack, children, actions = null, fill = false }: { title: string; onBack: () => void; children: Snippet; actions?: Snippet | null; fill?: boolean } = $props()
</script>

<svelte:window onkeydown={(e) => e.key === 'Escape' && !document.querySelector('dialog[open]') && onBack()} />

<section class="page">
  <header>
    <button class="ib" aria-label={t.back} title={t.back} onclick={onBack}>
      <svg viewBox="0 0 24 24" width="24" height="24"><path d="M19 12H5M12 19l-7-7 7-7" fill="none" stroke="var(--text)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
    </button>
    <h1>{title}</h1>
    {#if actions}<span class="sp"></span>{@render actions()}{/if}
  </header>
  <div class="body" class:fill>{@render children()}</div>
</section>

<style>
  .page { height: 100%; display: flex; flex-direction: column; min-height: 0; background: var(--bg); }
  header { display: flex; align-items: center; gap: 4px; min-height: 56px; padding: 4px 8px; }
  h1 { font-size: 22px; font-weight: 400; }
  .sp { flex: 1; }
  .ib { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; }
  .ib:hover { background: var(--surface); }
  .body { flex: 1; min-height: 0; overflow-y: auto; }
  .body.fill { overflow: hidden; display: flex; flex-direction: column; }
</style>
