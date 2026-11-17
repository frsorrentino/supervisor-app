<script lang="ts">
  import type { Pagella } from './conti'
  import { modelName, pagellaRows, ratePct } from './conti'
  import { locale } from './lang'
  import { t } from './t'
  // app-rendiconti-2-02-11 (contratto 1.103): la pagella in Utilizzo, per modello e tipo di lavoro, solo le righe da `min` casi (10)
  // in su; le altre non si mostrano. I consigli del relay sotto, come li scrive.
  let { pagella }: { pagella: Pagella } = $props()
  const rows = $derived(pagellaRows(pagella))
  const since = $derived(new Date(pagella.since * 1000).toLocaleDateString(locale, { day: 'numeric', month: 'numeric' }))
</script>

<div class="pag" data-testid="pagella">
  <p class="dim">{t.pagellaSince(since, pagella.min)}</p>
  {#each rows as r (r.model + r.kind)}
    <div class="row">
      <span class="nm">{modelName(r.model)}</span><span class="k">{t.pagellaKind[r.kind] ?? r.kind}</span>
      <span class="v">{t.pagellaCases(r.n)}{#if ratePct(r) != null}{' · '}<b class:bad={ratePct(r)! >= 20}>{t.pagellaDefects(ratePct(r)!)}</b>{/if}</span>
    </div>
  {/each}
  {#each pagella.advice ?? [] as a}<p class="adv">{a}</p>{/each}
</div>

<style>
  .pag { display: flex; flex-direction: column; padding: 4px 0 8px; }
  .dim { color: var(--tx2); font-size: 14px; padding: 0 14px 6px; }
  .row { display: flex; align-items: baseline; gap: 10px; min-height: 40px; padding: 8px 14px; border-top: 1px solid var(--line); }
  .nm { font: 600 15px var(--display); flex: none; }
  .k { color: var(--tx2); font-size: 14px; flex: 1; min-width: 0; }
  .v { font: 500 14px var(--mono); color: var(--tx); flex: none; }
  .v b { font-weight: 500; }
  .v b.bad { color: var(--wait); }
  .adv { font-size: 14px; color: var(--wait); padding: 6px 14px 0; }
</style>
