<script lang="ts">
  import type { Hunk } from './contract'
  import { gutter, MINUS, rows } from './diff'
  import { t } from './t'
  // Il diff aperto (contratto 1.72, DiffBox in DiffViews.kt): i numeri di riga fermi a sinistra (vecchio e nuovo), le righe
  // tolte in rosso e aggiunte in verde, carattere a larghezza fissa; le righe non vanno a capo e scorre di lato il riquadro,
  // mai la pagina. I numeri non si copiano con il testo.
  let { hunks }: { hunks: Hunk[] } = $props()
  const rs = $derived(rows(hunks))
  const w = $derived(gutter(rs))
</script>

<div class="dbox" style="--g:{w}">
  <div class="grid">
    {#each rs as r, i (i)}
      {#if r.kind === 'gap'}<div class="gap"></div>
      {:else}
        <div class="ln {r.kind}">
          <span class="n o">{r.old ?? ''}</span><span class="n w">{r.new ?? ''}</span>
          <span class="tx">{#if r.kind === 'added'}<b class="sg">+</b>{:else if r.kind === 'removed'}<b class="sg">{MINUS}</b>{:else if r.kind === 'same'}<b class="sg"> </b>{/if}{r.text}{#if r.more}<i class="more">{t.diffMore(r.more)}</i>{/if}</span>
        </div>
      {/if}
    {/each}
  </div>
</div>

<style>
  .dbox { max-width: 100%; min-width: 0; overflow-x: auto; overscroll-behavior-x: contain; border-radius: 12px; background: var(--low); border: 1px solid var(--line); padding: 6px 0; }
  .grid { width: max-content; min-width: 100%; font: 12.5px/1.5 var(--mono); }
  .ln { display: flex; white-space: pre; }
  .n { position: sticky; flex: none; width: calc(var(--g) * 1ch + 10px); padding-right: 6px; text-align: right; color: var(--text2); background: var(--low); user-select: none; }
  .o { left: 0; }
  .w { left: calc(var(--g) * 1ch + 10px); }
  .tx { flex: 1; padding: 0 12px 0 6px; color: var(--text); }
  .sg { font-weight: 600; }
  .added { background: color-mix(in srgb, var(--b-good) 14%, transparent); }
  .added .n { background: color-mix(in srgb, var(--b-good) 14%, var(--low)); }
  .added .sg { color: var(--b-good); }
  .removed { background: color-mix(in srgb, var(--b-alert) 14%, transparent); }
  .removed .n { background: color-mix(in srgb, var(--b-alert) 14%, var(--low)); }
  .removed .sg { color: var(--b-alert); }
  .note .tx { color: var(--text2); font-style: italic; }
  .gap { border-top: 1px dashed var(--line); margin: 6px 0; }
  .more { font-style: normal; color: var(--text2); margin-left: 12px; }
</style>
