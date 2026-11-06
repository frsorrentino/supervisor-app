<script lang="ts">
  import type { Snippet } from 'svelte'
  import { slide } from 'svelte/transition'
  import { rightText, shown, type Row } from './itemRows'
  import { t } from './t'
  import { SUG_ICON } from './sugIcons'
  // Le voci dentro le sezioni come righe di sessione (app-richiede-te-righe-02-11, mockup richiede-te.png punto 4), porta di
  // ItemList in ItemRowsUi.kt: pallino, nome, dato a destra, sotto cosa c'è; le prime tre e «altre N»; il tocco apre la scheda.
  // `more`: quante cose dicono le righe nascoste in «altre N»; di solito le righe, per Decidi le decisioni dei gruppi (web-plancia-j2-ripasso-06-11).
  // `allOpen` (app-sessioni-compatte-06-11): dentro la scheda di una sessione tutte aperte, coi dettagli e i tasti.
  let { rows, testid, allOpen = false, right = rightText, name = (r: Row) => r.name, more = (rest: Row[]) => rest.length, card }: { rows: Row[]; testid: string; allOpen?: boolean; right?: (r: Row) => string; name?: (r: Row) => string; more?: (rest: Row[]) => number; card: Snippet<[Row]> } = $props()
  let openKey = $state<string | null>(null)
  let all = $state(false)
  const n = $derived(allOpen ? rows.length : shown(rows.length, all))
  const reduced = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches
</script>

{#if rows.length}
  <div class="il lit" data-testid={testid}>
    {#each rows.slice(0, n) as r (r.key)}
      {@const o = allOpen || openKey === r.key}
      <div class="ir" data-key={r.key}>
        <button class="row" aria-expanded={o} data-testid={`${testid}-${r.key}`} onclick={() => (openKey = o ? null : r.key)}>
          <!-- Il segno al centro della riga del titolo; per un suggerimento l'icona del suo tipo (app-suggerimenti-icone-03-11). -->
          <span class="lead {r.tone}">{#if r.icon}<span class="ms" aria-hidden="true">{SUG_ICON[r.icon] ?? SUG_ICON.other}</span>{:else}<span class="dot"></span>{/if}</span>
          <span class="tx"><span class="top"><b>{name(r)}</b>{#if right(r)}<span class="rt">{right(r)}</span>{/if}</span>{#if !o && r.sub}<span class="sub">{r.sub}</span>{/if}</span>
        </button>
        {#if o}<div class="card" transition:slide={{ duration: reduced ? 0 : 200 }}>{@render card(r)}</div>{/if}
      </div>
    {/each}
    {#if n < rows.length}<button class="more" data-testid={`${testid}-altre`} onclick={() => (all = true)}>{t.itemMore(more(rows.slice(n)))}</button>{/if}
  </div>
{/if}

<style>
  .il { border-radius: 14px; background: var(--s1); overflow: hidden; }
  .ir + .ir, .more { border-top: 1px solid var(--line); }
  .row { display: flex; align-items: flex-start; width: 100%; min-height: 52px; padding: 10px 12px 10px 0; text-align: left; cursor: pointer; }
  .lead { flex: none; width: 52px; height: 20px; display: flex; align-items: center; justify-content: center; color: var(--tx3); }
  .lead.wait { color: var(--wait); } .lead.busy { color: var(--busy); } .lead.good { color: var(--good); } .lead.red { color: var(--red); }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
  .lead .ms { font-size: 18px; }
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
