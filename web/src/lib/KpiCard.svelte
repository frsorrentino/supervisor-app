<script lang="ts">
  import type { Snippet } from 'svelte'
  // La card delle viste KPI 9, 13 e 17: il titolo in mono e la cifra di sintesi a destra; se non c'è posto va sotto.
  let { kpi, title, right, children }: { kpi: string; title: string; right?: Snippet; children: Snippet } = $props()
</script>

<div class="kc" data-kpi={kpi}>
  <div class="head"><span>{title}</span>{#if right}<b>{@render right()}</b>{/if}</div>
  {@render children()}
</div>

<style>
  .kc { background: var(--low); border-radius: 16px; padding: 12px 14px; display: flex; flex-direction: column; gap: 8px; min-width: 0; }
  .head { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 4px 12px; font: 500 11px/1.3 var(--mono); color: var(--text2); letter-spacing: .06em; }
  .head span { text-transform: uppercase; min-width: 0; overflow-wrap: anywhere; }
  .head b { font: 400 12px/1.3 var(--mono); letter-spacing: 0; color: var(--text); }
</style>
