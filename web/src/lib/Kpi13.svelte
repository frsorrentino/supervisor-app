<script lang="ts">
  import type { KpiSuggestions } from './contract'
  import KpiCard from './KpiCard.svelte'
  import { acceptRate, suggestionsTotal } from './kpi'
  import { t } from './t'
  // Proposta 13 (25/10, contratto 1.96), sotto la card dei suggerimenti: come sono finiti quelli della settimana; le parole
  // sotto la barra sono le sue etichette, nello stesso colore del tratto.
  let { s }: { s: KpiSuggestions } = $props()
  const TONE: Record<string, string> = { accepted: 'var(--good)', discussed: 'var(--busy)', later: 'var(--b-week)', never: 'var(--stale)', expired: 'var(--b-track)' }
  const parts = $derived((['accepted', 'discussed', 'later', 'never', 'expired'] as const).map(k => ({ k, n: s[k] })).filter(p => p.n > 0))
  const total = $derived(suggestionsTotal(s))
  const rate = $derived(acceptRate(s))
</script>

<KpiCard kpi="kpi-13" title={t.kpi13Title}>
  {#snippet right()}{#if rate != null}<span class="g">{t.kpi13Rate(rate)}</span>{/if}{/snippet}
  {#if !total}<p>{t.kpi13None}</p>
  {:else}
    <div class="bar">{#each parts as p (p.k)}<i style:flex-grow={p.n} style:background={TONE[p.k]}></i>{/each}</div>
    <div class="names">{#each parts as p (p.k)}<span style:color={p.k === 'expired' ? 'var(--text2)' : TONE[p.k]}>{t.kpi13Parts[p.k](p.n)}</span>{/each}</div>
  {/if}
</KpiCard>

<style>
  .g { color: var(--good); }
  p { margin: 0; font-size: 12.5px; color: var(--text2); }
  .bar { display: flex; gap: 2px; height: 10px; }
  .bar i { display: block; flex-basis: 0; height: 100%; border-radius: 3px; }
  .names { display: flex; flex-wrap: wrap; gap: 4px 12px; font-size: 13px; }
</style>
