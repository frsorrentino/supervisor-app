<script lang="ts">
  import { catalog, grouped, tappable } from './features'
  import { lang } from './lang'
  import { t } from './t'
  // «Cosa sa fare» (Franz 01/11 20:39), porta di FeaturesScreen.kt: tutte le funzioni, per a cosa servono.
  let { onRoute }: { onRoute: (route: string) => void } = $props()
  const it = lang === 'it'
</script>

<div class="fx" data-testid="funzioni">
  {#each grouped(catalog) as { group, rows } (group.key)}
    <h3>{it ? group.it : group.en}</h3>
    <div class="card">
      {#each rows as f (f.key)}
        <button class="row" disabled={!tappable(f)} onclick={() => onRoute(f.route!)}>
          <span class="bd">
            <span class="hd"><b>{it ? f.name.it : f.name.en}</b>{#if f.state === 'coming'}<small>{t.featuresComing}</small>{/if}</span>
            <span class="tx">{it ? f.what.it : f.what.en}</span>
            {#if it ? f.where.it : f.where.en}<span class="wh">{it ? f.where.it : f.where.en}</span>{/if}
          </span>
          {#if tappable(f)}<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="var(--text2)" stroke-width="2.2" stroke-linecap="round" /></svg>{/if}
        </button>
      {/each}
    </div>
  {/each}
</div>

<style>
  .fx { padding: 0 12px 24px; max-width: 720px; }
  h3 { margin: 16px 4px 6px; font-size: 14px; font-weight: 600; color: var(--text2); }
  .card { background: var(--low); border-radius: 20px; overflow: hidden; }
  .row { display: flex; align-items: center; gap: 8px; width: 100%; padding: 10px 14px; text-align: left; color: var(--text); border-top: 1px solid var(--line); }
  .row:first-child { border-top: 0; }
  .row:disabled { cursor: default; color: var(--text); }
  .bd { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .hd { display: flex; align-items: baseline; gap: 8px; }
  .hd b { font-weight: 600; font-size: 15.5px; }
  .hd small { font: 12px var(--mono); color: var(--wait); white-space: nowrap; }
  .tx { font-size: 14.5px; color: var(--text2); overflow-wrap: anywhere; }
  .wh { font: 12px var(--mono); color: var(--text2); overflow-wrap: anywhere; }
</style>
