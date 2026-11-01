<script lang="ts">
  import { canGo, catalog, grouped, type Feature } from './features'
  import { lang } from './lang'
  import { t } from './t'
  import FeatureDemo, { DEMO_KEYS } from './FeatureDemo.svelte'
  // «Cosa sa fare» (Franz 01/11 20:39), porta di FeaturesScreen.kt. Ogni voce apre la sua pagina (22:05: tornare alla home non spiegava niente):
  // cosa fa e quando serve, come si usa, e «Portami lì» se ha un posto suo.
  let { onRoute }: { onRoute: (route: string) => void } = $props()
  const it = lang === 'it'
  const tx = (x: { it: string; en: string }) => (it ? x.it : x.en)
  let opened = $state<Feature | null>(null)
</script>

{#if opened}
  <div class="fx page" data-testid="funzione-{opened.key}">
    <button class="back" onclick={() => (opened = null)} aria-label={t.back}><svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path d="M15 6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" /></svg><span>{t.featuresTitle}</span></button>
    <h2>{tx(opened.name)}{#if opened.state === 'coming'} <small>{t.featuresComing}</small>{/if}</h2>
    <p class="about">{tx(opened.what)} {tx(opened.when)}</p>
    {#if DEMO_KEYS.includes(opened.key)}<h4>{t.featuresTry}</h4><FeatureDemo key={opened.key} />{/if}
    {#if opened.image}<h4>{t.featuresLook}</h4><img class="look" src="/feature-images/{opened.image}" alt={tx(opened.name)} loading="lazy" />{/if}
    {#if tx(opened.how)}<h4>{t.featuresHow}</h4><p class="how">{tx(opened.how)}</p>{/if}
    {#if tx(opened.where)}<p class="wh">{tx(opened.where)}</p>{/if}
    {#if canGo(opened)}<button class="go" onclick={() => onRoute(opened!.route!)}>{t.featuresGo}</button>{/if}
  </div>
{:else}
<div class="fx" data-testid="funzioni">
  {#each grouped(catalog) as { group, rows } (group.key)}
    <h3>{it ? group.it : group.en}</h3>
    <div class="card">
      {#each rows as f (f.key)}
        <button class="row" onclick={() => (opened = f)}>
          <span class="bd">
            <span class="hd"><b>{tx(f.name)}</b>{#if f.state === 'coming'}<small>{t.featuresComing}</small>{/if}</span>
            <span class="tx">{tx(f.what)}</span>
            {#if tx(f.where)}<span class="wh">{tx(f.where)}</span>{/if}
          </span>
          <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="var(--text2)" stroke-width="2.2" stroke-linecap="round" /></svg>
        </button>
      {/each}
    </div>
  {/each}
</div>
{/if}

<style>
  .fx { padding: 0 12px 24px; max-width: 720px; }
  h3 { margin: 16px 4px 6px; font-size: 14px; font-weight: 600; color: var(--text2); }
  .card { background: var(--low); border-radius: 20px; overflow: hidden; }
  .row { display: flex; align-items: center; gap: 8px; width: 100%; padding: 10px 14px; text-align: left; color: var(--text); border-top: 1px solid var(--line); }
  .row:first-child { border-top: 0; }
  .bd { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .hd { display: flex; align-items: baseline; gap: 8px; }
  .hd b { font-weight: 600; font-size: 15.5px; }
  small { font: 12px var(--mono); color: var(--wait); white-space: nowrap; }
  .tx { font-size: 14.5px; color: var(--text2); overflow-wrap: anywhere; }
  .wh { font: 12px var(--mono); color: var(--text2); overflow-wrap: anywhere; }
  .page { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }
  .back { display: flex; align-items: center; gap: 4px; color: var(--text2); font-size: 14px; padding: 6px 0; }
  h2 { font-size: 22px; font-weight: 600; color: var(--text); }
  h2 small { font-weight: 400; }
  .about, .how { font-size: 15.5px; line-height: 1.4; color: var(--text); }
  h4 { margin-top: 4px; font: 500 12px var(--mono); letter-spacing: .1em; text-transform: uppercase; color: var(--text2); }
  .how { margin-top: -6px; font-size: 14.5px; }
  .look { max-width: 100%; max-height: 520px; border: 1px solid var(--line); border-radius: 12px; }
  .go { background: var(--high); color: var(--text); border-radius: 999px; padding: 9px 16px; font-weight: 500; font-size: 14px; }
</style>
