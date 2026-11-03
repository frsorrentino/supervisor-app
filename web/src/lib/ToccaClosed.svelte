<script lang="ts">
  // Le voci uscite da sole da Tocca a te oggi (app-pulizia-tocca-03-11, contratto 1.113): una riga grigia in fondo alla categoria,
  // un tocco apre l'elenco, solo da guardare. Niente da decidere: le chiude il registro con le sue regole.
  import type { ToccaState } from './contract'
  import { t } from './t'

  let { today }: { today: NonNullable<ToccaState['today']> } = $props()
  let open = $state(false)
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  const more = $derived(today.n - today.items.length)
</script>

<div class="tc" data-testid="tocca-chiuse-oggi">
  <button class="row" aria-expanded={open} onclick={() => (open = !open)}>
    <span class="ms ic" aria-hidden="true">cleaning_services</span>
    <span class="tx">{t.toccaClosedToday(today.n)}</span>
    <span class="ms arr" class:up={open} aria-hidden="true">expand_more</span>
  </button>
  {#if open}
    <ul>
      {#each today.items as x (x.task + x.kind + x.at)}
        <li>
          <span class="what"><b>{x.short || x.task}</b> · {t.toccaKind[x.kind] ?? x.kind}</span>
          <span class="why">{t.toccaRule[x.rule] ?? ''}{t.toccaRule[x.rule] ? ' · ' : ''}{hm(x.at)}</span>
        </li>
      {/each}
      {#if more > 0}<li class="more">{t.toccaMore(more)}</li>{/if}
    </ul>
  {/if}
</div>

<style>
  .tc { color: var(--text2); }
  .row { display: flex; align-items: center; gap: 12px; width: 100%; min-height: 44px; padding: 8px 12px; background: none; border: 0; color: inherit; font: inherit; font-size: 14px; text-align: left; cursor: pointer; }
  .ic { font-size: 20px; width: 24px; text-align: center; }
  .tx { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: clip; }
  .arr { font-size: 20px; transition: transform .2s; }
  .arr.up { transform: rotate(180deg); }
  ul { list-style: none; margin: 0; padding: 0 12px 8px 48px; }
  li { display: flex; flex-direction: column; gap: 2px; padding: 6px 0; font-size: 13px; border-top: 1px solid var(--line); }
  li:first-child { border-top: 0; }
  .what { color: var(--text); }
  .what b { font-weight: 500; }
  .more { color: var(--text2); }
  @media (prefers-reduced-motion: reduce) { .arr { transition: none; } }
</style>
