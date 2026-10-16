<script lang="ts">
  import { MINUS } from './diff'
  import Icon from './Icon.svelte'
  import { t } from './t'
  // La riga chiusa di un diff (contratto 1.72, DiffLine in DiffViews.kt): «totals.ts · +2 −1»; nella vista «Modifiche» anche
  // la cartella sotto e l'etichetta del file. Il tocco apre e chiude.
  let { name, added, removed, open, onToggle, label = null, counts = true, folder = null }: {
    name: string; added: number; removed: number; open: boolean; onToggle: () => void; label?: string | null; counts?: boolean; folder?: string | null
  } = $props()
</script>

<button class="dline" aria-expanded={open} aria-label={open ? t.diffClose(name) : t.diffOpen(name)} onclick={onToggle}>
  <Icon name="diff" color="var(--icon)" />
  <span class="dt">
    <span class="dn">{name}{#if counts}<span class="sep">{' · '}</span><b class="plus">+{added}</b> <b class="minus">{MINUS}{removed}</b>{/if}</span>
    {#if folder}<span class="df">{folder}</span>{/if}
  </span>
  {#if label}<span class="lab">{label}</span>{/if}
  <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d={open ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
</button>

<style>
  .dline { display: flex; align-items: center; gap: 10px; width: 100%; padding: 10px 12px; border-radius: 12px; background: var(--surface); text-align: left; }
  .dline:hover { filter: brightness(1.15); }
  .dt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .dn { font-size: 14px; font-weight: 500; overflow-wrap: anywhere; }
  .sep { color: var(--text2); }
  .plus, .minus { font-weight: 500; }
  .plus { color: var(--b-good); }
  .minus { color: var(--b-alert); }
  .df { font: 12px/1.3 var(--mono); color: var(--text2); overflow-wrap: anywhere; }
  .lab { font: 500 11px/1 var(--mono); letter-spacing: .06em; color: var(--text2); background: var(--high); border-radius: 999px; padding: 4px 8px; white-space: nowrap; }
</style>
