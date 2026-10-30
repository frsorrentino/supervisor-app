<script lang="ts">
  import type { StepDiff } from './contract'
  import { name, shown, type Load } from './diff'
  import DiffBox from './DiffBox.svelte'
  import DiffLine from './DiffLine.svelte'
  import { t } from './t'
  // Sotto un passaggio di modifica (contratto 1.72, StepDiffRow in DiffViews.kt): la riga chiusa; il tocco apre il diff; con
  // `cut` l'ultima riga è «Mostra tutto», che chiede il passaggio intero con l'op `diff` (fino a 5000 righe).
  let { id, diff, onDiff = null }: { id: string; diff: StepDiff; onDiff?: ((id: string) => Promise<Load<StepDiff>>) | null } = $props()
  let open = $state(false)
  let wantAll = $state(false)
  let full = $state<Load<StepDiff> | null>(null)
  let busy = $state(false)
  const sh = $derived(shown(diff, full?.ok ? full.value : null, wantAll, busy || (full != null && !full.ok)))
  $effect(() => {
    if (!sh.ask || !onDiff) return
    busy = true
    onDiff(id).then(r => { full = r; busy = false })
  })
</script>

<div class="sd v3">
  <DiffLine name={name(diff.path)} added={diff.added} removed={diff.removed} {open} onToggle={() => (open = !open)} />
  {#if open}
    {#if sh.diff.hunks.length}<DiffBox hunks={sh.diff.hunks} />{:else}<p class="note">{t.diffHidden.empty}</p>{/if}
    {#if sh.showAll && onDiff}<button class="all" onclick={() => (wantAll = true)}>{t.diffShowAll}</button>
    {:else if busy}<p class="note">{t.diffLoading}</p>
    {:else if full && !full.ok}<p class="note bad"><span>{full.why === 'down' ? t.diffDown : full.why === 'git' ? t.diffGit : full.text}</span><button class="all" onclick={() => (full = null)}>{t.diffRetry}</button></p>
    {:else if sh.beyond}<p class="note">{t.diffBeyond}</p>{/if}
  {/if}
</div>

<style>
  /* I blocchi dei file nello stile v3 (Franz 30/10 21:19): .v3 porta i neutri, il fondo resta quello di chi lo contiene. */
  .sd { display: flex; flex-direction: column; gap: 6px; min-width: 0; background: none; }
  .note { font-size: 13.5px; color: var(--text2); padding: 0 4px; }
  .note.bad { color: var(--gone-dim); display: flex; align-items: center; gap: 8px; }
  .note.bad span { flex: 1; }
  .all { align-self: flex-start; padding: 8px 14px; border-radius: 999px; background: var(--high); color: var(--icon); font-size: 14px; font-weight: 500; }
  .all:hover { filter: brightness(1.15); }
</style>
