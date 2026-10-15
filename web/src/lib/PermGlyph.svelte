<script lang="ts">
  import type { Glyph } from './permissionModes'
  // Il segno della modalità dei permessi, come nel terminale di Claude Code: ⏸, ⏵⏵ (ambra senza conferme), ?.
  let { g, size = 14 }: { g: Glyph; size?: number } = $props()
</script>

{#if g !== 'none'}
  <svg class="pg" class:amber={g === 'bypass'} viewBox="0 0 24 24" width={size} height={size} aria-hidden="true">
    {#if g === 'plan'}<path d="M7 5h3.5v14H7zM13.5 5H17v14h-3.5z" fill="currentColor" />
    {:else if g === 'unknown'}<circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2" /><path d="M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.7.3-1 .8-1 1.5v.7" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /><circle cx="12" cy="17" r="1.2" fill="currentColor" />
    {:else}<path d="M3 6l9 6-9 6zM12 6l9 6-9 6z" fill="currentColor" />{/if}
  </svg>
{/if}

<style>
  .pg { flex: none; vertical-align: -2px; }
  .pg.amber { color: var(--b-warn); }
</style>
