<script lang="ts">
  // Le icone della home per categorie che il sottoinsieme di Material Symbols non ha (app-home-categorie-02-11): disegnate a tratto,
  // 24 × 24 come le altre, del colore del testo intorno.
  let { name, size = 22 }: { name: string; size?: number } = $props()
</script>

<svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
  {#if name === 'drag'}
    {#each [6, 12, 18] as y}<circle cx="9" cy={y} r="1.3" fill="currentColor" stroke="none" /><circle cx="15" cy={y} r="1.3" fill="currentColor" stroke="none" />{/each}
  {:else if name === 'inbox'}
    <path d="M4 13.5h4.2l1.3 2.5h5l1.3-2.5H20" /><path d="M6.2 5h11.6L20 13.5V19H4v-5.5z" />
  {:else if name === 'speed'}
    <path d="M4.5 17.5a8 8 0 1 1 15 0" /><path d="M12 15l4-5" />
  {:else if name === 'eye'}
    <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" /><circle cx="12" cy="12" r="3" />
  {:else if name === 'doc'}
    <path d="M6 3.5h8l4 4V20.5H6z" /><path d="M14 3.5v4h4M9 12h6M9 16h6" />
  {:else if name === 'medal'}
    <circle cx="12" cy="9" r="5" /><path d="M9 13.5L7.5 21l4.5-2.5 4.5 2.5-1.5-7.5" />
  {/if}
</svg>
