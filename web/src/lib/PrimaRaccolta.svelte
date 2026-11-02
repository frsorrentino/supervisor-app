<script lang="ts">
  import type { CmdResult, PrimaRaccoltaPage, PrimaRaccoltaSummary } from './contract'
  import { decodePrima } from './decideDecode'
  import { screen } from './decide'
  import { askConsent, canStart, emptySection, finished, firstRoundOffer } from './primaRaccolta'
  import { newCmd } from './transport'
  import { t } from './t'
  // «Prima raccolta» (contratto 1.100), porta di PrimaRaccoltaScreen: cosa si è trovato per ogni sezione; una vuota dice perché e quando si riempie.
  let { run, summary, onScreen, onFeatures }: { run: (c: ReturnType<typeof newCmd>) => Promise<CmdResult | null>; summary: PrimaRaccoltaSummary | null | undefined; onScreen: (s: string) => void; onFeatures: () => void } = $props()
  let page = $state<PrimaRaccoltaPage | null>(null)
  async function load() { const r = await run(newCmd('prima_raccolta', null)); if (r?.ok) page = decodePrima(r.text) }
  // La pagina segue la raccolta: ogni sezione finita cambia il riassunto nello stato.
  $effect(() => { void summary?.done; void summary?.status; void load() })
  async function touch(action: string, value?: string) { await run(newCmd('prima_raccolta_azione', null, null, undefined, undefined, { action, value })); await load() }
  const head = (p: PrimaRaccoltaPage) => p.status === 'running' ? t.primaRunning(p.done, p.total) : p.status === 'ask' ? t.primaAsk(p.done, p.total) : p.status === 'interrupted' ? t.primaInterrupted(p.done, p.total) : p.status === 'done' ? t.primaDone : t.primaNone
</script>

<div class="pr" data-testid="prima-raccolta">
  {#if !page}<p class="m">{t.proveLoading}</p>
  {:else}
    <p>{head(page)}</p>
    {#if page.cost === 0}<p class="m">{t.primaFree}</p>{/if}
    {#if canStart(page)}<button class="tonal wide" onclick={() => touch('start')}>{page.status === 'interrupted' ? t.primaResume : t.primaStart}</button>{/if}
    {#if askConsent(page)}
      <div class="c"><p>{page.ask_text}</p><div class="ac"><span class="sp"></span><button class="out" onclick={() => touch('consent', 'no')}>{t.primaNo}</button><button class="tonal" onclick={() => touch('consent', 'si')}>{t.primaYes}</button></div></div>
    {/if}
    {#each page.sections as s (s.key)}
      <div class="c" data-testid={`prima-${s.key}`}>
        <b>{t.primaSections[s.key] ?? s.key}</b>
        {#if emptySection(s)}{#if s.why}<p class="m">{s.why}</p>{/if}{#if s.when}<p class="m">{s.when}</p>{/if}{:else}<p>{s.text}</p>{/if}
        {#if screen(s.dove)}<div class="ac"><span class="sp"></span><button class="text" onclick={() => onScreen(screen(s.dove)!)}>{t.featuresGo}</button></div>{/if}
      </div>
    {/each}
    {#if firstRoundOffer(page)}
      <div class="c"><p>{firstRoundOffer(page)}</p><div class="ac"><span class="sp"></span><button class="tonal" onclick={() => touch('first_round')}>{t.primaFirstRound}</button></div></div>
    {/if}
    {#if finished(page)}<div class="ac"><span class="sp"></span><button class="text" onclick={onFeatures}>{t.primaFeatures}</button></div>{/if}
  {/if}
</div>

<style>
  .pr { padding: 0 12px 24px; max-width: 720px; display: flex; flex-direction: column; gap: 10px; }
  .c { background: var(--low); border-radius: 20px; padding: 14px; display: flex; flex-direction: column; gap: 6px; }
  .c b { font-weight: 600; }
  p { font-size: 14.5px; overflow-wrap: anywhere; }
  .m { color: var(--text2); }
  .ac { display: flex; align-items: center; gap: 8px; } .sp { flex: 1; }
  .tonal { background: var(--high); color: var(--text); border-radius: 999px; padding: 8px 14px; font-weight: 500; font-size: 14px; white-space: nowrap; }
  .wide { min-height: 44px; }
  .out { border: 1px solid var(--line); color: var(--text); border-radius: 999px; padding: 8px 14px; font-size: 14px; white-space: nowrap; }
  .text { color: var(--icon); font-size: 14px; font-weight: 500; padding: 8px 4px; cursor: pointer; }
</style>
