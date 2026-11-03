<script lang="ts">
  import type { Snippet } from 'svelte'
  import { slide } from 'svelte/transition'
  import { rightText, shown, type Row } from './itemRows'
  import { t } from './t'
  // Le voci dentro le sezioni come righe di sessione (app-richiede-te-righe-02-11, mockup richiede-te.png punto 4), porta di
  // ItemList in ItemRowsUi.kt: pallino, nome, dato a destra, sotto cosa c'è; le prime tre e «altre N»; il tocco apre la scheda.
  let { rows, testid, right = rightText, name = (r: Row) => r.name, card }: { rows: Row[]; testid: string; right?: (r: Row) => string; name?: (r: Row) => string; card: Snippet<[Row]> } = $props()
  let openKey = $state<string | null>(null)
  let all = $state(false)
  const n = $derived(shown(rows.length, all))
  const reduced = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches
</script>

{#if rows.length}
  <div class="il" data-testid={testid}>
    {#each rows.slice(0, n) as r (r.key)}
      {@const o = openKey === r.key}
      <div class="ir">
        <button class="row" aria-expanded={o} data-testid={`${testid}-${r.key}`} onclick={() => (openKey = o ? null : r.key)}>
          <span class="dot {r.tone}"></span>
          <span class="tx"><span class="top"><b>{name(r)}</b>{#if right(r)}<span class="rt">{right(r)}</span>{/if}</span>{#if !o && r.sub}<span class="sub">{r.sub}</span>{/if}</span>
        </button>
        {#if o}<div class="card" transition:slide={{ duration: reduced ? 0 : 200 }}>{@render card(r)}</div>{/if}
      </div>
    {/each}
    {#if n < rows.length}<button class="more" data-testid={`${testid}-altre`} onclick={() => (all = true)}>{t.itemMore(rows.length - n)}</button>{/if}
  </div>
{/if}

<style>
  .il { border-radius: 14px; background: var(--s1); overflow: hidden; }
  .ir + .ir, .more { border-top: 1px solid var(--line); }
  .row { display: flex; align-items: flex-start; width: 100%; min-height: 52px; padding: 10px 12px 10px 0; text-align: left; cursor: pointer; }
  .dot { flex: none; width: 8px; height: 8px; border-radius: 50%; margin: 7px 22px 0; background: var(--tx3); }
  .dot.wait { background: var(--wait); } .dot.busy { background: var(--busy); } .dot.good { background: var(--good); } .dot.red { background: var(--red); }
  .tx { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  /* Nome e dato: il dato a destra se ci sta, altrimenti va a capo intero sotto il nome, mai in colonna. */
  .top { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; column-gap: 8px; }
  .top b { font: 600 17px/20px var(--display); color: var(--tx); }
  .rt { margin-left: auto; font: 12.5px/17px var(--mono); color: var(--tx2); text-align: right; }
  /* Due righe al più, tagliate senza «…» (regole di forma, Franz 12/09): il testo intero sta nella scheda. */
  .sub { font: 14px/19px var(--sans, inherit); color: var(--tx2); display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; text-overflow: clip; }
  .card { padding: 0 12px 12px; }
  .more { display: block; width: 100%; min-height: 44px; font: 14px var(--sans, inherit); color: var(--tx2); cursor: pointer; }
</style>
